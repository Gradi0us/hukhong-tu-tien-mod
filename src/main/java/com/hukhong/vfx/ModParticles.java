package com.hukhong.vfx;

import com.hukhong.HuKhong;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particle riêng: thánh quang (vàng), linh khí (ngọc), bá khí (cam), hư không (tím). */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, HuKhong.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> THANH_QUANG = PARTICLES.register("thanh_quang", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LINH_KHI = PARTICLES.register("linh_khi", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BA_KHI = PARTICLES.register("ba_khi", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HU_KHONG = PARTICLES.register("hu_khong", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KIEM_KHI = PARTICLES.register("kiem_khi", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MA_KHI = PARTICLES.register("ma_khi", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) { PARTICLES.register(bus); ModSounds.SOUNDS.register(bus); }
}
