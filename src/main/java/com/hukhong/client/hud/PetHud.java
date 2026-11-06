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

    static final int W = 92, H = 15, GAP = 3;
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
        for (int i = 0; i < n; i++) panel(g, mc.font, 4, y + i * (H + GAP), list.get(i), dtSec);
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

    /** Thẻ gọn: dòng tên + cấp (+ % trưởng thành nếu ấu thể), thanh HP mảnh bên dưới. */
    private static void panel(GuiGraphics g, Font font, int x, int y, PetSyncPayload.Entry e, float dt) {
        PetBranch b = PetBranch.byId(e.branch());
        boolean dead = e.status() == 2;
        int accent = 0xFF000000 | (b == null ? 0xB8B8C4 : b.rgb);
        g.fill(x, y, x + W, y + H, 0x88000000);
        g.fill(x, y, x + 1, y + H, accent);
        String species = net.minecraft.network.chat.Component.translatable("entity.hukhong." + e.kind()).getString();
        String right = dead ? Math.max(0, e.reviveSec()) + "s" : "Lv." + e.level() + (e.growth() >= 0 ? " · " + e.growth() + "%" : "");
        float ts = 0.75f;
        g.pose().pushPose();
        g.pose().translate(x + 4, y + 2.5f, 0);
        g.pose().scale(ts, ts, 1f);
        int rw = font.width(right);
        int nameW = (int) ((W - 8) / ts) - rw - 6;
        g.drawString(font, font.plainSubstrByWidth(species, nameW), 0, 0, dead ? 0xFF8A8A8A : 0xFFFFFFFF, true);
        g.drawString(font, right, (int) ((W - 8) / ts) - rw, 0, dead ? 0xFFD0A0A0 : (e.growth() >= 0 ? 0xFF9FE08A : accent), true);
        g.pose().popPose();

        float target = e.maxHp() > 0 ? Math.max(0f, Math.min(1f, e.hp() / e.maxHp())) : 0f;
        float[] s = SMOOTH.computeIfAbsent(e.slot(), k -> new float[]{target, target});
        s[0] += (target - s[0]) * Math.min(1f, dt * 8f);
        if (s[1] > s[0]) s[1] = Math.max(s[0], s[1] - dt * 0.35f); else s[1] = s[0];
        int bx = x + 4, by = y + H - 4, bw = W - 8;
        g.fill(bx, by, bx + bw, by + 2, 0xFF2A1A1A);
        if (dead) {
            float prog = Math.max(0f, Math.min(1f, 1f - e.reviveSec() / REVIVE_SEC));
            g.fill(bx, by, bx + Math.round(bw * prog), by + 2, 0xFF4A8AE0);
        } else {
            g.fill(bx, by, bx + Math.round(bw * s[1]), by + 2, 0xFFF2F2F2);
            g.fill(bx, by, bx + Math.round(bw * s[0]), by + 2, 0xFFD83A3A);
        }
    }
}
