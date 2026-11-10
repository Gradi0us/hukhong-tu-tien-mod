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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;

/**
 * Bộ chiêu riêng của Chấp Hành Giả IV "Cơ Vô Tận" (kiểu Mohg, Lord of Blood): đinh ba máu, vệt huyết diễm trên đất,
 * vòng huyết diễm, cánh máu + lao từ trên không (pha 2), tối thượng "Hư Vô" (Nihil): đánh dấu mọi người chơi bằng huyết chú,
 * rồi 3 nhịp nổ gây sát thương người bị dấu + hồi máu cho boss. Mọi chiêu có telegraph trước khi gây sát thương.
 * <ul>
 *   <li>c4_quet: 2 nhát quét đinh ba (nón cảnh báo, nhát 2 quét ngược).</li>
 *   <li>c4_dam: đâm lướt thẳng (vạch), để lại vệt lửa máu.</li>
 *   <li>c4_huyet_diem: kéo đinh ba trên đất, vệt huyết diễm phun lên theo vạch + vũng lửa máu.</li>
 *   <li>c4_tam_lien: đâm - quét - nện trễ (vòng cảnh báo trước mặt).</li>
 *   <li>c4_huyet_luan: vòng huyết diễm nổ quanh thân 2 nhịp (ngoài rồi trong).</li>
 *   <li>c4_lao_canh: cánh máu bung, bay lên, lao xuống điểm đánh dấu.</li>
 *   <li>c4_huyet_tuyen: suối máu phun dưới chân từng người chơi (2 đợt).</li>
 *   <li>c4_nihil: Hư Vô — tối thượng (cutscene).</li>
 * </ul>
 */
public final class Chg4Moves {
    private Chg4Moves() {}

    public static final int BLOOD = 0xB00818, FLAME = 0xFF3020, DARK = 0x500010, GOLD = 0xFFB040, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("c4_quet", "c4_dam", "c4_huyet_diem", "c4_tam_lien",
            "c4_huyet_luan", "c4_lao_canh", "c4_huyet_tuyen", "c4_nihil");

    public static boolean handles(String id) { return IDS.contains(id); }

