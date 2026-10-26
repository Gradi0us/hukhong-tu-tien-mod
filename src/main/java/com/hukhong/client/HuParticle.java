package com.hukhong.client;

import com.hukhong.HuKhong;
import com.hukhong.vfx.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Particle tự phát sáng, mờ dần; tôn trọng tùy chọn giảm particle. */
public class HuParticle extends TextureSheetParticle {
    private final float baseSize;
    private final boolean shrink;

    HuParticle(ClientLevel l, double x, double y, double z, double vx, double vy, double vz,
               SpriteSet set, float rise, float size, int life, boolean shrink) {
        super(l, x, y, z, vx, vy, vz);
        this.xd = vx * 0.3 + (random.nextDouble() - 0.5) * 0.02;
        this.yd = vy * 0.3 + rise + random.nextDouble() * 0.01;
        this.zd = vz * 0.3 + (random.nextDouble() - 0.5) * 0.02;
        this.friction = 0.94f;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = life + random.nextInt(8);
        this.baseSize = size * (0.8f + random.nextFloat() * 0.4f);
        this.quadSize = baseSize;
        this.shrink = shrink;
        pickSprite(set);
    }

    @Override public void tick() {
        super.tick();
        float k = (float) age / lifetime;
        this.alpha = 1f - k * k;
        if (shrink) this.quadSize = baseSize * (1f - 0.6f * k);
    }

    @Override public int getLightColor(float pt) { return 0xF000F0; }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    record Provider(SpriteSet set, float rise, float size, int life, boolean shrink) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z,
                                                 double vx, double vy, double vz) {
            switch (ClientConfig.particles()) {
                case OFF -> { return null; }
                case LOW -> { if (l.random.nextInt(3) != 0) return null; }
                default -> {}
            }
            return new HuParticle(l, x, y, z, vx, vy, vz, set, rise, size, life, shrink);
        }
    }

    @EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegister(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ModParticles.THANH_QUANG.get(), s -> new Provider(s, 0.01f, 0.16f, 22, true));
            e.registerSpriteSet(ModParticles.LINH_KHI.get(), s -> new Provider(s, 0.02f, 0.12f, 26, false));
            e.registerSpriteSet(ModParticles.BA_KHI.get(), s -> new Provider(s, 0.04f, 0.2f, 18, true));
            e.registerSpriteSet(ModParticles.KIEM_KHI.get(), s -> new Provider(s, 0.0f, 0.22f, 14, true));
            e.registerSpriteSet(ModParticles.MA_KHI.get(), s -> new Provider(s, 0.03f, 0.2f, 22, true));
            e.registerSpriteSet(ModParticles.HU_KHONG.get(), s -> new Provider(s, 0.015f, 0.18f, 24, true));
        }
    }
}
