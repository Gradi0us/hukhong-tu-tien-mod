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
 * Bộ chiêu riêng của Chấp Hành Giả III — Lăng Uyên (kiểu Morgott / Margit, chủ đề vực sâu).
 * Vũ khí hư ảnh triệu từ vực: phi đao, cự kiếm, cự chùy, mâu trời; huyết uyên (máu nguyền) thành vũng rồi trào gai.
 * Mọi đòn có telegraph và nhịp TRỄ (đừng lăn sớm). Mỗi chiêu = dòng thời gian theo tick trong {@link #step}.
 * <ul>
 *   <li>c3_phi_dao — Uyên Nhận Trì Phát: 5 phi đao lần lượt khoá mục tiêu rồi phóng; đao cuối trễ thêm.</li>
 *   <li>c3_cu_kiem — Uyên Kiếm Hiện: 2 nhát chưởng nhanh, cự kiếm hiện giữa chuỗi, bổ dọc trễ.</li>
 *   <li>c3_huyet_tri — Huyết Trì Uyên Thứ: vũng huyết uyên dưới chân, trào gai 2 lần (lần 2 trễ).</li>
 *   <li>c3_uyen_chuy — Uyên Chùy Giáng: nhảy lên, cự chùy hiện, giáng trễ + sóng nhẫn.</li>
 *   <li>c3_huyet_kiem — Huyết Kiếm Uyên Lãng (P3): 3 nhát kiếm máu nguyền tạo sóng thẳng, nhát 3 trễ để lại vũng.</li>
 *   <li>c3_phi_dao_vu — Uyên Nhận Vũ (P3): 3 đợt quạt 3 phi đao, đợt cuối trễ.</li>
 *   <li>c3_song_chuy — Song Chùy Uyên Chấn (P3): đập 1 nhanh, đập 2 trễ kèm 3 vòng gai trào.</li>
 *   <li>c3_van_mau — Vạn Mâu Uyên Giáng (tối thượng): mưa mâu từ trời rồi cả vòng đất trào, an toàn sát chân boss.</li>
 * </ul>
 */
public final class Chg3Moves {
    private Chg3Moves() {}

    public static final int ABYSS = 0x5A3CC8, VOID = 0x2A1060, CURSE = 0xB0103A, GOLD = 0xE8C060, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("c3_phi_dao", "c3_cu_kiem", "c3_huyet_tri", "c3_uyen_chuy",
            "c3_van_mau", "c3_huyet_kiem", "c3_phi_dao_vu", "c3_song_chuy");

    public static boolean handles(String id) { return IDS.contains(id); }

    /** Vũng huyết uyên lâu dài: sát thương theo nhịp. */
    public static final class Pool {
        final Vec3 c; final double r; int left; final float dmg;
        Pool(Vec3 c, double r, int left, float dmg) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; }
    }

    /** Đòn hẹn giờ (mâu rơi / gai trào): nổ tại c ở tick at. */
    public record Strike(Vec3 c, int at, double r, double mul, Hit hit, int kind) {}

    // ------------------------------------------------------------------ chọn chiêu
    static String pick(RandomSource rnd, int phase, boolean ultReady, float dist) {
        if (phase >= 3 && ultReady && rnd.nextInt(5) == 0) return "c3_van_mau";
        if (dist > 10 && rnd.nextBoolean()) return phase >= 3 && rnd.nextBoolean() ? "c3_phi_dao_vu" : "c3_phi_dao";
        List<String> pool = phase >= 3
                ? List.of("c3_huyet_kiem", "c3_song_chuy", "c3_phi_dao_vu", "c3_cu_kiem", "c3_huyet_tri", "c3_uyen_chuy")
                : phase == 2
                ? List.of("c3_phi_dao", "c3_cu_kiem", "c3_huyet_tri", "c3_uyen_chuy")
                : List.of("c3_phi_dao", "c3_cu_kiem", "c3_huyet_tri");
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "chg_3". */
    static double mult(String id) {
        return switch (id) {
            case "c3_phi_dao" -> 0.6;
            case "c3_cu_kiem" -> 1.1;
            case "c3_huyet_tri" -> 1.0;
            case "c3_uyen_chuy" -> 1.3;
            case "c3_huyet_kiem" -> 1.2;
            case "c3_phi_dao_vu" -> 0.6;
            case "c3_song_chuy" -> 1.2;
            case "c3_van_mau" -> 2.5;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, ChapHanhGia d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("c3_van_mau");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2f, ult ? 0.4f : 0.7f);
        if (ult) { d.ultCd = 900; ultScene(sl, d); }
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
        fireQueue(sl, d, t);
        boolean more = switch (d.mv) {
            case "c3_phi_dao" -> daggers(sl, d, t);
            case "c3_phi_dao_vu" -> daggerFans(sl, d, t);
            case "c3_cu_kiem" -> greatsword(sl, d, t);
            case "c3_huyet_tri" -> bloodPools(sl, d, t);
            case "c3_uyen_chuy" -> hammerLeap(sl, d, t);
            case "c3_huyet_kiem" -> bloodSword(sl, d, t);
            case "c3_song_chuy" -> doubleHammer(sl, d, t);
            default -> spearRain(sl, d, t);
        };
        return more || !d.queue.isEmpty();
    }

    // ---- hàng đợi đòn hẹn giờ
    private static void fireQueue(ServerLevel sl, ChapHanhGia d, int t) {
        for (Iterator<Strike> it = d.queue.iterator(); it.hasNext(); ) {
            Strike s = it.next();
            if (s.at() > t) continue;
            it.remove();
            hitCircle(sl, d, s.c(), s.r(), mult(d.mv) * s.mul(), s.hit());
            if (s.kind() == 1) {  // mâu cắm đất
                sl.sendParticles(ParticleTypes.SCULK_SOUL, s.c().x, s.c().y + 0.4, s.c().z, 4, 0.3, 0.2, 0.3, 0.02);
                Fx.groundCrack(sl, null, s.c(), (float) s.r(), ABYSS, 5, 20);
            } else {              // gai huyết uyên trào
                Fx.elem(sl, null, Fx.E_SPIKES, s.c(), Vec3.ZERO, (float) s.r(), CURSE, 24);
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, s.c().x, s.c().y + 0.6, s.c().z, 14, s.r() * 0.4, 0.6, s.r() * 0.4, 0.02);
                d.playSound(SoundEvents.POINTED_DRIPSTONE_LAND, 1.5f, 0.6f);
            }
        }
    }

    private static void queue(ChapHanhGia d, Vec3 c, int at, double r, double mul, Hit h, int kind) {
        d.queue.add(new Strike(c, at, r, mul, h, kind));
    }

    // ---- Uyên Nhận Trì Phát: đao i khoá ở 12+6i (đao cuối +14), phóng sau 12 tick theo vạch
    static int daggerLock(int i, int n) { return 12 + i * 6 + (i == n - 1 ? 14 : 0); }

    private static boolean daggers(ServerLevel sl, ChapHanhGia d, int t) {
        int n = 5;
        if (t == 0) {
            d.triggerAnim("skill", "skill_a");
            conjure(sl, d, n, ABYSS);
            d.marks.clear();
            for (int i = 0; i < n; i++) d.marks.add(null);
        }
        for (int i = 0; i < n; i++) {
            int lock = daggerLock(i, n);
            if (t == lock) {
                reaim(d);
                Vec3 o = d.position().add(0, 0.05, 0);
                Vec3 v = d.aim.scale(20);
                d.marks.set(i, v);
                Fx.telegraphLine(sl, null, o, v, 1.4f, 12, i == n - 1 ? DANGER : ABYSS);
                d.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 1.6f - i * 0.1f);
            }
            if (t == lock + 12 && d.marks.get(i) != null) {
                Vec3 o = d.position();
                Vec3 v = d.marks.get(i);
                Fx.swordFly(sl, o.add(0, d.getBbHeight() * 0.8, 0), o.add(v).add(0, 1, 0), 6, 0, 0.8f);
                Fx.ribbon(sl, null, o.add(0, 1.4, 0), o.add(v).add(0, 1, 0), 0, 6, 0, 0.25f, ABYSS);
                lineHit(sl, d, o, v, 1.3, mult(d.mv), Hit.SLOW, true);
                d.hitOnce.clear();
                d.playSound(SoundEvents.TRIDENT_THROW.value(), 2f, 1.2f);
            }
        }
        return t < daggerLock(n - 1, n) + 20;
    }

    // ---- Uyên Nhận Vũ: 3 đợt quạt (-20/0/+20); khoá 10, 34, 68 (đợt cuối trễ); phóng sau 14 tick
    static final int[] FAN_LOCK = {10, 34, 68};

    private static boolean daggerFans(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { d.triggerAnim("skill", "skill_a"); conjure(sl, d, 9, CURSE); }
        for (int w = 0; w < FAN_LOCK.length; w++) {
            if (t == FAN_LOCK[w]) {
                reaim(d);
                d.from = d.position();
                for (int i = -1; i <= 1; i++) Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), rot(d.aim, i * 20).scale(22), 1.4f, 14, w == 2 ? DANGER : CURSE);
                d.spot = d.aim;
                d.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.3f);
            }
            if (t == FAN_LOCK[w] + 14) {
                d.hitOnce.clear();
                for (int i = -1; i <= 1; i++) {
                    Vec3 v = rot(d.spot, i * 20).scale(22);
                    Fx.swordFly(sl, d.from.add(0, d.getBbHeight() * 0.8, 0), d.from.add(v).add(0, 1, 0), 6, 0, 0.8f);
                    lineHit(sl, d, d.from, v, 1.3, mult(d.mv), Hit.WEAK, true);
                }
                d.playSound(SoundEvents.TRIDENT_THROW.value(), 2.5f, 0.9f);
            }
        }
        return t < FAN_LOCK[2] + 22;
    }

    // ---- Uyên Kiếm Hiện: chưởng t10, t20 (nón 4.5) -> cự kiếm hiện t22 -> bổ dọc TRỄ t54 theo vạch 10x3
    public static final int GS_SLAM = 54;

    private static boolean greatsword(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 4.5f, 60, 10); }
        if (t == 10 || t == 20) {
            reaim(d);
            lunge(d, 0.7);
            d.triggerAnim("skill", "attack");
            cone(sl, d, 4.5, 60, 0.6, t == 10 ? 30 : -30);
        }
        if (t == 22) {  // cự kiếm hư ảnh hiện giữa chuỗi
            reaim(d);
            d.from = d.position();
            d.to = d.from.add(d.aim.scale(10));
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), 3.0f, GS_SLAM - 22, DANGER);
            Fx.rune(sl, null, d.position().add(0, d.getBbHeight() + 1, 0), 2f, ABYSS, GS_SLAM - 22, 6);
            d.triggerAnim("skill", "skill_c");
            d.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.5f, 0.6f);
        }
        if (t > 22 && t < GS_SLAM && t % 3 == 0)
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, d.getX(), d.getY() + d.getBbHeight() + 1.5, d.getZ(), 6, 0.4, 1.0, 0.4, 0.02);
        if (t == GS_SLAM - 10) Fx.giantSword(sl, null, d.from.add(d.aim.scale(5)), 3f, ABYSS, 10, 40);
        if (t == GS_SLAM) {
            lineHit(sl, d, d.from, d.to.subtract(d.from), 1.6, mult(d.mv) * 1.4, Hit.KNOCK_UP, false);
            Fx.groundCrack(sl, null, d.from.add(d.aim.scale(5)), 5f, ABYSS, 8, 40);
            Fx.rift(sl, null, d.from.add(0, 0.6, 0), d.to.subtract(d.from), 1.2f, VOID, 30);
            Fx.shake(sl, d.position(), 2f, 12, 28f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
        }
        return t < GS_SLAM + 10;
    }

    // ---- Huyết Trì Uyên Thứ: vũng dưới chân mỗi người + 3 quanh boss; trào t30, trào lại TRỄ t60
    private static boolean bloodPools(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "skill_b");
            d.marks.clear();
            for (Player p : players(sl, d.position(), 24)) d.marks.add(p.position());
            for (int i = 0; i < 3; i++) {
                double a = d.getRandom().nextDouble() * Math.PI * 2, r = 3 + d.getRandom().nextDouble() * 6;
                d.marks.add(d.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
            for (Vec3 c : d.marks) {
                Fx.pool(sl, null, c, 2.6f, CURSE, 70);
                Fx.telegraphCircle(sl, null, c, 2.6f, 30, DANGER);
                queue(d, c, 30, 2.6, 1.0, Hit.KNOCK_UP, 2);
                queue(d, c, 60, 3.2, 0.8, Hit.WEAK, 2);
            }
            d.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2f, 0.6f);
        }
        if (t == 34) for (Vec3 c : d.marks) Fx.telegraphCircle(sl, null, c, 3.2f, 26, DANGER);  // vũng chưa cạn: sắp trào lần 2
        if (t % 5 == 0) for (Vec3 c : d.marks) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, c.x, c.y + 0.2, c.z, 3, 1, 0.05, 1, 0.01);
        return t < 64;
    }

    // ---- Uyên Chùy Giáng: bật lên, vòng 5 tại mục tiêu, cự chùy hiện, giáng TRỄ t40, sóng nhẫn 5..9 tại t48
    public static final int HL_SLAM = 40;

    private static boolean hammerLeap(ServerLevel sl, ChapHanhGia d, int t) {
        double rad = 5;
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "skill_c");
            d.spot = tpos(d);
            Fx.telegraphCircle(sl, null, d.spot, (float) rad, HL_SLAM, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) rad * 0.7f, ABYSS, HL_SLAM, 6);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2f, 1.2f);
        }
        if (t < 10) { d.setDeltaMovement(0, 0.8, 0); d.hurtMarked = true; }
        else if (t == 16) {
            d.teleportTo(d.spot.x, d.spot.y + 9, d.spot.z);
            d.setDeltaMovement(Vec3.ZERO);
            Fx.elem(sl, null, Fx.E_BOULDER, d.position().add(0, 2, 0), Vec3.ZERO, 1.6f, ABYSS, HL_SLAM - 16);  // cự chùy hư ảnh
        } else if (t > 16 && t < HL_SLAM) {
            d.setDeltaMovement(Vec3.ZERO);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, d.getX(), d.getY() + 2, d.getZ(), 8, 0.6, 0.6, 0.6, 0.05);
        } else if (t == HL_SLAM) {
            Vec3 s = d.spot;
            Fx.ribbon(sl, null, d.position(), s, 0, 3, 0, 0.8f, ABYSS);
            d.teleportTo(s.x, s.y, s.z);
            d.setDeltaMovement(Vec3.ZERO);
            d.setNoGravity(false);
            hitCircle(sl, d, s, rad, mult(d.mv), Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, s, (float) rad, ABYSS, 10, 40);
            Fx.shake(sl, s, 2.5f, 16, 32f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.ANVIL_LAND, 2.5f, 0.5f);
            Fx.telegraphCircle(sl, null, s, 9f, 8, ABYSS);
        } else if (t == HL_SLAM + 8) {  // sóng nhẫn: an toàn sát chân hoặc ngoài 9
            ring(sl, d, d.spot, rad, 9, mult(d.mv) * 0.6, Hit.SLOW);
            Fx.groundRing(sl, null, d.spot, 9f, VOID, 20);
        }
        return t < HL_SLAM + 14;
    }

    // ---- Huyết Kiếm Uyên Lãng: 3 nhát (t24, t44, t82 TRỄ); mỗi nhát sóng thẳng 18; nhát cuối để lại vũng
    static final int[] BS_SLAM = {24, 44, 82};

    private static boolean bloodSword(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { d.triggerAnim("skill", "skill_c"); Fx.rune(sl, null, d.position().add(0, 0.1, 0), 3f, CURSE, 90, 7); }
        for (int k = 0; k < 3; k++) {
            int tele = k == 2 ? 30 : 16;
            if (t == BS_SLAM[k] - tele) {
                reaim(d);
                d.from = d.position();
                d.to = d.from.add(d.aim.scale(18));
                Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.to.subtract(d.from), k == 2 ? 4f : 3f, tele, DANGER);
                d.playSound(SoundEvents.SCULK_CATALYST_BLOOM, 2.5f, 0.6f);
            }
            if (t == BS_SLAM[k]) {
                d.triggerAnim("skill", "attack");
                Vec3 v = d.to.subtract(d.from);
                Fx.giantSword(sl, null, d.from.add(d.aim.scale(3)), 2.5f, CURSE, 4, 24);
                Fx.ribbonWave(sl, null, d.from.add(0, 0.5, 0), d.to.add(0, 0.5, 0), 0.5, 0.6, 10, 2, 1.2f, CURSE);
                lineHit(sl, d, d.from, v, k == 2 ? 2.0 : 1.5, mult(d.mv) * (k == 2 ? 1.3 : 1.0), Hit.WEAK, false);
                Fx.shake(sl, d.position(), 1.5f + k, 10, 28f);
                d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.8f - k * 0.1f);
                if (k == 2) for (int i = 1; i <= 3; i++) addPool(sl, d, d.from.add(d.aim.scale(i * 5)), 2.5, 120, 0.25);
            }
        }
        return t < BS_SLAM[2] + 10;
    }

    // ---- Song Chùy Uyên Chấn: đập 1 t18 (r4 trước mặt), đập 2 TRỄ t50 (r6) + 3 vòng gai 3/6/9 tại t54/58/62
    private static boolean doubleHammer(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCircle(sl, null, d.position().add(d.aim.scale(3)), 4f, 18, DANGER); d.triggerAnim("skill", "skill_b"); }
        if (t == 18) {
            Vec3 c = d.position().add(d.aim.scale(3));
            hitCircle(sl, d, c, 4, mult(d.mv), Hit.KNOCK_UP);
            Fx.elem(sl, null, Fx.E_BOULDER, c.add(0, 1, 0), Vec3.ZERO, 1.4f, ABYSS, 10);
            Fx.groundCrack(sl, null, c, 4f, ABYSS, 6, 30);
            d.playSound(SoundEvents.ANVIL_LAND, 2f, 0.6f);
        }
        if (t == 22) {  // giơ chùy, dồn lực
            reaim(d);
            d.spot = d.position().add(d.aim.scale(3));
            Fx.telegraphCircle(sl, null, d.spot, 6f, 28, DANGER);
            Fx.telegraphCircle(sl, null, d.spot, 9f, 40, CURSE);
        }
        if (t > 22 && t < 50 && t % 4 == 0) sl.sendParticles(ParticleTypes.SCULK_SOUL, d.getX(), d.getY() + d.getBbHeight() + 1, d.getZ(), 4, 0.4, 0.4, 0.4, 0.02);
        if (t == 50) {
            d.triggerAnim("skill", "attack");
            hitCircle(sl, d, d.spot, 6, mult(d.mv) * 1.2, Hit.KNOCK_UP);
            Fx.elem(sl, null, Fx.E_BOULDER, d.spot.add(0, 1, 0), Vec3.ZERO, 2f, ABYSS, 12);
            Fx.shake(sl, d.spot, 3f, 14, 30f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, d.spot.x, d.spot.y + 0.5, d.spot.z, 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.5f);
            for (int k = 1; k <= 3; k++) {
                int n = 6 * k;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    queue(d, d.spot.add(Math.cos(a) * 3 * k, 0, Math.sin(a) * 3 * k), t + 4 * k, 1.6, 0.5, Hit.SLOW, 2);
                }
            }
        }
        return t < 52;
    }

    // ---- Vạn Mâu Uyên Giáng (tối thượng): bay lên; t20..76 mưa mâu (vòng nhỏ 12 tick trước khi cắm);
    //      t84 hạ xuống tâm; t100 cả vòng 20 trào - chỉ an toàn trong 4 block sát chân boss.
    public static final int VM_RAIN_END = 76, VM_LAND = 84, VM_ERUPT = 100, VM_END = 112;
    public static final double VM_RAD = 20, VM_SAFE = 4;

    private static boolean spearRain(ServerLevel sl, ChapHanhGia d, int t) {
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "phase");
            d.spot = d.position();
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2, 0), Vec3.ZERO, 3f, ABYSS, 0);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < 20) { d.setDeltaMovement(0, 0.3, 0); d.hurtMarked = true; return true; }
        if (t == 20) {
            Fx.rune(sl, null, d.position().add(0, 3, 0), 8f, ABYSS, VM_LAND - 20, 10);  // trận mâu trên trời
            Fx.telegraphCircle(sl, null, d.spot, (float) VM_RAD, VM_ERUPT - 20, DANGER);
        }
        if (t < VM_LAND) d.setDeltaMovement(Vec3.ZERO);
        if (t >= 20 && t <= VM_RAIN_END && t % 4 == 0) {
            RandomSource r = d.getRandom();
            for (int i = 0; i < 3; i++) {
                double a = r.nextDouble() * Math.PI * 2, rr = VM_SAFE + r.nextDouble() * (VM_RAD - VM_SAFE);
                spear(sl, d, d.spot.add(Math.cos(a) * rr, 0, Math.sin(a) * rr), t);
            }
            if (t % 12 == 0) for (Player p : players(sl, d.spot, VM_RAD)) spear(sl, d, p.position(), t);
        }
        if (t == VM_LAND) {
            d.teleportTo(d.spot.x, d.spot.y, d.spot.z);
            d.setDeltaMovement(Vec3.ZERO);
            d.setNoGravity(false);
            Fx.telegraphCircle(sl, null, d.spot, (float) VM_SAFE, VM_ERUPT - VM_LAND, GOLD);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) VM_SAFE, GOLD, VM_ERUPT - VM_LAND, 5);
            d.playSound(SoundEvents.ANVIL_LAND, 2.5f, 0.4f);
        }
        if (t > VM_LAND && t < VM_ERUPT && t % 3 == 0)
            sl.sendParticles(ParticleTypes.SCULK_SOUL, d.spot.x, d.spot.y + 0.2, d.spot.z, 20, VM_RAD * 0.5, 0.1, VM_RAD * 0.5, 0.02);
        if (t == VM_ERUPT) {
            ring(sl, d, d.spot, VM_SAFE, VM_RAD, mult(d.mv), Hit.KNOCK_UP);
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                Vec3 tip = d.spot.add(Math.cos(a) * VM_RAD, 0, Math.sin(a) * VM_RAD);
                Fx.ribbonWave(sl, null, d.spot.add(0, 0.4, 0), tip, 1.0, 0.6, 12, 0, 0.7f, ABYSS);
            }
            Fx.elem(sl, null, Fx.E_SPIKES, d.spot, Vec3.ZERO, (float) VM_RAD * 0.6f, VOID, 40);
            Fx.groundCrack(sl, null, d.spot, (float) VM_RAD, ABYSS, 16, 60);
            Fx.elem(sl, null, Fx.E_NUKE, d.spot, Vec3.ZERO, 6f, ABYSS, 0);
            Fx.shake(sl, d.spot, 4f, 30, 60f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.4f);
            d.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.6f);
        }
        return t < VM_END;
    }

    private static void spear(ServerLevel sl, ChapHanhGia d, Vec3 c, int t) {
        Fx.telegraphCircle(sl, null, c, 1.8f, 12, DANGER);
        Fx.swordStab(sl, c.add(0, 14, 0), c, 8, 4, 1.4f);
        queue(d, c, t + 12, 1.8, 0.35, Hit.SLOW, 1);
    }

    // ------------------------------------------------------------------ trợ giúp
    private static void conjure(ServerLevel sl, ChapHanhGia d, int n, int rgb) {
        Vec3 back = d.position().add(0, d.getBbHeight() * 0.9, 0);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * (0.15 + 0.7 * i / Math.max(1, n - 1));
            Vec3 side = new Vec3(-d.aim.z, 0, d.aim.x);
            Vec3 at = back.add(side.scale(Math.cos(a) * 2.2)).add(0, Math.sin(a) * 1.4, 0);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 8, 0.1, 0.2, 0.1, 0.02);
        }
        Fx.rune(sl, null, back.add(0, 0.6, 0), 1.8f, rgb, 30, n);
    }

    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    private static void lunge(ChapHanhGia d, double v) {
        d.setDeltaMovement(d.aim.x * v, 0.1, d.aim.z * v);
        d.hurtMarked = true;
    }

    private static void hitCircle(ServerLevel sl, ChapHanhGia d, Vec3 c, double r, double mul, Hit h) {
        for (Player p : players(sl, c, r))
            if (flat2(p.position(), c) <= r * r && Math.abs(p.getY() - c.y) <= 3 && d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
    }

    /** Vòng nhẫn [in, out]. */
    private static void ring(ServerLevel sl, ChapHanhGia d, Vec3 c, double in, double out, double mul, Hit h) {
        for (Player p : players(sl, c, out)) {
            double f = flat2(p.position(), c);
            if (f >= in * in && f <= out * out && Math.abs(p.getY() - c.y) <= 3 && d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void lineHit(ServerLevel sl, ChapHanhGia d, Vec3 o, Vec3 v, double half, double mul, Hit h, boolean once) {
        double len = v.multiply(1, 0, 1).length();
        if (len < 1e-3) return;
        Vec3 dir = v.multiply(1, 0, 1).normalize();
        for (Player p : players(sl, o.add(v.scale(0.5)), len)) {
            Vec3 rel = p.position().subtract(o).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > half || Math.abs(p.getY() - o.y) > 3) continue;
            if (once && !d.hitOnce.add(p.getUUID())) continue;
            if (d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void cone(ServerLevel sl, ChapHanhGia d, double rad, double half, double mul, double roll) {
        double cos = Math.cos(Math.toRadians(half));
        Vec3 c = d.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(d.aim) < cos)) continue;
            if (d.strike(p, d.moveDamage(mult(d.mv) * mul))) PhaseMoves.applyHit(p, Hit.NONE);
        }
        Fx.qi(sl, c.add(0, 1.4, 0), d.aim.scale(rad * 0.8), (float) (rad * 0.6), ABYSS, roll, 10);
        d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.7f);
    }

    static void addPool(ServerLevel sl, ChapHanhGia d, Vec3 c, double r, int life, double mul) {
        d.pools.add(new Pool(c, r, life, d.moveDamage(mul)));
        Fx.pool(sl, null, c, (float) r, CURSE, life);
    }

    /** Vũng huyết uyên: mỗi 10 tick sát thương + héo. */
    static void tickPools(ServerLevel sl, ChapHanhGia d) {
        for (Iterator<Pool> it = d.pools.iterator(); it.hasNext(); ) {
            Pool p = it.next();
            if (--p.left <= 0) { it.remove(); continue; }
            if (p.left % 5 == 0) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, p.c.x, p.c.y + 0.3, p.c.z, 5, p.r * 0.5, 0.1, p.r * 0.5, 0.01);
            if (p.left % 10 != 0) continue;
            for (Player pl : players(sl, p.c, p.r)) {
                if (flat2(pl.position(), p.c) > p.r * p.r || Math.abs(pl.getY() - p.c.y) > 2) continue;
                pl.hurt(d.damageSources().magic(), p.dmg);
                pl.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
        }
    }

    // ------------------------------------------------------------------ biến hình + cutscene
    /** Lộ diện pha: P2 vực mở sau lưng (vòng hào quang + vũ khí hư ảnh), P3 máu nguyền trào. */
    static void transformFx(ServerLevel sl, ChapHanhGia d, int ph) {
        Vec3 c = d.position();
        if (ph == 2) {
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 2, 0), Vec3.ZERO, 3f, ABYSS, 0);
            Fx.rift(sl, null, c.add(-3, d.getBbHeight() * 0.6, 0), new Vec3(6, 0, 0), 2.5f, VOID, 80);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 2, c.z, 120, 2, 2, 2, 0.1);
            d.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 3f, 0.5f);
        } else if (ph >= 3) {
            Fx.pool(sl, null, c, 8f, CURSE, 100);
            Fx.elem(sl, null, Fx.E_SPIKES, c, Vec3.ZERO, 6f, CURSE, 40);
            Fx.groundCrack(sl, null, c, 10f, ABYSS, 12, 60);
            Fx.shake(sl, c, 3f, 20, 40f);
            d.playSound(SoundEvents.WARDEN_ROAR, 3f, 0.8f);
        }
    }

    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: từ đáy vực nhìn lên -> vòng ngang -> cận mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 2.0, 0.3, 6.0, 72, 3.5, -16), k(T * 2 / 5, -4.0, 1.6, 3.0, 60, 2.6, -4),
                k(T * 3 / 4, 0.6, 2.8, 2.6, 45, 3.0, 0), k(T, 0, 2.8, 6.5, 66, 2.4, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 34, "entity.hukhong.chg_3"), new SubLine(34, 72, "hukhong.dialog.chg3.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.chg3.intro.2"));
    }

    static final int ULT_T = 60;

    private static void ultScene(ServerLevel sl, ChapHanhGia d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 2.5 * s, 0.3 * s, 4.0 * s, 62, 2.0 * s, -10), k(ULT_T / 2, -2.0 * s, 0.4 * s, 4.5 * s, 72, 5.0 * s, -24),
                k(ULT_T, 0, 2.0 * s, 10.0 * s, 82, 6.0 * s, -20));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 30, "hukhong.dialog.chg3.ult.1"),
                new SubLine(30, ULT_T - 2, "hukhong.dialog.chg3.ult.2")));
    }

    static void deathScene(ServerLevel sl, ChapHanhGia d) {
        int T = 90;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.6, 4.5, 55, 1.4, 0), k(45, 2.8, 2.2, 3.0, 60, 1.2, 0), k(T, 0, 5.0, 9.0, 74, 1.0, -10));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 46, "hukhong.dialog.chg3.death.1"),
                new SubLine(46, T - 4, "hukhong.dialog.chg3.death.2")));
        Fx.groundRing(sl, null, at, 8f, ABYSS, 60);
        Fx.pool(sl, null, at, 5f, CURSE, 120);
        Fx.rift(sl, null, at.add(-2, 1.5, 0), new Vec3(4, 0, 0), 2f, VOID, 80);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.5, at.z, 150, 1.5, 2, 1.5, 0.05);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 1, at.z, 40, 1.5, 1, 1.5, 0.03);
    }
}
