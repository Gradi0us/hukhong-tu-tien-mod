package com.hukhong.entity;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ chiêu riêng của Băng Nhãn — "con mắt băng của vũ trụ đông kết" (kiểu Astel + Rennala). Mỗi chiêu = dòng thời gian theo tick
 * trong {@link #step}; mọi chiêu có telegraph (Fx.telegraph*) trước khi gây sát thương, VFX riêng tông băng/lam.
 * <ul>
 *   <li>bn_bang_tia  Băng Nhãn Xạ Tuyến: tia băng thẳng (laser hư không của Astel).</li>
 *   <li>bn_thuan_anh Thuấn Ảnh Hàn Bộ: chớp dịch ra sau lưng mục tiêu rồi nổ băng (2 lần, pha 3: 3 lần).</li>
 *   <li>bn_bang_tinh Băng Tinh Truy Hồn: 6 mảnh băng lơ lửng rồi truy theo vị trí người chơi (glintstone của Rennala).</li>
 *   <li>bn_han_khi   Hàn Khí Băng Bạo: vòng nổ băng quanh thân (pha 1).</li>
 *   <li>bn_hap_nhan  Băng Nhãn Hấp Hồn: hút người chơi về phía con mắt rồi nổ (grab-and-suck của Astel).</li>
 *   <li>bn_sao_bang  Hàn Tinh Lưu Vẫn: mưa sao chổi băng so le (meteor shower).</li>
 *   <li>bn_huyen_anh Huyễn Ảnh Băng Thân: 2 phân thân băng hai bên bắn tia chéo rồi vỡ (linh triệu của Rennala).</li>
 *   <li>bn_toan_tuyen Hàn Nhãn Toàn Tuyến: 4 tia băng hình chữ thập xoay quanh thân.</li>
 *   <li>bn_bang_nguyet Băng Nguyệt (tối thượng): bay lên, trăng tròn băng giáng xuống đóng băng diện rộng.</li>
 * </ul>
 */
public final class BangNhanMoves {
    private BangNhanMoves() {}

    public static final int ICE = 0x9FE8FF, DEEP = 0x3A6BFF, PALE = 0xE6F6FF, DANGER = 0xFF3040;
    public static final List<String> IDS = List.of("bn_bang_tia", "bn_thuan_anh", "bn_bang_tinh", "bn_han_khi", "bn_hap_nhan",
            "bn_sao_bang", "bn_huyen_anh", "bn_toan_tuyen", "bn_bang_nguyet");

    public static boolean handles(String id) { return IDS.contains(id); }

    /** AI riêng (director chen thêm chiêu phase theo BossPhases "bang_nhan"). */
    static String pick(RandomSource rnd, int phase, float dist) {
        if (dist > 10 && rnd.nextBoolean()) return phase >= 2 && rnd.nextBoolean() ? "bn_bang_tinh" : "bn_bang_tia";
        List<String> pool = phase >= 3
                ? List.of("bn_bang_tia", "bn_thuan_anh", "bn_hap_nhan", "bn_sao_bang", "bn_toan_tuyen", "bn_huyen_anh")
                : phase == 2
                ? List.of("bn_bang_tia", "bn_thuan_anh", "bn_bang_tinh", "bn_hap_nhan", "bn_sao_bang")
                : List.of("bn_bang_tia", "bn_thuan_anh", "bn_bang_tinh", "bn_han_khi");
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Chiêu pha 1 theo chỉ số kỹ năng cũ của WorldBoss (begin(skill)). */
    static String bySkill(int skill) {
        return switch (skill) { case 0 -> "bn_bang_tia"; case 1 -> "bn_han_khi"; case 2 -> "bn_bang_tinh"; default -> "bn_thuan_anh"; };
    }

    /** Hệ số sát thương — khớp BossPhases "bang_nhan". */
    static double mult(String id) {
        return switch (id) {
            case "bn_bang_tia" -> 1.1;
            case "bn_thuan_anh" -> 1.0;
            case "bn_bang_tinh" -> 0.7;
            case "bn_han_khi" -> 1.3;
            case "bn_hap_nhan" -> 1.6;
            case "bn_sao_bang" -> 0.9;
            case "bn_huyen_anh" -> 0.9;
            case "bn_toan_tuyen" -> 0.8;
            case "bn_bang_nguyet" -> 2.4;
            default -> 1.0;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, BangNhan b) {
        reaim(b);
        boolean ult = b.mv.equals("bn_bang_nguyet");
        Component name = Component.translatable("hukhong.boss.move." + b.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD) : name, true);
        b.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, ult ? 0.4f : 0.8f);
        if (ult) ultScene(sl, b);
    }

    private static Vec3 tpos(BangNhan b) {
        LivingEntity t = b.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return b.position().add(b.aim.scale(6));
    }

    private static void reaim(BangNhan b) {
        Vec3 dir = tpos(b).subtract(b.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) b.aim = dir.normalize();
        b.setYRot((float) Math.toDegrees(Math.atan2(-b.aim.x, b.aim.z)));
        b.yBodyRot = b.getYRot(); b.yHeadRot = b.getYRot();
    }

    private static Vec3 eye(BangNhan b) { return b.position().add(0, b.getBbHeight() * 0.75, 0); }

    // ------------------------------------------------------------------ mỗi tick; false = xong
    static boolean step(ServerLevel sl, BangNhan b, int t) {
        b.getNavigation().stop();
        return switch (b.mv) {
            case "bn_bang_tia" -> laser(sl, b, t);
            case "bn_thuan_anh" -> blink(sl, b, t, b.phase() >= 3 ? 3 : 2);
            case "bn_bang_tinh" -> shards(sl, b, t);
            case "bn_hap_nhan" -> suck(sl, b, t);
            case "bn_sao_bang" -> comets(sl, b, t);
            case "bn_huyen_anh" -> phantoms(sl, b, t);
            case "bn_toan_tuyen" -> spinLaser(sl, b, t);
            case "bn_bang_nguyet" -> fullMoon(sl, b, t);
            default -> nova(sl, b, t);
        };
    }

    // ---- Băng Nhãn Xạ Tuyến: mắt tụ quang 22 tick (vạch) -> tia 14 tick
    static final double LASER_LEN = 24;

    private static boolean laser(ServerLevel sl, BangNhan b, int t) {
        if (t == 0) {
            b.triggerAnim("skill", "skill_a");
            b.from = b.position();
            Fx.telegraphLine(sl, null, b.from.add(0, 0.05, 0), b.aim.scale(LASER_LEN), 2.6f, 22, ICE);
            Fx.elem(sl, null, Fx.E_HALOS, eye(b), Vec3.ZERO, 1.2f, ICE, 22);
            b.playSound(SoundEvents.BEACON_ACTIVATE, 2.5f, 1.6f);
        }
        if (t < 22 && t % 3 == 0) sl.sendParticles(ParticleTypes.END_ROD, eye(b).x, eye(b).y, eye(b).z, 4, 0.4, 0.4, 0.4, 0.02);
        if (t >= 22 && t < 36) {
            if (t == 22) {
                Fx.rift(sl, null, b.from.add(0, 1.2, 0), b.aim.scale(LASER_LEN), 0.9f, ICE, 16);
                b.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2.5f, 1.5f);
            }
            if ((t - 22) % 6 == 0) b.hitOnce.clear();
            beamFx(sl, eye(b), b.from.add(b.aim.scale(LASER_LEN)).add(0, 1, 0));
            hitLine(sl, b, b.from, b.aim, LASER_LEN, 1.4, mult(b.mv) * 0.5, Hit.SLOW);
        }
        return t < 44;
    }

    // ---- Thuấn Ảnh Hàn Bộ: [vòng cảnh báo tại mục tiêu -> tan biến -> hiện sau lưng -> nổ băng] x n
    static final int BL_LEN = 26;

    private static boolean blink(ServerLevel sl, BangNhan b, int t, int n) {
        int k = t / BL_LEN, e = t % BL_LEN;
        if (k >= n) return t < n * BL_LEN + 6;
        double r = 4;
        if (e == 0) {
            reaim(b);
            b.spot = tpos(b);
            Fx.telegraphCircle(sl, null, b.spot, (float) r, 20, ICE);
            Fx.rune(sl, null, b.spot.add(0, 0.1, 0), 2.5f, DEEP, 20, 6);
            b.triggerAnim("skill", "skill_c");
        }
        if (e == 10) {  // tan thành tuyết, hiện ra sau lưng mục tiêu
            sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX(), b.getY() + 2, b.getZ(), 60, 1, 2, 1, 0.1);
            Vec3 to = b.spot.add(b.aim.scale(3));
            Fx.ribbon(sl, null, eye(b), to.add(0, 2, 0), 1.5, 4, 0, 0.4f, PALE);
            b.teleportTo(to.x, to.y, to.z);
            b.setDeltaMovement(Vec3.ZERO);
            b.aim = b.aim.scale(-1);
            b.setYRot((float) Math.toDegrees(Math.atan2(-b.aim.x, b.aim.z)));
            b.yBodyRot = b.getYRot();
            b.playSound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.6f);
        }
        if (e == 20) {
            for (Player p : players(sl, b.spot, r))
                if (flat2(p.position(), b.spot) <= r * r && b.strike(p, b.moveDamage(mult(b.mv)))) PhaseMoves.applyHit(p, Hit.FREEZE);
            Fx.elem(sl, null, Fx.E_SPIKES, b.spot, Vec3.ZERO, (float) r * 0.5f, ICE, 20);
            Fx.groundCrack(sl, null, b.spot, (float) r, ICE, 6, 30);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, b.spot.x, b.spot.y + 1, b.spot.z, 50, r * 0.4, 1, r * 0.4, 0.1);
            b.playSound(SoundEvents.GLASS_BREAK, 2.5f, 0.5f);
        }
        return true;
    }

    // ---- Băng Tinh Truy Hồn: 6 mảnh lơ lửng (20 tick) -> khoá mục tiêu (vòng 12 tick) -> lao xuống
    private static boolean shards(ServerLevel sl, BangNhan b, int t) {
        if (t == 0) {
            b.triggerAnim("skill", "skill_b");
            b.points.clear();
            for (int i = 0; i < 6; i++) {
                double a = Math.atan2(b.aim.z, b.aim.x) + Math.toRadians(-75 + i * 30);
                b.points.add(b.position().add(Math.cos(a) * 3, b.getBbHeight() + 1.5 + (i % 2), Math.sin(a) * 3));
            }
            b.playSound(SoundEvents.AMETHYST_CLUSTER_PLACE, 2.5f, 1.2f);
        }
        if (t < 20 && t % 2 == 0)
            for (Vec3 q : b.points) sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 2, 0.15, 0.15, 0.15, 0.01);
        if (t == 20) {  // khoá: mỗi mảnh nhắm vị trí hiện tại của 1 người chơi (xoay vòng) + lệch nhỏ
            List<Player> ps = players(sl, b.position(), 28);
            b.spots.clear();
            for (int i = 0; i < b.points.size(); i++) {
                Vec3 base = ps.isEmpty() ? tpos(b) : ps.get(i % ps.size()).position();
                Vec3 s = base.add((b.getRandom().nextDouble() - 0.5) * 3, 0, (b.getRandom().nextDouble() - 0.5) * 3);
                b.spots.add(s);
                Fx.telegraphCircle(sl, null, s, 1.8f, 12 + i * 2, ICE);
            }
        }
        if (t >= 32 && t < 32 + 6 * 2 && (t - 32) % 2 == 0) {  // từng mảnh lao xuống so le
            int i = (t - 32) / 2;
            if (i < b.spots.size()) {
                Vec3 from = b.points.get(i), s = b.spots.get(i);
                Fx.ribbon(sl, null, from, s.add(0, 0.3, 0), 0.6, 3, 0, 0.25f, ICE);
                for (Player p : players(sl, s, 1.8))
                    if (flat2(p.position(), s) <= 1.8 * 1.8 && b.strike(p, b.moveDamage(mult(b.mv)))) PhaseMoves.applyHit(p, Hit.SLOW);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, s.x, s.y + 0.5, s.z, 16, 0.5, 0.4, 0.5, 0.05);
                b.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 2f, 1.4f);
            }
        }
        return t < 52;
    }

    // ---- Hàn Khí Băng Bạo: vòng 9 block quanh thân
    private static boolean nova(ServerLevel sl, BangNhan b, int t) {
        double r = 9;
        if (t == 0) {
            b.triggerAnim("skill", "skill_b");
            Fx.telegraphCircle(sl, null, b.position(), (float) r, 28, ICE);
            Fx.rune(sl, null, b.position().add(0, 0.1, 0), 5f, DEEP, 28, 8);
        }
        if (t < 28 && t % 4 == 0) sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX(), b.getY() + 0.5, b.getZ(), 12, r * 0.4, 0.2, r * 0.4, 0.0);
        if (t == 28) {
            for (Player p : players(sl, b.position(), r))
                if (flat2(p.position(), b.position()) <= r * r && b.strike(p, b.moveDamage(mult(b.mv)))) PhaseMoves.applyHit(p, Hit.FREEZE);
            Fx.groundRing(sl, null, b.position(), (float) r, ICE, 24);
            Fx.elem(sl, null, Fx.E_SPIKES, b.position(), Vec3.ZERO, 2.5f, ICE, 30);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX(), b.getY() + 1, b.getZ(), 90, 4, 1.5, 4, 0.1);
            b.playSound(SoundEvents.GLASS_BREAK, 2f, 0.5f);
        }
        return t < 36;
    }

    // ---- Băng Nhãn Hấp Hồn: vòng hút 14 (30 tick hút dần) + lõi nổ 5 block
    private static boolean suck(ServerLevel sl, BangNhan b, int t) {
        double pull = 14, core = 5;
        if (t == 0) {
            b.triggerAnim("skill", "skill_c");
            Fx.telegraphCircle(sl, null, b.position(), (float) pull, 30, DEEP);
            Fx.telegraphCircle(sl, null, b.position(), (float) core, 34, DANGER);
            Fx.elem(sl, null, Fx.E_HALOS, eye(b), Vec3.ZERO, 2.2f, DEEP, 34);
            b.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 1.3f);
        }
        if (t > 4 && t < 30) {
            for (Player p : players(sl, b.position(), pull)) {
                Vec3 d = b.position().subtract(p.position()).multiply(1, 0, 1);
                if (d.lengthSqr() > pull * pull || d.lengthSqr() < 1) continue;
                p.setDeltaMovement(p.getDeltaMovement().add(d.normalize().scale(0.09)));
                p.hurtMarked = true;
                if (t % 4 == 0) Fx.ribbon(sl, null, p.position().add(0, 1, 0), eye(b), 0.6, 6, 0, 0.12f, ICE);
            }
            if (t % 3 == 0) {
                double a = t * 0.7;
                sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX() + Math.cos(a) * pull, b.getY() + 0.5, b.getZ() + Math.sin(a) * pull, 6, 0.5, 0.3, 0.5, 0);
            }
        }
        if (t == 34) {
            for (Player p : players(sl, b.position(), core))
                if (flat2(p.position(), b.position()) <= core * core && b.strike(p, b.moveDamage(mult(b.mv)))) PhaseMoves.applyHit(p, Hit.FREEZE);
            Fx.elem(sl, null, Fx.E_NUKE, b.position(), Vec3.ZERO, 2f, ICE, 0);
            Fx.groundCrack(sl, null, b.position(), (float) core, DEEP, 8, 40);
            Fx.shake(sl, b.position(), 2f, 14, 30f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 1.2f);
        }
        return t < 44;
    }

    // ---- Hàn Tinh Lưu Vẫn: sao chổi băng so le (mỗi 4 tick một vòng, rơi sau 24 tick)
    static final int CM_DELAY = 24, CM_GAP = 4;

    private static boolean comets(ServerLevel sl, BangNhan b, int t) {
        if (t == 0) {
            b.triggerAnim("skill", "skill_b");
            b.spots.clear();
            for (Player p : players(sl, b.position(), 28)) {
                b.spots.add(p.position());
                for (int i = 0; i < 3; i++) {
                    double a = b.getRandom().nextDouble() * Math.PI * 2, r = 3 + b.getRandom().nextDouble() * 5;
                    b.spots.add(p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
                }
                if (b.spots.size() >= 12) break;
            }
            if (b.spots.isEmpty()) for (int i = 0; i < 4; i++) b.spots.add(tpos(b).add(b.getRandom().nextGaussian() * 3, 0, b.getRandom().nextGaussian() * 3));
            Fx.rune(sl, null, b.position().add(0, b.getBbHeight() + 3, 0), 4f, DEEP, 30, 7);
            b.playSound(SoundEvents.BEACON_POWER_SELECT, 2.5f, 0.6f);
        }
        int n = b.spots.size();
        for (int i = 0; i < n; i++) {
            Vec3 s = b.spots.get(i);
            if (t == i * CM_GAP) Fx.telegraphCircle(sl, null, s, 3f, CM_DELAY, ICE);
            if (t == i * CM_GAP + CM_DELAY - 6) {
                Vec3 sky = s.add(b.aim.x * -6, 20, b.aim.z * -6);
                Fx.ribbon(sl, null, sky, s.add(0, 0.3, 0), 0.4, 6, 0, 0.6f, PALE);
            }
            if (t == i * CM_GAP + CM_DELAY) {
                for (Player p : players(sl, s, 3))
                    if (flat2(p.position(), s) <= 9 && b.strike(p, b.moveDamage(mult(b.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
                Fx.groundCrack(sl, null, s, 3f, ICE, 6, 30);
                sl.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, s.x, s.y + 1, s.z, 30, 1, 1, 1, 0.1);
                b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 1.3f);
            }
        }
        return t < Math.max(1, n) * CM_GAP + CM_DELAY + 8;
    }

    // ---- Huyễn Ảnh Băng Thân: 2 phân thân hai bên mục tiêu, bắn tia chéo, rồi vỡ vụn
    private static boolean phantoms(ServerLevel sl, BangNhan b, int t) {
        if (t == 0) {
            b.triggerAnim("skill", "phase");
            b.points.clear();
            Vec3 c = tpos(b), side = new Vec3(-b.aim.z, 0, b.aim.x);
            b.points.add(c.add(side.scale(8)).add(b.aim.scale(-2)));
            b.points.add(c.add(side.scale(-8)).add(b.aim.scale(2)));
            for (Vec3 q : b.points) {
                Fx.rune(sl, null, q.add(0, 0.1, 0), 1.8f, PALE, 40, 6);
                Fx.ribbonHelix(sl, null, q, q.add(0, 4, 0), 0.8, 0, 20, 0, 0.2f, ICE);
                Vec3 dir = c.subtract(q).multiply(1, 0, 1).normalize();
                Fx.telegraphLine(sl, null, q.add(0, 0.05, 0), dir.scale(18), 2.2f, 22, ICE);
            }
            b.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2.5f, 0.7f);
        }
        if (t < 22 && t % 3 == 0)
            for (Vec3 q : b.points) sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 2, q.z, 8, 0.4, 1.2, 0.4, 0.01);
        if (t >= 22 && t < 32) {
            if (t == 22) { b.hitOnce.clear(); b.playSound(SoundEvents.WARDEN_SONIC_BOOM, 2f, 1.7f); }
            Vec3 c = tpos(b);
            for (int i = 0; i < b.points.size(); i++) {
                Vec3 q = b.points.get(i);
                Vec3 dir = b.spots.size() > i ? b.spots.get(i) : null;
                if (t == 22) { dir = c.subtract(q).multiply(1, 0, 1).normalize(); if (b.spots.size() <= i) b.spots.add(dir); else b.spots.set(i, dir); }
                beamFx(sl, q.add(0, 2, 0), q.add(dir.scale(18)).add(0, 1, 0));
                hitLine(sl, b, q, dir, 18, 1.3, mult(b.mv), Hit.SLOW);
            }
        }
        if (t == 36) {
            for (Vec3 q : b.points) {
                for (Player p : players(sl, q, 3))
                    if (flat2(p.position(), q) <= 9 && b.strike(p, b.moveDamage(mult(b.mv) * 0.6))) PhaseMoves.applyHit(p, Hit.FREEZE);
                Fx.elem(sl, null, Fx.E_SPIKES, q, Vec3.ZERO, 1.5f, PALE, 20);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, q.x, q.y + 2, q.z, 40, 0.6, 1.5, 0.6, 0.1);
            }
            b.playSound(SoundEvents.GLASS_BREAK, 2.5f, 0.8f);
        }
        if (t == 0) b.spots.clear();
        return t < 46;
    }

    // ---- Hàn Nhãn Toàn Tuyến: 4 tia chữ thập; vạch 24 tick -> xoay 90 độ trong 40 tick
    static final double SPIN_LEN = 18;

    private static boolean spinLaser(ServerLevel sl, BangNhan b, int t) {
        if (t == 0) {
            b.triggerAnim("skill", "skill_a");
            b.from = b.position();
            for (int i = 0; i < 4; i++) Fx.telegraphLine(sl, null, b.from.add(0, 0.05, 0), rot(b.aim, i * 90).scale(SPIN_LEN), 2.2f, 24, ICE);
            Fx.elem(sl, null, Fx.E_HALOS, eye(b), Vec3.ZERO, 1.8f, ICE, 64);
            b.playSound(SoundEvents.BEACON_ACTIVATE, 2.5f, 1.2f);
        }
        if (t >= 24 && t < 64) {
            double ang = (t - 24) * 90.0 / 40;
            if ((t - 24) % 8 == 0) b.hitOnce.clear();
            if (t % 4 == 0)  // vạch chạy trước tia ~15 độ: hướng sẽ quét tới
                for (int i = 0; i < 4; i++) Fx.telegraphLine(sl, null, b.from.add(0, 0.05, 0), rot(b.aim, i * 90 + ang + 15).scale(SPIN_LEN), 1.4f, 5, DANGER);
            for (int i = 0; i < 4; i++) {
                Vec3 dir = rot(b.aim, i * 90 + ang);
                if (t % 2 == 0) beamFx(sl, eye(b), b.from.add(dir.scale(SPIN_LEN)).add(0, 1, 0));
                hitLine(sl, b, b.from, dir, SPIN_LEN, 1.2, mult(b.mv), Hit.FREEZE);
            }
            if (t % 10 == 0) b.playSound(SoundEvents.BEACON_AMBIENT, 2.5f, 1.8f);
        }
        return t < 70;
    }

    // ---- Băng Nguyệt (tối thượng): bay lên 30 tick -> trăng băng giáng (vòng 18, 60 tick) -> đóng băng diện rộng
    public static final int MOON_RISE = 30, MOON_HIT = 90, MOON_END = 104;
    public static final double MOON_RAD = 18;

    private static boolean fullMoon(ServerLevel sl, BangNhan b, int t) {
        if (t == 0) {
            b.setNoGravity(true);
            b.triggerAnim("skill", "phase");
            Fx.elem(sl, null, Fx.E_HALOS, eye(b), Vec3.ZERO, 3f, PALE, 0);
            b.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < MOON_RISE) {
            b.setDeltaMovement(0, 0.2, 0);
            b.hurtMarked = true;
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX(), b.getY() + 1, b.getZ(), 14, 2, 1, 2, 0.03);
            return true;
        }
        if (t == MOON_RISE) {
            b.setDeltaMovement(Vec3.ZERO);
            b.spot = tpos(b);
            Fx.telegraphCircle(sl, null, b.spot, (float) MOON_RAD, MOON_HIT - MOON_RISE, DANGER);
            Fx.rune(sl, null, b.spot.add(0, 0.1, 0), (float) MOON_RAD * 0.6f, ICE, MOON_HIT - MOON_RISE, 10);
            Fx.shake(sl, b.spot, 1f, MOON_HIT - MOON_RISE, 40f);
        }
        if (t > MOON_RISE && t < MOON_HIT) {
            b.setDeltaMovement(Vec3.ZERO);
            double f = (t - MOON_RISE) / (double) (MOON_HIT - MOON_RISE);
            Vec3 moon = b.spot.add(0, 26 - f * 22, 0);
            if (t % 5 == 0) {
                Fx.rune(sl, null, moon, 6f, PALE, 6, 12);       // đĩa trăng tròn hạ dần
                Fx.groundRing(sl, null, b.spot, (float) (MOON_RAD * (1 - f * 0.3)), ICE, 6);
            }
            if (t % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, moon.x, moon.y, moon.z, 10, 3, 3, 3, 0.02);
            if (t % 8 == 0) b.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 0.5f + (float) f);
            return true;
        }
        if (t == MOON_HIT) {
            Vec3 s = b.spot;
            for (Player p : players(sl, s, MOON_RAD))
                if (flat2(p.position(), s) <= MOON_RAD * MOON_RAD && b.strike(p, b.moveDamage(mult(b.mv)))) {
                    PhaseMoves.applyHit(p, Hit.FREEZE);
                    p.setTicksFrozen(p.getTicksRequiredToFreeze() + 200);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
                }
            moonBurst(sl, s, MOON_RAD);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.6f);
            b.playSound(SoundEvents.GLASS_BREAK, 4f, 0.3f);
        }
        if (t == MOON_HIT + 4) b.setNoGravity(false);
        return t < MOON_END;
    }

    static void moonBurst(ServerLevel sl, Vec3 s, double rad) {
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5;
            Fx.ribbonWave(sl, null, s.add(0, 0.6, 0), s.add(Math.cos(a) * rad, 0.4, Math.sin(a) * rad), 2.0, 0.8, 12, 0, 0.7f, ICE);
        }
        Fx.groundCrack(sl, null, s, (float) rad, ICE, 16, 60);
        Fx.groundRing(sl, null, s, (float) rad, PALE, 30);
        Fx.elem(sl, null, Fx.E_NUKE, s, Vec3.ZERO, (float) rad * 0.4f, PALE, 0);
        Fx.elem(sl, null, Fx.E_SPIKES, s, Vec3.ZERO, (float) rad * 0.3f, ICE, 40);
        Fx.shake(sl, s, 4f, 30, 60f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 1, s.z, 2, 1, 0.2, 1, 0);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, s.x, s.y + 2, s.z, 200, rad * 0.4, 2, rad * 0.4, 0.2);
    }

    // ------------------------------------------------------------------ trợ giúp
    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 6, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    /** Trúng người chơi trên đoạn thẳng (mỗi người 1 lần cho tới khi hitOnce bị xoá). */
    private static void hitLine(ServerLevel sl, BangNhan b, Vec3 from, Vec3 dir, double len, double half, double mul, Hit h) {
        for (Player p : players(sl, from, len)) {
            Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > half) continue;
            if (b.hitOnce.add(p.getUUID()) && b.strike(p, b.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void beamFx(ServerLevel sl, Vec3 a, Vec3 c) {
        int n = (int) Math.max(4, a.distanceTo(c) * 1.5);
        for (int i = 0; i <= n; i++) {
            Vec3 q = a.lerp(c, i / (double) n);
            sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.0);
            if (i % 3 == 0) sl.sendParticles(ParticleTypes.SNOWFLAKE, q.x, q.y, q.z, 1, 0.15, 0.15, 0.15, 0.01);
        }
    }

    /** Lộ diện pha: P2 con mắt mở thêm vòng sương lam; P3 bầu trời đông kết, giáp vai vỡ. */
    static void transformFx(ServerLevel sl, BangNhan b, int ph) {
        b.triggerAnim("skill", "phase");
        Fx.elem(sl, null, Fx.E_HALOS, eye(b), Vec3.ZERO, ph >= 3 ? 3.5f : 2.5f, ph >= 3 ? PALE : ICE, 0);
        Fx.groundRing(sl, null, b.position(), ph >= 3 ? 14f : 10f, ICE, 40);
        Fx.elem(sl, null, Fx.E_SPIKES, b.position(), Vec3.ZERO, ph >= 3 ? 3f : 2f, ICE, 40);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX(), b.getY() + 2, b.getZ(), 120, 3, 3, 3, 0.1);
        b.playSound(ph >= 3 ? SoundEvents.WARDEN_ROAR : SoundEvents.BEACON_POWER_SELECT, 3f, ph >= 3 ? 1.3f : 0.6f);
    }

    // ------------------------------------------------------------------ cutscene
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: từ trên cao nhìn xuống vùng tuyết -> hạ xuống trước mặt -> cận con mắt băng. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 2.0, 9.0, 10.0, 75, 3.0, 20), k(T * 2 / 5, -5.0, 2.5, 6.0, 62, 3.5, 0),
                k(T * 3 / 4, 0.6, 3.6, 3.0, 42, 3.8, 0), k(T, 0, 3.4, 7.0, 64, 3.2, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.bang_nhan"), new SubLine(36, 72, "hukhong.dialog.bangnhan.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.bangnhan.intro.2"));
    }

    static final int ULT_T = 64;

    /** Tối thượng: camera thấp nhìn lên Băng Nhãn bay lên, trăng tròn băng hiện sau lưng. */
    private static void ultScene(ServerLevel sl, BangNhan b) {
        double s = Math.max(1.0, b.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 3.0 * s, 0.4 * s, 5.0 * s, 62, 2.5 * s, -8), k(ULT_T / 2, -2.0 * s, 0.3 * s, 5.5 * s, 70, 5.0 * s, -22),
                k(ULT_T, 0, 1.0 * s, 10.0 * s, 80, 7.0 * s, -20));
        BossPhaseDirector.sendCutscene(sl, b, ULT_T, cam, List.of(new SubLine(2, 30, "hukhong.dialog.bangnhan.ult.1"),
                new SubLine(30, ULT_T - 2, "hukhong.dialog.bangnhan.ult.2")));
        Fx.rune(sl, null, b.position().add(0, b.getBbHeight() + 4, 0), 7f, PALE, ULT_T + 30, 12);
    }

    /** Tử trận: con mắt băng vỡ, tuyết rơi lặng. */
    static void deathScene(ServerLevel sl, BangNhan b) {
        int T = 100;
        Vec3 at = b.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 3.0, 5.0, 55, 2.8, 0), k(50, 3.0, 3.0, 4.0, 60, 2.0, 0), k(T, 0, 7.0, 10.0, 72, 1.5, -10));
        BossPhaseDirector.sendCutscene(sl, null, at, b.yBodyRot, T, cam, List.of(new SubLine(6, 36, "hukhong.dialog.bangnhan.death.1"),
                new SubLine(36, 68, "hukhong.dialog.bangnhan.death.2"), new SubLine(68, T - 4, "hukhong.dialog.bangnhan.death.3")));
        Fx.groundRing(sl, null, at, 10f, ICE, 60);
        Fx.elem(sl, null, Fx.E_SPIKES, at, Vec3.ZERO, 2.5f, PALE, 60);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 3, at.z, 220, 4, 3, 4, 0.03);
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 3, at.z, 60, 1.5, 1.5, 1.5, 0.05);
    }

}