    /** Vũng lửa máu. */
    public static final class Pool {
        final Vec3 c; final double r; int left; final float dmg;
        Pool(Vec3 c, double r, int left, float dmg) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; }
    }

    // ------------------------------------------------------------------ chọn chiêu (AI riêng)
    static String pick(RandomSource rnd, int phase, boolean nihilReady, float dist) {
        if (phase >= 3 && nihilReady && rnd.nextInt(5) == 0) return "c4_nihil";
        if (dist > 9 && rnd.nextBoolean()) return phase >= 2 && rnd.nextBoolean() ? "c4_lao_canh" : "c4_huyet_diem";
        List<String> pool = phase >= 3
                ? List.of("c4_quet", "c4_dam", "c4_huyet_diem", "c4_tam_lien", "c4_huyet_luan", "c4_lao_canh", "c4_huyet_tuyen")
                : phase == 2
                ? List.of("c4_quet", "c4_dam", "c4_huyet_diem", "c4_tam_lien", "c4_huyet_luan", "c4_lao_canh")
                : List.of("c4_quet", "c4_dam", "c4_huyet_diem", "c4_tam_lien");
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "chg_4". */
    static double mult(String id) {
        return switch (id) {
            case "c4_quet" -> 0.9;
            case "c4_dam" -> 1.2;
            case "c4_huyet_diem" -> 1.0;
            case "c4_tam_lien" -> 0.8;
            case "c4_huyet_luan" -> 1.0;
            case "c4_lao_canh" -> 1.3;
            case "c4_huyet_tuyen" -> 1.0;
            case "c4_nihil" -> 0.8;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, ChapHanhGia d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("c4_nihil");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, ult ? 0.4f : 0.7f);
        if (ult) { d.nihilCd = 1000; ultScene(sl, d); }
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

    // ------------------------------------------------------------------ mỗi tick; false = chiêu xong
    static boolean step(ServerLevel sl, ChapHanhGia d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "c4_quet" -> sweep(sl, d, t);
            case "c4_dam" -> thrust(sl, d, t);
            case "c4_huyet_diem" -> flameTrail(sl, d, t);
            case "c4_tam_lien" -> triple(sl, d, t);
            case "c4_huyet_luan" -> nova(sl, d, t);
            case "c4_lao_canh" -> wingDive(sl, d, t);
            case "c4_huyet_tuyen" -> geysers(sl, d, t);
            case "c4_nihil" -> nihil(sl, d, t);
            default -> false;
        };
    }

    // ---- Quét đinh ba: nón 14 tick -> quét; nón ngược -> quét
    private static boolean sweep(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 6.5f, 80, 14); d.triggerAnim("skill", "skill_a"); }
        if (t == 14 || t == 32) {
            if (t == 32) reaim(d);
            cone(sl, d, 6.5, 80, t == 14 ? 40 : -40, 1.0, Hit.WEAK);
            sl.sendParticles(ParticleTypes.FLAME, d.getX() + d.aim.x * 3, d.getY() + 0.6, d.getZ() + d.aim.z * 3, 20, 2, 0.2, 2, 0.02);
            if (t == 14) { Fx.telegraphCone(sl, null, d.position(), d.aim, 6.5f, 80, 18); d.triggerAnim("skill", "attack"); }
        }
        return t < 42;
    }

    private static void cone(ServerLevel sl, ChapHanhGia d, double rad, double half, double roll, double mul, Hit h) {
        double cos = Math.cos(Math.toRadians(half));
        Vec3 c = d.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(d.aim) < cos)) continue;
            if (d.strike(p, d.moveDamage(mult(d.mv) * mul))) PhaseMoves.applyHit(p, h);
        }
        Fx.qi(sl, c.add(0, 1.4, 0), d.aim.scale(rad * 0.8), (float) (rad * 0.6), FLAME, roll, 10);
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, mul > 1 ? 0.5f : 0.7f);
    }

    // ---- Đâm lướt: vạch 18 tick -> lướt 5 tick, để lại vệt lửa máu
    private static boolean thrust(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(14));
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), 2.4f, 18, FLAME);
            d.triggerAnim("skill", "skill_b");
        }
        if (t >= 18 && t < 23) {
            if (t == 18) {
                Fx.rift(sl, null, d.from.add(0, 1.2, 0), d.to.subtract(d.from), 1.0f, BLOOD, 30);
                d.playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.5f, 0.6f);
                d.triggerAnim("skill", "attack");
            }
            Vec3 q = d.from.lerp(d.to, (t - 17) / 5.0);
            d.teleportTo(q.x, q.y, q.z);
            Fx.qi(sl, q.add(0, 1.3, 0), d.aim.scale(3), 1.4f, FLAME, 0, 6);
            hitAround(sl, d, q, 2.0, mult(d.mv), Hit.KNOCK_UP, true);
            if (t == 22) addPool(sl, d, d.from.lerp(d.to, 0.5), 2.2, 80, 0.2);
        }
        return t < 32;
    }

    // ---- Huyết Diễm Lê Địa: kéo đinh ba, tia lửa chạy theo vạch 24 tick -> phun lửa dọc vạch + vũng
    private static boolean flameTrail(ServerLevel sl, ChapHanhGia d, int t) {
        double len = 16;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(len));
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), 3.0f, 24, DANGER);
            d.triggerAnim("skill", "skill_c");
            d.playSound(SoundEvents.GRINDSTONE_USE, 2.5f, 0.5f);
        }
        if (t < 24 && t % 2 == 0) {
            Vec3 q = d.from.lerp(d.to, t / 24.0);
            sl.sendParticles(ParticleTypes.FLAME, q.x, q.y + 0.2, q.z, 6, 0.3, 0.05, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.LAVA, q.x, q.y + 0.2, q.z, 1, 0.2, 0, 0.2, 0);
        }
        if (t == 24) {
            Fx.rift(sl, null, d.from.add(0, 1.5, 0), d.to.subtract(d.from), 1.6f, FLAME, 36);
            Fx.ribbonWave(sl, null, d.from.add(0, 0.5, 0), d.to.add(0, 0.5, 0), 0.5, 1.0, 10, 0, 0.6f, BLOOD);
            Vec3 dir = d.aim;
            for (Player p : players(sl, d.from, len)) {
                Vec3 rel = p.position().subtract(d.from).multiply(1, 0, 1);
                double along = rel.dot(dir);
                if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > 1.6) continue;
                if (d.strike(p, d.moveDamage(mult(d.mv)))) { PhaseMoves.applyHit(p, Hit.KNOCK_UP); p.igniteForSeconds(3); }
            }
            for (int i = 1; i <= 4; i++) addPool(sl, d, d.from.lerp(d.to, i / 4.5), 1.8, 100, 0.2);
            Fx.shake(sl, d.position(), 1.5f, 10, 28f);
            d.playSound(SoundEvents.BLAZE_SHOOT, 3f, 0.5f);
        }
        return t < 34;
    }

    // ---- Tam Liên: đâm (t10) - quét (t22) - nện TRỄ (t48) vào vòng trước mặt
    private static boolean triple(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), d.aim.scale(6), 2.0f, 10, FLAME); }
        if (t == 10) { // đâm ngắn
            d.triggerAnim("skill", "attack");
            Vec3 c = d.position().add(d.aim.scale(3));
            hitAround(sl, d, c, 2.2, mult(d.mv), Hit.NONE, false);
            Fx.qi(sl, c.add(0, 1.3, 0), d.aim.scale(3), 1.2f, FLAME, 0, 6);
            Fx.telegraphCone(sl, null, d.position(), d.aim, 6f, 70, 12);
        }
        if (t == 22) { reaim(d); d.triggerAnim("skill", "skill_a"); cone(sl, d, 6, 70, 50, 1.0, Hit.NONE); }
        if (t == 24) {
            d.spot = d.position().add(d.aim.scale(4));
            Fx.telegraphCircle(sl, null, d.spot, 4f, 24, DANGER);
            d.triggerAnim("skill", "skill_c");
        }
        if (t > 24 && t < 48 && t % 4 == 0) sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 3.5, d.getZ(), 4, 0.3, 0.3, 0.3, 0.01);
        if (t == 48) {
            for (Player p : players(sl, d.spot, 4))
                if (flat2(p.position(), d.spot) <= 16 && d.strike(p, d.moveDamage(mult(d.mv) * 1.6))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, d.spot, 4f, BLOOD, 8, 40);
            addPool(sl, d, d.spot, 3, 90, 0.2);
            Fx.shake(sl, d.spot, 2f, 12, 28f);
            sl.sendParticles(ParticleTypes.EXPLOSION, d.spot.x, d.spot.y + 0.5, d.spot.z, 3, 1, 0.2, 1, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
        }
        return t < 58;
    }

    // ---- Huyết Diễm Hoàn: vòng 12 (30 tick) nổ; ngay sau vòng trong 6 (nhịp 2, t46)
    static final double NOVA_R = 12;

    private static boolean nova(ServerLevel sl, ChapHanhGia d, int t) {
        Vec3 c = d.position();
        if (t == 0) {
            Fx.telegraphCircle(sl, null, c, (float) NOVA_R, 30, DANGER);
            Fx.rune(sl, null, c.add(0, 0.1, 0), (float) NOVA_R * 0.6f, BLOOD, 50, 8);
            d.triggerAnim("skill", "phase");
            d.playSound(SoundEvents.BLAZE_AMBIENT, 3f, 0.4f);
        }
        if (t < 30 && t % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 2.5, d.getZ(), 8, 0.6, 1.2, 0.6, 0.05);
        if (t == 30 || t == 46) {
            double r = t == 30 ? NOVA_R : NOVA_R * 0.5;
            for (Player p : players(sl, c, r))
                if (flat2(p.position(), c) <= r * r && d.strike(p, d.moveDamage(mult(d.mv)))) { PhaseMoves.applyHit(p, Hit.KNOCK_UP); p.igniteForSeconds(4); }
            Fx.groundRing(sl, null, c, (float) r, FLAME, 24);
            Fx.elem(sl, null, Fx.E_TONGUES, c, Vec3.ZERO, (float) r * 0.35f, FLAME, 0);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + t;
                Fx.ribbon(sl, null, c.add(0, 0.5, 0), c.add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 1.0, 8, 0, 0.5f, BLOOD);
            }
            if (t == 30) Fx.telegraphCircle(sl, null, c, (float) (NOVA_R * 0.5), 16, DANGER);
            Fx.shake(sl, c, 2f, 12, 32f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        }
        return t < 56;
    }

    // ---- Huyết Dực Giáng: cánh máu, bật lên -> vòng cảnh báo -> lao xuống + nổ lửa
    private static boolean wingDive(ServerLevel sl, ChapHanhGia d, int t) {
        double rad = 5;
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "skill_c");
            wingsFx(sl, d, BLOOD, 40);
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) rad, 28, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) rad * 0.7f, BLOOD, 28, 6);
            d.playSound(SoundEvents.PHANTOM_FLAP, 3f, 0.5f);
        }
        if (t < 10) { d.setDeltaMovement(0, 0.8, 0); d.hurtMarked = true; }
        else if (t == 18) { d.teleportTo(d.spot.x, d.spot.y + 9, d.spot.z); d.setDeltaMovement(Vec3.ZERO); }
        else if (t > 18 && t < 28) { d.setDeltaMovement(Vec3.ZERO); if (t % 2 == 0) sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY(), d.getZ(), 6, 0.5, 0.5, 0.5, 0.02); }
        else if (t == 28) {
            Vec3 s = d.spot;
            Fx.ribbon(sl, null, d.position(), s, 0, 3, 0, 0.8f, FLAME);
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, s, rad))
                if (flat2(p.position(), s) <= rad * rad && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, s, (float) rad, BLOOD, 8, 40);
            Fx.groundRing(sl, null, s, (float) rad * 1.4f, FLAME, 20);
            addPool(sl, d, s, 4, 100, 0.25);
            Fx.shake(sl, s, 2.5f, 16, 32f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.6f);
        }
        if (t == 32) d.setNoGravity(false);
        return t < 40;
    }

    // ---- Huyết Tuyền: vòng 2.8 dưới chân mỗi người chơi (24 tick) -> suối máu phun; 2 đợt
    private static boolean geysers(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0 || t == 30) {
            d.spots.clear();
            for (Player p : players(sl, d.position(), 24)) d.spots.add(p.position());
            if (d.spots.isEmpty()) d.spots.add(tpos(d));
            for (Vec3 s : d.spots) Fx.telegraphCircle(sl, null, s, 2.8f, 24, DANGER);
            d.triggerAnim("skill", "skill_b");
            d.playSound(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 3f, 0.5f);
        }
        if (t == 24 || t == 54) {
            for (Vec3 s : d.spots) {
                for (Player p : players(sl, s, 2.8))
                    if (flat2(p.position(), s) <= 2.8 * 2.8 && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
                Fx.elem(sl, null, Fx.E_SPIKES, s, new Vec3(0, 1, 0), 1.4f, BLOOD, 0);
                Fx.ribbonHelix(sl, null, s, s.add(0, 6, 0), 0.8, t * 0.3, 10, 0, 0.35f, FLAME);
                sl.sendParticles(ParticleTypes.FALLING_LAVA, s.x, s.y + 4, s.z, 20, 0.6, 1.5, 0.6, 0);
            }
            d.playSound(SoundEvents.GENERIC_SPLASH, 3f, 0.5f);
        }
        return t < 60;
    }

    // ---- Hư Vô (Nihil): bay lên + vòng 20 (cảnh báo) -> t30 đánh dấu huyết chú -> nổ t60/t80/t100 -> hạ xuống
    public static final int NI_MARK = 30, NI_END = 116;
    public static final int[] NI_BURST = {60, 80, 100};
    public static final double NI_RAD = 20, NI_HEAL = 3.0;

    private static boolean nihil(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "phase");
            wingsFx(sl, d, BLOOD, 100);
            Fx.telegraphCircle(sl, null, d.position(), (float) NI_RAD, NI_MARK, DANGER);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < 20) { d.setDeltaMovement(0, 0.18, 0); d.hurtMarked = true; }
        else d.setDeltaMovement(Vec3.ZERO);
        if (t % 4 == 0) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, d.getX(), d.getY() + 2, d.getZ(), 12, 2, 1.5, 2, 0.02);
        if (t == NI_MARK) {
            d.marked.clear();
            List<Player> ps = new java.util.ArrayList<>(players(sl, d.position(), NI_RAD + 4));
            if (d.mTarget instanceof Player tp && tp.isAlive() && !ps.contains(tp)) ps.add(tp); // mục tiêu luôn bị dấu
            for (Player p : ps) {
                d.marked.add(p.getUUID());
                Fx.ribbonWave(sl, null, d.position().add(0, 2, 0), p.position().add(0, 1, 0), 1.5, 0.8, 12, 0, 0.3f, BLOOD);
                Fx.rune(sl, null, p.position().add(0, 0.1, 0), 1.6f, BLOOD, NI_END - NI_MARK, 5);
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, NI_END - NI_MARK, 0));
            }
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 3f, BLOOD, 0);
            d.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3f, 0.6f);
        }
        for (int i = 0; i < NI_BURST.length; i++) {
            if (t == NI_BURST[i] - 12) for (Player p : markedPlayers(sl, d)) Fx.telegraphCircle(sl, null, p.position(), 1.8f, 12, DANGER);
            if (t == NI_BURST[i]) burst(sl, d, i);
        }
        if (t == NI_END - 6) d.setNoGravity(false);
        return t < NI_END;
    }

    static List<Player> markedPlayers(ServerLevel sl, ChapHanhGia d) {
        List<Player> out = new java.util.ArrayList<>();
        for (java.util.UUID id : d.marked) {
            Player p = sl.getPlayerByUUID(id);
            if (p != null && p.isAlive() && !p.isSpectator() && p.distanceToSqr(d) < 64 * 64) out.add(p);
        }
        return out;
    }

    /** Một nhịp Hư Vô: mỗi người bị dấu nhận sát thương, dải máu bay về boss, boss hồi máu. */
    private static void burst(ServerLevel sl, ChapHanhGia d, int n) {
        Vec3 core = d.position().add(0, d.getBbHeight() * 0.6, 0);
        for (Player p : markedPlayers(sl, d)) {
            float dmg = d.moveDamage(mult(d.mv) * (1 + n * 0.25));
            if (d.strike(p, dmg)) d.heal((float) (dmg * NI_HEAL));
            Vec3 at = p.position().add(0, 1, 0);
            Fx.elem(sl, null, Fx.E_NUKE, p.position(), Vec3.ZERO, 0.8f, BLOOD, 0);
            Fx.ribbonHelix(sl, null, at, core, 0.6, n * 2.0, 14, 0, 0.25f, BLOOD);
            Fx.ribbonWave(sl, null, at, core, 2.0, 1.0, 16, 2, 0.2f, FLAME);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.05);
        }
        Fx.groundRing(sl, null, d.position(), (float) (6 + n * 3), BLOOD, 20);
        sl.sendParticles(ParticleTypes.HEART, d.getX(), d.getY() + d.getBbHeight(), d.getZ(), 4, 0.6, 0.4, 0.6, 0);
        Fx.shake(sl, d.position(), 1.5f + n, 10, 48f);
        d.playSound(SoundEvents.WARDEN_HEARTBEAT, 4f, 0.6f + n * 0.1f);
    }

    // ------------------------------------------------------------------ trợ giúp
    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static void hitAround(ServerLevel sl, ChapHanhGia d, Vec3 c, double r, double mul, Hit h, boolean once) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (once && !d.hitOnce.add(p.getUUID())) continue;
            if (d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    static void addPool(ServerLevel sl, ChapHanhGia d, Vec3 c, double r, int life, double mul) {
        d.pools4.add(new Pool(c, r, life, d.moveDamage(mul)));
        Fx.pool(sl, null, c, (float) r, BLOOD, life);
    }

    /** Vũng lửa máu: mỗi 10 tick sát thương phép + bốc cháy. */
    static void tickPools(ServerLevel sl, ChapHanhGia d) {
        for (Iterator<Pool> it = d.pools4.iterator(); it.hasNext(); ) {
            Pool p = it.next();
            if (--p.left <= 0) { it.remove(); continue; }
            if (p.left % 5 == 0) sl.sendParticles(ParticleTypes.FLAME, p.c.x, p.c.y + 0.2, p.c.z, 4, p.r * 0.5, 0.05, p.r * 0.5, 0.01);
            if (p.left % 10 != 0) continue;
            for (Player pl : players(sl, p.c, p.r)) {
                if (flat2(pl.position(), p.c) > p.r * p.r || Math.abs(pl.getY() - p.c.y) > 2) continue;
                pl.hurt(d.damageSources().magic(), p.dmg);
                pl.igniteForSeconds(2);
            }
        }
    }

    // ------------------------------------------------------------------ VFX
    /** Cánh máu: dải đỏ cong từ lưng toả hai bên. */
    static void wingsFx(ServerLevel sl, ChapHanhGia d, int rgb, int life) {
        Vec3 back = d.position().add(0, d.getBbHeight() * 0.7, 0);
        float yaw = (float) Math.toRadians(d.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        for (int side = -1; side <= 1; side += 2)
            for (int i = 0; i < 4; i++) {
                Vec3 tip = back.add(right.scale(side * (3.5 + i))).add(0, 3.0 - i * 1.3, 0);
                Fx.ribbon(sl, null, back, tip, 2.0 - i * 0.4, 12 + i * 3, i * 2, 0.4f, i % 2 == 0 ? rgb : FLAME);
            }
        Fx.rune(sl, null, back.add(0, 1.2, 0), 2.5f, rgb, life, 6);
    }

    /** Lộ diện pha: P2 cánh máu bung, P3 huyết diễm bùng quanh thân. */
    static void transformFx(ServerLevel sl, ChapHanhGia d, int ph) {
        d.triggerAnim("skill", "phase");
        if (ph == 2) {
            wingsFx(sl, d, BLOOD, 80);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2.5, 0), Vec3.ZERO, 3f, BLOOD, 0);
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, d.getX(), d.getY() + 2, d.getZ(), 100, 2.5, 2, 2.5, 0.05);
            d.playSound(SoundEvents.PHANTOM_FLAP, 3f, 0.4f);
        } else if (ph >= 3) {
            wingsFx(sl, d, DARK, 80);
            Fx.groundRing(sl, null, d.position(), 10f, FLAME, 40);
            Fx.elem(sl, null, Fx.E_TONGUES, d.position(), Vec3.ZERO, 3.5f, FLAME, 0);
            Fx.pool(sl, null, d.position(), 6f, BLOOD, 120);
            d.playSound(SoundEvents.WARDEN_ROAR, 3f, 0.8f);
        }
    }

    // ------------------------------------------------------------------ cutscene (thoại: hukhong.dialog.chg4.*)
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: thấp dưới chân nhìn lên đinh ba -> vòng ngang -> cận mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 2.0, 0.4, 5.0, 70, 4.0, -18), k(T * 2 / 5, -4.0, 1.6, 2.5, 58, 3.0, -6),
                k(T * 3 / 4, 0.6, 3.2, 3.0, 45, 3.2, 0), k(T, 0, 3.0, 7.0, 66, 2.6, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.chg_4"), new SubLine(36, 72, "hukhong.dialog.chg4.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.chg4.intro.2"));
    }

    static final int ULT_T = 70;

    /** Hư Vô: camera thấp nhìn lên boss lơ lửng giữa cánh máu. */
    private static void ultScene(ServerLevel sl, ChapHanhGia d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 3.0 * s, 0.5 * s, 4.0 * s, 62, 2.0 * s, -8), k(ULT_T / 2, -2.0 * s, 0.3 * s, 4.5 * s, 70, 4.0 * s, -20),
                k(ULT_T, 0, 1.5 * s, 9.0 * s, 80, 5.0 * s, -15));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 24, "hukhong.dialog.chg4.ult.1"),
                new SubLine(24, 48, "hukhong.dialog.chg4.ult.2"), new SubLine(48, ULT_T - 2, "hukhong.dialog.chg4.ult.3")));
    }

    /** Tử trận: cutscene tại chỗ ngã, đinh ba tan thành tro máu. */
    static void deathScene(ServerLevel sl, ChapHanhGia d) {
        int T = 100;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.8, 4.5, 55, 1.4, 0), k(50, 3.0, 2.2, 3.0, 60, 1.2, 0), k(T, 0, 5.0, 9.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 36, "hukhong.dialog.chg4.death.1"),
                new SubLine(36, 68, "hukhong.dialog.chg4.death.2"), new SubLine(68, T - 4, "hukhong.dialog.chg4.death.3")));
        Fx.groundRing(sl, null, at, 8f, BLOOD, 60);
        Fx.pool(sl, null, at, 5f, BLOOD, 140);
        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, at.x, at.y + 2, at.z, 150, 2.5, 2, 2.5, 0.05);
        sl.sendParticles(ParticleTypes.ASH, at.x, at.y + 2, at.z, 120, 2.5, 2, 2.5, 0.05);
    }
}
