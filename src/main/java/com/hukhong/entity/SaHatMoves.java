package com.hukhong.entity;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.vfx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;

/**
 * Bộ chiêu riêng của Sa Hạt Vương (kiểu Starscourge Radahn — song đại kiếm, trọng lực, mưa tên cát, thiên thạch giáng).
 * Chủ đề: cát sa mạc + trọng lực (bão cát, cát lún). Mọi chiêu có telegraph trước khi gây sát thương.
 * <ul>
 *   <li>sh_song_kiem: Song Kiếm Liên Trảm — 2 nhát quét song đại kiếm (nón) + nện chéo (vòng) để lại cát lún.</li>
 *   <li>sh_tien_vu: Sa Tiễn Vũ — lùi lại, mưa tên cát rơi xuống vòng đánh dấu + một mũi trọng tiễn xuyên thẳng.</li>
 *   <li>sh_dan_luc_keo: Dẫn Lực Hấp — hút mọi người vào tâm rồi nện trọng lực.</li>
 *   <li>sh_luu_sa: Lưu Sa Hãm — vòng cát lún dưới chân từng người chơi, lún xuống + chậm + sát thương liên tục.</li>
 *   <li>sh_trong_luc_cau: Trọng Lực Cầu — phóng cầu trọng lực bám đuổi, nổ khi chạm/hết thời gian.</li>
 *   <li>sh_bao_cat: Sa Bạo Hoàn — xoay song kiếm giữa bão cát, đẩy văng + mù.</li>
 *   <li>sh_thien_mang: Sư Tử Phi Dược — bật trọng lực lên trời, giáng xuống điểm đánh dấu, hút vòng ngoài.</li>
 *   <li>sh_tinh_vu: Tinh Vẫn Giáng Thế — tối thượng: bay vút lên trời, giáng xuống như thiên thạch (cutscene).</li>
 * </ul>
 */
public final class SaHatMoves {
    private SaHatMoves() {}

    public static final int SAND = 0xE0B060, DUST = 0xB08040, GRAV = 0x8A4FFF, STAR = 0xFF8A30, DANGER = 0xFF6020;
    public static final String ULT = "sh_tinh_vu";
    public static final List<String> IDS = List.of("sh_song_kiem", "sh_tien_vu", "sh_dan_luc_keo", "sh_luu_sa",
            "sh_trong_luc_cau", "sh_bao_cat", "sh_thien_mang", ULT);

    public static boolean handles(String id) { return IDS.contains(id); }

    /** Cầu trọng lực bám đuổi. */
    public static final class Orb {
        Vec3 pos; int left; final float dmg;
        Orb(Vec3 pos, int left, float dmg) { this.pos = pos; this.left = left; this.dmg = dmg; }
    }

