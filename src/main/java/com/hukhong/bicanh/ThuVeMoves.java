package com.hukhong.bicanh;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.vfx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ chiêu riêng của Bí Cảnh Thủ Vệ (Tree Sentinel + Crucible Knight). Mỗi chiêu = dòng thời gian theo tick trong {@link #step};
 * tick đòn khớp keyframe trong tools/gen_bicanh_thu_ve.py.
 * <ul>
 *   <li>tv_combo — Kích Tam Liên: 2 nhát quét + nhát bổ dọc.</li>
 *   <li>tv_shield_bash — Khiên Chấn: húc khiên đẩy lùi rồi đâm kích thẳng.</li>
 *   <li>tv_tail — Giáp Vĩ Quét: xoay người, đuôi giáp quét vòng 360.</li>
 *   <li>tv_charge — Giác Xung: cúi sừng lao thẳng 12 block.</li>
 *   <li>tv_stomp — Cự Túc Trấn Địa: nhảy giẫm, sóng chấn lan (nhảy để né).</li>
 *   <li>tv_wings — Hỏa Dực Bộc Phát (pha 2+): cánh lửa bung, nổ quanh thân + vòng lửa trễ.</li>
 *   <li>tv_leap — Phi Kích: nhảy bổ kích, vết nứt lan thẳng phía trước.</li>
 *   <li>tv_ult — Thiên Phạt Thủ Hộ (tối thượng): bay lên, sét giáng vào từng người, cắm kích — sóng nhẫn 16 block.</li>
 * </ul>
 */
public final class ThuVeMoves {
    private ThuVeMoves() {}

    public static final int GOLD = 0xFFC458, EMBER = 0xFF7A2A, PALE = 0xFFE8B0, DANGER = 0xFF2030, STORM = 0xBFD8FF;
    public static final List<String> IDS = List.of("tv_combo", "tv_shield_bash", "tv_tail", "tv_charge",
            "tv_stomp", "tv_wings", "tv_leap", "tv_ult");

    public static boolean handles(String id) { return id != null && IDS.contains(id); }

    // ------------------------------------------------------------------ AI riêng (cả pha 1)
    static String pick(RandomSource rnd, int phase, boolean ultReady, float dist) {
        if (phase >= 3 && ultReady && rnd.nextInt(4) == 0) return "tv_ult";
        if (dist > 9) {
            if (phase >= 3 && rnd.nextBoolean()) return "tv_leap";
            return rnd.nextInt(3) > 0 ? "tv_charge" : null; // null: đi bộ áp sát
        }
        List<String> pool = new ArrayList<>(List.of("tv_combo", "tv_shield_bash", "tv_tail", "tv_combo", "tv_stomp"));
        if (phase >= 2) { pool.add("tv_wings"); pool.add("tv_charge"); }
        if (phase >= 3) { pool.add("tv_leap"); pool.add("tv_stomp"); }
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "bicanh_thu_ve". */
    static double mult(String id) {
        return switch (id) {
            case "tv_combo" -> 0.8;
            case "tv_shield_bash" -> 0.9;
            case "tv_tail" -> 1.0;
            case "tv_charge" -> 1.3;
            case "tv_stomp" -> 1.4;
            case "tv_wings" -> 1.1;
            case "tv_leap" -> 1.3;
            case "tv_ult" -> 2.4;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, BiCanhThuVe d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("tv_ult");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 40 * 40) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.5f, ult ? 0.5f : 0.8f);
        d.triggerAnim("skill", switch (d.mv) {
            case "tv_combo" -> "combo";
            case "tv_shield_bash" -> "shield_bash";
            case "tv_tail" -> "tail";
            case "tv_charge" -> "charge";
            case "tv_stomp" -> "stomp";
            case "tv_wings" -> "wings";
            case "tv_leap" -> "leap";
            default -> "ult";
        });
        if (ult) { d.ultCd = 640 - 60 * d.tier(); ultScene(sl, d); }
    }

    private static Vec3 tpos(BiCanhThuVe d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aim.scale(5));
    }

    private static void reaim(BiCanhThuVe d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aim = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aim.x, d.aim.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    // ------------------------------------------------------------------ mỗi tick; false = chiêu xong
    static boolean step(ServerLevel sl, BiCanhThuVe d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "tv_combo" -> combo(sl, d, t);
            case "tv_shield_bash" -> bash(sl, d, t);
            case "tv_tail" -> tail(sl, d, t);
            case "tv_charge" -> charge(sl, d, t);
            case "tv_stomp" -> stomp(sl, d, t);
            case "tv_wings" -> wings(sl, d, t);
            case "tv_leap" -> leap(sl, d, t);
            case "tv_ult" -> ult(sl, d, t);
            default -> false;
        };
    }

    // ---- Kích Tam Liên: quét t10, quét ngược t20, bổ dọc trễ t35
    private static boolean combo(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0 || t == 12) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 5f, 70, 9); }
        if (t == 10 || t == 20) sweep(sl, d, 5, 70, t == 10 ? 35 : -35, mult("tv_combo"));
        if (t == 22) {
            reaim(d);
            Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), d.aim.scale(7), 2.2f, 13, DANGER);
            d.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.2f, 0.7f);
        }
        if (t == 35) {
            Vec3 a = d.position(), b = a.add(d.aim.scale(7));
            hitLine(sl, d, a, b, 1.6, mult("tv_combo") * 1.5, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, a.add(d.aim.scale(4)), 2.5f, GOLD, 6, 25);
            Fx.shake(sl, a, 1.2f, 8, 16f);
            d.playSound(SoundEvents.ANVIL_LAND, 1.4f, 0.6f);
        }
        return t < 46;
    }

    private static void sweep(ServerLevel sl, BiCanhThuVe d, double rad, double half, double roll, double mul) {
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        Fx.qi(sl, d.position().add(0, 1.4, 0).add(d.aim.scale(rad * 0.4)), d.aim.scale(rad * 0.8), (float) (rad * 0.6), PALE, roll, 8);
        hitCone(sl, d, rad, half, mul, Hit.NONE);
    }

    // ---- Khiên Chấn: húc khiên t10 (đẩy lùi), đâm kích t22
    private static boolean bash(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 3.8f, 60, 10); }
        if (t == 10) {
            d.playSound(SoundEvents.SHIELD_BLOCK, 2f, 0.5f);
            Fx.groundRing(sl, null, d.position().add(d.aim.scale(2)), 2f, GOLD, 10);
            for (Player p : coneTargets(sl, d, 3.8, 60)) {
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult("tv_shield_bash")))) {
                    p.setDeltaMovement(p.getDeltaMovement().add(d.aim.scale(1.4)).add(0, 0.35, 0));
                    p.hurtMarked = true;
                    PhaseMoves.applyHit(p, Hit.SLOW);
                }
            }
        }
        if (t == 12) { reaim(d); Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), d.aim.scale(7), 1.4f, 10, DANGER); }
        if (t == 22) {
            Vec3 a = d.position(), b = a.add(d.aim.scale(7));
            Fx.qi(sl, a.add(0, 1.4, 0).add(d.aim.scale(2)), d.aim.scale(5), 1.2f, PALE, 0, 8);
            d.playSound(SoundEvents.TRIDENT_THROW.value(), 1.5f, 0.6f);
            hitLine(sl, d, a, b, 1.2, mult("tv_shield_bash"), Hit.NONE);
        }
        return t < 32;
    }

    // ---- Giáp Vĩ Quét: vòng 4.5 block, đánh t14
    private static boolean tail(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) { Fx.telegraphCircle(sl, null, d.position(), 4.5f, 14, DANGER); d.playSound(SoundEvents.CHAIN_BREAK, 1.6f, 0.5f); }
        if (t == 14) {
            Fx.groundRing(sl, null, d.position(), 4.5f, GOLD, 12);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, d.getX(), d.getY() + 0.8, d.getZ(), 8, 2.5, 0.2, 2.5, 0);
            d.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2f, 0.5f);
            for (Player p : players(sl, d.position(), 4.5)) {
                if (flat2(p.position(), d.position()) > 4.5 * 4.5 || Math.abs(p.getY() - d.getY()) > 3) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult("tv_tail")))) {
                    Vec3 out = p.position().subtract(d.position()).multiply(1, 0, 1);
                    if (out.lengthSqr() > 1e-4) p.setDeltaMovement(out.normalize().scale(1.2).add(0, 0.3, 0));
                    p.hurtMarked = true;
                }
            }
        }
        return t < 28;
    }

    // ---- Giác Xung: cảnh báo 0..10, lao 10..30 (0.6 block/tick, có va chạm)
    private static boolean charge(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) {
            reaim(d);
            Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), d.aim.scale(12), 2.2f, 12, DANGER);
            d.playSound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.8f);
        }
        if (t >= 10 && t < 30) {
            Vec3 before = d.position();
            d.move(MoverType.SELF, d.aim.scale(0.6));
            if (t % 2 == 0) sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, d.getX(), d.getY() + 0.2, d.getZ(), 2, 0.4, 0.1, 0.4, 0.01);
            if (t % 5 == 0) d.playSound(SoundEvents.RAVAGER_STEP, 1.5f, 0.7f);
            for (Player p : players(sl, d.position(), 2)) {
                if (flat2(p.position(), d.position()) > 2.0 * 2.0 || !d.hitOnce.add(p.getUUID())) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult("tv_charge")))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            }
            if (t > 12 && d.position().distanceToSqr(before) < 0.01) { // đâm tường: choáng nhẹ
                Fx.shake(sl, d.position(), 1f, 6, 12f);
                d.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 1.5f, 0.6f);
                return false;
            }
        }
        return t < 36;
    }

    // ---- Cự Túc Trấn Địa: nhảy 0..24, giẫm t24, sóng chấn lan 24..36 (r 5 -> 11)
    private static final double ST_CORE = 5, ST_WAVE = 11;

    private static boolean stomp(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) {
            d.from = d.position();
            Fx.telegraphCircle(sl, null, d.position(), (float) ST_CORE, 24, DANGER);
            Fx.telegraphCircle(sl, null, d.position(), (float) ST_WAVE, 36, GOLD);
            d.setNoGravity(true);
            d.playSound(SoundEvents.GOAT_LONG_JUMP, 1.6f, 0.6f);
        }
        if (t > 0 && t <= 24) {
            double k = t / 24.0, h = Math.sin(k * Math.PI) * 3.0;
            d.setPos(d.from.x, d.from.y + h, d.from.z);
        }
        if (t == 24) {
            d.setNoGravity(false);
            d.hitOnce.clear();
            hitAround(sl, d, d.from, ST_CORE, mult("tv_stomp"), Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, d.from, (float) ST_CORE, GOLD, 10, 40);
            Fx.shake(sl, d.from, 2.5f, 14, 28f);
            sl.sendParticles(ParticleTypes.EXPLOSION, d.from.x, d.from.y + 0.5, d.from.z, 4, 1.5, 0.2, 1.5, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.8f, 0.6f);
        }
        if (t > 24 && t <= 36) shockRing(sl, d, d.from, ST_CORE + (ST_WAVE - ST_CORE) * (t - 24) / 12.0, t, 0.5);
        return t < 40;
    }

    /** Dải sóng chấn bán kính r: ai đứng đất trong dải => trúng một lần (nhảy để né). */
    private static void shockRing(ServerLevel sl, BiCanhThuVe d, Vec3 c, double r, int t, double mul) {
        if (t % 3 == 1) Fx.groundRing(sl, null, c, (float) r, PALE, 8);
        for (Player p : players(sl, c, r + 1)) {
            double fd = Math.sqrt(flat2(p.position(), c));
            if (fd < r - 1.2 || fd > r + 1.0 || !p.onGround() || !d.hitOnce.add(p.getUUID())) continue;
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, Hit.SLOW);
        }
    }

    // ---- Hỏa Dực Bộc Phát: bung cánh 0..20, nổ r7 t20, vòng lửa trễ r7..10 t32
    private static boolean wings(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) {
            Fx.telegraphCircle(sl, null, d.position(), 7f, 20, DANGER);
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 2f, EMBER, 20, 5);
            d.playSound(SoundEvents.BLAZE_SHOOT, 1.6f, 0.5f);
        }
        if (t > 0 && t < 20 && t % 3 == 0)
            sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 2.2, d.getZ(), 10, 1.4, 0.8, 1.4, 0.02);
        if (t == 20) {
            for (Player p : players(sl, d.position(), 7)) {
                if (flat2(p.position(), d.position()) > 49 || Math.abs(p.getY() - d.getY()) > 4) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult("tv_wings")))) p.igniteForSeconds(4);
            }
            sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 1.5, d.getZ(), 120, 0.4, 0.6, 0.4, 0.45);
            sl.sendParticles(ParticleTypes.LAVA, d.getX(), d.getY() + 1.5, d.getZ(), 20, 2, 1, 2, 0);
            Fx.groundRing(sl, null, d.position(), 7f, EMBER, 20);
            Fx.shake(sl, d.position(), 1.5f, 10, 20f);
            d.playSound(SoundEvents.FIRECHARGE_USE, 2f, 0.5f);
            Fx.telegraphCircle(sl, null, d.position(), 10f, 12, EMBER);
        }
        if (t == 32) {
            Fx.groundRing(sl, null, d.position(), 10f, EMBER, 14);
            for (Player p : players(sl, d.position(), 10.5)) {
                double fd = Math.sqrt(flat2(p.position(), d.position()));
                if (fd < 6.5 || fd > 10.5) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult("tv_wings") * 0.7))) p.igniteForSeconds(3);
            }
            d.playSound(SoundEvents.BLAZE_SHOOT, 1.8f, 0.7f);
        }
        return t < 42;
    }

    // ---- Phi Kích: nhảy 0..20 về điểm đánh dấu, bổ t20 + vết nứt thẳng 8 block
    private static boolean leap(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, 3.2f, 20, DANGER);
            Fx.telegraphLine(sl, null, d.spot.add(0, 0.05, 0), d.aim.scale(8), 1.6f, 24, GOLD);
            d.setNoGravity(true);
            d.playSound(SoundEvents.GOAT_LONG_JUMP, 1.6f, 0.8f);
        }
        if (t > 0 && t <= 20) {
            double k = t / 20.0;
            Vec3 q = d.from.lerp(d.spot, k).add(0, Math.sin(k * Math.PI) * 4, 0);
            d.setPos(q.x, q.y, q.z);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, q.x, q.y + 1, q.z, 3, 0.3, 0.3, 0.3, 0.01);
        }
        if (t == 20) {
            d.setNoGravity(false);
            hitAround(sl, d, d.spot, 3.2, mult("tv_leap"), Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, d.spot, 3f, GOLD, 6, 30);
            Fx.shake(sl, d.spot, 1.8f, 10, 20f);
            d.playSound(SoundEvents.MACE_SMASH_GROUND, 1.8f, 0.7f);
        }
        if (t == 24) {
            Vec3 b = d.spot.add(d.aim.scale(8));
            Fx.rift(sl, null, d.spot.add(d.aim.scale(4)), d.aim.scale(8), 0.6f, EMBER, 20);
            hitLine(sl, d, d.spot, b, 1.3, mult("tv_leap") * 0.6, Hit.NONE);
        }
        return t < 32;
    }

    // ---- Thiên Phạt Thủ Hộ (tối thượng): bay lên 0..30, sét 50/62(/74), cắm kích t90, sóng nhẫn 90..110 (r 5 -> 16)
    public static final int U_RISE = 30, U_SLAM = 90, U_END = 116;
    public static final double U_CORE = 5, U_WAVE = 16;

    private static boolean ult(ServerLevel sl, BiCanhThuVe d, int t) {
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.spot = d.position();
            d.setNoGravity(true);
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 3.5f, GOLD, U_SLAM, 6);
            d.playSound(SoundEvents.EVOKER_PREPARE_WOLOLO, 2f, 0.5f);
        }
        if (t > 0 && t <= U_RISE) {
            d.setPos(d.from.x, d.from.y + 4.0 * t / U_RISE, d.from.z);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 2, d.getZ(), 6, 1.2, 0.6, 1.2, 0.02);
        }
        if (t == U_RISE) {
            Fx.telegraphCircle(sl, null, d.spot, (float) U_CORE, U_SLAM - U_RISE, DANGER);
            Fx.telegraphCircle(sl, null, d.spot, (float) U_WAVE, U_SLAM - U_RISE + 20, GOLD);
            Fx.giantSword(sl, null, d.spot, 3.5f, GOLD, U_SLAM - U_RISE, U_SLAM - U_RISE + 30);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 2.5f, GOLD, 0);
            d.playSound(SoundEvents.BEACON_ACTIVATE, 2f, 0.5f);
        }
        int waves = d.tier() >= 3 ? 3 : 2;
        for (int w = 0; w < waves; w++) {
            int strike = 50 + 12 * w;
            if (t == strike - 14) for (Player p : players(sl, d.spot, U_WAVE + 4)) Fx.telegraphCircle(sl, null, p.position(), 2f, 14, STORM);
            if (t == strike) for (Player p : players(sl, d.spot, U_WAVE + 4)) strike(sl, d, p.position());
        }
        if (t > U_RISE && t < U_SLAM && t % 4 == 0)
            sl.sendParticles(ParticleTypes.WAX_ON, d.getX(), d.getY() + 3, d.getZ(), 6, 0.8, 0.8, 0.8, 0.1);
        if (t == U_SLAM) {
            d.setPos(d.spot.x, d.spot.y, d.spot.z);
            d.setNoGravity(false);
            hitAround(sl, d, d.spot, U_CORE, mult("tv_ult"), Hit.KNOCK_UP);
            Fx.elem(sl, null, Fx.E_NUKE, d.spot, Vec3.ZERO, 1.8f, EMBER, 0);
            Fx.groundCrack(sl, null, d.spot, (float) U_CORE + 2, GOLD, 12, 60);
            Fx.shake(sl, d.spot, 3.5f, 24, 40f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, d.spot.x, d.spot.y + 1, d.spot.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.5f);
            d.hitOnce.clear();
        }
        if (t > U_SLAM && t <= U_SLAM + 20) shockRing(sl, d, d.spot, U_CORE + (U_WAVE - U_CORE) * (t - U_SLAM) / 20.0, t, 0.6);
        return t < U_END;
    }

    /** Sét chỉ hình (visual) + sát thương tự tính trong r 2. */
    private static void strike(ServerLevel sl, BiCanhThuVe d, Vec3 at) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
        if (b != null) { b.moveTo(at.x, at.y, at.z); b.setVisualOnly(true); sl.addFreshEntity(b); }
        for (Player p : players(sl, at, 2))
            if (flat2(p.position(), at) <= 4 && p.hurt(d.damageSources().lightningBolt(), d.moveDamage(0.7))) PhaseMoves.applyHit(p, Hit.SLOW);
    }

    // ------------------------------------------------------------------ trợ giúp sát thương
    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 6, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static void hitAround(ServerLevel sl, BiCanhThuVe d, Vec3 c, double r, double mul, Hit h) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static List<Player> coneTargets(ServerLevel sl, BiCanhThuVe d, double r, double halfDeg) {
        double cos = Math.cos(Math.toRadians(halfDeg));
        List<Player> out = new ArrayList<>();
        for (Player p : players(sl, d.position(), r)) {
            Vec3 v = p.position().subtract(d.position()).multiply(1, 0, 1);
            double len = v.length();
            if (len > r || Math.abs(p.getY() - d.getY()) > 3) continue;
            if (len > 0.8 && v.scale(1 / len).dot(d.aim) < cos) continue;
            out.add(p);
        }
        return out;
    }

    private static void hitCone(ServerLevel sl, BiCanhThuVe d, double r, double halfDeg, double mul, Hit h) {
        for (Player p : coneTargets(sl, d, r, halfDeg))
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
    }

    private static void hitLine(ServerLevel sl, BiCanhThuVe d, Vec3 a, Vec3 b, double w, double mul, Hit h) {
        Vec3 ab = b.subtract(a).multiply(1, 0, 1);
        double L2 = Math.max(1e-4, ab.lengthSqr());
        for (Player p : sl.getEntitiesOfClass(Player.class, new AABB(a, b).inflate(w + 1, 3, w + 1), q -> !q.isSpectator() && q.isAlive())) {
            Vec3 ap = p.position().subtract(a).multiply(1, 0, 1);
            double k = Math.max(0, Math.min(1, ap.dot(ab) / L2));
            if (ap.subtract(ab.scale(k)).lengthSqr() > w * w) continue;
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    // ------------------------------------------------------------------ chuyển pha / cutscene
    /** Lộ diện pha: P2 cánh lửa bung, P3 vầng hào quang + sét quanh trường. */
    static void transformFx(ServerLevel sl, BiCanhThuVe d, int ph) {
        d.triggerAnim("skill", "phase");
        Fx.groundRing(sl, null, d.position(), ph >= 3 ? 8f : 6f, ph >= 3 ? GOLD : EMBER, 30);
        sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 2, d.getZ(), 80, 1.2, 1.2, 1.2, 0.15);
        if (ph >= 3) {
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 2.5f, GOLD, 0);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2;
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
                if (b != null) { b.moveTo(d.getX() + Math.cos(a) * 6, d.getY(), d.getZ() + Math.sin(a) * 6); b.setVisualOnly(true); sl.addFreshEntity(b); }
            }
        }
        d.playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
    }

    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: Thủ Vệ đứng chắn trước bảo khố, camera thấp quét từ khiên lên mũ sừng. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 1.5, 0.5, 8.0, 68, 1.0, -4), k(T * 2 / 5, -2.0, 0.6, 4.0, 56, 1.8, -10),
                k(T * 3 / 4, 0.6, 2.0, 2.6, 46, 2.6, 0), k(T, 0, 2.2, 5.0, 62, 2.0, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 34, "entity.hukhong." + BiCanhThuVe.ID), new SubLine(34, 72, "hukhong.dialog.bicanh.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.bicanh.intro.2"));
    }

    static final int ULT_T = 60;

    private static void ultScene(ServerLevel sl, BiCanhThuVe d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 2.4 * s, 0.4 * s, 4.0 * s, 60, 1.6 * s, -6), k(ULT_T / 2, -1.6 * s, 0.3 * s, 4.5 * s, 68, 4.0 * s, -20),
                k(ULT_T, 0, 1.2 * s, 9.0 * s, 80, 5.0 * s, -16));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 30, "hukhong.dialog.bicanh.ult.1"),
                new SubLine(30, ULT_T - 2, "hukhong.dialog.bicanh.ult.2")));
    }

    static void deathScene(ServerLevel sl, BiCanhThuVe d) {
        int T = 80;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.4, 4.0, 55, 1.4, 0), k(40, 2.4, 1.8, 3.0, 60, 1.0, 0), k(T, 0, 4.0, 7.5, 72, 0.8, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 42, "hukhong.dialog.bicanh.death.1"),
                new SubLine(42, T - 4, "hukhong.dialog.bicanh.death.2")));
        Fx.groundRing(sl, null, at, 6f, GOLD, 50);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.5, at.z, 60, 1.2, 1.2, 1.2, 0.05);
        sl.sendParticles(ParticleTypes.WAX_ON, at.x, at.y + 1, at.z, 40, 1, 1, 1, 0.1);
    }
}
