package com.hukhong.client.discord;

import com.google.gson.JsonObject;
import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.core.Realm;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Discord Rich Presence: nói chuyện thẳng với app Discord qua IPC (pipe discord-ipc-N), không cần thư viện ngoài.
 * Chạy trên luồng riêng; không có Discord thì im lặng thử lại sau.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DiscordPresence {
    private DiscordPresence() {}
    private static final Logger LOG = LogUtils.getLogger();
    /** App ID mặc định của mod; config discordAppId (nếu điền) sẽ ghi đè. */
    public static final String DEFAULT_APP_ID = "1569341706644488282";

    private static final long START = System.currentTimeMillis() / 1000;
    private static volatile String details = "Ở sảnh chính", state = "", smallKey = "", smallText = "";
    private static volatile boolean dirty = true;
    private static String bossName;
    private static long bossSeen;
    private static int tick;
    private static Thread worker;

    // ------------------------------------------------------------ thu thập trạng thái (luồng game)
    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress e) {
        bossName = e.getBossEvent().getName().getString();
        bossSeen = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (++tick % 40 != 0) return;
        if (worker == null) start();
        Minecraft mc = Minecraft.getInstance();
        String d, s = "", sk = "", st = "";
        if (mc.player == null) {
            d = "Ở sảnh chính";
        } else {
            CultivationData c = mc.player.getData(ModAttachments.CULTIVATION);
            d = I18n.get(Realm.langKey(c.level())) + " · Lv " + c.level();
            if (bossName != null && System.currentTimeMillis() - bossSeen < 3000) s = "⚔ Đang đánh " + bossName;
            else s = mc.isSingleplayer() ? "Chơi đơn" : "Chơi trên server";
        }
        if (!d.equals(details) || !s.equals(state) || !sk.equals(smallKey)) {
            details = d; state = s; smallKey = sk; smallText = st; dirty = true;
        }
    }

    private static String pathName(Path p) {
        return I18n.get(switch (p) {
            case KIEM -> "hukhong.path.sword";
            case THE -> "hukhong.path.body";
            case MA -> "hukhong.path.demon";
            default -> "hukhong.path." + p.id;
        });
    }

    private static String appId() {
        String id = ClientConfig.SPEC.isLoaded() ? ClientConfig.DISCORD_APP_ID.get().trim() : "";
        return id.isEmpty() ? DEFAULT_APP_ID : id;
    }

    private static boolean enabled() {
        return (!ClientConfig.SPEC.isLoaded() || ClientConfig.DISCORD.get()) && !appId().isEmpty();
    }

    // ------------------------------------------------------------ IPC (luồng riêng)
    private static void start() {
        worker = new Thread(DiscordPresence::loop, "HuKhong-DiscordRPC");
        worker.setDaemon(true);
        worker.start();
    }

    private static void loop() {
        while (true) {
            if (!enabled()) { sleep(10_000); continue; }
            String id = appId();
            try (Ipc ipc = Ipc.open()) {
                if (ipc == null) { sleep(20_000); continue; }
                JsonObject hs = new JsonObject();
                hs.addProperty("v", 1);
                hs.addProperty("client_id", id);
                ipc.send(0, hs);
                dirty = true;
                LOG.info("Discord RPC connected");
                while (enabled() && id.equals(appId())) {
                    if (dirty) { dirty = false; ipc.send(1, activity()); }
                    sleep(2_000);
                }
                ipc.send(1, clear());
            } catch (Exception ex) {
                LOG.debug("Discord RPC lost: {}", ex.toString());
                sleep(20_000);
            }
        }
    }

    private static JsonObject activity() {
        JsonObject a = new JsonObject();
        a.addProperty("details", details);
        if (!state.isEmpty()) a.addProperty("state", state);
        JsonObject ts = new JsonObject();
        ts.addProperty("start", START);
        a.add("timestamps", ts);
        JsonObject as = new JsonObject();
        as.addProperty("large_image", "logo");
        as.addProperty("large_text", "Hư Không Tu Tiên Lục");
        if (!smallKey.isEmpty()) { as.addProperty("small_image", smallKey); as.addProperty("small_text", smallText); }
        a.add("assets", as);
        return cmd(a);
    }

    private static JsonObject clear() { return cmd(null); }

    private static JsonObject cmd(JsonObject activity) {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        if (activity != null) args.add("activity", activity);
        JsonObject o = new JsonObject();
        o.addProperty("cmd", "SET_ACTIVITY");
        o.add("args", args);
        o.addProperty("nonce", UUID.randomUUID().toString());
        return o;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    /** Khung IPC: [op int32 LE][len int32 LE][json UTF-8]. */
    private static final class Ipc implements AutoCloseable {
        private final AutoCloseable handle;
        private final OutputStream out;
        private final InputStream in;
        private final Thread reader;

        private Ipc(AutoCloseable h, OutputStream o, InputStream i) {
            handle = h; out = o; in = i;
            // đọc bỏ phản hồi để pipe không đầy
            reader = new Thread(() -> {
                byte[] buf = new byte[4096];
                try { while (in.read(buf) >= 0) {} } catch (IOException ignored) {}
            }, "HuKhong-DiscordRPC-read");
            reader.setDaemon(true);
            reader.start();
        }

        static Ipc open() {
            for (int i = 0; i < 10; i++) {
                try {
                    if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                        var f = new java.io.RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                        return new Ipc(f, Channels.newOutputStream(f.getChannel()), Channels.newInputStream(f.getChannel()));
                    }
                    for (String dir : unixDirs()) {
                        java.nio.file.Path sock = java.nio.file.Path.of(dir, "discord-ipc-" + i);
                        if (!java.nio.file.Files.exists(sock)) continue;
                        var ch = java.nio.channels.SocketChannel.open(java.net.StandardProtocolFamily.UNIX);
                        ch.connect(java.net.UnixDomainSocketAddress.of(sock));
                        return new Ipc(ch, Channels.newOutputStream(ch), Channels.newInputStream(ch));
                    }
                } catch (Exception ignored) {}
            }
            return null;
        }

        private static java.util.List<String> unixDirs() {
            java.util.List<String> l = new java.util.ArrayList<>();
            for (String k : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
                String v = System.getenv(k);
                if (v != null) { l.add(v); l.add(v + "/app/com.discordapp.Discord"); l.add(v + "/snap.discord"); }
            }
            l.add("/tmp");
            return l;
        }

        synchronized void send(int op, JsonObject json) throws IOException {
            byte[] b = json.toString().getBytes(StandardCharsets.UTF_8);
            ByteBuffer h = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(op).putInt(b.length);
            out.write(h.array());
            out.write(b);
            out.flush();
        }

        @Override public void close() throws Exception { handle.close(); }
    }
}
