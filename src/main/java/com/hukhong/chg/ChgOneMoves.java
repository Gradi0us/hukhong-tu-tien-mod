package com.hukhong.chg;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Bộ chiêu riêng của Chấp Hành Giả I "Thạch Hống" (kiểu Godfrey / Hoarah Loux: rìu, dậm đất, vật lộn).
 * Mỗi chiêu là dòng thời gian theo tick trong {@link #step}; mọi chiêu có telegraph trước khi gây sát thương.
 * <ul>
 *   <li>chg1_riu_giang (Phủ Giáng): bổ rìu dọc, vạch thẳng 10 block -> sóng nứt.</li>
 *   <li>chg1_dap_dat (Chấn Địa): dậm 2 nhịp, vòng trong r6 rồi vành ngoài 6..12 (đứng sát thân sau nhịp 1 để né nhịp 2).</li>
 *   <li>chg1_phi_phu (Phi Thân Phủ Kích): nhảy lên, bổ xuống vòng đánh dấu.</li>
 *   <li>chg1_hoanh_tao (Hoành Tảo Thiên Quân): 2 nhát quét quạt + bổ dọc chốt.</li>
 *   <li>chg1_dia_liet (Địa Liệt Tuyến): 3 khe nứt hình quạt, phun đá trễ.</li>
 *   <li>chg1_cam_na (Hống Thủ Cầm Nã): vồ thẳng; trúng => nhấc bổng rồi quật xuống đất (Hoarah Loux).</li>
 *   <li>chg1_ho_bo (Hổ Bộ Liên Chấn): 3 vòng dậm lan dần r5 -> r10 -> r15.</li>
 *   <li>chg1_nem_da (Liệt Thạch Phao): xé tảng đá ném 2 lần vào điểm đánh dấu.</li>
 *   <li>chg1_chan_thien (Chấn Thiên Liệt Địa, tối thượng): vọt lên, giáng xuống, cả đấu trường r18 nứt — NHẢY (đang trên không) để né.</li>
 * </ul>
 */
public final class ChgOneMoves {
    private ChgOneMoves() {}

    public static final int EARTH = 0xE0B060, MAGMA = 0xFF7A20, ROAR = 0xFFD080, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("chg1_riu_giang", "chg1_dap_dat", "chg1_phi_phu", "chg1_hoanh_tao", "chg1_dia_liet",
            "chg1_cam_na", "chg1_ho_bo", "chg1_nem_da", "chg1_chan_thien");

    public static boolean handles(String id) { return IDS.contains(id); }

    /** AI riêng (director chen thêm chiêu phase + tối thượng). */
    static String pick(RandomSource rnd, int phase, float dist) {
        if (dist > 10 && rnd.nextBoolean()) return phase >= 3 && rnd.nextBoolean() ? "chg1_nem_da" : "chg1_phi_phu";
        List<String> pool = phase >= 3
                ? List.of("chg1_riu_giang", "chg1_cam_na", "chg1_ho_bo", "chg1_hoanh_tao", "chg1_dia_liet", "chg1_dap_dat")
                : phase == 2
                ? List.of("chg1_riu_giang", "chg1_cam_na", "chg1_hoanh_tao", "chg1_dia_liet", "chg1_dap_dat")
                : List.of("chg1_riu_giang", "chg1_dap_dat", "chg1_phi_phu");
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "chg_1". */
    static double mult(String id) {
        return switch (id) {
            case "chg1_riu_giang" -> 1.3;
            case "chg1_dap_dat" -> 1.0;
            case "chg1_phi_phu" -> 1.4;
            case "chg1_hoanh_tao" -> 0.9;
            case "chg1_dia_liet" -> 1.0;
            case "chg1_cam_na" -> 1.8;
            case "chg1_ho_bo" -> 0.9;
            case "chg1_nem_da" -> 1.2;
            case "chg1_chan_thien" -> 2.4;
            default -> 1.0;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, ChapHanhGia d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("chg1_chan_thien");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.RAVAGER_ROAR, 2f, ult ? 0.5f : 0.8f);
        if (ult) ultScene(sl, d);
    }

    private static Vec3 tpos(ChapHanhGia d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aim.scale(6));
    }

    private static void reaim(ChapHanhGia d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aim = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aim.x, d.aim.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    /** Mỗi tick; false = chiêu xong. */
    static boolean step(ServerLevel sl, ChapHanhGia d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "chg1_riu_giang" -> axeSlam(sl, d, t);
            case "chg1_dap_dat" -> stomp(sl, d, t);
            case "chg1_phi_phu" -> leap(sl, d, t);
            case "chg1_hoanh_tao" -> sweep(sl, d, t);
            case "chg1_dia_liet" -> fissures(sl, d, t);
            case "chg1_cam_na" -> grab(sl, d, t);
            case "chg1_ho_bo" -> lionStomp(sl, d, t);
            case "chg1_nem_da" -> boulder(sl, d, t);
            case "chg1_chan_thien" -> earthshaker(sl, d, t);
            default -> false;
        };
    }

    // ---- Phủ Giáng: vạch 22 tick -> bổ, sóng nứt dọc vạch
    private static boolean axeSlam(ServerLevel sl, ChapHanhGia d, int t) {
        double len = 10;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.aim.scale(len), 3f, 22, DANGER);
            d.triggerAnim("skill", "skill_a");
            sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 3, d.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        }
        if (t == 22) {
            lineHit(sl, d, d.from, d.aim, len, 1.5, mult(d.mv), Hit.KNOCK_UP);
            Fx.rift(sl, null, d.from.add(0, 0.3, 0), d.aim.scale(len), 0.8f, MAGMA, 30);
            Fx.groundCrack(sl, null, d.from.add(d.aim.scale(3)), 3.5f, EARTH, 6, 40);
            Fx.shake(sl, d.position(), 2f, 12, 28f);
            dust(sl, d.from.add(d.aim.scale(len * 0.5)), len * 0.4);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.7f);
        }
        return t < 32;
    }

    // ---- Chấn Địa: nhịp 1 r6 (t18), nhịp 2 vành 6..12 (t34)
    private static boolean stomp(ServerLevel sl, ChapHanhGia d, int t) {
        Vec3 c = d.position();
        if (t == 0) { Fx.telegraphCircle(sl, null, c, 6f, 18, DANGER); d.triggerAnim("skill", "skill_b"); }
        if (t == 18) {
            ringHit(sl, d, c, 0, 6, mult(d.mv), Hit.KNOCK_UP);
            stompFx(sl, d, c, 6);
            Fx.telegraphCircle(sl, null, c, 12f, 16, DANGER);
        }
        if (t == 34) {
            ringHit(sl, d, c, 6, 12, mult(d.mv) * 0.8, Hit.SLOW);
            Fx.groundRing(sl, null, c, 12f, EARTH, 24);
            Fx.elem(sl, null, Fx.E_SPIKES, c, Vec3.ZERO, 10f, EARTH, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.9f);
        }
        return t < 42;
    }

    // ---- Phi Thân Phủ Kích: bật lên -> lơ lửng trên điểm đánh dấu -> bổ xuống t30
    private static boolean leap(ServerLevel sl, ChapHanhGia d, int t) {
        double rad = 4.5;
        if (t == 0) {
            reaim(d);
            d.setNoGravity(true);
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) rad, 30, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), 3f, EARTH, 30, 4);
            d.triggerAnim("skill", "skill_c");
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, d.getX(), d.getY(), d.getZ(), 16, 1, 0.1, 1, 0.03);
        }
        if (t < 10) { d.setDeltaMovement(0, 0.9, 0); d.hurtMarked = true; }
        else if (t == 20) { d.teleportTo(d.spot.x, d.spot.y + 9, d.spot.z); d.setDeltaMovement(Vec3.ZERO); }
        else if (t > 10 && t < 30) d.setDeltaMovement(Vec3.ZERO);
        if (t == 30) {
            Vec3 s = d.spot;
            Fx.ribbon(sl, null, d.position(), s, 0, 3, 0, 0.8f, MAGMA);
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            d.setNoGravity(false);
            ringHit(sl, d, s, 0, rad, mult(d.mv), Hit.KNOCK_UP);
            stompFx(sl, d, s, rad);
        }
        return t < 38;
    }

    // ---- Hoành Tảo Thiên Quân: quét trái (t14), quét phải (t28), bổ dọc chốt (t46)
    private static boolean sweep(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 6.5f, 75, 14); }
        if (t == 14 || t == 28) {
            reaim(d);
            lunge(d, 0.6);
            d.triggerAnim("skill", "attack");
            coneHit(sl, d, 6.5, 75, mult(d.mv), t == 14 ? 40 : -40);
            if (t == 14) Fx.telegraphCone(sl, null, d.position(), d.aim, 6.5f, 75, 14);
            else { d.from = d.position(); Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.aim.scale(12), 2.6f, 18, DANGER); }
        }
        if (t == 46) {
            d.triggerAnim("skill", "skill_a");
            lineHit(sl, d, d.from, d.aim, 12, 1.3, mult(d.mv) * 1.4, Hit.KNOCK_UP);
            Fx.rift(sl, null, d.from.add(0, 0.3, 0), d.aim.scale(12), 1f, MAGMA, 30);
            Fx.groundCrack(sl, null, d.from.add(d.aim.scale(4)), 4f, EARTH, 8, 40);
            Fx.shake(sl, d.position(), 2.5f, 14, 30f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.2f, 0.6f);
        }
        return t < 54;
    }

    // ---- Địa Liệt Tuyến: 3 khe -30/0/+30 độ, phun đá t24
    private static boolean fissures(ServerLevel sl, ChapHanhGia d, int t) {
        double len = 16;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            for (int i = -1; i <= 1; i++) Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), rot(d.aim, i * 30).scale(len), 2.4f, 24, MAGMA);
            d.triggerAnim("skill", "skill_b");
            Fx.groundCrack(sl, null, d.position(), 3f, MAGMA, 3, 24);
        }
        if (t == 24) {
            d.hitOnce.clear();
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = rot(d.aim, i * 30);
                lineHit(sl, d, d.from, dir, len, 1.2, mult(d.mv), Hit.KNOCK_UP);
                Fx.rift(sl, null, d.from.add(0, 0.2, 0), dir.scale(len), 1.2f, MAGMA, 34);
                for (int k = 2; k <= len; k += 3) {
                    Vec3 q = d.from.add(dir.scale(k));
                    sl.sendParticles(ParticleTypes.LAVA, q.x, q.y + 0.3, q.z, 3, 0.3, 0.2, 0.3, 0);
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, q.x, q.y + 0.5, q.z, 2, 0.3, 0.5, 0.3, 0.02);
                }
            }
            Fx.shake(sl, d.position(), 2f, 12, 30f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.8f);
        }
        return t < 32;
    }

    // ---- Hống Thủ Cầm Nã: vạch 16 tick -> vồ (t16..20); trúng => nhấc bổng 20 tick, quật xuống t40
    private static boolean grab(ServerLevel sl, ChapHanhGia d, int t) {
        double len = 7;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(len));
            d.grabbed = null;
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.aim.scale(len), 2.2f, 16, ROAR);
            d.triggerAnim("skill", "skill_c");
        }
        if (t >= 16 && t < 20 && d.grabbed == null) {
            Vec3 q = d.from.lerp(d.to, (t - 15) / 4.0);
            d.teleportTo(q.x, q.y, q.z);
            sl.sendParticles(ParticleTypes.CLOUD, q.x, q.y + 0.3, q.z, 4, 0.5, 0.1, 0.5, 0.02);
            for (Player p : players(sl, q, 2.2))
                if (flat2(p.position(), q) <= 2.2 * 2.2 && Math.abs(p.getY() - q.y) < 3) { d.grabbed = p; d.playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.6f); break; }
        }
        if (d.grabbed == null) return t < 28;
        LivingEntity g = d.grabbed;
        if (!g.isAlive()) { d.grabbed = null; return false; }
        if (t < 40) {  // nhấc bổng trước mặt, gầm
            Vec3 h = d.position().add(d.aim.scale(1.6)).add(0, 3.2, 0);
            g.teleportTo(h.x, h.y, h.z);
            g.setDeltaMovement(Vec3.ZERO);
            g.hurtMarked = true;
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, h.x, h.y, h.z, 4, 0.4, 0.4, 0.4, 0.02);
            return true;
        }
        if (t == 40) {
            Vec3 s = d.position().add(d.aim.scale(2.2));
            g.teleportTo(s.x, s.y, s.z);
            d.triggerAnim("skill", "skill_a");
            if (d.strike(g, d.moveDamage(mult(d.mv))) && g instanceof Player p) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            stompFx(sl, d, s, 3.5);
            d.grabbed = null;
        }
        return t < 48;
    }

    // ---- Hổ Bộ Liên Chấn: 3 vòng lan r5 (t20) -> 5..10 (t32) -> 10..15 (t44)
    private static boolean lionStomp(ServerLevel sl, ChapHanhGia d, int t) {
        Vec3 c = d.spot == null ? (d.spot = d.position()) : d.spot;
        if (t == 0) { Fx.telegraphCircle(sl, null, c, 5f, 20, DANGER); d.triggerAnim("skill", "skill_b"); Fx.rune(sl, null, c.add(0, 0.1, 0), 4f, ROAR, 50, 5); }
        int[] at = {20, 32, 44};
        for (int i = 0; i < 3; i++) {
            if (t != at[i]) continue;
            double in = i * 5, out = (i + 1) * 5;
            ringHit(sl, d, c, in, out, mult(d.mv), Hit.KNOCK_UP);
            Fx.groundRing(sl, null, c, (float) out, i == 2 ? MAGMA : EARTH, 20);
            Fx.groundCrack(sl, null, c, (float) out, EARTH, 6 + i * 2, 30);
            Fx.shake(sl, c, 1.5f + i * 0.5f, 10, 30f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 1.0f - i * 0.15f);
            if (i < 2) Fx.telegraphCircle(sl, null, c, (float) (out + 5), 12, DANGER);
        }
        return t < 50;
    }

    // ---- Liệt Thạch Phao: 2 lần [xé đá + vòng đánh dấu 24 tick -> tảng đá rơi]
    private static boolean boulder(ServerLevel sl, ChapHanhGia d, int t) {
        int e = t % 26, k = t / 26;
        if (k >= 2) return t < 56;
        double rad = 3.5;
        if (e == 0) {
            reaim(d);
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) rad, 24, DANGER);
            Fx.elem(sl, null, Fx.E_BOULDER, d.position().add(d.aim.scale(1.5)).add(0, 3.5, 0), Vec3.ZERO, 1.6f, EARTH, 24);
            Fx.groundCrack(sl, null, d.position().add(d.aim.scale(1.5)), 2f, EARTH, 4, 20);
            d.triggerAnim("skill", "skill_c");
            d.playSound(SoundEvents.STONE_BREAK, 2.5f, 0.5f);
        }
        if (e == 14) {
            d.triggerAnim("skill", "attack");
            Fx.elem(sl, null, Fx.E_BOULDER, d.position().add(0, 3.5, 0), d.spot.subtract(d.position()), 1.6f, EARTH, 10);
        }
        if (e == 24) {
            Vec3 s = d.spot;
            ringHit(sl, d, s, 0, rad, mult(d.mv), Hit.SLOW);
            Fx.groundCrack(sl, null, s, (float) rad, EARTH, 6, 30);
            sl.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5, s.z, 3, 0.8, 0.3, 0.8, 0);
            dust(sl, s, rad);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.9f);
        }
        return true;
    }

    // ---- Chấn Thiên Liệt Địa (Earthshaker): vọt lên (t0..14), vòng r18 cảnh báo tới t80 -> giáng; dư chấn t92
    public static final int ES_IMPACT = 80, ES_END = 100;
    public static final double ES_RAD = 18;

    private static boolean earthshaker(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) {
            d.spot = d.position();
            d.setNoGravity(true);
            d.triggerAnim("skill", "phase");
            Fx.telegraphCircle(sl, null, d.spot, (float) ES_RAD, ES_IMPACT, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) ES_RAD * 0.5f, MAGMA, ES_IMPACT, 8);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 3f, ROAR, 0);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < 14) { d.setDeltaMovement(0, 0.6, 0); d.hurtMarked = true; return true; }
        if (t < ES_IMPACT - 6) {
            d.setDeltaMovement(Vec3.ZERO);
            if (t % 4 == 0) sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 1, d.getZ(), 12, 1.2, 1.2, 1.2, 0.05);
            if (t % 10 == 0) for (ServerPlayer p : sl.players())
                if (p.distanceToSqr(d.spot) < 30 * 30) p.displayClientMessage(Component.translatable("hukhong.chg1.jump_hint").withStyle(ChatFormatting.YELLOW), true);
            return true;
        }
        if (t == ES_IMPACT - 6) Fx.ribbon(sl, null, d.position(), d.spot, 0, 6, 0, 1.2f, MAGMA);
        if (t == ES_IMPACT) {
            Vec3 s = d.spot;
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            d.setNoGravity(false);
            for (Player p : players(sl, s, ES_RAD)) {  // đang trên không (nhảy) => né được
                if (flat2(p.position(), s) > ES_RAD * ES_RAD || !p.onGround()) continue;
                if (d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            }
            Fx.groundCrack(sl, null, s, (float) ES_RAD, MAGMA, 16, 80);
            Fx.groundRing(sl, null, s, (float) ES_RAD, EARTH, 40);
            Fx.elem(sl, null, Fx.E_NUKE, s, Vec3.ZERO, (float) ES_RAD * 0.35f, MAGMA, 0);
            Fx.elem(sl, null, Fx.E_SPIKES, s, Vec3.ZERO, (float) ES_RAD * 0.8f, EARTH, 0);
            Fx.shake(sl, s, 4.5f, 30, 60f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 1, s.z, 3, 2, 0.3, 2, 0);
            dust(sl, s, ES_RAD);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.4f);
            d.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.5f);
            Fx.telegraphCircle(sl, null, s, 8f, 12, DANGER);  // dư chấn
        }
        if (t == ES_IMPACT + 12) {
            ringHit(sl, d, d.spot, 0, 8, 0.6, Hit.SLOW);
            Fx.groundRing(sl, null, d.spot, 8f, MAGMA, 20);
        }
        return t < ES_END;
    }

    // ------------------------------------------------------------------ trợ giúp
    private static void lunge(ChapHanhGia d, double v) { d.setDeltaMovement(d.aim.x * v, 0.1, d.aim.z * v); d.hurtMarked = true; }

    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static void ringHit(ServerLevel sl, ChapHanhGia d, Vec3 c, double in, double out, double mul, Hit h) {
        for (Player p : players(sl, c, out)) {
            double f = flat2(p.position(), c);
            if (f > out * out || (in > 0 && f < in * in) || Math.abs(p.getY() - c.y) > 3) continue;
            if (d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void lineHit(ServerLevel sl, ChapHanhGia d, Vec3 from, Vec3 dir, double len, double half, double mul, Hit h) {
        for (Player p : players(sl, from, len)) {
            Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > half) continue;
            if (d.hitOnce.add(p.getUUID()) && d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void coneHit(ServerLevel sl, ChapHanhGia d, double rad, double halfDeg, double mul, double roll) {
        double cos = Math.cos(Math.toRadians(halfDeg));
        Vec3 c = d.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(d.aim) < cos)) continue;
            if (d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, Hit.NONE);
        }
        Fx.qi(sl, c.add(0, 1.6, 0), d.aim.scale(rad * 0.8), (float) (rad * 0.6), EARTH, roll, 10);
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
    }

    private static void dust(ServerLevel sl, Vec3 c, double r) {
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.5, c.z, (int) Math.min(80, 10 + r * 4), r * 0.4, 0.4, r * 0.4, 0.03);
    }

    private static void stompFx(ServerLevel sl, ChapHanhGia d, Vec3 c, double r) {
        Fx.groundCrack(sl, null, c, (float) r, EARTH, 8, 40);
        Fx.groundRing(sl, null, c, (float) r * 1.2f, EARTH, 20);
        Fx.shake(sl, c, 2.5f, 14, 32f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
        dust(sl, c, r);
        d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
    }

    /** Lộ diện pha (khi đang chuyển pha): P2 Hoarah Loux trút giáp gầm vang, P3 đất nứt dung nham quanh thân. */
    static void transformFx(ServerLevel sl, ChapHanhGia d, int ph) {
        d.triggerAnim("skill", "phase");
        Vec3 c = d.position();
        if (ph == 2) {
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 2, 0), Vec3.ZERO, 3f, ROAR, 0);
            Fx.groundRing(sl, null, c, 10f, ROAR, 40);
            sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 2, c.z, 80, 2, 1.5, 2, 0.15);
            d.playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
            Fx.shake(sl, c, 3f, 24, 40f);
        } else if (ph >= 3) {
            Fx.groundCrack(sl, null, c, 12f, MAGMA, 12, 80);
            Fx.elem(sl, null, Fx.E_SPIKES, c, Vec3.ZERO, 8f, EARTH, 0);
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 60, 4, 0.5, 4, 0);
            d.playSound(SoundEvents.WARDEN_ROAR, 3f, 0.7f);
            Fx.shake(sl, c, 3.5f, 30, 40f);
        }
    }

    // ------------------------------------------------------------------ cutscene
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: góc thấp nhìn lên chiếc rìu -> vòng ra trước -> cận mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 3.0, 0.4, 5.0, 70, 3.5, -14), k(T * 2 / 5, -3.5, 1.2, 3.5, 60, 2.8, -4),
                k(T * 3 / 4, 0.6, 2.6, 2.8, 45, 3.0, 0), k(T, 0, 2.8, 6.5, 64, 2.6, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.chg_1"), new SubLine(36, 72, "hukhong.dialog.chg1.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.chg1.intro.2"));
    }

    static final int ULT_T = 60;

    private static void ultScene(ServerLevel sl, ChapHanhGia d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 3.0 * s, 0.3 * s, 4.0 * s, 62, 1.8 * s, -8), k(ULT_T / 2, -2.0 * s, 0.2 * s, 4.5 * s, 72, 5.0 * s, -22),
                k(ULT_T, 0, 1.2 * s, 9.0 * s, 82, 6.0 * s, -16));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 28, "hukhong.dialog.chg1.ult.1"),
                new SubLine(28, ULT_T - 2, "hukhong.dialog.chg1.ult.2")));
    }

    /** Tử trận: cutscene tĩnh tại chỗ ngã, rìu cắm xuống đất. */
    static void deathScene(ServerLevel sl, ChapHanhGia d) {
        int T = 90;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.8, 4.5, 55, 1.4, 0), k(45, 3.0, 2.2, 3.0, 60, 1.2, 0), k(T, 0, 5.0, 9.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 46, "hukhong.dialog.chg1.death.1"),
                new SubLine(46, T - 4, "hukhong.dialog.chg1.death.2")));
        Fx.groundCrack(sl, null, at, 6f, EARTH, 8, 80);
        Fx.groundRing(sl, null, at, 8f, ROAR, 60);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 1, at.z, 60, 2, 1, 2, 0.02);
    }
}
