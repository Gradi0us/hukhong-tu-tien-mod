package com.hukhong.endgame;

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
 * Bộ chiêu riêng của Dạ Khuyết (kiểu Malenia, chủ đề trăng/máu). Mỗi chiêu = dòng thời gian theo tick {@code t} trong {@link #step};
 * mọi chiêu có telegraph trước khi gây sát thương và VFX riêng. Đòn trúng => hồi máu ({@link DaKhuyet#strike}).
 * <ul>
 *   <li>dk_thuy_dieu (Thủy Điểu Vũ): bay lên, 3 đợt lướt chém theo vạch cảnh báo; ra khỏi vạch trong lúc vạch nhấp nháy để né.</li>
 *   <li>dk_thuy_dieu_huyet: như trên, nhanh hơn, cuối mỗi đợt để lại vũng máu.</li>
 *   <li>dk_dam_xuyen: đâm lao thẳng (vạch dài).</li>
 *   <li>dk_lien_tram: 2 nhát nhanh + nhát cuối TRỄ lớn hơn (đừng lăn sớm).</li>
 *   <li>dk_lao_khong / dk_song_lao: nhảy lên rồi lao xuống điểm đánh dấu (1 / 2 lần).</li>
 *   <li>dk_nguyet_luan: 3 lưỡi trăng khuyết bay theo hình quạt.</li>
 *   <li>dk_dem_khuyet: Đêm Khuyết (mù + yếu quanh thân) — chiêu cũ, pha 1.</li>
 *   <li>dk_huyet_hoa (Huyết Nguyệt Khai Hoa, tối thượng): bay lên với cánh trăng, lao xuống, hoa máu nổ lớn + vũng máu lâu.</li>
 * </ul>
 */
public final class DaKhuyetMoves {
    private DaKhuyetMoves() {}

    public static final int MOON = 0x8FA8FF, BLOOD = 0xC01028, SCARLET = 0xFF2040, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("dk_thuy_dieu", "dk_thuy_dieu_huyet", "dk_dam_xuyen", "dk_lien_tram",
            "dk_lao_khong", "dk_song_lao", "dk_nguyet_luan", "dk_dem_khuyet", "dk_huyet_hoa");

    public static boolean handles(String id) { return IDS.contains(id); }

    /** Vũng gây sát thương. */
    public static final class Pool {
        final Vec3 c; final double r; int left; final float dmg;
        Pool(Vec3 c, double r, int left, float dmg) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; }
    }

    // ------------------------------------------------------------------ chọn chiêu (AI riêng; director chen thêm chiêu phase)
    static String pick(RandomSource rnd, int phase, boolean aeoniaReady, float dist) {
        if (phase >= 2 && aeoniaReady && rnd.nextInt(6) == 0) return "dk_huyet_hoa";
        if (dist > 9 && rnd.nextBoolean()) return phase >= 2 && rnd.nextBoolean() ? "dk_nguyet_luan" : "dk_dam_xuyen";
        List<String> pool = phase >= 3
                ? List.of("dk_lien_tram", "dk_dam_xuyen", "dk_thuy_dieu_huyet", "dk_song_lao", "dk_nguyet_luan")
                : phase == 2
                ? List.of("dk_lien_tram", "dk_dam_xuyen", "dk_thuy_dieu", "dk_lao_khong", "dk_nguyet_luan")
                : List.of("dk_lien_tram", "dk_dam_xuyen", "dk_thuy_dieu", "dk_lao_khong", "dk_dem_khuyet");
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "da_khuyet". */
    static double mult(String id) {
        return switch (id) {
            case "dk_thuy_dieu" -> 0.5;
            case "dk_thuy_dieu_huyet" -> 0.55;
            case "dk_dam_xuyen" -> 1.3;
            case "dk_lien_tram" -> 0.9;
            case "dk_lao_khong" -> 1.4;
            case "dk_song_lao" -> 1.2;
            case "dk_nguyet_luan" -> 1.0;
            case "dk_huyet_hoa" -> 2.4;
            default -> 0.8;
        };
    }

    private static boolean blood(DaKhuyet d) { return d.mv.endsWith("_huyet") || d.mv.equals("dk_song_lao") || d.mv.equals("dk_huyet_hoa"); }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, DaKhuyet d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        if (t != null) d.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        boolean ult = d.mv.equals("dk_huyet_hoa");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, ult ? 0.4f : 0.7f);
        if (ult) d.aeoniaCd = 900;
        if (ult && !d.avatar()) ultScene(sl, d);
    }

    private static Vec3 tpos(DaKhuyet d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aim.scale(6));
    }

    private static void reaim(DaKhuyet d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aim = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aim.x, d.aim.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    // ------------------------------------------------------------------ mỗi tick; false = chiêu xong
    static boolean step(ServerLevel sl, DaKhuyet d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "dk_thuy_dieu", "dk_thuy_dieu_huyet" -> waterfowl(sl, d, t);
            case "dk_dam_xuyen" -> thrust(sl, d, t);
            case "dk_lien_tram" -> combo(sl, d, t);
            case "dk_lao_khong" -> dive(sl, d, t, 1);
            case "dk_song_lao" -> dive(sl, d, t, 2);
            case "dk_nguyet_luan" -> crescents(sl, d, t);
            case "dk_huyet_hoa" -> aeonia(sl, d, t);
            default -> darkNight(sl, d, t);
        };
    }

    // ---- Thủy Điểu Vũ: bay lên -> 3 đợt [vạch cảnh báo TELE tick -> lướt DASH tick]
    static final int WF_RISE = 18, WF_TELE = 12, WF_DASH = 6, WF_GAP = 6;

    private static boolean waterfowl(ServerLevel sl, DaKhuyet d, int t) {
        boolean bl = blood(d);
        int tele = bl ? WF_TELE - 2 : WF_TELE, cyc = tele + WF_DASH + WF_GAP, rgb = bl ? SCARLET : MOON;
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "skill_c");
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 3.5f, rgb, WF_RISE + 6, 6);
            sl.sendParticles(ParticleTypes.CLOUD, d.getX(), d.getY(), d.getZ(), 20, 0.8, 0.1, 0.8, 0.05);
            d.playSound(SoundEvents.PHANTOM_FLAP, 2.5f, 0.6f);
        }
        if (t < WF_RISE) {  // bay lên ~5 block, xoay
            d.setDeltaMovement(0, t < 10 ? 0.5 : 0.02, 0);
            d.hurtMarked = true;
            if (t % 3 == 0) Fx.qi(sl, d.position().add(0, 1.4, 0), new Vec3(Math.cos(t), 0, Math.sin(t)).scale(2), 2f, rgb, 0, 8);
            return true;
        }
        int k = (t - WF_RISE) / cyc, e = (t - WF_RISE) % cyc;
        if (k >= 3) { d.setNoGravity(false); return t < WF_RISE + 3 * cyc + 8; }
        if (e == 0) {  // vạch cảnh báo: từ vị trí hiện tại xuyên qua mục tiêu (vượt quá 3 block)
            reaim(d);
            Vec3 tp = tpos(d);
            d.from = d.position();
            Vec3 flat = tp.subtract(d.from).multiply(1, 0, 1);
            double len = Math.min(d.mv.equals("dk_thuy_dieu_huyet") ? 15 : 14, flat.length() + 3);
            d.to = new Vec3(d.from.x, tp.y, d.from.z).add(d.aim.scale(len));
            Vec3 g = new Vec3(d.from.x, tp.y + 0.05, d.from.z);
            Fx.telegraphLine(sl, null, g, d.to.subtract(g), 3.0f, tele, rgb);
            d.hitOnce.clear();
            d.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.5f, 1.4f + k * 0.2f);
            sl.sendParticles(ParticleTypes.END_ROD, d.getX(), d.getY() + 1.5, d.getZ(), 12, 0.6, 0.8, 0.6, 0.05);
        } else if (e >= tele && e < tele + WF_DASH) {  // lướt chém theo vạch
            int s = e - tele;
            if (s == 0) {
                d.triggerAnim("skill", "flurry");
                Fx.ribbonWave(sl, null, d.from.add(0, 1.2, 0), d.to.add(0, 1.2, 0), 1.5, 0.8, WF_DASH + 2, 0, 0.5f, rgb);
                d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.6f + k * 0.15f);
            }
            Vec3 q = d.from.lerp(d.to, (s + 1) / (double) WF_DASH);
            d.teleportTo(q.x, q.y, q.z);
            d.setDeltaMovement(Vec3.ZERO);
            Fx.qi(sl, q.add(0, 1.2, 0), d.aim.scale(2.5), 2.6f, rgb, s % 2 == 0 ? 35 : -35, 8);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, q.x, q.y + 1.2, q.z, 2, 0.8, 0.4, 0.8, 0);
            hitAround(sl, d, q, 2.4, mult(d.mv), bl ? Hit.WEAK : Hit.SLOW, true);
            if (s == WF_DASH - 1 && bl) addPool(sl, d, q, 3.5, 100, 0.25);
        }
        return true;
    }

    // ---- Đâm lao: vạch 20 tick -> lướt 5 tick
    private static boolean thrust(ServerLevel sl, DaKhuyet d, int t) {
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(13));
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), 2.6f, 20, MOON);
            sl.sendParticles(ParticleTypes.GLOW, d.getX(), d.getY() + 1.4, d.getZ(), 16, 0.4, 0.6, 0.4, 0.02);
        }
        if (t == 10) d.triggerAnim("skill", "thrust");
        if (t >= 20 && t < 25) {
            if (t == 20) {
                Fx.rift(sl, null, d.from.add(0, 1.2, 0), d.to.subtract(d.from), 1.2f, MOON, 30);
                d.playSound(SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.5f, 0.9f);
            }
            Vec3 q = d.from.lerp(d.to, (t - 19) / 5.0);
            d.teleportTo(q.x, q.y, q.z);
            Fx.qi(sl, q.add(0, 1.3, 0), d.aim.scale(3), 1.4f, MOON, 0, 6);
            hitAround(sl, d, q, 2.0, mult(d.mv), Hit.KNOCK_UP, true);
        }
        return t < 34;
    }

    // ---- Chém liên hoàn: nhát 1 (t12), nhát 2 (t22), giữ thế... nhát 3 TRỄ (t52) to hơn
    private static boolean combo(ServerLevel sl, DaKhuyet d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 5.5f, 60, 12); }
        if (t == 12 || t == 22) {
            int n = t == 12 ? 0 : 1;
            reaim(d);
            lunge(d, 0.8);
            d.triggerAnim("skill", "attack");
            slash(sl, d, 5.5, 60, n == 0 ? 30 : -30, 1.0, MOON);
            if (n == 1) Fx.telegraphCone(sl, null, d.position(), d.aim, 7f, 90, 30);  // cảnh báo nhát cuối (trễ)
        }
        if (t > 22 && t < 52 && t % 4 == 0) // giữ kiếm, ánh trăng tụ lưỡi
            sl.sendParticles(ParticleTypes.END_ROD, d.getX() + d.aim.x, d.getY() + 2.2, d.getZ() + d.aim.z, 3, 0.3, 0.3, 0.3, 0.01);
        if (t == 52) {
            reaim(d);
            lunge(d, 1.2);
            d.triggerAnim("skill", "skill_a");
            slash(sl, d, 7.0, 90, 80, 1.5, SCARLET);
            Fx.groundCrack(sl, null, d.position().add(d.aim.scale(3)), 4f, MOON, 6, 30);
            Fx.shake(sl, d.position(), 1.5f, 10, 24f);
        }
        return t < 62;
    }

    private static void lunge(DaKhuyet d, double v) {
        d.setDeltaMovement(d.aim.x * v, 0.1, d.aim.z * v);
        d.hurtMarked = true;
    }

    private static void slash(ServerLevel sl, DaKhuyet d, double rad, double half, double roll, double mul, int rgb) {
        double cos = Math.cos(Math.toRadians(half));
        Vec3 c = d.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(d.aim) < cos)) continue;
            if (d.strike(p, d.moveDamage(mult(d.mv) * mul))) PhaseMoves.applyHit(p, Hit.NONE);
        }
        Fx.qi(sl, c.add(0, 1.4, 0), d.aim.scale(rad * 0.8), (float) (rad * 0.6), rgb, roll, 10);
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, mul > 1 ? 0.5f : 0.8f);
    }

    // ---- Lao từ trên không (n lần): bật lên -> vòng cảnh báo tại mục tiêu -> lơ lửng -> lao xuống
    static final int DV_LEN = 34;

    private static boolean dive(ServerLevel sl, DaKhuyet d, int t, int n) {
        int k = t / DV_LEN, e = t % DV_LEN;
        if (k >= n) { d.setNoGravity(false); return t < n * DV_LEN + 6; }
        int rgb = n > 1 ? SCARLET : MOON;
        double rad = 4.5;
        if (e == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "dive");
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) rad, 26, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) rad * 0.7f, rgb, 26, 5);
            sl.sendParticles(ParticleTypes.CLOUD, d.getX(), d.getY(), d.getZ(), 20, 1, 0.1, 1, 0.06);
            d.playSound(SoundEvents.PHANTOM_FLAP, 2.5f, 0.8f);
        }
        if (e < 10) { d.setDeltaMovement(0, 0.8, 0); d.hurtMarked = true; }
        else if (e == 18) { d.teleportTo(d.spot.x, d.spot.y + 8, d.spot.z); d.setDeltaMovement(Vec3.ZERO); }
        else if (e > 18 && e < 26 && e % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, d.getX(), d.getY(), d.getZ(), 4, 0.4, 0.4, 0.4, 0.02);
        else if (e == 26) {
            Vec3 s = d.spot;
            Fx.ribbon(sl, null, d.position(), s, 0, 3, 0, 0.6f, rgb);
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, s, rad))
                if (flat2(p.position(), s) <= rad * rad && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, s, (float) rad, rgb, 8, 40);
            Fx.groundRing(sl, null, s, (float) rad * 1.3f, rgb, 20);
            Fx.shake(sl, s, 2.5f, 16, 32f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.7f);
        }
        return true;
    }

    // ---- Nguyệt Luân: 3 lưỡi trăng khuyết hình quạt (-25/0/+25 độ), vạch 18 tick
    private static boolean crescents(ServerLevel sl, DaKhuyet d, int t) {
        double len = 18;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            for (int i = -1; i <= 1; i++) Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), rot(d.aim, i * 25).scale(len), 2.2f, 18, MOON);
            d.triggerAnim("skill", "skill_b");
        }
        if (t == 18) {
            for (int i = -1; i <= 1; i++) {
                Vec3 dir = rot(d.aim, i * 25);
                Fx.qi(sl, d.from.add(0, 1.2, 0), dir.scale(len), 2.2f, MOON, i * 30, 14);
                for (Player p : players(sl, d.from, len)) {
                    Vec3 rel = p.position().subtract(d.from).multiply(1, 0, 1);
                    double along = rel.dot(dir);
                    if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > 1.6) continue;
                    if (d.hitOnce.add(p.getUUID()) && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.SLOW);
                }
            }
            d.playSound(SoundEvents.TRIDENT_THROW.value(), 2.5f, 0.6f);
        }
        return t < 28;
    }

    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    // ---- Đêm Khuyết (chiêu cũ): vòng 10 block quanh thân, mù + yếu
    private static boolean darkNight(ServerLevel sl, DaKhuyet d, int t) {
        if (t == 0) {
            Fx.telegraphCircle(sl, null, d.position(), 10f, 30, MOON);
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 6f, 0x404070, 30, 8);
            d.triggerAnim("skill", "skill_b");
        }
        if (t == 30) {
            for (Player p : players(sl, d.position(), 10)) {
                if (flat2(p.position(), d.position()) > 100) continue;
                if (d.strike(p, d.moveDamage(mult(d.mv)))) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                }
            }
            Fx.groundRing(sl, null, d.position(), 10f, MOON, 24);
            sl.sendParticles(ParticleTypes.SQUID_INK, d.getX(), d.getY() + 1, d.getZ(), 60, 4, 1.5, 4, 0.05);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 1.4f);
        }
        return t < 36;
    }

    // ---- Huyết Nguyệt Khai Hoa (Scarlet Aeonia): bay lên 30 tick -> vòng đỏ 16 block 50 tick -> lao xuống t80 -> hoa nổ + vũng máu
    public static final int AE_RISE = 30, AE_DIVE = 80, AE_END = 96;
    public static final double AE_RAD = 16;

    private static boolean aeonia(ServerLevel sl, DaKhuyet d, int t) {
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "rise");
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 3f, SCARLET, 0);
            wingsFx(sl, d, SCARLET, 40);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.6f);
        }
        if (t < AE_RISE) {
            d.setDeltaMovement(0, 0.33, 0);
            d.hurtMarked = true;
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, d.getX(), d.getY() + 1, d.getZ(), 10, 1.5, 1, 1.5, 0.02);
            return true;
        }
        if (t == AE_RISE) {
            d.setDeltaMovement(Vec3.ZERO);
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) AE_RAD, AE_DIVE - AE_RISE, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) AE_RAD * 0.6f, BLOOD, AE_DIVE - AE_RISE, 8);
            Fx.rune(sl, null, d.position().add(0, 2, 0), 5f, SCARLET, AE_DIVE - AE_RISE + 10, 6);  // trăng máu sau lưng
        }
        if (t > AE_RISE && t < AE_DIVE) {
            d.setDeltaMovement(Vec3.ZERO);
            if (t % 4 == 0) {  // cánh hoa xoáy vào tâm
                double a0 = t * 0.3;
                for (int i = 0; i < 8; i++) {
                    double a = a0 + i * Math.PI / 4, rr = AE_RAD * (1 - (t - AE_RISE) / (double) (AE_DIVE - AE_RISE));
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, d.spot.x + Math.cos(a) * rr, d.spot.y + 0.4, d.spot.z + Math.sin(a) * rr, 1, 0.1, 0.1, 0.1, 0);
                }
            }
            if (t == AE_DIVE - 8) { d.teleportTo(d.spot.x, d.spot.y + 10, d.spot.z); d.triggerAnim("skill", "dive"); }
            return true;
        }
        if (t == AE_DIVE) {
            Vec3 s = d.spot;
            Fx.ribbon(sl, null, d.position(), s, 0, 3, 0, 1.0f, SCARLET);
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, s, AE_RAD))
                if (flat2(p.position(), s) <= AE_RAD * AE_RAD && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.WEAK);
            bloomFx(sl, s, AE_RAD);
            addPool(sl, d, s, 9, 200, 0.25);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
            d.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.6f);
        }
        if (t == AE_DIVE + 4) d.setNoGravity(false);
        return t < AE_END;
    }

    // ------------------------------------------------------------------ trợ giúp sát thương
    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    /** Trúng người chơi quanh điểm (mỗi người tối đa 1 lần / đợt nếu once). */
    private static void hitAround(ServerLevel sl, DaKhuyet d, Vec3 c, double r, double mul, Hit h, boolean once) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (once && !d.hitOnce.add(p.getUUID())) continue;
            if (d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    static void addPool(ServerLevel sl, DaKhuyet d, Vec3 c, double r, int life, double mul) {
        d.pools.add(new Pool(c, r, life, d.moveDamage(mul)));
        Fx.pool(sl, null, c, (float) r, BLOOD, life);
    }

    /** Vũng máu: mỗi 10 tick gây sát thương + độc ("thối hoa") cho người đứng trong; không hút máu. */
    static void tickPools(ServerLevel sl, DaKhuyet d) {
        for (Iterator<Pool> it = d.pools.iterator(); it.hasNext(); ) {
            Pool p = it.next();
            if (--p.left <= 0) { it.remove(); continue; }
            if (p.left % 5 == 0) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.c.x, p.c.y + 0.3, p.c.z, 6, p.r * 0.5, 0.1, p.r * 0.5, 0.01);
            if (p.left % 10 != 0) continue;
            for (Player pl : players(sl, p.c, p.r)) {
                if (flat2(pl.position(), p.c) > p.r * p.r || Math.abs(pl.getY() - p.c.y) > 2) continue;
                pl.hurt(d.damageSources().magic(), p.dmg);
                pl.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
            }
        }
    }

    // ------------------------------------------------------------------ VFX
    /** Hút máu: dải máu từ nạn nhân về Dạ Khuyết. */
    static void drainFx(ServerLevel sl, DaKhuyet d, LivingEntity from) {
        Fx.ribbonWave(sl, null, from.position().add(0, from.getBbHeight() * 0.6, 0), d.position().add(0, d.getBbHeight() * 0.6, 0), 1.0, 0.4, 10, 2, 0.12f, BLOOD);
        sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, d.getX(), d.getY() + d.getBbHeight() * 0.7, d.getZ(), 3, 0.4, 0.4, 0.4, 0.05);
    }

    /** Hai cánh trăng khuyết: dải sáng cong từ lưng toả lên hai bên. */
    static void wingsFx(ServerLevel sl, DaKhuyet d, int rgb, int life) {
        Vec3 back = d.position().add(0, d.getBbHeight() * 0.65, 0);
        float yaw = (float) Math.toRadians(d.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        for (int side = -1; side <= 1; side += 2)
            for (int i = 0; i < 3; i++) {
                Vec3 tip = back.add(right.scale(side * (3.0 + i))).add(0, 2.5 - i * 1.2, 0);
                Fx.ribbon(sl, null, back, tip, 1.8 - i * 0.4, 12 + i * 3, i * 2, 0.35f, rgb);
            }
        Fx.rune(sl, null, back.add(0, 1.0, 0), 2.5f, rgb, life, 6);
    }

    /** Hoa trăng máu nở: 8 cánh dải đỏ + nứt đất + vòng + nổ. */
    static void bloomFx(ServerLevel sl, Vec3 s, double rad) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            Vec3 tip = s.add(Math.cos(a) * rad, 0.4, Math.sin(a) * rad);
            Fx.ribbonWave(sl, null, s.add(0, 0.6, 0), tip, 3.0, 1.2, 14, 0, 0.9f, SCARLET);
        }
        Fx.groundCrack(sl, null, s, (float) rad, BLOOD, 16, 60);
        Fx.groundRing(sl, null, s, (float) rad, SCARLET, 30);
        Fx.elem(sl, null, Fx.E_NUKE, s, Vec3.ZERO, (float) rad * 0.4f, SCARLET, 0);
        Fx.shake(sl, s, 4f, 30, 60f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 1, s.z, 2, 1, 0.2, 1, 0);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, s.x, s.y + 2, s.z, 120, rad * 0.4, 2, rad * 0.4, 0.2);
        sl.sendParticles(ParticleTypes.CRIMSON_SPORE, s.x, s.y + 1, s.z, 120, rad * 0.4, 1, rad * 0.4, 0.05);
    }

    /** Lộ diện pha (gọi từ applyPhase khi đang chuyển pha): P2 cánh trăng mọc, P3 hoa máu nở. */
    static void transformFx(ServerLevel sl, DaKhuyet d, int ph) {
        d.triggerAnim("skill", ph >= 3 ? "phase" : "rise");
        if (ph == 2) {
            wingsFx(sl, d, MOON, 60);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 3f, MOON, 0);
            sl.sendParticles(ParticleTypes.END_ROD, d.getX(), d.getY() + 2, d.getZ(), 80, 2, 2, 2, 0.1);
            d.playSound(SoundEvents.PHANTOM_FLAP, 3f, 0.5f);
        } else if (ph >= 3) {
            wingsFx(sl, d, SCARLET, 60);
            bloomFx(sl, d.position(), 10);
            d.playSound(SoundEvents.WARDEN_ROAR, 3f, 1.2f);
        }
    }

    // ------------------------------------------------------------------ cutscene
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: từ sau lưng nhìn qua vai ra trăng -> vòng ra trước -> cận mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 1.5, 3.2, -5.0, 70, 3.0, -10), k(T * 2 / 5, 4.5, 1.4, 2.0, 60, 2.2, 0),
                k(T * 3 / 4, 0.8, 2.4, 3.2, 45, 2.6, 0), k(T, 0, 2.6, 6.0, 64, 2.2, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.da_khuyet"), new SubLine(36, 72, "hukhong.dialog.dakhuyet.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.dakhuyet.intro.2"));
    }

    static final int ULT_T = 60;

    /** Tối thượng: camera thấp nhìn lên Dạ Khuyết bay lên với cánh trăng máu. */
    private static void ultScene(ServerLevel sl, DaKhuyet d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 2.5 * s, 0.4 * s, 3.5 * s, 62, 1.5 * s, -6), k(ULT_T / 2, -1.5 * s, 0.2 * s, 4.0 * s, 70, 4.0 * s, -20),
                k(ULT_T, 0, 1.0 * s, 8.0 * s, 80, 6.0 * s, -18));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 28, "hukhong.dialog.dakhuyet.ult.1"),
                new SubLine(28, ULT_T - 2, "hukhong.dialog.dakhuyet.ult.2")));
    }

    /** Tử trận: cutscene tĩnh tại chỗ ngã, cánh tan thành cánh hoa. */
    static void deathScene(ServerLevel sl, DaKhuyet d) {
        int T = 90;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.6, 4.0, 55, 1.2, 0), k(45, 2.5, 2.0, 3.0, 60, 1.0, 0), k(T, 0, 4.5, 8.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 46, "hukhong.dialog.dakhuyet.death.1"),
                new SubLine(46, T - 4, "hukhong.dialog.dakhuyet.death.2")));
        Fx.groundRing(sl, null, at, 8f, MOON, 60);
        Fx.pool(sl, null, at, 5f, BLOOD, 120);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, at.x, at.y + 2, at.z, 150, 2.5, 2, 2.5, 0.05);
        sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.5, at.z, 60, 1.5, 1.5, 1.5, 0.05);
    }
}
