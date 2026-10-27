package com.hukhong.siege;

import com.hukhong.city.ambient.AmbientNpc;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** Hieu ung hinh/am cho man vay thanh: thuan server (hat + am thanh vanilla), khong khoi chat, khong pha block. */
public final class SiegeFx {
    private SiegeFx() {}

    public static final DustParticleOptions VOID_DUST = new DustParticleOptions(new Vector3f(0.55f, 0.18f, 0.95f), 1.1f);
    public static final DustParticleOptions SPIRIT_DUST = new DustParticleOptions(new Vector3f(0.45f, 0.92f, 1.0f), 0.9f);
    public static final DustParticleOptions TALISMAN_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.25f), 1.0f);

    /** Dang co man vay thanh / bao ve tong nao dang chay (dan cu dung de tu thoat trang thai khi het su kien). */
    public static boolean sieging() { return SiegeDirector.running(); }

    public static void sound(ServerLevel l, Vec3 p, SoundEvent s, float vol, float pitch) {
        l.playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    public static void ambientSound(ServerLevel l, Vec3 p, SoundEvent s, float vol, float pitch) {
        l.playSound(null, p.x, p.y, p.z, s, SoundSource.AMBIENT, vol, pitch);
    }

    /** Hoi chuong bao dong: tieng dong tram + vang. */
    public static void bell(ServerLevel l, Vec3 p, float pitch) {
        l.playSound(null, p.x, p.y, p.z, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 5f, pitch);
        l.playSound(null, p.x, p.y, p.z, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 3f, pitch);
    }

    /** Lan khoi nho (xuat hien / bien mat). */
    public static void puff(ServerLevel l, Vec3 p) {
        l.sendParticles(ParticleTypes.POOF, p.x, p.y, p.z, 8, 0.25, 0.4, 0.25, 0.02);
        l.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 4, 0.25, 0.4, 0.25, 0.03);
    }

    /** Vong hat nam ngang. */
    public static void ring(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n, double phase) {
        for (int i = 0; i < n; i++) {
            double a = phase + i * Math.PI * 2 / n;
            l.sendParticles(o, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Vong xung kich lan rong (goi moi tick, t = 0..dur). */
    public static void shockwave(ServerLevel l, Vec3 c, double maxR, int t, int dur, ParticleOptions o) {
        double r = maxR * (t / (double) dur);
        int n = Math.max(10, (int) (r * 5));
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            l.sendParticles(o, c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 1, 0, 0.02, 0, 0.01);
        }
    }

    /** "Mui ten linh khi": vet sang xanh ngoc bay tu cung thu toi muc tieu trong 3 tick, roi gay sat thuong. */
    public static void spiritArrow(AmbientNpc from, LivingEntity to, float damage) {
        if (!(from.level() instanceof ServerLevel l)) return;
        Vec3 a = from.getEyePosition().add(from.getLookAngle().scale(0.6));
        Vec3 b = to.position().add(0, to.getBbHeight() * 0.55, 0);
        l.playSound(null, a.x, a.y, a.z, SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 0.9f, 1.5f);
        l.playSound(null, a.x, a.y, a.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 0.6f, 1.8f);
        for (int seg = 0; seg < 3; seg++) {
            final int sg = seg;
            SiegeDirector.schedule(l, seg, () -> {
                for (int i = 0; i < 4; i++) {
                    double f = (sg * 4 + i) / 12.0;
                    Vec3 p = a.lerp(b, f);
                    l.sendParticles(i == 3 ? ParticleTypes.END_ROD : SPIRIT_DUST, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
                }
            });
        }
        SiegeDirector.schedule(l, 3, () -> {
            if (!to.isAlive() || !from.isAlive()) return;
            to.hurt(l.damageSources().mobAttack(from), damage);
            l.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 8, 0.25, 0.25, 0.25, 0.15);
            l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, b.x, b.y, b.z, 3, 0.15, 0.15, 0.15, 0.02);
            l.playSound(null, b.x, b.y, b.z, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.HOSTILE, 0.8f, 1.4f);
        });
    }

    /** Phao hieu / phao hoa that (nhe: phat no tren cao). */
    public static void rocket(ServerLevel l, Vec3 p, int color, int fade, FireworkExplosion.Shape shape, int flight) {
        ItemStack st = new ItemStack(Items.FIREWORK_ROCKET);
        st.set(DataComponents.FIREWORKS, new Fireworks(flight,
                List.of(new FireworkExplosion(shape, IntList.of(color), IntList.of(fade), false, true))));
        l.addFreshEntity(new FireworkRocketEntity(l, p.x, p.y, p.z, st));
    }

    /** Cot sang (beacon gia) tu mat dat len troi, chi la hat. */
    public static void beam(ServerLevel l, Vec3 base, double height, ParticleOptions o) {
        l.sendParticles(o, base.x, base.y + height / 2, base.z, 10, 0.05, height / 2, 0.05, 0);
    }
}
