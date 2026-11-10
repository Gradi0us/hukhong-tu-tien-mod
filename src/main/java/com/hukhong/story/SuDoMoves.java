package com.hukhong.story;

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
 * Bộ chiêu riêng của Sứ Đồ Dạ Khuyết (kiểu Margit — kẻ gác cổng: trượng nhanh, nhịp trễ, vũ khí hư-kim triệu ra ít nhưng nặng, hay chế nhạo).
 * Khác CHG III (Morgott, c3_*): không vũng máu/gai, phi đao ít hơn, chùy/kiếm hiện một lần mỗi chiêu. Mỗi chiêu = dòng thời gian theo tick trong {@link #step}.
 * <ul>
 *   <li>sd_truong_kich — Trượng Kích Liên Hoàn: 2 nhát trượng nhanh + nhát bổ TRỄ.</li>
 *   <li>sd_phi_dao — Kim Hư Phi Đao: 3 phi đao hư-kim khoá mục tiêu lần lượt, đao cuối trễ.</li>
 *   <li>sd_thanh_chuy — Hư Kim Chùy: triệu chùy, giơ cao, giáng trễ trước mặt.</li>
 *   <li>sd_khieu_khich — Khiêu Khích: đứng cười nhạo (cửa sổ phạt) rồi đâm trượng lướt.</li>
 *   <li>sd_kiem_hien — Kiếm Hiện Giữa Chiêu (P3): 2 nhát trượng, kiếm hư-kim hiện quét quạt rộng.</li>
 *   <li>sd_dao_vu — Phi Đao Vũ (P3): quạt 3 đao, rồi 2 đao trễ khoá lại mục tiêu.</li>
 *   <li>sd_nhay_truong — Phi Thân Trượng (P3): nhảy bổ trượng xuống chỗ mục tiêu.</li>
 *   <li>sd_thien_chuy — Thiên Chùy Trấn Môn (tối thượng): bay lên, cự chùy hiện, giáng + sóng nhẫn lan 15 block (nhảy để né sóng).</li>
 * </ul>
 */
public final class SuDoMoves {
    private SuDoMoves() {}

    public static final int GOLD = 0xFFC844, PALE = 0xF4E3A0, VOIDG = 0xB088FF, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("sd_truong_kich", "sd_phi_dao", "sd_thanh_chuy", "sd_khieu_khich",
            "sd_thien_chuy", "sd_kiem_hien", "sd_dao_vu", "sd_nhay_truong");

    public static boolean handles(String id) { return IDS.contains(id); }

    // ------------------------------------------------------------------ AI riêng (pha 1 cũng dùng)
    static String pick(RandomSource rnd, int phase, boolean ultReady, float dist) {
        if (phase >= 3 && ultReady && rnd.nextInt(5) == 0) return "sd_thien_chuy";
        if (dist > 8) {
            if (phase >= 3 && rnd.nextBoolean()) return rnd.nextBoolean() ? "sd_dao_vu" : "sd_nhay_truong";
            return rnd.nextInt(3) == 0 ? "sd_khieu_khich" : "sd_phi_dao";
        }
        List<String> pool = phase >= 3 ? List.of("sd_truong_kich", "sd_kiem_hien", "sd_thanh_chuy", "sd_khieu_khich")
                : phase == 2 ? List.of("sd_truong_kich", "sd_thanh_chuy", "sd_khieu_khich", "sd_truong_kich")
                : List.of("sd_truong_kich", "sd_khieu_khich", "sd_truong_kich");
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "su_do". */
    static double mult(String id) {
        return switch (id) {
            case "sd_truong_kich" -> 0.8;
            case "sd_phi_dao" -> 0.6;
            case "sd_thanh_chuy" -> 1.4;
            case "sd_khieu_khich" -> 1.1;
            case "sd_kiem_hien" -> 1.2;
            case "sd_dao_vu" -> 0.6;
            case "sd_nhay_truong" -> 1.2;
            case "sd_thien_chuy" -> 2.2;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, SuDo d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("sd_thien_chuy");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 40 * 40) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, ult ? 0.5f : 1.1f);
        if (ult) { d.ultCd = 700; ultScene(sl, d); }
    }

    private static Vec3 tpos(SuDo d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aim.scale(5));
    }

    private static void reaim(SuDo d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aim = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aim.x, d.aim.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    // ------------------------------------------------------------------ mỗi tick; false = chiêu xong
    static boolean step(ServerLevel sl, SuDo d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "sd_truong_kich" -> caneCombo(sl, d, t);
            case "sd_phi_dao" -> daggers(sl, d, t);
            case "sd_thanh_chuy" -> hammer(sl, d, t);
            case "sd_khieu_khich" -> taunt(sl, d, t);
            case "sd_kiem_hien" -> swordMid(sl, d, t);
            case "sd_dao_vu" -> daggerFan(sl, d, t);
            case "sd_nhay_truong" -> leapCane(sl, d, t);
            case "sd_thien_chuy" -> skyHammer(sl, d, t);
            default -> false;
        };
    }

    // ---- Trượng Kích: nhát 1 (t8), nhát 2 (t15), nhát 3 trễ (t40)
    private static boolean caneCombo(ServerLevel sl, SuDo d, int t) {
        if (t == 0 || t == 9) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 4.5f, 55, 7); }
        if (t == 8 || t == 15) { cane(sl, d, 4.5, 55, t == 8 ? 30 : -30, 1.0); }
        if (t == 20) {
            reaim(d);
            Fx.telegraphCone(sl, null, d.position(), d.aim, 5.5f, 40, 20);
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 1.5f, GOLD, 20, 4);
            d.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.2f, 1.4f);
        }
        if (t == 40) {
            cane(sl, d, 5.5, 40, 90, 1.3);
            Fx.groundCrack(sl, null, d.position().add(d.aim.scale(3)), 2.5f, GOLD, 6, 25);
            Fx.shake(sl, d.position(), 1f, 8, 16f);
        }
        return t < 50;
    }

    private static void cane(ServerLevel sl, SuDo d, double rad, double half, double roll, double mul) {
        d.triggerAnim("skill", "attack");
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.8f);
        Fx.qi(sl, d.position().add(0, 1.2, 0).add(d.aim.scale(rad * 0.4)), d.aim.scale(rad * 0.8), (float) (rad * 0.6), PALE, roll, 8);
        hitCone(sl, d, rad, half, mul, Hit.NONE);
    }

    // ---- Kim Hư Phi Đao: 3 đao khoá lần lượt; phóng tại 22, 32, 50 (đao cuối trễ)
    private static final int[] DG_FIRE = {22, 32, 50};

    private static boolean daggers(ServerLevel sl, SuDo d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "cast");
            d.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.4f, 1.3f);
            for (int i = 0; i < 3; i++) Fx.rune(sl, null, conjure(d, i - 1), 0.5f, GOLD, 50, 3);
        }
        for (int i = 0; i < 3; i++) {
            int fire = DG_FIRE[i], tele = i == 2 ? 18 : 10;
            if (t == fire - tele) {
                reaim(d);
                d.from = conjure(d, i - 1);
                d.to = tpos(d).add(0, 1, 0).add(d.aim.scale(4));
                Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), d.to.subtract(d.from).multiply(1, 0, 1), 1.4f, tele, GOLD);
            }
            if (t == fire && d.from != null) {
                Fx.swordFly(sl, d.from, d.to, 6, 0, 0.6f);
                Fx.ribbon(sl, null, d.from, d.to, 0, 6, 0, 0.15f, GOLD);
                d.playSound(SoundEvents.TRIDENT_THROW.value(), 1.4f, 1.4f);
                hitLine(sl, d, d.from, d.to, 1.1, mult("sd_phi_dao"), Hit.SLOW);
            }
        }
        return t < 58;
    }

    /** Vị trí đao hư-kim lơ lửng trên vai (slot -1/0/1). */
    private static Vec3 conjure(SuDo d, int slot) {
        Vec3 right = new Vec3(-d.aim.z, 0, d.aim.x);
        return d.position().add(0, d.getBbHeight() + 0.6, 0).add(right.scale(slot * 0.9));
    }

    // ---- Hư Kim Chùy: triệu chùy, giáng trễ ở t30
    private static boolean hammer(ServerLevel sl, SuDo d, int t) {
        if (t == 0) {
            reaim(d);
            d.spot = d.position().add(d.aim.scale(3));
            d.triggerAnim("skill", "cast");
            Fx.telegraphCircle(sl, null, d.spot, 3f, 30, DANGER);
            Fx.giantSword(sl, null, d.spot, 1.6f, GOLD, 30, 44);
            sl.sendParticles(ParticleTypes.END_ROD, d.getX(), d.getY() + 2.5, d.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            d.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5f, 0.7f);
        }
        if (t == 30) {
            d.triggerAnim("skill", "attack");
            hitAround(sl, d, d.spot, 3, mult("sd_thanh_chuy"), Hit.KNOCK_UP);
            Fx.elem(sl, null, Fx.E_BOULDER, d.spot, Vec3.ZERO, 1.2f, GOLD, 0);
            Fx.groundCrack(sl, null, d.spot, 3.5f, GOLD, 8, 40);
            Fx.groundRing(sl, null, d.spot, 4f, PALE, 18);
            Fx.shake(sl, d.spot, 2f, 12, 24f);
            d.playSound(SoundEvents.ANVIL_LAND, 1.6f, 0.6f);
        }
        return t < 40;
    }

    // ---- Khiêu Khích: cười nhạo 0..20 (đứng yên, bị phạt được), đâm trượng lướt ở t32
    private static boolean taunt(ServerLevel sl, SuDo d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "cast");
            tauntLine(sl, d);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 1.6, 0), Vec3.ZERO, 1.2f, GOLD, 0);
            d.playSound(SoundEvents.EVOKER_CELEBRATE, 1.5f, 0.8f);
        }
        if (t == 20) {
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(7));
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), 1.8f, 12, GOLD);
        }
        if (t >= 32 && t < 36 && d.from != null) {
            Vec3 q = d.from.lerp(d.to, (t - 31) / 4.0);
            d.setPos(q.x, d.getY(), q.z);
            if (t == 32) { d.triggerAnim("skill", "attack"); d.playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.4f, 1.2f); }
            Fx.qi(sl, q.add(0, 1.1, 0), d.aim.scale(2), 1.0f, PALE, 0, 6);
            for (Player p : players(sl, q, 1.8)) if (flat2(p.position(), q) < 1.8 * 1.8 && d.hitOnce.add(p.getUUID()))
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult("sd_khieu_khich")))) PhaseMoves.applyHit(p, Hit.NONE);
        }
        return t < 42;
    }

    static void tauntLine(ServerLevel sl, SuDo d) {
        Component c = Component.translatable("hukhong.dialog.sudo.taunt." + (1 + d.getRandom().nextInt(4))).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(d) < 32 * 32) p.displayClientMessage(c, true);
    }

    // ---- Kiếm Hiện: 2 nhát trượng (t6, t12), kiếm hư-kim hiện ở t14, quét quạt rộng ở t32
    private static boolean swordMid(ServerLevel sl, SuDo d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 4.5f, 55, 6); }
        if (t == 6 || t == 12) { if (t == 12) reaim(d); cane(sl, d, 4.5, 55, t == 6 ? 25 : -25, 1.0); }
        if (t == 14) {
            reaim(d);
            Fx.telegraphCone(sl, null, d.position(), d.aim, 7f, 75, 18);
            Fx.giantSword(sl, null, d.position().add(d.aim.scale(2)).add(0, 3, 0), 1.3f, VOIDG, 18, 24);
            d.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.5f, 1.1f);
        }
        if (t == 32) {
            d.triggerAnim("skill", "attack");
            Fx.qi(sl, d.position().add(0, 1.3, 0).add(d.aim.scale(3)), d.aim.scale(6), 4.5f, VOIDG, 0, 12);
            Fx.ribbonWave(sl, null, d.position().add(rot(d.aim, -75).scale(6)).add(0, 1.2, 0), d.position().add(rot(d.aim, 75).scale(6)).add(0, 1.2, 0), 2.5, 0.6, 8, 0, 0.5f, GOLD);
            d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.5f);
            hitCone(sl, d, 7, 75, mult("sd_kiem_hien"), Hit.KNOCK_UP);
        }
        return t < 42;
    }

    // ---- Phi Đao Vũ: quạt 3 đao (t24), 2 đao khoá lại trễ (t46)
    private static boolean daggerFan(ServerLevel sl, SuDo d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "cast");
            reaim(d);
            for (int i = -2; i <= 2; i++) Fx.rune(sl, null, conjure(d, i), 0.45f, GOLD, 48, 3);
            for (int i = -1; i <= 1; i++) Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), rot(d.aim, i * 22).scale(14), 1.3f, 24, GOLD);
            d.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.5f, 1.1f);
        }
        if (t == 24) for (int i = -1; i <= 1; i++) throwDagger(sl, d, conjure(d, i), rot(d.aim, i * 22).scale(14));
        if (t == 30) { reaim(d); for (int i = -1; i <= 1; i += 2) Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), rot(d.aim, i * 8).scale(14), 1.3f, 16, DANGER); }
        if (t == 46) for (int i = -1; i <= 1; i += 2) throwDagger(sl, d, conjure(d, i * 2), rot(d.aim, i * 8).scale(14));
        return t < 54;
    }

    private static void throwDagger(ServerLevel sl, SuDo d, Vec3 from, Vec3 vec) {
        Vec3 to = d.position().add(0, 1, 0).add(vec);
        Fx.swordFly(sl, from, to, 6, 0, 0.6f);
        Fx.ribbon(sl, null, from, to, 0, 6, 0, 0.12f, GOLD);
        d.playSound(SoundEvents.TRIDENT_THROW.value(), 1.2f, 1.5f);
        hitLine(sl, d, d.position().add(0, 1, 0), to, 1.0, mult("sd_dao_vu"), Hit.SLOW);
    }

    // ---- Phi Thân Trượng: nhảy (t0..18) bổ xuống điểm đánh dấu
    private static boolean leapCane(ServerLevel sl, SuDo d, int t) {
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, 2.8f, 18, DANGER);
            d.setNoGravity(true);
            d.triggerAnim("skill", "cast");
            d.playSound(SoundEvents.GOAT_LONG_JUMP, 1.5f, 0.9f);
        }
        if (t > 0 && t <= 18) {
            double k = t / 18.0;
            Vec3 q = d.from.lerp(d.spot, k).add(0, Math.sin(k * Math.PI) * 4, 0);
            d.setPos(q.x, q.y, q.z);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 1, q.z, 2, 0.2, 0.2, 0.2, 0.01);
        }
        if (t == 18) {
            d.setNoGravity(false);
            d.triggerAnim("skill", "attack");
            hitAround(sl, d, d.spot, 2.8, mult("sd_nhay_truong"), Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, d.spot, 3f, GOLD, 6, 30);
            Fx.shake(sl, d.spot, 1.5f, 10, 20f);
            d.playSound(SoundEvents.MACE_SMASH_GROUND, 1.6f, 0.9f);
        }
        return t < 28;
    }

    // ---- Thiên Chùy Trấn Môn (tối thượng): bay lên (0..30), cự chùy hiện, giáng t60, sóng nhẫn 62..80
    public static final int UH_RISE = 30, UH_SLAM = 60, UH_END = 88;
    public static final double UH_CORE = 4, UH_WAVE = 15;

    private static boolean skyHammer(ServerLevel sl, SuDo d, int t) {
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.spot = tpos(d);
            d.setNoGravity(true);
            d.triggerAnim("skill", "cast");
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 3f, GOLD, UH_RISE, 6);
            d.playSound(SoundEvents.EVOKER_PREPARE_WOLOLO, 2f, 0.6f);
        }
        if (t > 0 && t <= UH_RISE) {
            Vec3 q = d.from.add(0, 6.0 * t / UH_RISE, 0);
            d.setPos(q.x, q.y, q.z);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y + 1, q.z, 4, 0.5, 0.5, 0.5, 0.02);
        }
        if (t == UH_RISE) {
            reaim(d);
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) UH_CORE, UH_SLAM - UH_RISE, DANGER);
            Fx.telegraphCircle(sl, null, d.spot, (float) UH_WAVE, UH_SLAM - UH_RISE + 18, GOLD);
            Fx.giantSword(sl, null, d.spot, 3.5f, GOLD, UH_SLAM - UH_RISE, UH_SLAM - UH_RISE + 30);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 1.5, 0), Vec3.ZERO, 2.5f, GOLD, 0);
            d.playSound(SoundEvents.BEACON_ACTIVATE, 2f, 0.6f);
        }
        if (t > UH_RISE && t < UH_SLAM && t % 4 == 0)
            sl.sendParticles(ParticleTypes.WAX_ON, d.getX(), d.getY() + 2.5, d.getZ(), 6, 0.8, 0.8, 0.8, 0.1);
        if (t == UH_SLAM) {
            d.setPos(d.spot.x, d.spot.y, d.spot.z);
            d.setNoGravity(false);
            d.triggerAnim("skill", "attack");
            hitAround(sl, d, d.spot, UH_CORE, mult("sd_thien_chuy"), Hit.KNOCK_UP);
            Fx.elem(sl, null, Fx.E_NUKE, d.spot, Vec3.ZERO, 1.8f, GOLD, 0);
            Fx.groundCrack(sl, null, d.spot, (float) UH_CORE + 2, GOLD, 12, 60);
            Fx.shake(sl, d.spot, 3.5f, 24, 40f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, d.spot.x, d.spot.y + 1, d.spot.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
            d.hitOnce.clear();
        }
        if (t > UH_SLAM && t <= UH_SLAM + 20) { // sóng nhẫn lan ra: đứng đất trong dải => trúng (nhảy để né)
            double r = UH_CORE + (UH_WAVE - UH_CORE) * (t - UH_SLAM) / 20.0;
            if (t % 4 == 1) Fx.groundRing(sl, null, d.spot, (float) r, PALE, 8);
            for (Player p : players(sl, d.spot, r + 1)) {
                double fd = Math.sqrt(flat2(p.position(), d.spot));
                if (fd < r - 1.2 || fd > r + 1.0 || !p.onGround() || !d.hitOnce.add(p.getUUID())) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(0.6))) PhaseMoves.applyHit(p, Hit.SLOW);
            }
        }
        return t < UH_END;
    }

    // ------------------------------------------------------------------ trợ giúp sát thương
    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    private static void hitAround(ServerLevel sl, SuDo d, Vec3 c, double r, double mul, Hit h) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void hitCone(ServerLevel sl, SuDo d, double r, double halfDeg, double mul, Hit h) {
        double cos = Math.cos(Math.toRadians(halfDeg));
        for (Player p : players(sl, d.position(), r)) {
            Vec3 v = p.position().subtract(d.position()).multiply(1, 0, 1);
            double len = v.length();
            if (len > r || Math.abs(p.getY() - d.getY()) > 3) continue;
            if (len > 0.8 && v.scale(1 / len).dot(d.aim) < cos) continue;
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    /** Phi đao nhanh (BossBrain): một đao thẳng vào mục tiêu — sau khi nhảy lùi, hoặc trừng phạt người chơi hồi máu. */
    static void quickDagger(ServerLevel sl, SuDo d, LivingEntity t) {
        Vec3 from = d.position().add(0, 1.4, 0), at = t.position().add(0, 1, 0);
        Vec3 dir = at.subtract(from).multiply(1, 0, 1);
        Vec3 to = at.add(dir.lengthSqr() < 1e-4 ? Vec3.ZERO : dir.normalize().scale(3));
        Fx.swordFly(sl, from, to, 4, 0, 0.5f);
        Fx.ribbon(sl, null, from, to, 0, 4, 0, 0.12f, GOLD);
        d.playSound(SoundEvents.TRIDENT_THROW.value(), 1.2f, 1.6f);
        hitLine(sl, d, from, to, 1.0, mult("sd_phi_dao"), Hit.SLOW);
        d.daggersThrown++;
    }

    private static void hitLine(ServerLevel sl, SuDo d, Vec3 a, Vec3 b, double w, double mul, Hit h) {
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
    /** Lộ diện pha: P2 trượng bừng hư-kim, P3 đao kiếm chùy hư-kim vây quanh. */
    static void transformFx(ServerLevel sl, SuDo d, int ph) {
        d.triggerAnim("skill", "cast");
        Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 1.5, 0), Vec3.ZERO, ph >= 3 ? 2.5f : 1.8f, GOLD, 0);
        Fx.groundRing(sl, null, d.position(), ph >= 3 ? 7f : 5f, GOLD, 30);
        if (ph >= 3) for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            Fx.giantSword(sl, null, d.position().add(Math.cos(a) * 3, 0, Math.sin(a) * 3), 1.0f, i % 2 == 0 ? GOLD : VOIDG, 20, 50);
        }
        sl.sendParticles(ParticleTypes.END_ROD, d.getX(), d.getY() + 1.5, d.getZ(), 50, 1.2, 1.2, 1.2, 0.08);
        d.playSound(SoundEvents.EVOKER_CELEBRATE, 2f, 0.6f);
    }

    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: kẻ gác cổng chống trượng chắn lối — camera thấp từ xa tiến dần vào mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 0, 0.4, 9.0, 70, 1.4, -6), k(T * 2 / 5, 2.4, 0.8, 4.0, 58, 1.5, -4),
                k(T * 3 / 4, -0.8, 1.6, 2.0, 46, 1.6, 0), k(T, 0, 1.8, 3.6, 62, 1.4, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 34, "entity.hukhong.su_do"), new SubLine(34, 72, "hukhong.dialog.sudo.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.sudo.intro.2"));
    }

    static final int ULT_T = 50;

    private static void ultScene(ServerLevel sl, SuDo d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 2.0 * s, 0.3 * s, 3.0 * s, 60, 1.2 * s, -6), k(ULT_T / 2, -1.2 * s, 0.2 * s, 3.5 * s, 68, 3.5 * s, -18),
                k(ULT_T, 0, 1.0 * s, 7.0 * s, 78, 5.0 * s, -16));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 24, "hukhong.dialog.sudo.ult.1"),
                new SubLine(24, ULT_T - 2, "hukhong.dialog.sudo.ult.2")));
    }

    static void deathScene(ServerLevel sl, SuDo d) {
        int T = 80;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.2, 3.2, 55, 1.0, 0), k(40, 2.0, 1.6, 2.4, 60, 0.8, 0), k(T, 0, 3.5, 6.5, 72, 0.8, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 42, "hukhong.dialog.sudo.death.1"),
                new SubLine(42, T - 4, "hukhong.dialog.sudo.death.2")));
        Fx.groundRing(sl, null, at, 5f, GOLD, 50);
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.2, at.z, 60, 1.2, 1.2, 1.2, 0.05);
        sl.sendParticles(ParticleTypes.WAX_ON, at.x, at.y + 1, at.z, 40, 1, 1, 1, 0.1);
    }
}
