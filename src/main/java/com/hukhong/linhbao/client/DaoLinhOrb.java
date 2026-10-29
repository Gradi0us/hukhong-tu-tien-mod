package com.hukhong.linhbao.client;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.client.ClientCache;
import com.hukhong.client.ClientConfig;
import com.hukhong.linhbao.LinhBaoMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Đạo Linh (client): cầu thức thần vàng-trắng lơ lửng ở vai phải, rune quay quanh. Vẽ bằng hạt (không cần model/GeckoLib).
 * Người khác chỉ thấy khi Đạo Linh đã tiến hóa (giai đoạn >= 1: ArtifactData là phần duy nhất đồng bộ cho mọi client); bản thân luôn thấy.
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class DaoLinhOrb {
    private DaoLinhOrb() {}

    private static final DustParticleOptions CORE = new DustParticleOptions(new Vector3f(1.0f, 0.93f, 0.62f), 1.1f);
    private static final DustParticleOptions HALO = new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 0.6f);

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        boolean low = ClientConfig.particles() == ClientConfig.ParticleLevel.LOW;
        long t = level.getGameTime();
        if (low && (t & 1) == 1) return;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        for (AbstractClientPlayer p : level.players()) {
            if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(cam) > 40 * 40) continue;
            boolean self = p == mc.player;
            if (self && mc.options.getCameraType().isFirstPerson()) continue; // hạt sát camera che màn hình
            int stage = p.getData(ArtifactAttachments.ARTIFACT.get()).dao();
            if (self ? !isDao() : stage <= 0) continue;
            double[] o = LinhBaoMath.orbOffset(p.yBodyRot, t + p.getId());
            double x = p.getX() + o[0], y = p.getY() + o[1], z = p.getZ() + o[2];
            level.addParticle(CORE, x, y, z, 0, 0, 0);
            if ((t & 1) == 0) level.addParticle(HALO, x, y, z, 0, 0.003, 0);
            // rune quay quanh cầu (mặt phẳng nghiêng), số rune tăng theo giai đoạn
            int n = LinhBaoMath.orbRunes(stage);
            double r = 0.2 + 0.015 * stage;
            for (int i = 0; i < n; i++) {
                double a = t * 0.18 + i * (Math.PI * 2 / n);
                double cx = Math.cos(a) * r, cz = Math.sin(a) * r;
                if ((t + i) % 3 == 0) level.addParticle(ParticleTypes.ENCHANT, x + cx, y + cz * 0.45, z + cz * 0.9, 0, 0, 0);
            }
            // vệt sao rơi nhẹ, chỉ khi di chuyển
            if (stage >= 2 && p.getDeltaMovement().horizontalDistanceSqr() > 0.004 && (t & 1) == 0)
                level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, -0.01, 0);
        }
    }

    private static boolean isDao() {
        var cd = ClientCache.cultivation();
        return cd != null && cd.paths().contains(com.hukhong.core.Path.DAO);
    }
}
