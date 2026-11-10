package com.hukhong.entity;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;

/**
 * Bộ chiêu riêng của Hư Vương (kiểu Rykard + Godrick: rắn nuốt hồn, dung nham, sọ hư không phun trào, tay ghép, rìu gió, tay rồng phun lửa).
 * Chủ đề: Hư Không đói khát và nuốt chửng. Mỗi chiêu = dòng thời gian theo tick trong {@link #step}; mọi chiêu có telegraph trước sát thương.
 * <ul>
 *   <li>hvg_xa_ngoan (Xà Thần Nuốt Hồn): vạch dài -> rắn hư không lao cắn, kéo người trúng về phía boss.</li>
 *   <li>hvg_phong_phu (Phong Phủ Đạp): quạt rìu gió (đẩy lùi) rồi dậm đất quanh thân.</li>
 *   <li>hvg_cot_tra (Ghép Thủ Loạn Vũ): tay ghép quét 3 nhát luân phiên.</li>
 *   <li>hvg_dung_nham (Dung Nham Cuồn Cuộn): sóng phun trào dung nham chạy dọc vạch, để lại vũng magma.</li>
 *   <li>hvg_trieu_hoi (Triệu Hồn Bị Nuốt): gọi hồn đã nuốt (HuThi/HuCot) — chỉ AI, tối đa 6.</li>
 *   <li>hvg_long_tuc (Long Tức): tay rồng ghép phun lửa hình nón (pha 3).</li>
 *   <li>hvg_lau_cot (Lâu Cốt Phún Phát): sọ hư không trồi lên dưới chân từng người, 2 đợt.</li>
 *   <li>hvg_dia_chan (Địa Chấn Tam Đạp): 3 lần dậm, vòng lan rộng dần.</li>
 *   <li>hvg_thon_thien (Hư Không Thôn Thiên, tối thượng): bay lên, sọ khổng lồ hút mọi thứ vào rồi nổ, để lại hồ magma.</li>
 * </ul>
 */
public final class HuVuongMoves {
    private HuVuongMoves() {}

    public static final int VOID = 0xA050FF, DEEP = 0x4A1078, MAGMA = 0xFF5A1E, EMBER = 0xFFA040, WIND = 0xD8F0FF, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("hvg_xa_ngoan", "hvg_phong_phu", "hvg_cot_tra", "hvg_dung_nham", "hvg_trieu_hoi",
            "hvg_long_tuc", "hvg_lau_cot", "hvg_dia_chan", "hvg_thon_thien");

    public static boolean handles(String id) { return IDS.contains(id); }