    /** Vũng cát lún. */
    public static final class Sand {
        final Vec3 c; final double r; int left; final float dmg;
        Sand(Vec3 c, double r, int left, float dmg) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; }
    }

    // ------------------------------------------------------------------ AI chọn chiêu
    static String pick(RandomSource rnd, int phase, boolean ultReady, float dist) {
        if (phase >= 3 && ultReady && rnd.nextInt(4) == 0) return ULT;
        if (dist > 11 && rnd.nextBoolean())
            return phase >= 2 && rnd.nextBoolean() ? (rnd.nextBoolean() ? "sh_thien_mang" : "sh_trong_luc_cau") : (rnd.nextBoolean() ? "sh_tien_vu" : "sh_dan_luc_keo");
        List<String> pool = phase >= 3 ? IDS.subList(0, 7) : phase == 2 ? IDS.subList(0, 7).stream().filter(s -> !s.equals("sh_bao_cat")).toList() : IDS.subList(0, 4);
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương — khớp BossPhases "sa_hat_vuong". */
    static double mult(String id) {
        return switch (id) {
            case "sh_song_kiem" -> 0.9;
            case "sh_tien_vu" -> 0.8;
            case "sh_dan_luc_keo" -> 1.1;
            case "sh_luu_sa" -> 0.9;
            case "sh_trong_luc_cau" -> 0.9;
            case "sh_bao_cat" -> 0.8;
            case "sh_thien_mang" -> 1.2;
            case ULT -> 2.4;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, SaHatVuong d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals(ULT);
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, ult ? 0.4f : 0.7f);
        if (ult) { d.ultCd = 900; ultScene(sl, d); }
    }

    private static Vec3 tpos(SaHatVuong d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aim.scale(6));
    }

    private static void reaim(SaHatVuong d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aim = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aim.x, d.aim.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    // ------------------------------------------------------------------ mỗi tick; false = xong
    static boolean step(ServerLevel sl, SaHatVuong d, int t) {
        return switch (d.mv) {
            case "sh_song_kiem" -> twinCombo(sl, d, t);
            case "sh_tien_vu" -> arrowRain(sl, d, t);
            case "sh_dan_luc_keo" -> gravityPull(sl, d, t);
            case "sh_luu_sa" -> quicksand(sl, d, t);
            case "sh_trong_luc_cau" -> orbsCast(sl, d, t);
            case "sh_bao_cat" -> sandstorm(sl, d, t);
            case "sh_thien_mang" -> lionLeap(sl, d, t);
            case ULT -> meteor(sl, d, t);
            default -> false;
        };
    }

    // ---- Song Kiếm Liên Trảm: nón 14t -> quét; nón ngược 16t -> quét; vòng 20t -> nện chéo + cát lún
    private static boolean twinCombo(ServerLevel sl, SaHatVuong d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 7f, 65, 14); d.triggerAnim("skill", "skill_a"); }
        if (t == 14 || t == 30) {
            if (t == 30) reaim(d);
            cone(sl, d, 7, 65, t == 14 ? 35 : -35, 1.0, Hit.SLOW);
            if (t == 14) { Fx.telegraphCone(sl, null, d.position(), d.aim, 7f, 65, 16); d.triggerAnim("skill", "attack"); }
            else {
                d.spot = d.position().add(d.aim.scale(4.5));
                Fx.telegraphCircle(sl, null, d.spot, 4f, 20, DANGER);
                d.triggerAnim("skill", "skill_c");
            }
        }
        if (t > 30 && t < 50 && t % 4 == 0) sandPuff(sl, d.position().add(0, d.getBbHeight() + 1, 0), 4, 0.4);
        if (t == 50) {
            for (Player p : players(sl, d.spot, 4))
                if (flat2(p.position(), d.spot) <= 16 && d.strike(p, d.moveDamage(mult(d.mv) * 1.5))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, d.spot, 4.5f, SAND, 10, 40);
            Fx.elem(sl, null, Fx.E_SPIKES, d.spot, new Vec3(0, 1, 0), 1.6f, DUST, 0);
            addSand(sl, d, d.spot, 3, 80, 0.15);
            Fx.shake(sl, d.spot, 2f, 12, 28f);
            sandPuff(sl, d.spot, 60, 2);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
        }
        return t < 60;
    }

    private static void cone(ServerLevel sl, SaHatVuong d, double rad, double half, double roll, double mul, Hit h) {
        double cos = Math.cos(Math.toRadians(half));
        Vec3 c = d.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(d.aim) < cos)) continue;
            if (d.strike(p, d.moveDamage(mult(d.mv) * mul))) PhaseMoves.applyHit(p, h);
        }
        Fx.qi(sl, c.add(0, 1.6, 0), d.aim.scale(rad * 0.8), (float) (rad * 0.6), SAND, roll, 10);
        Fx.qi(sl, c.add(0, 1.2, 0), d.aim.scale(rad * 0.7), (float) (rad * 0.5), GRAV, -roll, 10);
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
    }

    // ---- Sa Tiễn Vũ: lùi -> t10 đánh dấu vòng (22t) -> mưa tên t32; trọng tiễn vạch (14t) t34 -> t48
    private static boolean arrowRain(ServerLevel sl, SaHatVuong d, int t) {
        if (t == 0) {
            reaim(d);
            d.setDeltaMovement(d.aim.scale(-1.4).add(0, 0.45, 0)); d.hurtMarked = true;
            d.triggerAnim("skill", "skill_b");
            d.playSound(SoundEvents.CROSSBOW_LOADING_START.value(), 3f, 0.5f);
        }
        if (t == 10) {
            d.spots.clear();
            RandomSource r = d.getRandom();
            for (Player p : players(sl, d.position(), 32)) {
                d.spots.add(p.position());
                for (int i = 0; i < 2; i++) d.spots.add(p.position().add(r.nextGaussian() * 3.5, 0, r.nextGaussian() * 3.5));
            }
            if (d.spots.isEmpty()) d.spots.add(tpos(d));
            for (Vec3 s : d.spots) Fx.telegraphCircle(sl, null, s, 2.6f, 22, DANGER);
            d.playSound(SoundEvents.CROSSBOW_SHOOT, 3f, 0.4f);
        }
        if (t > 10 && t < 32 && t % 3 == 0)
            sl.sendParticles(ParticleTypes.FIREWORK, d.getX(), d.getY() + d.getBbHeight() + 1, d.getZ(), 4, 0.3, 1.5, 0.3, 0.2);
        if (t == 26) for (Vec3 s : d.spots) Fx.ribbon(sl, null, s.add(0, 22, 0), s, 0, 6, 0, 0.25f, SAND);
        if (t == 32) {
            for (Vec3 s : d.spots) {
                for (Player p : players(sl, s, 2.6))
                    if (flat2(p.position(), s) <= 2.6 * 2.6 && d.hitOnce.add(p.getUUID()) && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.SLOW);
                Fx.elem(sl, null, Fx.E_NEEDLES, s, new Vec3(0, -1, 0), 1.2f, SAND, 0);
                sandPuff(sl, s, 14, 0.8);
            }
            d.playSound(SoundEvents.ARROW_HIT, 3f, 0.5f);
        }
        if (t == 34) {
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(28));
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), 2.0f, 14, GRAV);
        }
        if (t == 48) {
            d.hitOnce.clear();
            Fx.ribbonHelix(sl, null, d.from.add(0, 1.5, 0), d.to.add(0, 1.5, 0), 0.6, 0, 8, 0, 0.3f, GRAV);
            Fx.ribbon(sl, null, d.from.add(0, 1.5, 0), d.to.add(0, 1.5, 0), 0, 6, 0, 0.5f, STAR);
            line(sl, d, d.from, d.aim, 28, 1.3, mult(d.mv) * 1.3, Hit.KNOCK_UP);
            d.playSound(SoundEvents.TRIDENT_THROW.value(), 3f, 0.4f);
        }
        return t < 56;
    }

    // ---- Dẫn Lực Hấp: vòng 12 (hút 24t) -> vòng trong 5 (12t) -> nện t36
    static final double PULL_R = 12;

    private static boolean gravityPull(ServerLevel sl, SaHatVuong d, int t) {
        Vec3 c = d.position();
        if (t == 0) {
            Fx.telegraphCircle(sl, null, c, (float) PULL_R, 24, GRAV);
            Fx.rune(sl, null, c.add(0, 0.1, 0), (float) PULL_R * 0.7f, GRAV, 40, 8);
            d.triggerAnim("skill", "phase");
            d.playSound(SoundEvents.BEACON_ACTIVATE, 3f, 0.4f);
        }
        if (t < 24) {
            for (Player p : players(sl, c, PULL_R)) {
                if (flat2(p.position(), c) > PULL_R * PULL_R) continue;
                Vec3 in = c.subtract(p.position()).multiply(1, 0, 1);
                if (in.lengthSqr() < 4) continue;
                p.setDeltaMovement(p.getDeltaMovement().add(in.normalize().scale(0.09)));
                p.hurtMarked = true;
            }
            if (t % 3 == 0) {
                double a = t * 0.7;
                Vec3 o = c.add(Math.cos(a) * PULL_R, 0.4, Math.sin(a) * PULL_R);
                Fx.ribbonWave(sl, null, o, c.add(0, 1, 0), 1.5, 0.6, 8, 0, 0.25f, GRAV);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1, c.z, 20, PULL_R * 0.4, 0.3, PULL_R * 0.4, 0.05);
            }
        }
        if (t == 24) { Fx.telegraphCircle(sl, null, c, 5f, 12, DANGER); d.triggerAnim("skill", "skill_c"); }
        if (t == 36) {
            for (Player p : players(sl, c, 5))
                if (flat2(p.position(), c) <= 25 && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, c, 5.5f, GRAV, 10, 40);
            Fx.groundRing(sl, null, c, 9f, SAND, 24);
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 1, 0), Vec3.ZERO, 3f, GRAV, 0);
            sandPuff(sl, c, 80, 3);
            Fx.shake(sl, c, 2.5f, 14, 32f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        }
        return t < 46;
    }

    // ---- Lưu Sa Hãm: vòng 3.5 dưới chân từng người (20t) -> phun cát + để lại cát lún 100t
    private static boolean quicksand(ServerLevel sl, SaHatVuong d, int t) {
        if (t == 0) {
            d.spots.clear();
            for (Player p : players(sl, d.position(), 26)) d.spots.add(p.position());
            if (d.spots.isEmpty()) d.spots.add(tpos(d));
            for (Vec3 s : d.spots) { Fx.telegraphCircle(sl, null, s, 3.5f, 20, SAND); Fx.rune(sl, null, s.add(0, 0.1, 0), 2.5f, DUST, 20, 5); }
            d.triggerAnim("skill", "skill_b");
            d.playSound(SoundEvents.SAND_BREAK, 3f, 0.4f);
        }
        if (t > 0 && t < 20 && t % 4 == 0) for (Vec3 s : d.spots) sandPuff(sl, s, 6, 1.5);
        if (t == 20) {
            for (Vec3 s : d.spots) {
                for (Player p : players(sl, s, 3.5))
                    if (flat2(p.position(), s) <= 3.5 * 3.5 && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.SLOW);
                Fx.elem(sl, null, Fx.E_TIDE, s, Vec3.ZERO, 2f, SAND, 0);
                Fx.ribbonHelix(sl, null, s, s.add(0, 4, 0), 1.2, 0, 10, 0, 0.3f, DUST);
                addSand(sl, d, s, 3.5, 100, 0.12);
            }
            d.playSound(SoundEvents.SAND_FALL, 3f, 0.4f);
        }
        return t < 30;
    }

    // ---- Trọng Lực Cầu: gồng 12t -> phóng 3 (pha 3: 5) cầu bám đuổi
    private static boolean orbsCast(ServerLevel sl, SaHatVuong d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "skill_a");
            Fx.rune(sl, null, d.position().add(0, d.getBbHeight() + 1.5, 0), 2.5f, GRAV, 14, 6);
            d.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 3f, 0.5f);
        }
        if (t == 12) {
            int n = d.phase() >= 3 ? 5 : 3;
            Vec3 head = d.position().add(0, d.getBbHeight() + 1.5, 0);
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                Vec3 o = head.add(Math.cos(a) * 2.5, 0.5, Math.sin(a) * 2.5);
                d.orbs.add(new Orb(o, 90 + i * 8, d.moveDamage(mult(d.mv))));
                Fx.elem(sl, null, Fx.E_HALOS, o, Vec3.ZERO, 0.8f, GRAV, 0);
            }
            d.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 3f, 0.4f);
        }
        return t < 24;
    }

    static final double ORB_SPEED = 0.24, ORB_R = 2.2;

    /** Cầu trọng lực: bay về người chơi gần nhất, telegraph dưới đất mỗi 10t, nổ khi chạm/hết giờ. */
    static void tickOrbs(ServerLevel sl, SaHatVuong d) {
        for (Iterator<Orb> it = d.orbs.iterator(); it.hasNext(); ) {
            Orb o = it.next();
            Player near = null;
            double best = 40 * 40;
            for (Player p : players(sl, o.pos, 40)) { double q = p.position().add(0, 1, 0).distanceToSqr(o.pos); if (q < best) { best = q; near = p; } }
            if (near != null) {
                Vec3 to = near.position().add(0, 1, 0).subtract(o.pos);
                if (to.lengthSqr() > 1e-4) o.pos = o.pos.add(to.normalize().scale(ORB_SPEED));
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, o.pos.x, o.pos.y, o.pos.z, 6, 0.3, 0.3, 0.3, 0.02);
            sl.sendParticles(ParticleTypes.DRAGON_BREATH, o.pos.x, o.pos.y, o.pos.z, 2, 0.15, 0.15, 0.15, 0.0);
            if (o.left % 10 == 0) Fx.telegraphCircle(sl, null, new Vec3(o.pos.x, Math.floor(o.pos.y - 1), o.pos.z), (float) ORB_R, 10, GRAV);
            boolean touch = near != null && best < 1.6 * 1.6;
            if (--o.left > 0 && !touch) continue;
            it.remove();
            for (Player p : players(sl, o.pos, ORB_R))
                if (p.position().add(0, 1, 0).distanceToSqr(o.pos) <= ORB_R * ORB_R && d.strike(p, o.dmg)) {
                    Vec3 push = p.position().subtract(o.pos).multiply(1, 0, 1);
                    if (push.lengthSqr() > 1e-4) p.setDeltaMovement(p.getDeltaMovement().add(push.normalize().scale(0.8)).add(0, 0.4, 0));
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                }
            Fx.elem(sl, null, Fx.E_NUKE, o.pos, Vec3.ZERO, 0.7f, GRAV, 0);
            sl.sendParticles(ParticleTypes.EXPLOSION, o.pos.x, o.pos.y, o.pos.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.ENDER_EYE_DEATH, 2f, 0.5f);
        }
    }

    // ---- Sa Bạo Hoàn: vòng 8 (30t) -> xoay 20t trong bão cát, mỗi 5t đẩy văng + mù
    static final double STORM_R = 8;

    private static boolean sandstorm(ServerLevel sl, SaHatVuong d, int t) {
        Vec3 c = d.position();
        if (t == 0) {
            Fx.telegraphCircle(sl, null, c, (float) STORM_R, 30, DANGER);
            d.triggerAnim("skill", "skill_b");
            d.playSound(SoundEvents.ELYTRA_FLYING, 3f, 0.5f);
        }
        if (t < 30 && t % 3 == 0) sandPuff(sl, c.add(0, 1, 0), 10, STORM_R * 0.5);
        if (t >= 30 && t <= 50) {
            d.setYRot(d.getYRot() + 36); d.yBodyRot = d.getYRot();
            if (t % 5 == 0) {
                for (Player p : players(sl, c, STORM_R)) {
                    if (flat2(p.position(), c) > STORM_R * STORM_R) continue;
                    if (d.strike(p, d.moveDamage(mult(d.mv) * 0.5))) PhaseMoves.applyHit(p, Hit.BLIND);
                    Vec3 out = p.position().subtract(c).multiply(1, 0, 1);
                    if (out.lengthSqr() > 1e-4) p.setDeltaMovement(p.getDeltaMovement().add(out.normalize().scale(0.7)).add(0, 0.25, 0));
                    p.hurtMarked = true;
                }
                Fx.qi(sl, c.add(0, 1.5, 0), new Vec3(Math.cos(t), 0, Math.sin(t)).scale(STORM_R * 0.8), (float) STORM_R * 0.6f, SAND, t * 20, 8);
                Fx.ribbonHelix(sl, null, c, c.add(0, 9, 0), STORM_R * 0.6, t * 0.4, 10, 0, 0.6f, DUST);
                Fx.groundRing(sl, null, c, (float) STORM_R, SAND, 10);
                d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.6f);
            }
            sandPuff(sl, c.add(0, 2, 0), 16, STORM_R * 0.6);
        }
        return t < 58;
    }

    // ---- Sư Tử Phi Dược: bật lên (đánh dấu vòng 5, 26t) -> treo trên điểm -> giáng t26 + vòng hút 9
    private static boolean lionLeap(ServerLevel sl, SaHatVuong d, int t) {
        double rad = 5;
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "skill_c");
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) rad, 26, DANGER);
            Fx.telegraphCircle(sl, null, d.spot, 9f, 30, GRAV);
            Fx.groundRing(sl, null, d.position(), 4f, GRAV, 16);
            d.playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.6f);
        }
        if (t < 10) { d.setDeltaMovement(0, 0.9, 0); d.hurtMarked = true; sl.sendParticles(ParticleTypes.REVERSE_PORTAL, d.getX(), d.getY(), d.getZ(), 8, 0.6, 0.2, 0.6, 0.05); }
        else if (t == 16) { d.teleportTo(d.spot.x, d.spot.y + 8, d.spot.z); d.setDeltaMovement(Vec3.ZERO); }
        else if (t > 16 && t < 26) d.setDeltaMovement(Vec3.ZERO);
        else if (t == 26) {
            Vec3 s = d.spot;
            Fx.ribbon(sl, null, d.position(), s, 0, 3, 0, 0.8f, GRAV);
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, s, 9)) {
                double q = flat2(p.position(), s);
                if (q <= rad * rad) { if (d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP); }
                else if (q <= 81) {
                    Vec3 in = s.subtract(p.position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(in.scale(0.9))); p.hurtMarked = true;
                }
            }
            Fx.groundCrack(sl, null, s, (float) rad, GRAV, 8, 40);
            Fx.groundRing(sl, null, s, 9f, SAND, 20);
            addSand(sl, d, s, 3.5, 80, 0.15);
            sandPuff(sl, s, 60, 3);
            Fx.shake(sl, s, 2.5f, 16, 32f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.6f);
        }
        if (t == 30) d.setNoGravity(false);
        return t < 38;
    }

    // ---- Tinh Vẫn Giáng Thế: bay vút (t0-24) -> biến mất trên trời, vòng 18 trên đất (t28-74) -> giáng như thiên thạch t74
    public static final double MET_R = 18, MET_CORE = 7, MET_H = 40;
    public static final int MET_MARK = 28, MET_HIT = 74, MET_END = 96;

    private static boolean meteor(ServerLevel sl, SaHatVuong d, int t) {
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "phase");
            Fx.groundRing(sl, null, d.position(), 8f, GRAV, 30);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 3f, GRAV, 0);
            sandPuff(sl, d.position(), 120, 5);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 4f, 0.4f);
        }
        if (t < 24) {
            d.setDeltaMovement(0, 0.25 + t * 0.06, 0); d.hurtMarked = true;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, d.getX(), d.getY(), d.getZ(), 16, 0.8, 0.5, 0.8, 0.05);
            sl.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY(), d.getZ(), 6, 0.6, 0.3, 0.6, 0.02);
        }
        if (t == 24) { d.setDeltaMovement(Vec3.ZERO); d.setInvisible(true); d.playSound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 4f, 0.4f); }
        if (t == MET_MARK) {
            d.spot = tpos(d);
            d.teleportTo(d.spot.x, d.spot.y + MET_H, d.spot.z);
            Fx.telegraphCircle(sl, null, d.spot, (float) MET_R, MET_HIT - MET_MARK, DANGER);
            Fx.telegraphCircle(sl, null, d.spot, (float) MET_CORE, MET_HIT - MET_MARK, STAR);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) MET_R * 0.6f, GRAV, MET_HIT - MET_MARK, 8);
        }
        if (t > MET_MARK && t < MET_HIT) {
            d.setDeltaMovement(Vec3.ZERO);
            if (t % 4 == 0) sl.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    d.spot.x, d.spot.y + 6, d.spot.z, 30, MET_R * 0.5, 3, MET_R * 0.5, 0);
        }
        if (t == MET_HIT - 14) { // xuất hiện như sao băng rực cháy, lao xuống
            d.setInvisible(false);
            Fx.ribbon(sl, null, d.spot.add(14, MET_H + 10, 6), d.spot.add(0, 1, 0), 0, 14, 0, 1.6f, STAR);
            Fx.ribbonHelix(sl, null, d.spot.add(0, MET_H, 0), d.spot.add(0, 1, 0), 2.0, 0, 14, 0, 0.6f, GRAV);
            d.playSound(SoundEvents.BLAZE_SHOOT, 5f, 0.3f);
        }
        if (t > MET_HIT - 14 && t < MET_HIT) {
            double k = (t - (MET_HIT - 14)) / 14.0;
            Vec3 q = d.spot.add(0, MET_H * (1 - k * k), 0);
            d.teleportTo(q.x, q.y, q.z);
            sl.sendParticles(ParticleTypes.FLAME, q.x, q.y + 1, q.z, 30, 1.2, 1.5, 1.2, 0.05);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, q.x, q.y + 3, q.z, 10, 1.0, 2.0, 1.0, 0.02);
        }
        if (t == MET_HIT) {
            Vec3 s = d.spot;
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, s, MET_R)) {
                double q = flat2(p.position(), s);
                if (q > MET_R * MET_R) continue;
                boolean core = q <= MET_CORE * MET_CORE;
                if (d.strike(p, d.moveDamage(mult(d.mv) * (core ? 1.0 : 0.45)))) PhaseMoves.applyHit(p, core ? Hit.KNOCK_UP : Hit.BLIND);
                Vec3 out = p.position().subtract(s).multiply(1, 0, 1);
                if (out.lengthSqr() > 1e-4) { p.setDeltaMovement(p.getDeltaMovement().add(out.normalize().scale(1.2)).add(0, 0.5, 0)); p.hurtMarked = true; }
            }
            Fx.groundCrack(sl, null, s, (float) MET_R * 0.7f, STAR, 14, 60);
            Fx.groundRing(sl, null, s, (float) MET_R, SAND, 30);
            Fx.groundRing(sl, null, s, (float) MET_CORE, GRAV, 30);
            Fx.elem(sl, null, Fx.E_NUKE, s, Vec3.ZERO, 3f, STAR, 0);
            Fx.elem(sl, null, Fx.E_BOULDER, s, Vec3.ZERO, 3f, DUST, 0);
            Fx.pool(sl, null, s, 6f, STAR, 100);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 1, s.z, 3, 2, 0.5, 2, 0);
            sandPuff(sl, s, 200, MET_R * 0.5);
            Fx.shake(sl, s, 5f, 30, 60f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 5f, 0.4f);
            d.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 5f, 0.5f);
            // dư chấn: cầu trọng lực bung ra + cát lún quanh hố
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2;
                d.orbs.add(new Orb(s.add(Math.cos(a) * 4, 2, Math.sin(a) * 4), 70 + i * 6, d.moveDamage(0.8)));
                addSand(sl, d, s.add(Math.cos(a + 0.8) * 10, 0, Math.sin(a + 0.8) * 10), 3, 100, 0.15);
            }
        }
        if (t == MET_HIT + 4) d.setNoGravity(false);
        return t < MET_END;
    }

    // ------------------------------------------------------------------ trợ giúp
    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static void line(ServerLevel sl, SaHatVuong d, Vec3 from, Vec3 dir, double len, double half, double mul, Hit h) {
        for (Player p : players(sl, from.add(dir.scale(len / 2)), len / 2 + 1)) {
            Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > half) continue;
            if (d.hitOnce.add(p.getUUID()) && d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void sandPuff(ServerLevel sl, Vec3 c, int n, double spread) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), c.x, c.y + 0.3, c.z, n, spread, 0.4, spread, 0.1);
        sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.5, c.z, Math.max(1, n / 4), spread, 0.3, spread, 0.02);
    }

    static void addSand(ServerLevel sl, SaHatVuong d, Vec3 c, double r, int life, double mul) {
        d.sands.add(new Sand(c, r, life, d.moveDamage(mul)));
        Fx.pool(sl, null, c, (float) r, SAND, life);
    }

    /** Cát lún: kéo chìm (vận tốc xuống + chậm nặng), mỗi 10t sát thương phép. */
    static void tickSands(ServerLevel sl, SaHatVuong d) {
        for (Iterator<Sand> it = d.sands.iterator(); it.hasNext(); ) {
            Sand s = it.next();
            if (--s.left <= 0) { it.remove(); continue; }
            if (s.left % 6 == 0) sandPuff(sl, s.c, 3, s.r * 0.5);
            for (Player pl : players(sl, s.c, s.r)) {
                if (flat2(pl.position(), s.c) > s.r * s.r || Math.abs(pl.getY() - s.c.y) > 2) continue;
                pl.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                Vec3 in = s.c.subtract(pl.position()).multiply(1, 0, 1);
                pl.setDeltaMovement(pl.getDeltaMovement().multiply(0.6, 1, 0.6).add(in.scale(0.03)).add(0, -0.08, 0));
                pl.hurtMarked = true;
                if (s.left % 10 == 0) pl.hurt(d.damageSources().magic(), s.dmg);
            }
        }
    }

    // ------------------------------------------------------------------ VFX pha
    /** Lộ diện pha: P2 trọng lực bùng (cát lơ lửng), P3 bão cát + vòng sao. */
    static void transformFx(ServerLevel sl, SaHatVuong d, int ph) {
        d.triggerAnim("skill", "phase");
        Vec3 c = d.position();
        if (ph == 2) {
            Fx.groundRing(sl, null, c, 10f, GRAV, 40);
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 2.5, 0), Vec3.ZERO, 3.5f, GRAV, 0);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Fx.ribbonHelix(sl, null, c.add(Math.cos(a) * 6, 0, Math.sin(a) * 6), c.add(Math.cos(a) * 3, 7, Math.sin(a) * 3), 0.8, a, 16, i * 2, 0.3f, SAND);
            }
            sandPuff(sl, c, 150, 5);
            d.playSound(SoundEvents.BEACON_POWER_SELECT, 4f, 0.4f);
        } else if (ph >= 3) {
            Fx.groundCrack(sl, null, c, 9f, STAR, 12, 60);
            Fx.ribbonHelix(sl, null, c, c.add(0, 14, 0), 6, 0, 20, 0, 0.8f, DUST);
            Fx.elem(sl, null, Fx.E_TONGUES, c, Vec3.ZERO, 3.5f, STAR, 0);
            Fx.pool(sl, null, c, 7f, SAND, 120);
            sandPuff(sl, c, 250, 8);
            d.playSound(SoundEvents.WARDEN_ROAR, 4f, 0.7f);
        }
    }

    // ------------------------------------------------------------------ cutscene (thoại: hukhong.dialog.sahat.*)
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: xa giữa bão cát -> sát đất nhìn lên song kiếm -> cận mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 6.0, 1.0, 12.0, 78, 2.0, -4), k(T * 2 / 5, -3.5, 0.3, 4.0, 60, 3.0, -16),
                k(T * 3 / 4, 0.8, 2.6, 3.4, 46, 2.6, 0), k(T, 0, 2.6, 7.5, 68, 2.2, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.sa_hat_vuong"), new SubLine(36, 72, "hukhong.dialog.sahat.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.sahat.intro.2"));
    }

    static final int ULT_T = 80;

    /** Tinh Vẫn: camera thấp nhìn boss bay vút lên trời rồi ngước theo. */
    private static void ultScene(ServerLevel sl, SaHatVuong d) {
        double s = Math.max(1.0, d.director().currentScale());
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 4.0 * s, 0.6 * s, 6.0 * s, 64, 2.0 * s, -6), k(22, 3.0 * s, 0.4 * s, 7.0 * s, 72, 10.0, -35),
                k(50, -5.0, 1.0, 10.0, 80, 24.0, -50), k(ULT_T, 0, 4.0, 16.0, 85, 6.0, -10));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, ULT_T, cam, List.of(new SubLine(2, 26, "hukhong.dialog.sahat.ult.1"),
                new SubLine(26, 54, "hukhong.dialog.sahat.ult.2"), new SubLine(54, ULT_T - 2, "hukhong.dialog.sahat.ult.3")));
    }

    /** Tử trận: thân hoá cát, song kiếm cắm xuống đất, cầu trọng lực tan. */
    static void deathScene(ServerLevel sl, SaHatVuong d) {
        int T = 100;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 2.0, 5.5, 55, 1.6, 0), k(50, 3.5, 2.4, 3.5, 60, 1.4, 0), k(T, 0, 6.0, 11.0, 74, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 36, "hukhong.dialog.sahat.death.1"),
                new SubLine(36, 68, "hukhong.dialog.sahat.death.2"), new SubLine(68, T - 4, "hukhong.dialog.sahat.death.3")));
        Fx.groundRing(sl, null, at, 9f, SAND, 60);
        Fx.pool(sl, null, at, 5f, SAND, 140);
        Fx.ribbonHelix(sl, null, at, at.add(0, 12, 0), 3, 0, 30, 0, 0.5f, DUST);
        sandPuff(sl, at.add(0, 1.5, 0), 250, 3);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 2, at.z, 120, 2.5, 2, 2.5, 0.05);
    }
}
