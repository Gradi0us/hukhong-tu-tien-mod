package com.hukhong.client.render.artifact;

import com.hukhong.HuKhong;
import com.hukhong.artifact.ArtifactAttachments;
import com.hukhong.artifact.ArtifactData;
import com.hukhong.client.ClientConfig;
import com.hukhong.vfx.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/** Hào quang hạt theo giai đoạn pháp khí: Đạo (thánh quang rơi từ cánh), Thể (khí vàng-cam), Ma (ma khí + khói tím). */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ArtifactAura {
    private ArtifactAura() {}

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.78f, 0.2f), 1.0f);
    private static final DustParticleOptions DARK = new DustParticleOptions(new Vector3f(0.28f, 0.02f, 0.38f), 1.2f);

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused() || ClientConfig.particles() == ClientConfig.ParticleLevel.OFF) return;
        boolean low = ClientConfig.particles() == ClientConfig.ParticleLevel.LOW;
        if (level.getGameTime() % (low ? 6 : 3) != 0) return;
        var cam = mc.gameRenderer.getMainCamera().getPosition();
        RandomSource r = level.random;
        for (AbstractClientPlayer p : level.players()) {
            if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(cam) > 32 * 32) continue;
            // Không sinh hạt quanh chính mình ở góc nhìn thứ nhất (hạt sát camera che màn hình)
            if (p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            ArtifactData a = p.getData(ArtifactAttachments.ARTIFACT.get());
            if (a.dao() > 0) {
                int n = 1 + a.dao() / 3;
                for (int i = 0; i < n; i++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    level.addParticle(ModParticles.THANH_QUANG.get(), p.getX() + Math.cos(ang) * 0.5,
                            p.getY() + 0.8 + r.nextDouble() * 0.9, p.getZ() + Math.sin(ang) * 0.5, 0, -0.01, 0);
                }
            }
            if (a.the() > 0) {
                int n = 1 + a.the() / 2;
                for (int i = 0; i < n; i++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    double x = p.getX() + Math.cos(ang) * 0.45, z = p.getZ() + Math.sin(ang) * 0.45;
                    level.addParticle(ModParticles.BA_KHI.get(), x, p.getY() + 0.1 + r.nextDouble() * 1.6, z, 0, 0.03, 0);
                    if (a.the() >= 3) level.addParticle(GOLD, x, p.getY() + r.nextDouble() * 1.8, z, 0, 0.02, 0);
                }
            }
            if (a.ma() > 0) {
                int n = 1 + a.ma() / 2;
                for (int i = 0; i < n; i++) {
                    double ang = r.nextDouble() * Math.PI * 2, rad = 0.3 + r.nextDouble() * 0.5;
                    double x = p.getX() + Math.cos(ang) * rad, z = p.getZ() + Math.sin(ang) * rad;
                    level.addParticle(ModParticles.MA_KHI.get(), x, p.getY() + 0.1 + r.nextDouble() * 1.7, z, 0, 0.02, 0);
                    level.addParticle(DARK, x, p.getY() + 0.05 + r.nextDouble() * 0.5, z, 0, 0.01, 0);
                }
                if (a.ma() >= 5) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + (r.nextDouble() - 0.5) * 0.4,
                        p.getEyeY() + 0.3, p.getZ() + (r.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
            }
        }
    }
}
