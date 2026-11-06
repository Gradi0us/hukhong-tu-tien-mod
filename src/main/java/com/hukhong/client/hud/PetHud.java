package com.hukhong.client.hud;

import com.hukhong.HuKhong;
import com.hukhong.auction.Tx;
import com.hukhong.client.ClientConfig;
import com.hukhong.creature.pet.PetBranch;
import com.hukhong.creature.pet.PetSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD linh thu ben TRAI giua man hinh: moi thu (chien dau + linh sung) 1 the: huy hieu, ten, cap/tang/nhanh, thanh HP
 * (do, vet trang tre xuong sau khi mat mau kieu boss bar), trang thai (theo sat / ngoi / dem nguoc hoi sinh).
 * Du lieu tu {@link PetSyncPayload} moi 5 tick; HP chay muot theo thoi gian thuc.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class PetHud {
    private PetHud() {}

    static final int W = 124, H = 40, GAP = 6, TX = 33;
    /** Thoi gian hoi sinh (giay) de ve thanh tien do - khop PetService.REVIVE_TICKS. */
    static final float REVIVE_SEC = 120f;

    private static volatile List<PetSyncPayload.Entry> entries = List.of();
    /** slot -> {hp hien thi, vet trang}. */
    private static final Map<String, float[]> SMOOTH = new HashMap<>();
    private static long lastNanos;

    public static void update(List<PetSyncPayload.Entry> list) { entries = list == null ? List.of() : list; }

    @SubscribeEvent
    public static void onRegister(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "pet_hud"), PetHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker dt) {
        if (!ClientConfig.showHud()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        List<PetSyncPayload.Entry> list = entries;
        if (list.isEmpty()) { SMOOTH.clear(); lastNanos = 0; return; }
        long now = System.nanoTime();
        float dtSec = lastNanos == 0 ? 0.05f : Math.min(0.2f, (now - lastNanos) / 1e9f);
        lastNanos = now;
        int n = list.size();
        int total = n * H + (n - 1) * GAP;
        int y = Math.max(72, g.guiHeight() / 2 - total / 2);
        SMOOTH.keySet().removeIf(k -> list.stream().noneMatch(e -> e.slot().equals(k)));
        for (int i = 0; i < n; i++) panel(g, mc.font, 6, y + i * (H + GAP), list.get(i), dtSec);
    }

    /** Thú của mình (đúng loại) đang được client tải, gần nhất. */
    private static net.minecraft.world.entity.LivingEntity find(String kind) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        net.minecraft.world.entity.LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (var en : mc.level.entitiesForRendering()) {
            if (!(en instanceof com.hukhong.creature.LinhThu t) || !t.isTame() || !mc.player.getUUID().equals(t.getOwnerUUID())) continue;
            if (!kind.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t.getType()).getPath())) continue;
            double d = t.distanceToSqr(mc.player);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    private static void panel(GuiGraphics g, Font font, int x, int y, PetSyncPayload.Entry e, float dt) {
        PetBranch b = PetBranch.byId(e.branch());
        boolean dead = e.status() == 2;
        int accent = 0xFF000000 | (b == null ? 0x9A9AA6 : b.rgb);
        g.fill(x, y, x + W, y + H, dead ? 0x9A0E0A10 : 0xD80E0812);
        g.fill(x, y, x + 2, y + H, accent);

        // huy hieu: vong mau nhanh + chu cai dau loai
        int px = x + 6, py = y + (H - 22) / 2;
        g.fill(px - 1, py - 1, px + 23, py + 23, accent);
        g.fill(px, py, px + 22, py + 22, 0xFF1A1224);
        String species = net.minecraft.network.chat.Component.translatable("entity.hukhong." + e.kind()).getString();
        String ini = species.isEmpty() ? "?" : species.substring(0, 1);
        net.minecraft.world.entity.LivingEntity live = dead ? null : find(e.kind());
        if (live != null) { // chân dung 3D của chính con thú
            float h = Math.max(0.5f, live.getBbHeight()), w = Math.max(0.5f, live.getBbWidth());
            int sc = Math.max(4, Math.round(15f / Math.max(h, w * 1.2f)));
            g.enableScissor(px, py, px + 22, py + 22);
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, px - 10, py - 6, px + 32, py + 22,
                    sc, 0.0625f, px + 30, py + 2, live);
            g.disableScissor();
        } else g.drawString(font, ini, px + 11 - font.width(ini) / 2, py + 7, dead ? 0xFF707070 : 0xFFFFFFFF, false);

        int tw = W - TX - 4;
        g.drawString(font, font.plainSubstrByWidth(species, tw), x + TX, y + 4, dead ? 0xFF8A8A8A : 0xFFFFFFFF, true);

        String form = b == null || e.stage() == 0 ? "" : b.formName(e.stage()).getString() + " ";
        String sub = "Lv." + e.level() + (form.isEmpty() ? "" : " · " + form.trim());
        g.drawString(font, font.plainSubstrByWidth(sub, tw), x + TX, y + 13, dead ? 0xFF707070 : accent, true);

        // thanh HP: do + vet trang tre xuong; chet -> thanh xanh = tien do hoi sinh
        float target = e.maxHp() > 0 ? Math.max(0f, Math.min(1f, e.hp() / e.maxHp())) : 0f;
        float[] s = SMOOTH.computeIfAbsent(e.slot(), k -> new float[]{target, target});
        float k1 = Math.min(1f, dt * 8f);
        s[0] += (target - s[0]) * k1;
        if (s[1] > s[0]) s[1] = Math.max(s[0], s[1] - dt * 0.35f); else s[1] = s[0];

        int by = y + 24, bw = tw;
        g.fill(x + TX - 1, by - 1, x + TX + bw + 1, by + 6, 0xFF000000);
        g.fill(x + TX, by, x + TX + bw, by + 5, 0xFF2A1A1A);
        int trailW = Math.round(bw * s[1]);
        g.fill(x + TX, by, x + TX + trailW, by + 5, 0xFFF2F2F2);
        int mainW = Math.round(bw * s[0]);
        if (dead) {
            float prog = Math.max(0f, Math.min(1f, 1f - e.reviveSec() / REVIVE_SEC));
            g.fill(x + TX, by, x + TX + Math.round(bw * prog), by + 5, 0xFF4A8AE0);
        } else {
            g.fill(x + TX, by, x + TX + mainW, by + 5, 0xFFD83A3A);
            g.fill(x + TX, by, x + TX + mainW, by + 1, 0xFFFF8A8A);
        }

        // trang thai
        String status;
        if (dead) {
            int sec = Math.max(0, e.reviveSec());
            status = Tx.t("hukhong.pet.hud.dead", "Hồi sinh %s", "Revives in %s", (sec / 60) + ":" + String.format("%02d", sec % 60)).getString();
        } else {
            status = (e.status() == 1 ? Tx.t("hukhong.pet.hud.sit", "Ngồi yên", "Sitting") : Tx.t("hukhong.pet.hud.follow", "Theo sát", "Following")).getString()
                    + " · " + Tx.t("hukhong.pet.hud.bond", "TM %s", "Bond %s", e.bond()).getString();
        }
        String hpTxt = dead ? "" : Math.round(e.hp()) + "/" + Math.round(e.maxHp());
        float ts = 0.7f;
        g.pose().pushPose();
        g.pose().translate(x + TX, y + 31.5f, 0);
        g.pose().scale(ts, ts, 1f);
        g.drawString(font, status, 0, 0, dead ? 0xFFD0A0A0 : 0xFFB8B8C8, true);
        g.pose().popPose();
        if (!hpTxt.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x + TX + bw - font.width(hpTxt) * ts, y + 31.5f, 0);
            g.pose().scale(ts, ts, 1f);
            g.drawString(font, hpTxt, 0, 0, 0xFFFFFFFF, true);
            g.pose().popPose();
        }
    }
}
