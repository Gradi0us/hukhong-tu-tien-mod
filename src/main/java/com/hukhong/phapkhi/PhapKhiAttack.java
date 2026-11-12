package com.hukhong.phapkhi;

import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import com.hukhong.vfx.MaClawFx;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Phù Công: 7 nguyên tố x {Toàn Lực, Kỹ Năng}. Sát thương qua DamageRules.hurtBySkill (đường kỹ năng chuẩn) =
 * hệ số x max(Công lực, Phép lực) của người dùng. Kỹ Năng là bản viết riêng cho phù (không phụ thuộc đạo, không dùng
 * Fx.slash vì nó vẽ Kiếm Bản Mệnh của chủ), dùng cùng bộ VFX Fx/MaClawFx với các đạo.
 */
final class PhapKhiAttack {
    private PhapKhiAttack() {}

    static final double RANGE = 24;

    static double power(ServerPlayer p) {
        return Math.max(1.0, Math.max(StatService.attackPower(p), StatService.spellPower(p)));
    }

    static boolean valid(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.npc.Npc)
                && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p));
    }

    static void hit(ServerPlayer p, LivingEntity e, double coef, boolean crit) {
        float dmg = (float) (coef * power(p));
        e.invulnerableTime = 0;
        if (crit) DamageRules.hurtBySkillCrit(p, e, dmg);
        else DamageRules.hurtBySkill(p, e, dmg);
    }

    static List<LivingEntity> around(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r + 2, 3, r + 2))) {
            if (!valid(p, e)) continue;
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            double reach = r + e.getBbWidth() / 2;
            if (dx * dx + dz * dz <= reach * reach) out.add(e);
        }
        return out;
    }

    /** Mục tiêu nằm trên đường nhìn from -> from+dir*range, rộng width. */
    static List<LivingEntity> line(ServerLevel lv, ServerPlayer p, Vec3 from, Vec3 dir, double range, double width) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 d = dir.normalize();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range + 2, 3, range + 2))) {
            if (!valid(p, e)) continue;
            Vec3 c = e.getBoundingBox().getCenter().subtract(from);
            double t = c.dot(d);
            if (t < -0.5 || t > range + 0.5) continue;
            if (c.subtract(d.scale(t)).length() <= width + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** Điểm ngắm: thực thể đầu tiên trên tia nhìn, hoặc điểm chạm khối, rồi hạ xuống mặt đất. */
    static Vec3 aim(ServerLevel lv, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle(), end = eye.add(look.scale(range));
        var hit = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        double best = eye.distanceToSqr(stop);
        LivingEntity pick = null;
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().expandTowards(look.scale(range)).inflate(2))) {
            if (!valid(p, e)) continue;
            var c = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (c.isPresent() && eye.distanceToSqr(c.get()) < best) { best = eye.distanceToSqr(c.get()); pick = e; }
        }
        if (pick != null) return pick.position();
        return ground(lv, stop);
    }

    static Vec3 ground(ServerLevel lv, Vec3 v) {
        var r = lv.clip(new ClipContext(v.add(0, 0.5, 0), v.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return r.getType() == HitResult.Type.MISS ? v : r.getLocation();
    }

    static Vec3 hand(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        return p.getEyePosition().subtract(0, 0.35, 0).add(look.scale(0.8));
    }

    static Vec3 flatLook(ServerPlayer p) {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    static void sfx(ServerLevel lv, Vec3 at, SoundEvent s, float vol, float pitch) {
        lv.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    static void puff(ServerLevel lv, ParticleOptions o, Vec3 at, int n, double spread, double speed) {
        lv.sendParticles(o, at.x, at.y + 0.3, at.z, n, spread, spread * 0.6, spread, speed);
    }

    /** Chạy sau {@code ticks}; bỏ qua nếu người dùng đã mất. */
    static void later(ServerPlayer p, int ticks, Runnable r) {
        if (ticks <= 0) { r.run(); return; }
        SkillScheduler.later(ticks, () -> { if (p.isAlive() && !p.hasDisconnected()) r.run(); });
    }

    // ------------------------------------------------------------------ entry

    /** qm = hệ số phẩm chế (CraftQuality.mult), nhân vào mọi hệ số sát thương. */
    static boolean fire(ServerPlayer p, PhapKhiKind kind, PhapKhiTier tier, boolean skillMode, double qm) {
        if (!(p.level() instanceof ServerLevel lv)) return false;
        Vec3 at = aim(lv, p, RANGE);
        if (skillMode) skill(lv, p, kind, tier, at, qm); else full(lv, p, kind, tier, at, qm);
        return true;
    }

    // ------------------------------------------------------------------ Toàn Lực

    private static void full(ServerLevel lv, ServerPlayer p, PhapKhiKind kind, PhapKhiTier tier, Vec3 at, double qm) {
        int t = tier.ordinal();
        double r = PhapKhiTier.FULL_RADIUS[t];
        double coef = PhapKhiTier.FULL_COEF[t] * qm;
        List<LivingEntity> targets = around(lv, p, at, r);
        for (LivingEntity e : targets) hit(p, e, coef, kind == PhapKhiKind.MA);
        Vec3 g = at.add(0, 0.06, 0);
        int rgb = kind.rgb;
        switch (kind) {
            case KIEM -> {
                Fx.giantSword(lv, p, at, 1.0f + 0.35f * t, 0xBFE4FF, 8, 18);
                for (int i = 0; i < 3 + t; i++) {
                    double a = Math.PI * 2 * i / (3 + t);
                    Vec3 end = at.add(Math.cos(a) * r * 0.7, 0.4, Math.sin(a) * r * 0.7);
                    Fx.ribbon(lv, p, end.add(0, 11, 0), end, 0, 4, i, 0.18f, i % 2 == 0 ? 0xFFFFFF : 0xBFE4FF);
                }
                Fx.groundCrack(lv, p, g, (float) r, 0xBFE4FF, 8, 34);
                Fx.groundRing(lv, p, g, (float) r * 1.1f, 0xFFFFFF, 20);
                Fx.rune(lv, p, g, (float) r * 0.8f, 0xBFE4FF, 40, 5);
                Fx.shake(lv, at, 0.6f + 0.1f * t, 8, 20);
                sfx(lv, at, SoundEvents.ANVIL_LAND, 0.7f, 1.5f);
                sfx(lv, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.7f);
                puff(lv, ParticleTypes.END_ROD, at, 40, r * 0.5, 0.25);
                puff(lv, ParticleTypes.CRIT, at, 30, r * 0.6, 0.6);
            }
            case HOA -> {
                Vec3 from = hand(p);
                Fx.ribbon(lv, p, from, at.add(0, 0.8, 0), 1.5, 4, 0, 0.35f + 0.05f * t, 0xFF7A20);
                Fx.ribbon(lv, p, from, at.add(0, 0.8, 0), 1.2, 4, 0, 0.2f, 0xFFD060);
                sfx(lv, p.position(), SoundEvents.BLAZE_SHOOT, 0.9f, 0.8f);
                for (LivingEntity e : targets) e.igniteForSeconds(3 + t);
                later(p, 4, () -> {
                    Fx.elem(lv, p, Fx.E_TONGUES, at, Vec3.ZERO, (float) r, 0xFF6A2A, 30);
                    Fx.pool(lv, p, g, (float) r, 0xFF3A10, 40);
                    Fx.groundRing(lv, p, g, (float) r * 1.2f, 0xFFB040, 20);
                    Fx.rune(lv, p, g, (float) r * 0.9f, 0xFF6A2A, 36, 6);
                    Fx.shake(lv, at, 0.8f, 8, 22);
                    sfx(lv, at, SoundEvents.GENERIC_EXPLODE.value(), 1f, 0.8f);
                    puff(lv, ParticleTypes.FLAME, at, 80, r * 0.5, 0.3);
                    puff(lv, ParticleTypes.LAVA, at, 20, r * 0.4, 0.1);
                    puff(lv, ParticleTypes.EXPLOSION, at, 3 + t, r * 0.5, 0);
                });
            }
            case LOI -> {
                for (int i = 0; i <= t; i++) {
                    Vec3 o = i == 0 ? at : at.add((lv.random.nextDouble() - 0.5) * r * 1.4, 0, (lv.random.nextDouble() - 0.5) * r * 1.4);
                    Fx.play(lv, FxType.LIGHTNING_BOLT, o, Vec3.ZERO, 1f + 0.2f * t, 0xBFD8FF);
                    Fx.ribbonWave(lv, p, o.add(0, 18, 0), o, 0, 0.6, 3, i * 2, 0.3f, 0xE8F0FF);
                }
                Fx.elem(lv, p, Fx.E_ARC, at.add(0, 0.9, 0), Vec3.ZERO, (float) r, 0x9CC4FF, 10);
                Fx.groundRing(lv, p, g, (float) r * 1.1f, 0x9CC4FF, 20);
                Fx.rune(lv, p, g, (float) r * 0.8f, 0xE8F0FF, 30, 4);
                Fx.shake(lv, at, 0.7f, 8, 22);
                sfx(lv, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.2f);
                sfx(lv, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 1.4f);
                puff(lv, ParticleTypes.ELECTRIC_SPARK, at, 60, r * 0.5, 0.5);
            }
            case BANG -> {
                Fx.ribbon(lv, p, hand(p), at.add(0, 0.8, 0), 1.0, 4, 0, 0.3f, 0x8FE6FF);
                for (LivingEntity e : targets) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80 + 20 * t, 1));
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), 140));
                }
                later(p, 3, () -> {
                    Fx.elem(lv, p, Fx.E_NEEDLES, at, Vec3.ZERO, (float) r, 0x8FE6FF, 14);
                    Fx.pool(lv, p, g, (float) r, 0xBFF0FF, 44);
                    Fx.groundRing(lv, p, g, (float) r * 1.2f, 0xFFFFFF, 20);
                    Fx.rune(lv, p, g, (float) r * 0.9f, 0x8FE6FF, 40, 6);
                    sfx(lv, at, SoundEvents.GLASS_BREAK, 1f, 0.7f);
                    sfx(lv, at, SoundEvents.POWDER_SNOW_BREAK, 1f, 0.8f);
                    puff(lv, ParticleTypes.SNOWFLAKE, at, 100, r * 0.5, 0.2);
                    puff(lv, ParticleTypes.CLOUD, at, 20, r * 0.4, 0.05);
                });
            }
            case THO -> {
                Fx.elem(lv, p, Fx.E_SPIKES, at, Vec3.ZERO, (float) r, 0xB08A4F, 34);
                if (t >= 2) Fx.elem(lv, p, Fx.E_BOULDER, at, Vec3.ZERO, (float) r * 0.6f, 0xB08A4F, 30);
                Fx.groundCrack(lv, p, g, (float) r * 1.2f, 0xB08A4F, 8, 40);
                Fx.groundRing(lv, p, g, (float) r, 0xD8B070, 20);
                Fx.shake(lv, at, 1.0f, 10, 24);
                for (LivingEntity e : targets) { e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0)); e.hurtMarked = true; }
                sfx(lv, at, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.5f);
                sfx(lv, at, SoundEvents.STONE_BREAK, 1f, 0.6f);
                puff(lv, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), at, 80, r * 0.5, 0.4);
                puff(lv, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at, 60, r * 0.5, 0.5);
            }
            case MA -> {
                Fx.ribbonWave(lv, p, hand(p), at.add(0, 0.8, 0), 0.8, 0.7, 5, 0, 0.3f, 0xC01028);
                Fx.play(lv, FxType.BLOOD_VORTEX, at, Vec3.ZERO, 1.2f + 0.3f * t, Fx.RED, 40);
                Fx.pool(lv, p, g, (float) r * 1.1f, 0x7A0A28, 50);
                Fx.rune(lv, p, g, (float) r, 0xB01030, 50, 5);
                Fx.groundCrack(lv, p, g, (float) r, 0xC01028, 6, 36);
                for (LivingEntity e : targets) MaClawFx.burst(lv, e.position().add(0, e.getBbHeight() * 0.5, 0));
                sfx(lv, at, SoundEvents.WITHER_SHOOT, 0.7f, 0.6f);
                sfx(lv, at, SoundEvents.RAVAGER_ATTACK, 0.9f, 0.6f);
                puff(lv, ParticleTypes.SOUL, at, 40, r * 0.4, 0.15);
                puff(lv, ParticleTypes.SOUL_FIRE_FLAME, at, 40, r * 0.4, 0.15);
            }
            case QUANG -> {
                pillar(lv, p, at, 0.55f + 0.08f * t, 0);
                int n = 2 + t;
                for (int i = 0; i < n; i++) {
                    double a = Math.PI * 2 * i / n;
                    pillar(lv, p, at.add(Math.cos(a) * r * 0.75, 0, Math.sin(a) * r * 0.75), 0.28f, 2 + i);
                }
                Fx.elem(lv, p, Fx.E_HALOS, at, Vec3.ZERO, (float) r, 0xFFE066, 16);
                Fx.rune(lv, p, g, (float) r, 0xFFE066, 44, 6);
                Fx.groundRing(lv, p, g, (float) r * 1.2f, 0xFFFFFF, 22);
                Fx.shake(lv, at, 0.5f, 8, 20);
                sfx(lv, at, SoundEvents.BEACON_POWER_SELECT, 1f, 1.4f);
                sfx(lv, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 1.2f);
                for (double y = 0; y <= 12; y += 0.5) lv.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 2, 0.15, 0, 0.15, 0.01);
            }
            default -> { }
        }
    }

    /** Cột sáng dựng từ ba dải Fx.ribbon chồng nhau (vàng / nhạt / trắng), cao ~22 block. */
    static void pillar(ServerLevel lv, ServerPlayer p, Vec3 at, float width, int delay) {
        Vec3 top = at.add(0, 22, 0);
        Fx.ribbon(lv, p, top, at, 0, 6, delay, width, 0xFFD866);
        Fx.ribbon(lv, p, top, at, 0, 6, delay, width * 0.62f, 0xFFF3B0);
        Fx.ribbon(lv, p, top, at, 0, 6, delay, width * 0.3f, 0xFFFFFF);
    }

    // ------------------------------------------------------------------ Kỹ Năng

    private static void skill(ServerLevel lv, ServerPlayer p, PhapKhiKind kind, PhapKhiTier tier, Vec3 at, double qm) {
        int t = tier.ordinal();
        double c = PhapKhiTier.SKILL_COEF[t] * qm;
        Vec3 look = p.getLookAngle();
        Vec3 flat = flatLook(p);
        Vec3 eyeLow = p.getEyePosition().subtract(0, 0.5, 0);
        switch (kind) {
            case KIEM -> { // Trảm Khí: các đợt kiếm khí lưỡi liềm xuyên hàng
                double len = 8 + 1.5 * t;
                int waves = 3;
                Vec3 from = eyeLow.add(look.scale(1.2));
                double[] rolls = {35, -30, 90};
                for (int i = 0; i < waves; i++) {
                    final int k = i;
                    later(p, i * 3, () -> {
                        Fx.qi(lv, from, look.scale(len), 1.6f + 0.2f * t, 0xBFE4FF, rolls[k], 11);
                        Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale((k - 1) * 0.5);
                        Fx.ribbon(lv, p, from.add(side), from.add(side).add(look.scale(len)), 0, 4, 0, 0.14f, k == 2 ? 0xFFFFFF : 0xBFE4FF);
                        sfx(lv, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f + 0.2f * k);
                        for (LivingEntity e : line(lv, p, from, look, len, 1.3)) hit(p, e, c / waves, false);
                    });
                }
            }
            case HOA -> { // Liệt Diễm Hoàn: ba quả cầu lửa nổ liên tiếp
                for (int i = 0; i < 3; i++) {
                    double a = Math.PI * 2 * i / 3 + 0.5;
                    Vec3 o = i == 0 ? at : ground(lv, at.add(Math.cos(a) * 2.2, 0, Math.sin(a) * 2.2));
                    later(p, i * 3, () -> {
                        Fx.ribbon(lv, p, hand(p), o.add(0, 0.7, 0), 1.3, 4, 0, 0.28f, 0xFF7A20);
                        sfx(lv, p.position(), SoundEvents.FIRECHARGE_USE, 0.8f, 1f);
                        later(p, 4, () -> {
                            Fx.elem(lv, p, Fx.E_TONGUES, o, Vec3.ZERO, 2.4f, 0xFF6A2A, 26);
                            Fx.pool(lv, p, o.add(0, 0.05, 0), 2.4f, 0xFF3A10, 30);
                            Fx.groundRing(lv, p, o.add(0, 0.06, 0), 2.8f, 0xFFB040, 16);
                            puff(lv, ParticleTypes.FLAME, o, 40, 1.1, 0.25);
                            puff(lv, ParticleTypes.EXPLOSION, o, 1, 0.3, 0);
                            sfx(lv, o, SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.1f);
                            for (LivingEntity e : around(lv, p, o, 2.4)) {
                                hit(p, e, c / 3, false);
                                e.igniteForSeconds(2 + t);
                            }
                        });
                    });
                }
            }
            case LOI -> { // Thiên Lôi Liên: sét nhảy chuỗi
                List<LivingEntity> chain = new ArrayList<>();
                LivingEntity first = null;
                for (LivingEntity e : around(lv, p, at, 5)) if (first == null || e.distanceToSqr(at) < first.distanceToSqr(at)) first = e;
                if (first != null) {
                    chain.add(first);
                    LivingEntity cur = first;
                    while (chain.size() < 3 + t) {
                        LivingEntity next = null;
                        for (LivingEntity e : around(lv, p, cur.position(), 7))
                            if (!chain.contains(e) && (next == null || e.distanceToSqr(cur) < next.distanceToSqr(cur))) next = e;
                        if (next == null) break;
                        chain.add(next);
                        cur = next;
                    }
                }
                Fx.play(lv, FxType.LIGHTNING_BOLT, at, Vec3.ZERO, 1.1f, 0xBFD8FF);
                Fx.ribbonWave(lv, p, at.add(0, 18, 0), at, 0, 0.6, 3, 0, 0.3f, 0xE8F0FF);
                Vec3 prev = hand(p);
                for (int i = 0; i < chain.size(); i++) {
                    LivingEntity e = chain.get(i);
                    Vec3 b = e.getBoundingBox().getCenter();
                    Fx.elem(lv, p, Fx.E_ARC, prev, b.subtract(prev), 1.1f, 0x9CC4FF, 8);
                    Fx.ribbonWave(lv, p, prev, b, 0, 0.5, 3, i, 0.14f, 0xE8F0FF);
                    puff(lv, ParticleTypes.ELECTRIC_SPARK, b, 25, 0.5, 0.4);
                    hit(p, e, c, false);
                    prev = b;
                }
                sfx(lv, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.5f);
                sfx(lv, at, SoundEvents.TRIDENT_THUNDER.value(), 0.7f, 1.4f);
            }
            case BANG -> { // Băng Triều: sóng băng quét thẳng, đóng băng
                double len = 9 + t;
                Vec3 from = p.position().add(flat.scale(1.0)).add(0, 0.2, 0);
                Vec3 v = flat.scale(len);
                Fx.elem(lv, p, Fx.E_TIDE, from, v, 1.8f, 0x8FE6FF, 18);
                Fx.elem(lv, p, Fx.E_NEEDLES, from.add(0, 0.5, 0), v, 1.4f, 0xFFFFFF, 12);
                Fx.ribbon(lv, p, from.add(0, 0.5, 0), from.add(v).add(0, 0.5, 0), 0, 5, 0, 0.5f, 0xBFF0FF);
                for (double d = 1; d < len; d += 1.0) puff(lv, ParticleTypes.SNOWFLAKE, from.add(flat.scale(d)), 6, 0.6, 0.05);
                sfx(lv, p.position(), SoundEvents.GLASS_BREAK, 0.9f, 0.9f);
                sfx(lv, p.position(), SoundEvents.PLAYER_HURT_FREEZE, 1f, 1.1f);
                for (LivingEntity e : line(lv, p, from, flat, len, 1.8)) {
                    hit(p, e, c, false);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160));
                }
            }
            case THO -> { // Địa Thích: gai đá trồi dọc đường
                double len = 9 + t;
                Vec3 from = ground(lv, p.position().add(flat.scale(1.2)));
                Vec3 v = flat.scale(len);
                Fx.elem(lv, p, Fx.E_SPIKES, from, v, 1.6f, 0xB08A4F, 34);
                Fx.groundCrack(lv, p, ground(lv, from.add(v.scale(0.5))).add(0, 0.05, 0), (float) (len * 0.45), 0xB08A4F, 4, 36);
                Fx.shake(lv, from, 0.7f, 8, 20);
                sfx(lv, from, SoundEvents.STONE_BREAK, 1f, 0.6f);
                sfx(lv, from, SoundEvents.GRAVEL_BREAK, 1f, 0.6f);
                for (double d = 1; d < len; d += 1.2)
                    puff(lv, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), from.add(flat.scale(d)), 12, 0.5, 0.3);
                for (LivingEntity e : line(lv, p, from, flat, len, 1.5)) {
                    hit(p, e, c, false);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                }
            }
            case MA -> huyetTrao(lv, p, c);
            case QUANG -> { // Thánh Quang Vũ: mưa cột sáng
                int n = 4 + t;
                double R = 2.5 + 0.3 * t;
                Fx.elem(lv, p, Fx.E_HALOS, at, Vec3.ZERO, (float) R, 0xFFE066, 16);
                Fx.rune(lv, p, at.add(0, 0.06, 0), (float) R, 0xFFE066, 30, 6);
                for (int i = 0; i < n; i++) {
                    double a = lv.random.nextDouble() * Math.PI * 2, d = R * Math.sqrt(lv.random.nextDouble());
                    Vec3 o = ground(lv, at.add(Math.cos(a) * d, 0, Math.sin(a) * d));
                    later(p, i * 3, () -> {
                        pillar(lv, p, o, 0.3f, 0);
                        puff(lv, ParticleTypes.END_ROD, o, 14, 0.5, 0.3);
                        sfx(lv, o, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.3f);
                        for (LivingEntity e : around(lv, p, o, 1.8)) hit(p, e, c / 2, false);
                    });
                }
                sfx(lv, at, SoundEvents.BEACON_POWER_SELECT, 0.9f, 1.5f);
            }
            default -> { }
        }
    }

    /** Huyết Trảo bản phù: kéo địch quạt 8 block về sát người, trói ngắn, 3 nhát móng (nhát 3 bạo kích). */
    private static void huyetTrao(ServerLevel lv, ServerPlayer p, double c) {
        Vec3 look = p.getLookAngle();
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8, 1.5, 8))) {
            if (!valid(p, e)) continue;
            Vec3 d = e.getBoundingBox().getCenter().subtract(p.getEyePosition());
            double len = d.length();
            if (len <= 8 + e.getBbWidth() / 2 && (len < 0.8 || d.normalize().dot(look) >= 0.5)) targets.add(e);
        }
        for (LivingEntity e : targets) {
            Vec3 from = e.position();
            Vec3 d = new Vec3(from.x - p.getX(), 0, from.z - p.getZ());
            if (d.lengthSqr() > 1e-4) {
                d = d.normalize();
                e.setPos(p.getX() + d.x * 1.8, e.getY(), p.getZ() + d.z * 1.8);
                e.setDeltaMovement(Vec3.ZERO);
                e.hurtMarked = true;
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6));
        }
        for (int i = 0; i < 3; i++) {
            final int hitNo = i;
            later(p, i * 6, () -> {
                boolean finisher = hitNo == 2;
                for (LivingEntity e : targets) {
                    if (!e.isAlive() || e.isRemoved()) continue;
                    hit(p, e, c / 3, finisher);
                    MaClawFx.burst(lv, e.position().add(0, e.getBbHeight() * 0.5, 0));
                    if (hitNo == 0) MaClawFx.qPull(lv, p, e); else MaClawFx.qSlash(lv, p, e, hitNo == 1, finisher);
                }
                Vec3 front = p.position().add(flatLook(p).scale(2.2)).add(0, 1.0, 0);
                if (hitNo == 0) MaClawFx.qPullAt(lv, p, front); else MaClawFx.qSlashAt(lv, p, front, hitNo == 1, finisher);
                sfx(lv, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, finisher ? 0.5f : 0.7f);
            });
        }
        Fx.rune(lv, p, p.position().add(0, 0.06, 0), 3f, 0xB01030, 30, 5);
        sfx(lv, p.position(), SoundEvents.RAVAGER_ATTACK, 0.6f, 1.3f);
    }
}
