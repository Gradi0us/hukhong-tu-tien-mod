package com.hukhong.client.endgame;

import com.hukhong.HuKhong;
import com.hukhong.endgame.VoidMark;
import com.hukhong.vfx.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Trạng thái dấu ngoại hình sau kết ({@link VoidMark}) phía client cho mọi người chơi (theo UUID), hạt hư không từ
 * nửa thân phải, và tay người thứ nhất. Lớp vẽ trên model: {@link VoidHalfLayer}.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class VoidMarkClient {
    private VoidMarkClient() {}

    private record Mark(int kind, long since) {}
    private static final Map<UUID, Mark> MARKS = new HashMap<>();
    static final int FADE = 40;

    public static void set(UUID who, int mark, boolean fade) {
        if (mark == VoidMark.NONE) { MARKS.remove(who); return; }
        Mark old = MARKS.get(who);
        if (old != null && old.kind == mark && !fade) return;
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0;
        MARKS.put(who, new Mark(mark, fade ? now : now - FADE));
    }

    public static int kind(Player p) { Mark m = MARKS.get(p.getUUID()); return m == null ? VoidMark.NONE : m.kind; }

    /** 0..1: hiện dần lần đầu (cutscene Hấp Thụ t200). */
    public static float alpha(Player p, float pt) {
        Mark m = MARKS.get(p.getUUID());
        if (m == null) return 0f;
        float age = p.level().getGameTime() - m.since + pt;
        return Math.max(0f, Math.min(1f, age / FADE));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) { MARKS.clear(); }

    /** Hạt hư không thoát ra từ nửa thân phải (mỗi 6 tick, người chơi trong 32 block, không tàng hình). */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || MARKS.isEmpty() || mc.level.getGameTime() % 6 != 0) return;
        for (Player p : mc.level.players()) {
            int k = kind(p);
            if (k == VoidMark.NONE || p.isInvisible() || p.distanceToSqr(mc.player) > 32 * 32) continue;
            if (p == mc.player && mc.options.getCameraType().isFirstPerson() && k != VoidMark.HALF_VOID) continue;
            double yr = Math.toRadians(p.yBodyRot);
            double rx = -Math.cos(yr), rz = -Math.sin(yr); // phải của thân
            var rnd = p.getRandom();
            if (k == VoidMark.HALF_VOID) {
                for (int i = 0; i < 1 + rnd.nextInt(2); i++) {
                    double off = 0.15 + rnd.nextDouble() * 0.25;
                    mc.level.addParticle(rnd.nextBoolean() ? ModParticles.HU_KHONG.get() : ParticleTypes.REVERSE_PORTAL,
                            p.getX() + rx * off, p.getY() + 0.3 + rnd.nextDouble() * 1.5, p.getZ() + rz * off, 0, 0.02, 0);
                }
            } else if (k == VoidMark.SEAL && rnd.nextInt(4) == 0) {
                mc.level.addParticle(ParticleTypes.WAX_OFF, p.getX() + rx * 0.4, p.getY() + 0.75, p.getZ() + rz * 0.4, 0, 0, 0);
            } else if (k == VoidMark.HALO && rnd.nextInt(3) == 0 && p.walkAnimation.speed() < 0.05f) {
                double fx = -Math.sin(yr), fz = Math.cos(yr);
                mc.level.addParticle(ParticleTypes.END_ROD, p.getX() - fx * 0.5 + (rnd.nextDouble() - 0.5) * 0.8,
                        p.getY() + 1.4 + rnd.nextDouble() * 0.8, p.getZ() - fz * 0.5 + (rnd.nextDouble() - 0.5) * 0.8, 0, 0.005, 0);
            }
        }
    }

    /** Tay người thứ nhất: vẽ tay vanilla rồi phủ lớp hư không / ấn vàng lên tay phải. */
    @SubscribeEvent
    public static void onArm(RenderArmEvent e) {
        AbstractClientPlayer p = e.getPlayer();
        int k = kind(p);
        if (e.getArm() != HumanoidArm.RIGHT || (k != VoidMark.HALF_VOID && k != VoidMark.SEAL)) return;
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        e.setCanceled(true);
        r.renderRightHand(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), p);
        VoidHalfLayer.renderArm(r.getModel(), k, alpha(p, 0f), e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight());
    }
}
