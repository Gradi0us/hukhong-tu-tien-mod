package com.hukhong.creature;

import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Kỹ năng mini boss (gọi mỗi tick từ LinhQuai.aiStep phía server). Mỗi con 2 chiêu luân phiên, báo trước bằng vòng đỏ:
 * <ul>
 *   <li>Hư Lang Vương: Tru Nguyệt (gọi 3 Hư Lang + tăng tốc bầy), Liệt Trảo Xung (lao vào mục tiêu, cào xé quạt trước mặt).</li>
 *   <li>Thạch Cự Nhân: Chấn Địa (dậm đất bán kính 7, hất tung), Phi Thạch (ném 3 tảng đá vào người chơi).</li>
 *   <li>Huyết Yêu Vương: Huyết Triều (hút người chơi trong 9 ô vào tâm + hút máu), Huyết Châm (mưa kim máu quanh mục tiêu).</li>
 * </ul>
 */
public final class MiniBossSkills {
    private MiniBossSkills() {}

    public static final int CD = 110, WINDUP = 24;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.75f, 0.03f, 0.08f), 1.5f);

    static void tick(LinhQuai m) {
        if (!(m.level() instanceof ServerLevel sl) || !m.isAlive()) return;
        LivingEntity t = m.getTarget();
        if (t == null || !t.isAlive() || t.distanceToSqr(m) > 20 * 20) return;
        int phase = (m.tickCount % CD);
        int which = (m.tickCount / CD) % (m.kind() == CreatureKind.VAN_NIEN_THU_VUONG ? 3 : 2);
        if (phase == CD - WINDUP) telegraph(sl, m, which); // báo trước
        if (phase == 0 && m.tickCount > 0) {
            switch (m.kind()) {
                case HU_LANG_VUONG -> { if (which == 0) howl(sl, m); else lunge(sl, m, t); }
                case THACH_CU_NHAN -> { if (which == 0) quake(sl, m); else boulders(sl, m); }
                case HUYET_YEU_VUONG -> { if (which == 0) bloodTide(sl, m); else needles(sl, m, t); }
                case VAN_NIEN_THU_VUONG -> { if (which == 0) vanDang(sl, m); else if (which == 1) sprouts(sl, m, t); else leafStorm(sl, m); }
                default -> { }
            }
        }
        if (m.getHealth() < m.getMaxHealth() * 0.5f && !m.getPersistentData().getBoolean("hk_enraged")) { // nộ ở 50% máu
            m.getPersistentData().putBoolean("hk_enraged", true);
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 1, false, true));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0, false, true));
            Fx.play(sl, FxType.SHOCKWAVE, m.position().add(0, 0.5, 0), Vec3.ZERO, 6f, 0xFF3020, 16);
            sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.6f);
            for (Player p : players(sl, m, 24)) p.displayClientMessage(Component.translatable("hukhong.miniboss.enraged", m.getDisplayName()).withColor(0xFF4030), true);
        }
    }

    private static List<Player> players(ServerLevel sl, LivingEntity m, double r) {
        return sl.getEntitiesOfClass(Player.class, m.getBoundingBox().inflate(r), p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(m) <= r * r);
    }

    private static float atk(LinhQuai m) {
        var a = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        return a == null ? 6f : (float) a.getValue();
    }

    private static void telegraph(ServerLevel sl, LinhQuai m, int which) {
        float r = switch (m.kind()) { case THACH_CU_NHAN -> which == 0 ? 7f : 3f; case HUYET_YEU_VUONG -> which == 0 ? 9f : 4f; case VAN_NIEN_THU_VUONG -> (which == 0 ? 10f : which == 1 ? 5f : 8f) * YeuThuSize.skillK(m); default -> which == 0 ? 4f : 6f; };
        Fx.telegraphCircle(sl, null, m.position().add(0, 0.05, 0), r, WINDUP, 0xFF3020);
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.2f, 0.6f);
    }

    // ---------------------------------------------------------------- Hư Lang Vương

    private static void howl(ServerLevel sl, LinhQuai m) {
        int n = 0;
        for (int i = 0; i < 3; i++) {
            var w = CreatureRegistry.type(CreatureKind.HU_LANG).create(sl);
            if (!(w instanceof LinhQuai q)) continue;
            double a = Math.PI * 2 * i / 3;
            q.moveTo(m.getX() + Math.cos(a) * 2.5, m.getY(), m.getZ() + Math.sin(a) * 2.5, m.getYRot(), 0);
            q.finalizeSpawn(sl, sl.getCurrentDifficultyAt(q.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            q.setTarget(m.getTarget());
            sl.addFreshEntity(q);
            sl.sendParticles(ParticleTypes.PORTAL, q.getX(), q.getY() + 0.5, q.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
            n++;
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LinhQuai.class, m.getBoundingBox().inflate(12), q -> q.kind() == CreatureKind.HU_LANG))
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
        Fx.play(sl, FxType.SHOCKWAVE, m.position().add(0, 1.5, 0), Vec3.ZERO, 8f, 0x8A60FF, 20);
        Fx.groundRing(sl, null, m.position().add(0, 0.06, 0), 6f, 0x8A60FF, 20);
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 3f, 0.5f);
    }

    private static void lunge(ServerLevel sl, LinhQuai m, LivingEntity t) {
        Vec3 d = t.position().subtract(m.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) return;
        Vec3 f = d.normalize();
        Vec3 from = m.position();
        m.setDeltaMovement(f.x * 2.2, 0.35, f.z * 2.2);
        m.hurtMarked = true;
        for (Player p : players(sl, m, 7)) {
            Vec3 to = p.position().subtract(from).multiply(1, 0, 1);
            if (to.lengthSqr() > 1e-4 && to.normalize().dot(f) > 0.3) {
                p.hurt(m.damageSources().mobAttack(m), atk(m) * 1.6f);
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
        }
        for (int i = 0; i < 3; i++) {
            Vec3 c = from.add(f.scale(2 + i * 1.5)).add(0, 1, 0);
            Fx.qi(sl, c, f, 2.2f, 0xA070FF, i % 2 == 0 ? 30 : -30, 8);
        }
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 2f, 0.6f);
    }

    // ---------------------------------------------------------------- Vạn Niên Thụ Vương (2026-11-06)

    /** Vạn Đằng Triền: rễ trồi khắp bán kính 10, trói + sát thương, Thụ Vương hồi 5% máu mỗi người trúng. */
    private static void vanDang(ServerLevel sl, LinhQuai m) {
        Vec3 c = m.position();
        float k = YeuThuSize.skillK(m);
        for (int i = 0; i < 18 * k; i++) { // rừng rễ nhọn trồi khắp vùng
            double a = sl.random.nextDouble() * Math.PI * 2, r = 2 + sl.random.nextDouble() * 8 * k;
            double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
            int hgt = 4 + sl.random.nextInt((int) (4 * k) + 1);
            for (int y = 0; y < hgt; y++)
                sl.sendParticles(new DustParticleOptions(y < hgt - 1 ? new Vector3f(.42f, .29f, .16f) : new Vector3f(.56f, .83f, .35f), 1.8f), x, c.y + y * 0.35, z, 2, 0.08, 0, 0.08, 0);
        }
        Fx.shake(sl, c, 1.4f, 20, 30 * k);
        for (Player p : players(sl, m, 10 * k)) {
            p.hurt(m.damageSources().mobAttack(m), atk(m) * 1.2f);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            p.displayClientMessage(Component.translatable("hukhong.thu_vuong.roots").withColor(0x8FD45A), true);
            m.heal(m.getMaxHealth() * 0.05f);
        }
        Fx.groundCrack(sl, null, c.add(0, 0.06, 0), 10f * k, 0x6B4A2A, 16, 60);
        com.hukhong.world.TreeFell.fellRadius(sl, c, 8 * k, 8, 10); // rừng rễ trồi lên quật đổ cây
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.ROOTED_DIRT.defaultBlockState()),
                c.x, c.y + 0.3, c.z, 160, 5, 0.3, 5, 0.3);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2.5f, 0.4f);
    }

    /** Gọi mầm yêu: 2 Thực Nhân Hoa / Đằng Xà trẻ (10-100 năm) quanh mục tiêu. */
    private static void sprouts(ServerLevel sl, LinhQuai m, LivingEntity t) {
        for (int i = 0; i < 2; i++) {
            CreatureKind k = i == 0 ? CreatureKind.THUC_NHAN_HOA : CreatureKind.DANG_XA;
            if (!(CreatureRegistry.type(k).create(sl) instanceof LinhQuai s)) continue;
            double a = sl.random.nextDouble() * Math.PI * 2;
            s.moveTo(t.getX() + Math.cos(a) * 3, t.getY(), t.getZ() + Math.sin(a) * 3, sl.random.nextFloat() * 360f, 0);
            s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(s.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            s.setAge(sl.random.nextBoolean() ? YeuThuAge.Y10 : YeuThuAge.Y100);
            s.setTarget(t);
            sl.addFreshEntity(s);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 0.5, s.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }
        for (Player p : players(sl, m, 24)) p.displayClientMessage(Component.translatable("hukhong.thu_vuong.sprout").withColor(0x8FD45A), true);
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.BIG_DRIPLEAF_TILT_DOWN, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** Lá Kiếm Phong Bạo: lốc lá sắc xoáy ra từ tán cây, cắt người trong bán kính 8 x thân, hất lên. */
    private static void leafStorm(ServerLevel sl, LinhQuai m) {
        float k = YeuThuSize.skillK(m);
        Vec3 c = m.position();
        double h = m.getBbHeight();
        for (int ring = 0; ring < 4; ring++) {
            double r = (2 + ring * 2) * k, y = c.y + 1 + ring * h * 0.15;
            int n = (int) (24 + ring * 10);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n + ring * 0.4;
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x + Math.cos(a) * r, y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                sl.sendParticles(new DustParticleOptions(new Vector3f(.35f, .8f, .3f), 1.3f), c.x + Math.cos(a) * r, y + 0.3, c.z + Math.sin(a) * r,
                        1, -Math.sin(a) * 0.5, 0.05, Math.cos(a) * 0.5, 0.4);
            }
        }
        Fx.play(sl, FxType.SHOCKWAVE, c.add(0, 1, 0), Vec3.ZERO, 8f * k, 0x6FD45A, 18);
        for (Player p : players(sl, m, 8 * k)) {
            p.hurt(m.damageSources().mobAttack(m), atk(m) * 1.0f);
            Vec3 out = p.position().subtract(c).multiply(1, 0, 1).normalize().scale(1.2);
            p.setDeltaMovement(out.x, 0.7, out.z);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 2f, 0.6f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 2.5f, 0.5f);
    }

    // ---------------------------------------------------------------- Thạch Cự Nhân

    private static void quake(ServerLevel sl, LinhQuai m) {
        Vec3 c = m.position();
        for (Player p : players(sl, m, 7)) {
            p.hurt(m.damageSources().mobAttack(m), atk(m) * 1.4f);
            p.setDeltaMovement(p.getDeltaMovement().add(0, 0.8, 0));
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }
        Fx.groundCrack(sl, null, c.add(0, 0.06, 0), 7.5f, 0x8A7A60, 12, 50);
        com.hukhong.world.TreeFell.fellRadius(sl, c, 7, 6, 6); // động đất làm đổ cây
        Fx.play(sl, FxType.SHOCKWAVE, c.add(0, 0.3, 0), Vec3.ZERO, 7f, 0xC0A070, 16);
        Fx.shake(sl, c, 1.2f, 16, 24f);
        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                c.x, c.y + 0.3, c.z, 120, 3.5, 0.3, 3.5, 0.3);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.5f);
    }

    private static void boulders(ServerLevel sl, LinhQuai m) {
        List<Player> ps = players(sl, m, 18);
        for (int i = 0; i < Math.min(3, ps.size()); i++) {
            Player p = ps.get(i);
            Vec3 from = m.position().add(0, m.getBbHeight() * 0.9, 0), to = p.position();
            int flight = Math.max(6, (int) (from.distanceTo(to) / 1.2));
            Fx.elem(sl, null, Fx.E_SHOT, from, to.subtract(from), 3.0f, 0x8A7A60, flight);
            Fx.telegraphCircle(sl, null, to.add(0, 0.05, 0), 2.5f, flight, 0xFF3020);
            com.hukhong.skill.SkillScheduler.later(flight, () -> {
                for (Player q : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(to, to).inflate(2.5), x -> x.isAlive())) {
                    q.hurt(m.damageSources().mobAttack(m), atk(m) * 1.8f);
                    q.setDeltaMovement(q.getDeltaMovement().add(0, 0.5, 0));
                    q.hurtMarked = true;
                }
                Fx.groundCrack(sl, null, to.add(0, 0.06, 0), 2.6f, 0x8A7A60, 6, 30);
                sl.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y + 0.5, to.z, 1, 0, 0, 0, 0);
                sl.playSound(null, to.x, to.y, to.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
            });
        }
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 2f, 0.5f);
    }

    // ---------------------------------------------------------------- Huyết Yêu Vương

    private static void bloodTide(ServerLevel sl, LinhQuai m) {
        Vec3 c = m.position();
        float drained = 0;
        for (Player p : players(sl, m, 9)) {
            Vec3 in = c.subtract(p.position()).multiply(1, 0, 1);
            if (in.lengthSqr() > 1e-4) in = in.normalize();
            p.setDeltaMovement(in.x * 1.1, 0.2, in.z * 1.1);
            p.hurtMarked = true;
            float d = atk(m) * 1.2f;
            p.hurt(m.damageSources().mobAttack(m), d);
            drained += d;
            Fx.ribbon(sl, null, p.getBoundingBox().getCenter(), m.getBoundingBox().getCenter(), 0.4, 6, 0, 0.14f, 0xC01028);
        }
        m.heal(drained * 0.5f);
        Fx.play(sl, FxType.BLOOD_VORTEX, c, Vec3.ZERO, 9f, 0xC01028, 30);
        Fx.pool(sl, null, c.add(0, 0.05, 0), 8f, 0x3A0008, 60);
        sl.sendParticles(BLOOD, c.x, c.y + 1, c.z, 120, 4, 0.5, 4, 0.1);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 2f, 0.6f);
    }

    private static void needles(ServerLevel sl, LinhQuai m, LivingEntity t) {
        Vec3 c = t.position();
        for (int i = 0; i < 10; i++) {
            double a = sl.random.nextDouble() * Math.PI * 2, r = Math.sqrt(sl.random.nextDouble()) * 4;
            Vec3 at = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            Fx.ribbon(sl, null, at.add(0, 8, 0), at, 0, 5, i, 0.12f, 0xE02030);
        }
        com.hukhong.skill.SkillScheduler.later(8, () -> {
            for (Player p : sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(4.5, 3, 4.5), x -> x.isAlive())) {
                p.hurt(m.damageSources().mobAttack(m), atk(m) * 1.5f);
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            }
            sl.sendParticles(BLOOD, c.x, c.y + 0.5, c.z, 60, 3, 0.3, 3, 0.1);
        });
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 2f, 0.6f);
    }
}
