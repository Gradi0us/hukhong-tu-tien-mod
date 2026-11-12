package com.hukhong.client.combatui;

import com.hukhong.HuKhong;
import com.hukhong.client.boss.CutsceneClient;
import com.hukhong.endgame.DaKhuyet;
import com.hukhong.endgame.NguyenThuyTichDiet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Dev: ./gradlew runClient -Pcutscenedemo -> tạo thế giới phẳng MỚI, lần lượt trình diễn các cutscene mới cho người chơi xem:
 * Lv10 (NTTD + Mộ Dao) -> khảo hạch Dạ Khuyết + cải tà -> NTTD: đổi pha, Dạ Khuyết đồng minh + đỡ tất sát, hy sinh, Mộ Dao pha 4 -> kết thúc.
 * Mỗi bước đợi cutscene trước chạy xong + nghỉ vài giây. Cửa sổ thật, không tự tắt.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DevCutsceneDemo {
    private DevCutsceneDemo() {}
    private static final boolean ON = System.getProperty("hukhong.cutscenedemo") != null;
    private static final String WORLD = "hk-cutscene-demo";
    private static boolean created;
    private static int step, wait = 100, hits, lastPhase, lastForm, phaseWait;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.level == null && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
            created = true;
            java.io.File dir = new java.io.File(mc.gameDirectory, "saves/" + WORLD);
            if (dir.exists()) deleteDir(dir); // luôn là thế giới mới
            LevelSettings ls = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(WORLD, ls, new WorldOptions(424242L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.isPaused() || CutsceneClient.active()) return;
        if (wait > 0) { wait--; return; }
        ServerPlayer sp = server.getPlayerList().getPlayer(mc.player.getUUID());
        if (sp == null) return;
        server.execute(() -> run(server, sp));
        wait = 10;
    }

    private static void say(ServerPlayer sp, String s) {
        sp.sendSystemMessage(Component.literal("§d[Demo] §f" + s));
        sp.displayClientMessage(Component.literal("§d" + s), true);
    }

    private static void cmd(MinecraftServer server, ServerPlayer sp, String c) {
        server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
    }

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static NguyenThuyTichDiet boss;

    private static void run(MinecraftServer server, ServerPlayer sp) {
        var lv = sp.serverLevel();
        LOG.info("[CutsceneDemo] step {} at {} dim {}", step, sp.blockPosition(), lv.dimension().location());
        switch (step) {
            case 0 -> { // đợi xong màn tạo nhân vật / dịch chuyển điểm hồi sinh
                if (Minecraft.getInstance().screen != null) { wait = 40; return; }
                lv.setDayTime(14000);
                lv.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                lv.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                sp.setInvulnerable(true);
                say(sp, "1/6 Cutscene đột phá cấp 10: Nguyên Thủy Tịch Diệt tập kích, Mộ Dao ngăn cản");
                wait = 100; step++;
            }
            case 1 -> {
                boolean ok = com.hukhong.core.Lv10Cutscene.replay(sp);
                LOG.info("[CutsceneDemo] lv10 replay -> {}", ok);
                if (!ok) say(sp, "Lv10 cutscene không chạy được");
                wait = 100; step++;
            }
            case 2 -> {
                say(sp, "2/6 Khảo hạch Dạ Khuyết (459.000 máu) -> cutscene cải tà quy chính");
                var at = sp.position().add(sp.getLookAngle().multiply(8, 0, 8));
                dk = DaKhuyet.spawnTrial(lv, at, sp.getYRot() + 180);
                LOG.info("[CutsceneDemo] trial spawn -> {}", dk);
                wait = 300; step++; // xem nàng ra chiêu một lúc
            }
            case 3 -> {
                if (dk != null && dk.isAlive()) dk.devRedeem();
                wait = 200; step++;
            }
            case 4 -> {
                say(sp, "3/6 Vào Endgame, triệu hồi Nguyên Thủy Tịch Diệt");
                com.hukhong.endgame.EndgameService.enter(sp, true);
                wait = 100; step++;
            }
            case 5 -> {
                boss = com.hukhong.endgame.EndgameService.spawnBoss(lv);
                LOG.info("[CutsceneDemo] boss -> {}", boss);
                if (boss != null) sp.teleportTo(lv, boss.getX() + 14, boss.getY(), boss.getZ(), sp.getYRot(), 0);
                wait = 400; step++;
            }
            case 6 -> { // dạng 1 -> biến hình dạng 2
                if (boss == null || !boss.isAlive()) { step = 20; return; }
                say(sp, "4/6 Dạng 1 vỡ -> Nguyên Thủy Tịch Diệt thật lộ diện");
                boss.startMorph(lv);
                wait = 500; step++;
            }
            case 7 -> { setFrac(0.70f); wait = 400; step++; }          // pha 2
            case 8 -> { setFrac(0.45f); say(sp, "Pha 3: Dạ Khuyết tham chiến. Chờ ~90s để thấy đại chiêu tất sát và nàng dựng kết giới đỡ"); wait = 2000; step++; }
            case 9 -> { say(sp, "5/6 Sang pha 4: Dạ Khuyết hy sinh, Mộ Dao trợ chiến"); setFrac(0.20f); wait = 700; step++; }
            case 10 -> { say(sp, "6/6 Boss gục -> cutscene kết thúc (bỏ qua được sau một nửa)"); if (boss != null && boss.isAlive()) boss.kill(); wait = 200; step = 20; }
            case 20 -> { say(sp, "Demo xong."); step++; }
            default -> wait = Integer.MAX_VALUE / 2;
        }
    }

    private static DaKhuyet dk;

    private static void setFrac(float f) {
        if (boss != null && boss.isAlive()) boss.setHealth(boss.getMaxHealth() * f);
    }

    private static <T extends net.minecraft.world.entity.Entity> T first(net.minecraft.server.level.ServerLevel lv, Class<T> c, ServerPlayer sp, double r) {
        var l = lv.getEntitiesOfClass(c, new AABB(sp.blockPosition()).inflate(r), x -> x.isAlive());
        return l.isEmpty() ? null : l.get(0);
    }

    private static void deleteDir(java.io.File f) {
        java.io.File[] k = f.listFiles();
        if (k != null) for (java.io.File c : k) deleteDir(c);
        f.delete();
    }
}