    /** Vũng magma: gây sát thương + đốt theo nhịp. */
    static final class Pool {
        final Vec3 c; final double r; int left; final float dmg;
        Pool(Vec3 c, double r, int left, float dmg) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; }
    }

    // ------------------------------------------------------------------ AI chọn chiêu
    static String pick(RandomSource rnd, int phase, boolean ultReady, float dist, boolean canSummon) {
        if (phase >= 3 && ultReady && rnd.nextInt(5) == 0) return "hvg_thon_thien";
        if (dist > 10 && rnd.nextBoolean()) return phase >= 2 && rnd.nextBoolean() ? "hvg_dung_nham" : "hvg_xa_ngoan";
        List<String> pool = phase >= 3
                ? List.of("hvg_long_tuc", "hvg_lau_cot", "hvg_dia_chan", "hvg_cot_tra", "hvg_dung_nham", "hvg_phong_phu")
                : phase == 2
                ? List.of("hvg_phong_phu", "hvg_cot_tra", "hvg_dung_nham", "hvg_xa_ngoan", "hvg_trieu_hoi")
                : List.of("hvg_phong_phu", "hvg_cot_tra", "hvg_xa_ngoan", "hvg_trieu_hoi");
        String id = pool.get(rnd.nextInt(pool.size()));
        return id.equals("hvg_trieu_hoi") && !canSummon ? "hvg_phong_phu" : id;
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE) — khớp BossPhases "hu_vuong". */
    static double mult(String id) {
        return switch (id) {
            case "hvg_xa_ngoan" -> 1.1;
            case "hvg_phong_phu" -> 1.0;
            case "hvg_cot_tra" -> 0.8;
            case "hvg_dung_nham" -> 0.9;
            case "hvg_long_tuc" -> 0.35;
            case "hvg_lau_cot" -> 1.2;
            case "hvg_dia_chan" -> 0.9;
            case "hvg_thon_thien" -> 2.6;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, HuVuong d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("hvg_thon_thien");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD) : name, true);
        d.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 1.5f, ult ? 0.4f : 0.6f);
        if (ult) { d.ultCd = 900; ultScene(sl, d); }
    }

    private static Vec3 tpos(HuVuong d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aim.scale(6));
    }

    private static void reaim(HuVuong d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aim = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aim.x, d.aim.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    // ------------------------------------------------------------------ mỗi tick; false = xong
    static boolean step(ServerLevel sl, HuVuong d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "hvg_xa_ngoan" -> serpent(sl, d, t);
            case "hvg_phong_phu" -> windAxe(sl, d, t);
            case "hvg_cot_tra" -> grafted(sl, d, t);
            case "hvg_dung_nham" -> magmaWave(sl, d, t);
            case "hvg_trieu_hoi" -> summon(sl, d, t);
            case "hvg_long_tuc" -> dragonBreath(sl, d, t);
            case "hvg_lau_cot" -> skulls(sl, d, t);
            case "hvg_dia_chan" -> tripleStomp(sl, d, t);
            case "hvg_thon_thien" -> devourHeaven(sl, d, t);
            default -> false;
        };
    }

    // ---- Xà Thần Nuốt Hồn: vạch 22 tick -> rắn lao + kéo
    private static boolean serpent(ServerLevel sl, HuVuong d, int t) {
        double len = 16;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.aim.scale(len), 3.5f, 22, VOID);
            d.triggerAnim("skill", "summon");
            sl.sendParticles(ParticleTypes.SCULK_SOUL, d.getX(), d.getY() + 2, d.getZ(), 16, 0.8, 0.8, 0.8, 0.02);
        }
        if (t == 22) {
            Vec3 a = d.from.add(0, 1.2, 0), b = a.add(d.aim.scale(len));
            Fx.ribbonWave(sl, null, a, b, 1.0, 1.4, 8, 0, 1.1f, VOID);
            Fx.ribbonHelix(sl, null, a, b, 0.9, 0, 8, 0, 0.4f, MAGMA);
            Fx.elem(sl, null, Fx.E_TONGUES, b, d.aim, 2f, DEEP, 0);
            d.triggerAnim("skill", "attack");
            for (Player p : players(sl, d.from, len)) {
                Vec3 rel = p.position().subtract(d.from).multiply(1, 0, 1);
                double along = rel.dot(d.aim);
                if (along < 0 || along > len || rel.subtract(d.aim.scale(along)).length() > 1.9) continue;
                if (d.strike(p, d.moveDamage(mult(d.mv)))) {
                    PhaseMoves.applyHit(p, Hit.SLOW);
                    Vec3 pull = d.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(1.3);
                    p.setDeltaMovement(pull.x, 0.25, pull.z);
                    p.hurtMarked = true;
                }
            }
            d.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 3f, 0.5f);
        }
        return t < 34;
    }

    // ---- Phong Phủ Đạp: quạt rìu gió (t18) rồi dậm (t36)
    private static boolean windAxe(ServerLevel sl, HuVuong d, int t) {
        if (t == 0) { reaim(d); Fx.telegraphCone(sl, null, d.position(), d.aim, 7.5f, 70, 18); d.triggerAnim("skill", "slam"); }
        if (t > 0 && t < 18 && t % 3 == 0)
            sl.sendParticles(ParticleTypes.CLOUD, d.getX() + d.aim.x * 2, d.getY() + 2.5, d.getZ() + d.aim.z * 2, 4, 0.5, 0.5, 0.5, 0.05);
        if (t == 18) {
            cone(sl, d, 7.5, 70, 1.0, Hit.NONE, 1.6);
            Fx.qi(sl, d.position().add(0, 1.4, 0), d.aim.scale(6), 5f, WIND, 25, 12);
            sl.sendParticles(ParticleTypes.GUST, d.getX() + d.aim.x * 4, d.getY() + 1, d.getZ() + d.aim.z * 4, 6, 2, 0.5, 2, 0);
            d.playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 3f, 0.6f);
            Fx.telegraphCircle(sl, null, d.position(), 5f, 18, DANGER);
        }
        if (t == 36) {
            hitAround(sl, d, d.position(), 5, mult(d.mv) * 0.9, Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, d.position(), 5f, WIND, 8, 30);
            Fx.groundRing(sl, null, d.position(), 6f, WIND, 20);
            Fx.shake(sl, d.position(), 1.8f, 12, 24f);
            sl.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, d.getX(), d.getY() + 0.3, d.getZ(), 1, 0, 0, 0, 0);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.8f);
        }
        return t < 44;
    }

    // ---- Ghép Thủ Loạn Vũ: 3 nhát quét luân phiên (t12/t24/t38)
    private static boolean grafted(ServerLevel sl, HuVuong d, int t) {
        if (t == 0 || t == 12 || t == 24) {
            reaim(d);
            Fx.telegraphCone(sl, null, d.position(), d.aim, t == 24 ? 7.5f : 6f, t == 24 ? 110 : 90, t == 24 ? 14 : 12);
        }
        if (t == 12 || t == 24 || t == 38) {
            int n = t / 12;
            d.setDeltaMovement(d.aim.x * 0.6, 0.05, d.aim.z * 0.6);
            d.hurtMarked = true;
            d.triggerAnim("skill", n == 3 ? "slam" : "attack");
            cone(sl, d, n == 3 ? 7.5 : 6, n == 3 ? 110 : 90, n == 3 ? 1.4 : 1.0, Hit.NONE, 0);
            Fx.qi(sl, d.position().add(0, 1.6, 0), d.aim.scale(5), 4f, n == 3 ? MAGMA : VOID, n % 2 == 0 ? -40 : 40, 10);
            for (int i = -1; i <= 1; i += 2)  // tay ghép mọc hai bên
                Fx.ribbon(sl, null, d.position().add(0, 2.4, 0), d.position().add(rot(d.aim, i * 60).scale(4)).add(0, 1, 0), 1.2, 6, 0, 0.3f, DEEP);
            d.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
        }
        return t < 46;
    }

    // ---- Dung Nham Cuồn Cuộn: vạch 20 tick -> 10 cột phun lần lượt, cuối để vũng magma
    private static boolean magmaWave(ServerLevel sl, HuVuong d, int t) {
        double len = 20;
        if (t == 0) {
            reaim(d);
            d.from = d.position();
            d.hitOnce.clear();
            Fx.telegraphLine(sl, null, d.from.add(0, 0.05, 0), d.aim.scale(len), 4f, 20, MAGMA);
            d.triggerAnim("skill", "slam");
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 3f, MAGMA, 20, 5);
        }
        if (t >= 20 && t < 40 && (t - 20) % 2 == 0) {
            int k = (t - 20) / 2 + 1;
            Vec3 c = d.from.add(d.aim.scale(k * 2.0));
            Fx.elem(sl, null, Fx.E_SPIKES, c, new Vec3(0, 1, 0), 1.4f, MAGMA, 0);
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 8, 0.8, 0.3, 0.8, 0);
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 1, c.z, 14, 0.8, 1, 0.8, 0.05);
            for (Player p : players(sl, c, 2.2))
                if (flat2(p.position(), c) <= 2.2 * 2.2 && d.hitOnce.add(p.getUUID()) && d.strike(p, d.moveDamage(mult(d.mv)))) {
                    p.igniteForSeconds(4);
                    PhaseMoves.applyHit(p, Hit.KNOCK_UP);
                }
            if (k == 1) d.playSound(SoundEvents.LAVA_POP, 3f, 0.6f);
            if (k == 10) addPool(sl, d, c, 3.5, 120, 0.2);
        }
        return t < 46;
    }

    // ---- Triệu Hồn Bị Nuốt (chiêu AI)
    private static boolean summon(ServerLevel sl, HuVuong d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "summon");
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 4f, VOID, 22, 6);
            Fx.telegraphCircle(sl, null, d.position(), 4f, 20, VOID);
        }
        if (t == 20) {
            for (int i = 0; i < 3 && d.minionCount(sl) < 6; i++) {
                EntityType<? extends Mob> type = d.getRandom().nextBoolean() ? ModEntities.HU_THI.get() : ModEntities.HU_COT.get();
                Mob m = type.create(sl);
                if (m == null) continue;
                double a = d.getRandom().nextDouble() * Math.PI * 2;
                m.moveTo(d.getX() + Math.cos(a) * 3, d.getY(), d.getZ() + Math.sin(a) * 3, d.getRandom().nextFloat() * 360f, 0f);
                m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                m.setTarget(d.getTarget());
                sl.addFreshEntity(m);
                Fx.ribbon(sl, null, d.position().add(0, 2.5, 0), m.position(), 2, 8, 0, 0.3f, VOID);
            }
            sl.sendParticles(ModParticles.HU_KHONG.get(), d.getX(), d.getY() + 1, d.getZ(), 40, 2, 1, 2, 0.05);
            d.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 0.7f);
        }
        return t < 28;
    }

    // ---- Long Tức: nón cảnh báo 28 tick -> phun lửa 30 tick (đánh mỗi 5 tick)
    private static boolean dragonBreath(ServerLevel sl, HuVuong d, int t) {
        double rad = 12, half = 35;
        if (t == 0) {
            reaim(d);
            Fx.telegraphCone(sl, null, d.position(), d.aim, (float) rad, (float) half, 28);
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(d.aim.scale(1.5)).add(0, 2.4, 0), d.aim, 1.2f, MAGMA, 0);
            d.triggerAnim("skill", "roar");
            d.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.7f);
        }
        if (t >= 28 && t < 58) {
            Vec3 mouth = d.position().add(d.aim.scale(1.5)).add(0, 2.4, 0);
            if (t % 2 == 0) {
                Fx.qi(sl, mouth, d.aim.scale(rad * 0.8), 3f, t % 4 == 0 ? MAGMA : EMBER, (t * 17) % 60 - 30, 8);
                for (int i = 0; i < 6; i++) {
                    Vec3 v = rot(d.aim, (d.getRandom().nextDouble() * 2 - 1) * half).scale(0.6 + d.getRandom().nextDouble() * 0.5);
                    sl.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, v.x, -0.05, v.z, 1.0);
                }
            }
            if (t % 5 == 3) cone(sl, d, rad, half, 1.0, Hit.NONE, 0, true);
            if (t % 6 == 0) d.playSound(SoundEvents.BLAZE_SHOOT, 2.5f, 0.5f);
        }
        return t < 64;
    }

    // ---- Lâu Cốt Phún Phát: 2 đợt sọ hư không dưới chân từng người (+3 điểm ngẫu nhiên)
    private static boolean skulls(ServerLevel sl, HuVuong d, int t) {
        if (t == 0 || t == 30) {
            d.spots.clear();
            for (Player p : players(sl, d.position(), 24)) d.spots.add(p.position());
            for (int i = 0; i < 3; i++) {
                double a = d.getRandom().nextDouble() * Math.PI * 2, r = 4 + d.getRandom().nextDouble() * 8;
                d.spots.add(d.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
            for (Vec3 s : d.spots) {
                Fx.telegraphCircle(sl, null, s, 3.5f, 24, DANGER);
                Fx.rune(sl, null, s.add(0, 0.1, 0), 2.5f, DEEP, 24, 3);
            }
            d.triggerAnim("skill", "summon");
            d.playSound(SoundEvents.SKELETON_AMBIENT, 3f, 0.4f);
        }
        if (t == 24 || t == 54) {
            d.hitOnce.clear();
            for (Vec3 s : d.spots) {
                Fx.elem(sl, null, Fx.E_SPIKES, s, new Vec3(0, 1, 0), 2.2f, VOID, 0);
                Fx.elem(sl, null, Fx.E_TONGUES, s.add(0, 1, 0), new Vec3(0, 1, 0), 1.6f, MAGMA, 0);
                sl.sendParticles(ParticleTypes.SOUL, s.x, s.y + 1.5, s.z, 12, 0.8, 1.2, 0.8, 0.05);
                sl.sendParticles(ParticleTypes.LAVA, s.x, s.y + 0.5, s.z, 6, 0.8, 0.3, 0.8, 0);
                for (Player p : players(sl, s, 3.5))
                    if (flat2(p.position(), s) <= 3.5 * 3.5 && d.hitOnce.add(p.getUUID()) && d.strike(p, d.moveDamage(mult(d.mv))))
                        PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            }
            Fx.shake(sl, d.position(), 1.5f, 10, 30f);
            d.playSound(SoundEvents.WITHER_SHOOT, 2.5f, 0.5f);
        }
        return t < 62;
    }

    // ---- Địa Chấn Tam Đạp: 3 dậm r 5/8/11
    private static boolean tripleStomp(ServerLevel sl, HuVuong d, int t) {
        int[] at = {16, 32, 48};
        double[] rr = {5, 8, 11};
        for (int i = 0; i < 3; i++) {
            if (t == at[i] - 16) { Fx.telegraphCircle(sl, null, d.position(), (float) rr[i], 16, i == 2 ? DANGER : MAGMA); d.triggerAnim("skill", "slam"); }
            if (t == at[i]) {
                hitAround(sl, d, d.position(), rr[i], mult(d.mv) * (i == 2 ? 1.4 : 1.0), Hit.KNOCK_UP);
                Fx.groundCrack(sl, null, d.position(), (float) rr[i], MAGMA, 6 + i * 3, 40);
                Fx.groundRing(sl, null, d.position(), (float) rr[i], i == 2 ? VOID : EMBER, 20);
                Fx.shake(sl, d.position(), 1.5f + i, 12, 32f);
                sl.sendParticles(ParticleTypes.LAVA, d.getX(), d.getY() + 0.5, d.getZ(), 10 + i * 8, rr[i] * 0.4, 0.2, rr[i] * 0.4, 0);
                d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f + i * 0.5f, 0.8f - i * 0.1f);
            }
        }
        return t < 56;
    }

    // ---- Hư Không Thôn Thiên (tối thượng): bay lên -> sọ khổng lồ hút 70 tick -> nổ t90 -> hồ magma
    public static final int UL_RISE = 20, UL_BOOM = 90, UL_END = 104;
    public static final double UL_RAD = 18;

    private static boolean devourHeaven(ServerLevel sl, HuVuong d, int t) {
        if (t == 0) {
            d.setNoGravity(true);
            d.triggerAnim("skill", "roar");
            d.spot = d.position();
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < UL_RISE) {
            d.setDeltaMovement(0, 0.3, 0);
            d.hurtMarked = true;
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, d.getX(), d.getY() + 1, d.getZ(), 10, 1.5, 1, 1.5, 0.02);
            return true;
        }
        if (t == UL_RISE) {
            d.setDeltaMovement(Vec3.ZERO);
            Fx.telegraphCircle(sl, null, d.spot, (float) UL_RAD, UL_BOOM - UL_RISE, DANGER);
            Fx.rune(sl, null, d.spot.add(0, 0.1, 0), (float) UL_RAD * 0.6f, DEEP, UL_BOOM - UL_RISE, 8);
            skullFx(sl, d.position().add(0, 4, 0), 4f, UL_BOOM - UL_RISE + 10);
        }
        if (t > UL_RISE && t < UL_BOOM) {
            d.setDeltaMovement(Vec3.ZERO);
            if (t % 2 == 0) for (Player p : players(sl, d.spot, UL_RAD + 4)) {  // hút vào miệng sọ
                Vec3 in = d.spot.subtract(p.position()).multiply(1, 0, 1);
                if (in.lengthSqr() < 4) continue;
                Vec3 v = in.normalize().scale(0.18);
                p.setDeltaMovement(p.getDeltaMovement().add(v.x, 0, v.z));
                p.hurtMarked = true;
            }
            if (t % 4 == 0) {
                double a0 = t * 0.35;
                double rr = UL_RAD * (1 - (t - UL_RISE) / (double) (UL_BOOM - UL_RISE));
                for (int i = 0; i < 8; i++) {
                    double a = a0 + i * Math.PI / 4;
                    Vec3 o = d.spot.add(Math.cos(a) * rr, 0.4, Math.sin(a) * rr);
                    sl.sendParticles(ModParticles.HU_KHONG.get(), o.x, o.y, o.z, 1, 0.1, 0.1, 0.1, 0);
                    sl.sendParticles(ParticleTypes.SOUL, o.x, o.y + 0.5, o.z, 1, 0.1, 0.1, 0.1, 0);
                }
            }
            if (t % 10 == 0) {  // sọ nhỏ phun trào ngẫu nhiên trong vòng
                double a = d.getRandom().nextDouble() * Math.PI * 2, r = d.getRandom().nextDouble() * UL_RAD;
                Fx.elem(sl, null, Fx.E_SPIKES, d.spot.add(Math.cos(a) * r, 0, Math.sin(a) * r), new Vec3(0, 1, 0), 1.2f, MAGMA, 0);
            }
            if (t == UL_BOOM - 10) d.playSound(SoundEvents.WARDEN_SONIC_BOOM, 4f, 0.5f);
            return true;
        }
        if (t == UL_BOOM) {
            Vec3 s = d.spot;
            d.teleportTo(s.x, s.y, s.z);
            for (Player p : players(sl, s, UL_RAD))
                if (flat2(p.position(), s) <= UL_RAD * UL_RAD && d.strike(p, d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            Fx.elem(sl, null, Fx.E_NUKE, s, Vec3.ZERO, (float) UL_RAD * 0.45f, VOID, 0);
            Fx.groundCrack(sl, null, s, (float) UL_RAD, MAGMA, 16, 60);
            Fx.groundRing(sl, null, s, (float) UL_RAD, VOID, 30);
            Fx.shake(sl, s, 4f, 30, 60f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.x, s.y + 1, s.z, 2, 1, 0.2, 1, 0);
            sl.sendParticles(ParticleTypes.LAVA, s.x, s.y + 1, s.z, 80, UL_RAD * 0.4, 1, UL_RAD * 0.4, 0);
            addPool(sl, d, s, 8, 200, 0.25);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
            d.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.5f);
        }
        if (t == UL_BOOM + 4) d.setNoGravity(false);
        return t < UL_END;
    }

    // ------------------------------------------------------------------ trợ giúp
    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    private static void hitAround(ServerLevel sl, HuVuong d, Vec3 c, double r, double mul, Hit h) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (d.strike(p, d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static void cone(ServerLevel sl, HuVuong d, double rad, double half, double mul, Hit h, double push) { cone(sl, d, rad, half, mul, h, push, false); }

    private static void cone(ServerLevel sl, HuVuong d, double rad, double half, double mul, Hit h, double push, boolean fire) {
        double cos = Math.cos(Math.toRadians(half));
        Vec3 c = d.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(d.aim) < cos)) continue;
            if (!d.strike(p, d.moveDamage(mult(d.mv) * mul))) continue;
            PhaseMoves.applyHit(p, h);
            if (fire) p.igniteForSeconds(3);
            if (push > 0) {
                Vec3 away = dist > 1e-3 ? rel.normalize() : d.aim;
                p.setDeltaMovement(away.x * push, 0.4, away.z * push);
                p.hurtMarked = true;
            }
        }
    }

    static void addPool(ServerLevel sl, HuVuong d, Vec3 c, double r, int life, double mul) {
        d.pools.add(new Pool(c, r, life, d.moveDamage(mul)));
        Fx.pool(sl, null, c, (float) r, MAGMA, life);
    }

    static void tickPools(ServerLevel sl, HuVuong d) {
        for (Iterator<Pool> it = d.pools.iterator(); it.hasNext(); ) {
            Pool p = it.next();
            if (--p.left <= 0) { it.remove(); continue; }
            if (p.left % 6 == 0) sl.sendParticles(ParticleTypes.LAVA, p.c.x, p.c.y + 0.2, p.c.z, 2, p.r * 0.5, 0.1, p.r * 0.5, 0);
            if (p.left % 10 != 0) continue;
            for (Player pl : players(sl, p.c, p.r)) {
                if (flat2(pl.position(), p.c) > p.r * p.r || Math.abs(pl.getY() - p.c.y) > 2) continue;
                pl.hurt(d.damageSources().magic(), p.dmg);
                pl.igniteForSeconds(2);
            }
        }
    }

    /** Sọ hư không khổng lồ: vòng quầng + răng (lưỡi) + hốc mắt. */
    static void skullFx(ServerLevel sl, Vec3 c, float size, int life) {
        Fx.elem(sl, null, Fx.E_HALOS, c, Vec3.ZERO, size, DEEP, 0);
        Fx.rune(sl, null, c, size, VOID, life, 7);
        for (int i = -1; i <= 1; i += 2) Fx.rune(sl, null, c.add(i * size * 0.35, size * 0.2, 0), size * 0.22f, MAGMA, life, 3);
        Fx.elem(sl, null, Fx.E_TONGUES, c.add(0, -size * 0.4, 0), new Vec3(0, -1, 0), size * 0.5f, MAGMA, 0);
    }

    /** Lộ diện pha: P2 tay ghép mọc ra (Godrick), P3 tay rồng + rắn dung nham quấn thân (Rykard). */
    static void transformFx(ServerLevel sl, HuVuong d, int ph) {
        d.triggerAnim("skill", "roar");
        Vec3 c = d.position().add(0, d.getBbHeight() * 0.6, 0);
        if (ph == 2) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Fx.ribbonWave(sl, null, c, c.add(Math.cos(a) * 4, 1.5 + (i % 2), Math.sin(a) * 4), 1.5, 0.6, 14, i * 2, 0.45f, DEEP);
            }
            Fx.groundCrack(sl, null, d.position(), 8f, VOID, 10, 40);
            sl.sendParticles(ModParticles.HU_KHONG.get(), c.x, c.y, c.z, 80, 2, 2, 2, 0.1);
            d.playSound(SoundEvents.WARDEN_ROAR, 3f, 0.7f);
        } else if (ph >= 3) {
            Fx.ribbonHelix(sl, null, d.position(), d.position().add(0, d.getBbHeight() * 1.6, 0), 2.0, 0, 30, 0, 0.6f, MAGMA);
            skullFx(sl, c.add(0, 2, 0), 3f, 60);
            Fx.pool(sl, null, d.position(), 7f, MAGMA, 80);
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 60, 2, 1, 2, 0);
            d.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.6f);
        }
    }

    // ------------------------------------------------------------------ cutscene
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: thấp dưới chân nhìn lên -> vòng sang -> cận mặt -> lùi xa. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 2.0, 0.4, 9.0, 75, 3.0, -14), k(T * 2 / 5, -4.0, 1.0, 5.0, 60, 3.2, -6),
                k(T * 3 / 4, 0.6, 3.0, 3.4, 45, 3.2, 0), k(T, 0, 3.2, 8.0, 66, 2.6, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.hu_vuong"), new SubLine(36, 72, "hukhong.dialog.huvuong.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.huvuong.intro.2"));
    }

    static final int ULT_T = 60;

    private static void ultScene(ServerLevel sl, HuVuong d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 3.0 * s, 0.4 * s, 5.0 * s, 62, 2.0 * s, -8), k(ULT_T / 2, -2.5 * s, 0.3 * s, 6.0 * s, 72, 5.0 * s, -22),
                k(ULT_T, 0, 1.5 * s, 11.0 * s, 82, 6.0 * s, -18));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 30, "hukhong.dialog.huvuong.ult.1"),
                new SubLine(30, ULT_T - 2, "hukhong.dialog.huvuong.ult.2")));
    }

    /** Tử trận: thân sụp, sọ tan, hồn bị nuốt thoát ra. */
    static void deathScene(ServerLevel sl, HuVuong d) {
        int T = 100;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 2.0, 6.0, 55, 2.0, 0), k(50, 3.5, 2.5, 4.0, 60, 1.6, 0), k(T, 0, 6.0, 11.0, 74, 1.2, -10));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 40, "hukhong.dialog.huvuong.death.1"),
                new SubLine(40, 72, "hukhong.dialog.huvuong.death.2"), new SubLine(72, T - 4, "hukhong.dialog.huvuong.death.3")));
        skullFx(sl, at.add(0, 3, 0), 3f, 80);
        Fx.groundRing(sl, null, at, 9f, VOID, 60);
        sl.sendParticles(ParticleTypes.SOUL, at.x, at.y + 2, at.z, 150, 3, 2, 3, 0.08);
        sl.sendParticles(ModParticles.HU_KHONG.get(), at.x, at.y + 1.5, at.z, 80, 2, 2, 2, 0.05);
    }


    // ================================================================== BossBrain (PLAN-v1 §4: khổng lồ, không đuổi)
    /**
     * Gần như đứng yên, xoay chậm (3-4.5°/tick); người chơi đứng xa -> HÚT kéo lại (xa hơn 14 ô thì nhảy rung đất).
     * Rìu: quét - quét - dậm; pha 2: hai tay đánh luân phiên. Người chơi ra sau lưng -> quét xà thần 360°. Không né.
     */
    public static final com.hukhong.boss.ai.BossProfile BRAIN = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(2, 4.5, 0.0, 3f, 0, 999, 30, 50))
            .phase(2, new com.hukhong.boss.ai.BossProfile.PhaseVals(2, 4.5, 0.0, 3.5f, 0, 999, 24, 40))
            .phase(3, new com.hukhong.boss.ai.BossProfile.PhaseVals(2, 4.5, 0.0, 4.5f, 0, 999, 18, 32))
            .weight(3).accel(10, 6).moveSpeed(0.05)
            .gapClose(new PullOrLeap(), 7)
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("riu", List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(16, 5, 12, 4.5, 170, 1.0, 0.3, 0, false, false, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(12, 5, 12, 4.5, 170, 1.0, 0.3, 0, false, false, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(18, 5, 18, 4.0, 360, 1.3, 0, 0, false, true, false)),
                    0, 6, 3, 1, List.of(), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("tay", List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 4, 8, 4.2, 120, 0.8, 0.4, 0, false, false, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 4, 8, 4.2, 120, 0.8, 0.4, 0, false, false, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 4, 8, 4.2, 120, 0.8, 0.4, 0, false, false, false),
                    new com.hukhong.boss.ai.BossProfile.Hit(10, 4, 16, 4.5, 140, 1.1, 0.6, 0, false, true, false)),
                    0, 5.5, 3, 2, List.of(), false))
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("duoi", List.of(
                    new com.hukhong.boss.ai.BossProfile.Hit(14, 8, 16, 5.5, 360, 1.1, 0, 0, false, true, false)),
                    0, 7, 0, 1, List.of(), true))
            .bigMoveEvery(99)
            .build();

    static final java.util.Set<String> BRAIN_ANIMS = java.util.Set.of("dash", "leap", "combo_riu_1", "combo_riu_2", "combo_riu_3",
            "combo_tay_1", "combo_tay_2", "combo_tay_3", "combo_tay_4", "combo_duoi_1");

    /** Áp sát kiểu khổng lồ: hút người chơi về phía mình (~1 giây); xa hơn 14 ô thì nhảy rung đất. */
    static final class PullOrLeap implements com.hukhong.boss.ai.GapClose {
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, net.minecraft.world.entity.Mob b, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i) {
            HuVuong h = (HuVuong) b;
            if (i == 0) {
                h.gapLeap = b.distanceTo(t) > 14;
                if (h.gapLeap) h.playBrainAnim("leap");
                else { h.pulls++; b.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2f, 0.5f); }
            }
            if (h.gapLeap) {
                if (com.hukhong.boss.ai.GapClose.LEAP.step(sl, b, mv, t, i)) return true;
                for (Player p : sl.getEntitiesOfClass(Player.class, b.getBoundingBox().inflate(4, 1, 4), p -> !p.isSpectator()))
                    if (h.strike(p, h.moveDamage(0.6))) { p.setDeltaMovement(p.getDeltaMovement().add(0, 0.5, 0)); p.hurtMarked = true; }
                Fx.shake(sl, b.position(), 2f, 12, 24f);
                return false;
            }
            mv.face(t.position(), 4f);
            mv.stop();
            Vec3 in = b.position().subtract(t.position()).multiply(1, 0, 1);
            double d = in.length();
            if (d < 4.2 || i > 26) return false;
            if (i >= 6) {   // 6 tick gồng (vòng xoáy) rồi kéo
                Vec3 v = in.scale(0.32 / d);
                t.setDeltaMovement(v.x, Math.max(t.getDeltaMovement().y, 0.02), v.z);
                t.hurtMarked = true;
                Vec3 a = b.position().add(0, b.getBbHeight() * 0.55, 0), c = t.position().add(0, 1, 0);
                for (int k = 0; k <= 6; k++) { Vec3 q = a.lerp(c, k / 6.0); sl.sendParticles(ParticleTypes.REVERSE_PORTAL, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.01); }
            } else sl.sendParticles(ParticleTypes.PORTAL, b.getX(), b.getY() + b.getBbHeight() * 0.6, b.getZ(), 8, 1, 1, 1, 0.4);
            return true;
        }
    }

    /** Mỗi tick (khi không đang chiêu riêng): người chơi lọt ra sau lưng -> quét xà thần 360°. */
    static void brainTick(ServerLevel sl, HuVuong h) {
        if (h.tailCd > 0) h.tailCd--;
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(h);
        LivingEntity t = h.getTarget();
        if (b == null || t == null || !t.isAlive() || h.tailCd > 0 || b.state() != com.hukhong.boss.ai.BossBrain.State.STRAFE || !b.allowAttacks) return;
        Vec3 to = t.position().subtract(h.position()).multiply(1, 0, 1);
        if (to.lengthSqr() > 7.5 * 7.5 || to.lengthSqr() < 1e-3) return;
        double cos = to.normalize().dot(b.movement().forward());
        if (cos > -0.35) return; // trong khoảng ±110° phía trước: để chuỗi rìu thường lo
        h.tailCd = 80;
        h.tailSweeps++;
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, h.getX(), h.getY() + 0.3, h.getZ(), 30, 2.5, 0.2, 2.5, 0.05);
        b.startCombo(BRAIN.combo("duoi"), t);
    }
}
