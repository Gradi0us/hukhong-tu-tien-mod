package com.hukhong.phaptu;

import com.hukhong.vfx.FxSfx;
import com.hukhong.gates.DamageRules;
import com.hukhong.skill.SkillEffects;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 24 kỹ năng Pháp Tu: 6 nguyên tố x phím Q/E/R/T (slot 0..3). Sát thương mỗi phím = {@link PhapMath#coef} x Phép lực
 * (tổng lên MỘT mục tiêu), đi qua DamageRules.hurtBySkill như mọi kỹ năng khác. VFX dùng com.hukhong.vfx.Fx.
 */
public final class PhapSkills {
    private PhapSkills() {}

    private static final BlockParticleOption STONE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    private static final BlockParticleOption DIRT = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
    private static final BlockParticleOption IRON = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState());
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.88f, 0.4f), 1.1f);
    private static final DustParticleOptions SILVER = new DustParticleOptions(new Vector3f(0.82f, 0.86f, 0.92f), 1.0f);
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.25f, 0.65f, 1f), 1.1f);

    /** Phím Q/E/R/T (slot 0..3) của Pháp Tu. */
    public static void run(int slot, ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        PhapElement el = PhapTu.element(p);
        if (el == null) return;
        p.displayClientMessage(Component.translatable("hukhong.phap.cast." + el.id() + "." + slot).withColor(el.rgb), true);
        double c = PhapMath.coef(el, slot) * StatService.spellPower(p);
        Vec3 staffTip = com.hukhong.phaptu.linhtruong.LinhTruongService.tipIfHeld(p);
        if (staffTip != null) { // Linh Trượng: chớp sáng nạp lực ở đầu trượng, kỹ năng phát ra từ đây
            Fx.rune(lv, p, staffTip, 0.8f + 0.12f * slot, el.rgb, 10, 6);
            lv.sendParticles(ParticleTypes.END_ROD, staffTip.x, staffTip.y, staffTip.z, 6, 0.1, 0.1, 0.1, 0.05);
        }
        switch (el) {
            case QUANG -> quang(lv, p, slot, c);
            case LOI -> loi(lv, p, slot, c);
            case HOA -> hoa(lv, p, slot, c);
            case THUY -> thuy(lv, p, slot, c);
            case THO -> tho(lv, p, slot, c);
            case KIM -> kim(lv, p, slot, c);
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void hit(ServerPlayer p, LivingEntity t, double amount) {
        if (amount <= 0) return;
        t.invulnerableTime = 0;
        DamageRules.hurtBySkill(p, t, (float) amount);
    }

    private static Vec3 eye(ServerPlayer p) { return p.getEyePosition().subtract(0, 0.25, 0); }

    /** Gốc hiệu ứng nhìn thấy: đầu Linh Trượng nếu đang cầm, không thì ngang ngực như cũ. Sát thương vẫn tính từ eye(p). */
    private static Vec3 vfx(ServerPlayer p) {
        Vec3 t = com.hukhong.phaptu.linhtruong.LinhTruongService.tipIfHeld(p);
        return t != null ? t : eye(p);
    }

    private static Vec3 flat(ServerPlayer p) {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** Mục tiêu gần tia (origin, dir) dài range, bán kính width; gần nhất trước. */
    private static List<LivingEntity> line(ServerLevel lv, ServerPlayer p, Vec3 origin, Vec3 dir, double range, double width) {
        Vec3 end = origin.add(dir.scale(range));
        AABB box = new AABB(origin, end).inflate(width + 1);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, box, x -> PhapRuntime.valid(p, x))) {
            Vec3 c = e.getBoundingBox().getCenter().subtract(origin);
            double t = c.dot(dir);
            if (t < 0 || t > range + 0.5) continue;
            if (c.subtract(dir.scale(t)).length() <= width + e.getBbWidth() / 2) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
        return out;
    }

    private static List<LivingEntity> around(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        return lv.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, Math.max(2, r * 0.6), r),
                e -> PhapRuntime.valid(p, e) && e.position().distanceToSqr(c) <= (r + e.getBbWidth()) * (r + e.getBbWidth()));
    }

    private static List<LivingEntity> cone(ServerLevel lv, ServerPlayer p, double range, double cosHalf) {
        Vec3 look = p.getLookAngle(), eye = p.getEyePosition();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range, 2, range), x -> PhapRuntime.valid(p, x))) {
            Vec3 d = e.getBoundingBox().getCenter().subtract(eye);
            double l = d.length();
            if (l <= range + e.getBbWidth() / 2 && (l < 0.8 || d.normalize().dot(look) >= cosHalf)) out.add(e);
        }
        return out;
    }

    /** Điểm ngắm: mục tiêu đầu tiên trên tia nhìn, nếu không có thì điểm chạm khối/đầu tầm. */
    private static Vec3 aim(ServerLevel lv, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> l = line(lv, p, eye, look, range, 0.8);
        if (!l.isEmpty()) return l.get(0).position();
        Vec3 end = eye.add(look.scale(range));
        var r = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return r.getType() == HitResult.Type.MISS ? ground(lv, end) : r.getLocation();
    }

    private static Vec3 ground(ServerLevel lv, Vec3 v) {
        var r = lv.clip(new ClipContext(v, v.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
        return r.getType() == HitResult.Type.MISS ? v : r.getLocation();
    }

    private static void sound(ServerLevel lv, Vec3 at, SoundEvent s, float vol, float pitch) {
        lv.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void streak(ServerLevel lv, Vec3 from, Vec3 dir, double len, ParticleOptions type, double step) {
        Vec3 n = dir.normalize();
        for (double t = 0; t <= len; t += step) {
            Vec3 q = from.add(n.scale(t));
            lv.sendParticles(type, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    private static void beam(ServerLevel lv, Vec3 from, Vec3 dir, double len, int rgb, int life) {
        Fx.play(lv, FxType.BEAM, from, dir.normalize(), (float) len, rgb, life);
    }

    private static void knock(LivingEntity t, Vec3 from, double power, double up) {
        Vec3 d = t.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        d = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
        t.push(d.x * power, up, d.z * power);
        t.hurtMarked = true;
    }

    // ------------------------------------------------------------------ QUANG: nhanh + xuyên thấu

    private static void quang(ServerLevel lv, ServerPlayer p, int slot, double c) {
        int col = PhapElement.QUANG.rgb;
        Vec3 from = eye(p), look = p.getLookAngle(), vf = vfx(p);
        switch (slot) {
            case 0 -> { // Quang Tiễn: tia xuyên
                for (LivingEntity t : line(lv, p, from, look, 16, 0.6)) hit(p, t, c);
                beam(lv, vf, look, 16, col, 9);
                streak(lv, vf, look, 16, ParticleTypes.END_ROD, 0.8);
                Fx.ribbon(lv, p, vf, vf.add(look.scale(16)), 0, 3, 0, 0.1f, col);
                FxSfx.cast(lv, from, 0.5f, 1.8f, SoundEvents.TRIDENT_THROW.value(), SoundEvents.AMETHYST_BLOCK_CHIME, null);
                sound(lv, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9f, 1.6f);
            }
            case 1 -> ThiemBoService.dash(lv, p); // Thiểm Bộ (bấm nhanh E): lùi + Bá Thể; giữ E = chạy tia sáng (ThiemBoService)
            case 2 -> { // Quang Vũ: 5 tia quạt, mỗi mục tiêu trúng một lần
                Set<LivingEntity> hit = new LinkedHashSet<>();
                for (int i = -2; i <= 2; i++) {
                    Vec3 d = look.yRot((float) Math.toRadians(i * 11));
                    hit.addAll(line(lv, p, from, d, 14, 0.55));
                    beam(lv, vf, d, 14, col, 8);
                    streak(lv, vf, d, 14, GOLD, 1.0);
                    Fx.ribbon(lv, p, vf, vf.add(d.scale(14)), 0, 3, Math.abs(i), 0.07f, col);
                }
                for (LivingEntity t : hit) hit(p, t, c);
                sound(lv, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            }
            default -> { // Thánh Quang Trụ: dồn lực 0,4s rồi bắn tia khổng lồ
                Fx.play(lv, FxType.TALISMAN_CIRCLE, p.position(), Vec3.ZERO, 2.2f, col, 16);
                Fx.rune(lv, p, p.position().add(0, 0.06, 0), 3.2f, col, 24, 8);   // tụ lực
                SkillEffects.spiral(lv, p.position(), 1.8, 0.2, 2.2, 3, 24, ParticleTypes.END_ROD);
                SkillScheduler.later(8, () -> {
                    if (!p.isAlive()) return;
                    Vec3 f2 = eye(p), l2 = p.getLookAngle(), vf2 = vfx(p);
                    for (LivingEntity t : line(lv, p, f2, l2, 24, 1.6)) hit(p, t, c);
                    for (int k = -1; k <= 1; k++) beam(lv, vf2.add(0, k * 0.35, 0), l2, 24, k == 0 ? 0xFFFFFF : col, 12);
                    Vec3 end = f2.add(l2.scale(24));
                    Fx.play(lv, FxType.LIGHT_PILLAR, ground(lv, end), Vec3.ZERO, 0.9f, col, 28);
                    Fx.play(lv, FxType.SCREEN_TINT, p.position(), Vec3.ZERO, 0.25f, col, 8);
                    Fx.ribbon(lv, p, vf2, end, 0, 4, 0, 0.55f, col);
                    Fx.ribbon(lv, p, vf2, end, 0, 5, 0, 0.2f, 0xFFFFFF);
                    Fx.groundRing(lv, p, ground(lv, end).add(0, 0.06, 0), 4f, col, 22);
                    Fx.shake(lv, p.position(), 0.8f, 8, 24f);
                    FxSfx.cast(lv, f2, 0.7f, 1.4f, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundEvents.BEACON_DEACTIVATE);
                    streak(lv, vf2, l2, 24, ParticleTypes.END_ROD, 0.5);
                    sound(lv, f2, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.8f);
                });
            }
        }
    }

    // ------------------------------------------------------------------ LÔI: chuỗi + choáng

    private static void loi(ServerLevel lv, ServerPlayer p, int slot, double c) {
        int col = PhapElement.LOI.rgb;
        switch (slot) {
            case 0 -> { // Lôi Châu: sét đánh + lan 2 mục tiêu
                Vec3 pos = aim(lv, p, 18);
                List<LivingEntity> near = around(lv, p, pos, 1.6);
                LivingEntity first = near.stream().min(Comparator.comparingDouble(e -> e.position().distanceToSqr(pos))).orElse(null);
                Fx.play(lv, FxType.LIGHTNING_BOLT, pos, Vec3.ZERO, 0.8f, col);
                if (first != null) {
                    hit(p, first, c);
                    int n = 0;
                    for (LivingEntity e : around(lv, p, first.position(), 5)) {
                        if (e == first || n++ >= 2) continue;
                        hit(p, e, c * 0.5);
                        Vec3 a = first.getBoundingBox().getCenter(), b = e.getBoundingBox().getCenter();
                        beam(lv, a, b.subtract(a), a.distanceTo(b), col, 6);
                    }
                }
                sound(lv, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.5f);
            }
            case 1 -> { // Lôi Trận: 3 đợt sét trong vùng, choáng ngắn
                Vec3 pos = aim(lv, p, 14);
                Fx.play(lv, FxType.TALISMAN_CIRCLE, pos, Vec3.ZERO, 5f, col, 30);
                for (int k = 0; k < 3; k++) {
                    final int kk = k;
                    SkillScheduler.later(4 + k * 6, () -> {
                        for (LivingEntity e : around(lv, p, pos, 5)) {
                            hit(p, e, c / 3);
                            if (kk == 0) PhapRuntime.stun(e, 0.5);
                        }
                        Vec3 off = pos.add((lv.random.nextDouble() - 0.5) * 6, 0, (lv.random.nextDouble() - 0.5) * 6);
                        Fx.play(lv, FxType.LIGHTNING_BOLT, ground(lv, off.add(0, 4, 0)), Vec3.ZERO, 0.9f, col);
                        sound(lv, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.3f);
                    });
                }
            }
            case 2 -> { // Thiên Lôi Liên: sét nhảy chuỗi tối đa 6 mục tiêu
                List<LivingEntity> first = line(lv, p, p.getEyePosition(), p.getLookAngle(), 16, 1.6);
                if (first.isEmpty()) first = cone(lv, p, 12, 0.6);
                if (first.isEmpty()) {
                    beam(lv, vfx(p), p.getLookAngle(), 10, col, 6);
                    return;
                }
                LivingEntity cur = first.get(0);
                Set<LivingEntity> done = new LinkedHashSet<>();
                Vec3 prev = vfx(p);
                double mul = 1.0;
                for (int i = 0; i < 6 && cur != null; i++) {
                    done.add(cur);
                    hit(p, cur, c * mul);
                    PhapRuntime.stun(cur, 0.4);
                    Vec3 b = cur.getBoundingBox().getCenter();
                    beam(lv, prev, b.subtract(prev), prev.distanceTo(b), col, 7);
                    lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 8, 0.3, 0.3, 0.3, 0.2);
                    prev = b;
                    mul *= 0.85;
                    final LivingEntity from = cur;
                    cur = around(lv, p, from.position(), 6).stream().filter(e -> !done.contains(e))
                            .min(Comparator.comparingDouble(e -> e.distanceToSqr(from))).orElse(null);
                }
                sound(lv, prev, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);
            }
            default -> { // Lôi Kiếp: mây bão + 4 đợt sét trên diện rộng
                Vec3 pos = aim(lv, p, 16);
                Fx.play(lv, FxType.STORM_CLOUD, pos.add(0, 10, 0), Vec3.ZERO, 9f, 0x20243A, 60);
                Fx.play(lv, FxType.TELEGRAPH, pos, Vec3.ZERO, 9f, col, 40);
                for (int k = 0; k < 4; k++) {
                    final int kk = k;
                    SkillScheduler.later(12 + k * 9, () -> {
                        int bolts = 0;
                        for (LivingEntity e : around(lv, p, pos, 9)) {
                            hit(p, e, c / 4);
                            if (kk == 3) PhapRuntime.stun(e, 1.2);
                            if (bolts++ < 4) Fx.play(lv, FxType.LIGHTNING_BOLT, e.position(), Vec3.ZERO, 0.8f, col);
                        }
                        if (bolts == 0) Fx.play(lv, FxType.LIGHTNING_BOLT, pos.add((lv.random.nextDouble() - 0.5) * 12, 0, (lv.random.nextDouble() - 0.5) * 12), Vec3.ZERO, 0.8f, col);
                        sound(lv, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 1f, 1.0f + kk * 0.1f);
                    });
                }
            }
        }
    }

    // ------------------------------------------------------------------ HỎA: bùng nổ + thiêu

    private static void hoa(ServerLevel lv, ServerPlayer p, int slot, double c) {
        int col = PhapElement.HOA.rgb;
        Vec3 from = eye(p), look = p.getLookAngle(), vf = vfx(p);
        switch (slot) {
            case 0 -> { // Hỏa Cầu
                Vec3 pos = aim(lv, p, 18);
                Vec3 mid = pos.add(0, 0.8, 0);
                streak(lv, vf, mid.subtract(vf), vf.distanceTo(mid), ParticleTypes.FLAME, 0.4);
                beam(lv, vf, mid.subtract(vf), vf.distanceTo(mid), col, 4);
                LivingEntity prim = around(lv, p, pos, 1.6).stream().min(Comparator.comparingDouble(e -> e.position().distanceToSqr(pos))).orElse(null);
                for (LivingEntity e : around(lv, p, pos, 2.6)) {
                    hit(p, e, e == prim ? c : c * 0.6);
                    e.igniteForSeconds(3);
                }
                Fx.play(lv, FxType.SHOCKWAVE, pos, Vec3.ZERO, 2.6f, col, 12);
                lv.sendParticles(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z, 8, 0.6, 0.3, 0.6, 0.1);
                sound(lv, pos, SoundEvents.FIRECHARGE_USE, 1f, 0.9f);
            }
            case 1 -> { // Liệt Diễm Hoàn
                Vec3 o = p.position();
                for (LivingEntity e : around(lv, p, o, 6)) {
                    hit(p, e, c);
                    e.igniteForSeconds(4);
                    knock(e, o, 0.7, 0.2);
                }
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 6f, col, 16);
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 3.5f, 0xFFD040, 12);
                for (int k = 1; k <= 3; k++) SkillEffects.ringLater(lv, o.add(0, 0.2, 0), 2.0 * k, 30 + 12 * k, k * 2, ParticleTypes.FLAME);
                sound(lv, o, SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
            }
            case 2 -> { // Hỏa Long: rồng lửa xoắn
                for (LivingEntity e : line(lv, p, from, look, 14, 1.2)) {
                    hit(p, e, c);
                    e.igniteForSeconds(4);
                }
                Vec3 end = from.add(look.scale(14));
                SkillEffects.helixBeam(lv, vf, end, 0.8, 4, 90, ParticleTypes.FLAME);
                SkillEffects.helixBeam(lv, vf, end, 1.3, -3, 70, ParticleTypes.SMALL_FLAME);
                beam(lv, vf, look, 14, col, 10);
                beam(lv, vf, look, 14, 0xFFE070, 8);
                lv.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 10, 0.5, 0.5, 0.5, 0.2);
                sound(lv, from, SoundEvents.BLAZE_SHOOT, 1.2f, 0.6f);
            }
            default -> { // Phần Thiên: 3 thiên thạch lửa
                Vec3 pos = aim(lv, p, 16);
                Fx.play(lv, FxType.TALISMAN_CIRCLE, pos, Vec3.ZERO, 6f, col, 36);
                for (int k = 0; k < 3; k++) {
                    SkillScheduler.later(8 + k * 8, () -> {
                        Vec3 off = pos.add((lv.random.nextDouble() - 0.5) * 4, 0, (lv.random.nextDouble() - 0.5) * 4);
                        streak(lv, off.add(0, 16, 0), new Vec3(0, -1, 0), 16, ParticleTypes.FLAME, 0.6);
                        for (LivingEntity e : around(lv, p, off, 6)) {
                            hit(p, e, c / 3);
                            e.igniteForSeconds(5);
                        }
                        Fx.play(lv, FxType.LIGHT_PILLAR, off, Vec3.ZERO, 0.8f, col, 20);
                        Fx.play(lv, FxType.GROUND_CRACK, off, Vec3.ZERO, 3f, col, 40);
                        Fx.play(lv, FxType.SHOCKWAVE, off, Vec3.ZERO, 6f, 0xFFB040, 14);
                        sound(lv, off, SoundEvents.FIRECHARGE_USE, 1.2f, 0.5f);
                    });
                }
            }
        }
    }

    // ------------------------------------------------------------------ THỦY: hồi phục + hỗ trợ

    private static List<ServerPlayer> allies(ServerLevel lv, ServerPlayer p, double r) {
        return lv.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r), a -> a.isAlive() && a.distanceToSqr(p) <= r * r);
    }

    private static void thuy(ServerLevel lv, ServerPlayer p, int slot, double c) {
        int col = PhapElement.THUY.rgb;
        Vec3 from = eye(p), look = p.getLookAngle(), o = p.position(), vf = vfx(p);
        switch (slot) {
            case 0 -> { // Thủy Tiễn: gây sát thương + tự hồi
                List<LivingEntity> ts = line(lv, p, from, look, 16, 0.7);
                if (!ts.isEmpty()) {
                    hit(p, ts.get(0), c);
                    p.heal(p.getMaxHealth() * 0.04f);
                }
                beam(lv, vf, look, 16, col, 8);
                streak(lv, vf, look, 16, BLUE, 0.6);
                streak(lv, vf, look, 16, ParticleTypes.SPLASH, 1.2);
                sound(lv, from, SoundEvents.PLAYER_SPLASH, 0.8f, 1.5f);
            }
            case 1 -> { // Cam Lộ: hồi 25% máu đồng đội + gây nhẹ lên địch
                for (ServerPlayer a : allies(lv, p, 8)) {
                    a.heal(a.getMaxHealth() * 0.25f);
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
                    lv.sendParticles(ParticleTypes.HEART, a.getX(), a.getY() + 1.6, a.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                }
                for (LivingEntity e : around(lv, p, o, 6)) hit(p, e, c);
                Fx.play(lv, FxType.TALISMAN_CIRCLE, o, Vec3.ZERO, 8f, col, 50);
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 8f, 0xBFE8FF, 16);
                SkillEffects.spiral(lv, o, 1.5, 0.3, 3.0, 3, 60, ParticleTypes.FALLING_WATER);
                sound(lv, o, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            }
            case 2 -> { // Thủy Bích: khiên nước + đẩy lùi
                for (ServerPlayer a : allies(lv, p, 8)) {
                    a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1, false, true));
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0, false, true));
                }
                for (LivingEntity e : around(lv, p, o, 5)) {
                    hit(p, e, c);
                    knock(e, o, 1.2, 0.25);
                }
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 5f, col, 14);
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 3f, 0xFFFFFF, 10);
                SkillEffects.spiral(lv, o, 2.6, 0.4, 2.8, 4, 90, ParticleTypes.BUBBLE_POP);
                SkillEffects.ring(lv, o.add(0, 0.2, 0), 5, 50, ParticleTypes.SPLASH);
                sound(lv, o, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1f, 0.9f);
            }
            default -> { // Đại Hồng Thủy
                for (LivingEntity e : cone(lv, p, 14, 0.55)) {
                    hit(p, e, c);
                    knock(e, o, 1.6, 0.3);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
                }
                for (ServerPlayer a : allies(lv, p, 10)) {
                    a.heal(a.getMaxHealth() * 0.4f);
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, true));
                }
                Vec3 f = flat(p);
                for (int k = 1; k <= 5; k++) {
                    final int kk = k;
                    SkillScheduler.later(k * 2, () -> {
                        Vec3 at = o.add(f.scale(kk * 2.6));
                        Fx.play(lv, FxType.SHOCKWAVE, at, Vec3.ZERO, 3.2f + kk * 0.5f, col, 12);
                        lv.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.6, at.z, 40, 1.8, 0.5, 1.8, 0.3);
                        lv.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + 1.0, at.z, 12, 1.5, 0.6, 1.5, 0.05);
                    });
                }
                Fx.play(lv, FxType.SCREEN_TINT, o, Vec3.ZERO, 0.25f, col, 10);
                sound(lv, o, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.4f, 0.6f);
            }
        }
    }

    // ------------------------------------------------------------------ THỔ: tháp + địa chấn

    private static void tho(ServerLevel lv, ServerPlayer p, int slot, double c) {
        int col = PhapElement.THO.rgb;
        Vec3 o = p.position(), f = flat(p);
        switch (slot) {
            case 0 -> { // Thạch Mâu: gai đá dọc đường
                Vec3 from = o.add(0, 0.9, 0);
                for (LivingEntity e : line(lv, p, from, f, 10, 0.9)) hit(p, e, c);
                for (int i = 1; i <= 10; i++) {
                    final int ii = i;
                    SkillScheduler.later(ii / 2, () -> {
                        Vec3 at = o.add(f.scale(ii));
                        lv.sendParticles(STONE, at.x, at.y + 0.4, at.z, 10, 0.25, 0.5, 0.25, 0.15);
                        lv.sendParticles(DIRT, at.x, at.y + 0.1, at.z, 6, 0.3, 0.1, 0.3, 0.05);
                    });
                }
                Fx.play(lv, FxType.GROUND_CRACK, o.add(f.scale(5)), f, 1.6f, col, 40);
                sound(lv, o, SoundEvents.GRAVEL_BREAK, 1f, 0.7f);
            }
            case 1 -> { // Thổ Tháp: dựng 1 tháp
                Vec3 pos = ground(lv, o.add(f.scale(2.5)).add(0, 1, 0));
                PhapRuntime.summonTurret(lv, p, pos);
                sound(lv, pos, SoundEvents.ANVIL_LAND, 0.7f, 0.6f);
            }
            case 2 -> { // Địa Chấn
                for (LivingEntity e : around(lv, p, o, 7)) {
                    hit(p, e, c);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
                    e.push(0, 0.5, 0);
                    e.hurtMarked = true;
                }
                Fx.play(lv, FxType.GROUND_CRACK, o, Vec3.ZERO, 7f, col, 60);
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 7f, col, 16);
                SkillEffects.ring(lv, o.add(0, 0.2, 0), 3.5, 40, DIRT);
                SkillEffects.ring(lv, o.add(0, 0.2, 0), 6.5, 70, STONE);
                sound(lv, o, SoundEvents.ANVIL_LAND, 1.2f, 0.5f);
            }
            default -> { // Thiên Địa Phong Tháp: lấp đầy tháp + khuếch đại
                int max = PhapMath.thoMaxTurrets(PhapTu.aptitude(p));
                int have = PhapRuntime.turretCount(lv, p);
                for (int i = have; i < max; i++) {
                    double a = 2 * Math.PI * i / Math.max(1, max) + p.getYRot() * 0.0174;
                    PhapRuntime.summonTurret(lv, p, ground(lv, o.add(Math.cos(a) * 3.2, 1, Math.sin(a) * 3.2)));
                }
                PhapRuntime.empowerTurrets(lv, p, 200);
                for (LivingEntity e : around(lv, p, o, 8)) {
                    hit(p, e, c);
                    knock(e, o, 0.6, 0.3);
                }
                Fx.play(lv, FxType.GROUND_CRACK, o, Vec3.ZERO, 8f, col, 70);
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 8f, col, 18);
                Fx.play(lv, FxType.LIGHT_PILLAR, o, Vec3.ZERO, 0.7f, col, 30);
                Fx.play(lv, FxType.SCREEN_TINT, o, Vec3.ZERO, 0.2f, col, 8);
                sound(lv, o, SoundEvents.ANVIL_LAND, 1.4f, 0.4f);
            }
        }
    }

    // ------------------------------------------------------------------ KIM: sát thương + phòng thủ, chậm

    private static void kim(ServerLevel lv, ServerPlayer p, int slot, double c) {
        int col = PhapElement.KIM.rgb;
        Vec3 from = eye(p), look = p.getLookAngle(), o = p.position(), vf = vfx(p);
        switch (slot) {
            case 0 -> { // Kim Nhận: 3 lưỡi kim bay
                for (LivingEntity e : line(lv, p, from, look, 14, 0.8)) hit(p, e, c);
                for (int i = -1; i <= 1; i++) Fx.qi(lv, vf.add(0, i * 0.2, 0), look.scale(14), 0.7f, col, i * 35, 9);
                streak(lv, vf, look, 14, SILVER, 0.7);
                streak(lv, vf, look, 14, ParticleTypes.CRIT, 1.2);
                sound(lv, from, SoundEvents.IRON_GOLEM_ATTACK, 0.8f, 1.6f);
            }
            case 1 -> { // Kim Giáp: giáp + lưỡi phản chấn
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1, false, true));
                for (LivingEntity e : around(lv, p, o, 4)) {
                    hit(p, e, c);
                    knock(e, o, 0.6, 0.15);
                }
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI / 2 * i + 0.4;
                    Vec3 to = o.add(Math.cos(a) * 2.6, 0, Math.sin(a) * 2.6);
                    Fx.swordStab(lv, to.add(0, 5, 0), to, 5, i, 0.9f);
                }
                Fx.play(lv, FxType.SHOCKWAVE, o, Vec3.ZERO, 4f, col, 14);
                lv.sendParticles(IRON, o.x, o.y + 1, o.z, 30, 0.6, 0.8, 0.6, 0.1);
                sound(lv, o, SoundEvents.ANVIL_LAND, 0.9f, 1.2f);
            }
            case 2 -> { // Kim Quang Phá: chém quạt lớn
                for (LivingEntity e : cone(lv, p, 6.5, 0.5)) {
                    hit(p, e, c);
                    knock(e, o, 0.5, 0.1);
                }
                Fx.slash(lv, p, 20, 4.6, 0, 8, 2.2f);
                Fx.play(lv, FxType.SWORD_CRESCENT, from.add(look.scale(2.2)), look, 1.5f, col, 11);
                lv.sendParticles(ParticleTypes.CRIT, from.x + look.x * 3, from.y, from.z + look.z * 3, 30, 1.5, 0.6, 1.5, 0.3);
                sound(lv, from, SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.8f);
            }
            default -> { // Thiên Kim Trảm: lưỡi kiếm khổng lồ giáng xuống
                Vec3 pos = aim(lv, p, 14);
                Fx.play(lv, FxType.TALISMAN_CIRCLE, pos, Vec3.ZERO, 6f, col, 30);
                Fx.swordStab(lv, pos.add(0, 13, 0), pos, 10, 4, 3.6f);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, true));
                SkillScheduler.later(14, () -> {
                    for (LivingEntity e : around(lv, p, pos, 6)) {
                        hit(p, e, c);
                        knock(e, pos, 0.8, 0.3);
                    }
                    Fx.play(lv, FxType.SHOCKWAVE, pos, Vec3.ZERO, 6f, col, 16);
                    Fx.play(lv, FxType.GROUND_CRACK, pos, Vec3.ZERO, 6f, 0xDDE6F0, 60);
                    Fx.play(lv, FxType.SCREEN_TINT, pos, Vec3.ZERO, 0.3f, 0xFFFFFF, 8);
                    lv.sendParticles(IRON, pos.x, pos.y + 0.5, pos.z, 40, 1.5, 0.5, 1.5, 0.2);
                    sound(lv, pos, SoundEvents.ANVIL_LAND, 1.5f, 0.5f);
                });
            }
        }
    }
}
