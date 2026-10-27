package com.hukhong.client.fx;

import com.hukhong.HuKhong;
import com.hukhong.vfx.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.concurrent.atomic.AtomicInteger;

/** Hạt VFX tint theo màu, sinh trực tiếp phía client (không qua mạng), có trần số lượng. */
public final class FxParticle extends TextureSheetParticle {
    static final AtomicInteger COUNT = new AtomicInteger();
    static SpriteSet GLOW, STAR, SHARD;

    private final float base;
    private final boolean shrink;
    private float maxA = 1f;

    FxParticle a(float v) { maxA = v; return this; }

    FxParticle(ClientLevel l, double x, double y, double z, double vx, double vy, double vz,
               SpriteSet set, int rgb, float size, int life, float grav, boolean shrink) {
        super(l, x, y, z, vx, vy, vz);
        this.xd = vx; this.yd = vy; this.zd = vz;
        this.friction = 0.92f; this.gravity = grav; this.hasPhysics = false;
        this.lifetime = life; this.base = size; this.quadSize = size; this.shrink = shrink;
        setColor((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        pickSprite(set);
        COUNT.incrementAndGet();
    }

    @Override public void tick() {
        super.tick();
        float k = (float) age / lifetime;
        alpha = Math.max(0f, maxA * (1f - k * k));
        if (shrink) quadSize = base * (1f - 0.7f * k);
    }

    @Override public void remove() {
        if (!removed) COUNT.decrementAndGet();
        super.remove();
    }

    @Override public int getLightColor(float pt) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    @EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void onRegister(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ModParticles.FX_GLOW.get(), s -> { GLOW = s; return (t, l, x, y, z, vx, vy, vz) -> null; });
            e.registerSpriteSet(ModParticles.FX_STAR.get(), s -> { STAR = s; return (t, l, x, y, z, vx, vy, vz) -> null; });
            e.registerSpriteSet(ModParticles.FX_SHARD.get(), s -> { SHARD = s; return (t, l, x, y, z, vx, vy, vz) -> null; });
        }
    }
}
