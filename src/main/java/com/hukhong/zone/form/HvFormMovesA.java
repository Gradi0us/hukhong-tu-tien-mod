package com.hukhong.zone.form;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.entity.HuSkill;
import com.hukhong.events.BossBuff;
import com.hukhong.vfx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bộ chiêu RIÊNG (kiểu Elden Ring) cho 4 dạng Hư Vương: Huyết Ma (Mohg), Cốt Long (Lichdragon/Placidusax),
 * Mộc Yêu (Ulcerated Tree Spirit), Băng Phách (Borealis). Mỗi dạng 8 chiêu (chỉ số 4 = tối thượng có cutscene),
 * mọi chiêu có telegraph trước sát thương. Director giao chiêu pha 2/3 qua {@link HuVuongForm#startOwnMove};
 * AI riêng ({@link #aiStep}) dùng thêm chiêu cơ bản từ pha 1. Thoại: hukhong.dialog.hvf_&lt;dạng&gt;.*.
 */
public final class HvFormMovesA {
    private HvFormMovesA() {}

    // ------------------------------------------------------------------ màu
    static final int BLOOD = 0xB00818, FLAME = 0xFF3020, DARK = 0x500010, DANGER = 0xFF2030;
    static final int BONE = 0xD8D0B8, RED_BOLT = 0xFF3A2A, GOLD = 0xFFC060, ASHC = 0x3A3328;
    static final int WOOD = 0x5A7A2A, ROT = 0x8A3A1A, BULB = 0xFFB040, SAP = 0xC8E060;
    static final int FROST = 0x9FE8FF, ICE = 0xDFF8FF, DEEP = 0x3A7AD0;

    /** 8 chiêu mỗi dạng, chỉ số 4 = tối thượng. Thứ tự khớp BossPhases (pha 2 = 0..3, pha 3 = 4..7). */
    public static final Map<HvForm, List<String>> MOVES = Map.of(
            HvForm.HUYET_MA, List.of("hvhm_xoa_quet", "hvhm_xoa_dam", "hvhm_huyet_diem", "hvhm_huyet_chu",
                    "hvhm_thanh_huyet", "hvhm_huyet_luan", "hvhm_huyet_tuyen", "hvhm_huyet_vu"),
            HvForm.COT_LONG, List.of("hvcl_long_tuc", "hvcl_vi_tao", "hvcl_loi_kich", "hvcl_tram_dap",
                    "hvcl_thoi_khong", "hvcl_thuan_di", "hvcl_loi_tuc", "hvcl_cot_vu"),
            HvForm.MOC_YEU, List.of("hvmy_phe_cau", "hvmy_re_dam", "hvmy_no_buou", "hvmy_lan_cuon",
                    "hvmy_dai_thu", "hvmy_quat_than", "hvmy_hoa_phun", "hvmy_nhay_dap"),
            HvForm.BANG_PHACH, List.of("hvbp_han_tuc", "hvbp_vu_dong", "hvbp_canh_phong", "hvbp_phi_tao",
                    "hvbp_bang_bao", "hvbp_bang_chuy", "hvbp_suong_mu", "hvbp_lao_xuong"));

    public static boolean handles(HvForm f) { return MOVES.containsKey(f); }

    public static boolean handles(HvForm f, String id) { List<String> l = MOVES.get(f); return l != null && l.contains(id); }

    static boolean isUlt(String id) {
        for (List<String> l : MOVES.values()) if (l.get(4).equals(id)) return true;
        return false;
    }

    /** Hệ số sát thương (x ATTACK_DAMAGE). */
    static double mult(String id) {
        return switch (id) {
            case "hvhm_xoa_dam", "hvcl_tram_dap", "hvcl_thuan_di" -> 1.2;
            case "hvmy_lan_cuon", "hvbp_lao_xuong" -> 1.3;
            case "hvmy_nhay_dap" -> 1.4;
            case "hvhm_huyet_diem", "hvhm_huyet_luan", "hvhm_huyet_tuyen", "hvcl_loi_kich", "hvmy_phe_cau", "hvbp_bang_chuy", "hvcl_thoi_khong", "hvmy_dai_thu" -> 1.0;
            case "hvcl_vi_tao", "hvmy_no_buou", "hvbp_phi_tao", "hvbp_bang_bao" -> 1.1;
            case "hvcl_long_tuc", "hvmy_hoa_phun", "hvbp_han_tuc" -> 0.3;   // mỗi nhịp hơi thở
            case "hvbp_vu_dong" -> 0.25;
            default -> 0.9;
        };
    }

    // ------------------------------------------------------------------ trạng thái trên thực thể
    /** Vùng lưu (vũng lửa máu / sương băng / bầu nổ): kind 0 lửa, 1 băng. */
    static final class Zone {
        final Vec3 c; final double r; int left; final float dmg; final int kind;
        Zone(Vec3 c, double r, int left, float dmg, int kind) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; this.kind = kind; }
    }

    /** Đòn trễ: vòng cảnh báo rồi nổ sau {@code left} tick. vfx: 0 gai máu, 1 sét, 2 nổ, 3 gai băng, 4 rễ. */
    static final class Delayed {
        final Vec3 c; final double r; int left; final float dmg; final Hit hit; final int vfx, rgb;
        Delayed(Vec3 c, double r, int left, float dmg, Hit hit, int vfx, int rgb) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; this.hit = hit; this.vfx = vfx; this.rgb = rgb; }
    }

    public static final class State {
        String mv;
        int mt, ultCd = 500, ownCd = 120, done;
        /** Game-time luc chieu bat dau: watchdog dua tren dong ho the gioi, doc lap voi mt (mt chi tang khi tick() chay). */
        long startGt;
        LivingEntity tgt;
        Vec3 aim = new Vec3(0, 0, 1), spot, from, to;
        final Set<UUID> hitOnce = new HashSet<>();
        final List<UUID> marked = new ArrayList<>();
        final List<Vec3> spots = new ArrayList<>();
        final List<Zone> zones = new ArrayList<>();
        final List<Delayed> delayed = new ArrayList<>();

        public String current() { return mv; }
        /** Số chiêu riêng đã thi triển xong (GameTest). */
        public int done() { return done; }
        /** Chặn AI riêng tự chọn chiêu (GameTest). */
        public void hold(int t) { ownCd = t; }
        public boolean busy() { return mv != null; }
    }

    // ------------------------------------------------------------------ vòng đời
    /** Bắt đầu chiêu riêng; false nếu dạng không có chiêu này / đang bận. */
    public static boolean start(ServerLevel sl, HuVuongForm b, String id, LivingEntity target) {
        State s = b.movesA;
        if (s.mv != null && expired(sl, s)) end(b, false);  // chieu ket -> don sach de khong chan vinh vien
        if (!handles(b.form(), id) || s.mv != null || !b.isAlive() || b.casting()) return false;
        s.mv = id; s.mt = 0; s.startGt = sl.getGameTime(); s.tgt = target; s.spot = null; s.from = null; s.to = null;
        s.hitOnce.clear(); s.marked.clear(); s.spots.clear();
        b.getNavigation().stop();
        Vec3 dir = target != null ? target.position().subtract(b.position()).multiply(1, 0, 1) : b.getViewVector(1f).multiply(1, 0, 1);
        s.aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = isUlt(id);
        Component name = Component.translatable("hukhong.boss.move." + id);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD) : name, true);
        b.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, ult ? 0.4f : 0.75f);
        if (ult) { s.ultCd = 900; ultScene(sl, b); }
        return true;
    }

    /** Kết thúc chiêu (xong / bị huỷ); trả boss về trạng thái bình thường. */
    static void end(HuVuongForm b, boolean finished) {
        State s = b.movesA;
        if (s.mv == null) return;
        s.mv = null; s.tgt = null;
        if (finished) s.done++;
        b.setNoGravity(false);
        if (!b.burrowing()) b.setInvisible(false);
    }

    /** Huỷ khi chuyển pha / chết: dừng chiêu + đòn trễ. */
    public static void cancel(HuVuongForm b) {
        end(b, false);
        b.movesA.delayed.clear();
    }

    /** Chieu dai nhat ~118 tick; qua 400 tick (game-time) la ket. */
    static final int MAX_MOVE_TICKS = 400;

    static boolean expired(ServerLevel sl, State s) {
        return s.mt > MAX_MOVE_TICKS || sl.getGameTime() - s.startGt > MAX_MOVE_TICKS;
    }

    /** Gọi mỗi tick server (kể cả khi boss bất động). */
    public static void tick(ServerLevel sl, HuVuongForm b) {
        State s = b.movesA;
        if (s.ultCd > 0) s.ultCd--;
        if (s.mv != null) {
            if (expired(sl, s)) { end(b, false); s.ownCd = 60; }
            else if (b.director().transitioning()) cancel(b);
            else if (!step(sl, b, s.mt++)) { end(b, true); s.ownCd = BossBuff.cd(b.phase() >= 3 ? 50 : 70); }
        }
        tickZones(sl, b);
        tickDelayed(sl, b);
    }

    /** AI riêng: true = boss đang bận chiêu riêng (bỏ qua AI cũ). */
    public static boolean aiStep(ServerLevel sl, HuVuongForm b) {
        State s = b.movesA;
        if (s.mv != null && expired(sl, s)) { end(b, false); s.ownCd = 60; }
        if (s.mv != null) { b.getNavigation().stop(); return true; }
        if (!handles(b.form()) || b.casting()) return false;
        if (s.ownCd > 0) { s.ownCd--; return false; }
        LivingEntity t = b.getTarget();
        if (t == null || !t.isAlive() || b.distanceToSqr(t) > 24 * 24) return false;
        return start(sl, b, pick(b.getRandom(), b.form(), b.phase(), s.ultCd <= 0, b.distanceTo(t)), t);
    }

    static String pick(RandomSource rnd, HvForm f, int phase, boolean ultReady, float dist) {
        List<String> ids = MOVES.get(f);
        if (phase >= 3 && ultReady && rnd.nextInt(4) == 0) return ids.get(4);
        List<String> pool = new ArrayList<>(ids.subList(0, phase <= 1 ? 2 : 4));
        if (phase >= 3) pool.addAll(ids.subList(5, 8));
        if (dist > 10 && rnd.nextBoolean()) return pool.get(pool.size() - 1 - rnd.nextInt(Math.min(2, pool.size())));
        return pool.get(rnd.nextInt(pool.size()));
    }

    // ------------------------------------------------------------------ mỗi tick; false = chiêu xong
    static boolean step(ServerLevel sl, HuVuongForm b, int t) {
        b.getNavigation().stop();
        return switch (b.movesA.mv) {
            // Huyết Ma ~ Mohg
            case "hvhm_xoa_quet" -> sweep2(sl, b, t, 7, 80, FLAME, Hit.WEAK, ParticleTypes.FLAME);
            case "hvhm_xoa_dam" -> lunge(sl, b, t, 14, 2.4, FLAME, Hit.KNOCK_UP, 0);
            case "hvhm_huyet_diem" -> flameTrail(sl, b, t);
            case "hvhm_huyet_chu" -> bloodCurse(sl, b, t);
            case "hvhm_thanh_huyet" -> bloodboon(sl, b, t);
            case "hvhm_huyet_luan" -> nova2(sl, b, t, 12, BLOOD, FLAME, Hit.KNOCK_UP, true);
            case "hvhm_huyet_tuyen" -> geysers(sl, b, t, BLOOD, 0, Hit.KNOCK_UP);
            case "hvhm_huyet_vu" -> rain(sl, b, t, 12, 2.2, BLOOD, 0, Hit.WEAK);
            // Cốt Long ~ Lichdragon / Placidusax
            case "hvcl_long_tuc" -> breath(sl, b, t, 10, 30, BONE, ParticleTypes.SOUL_FIRE_FLAME, Hit.WEAK);
            case "hvcl_vi_tao" -> tailSweep(sl, b, t);
            case "hvcl_loi_kich" -> lightningWaves(sl, b, t);
            case "hvcl_tram_dap" -> lunge(sl, b, t, 10, 3.0, BONE, Hit.KNOCK_UP, 1);
            case "hvcl_thoi_khong" -> timeRift(sl, b, t);
            case "hvcl_thuan_di" -> warpBite(sl, b, t);
            case "hvcl_loi_tuc" -> boltBreath(sl, b, t);
            case "hvcl_cot_vu" -> boneWaves(sl, b, t);
            // Mộc Yêu ~ Ulcerated Tree Spirit
            case "hvmy_phe_cau" -> doubleBite(sl, b, t);
            case "hvmy_re_dam" -> roots(sl, b, t);
            case "hvmy_no_buou" -> bulbs(sl, b, t);
            case "hvmy_lan_cuon" -> rollCharge(sl, b, t);
            case "hvmy_dai_thu" -> treeUlt(sl, b, t);
            case "hvmy_quat_than" -> flail(sl, b, t);
            case "hvmy_hoa_phun" -> breath(sl, b, t, 9, 25, ROT, ParticleTypes.FLAME, Hit.POISON);
            case "hvmy_nhay_dap" -> dive(sl, b, t, 5, WOOD, BULB, false);
            // Băng Phách ~ Borealis
            case "hvbp_han_tuc" -> breath(sl, b, t, 11, 28, FROST, ParticleTypes.SNOWFLAKE, Hit.FREEZE);
            case "hvbp_vu_dong" -> mist(sl, b, t);
            case "hvbp_canh_phong" -> gust(sl, b, t);
            case "hvbp_phi_tao" -> flySweep(sl, b, t);
            case "hvbp_bang_bao" -> blizzard(sl, b, t);
            case "hvbp_bang_chuy" -> geysers(sl, b, t, ICE, 3, Hit.FREEZE);
            case "hvbp_suong_mu" -> fanLines(sl, b, t);
            case "hvbp_lao_xuong" -> dive(sl, b, t, 5, FROST, ICE, true);
            default -> false;
        };
    }

    // ================================================================== chiêu dùng chung (tham số theo dạng)
    /** 2 nhát quét nón (nhát 2 quét ngược). */
    private static boolean sweep2(ServerLevel sl, HuVuongForm b, int t, double r, double half, int rgb, Hit h, ParticleOptions pt) {
        State s = b.movesA;
        if (t == 0) { reaim(b); Fx.telegraphCone(sl, null, b.position(), s.aim, (float) r, (float) half, 14); b.triggerAnim("skill", "cast"); }
        if (t == 14 || t == 34) {
            if (t == 34) reaim(b);
            hitCone(sl, b, b.position(), s.aim, r, half, mult(s.mv), h);
            Fx.qi(sl, b.position().add(0, 1.6, 0), s.aim.scale(r * 0.8), (float) (r * 0.6), rgb, t == 14 ? 40 : -40, 10);
            sl.sendParticles(pt, b.getX() + s.aim.x * 3, b.getY() + 0.6, b.getZ() + s.aim.z * 3, 20, 2, 0.2, 2, 0.02);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.6f);
            if (t == 14) Fx.telegraphCone(sl, null, b.position(), s.aim, (float) r, (float) half, 20);
        }
        return t < 44;
    }

    /** Đâm / cắn lướt theo vạch; kind 1 = thêm dậm đất trễ ở điểm cuối. */
    private static boolean lunge(ServerLevel sl, HuVuongForm b, int t, double len, double w, int rgb, Hit h, int kind) {
        State s = b.movesA;
        if (t == 0) {
            reaim(b);
            s.from = b.position(); s.to = s.from.add(s.aim.scale(len));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.to.subtract(s.from), (float) w, 18, rgb);
            b.triggerAnim("skill", "cast");
        }
        if (t >= 18 && t < 23) {
            if (t == 18) {
                Fx.rift(sl, null, s.from.add(0, 1.2, 0), s.to.subtract(s.from), 1.0f, rgb, 30);
                b.playSound(kind == 1 ? SoundEvents.RAVAGER_ROAR : SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.5f, 0.6f);
                b.triggerAnim("skill", "attack");
            }
            Vec3 q = s.from.lerp(s.to, (t - 17) / 5.0);
            b.teleportTo(q.x, q.y, q.z);
            Fx.qi(sl, q.add(0, 1.3, 0), s.aim.scale(3), 1.4f, rgb, 0, 6);
            hitCircle(sl, b, q, w, mult(s.mv), h, true);
            if (t == 22 && kind == 0) addZone(sl, b, s.from.lerp(s.to, 0.5), 2.2, 80, 0.2, 0);
            if (t == 22 && kind == 1) delay(sl, b, s.to.add(s.aim.scale(2)), 4, 14, mult(s.mv) * 0.8, Hit.KNOCK_UP, 2, rgb);
        }
        return t < (kind == 1 ? 40 : 32);
    }

    /** Hơi thở hình nón: cảnh báo 16 tick, phun 24 tick (mỗi 4 tick 1 nhịp), quay chậm theo mục tiêu. */
    private static boolean breath(ServerLevel sl, HuVuongForm b, int t, double r, double half, int rgb, ParticleOptions pt, Hit h) {
        State s = b.movesA;
        if (t == 0) {
            reaim(b);
            Fx.telegraphCone(sl, null, b.position(), s.aim, (float) r, (float) half, 16);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, rgb == FROST ? 1.3f : 0.7f);
        }
        if (t >= 16 && t < 40) {
            if (t % 6 == 0) { // bám theo mục tiêu chậm
                Vec3 want = tpos(b).subtract(b.position()).multiply(1, 0, 1);
                if (want.lengthSqr() > 1e-3) { s.aim = s.aim.lerp(want.normalize(), 0.3).normalize(); face(b); }
            }
            Vec3 mouth = b.position().add(0, b.getBbHeight() * 0.7, 0);
            for (int i = 0; i < 3; i++) {
                Vec3 v = s.aim.yRot((float) Math.toRadians((b.getRandom().nextDouble() - 0.5) * half * 1.6)).scale(0.6 + b.getRandom().nextDouble() * 0.5);
                sl.sendParticles(pt, mouth.x, mouth.y, mouth.z, 0, v.x, -0.08, v.z, 1.0);
            }
            if (t % 4 == 0) {
                hitCone(sl, b, b.position(), s.aim, r, half, mult(s.mv), h);
                Fx.qi(sl, mouth, s.aim.scale(r * 0.9), (float) (r * 0.35), rgb, 0, 6);
                if (rgb == FROST) for (Player p : players(sl, b.position(), r)) p.setTicksFrozen(Math.min(p.getTicksFrozen() + 30, 300));
            }
            if (t == 16) b.triggerAnim("skill", "attack");
        }
        return t < 46;
    }

    /** Vòng nổ quanh thân 2 nhịp (ngoài rồi trong). */
    private static boolean nova2(ServerLevel sl, HuVuongForm b, int t, double R, int rgb, int rgb2, Hit h, boolean fire) {
        State s = b.movesA;
        Vec3 c = b.position();
        if (t == 0) {
            Fx.telegraphCircle(sl, null, c, (float) R, 30, DANGER);
            Fx.rune(sl, null, c.add(0, 0.1, 0), (float) R * 0.6f, rgb, 50, 8);
            b.triggerAnim("skill", "phase");
            b.playSound(SoundEvents.BLAZE_AMBIENT, 3f, 0.4f);
        }
        if (t < 30 && t % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, b.getX(), b.getY() + 2.5, b.getZ(), 8, 0.6, 1.2, 0.6, 0.05);
        if (t == 30 || t == 46) {
            double r = t == 30 ? R : R * 0.5;
            for (Player p : players(sl, c, r))
                if (flat2(p.position(), c) <= r * r && strike(b, p, b.moveDamage(mult(s.mv)))) { PhaseMoves.applyHit(p, h); if (fire) p.igniteForSeconds(4); }
            Fx.groundRing(sl, null, c, (float) r, rgb2, 24);
            Fx.elem(sl, null, Fx.E_TONGUES, c, Vec3.ZERO, (float) r * 0.35f, rgb2, 0);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + t;
                Fx.ribbon(sl, null, c.add(0, 0.5, 0), c.add(Math.cos(a) * r, 0.3, Math.sin(a) * r), 1.0, 8, 0, 0.5f, rgb);
            }
            if (t == 30) Fx.telegraphCircle(sl, null, c, (float) (R * 0.5), 16, DANGER);
            Fx.shake(sl, c, 2f, 12, 32f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        }
        return t < 56;
    }

    /** Cột phun dưới chân mọi người chơi, 2 đợt. vfx 0 máu / 3 băng. */
    private static boolean geysers(ServerLevel sl, HuVuongForm b, int t, int rgb, int vfx, Hit h) {
        State s = b.movesA;
        if (t == 0 || t == 30) {
            List<Vec3> at = new ArrayList<>();
            for (Player p : players(sl, b.position(), 24)) at.add(p.position());
            if (at.isEmpty()) at.add(tpos(b));
            for (Vec3 v : at) delay(sl, b, v, 2.8, 24, mult(s.mv), h, vfx, rgb);
            b.triggerAnim("skill", "cast");
            b.playSound(vfx == 3 ? SoundEvents.GLASS_BREAK : SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 3f, 0.5f);
        }
        return t < 60;
    }

    /** Mưa đòn trễ rải quanh mục tiêu (vòng nhỏ, so le). */
    private static boolean rain(ServerLevel sl, HuVuongForm b, int t, int n, double r, int rgb, int vfx, Hit h) {
        State s = b.movesA;
        if (t == 0) { b.triggerAnim("skill", "phase"); b.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.5f, 0.6f); }
        if (t % 4 == 0 && t / 4 < n) {
            Vec3 c = t / 4 % 3 == 0 ? tpos(b) : tpos(b).add((b.getRandom().nextDouble() - 0.5) * 12, 0, (b.getRandom().nextDouble() - 0.5) * 12);
            delay(sl, b, c, r, 22, mult(s.mv), h, vfx, rgb);
            sl.sendParticles(ParticleTypes.FALLING_LAVA, c.x, c.y + 8, c.z, 10, 0.6, 0.5, 0.6, 0);
        }
        return t < n * 4 + 28;
    }

    /** Nhảy / bay lên rồi lao xuống điểm đánh dấu. frost = để lại sương băng. */
    private static boolean dive(ServerLevel sl, HuVuongForm b, int t, double rad, int rgb, int rgb2, boolean frost) {
        State s = b.movesA;
        if (t == 0) {
            b.setNoGravity(true);
            b.triggerAnim("skill", "cast");
            s.spot = tpos(b);
            Fx.telegraphCircle(sl, null, s.spot, (float) rad, 28, DANGER);
            Fx.rune(sl, null, s.spot.add(0, 0.1, 0), (float) rad * 0.7f, rgb, 28, 6);
            if (frost) wings(sl, b, FROST, 40);
            b.playSound(frost ? SoundEvents.PHANTOM_FLAP : SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
        }
        if (t < 10) { b.setDeltaMovement(0, 0.8, 0); b.hurtMarked = true; }
        else if (t == 18) { b.teleportTo(s.spot.x, s.spot.y + 9, s.spot.z); b.setDeltaMovement(Vec3.ZERO); }
        else if (t > 18 && t < 28) { b.setDeltaMovement(Vec3.ZERO); if (t % 2 == 0) sl.sendParticles(frost ? ParticleTypes.SNOWFLAKE : ParticleTypes.COMPOSTER, b.getX(), b.getY(), b.getZ(), 8, 0.5, 0.5, 0.5, 0.02); }
        else if (t == 28) {
            Vec3 c = s.spot;
            Fx.ribbon(sl, null, b.position(), c, 0, 3, 0, 0.8f, rgb2);
            b.teleportTo(c.x, c.y, c.z);
            b.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, c, rad))
                if (flat2(p.position(), c) <= rad * rad && strike(b, p, b.moveDamage(mult(s.mv)))) PhaseMoves.applyHit(p, frost ? Hit.FREEZE : Hit.KNOCK_UP);
            Fx.groundCrack(sl, null, c, (float) rad, rgb, 8, 40);
            Fx.groundRing(sl, null, c, (float) rad * 1.4f, rgb2, 20);
            if (frost) addZone(sl, b, c, 4, 100, 0.2, 1);
            Fx.shake(sl, c, 2.5f, 16, 32f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.6f);
        }
        if (t == 32) b.setNoGravity(false);
        return t < 40;
    }

    // ================================================================== HUYẾT MA (Mohg)
    /** Huyết Diễm Lê Địa: kéo đinh ba, tia lửa chạy dọc vạch -> phun lửa + vũng máu. */
    private static boolean flameTrail(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        double len = 16;
        if (t == 0) {
            reaim(b);
            s.from = b.position(); s.to = s.from.add(s.aim.scale(len));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.to.subtract(s.from), 3.0f, 24, DANGER);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.GRINDSTONE_USE, 2.5f, 0.5f);
        }
        if (t < 24 && t % 2 == 0) {
            Vec3 q = s.from.lerp(s.to, t / 24.0);
            sl.sendParticles(ParticleTypes.FLAME, q.x, q.y + 0.2, q.z, 6, 0.3, 0.05, 0.3, 0.02);
        }
        if (t == 24) {
            Fx.rift(sl, null, s.from.add(0, 1.5, 0), s.to.subtract(s.from), 1.6f, FLAME, 36);
            Fx.ribbonWave(sl, null, s.from.add(0, 0.5, 0), s.to.add(0, 0.5, 0), 0.5, 1.0, 10, 0, 0.6f, BLOOD);
            for (Player p : hitLine(sl, b, s.from, s.aim, len, 1.6, mult(s.mv), Hit.KNOCK_UP)) p.igniteForSeconds(3);
            for (int i = 1; i <= 4; i++) addZone(sl, b, s.from.lerp(s.to, i / 4.5), 1.8, 100, 0.2, 0);
            Fx.shake(sl, b.position(), 1.5f, 10, 28f);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.BLAZE_SHOOT, 3f, 0.5f);
        }
        return t < 34;
    }

    /** Huyết Chú: đánh dấu người chơi bằng huyết ấn, ấn đi theo người -> nổ tại chỗ đứng (né bằng cách chạy khỏi vòng). */
    private static boolean bloodCurse(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            b.triggerAnim("skill", "phase");
            for (Player p : players(sl, b.position(), 22)) s.marked.add(p.getUUID());
            if (s.tgt instanceof Player tp && !s.marked.contains(tp.getUUID())) s.marked.add(tp.getUUID());
            for (Player p : marked(sl, b)) {
                Fx.ribbonWave(sl, null, b.position().add(0, 3, 0), p.position().add(0, 1, 0), 1.5, 0.8, 12, 0, 0.3f, BLOOD);
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
            b.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3f, 0.7f);
        }
        if (t < 26 && t % 5 == 0) for (Player p : marked(sl, b)) Fx.rune(sl, null, p.position().add(0, 0.1, 0), 1.6f, BLOOD, 6, 5);
        if (t == 26) for (Player p : marked(sl, b)) delay(sl, b, p.position(), 2.6, 18, mult(s.mv), Hit.WEAK, 0, BLOOD);
        if (t == 44) b.heal(b.moveDamage(0.5) * marked(sl, b).size());
        return t < 50;
    }

    /** Thánh Huyết Tế Lễ (tối thượng, kiểu Bloodboon/Nihil): bay lên, đánh dấu mọi người, 3 nhịp nổ + hồi máu. */
    static final int NI_MARK = 30, NI_END = 116;
    static final int[] NI_BURST = {60, 80, 100};

    private static boolean bloodboon(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            b.setNoGravity(true);
            b.triggerAnim("skill", "phase");
            wings(sl, b, BLOOD, 100);
            Fx.telegraphCircle(sl, null, b.position(), 20f, NI_MARK, DANGER);
            b.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        }
        if (t < 20) { b.setDeltaMovement(0, 0.18, 0); b.hurtMarked = true; } else b.setDeltaMovement(Vec3.ZERO);
        if (t % 4 == 0) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, b.getX(), b.getY() + 2, b.getZ(), 12, 2, 1.5, 2, 0.02);
        if (t == NI_MARK) {
            s.marked.clear();
            for (Player p : players(sl, b.position(), 24)) s.marked.add(p.getUUID());
            if (s.tgt instanceof Player tp && tp.isAlive() && !s.marked.contains(tp.getUUID())) s.marked.add(tp.getUUID());
            for (Player p : marked(sl, b)) {
                Fx.ribbonWave(sl, null, b.position().add(0, 2, 0), p.position().add(0, 1, 0), 1.5, 0.8, 12, 0, 0.3f, BLOOD);
                Fx.rune(sl, null, p.position().add(0, 0.1, 0), 1.6f, BLOOD, NI_END - NI_MARK, 5);
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, NI_END - NI_MARK, 0));
            }
            Fx.elem(sl, null, Fx.E_HALOS, b.position().add(0, 2, 0), Vec3.ZERO, 3f, BLOOD, 0);
            b.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3f, 0.6f);
        }
        for (int i = 0; i < NI_BURST.length; i++) {
            if (t == NI_BURST[i] - 12) for (Player p : marked(sl, b)) Fx.telegraphCircle(sl, null, p.position(), 1.8f, 12, DANGER);
            if (t == NI_BURST[i]) {
                Vec3 core = b.position().add(0, b.getBbHeight() * 0.6, 0);
                for (Player p : marked(sl, b)) {
                    float dmg = b.moveDamage(mult(s.mv) * (1 + i * 0.25));
                    if (strike(b, p, dmg)) b.heal(dmg * 3f);
                    Fx.elem(sl, null, Fx.E_NUKE, p.position(), Vec3.ZERO, 0.8f, BLOOD, 0);
                    Fx.ribbonHelix(sl, null, p.position().add(0, 1, 0), core, 0.6, i * 2.0, 14, 0, 0.25f, BLOOD);
                }
                Fx.groundRing(sl, null, b.position(), 6f + i * 3, BLOOD, 20);
                Fx.shake(sl, b.position(), 1.5f + i, 10, 48f);
                b.playSound(SoundEvents.WARDEN_HEARTBEAT, 4f, 0.6f + i * 0.1f);
            }
        }
        if (t == NI_END - 6) b.setNoGravity(false);
        return t < NI_END;
    }

    // ================================================================== CỐT LONG (Lichdragon / Placidusax)
    /** Vĩ Tảo: quét đuôi quanh thân 2 vòng (vòng 2 rộng hơn, trễ). */
    private static boolean tailSweep(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        Vec3 c = b.position();
        if (t == 0) { Fx.telegraphCircle(sl, null, c, 7f, 18, DANGER); b.triggerAnim("skill", "attack"); b.playSound(SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.6f); }
        if (t == 18 || t == 40) {
            double r = t == 18 ? 7 : 9;
            hitCircle(sl, b, c, r, mult(s.mv), Hit.KNOCK_UP, false);
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5 + (t == 18 ? 0 : 0.3);
                Fx.ribbon(sl, null, c.add(0, 0.8, 0), c.add(Math.cos(a) * r, 0.5, Math.sin(a) * r), 1.5, 6, i, 0.6f, BONE);
            }
            Fx.groundRing(sl, null, c, (float) r, BONE, 20);
            sl.sendParticles(ParticleTypes.WHITE_ASH, c.x, c.y + 0.6, c.z, 60, r * 0.5, 0.2, r * 0.5, 0.05);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.4f);
            if (t == 18) { Fx.telegraphCircle(sl, null, c, 9f, 22, DANGER); b.triggerAnim("skill", "attack"); }
        }
        return t < 48;
    }

    /** Hồng Lôi Kích: 3 đợt sét đỏ giáng dưới chân người chơi (+ điểm ngẫu nhiên). */
    private static boolean lightningWaves(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) { b.triggerAnim("skill", "phase"); b.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 1.4f); }
        if (t == 0 || t == 16 || t == 32) {
            for (Player p : players(sl, b.position(), 24)) delay(sl, b, p.position(), 2.4, 16, mult(s.mv), Hit.SLOW, 1, RED_BOLT);
            Vec3 r = b.position().add((b.getRandom().nextDouble() - 0.5) * 16, 0, (b.getRandom().nextDouble() - 0.5) * 16);
            delay(sl, b, r, 2.4, 16, mult(s.mv), Hit.SLOW, 1, RED_BOLT);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.getX(), b.getY() + b.getBbHeight(), b.getZ(), 30, 1, 1, 1, 0.2);
        }
        return t < 52;
    }

    /** Thời Không Liệt (tối thượng, kiểu Placidusax): rồng tan vào khe thời gian, sét đỏ phủ đấu trường, tái hiện trên mục tiêu + dậm. */
    private static boolean timeRift(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            Fx.telegraphCircle(sl, null, b.position(), 20f, 30, DANGER);
            Fx.rune(sl, null, b.position().add(0, 0.1, 0), 8f, GOLD, 100, 12);
            b.triggerAnim("skill", "phase");
            b.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.5f);
        }
        if (t == 22) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, b.getX(), b.getY() + 1.5, b.getZ(), 120, 1.5, 1.5, 1.5, 0.3);
            Fx.elem(sl, null, Fx.E_HALOS, b.position().add(0, 2, 0), Vec3.ZERO, 3f, GOLD, 0);
            b.setInvisible(true);
            b.setNoGravity(true);
            s.spot = b.position();
            b.playSound(SoundEvents.ENDERMAN_TELEPORT, 3f, 0.5f);
        }
        if (t >= 30 && t <= 78 && t % 8 == 6) {
            List<Player> ps = players(sl, s.spot != null ? s.spot : b.position(), 24);
            for (Player p : ps) delay(sl, b, p.position(), 2.6, 16, mult(s.mv) * 0.7, Hit.SLOW, 1, RED_BOLT);
            for (int i = 0; i < 2; i++) {
                Vec3 base = s.spot != null ? s.spot : b.position();
                delay(sl, b, base.add((b.getRandom().nextDouble() - 0.5) * 30, 0, (b.getRandom().nextDouble() - 0.5) * 30), 2.6, 16, mult(s.mv) * 0.7, Hit.SLOW, 1, RED_BOLT);
            }
        }
        if (t == 84) {
            Vec3 c = tpos(b);
            s.spot = c;
            Fx.telegraphCircle(sl, null, c, 7f, 18, DANGER);
            Fx.rune(sl, null, c.add(0, 0.1, 0), 5f, GOLD, 18, 8);
            b.teleportTo(c.x, c.y + 8, c.z);
        }
        if (t > 84 && t < 102) b.setDeltaMovement(Vec3.ZERO);
        if (t == 90) { b.setInvisible(false); sl.sendParticles(ParticleTypes.REVERSE_PORTAL, b.getX(), b.getY(), b.getZ(), 80, 1.5, 1.5, 1.5, 0.3); }
        if (t == 102) {
            Vec3 c = s.spot;
            b.teleportTo(c.x, c.y, c.z);
            b.setNoGravity(false);
            hitCircle(sl, b, c, 7, mult(s.mv) * 1.5, Hit.KNOCK_UP, false);
            Fx.groundCrack(sl, null, c, 7f, GOLD, 10, 50);
            Fx.groundRing(sl, null, c, 10f, RED_BOLT, 30);
            for (int i = 0; i < 6; i++) bolt(sl, c.add(Math.cos(i) * 5, 0, Math.sin(i) * 5));
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 2, 1, 0, 1, 0);
            Fx.shake(sl, c, 3f, 20, 40f);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
        }
        return t < 112;
    }

    /** Thuấn Di: tan vào khe thời gian, hiện sau lưng mục tiêu và cắn. */
    private static boolean warpBite(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            LivingEntity tg = s.tgt;
            Vec3 tp = tpos(b);
            Vec3 back = tg != null ? tg.getViewVector(1f).multiply(1, 0, 1) : s.aim;
            if (back.lengthSqr() < 1e-3) back = s.aim;
            s.spot = tp.subtract(back.normalize().scale(3));
            Fx.telegraphCircle(sl, null, tp, 3.5f, 20, DANGER);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, b.getX(), b.getY() + 1.5, b.getZ(), 80, 1.2, 1.2, 1.2, 0.3);
            b.setInvisible(true);
            b.playSound(SoundEvents.ENDERMAN_TELEPORT, 3f, 0.6f);
            s.to = tp;
        }
        if (t == 14) { b.teleportTo(s.spot.x, s.spot.y, s.spot.z); b.setInvisible(false); s.aim = s.to.subtract(s.spot).multiply(1, 0, 1).normalize(); face(b); b.triggerAnim("skill", "attack"); }
        if (t == 20) {
            hitCircle(sl, b, s.to, 3.5, mult(s.mv), Hit.WEAK, false);
            Fx.elem(sl, null, Fx.E_ARC, s.to.add(0, 1, 0), s.aim, 1.5f, BONE, 0);
            Fx.groundCrack(sl, null, s.to, 3.5f, GOLD, 6, 30);
            b.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 3f, 0.5f);
        }
        return t < 30;
    }

    /** Lôi Tức: phun tia sét thẳng (2 lần, lần 2 ngắm lại). */
    private static boolean boltBreath(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        double len = 22;
        if (t == 0 || t == 30) {
            reaim(b);
            Fx.telegraphLine(sl, null, b.position().add(0, 0.05, 0), s.aim.scale(len), 2.6f, 18, RED_BOLT);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 0.9f);
        }
        if (t == 18 || t == 48) {
            Vec3 o = b.position();
            Fx.rift(sl, null, o.add(0, b.getBbHeight() * 0.6, 0), s.aim.scale(len), 0.6f, RED_BOLT, 20);
            Fx.ribbonWave(sl, null, o.add(0, b.getBbHeight() * 0.6, 0), o.add(s.aim.scale(len)).add(0, 0.6, 0), 0.3, 1.4, 6, 0, 0.4f, RED_BOLT);
            hitLine(sl, b, o, s.aim, len, 1.3, mult(s.mv), Hit.SLOW);
            for (int i = 1; i <= 3; i++) bolt(sl, o.add(s.aim.scale(len * i / 3.0)));
            b.triggerAnim("skill", "attack");
        }
        return t < 56;
    }

    /** Cốt Vũ: 4 vòng gai xương lan ra từ thân (nhảy qua từng vòng). */
    private static boolean boneWaves(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            s.spot = b.position();
            Fx.telegraphCircle(sl, null, s.spot, 13f, 20, DANGER);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
        }
        for (int i = 0; i < 4; i++) {
            if (t != 20 + i * 6) continue;
            double r = 3 + i * 3.2;
            for (Player p : players(sl, s.spot, r + 1.5)) {
                double d = Math.sqrt(flat2(p.position(), s.spot));
                if (d >= r - 1.6 && d <= r + 1.6 && strike(b, p, b.moveDamage(mult(s.mv)))) PhaseMoves.applyHit(p, Hit.KNOCK_UP);
            }
            Fx.groundRing(sl, null, s.spot, (float) r, BONE, 14);
            for (int k = 0; k < 8 + i * 3; k++) {
                double a = k * 2 * Math.PI / (8 + i * 3);
                Fx.elem(sl, null, Fx.E_SPIKES, s.spot.add(Math.cos(a) * r, 0, Math.sin(a) * r), new Vec3(0, 1, 0), 0.6f, BONE, 0);
            }
            Fx.shake(sl, s.spot, 1.2f, 6, 24f);
            b.playSound(SoundEvents.BONE_BLOCK_BREAK, 3f, 0.6f);
        }
        return t < 46;
    }

    // ================================================================== MỘC YÊU (Ulcerated Tree Spirit)
    /** Phệ Cấu: 2 cú lao cắn liên tiếp. */
    private static boolean doubleBite(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        int k = t < 26 ? t : t - 26;
        if (k == 0) {
            reaim(b);
            s.from = b.position(); s.to = s.from.add(s.aim.scale(9));
            s.hitOnce.clear();
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.to.subtract(s.from), 3.0f, 12, ROT);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.8f);
        }
        if (k >= 12 && k < 16) {
            if (k == 12) b.triggerAnim("skill", "attack");
            Vec3 q = s.from.lerp(s.to, (k - 11) / 4.0);
            b.teleportTo(q.x, q.y, q.z);
            sl.sendParticles(ParticleTypes.COMPOSTER, q.x, q.y + 1, q.z, 10, 1, 0.5, 1, 0.05);
            hitCircle(sl, b, q, 2.8, mult(s.mv), Hit.POISON, true);
            if (k == 15) { Fx.elem(sl, null, Fx.E_ARC, q.add(0, 1, 0), s.aim, 1.6f, ROT, 0); b.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 3f, 0.6f); }
        }
        return t < 50;
    }

    /** Rễ Đâm: hàng rễ trồi theo vạch từ thân tới từng người chơi. */
    private static boolean roots(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            b.triggerAnim("skill", "phase");
            List<Player> ps = players(sl, b.position(), 22);
            List<Vec3> ends = new ArrayList<>();
            for (Player p : ps) ends.add(p.position());
            if (ends.isEmpty()) ends.add(tpos(b));
            for (Vec3 e : ends) {
                Vec3 d = e.subtract(b.position()).multiply(1, 0, 1);
                double len = Math.max(4, d.length() + 2);
                Vec3 n = d.lengthSqr() < 1e-3 ? s.aim : d.normalize();
                for (int i = 1; i <= 6; i++) delay(sl, b, b.position().add(n.scale(len * i / 6.0)), 1.7, 14 + i * 3, mult(s.mv), Hit.SLOW, 4, WOOD);
            }
            b.playSound(SoundEvents.ROOTED_DIRT_BREAK, 3f, 0.5f);
        }
        return t < 40;
    }

    /** Nổ Bướu: gieo bầu mủ quanh người chơi; bầu phát sáng rồi nổ. */
    private static boolean bulbs(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            b.triggerAnim("skill", "cast");
            List<Vec3> at = new ArrayList<>();
            for (Player p : players(sl, b.position(), 22)) at.add(p.position());
            while (at.size() < 5) at.add(b.position().add((b.getRandom().nextDouble() - 0.5) * 18, 0, (b.getRandom().nextDouble() - 0.5) * 18));
            for (int i = 0; i < at.size(); i++) {
                Vec3 c = at.get(i);
                Fx.ribbon(sl, null, b.position().add(0, 2, 0), c, 3.0, 10, i * 2, 0.3f, SAP);
                Fx.pool(sl, null, c, 1.0f, BULB, 40 + i * 4);
                delay(sl, b, c, 2.8, 40 + i * 4, mult(s.mv), Hit.KNOCK_UP, 2, BULB);
            }
            b.playSound(SoundEvents.SLIME_SQUISH, 3f, 0.4f);
        }
        if (t % 6 == 0) sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, b.getX(), b.getY() + 2, b.getZ(), 10, 2, 1, 2, 0);
        return t < 30;
    }

    /** Lăn Cuộn: cuộn thân lăn húc dài theo vạch, kết thúc bằng cú đập. */
    private static boolean rollCharge(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        double len = 18;
        if (t == 0) {
            reaim(b);
            s.from = b.position(); s.to = s.from.add(s.aim.scale(len));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.to.subtract(s.from), 3.4f, 18, ROT);
            b.triggerAnim("skill", "phase");
            b.playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.6f);
        }
        if (t >= 18 && t < 28) {
            Vec3 q = s.from.lerp(s.to, (t - 17) / 10.0);
            b.teleportTo(q.x, q.y, q.z);
            sl.sendParticles(ParticleTypes.COMPOSTER, q.x, q.y + 0.5, q.z, 14, 1.5, 0.3, 1.5, 0.05);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, q.x, q.y + 0.2, q.z, 2, 0.5, 0, 0.5, 0.01);
            for (Player p : players(sl, q, 3.2)) {
                if (flat2(p.position(), q) > 3.2 * 3.2 || !s.hitOnce.add(p.getUUID())) continue;
                if (strike(b, p, b.moveDamage(mult(s.mv)))) {
                    p.setDeltaMovement(p.getDeltaMovement().add(s.aim.x * 1.5, 0.6, s.aim.z * 1.5));
                    p.hurtMarked = true;
                }
            }
            if (t % 3 == 0) b.playSound(SoundEvents.RAVAGER_STEP, 3f, 0.5f);
        }
        if (t == 28) Fx.telegraphCircle(sl, null, s.to, 4f, 10, DANGER);
        if (t == 38) {
            hitCircle(sl, b, s.to, 4, mult(s.mv) * 0.7, Hit.KNOCK_UP, false);
            Fx.groundCrack(sl, null, s.to, 4f, WOOD, 8, 30);
            Fx.shake(sl, s.to, 2f, 10, 28f);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.7f);
        }
        return t < 46;
    }

    /** Quật Thân: xoay quật thân 3 nhịp quanh mình. */
    private static boolean flail(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        Vec3 c = b.position();
        if (t == 0) { Fx.telegraphCircle(sl, null, c, 6f, 16, DANGER); b.triggerAnim("skill", "phase"); }
        if (t == 16 || t == 24 || t == 32) {
            s.hitOnce.clear();
            hitCircle(sl, b, c, 6, mult(s.mv), t == 32 ? Hit.KNOCK_UP : Hit.POISON, false);
            Fx.qi(sl, c.add(0, 1.5, 0), s.aim.yRot(t * 0.5f).scale(4), 3f, ROT, t * 20, 8);
            sl.sendParticles(ParticleTypes.COMPOSTER, c.x, c.y + 1, c.z, 40, 3, 0.5, 3, 0.1);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.4f);
        }
        return t < 40;
    }

    /** Vạn Mộc Hủy Diệt (tối thượng): rễ quấn quanh đấu trường, bầu mủ phủ đất nổ dây chuyền, rồi bùng nổ tâm. */
    private static boolean treeUlt(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        Vec3 c = b.position();
        if (t == 0) {
            s.spot = c;
            Fx.telegraphCircle(sl, null, c, 18f, 30, DANGER);
            Fx.rune(sl, null, c.add(0, 0.1, 0), 9f, WOOD, 110, 10);
            b.triggerAnim("skill", "phase");
            b.playSound(SoundEvents.WARDEN_ROAR, 3f, 0.6f);
        }
        if (t == 30) {
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8;
                delay(sl, b, s.spot.add(Math.cos(a) * 15, 0, Math.sin(a) * 15), 2.2, 6 + k, mult(s.mv) * 0.6, Hit.SLOW, 4, WOOD);
            }
        }
        if (t >= 40 && t <= 84 && t % 4 == 0) {
            List<Player> ps = players(sl, s.spot, 20);
            Vec3 at = !ps.isEmpty() && t % 8 == 0 ? ps.get(b.getRandom().nextInt(ps.size())).position()
                    : s.spot.add((b.getRandom().nextDouble() - 0.5) * 28, 0, (b.getRandom().nextDouble() - 0.5) * 28);
            Fx.pool(sl, null, at, 1.0f, BULB, 26);
            delay(sl, b, at, 3.0, 26, mult(s.mv), Hit.KNOCK_UP, 2, BULB);
        }
        if (t == 88) { Fx.telegraphCircle(sl, null, s.spot, 9f, 20, DANGER); b.triggerAnim("skill", "phase"); }
        if (t == 108) {
            hitCircle(sl, b, s.spot, 9, mult(s.mv) * 1.4, Hit.KNOCK_UP, false);
            Fx.elem(sl, null, Fx.E_NUKE, s.spot, Vec3.ZERO, 2.5f, BULB, 0);
            Fx.groundCrack(sl, null, s.spot, 9f, ROT, 12, 50);
            Fx.groundRing(sl, null, s.spot, 12f, BULB, 30);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, s.spot.x, s.spot.y + 0.5, s.spot.z, 3, 2, 0.3, 2, 0);
            Fx.shake(sl, s.spot, 3f, 20, 40f);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
        }
        return t < 116;
    }

    // ================================================================== BĂNG PHÁCH (Borealis)
    /** Vụ Đông: phả sương băng thành các vùng lưu quanh người chơi + quanh thân. */
    private static boolean mist(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.POWDER_SNOW_BREAK, 3f, 0.5f);
            s.spots.clear();
            for (Player p : players(sl, b.position(), 22)) s.spots.add(p.position());
            while (s.spots.size() < 3) s.spots.add(b.position().add((b.getRandom().nextDouble() - 0.5) * 14, 0, (b.getRandom().nextDouble() - 0.5) * 14));
            for (Vec3 v : s.spots) Fx.telegraphCircle(sl, null, v, 3.5f, 20, FROST);
            Fx.telegraphCircle(sl, null, b.position(), 5f, 20, FROST);
        }
        if (t < 20 && t % 2 == 0) for (Vec3 v : s.spots) Fx.ribbon(sl, null, b.position().add(0, 3, 0), v, 2.0, 10, 0, 0.3f, ICE);
        if (t == 20) {
            for (Vec3 v : s.spots) addZone(sl, b, v, 3.5, 120, mult(s.mv), 1);
            addZone(sl, b, b.position(), 5, 80, mult(s.mv), 1);
            b.playSound(SoundEvents.PLAYER_HURT_FREEZE, 3f, 0.6f);
        }
        return t < 26;
    }

    /** Phong Dực: vỗ cánh 2 lần — gió đẩy văng + mảnh băng. */
    private static boolean gust(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0 || t == 26) {
            reaim(b);
            Fx.telegraphCone(sl, null, b.position(), s.aim, 10f, 50, 14);
            wings(sl, b, FROST, 20);
            b.triggerAnim("skill", "cast");
        }
        if (t == 14 || t == 40) {
            for (Player p : hitCone(sl, b, b.position(), s.aim, 10, 50, mult(s.mv), Hit.SLOW)) {
                Vec3 away = p.position().subtract(b.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-3) away = away.normalize();
                p.setDeltaMovement(p.getDeltaMovement().add(away.x * 2.0, 0.45, away.z * 2.0));
                p.hurtMarked = true;
            }
            Fx.elem(sl, null, Fx.E_NEEDLES, b.position().add(0, 2, 0), s.aim, 1.6f, ICE, 0);
            Fx.qi(sl, b.position().add(0, 1.5, 0), s.aim.scale(8), 5f, FROST, 0, 10);
            sl.sendParticles(ParticleTypes.CLOUD, b.getX() + s.aim.x * 4, b.getY() + 1, b.getZ() + s.aim.z * 4, 40, 3, 0.6, 3, 0.2);
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.PHANTOM_FLAP, 3f, 0.4f);
        }
        return t < 48;
    }

    /** Phi Tảo: bay lên, lướt ngang qua mục tiêu, để lại vệt sương băng. */
    private static boolean flySweep(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        double len = 24;
        if (t == 0) {
            reaim(b);
            b.setNoGravity(true);
            wings(sl, b, FROST, 50);
            s.from = b.position(); s.to = s.from.add(s.aim.scale(len));
            Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.to.subtract(s.from), 4f, 20, FROST);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.PHANTOM_FLAP, 3f, 0.6f);
        }
        if (t < 8) { b.setDeltaMovement(0, 0.35, 0); b.hurtMarked = true; }
        if (t >= 20 && t < 30) {
            Vec3 q = s.from.lerp(s.to, (t - 19) / 10.0);
            b.teleportTo(q.x, q.y + 1.5, q.z);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, q.x, q.y + 1, q.z, 20, 1.5, 0.5, 1.5, 0.05);
            for (Player p : players(sl, q, 2.6)) {
                if (flat2(p.position(), q) > 2.6 * 2.6 || !s.hitOnce.add(p.getUUID())) continue;
                if (strike(b, p, b.moveDamage(mult(s.mv)))) PhaseMoves.applyHit(p, Hit.FREEZE);
            }
            if (t % 3 == 0) addZone(sl, b, q, 2.2, 80, 0.2, 1);
            if (t == 20) b.triggerAnim("skill", "attack");
        }
        if (t == 30) { b.setNoGravity(false); b.setDeltaMovement(Vec3.ZERO); }
        return t < 36;
    }

    /** Sương Mù Tức: 3 tia sương quét hình quạt (-25°, 0°, +25°). */
    private static boolean fanLines(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        double len = 20;
        if (t == 0) {
            reaim(b);
            s.from = b.position();
            for (int i = -1; i <= 1; i++)
                Fx.telegraphLine(sl, null, s.from.add(0, 0.05, 0), s.aim.yRot((float) Math.toRadians(25 * i)).scale(len), 2.4f, 16 + (i + 1) * 8, FROST);
            b.triggerAnim("skill", "cast");
            b.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.5f, 1.4f);
        }
        for (int i = -1; i <= 1; i++) {
            if (t != 16 + (i + 1) * 8) continue;
            Vec3 d = s.aim.yRot((float) Math.toRadians(25 * i));
            Fx.rift(sl, null, s.from.add(0, 1.5, 0), d.scale(len), 0.8f, FROST, 20);
            hitLine(sl, b, s.from, d, len, 1.2, mult(s.mv), Hit.FREEZE);
            for (int k = 1; k <= 4; k++) { Vec3 q = s.from.add(d.scale(len * k / 4.0)); sl.sendParticles(ParticleTypes.SNOWFLAKE, q.x, q.y + 0.5, q.z, 12, 0.6, 0.4, 0.6, 0.02); }
            b.triggerAnim("skill", "attack");
            b.playSound(SoundEvents.GLASS_BREAK, 2f, 0.6f);
        }
        return t < 40;
    }

    /** Băng Phách Tuyệt Vực (tối thượng, kiểu Borealis): bay cao, bão tuyết phủ trường, 3 đợt băng giáng, rồi đóng băng toàn vùng (chỉ an toàn sát chân boss). */
    private static boolean blizzard(ServerLevel sl, HuVuongForm b, int t) {
        State s = b.movesA;
        if (t == 0) {
            s.spot = b.position();
            b.setNoGravity(true);
            wings(sl, b, FROST, 110);
            Fx.telegraphCircle(sl, null, s.spot, 20f, 30, FROST);
            b.triggerAnim("skill", "phase");
            b.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 1.2f);
        }
        if (t < 16) { b.setDeltaMovement(0, 0.3, 0); b.hurtMarked = true; } else b.setDeltaMovement(Vec3.ZERO);
        if (t % 3 == 0) sl.sendParticles(ParticleTypes.SNOWFLAKE, s.spot.x, s.spot.y + 6, s.spot.z, 40, 14, 3, 14, 0.05);
        if (t == 30 || t == 50 || t == 70) {
            for (Player p : players(sl, s.spot, 24)) {
                delay(sl, b, p.position(), 3.0, 18, mult(s.mv) * 0.7, Hit.FREEZE, 3, ICE);
                Fx.ribbon(sl, null, b.position().add(0, 2, 0), p.position(), 0.5, 10, 0, 0.5f, FROST);
            }
            b.triggerAnim("skill", "attack");
        }
        if (t == 80) {
            Fx.telegraphCircle(sl, null, s.spot, 20f, 30, DANGER);
            Fx.groundRing(sl, null, s.spot, 4f, ICE, 30);  // vùng an toàn sát chân
            b.teleportTo(s.spot.x, s.spot.y + 2, s.spot.z);
        }
        if (t == 110) {
            for (Player p : players(sl, s.spot, 20)) {
                double d2 = flat2(p.position(), s.spot);
                if (d2 <= 4 * 4 || d2 > 20 * 20) continue;
                if (strike(b, p, b.moveDamage(mult(s.mv) * 1.3))) PhaseMoves.applyHit(p, Hit.FREEZE);
            }
            Fx.elem(sl, null, Fx.E_NUKE, s.spot, Vec3.ZERO, 3f, ICE, 0);
            Fx.groundCrack(sl, null, s.spot, 20f, FROST, 16, 60);
            addZone(sl, b, s.spot, 8, 100, 0.2, 1);
            Fx.shake(sl, s.spot, 3f, 20, 40f);
            b.playSound(SoundEvents.GLASS_BREAK, 4f, 0.4f);
            b.setNoGravity(false);
        }
        return t < 118;
    }

    // ================================================================== trợ giúp
    static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 6, r), p -> !p.isSpectator() && p.isAlive());
    }

    static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    static boolean strike(HuVuongForm b, LivingEntity e, float dmg) {
        boolean ok = e.hurt(b.damageSources().mobAttack(b), dmg);
        if (ok) HuSkill.hu(e, 100);
        return ok;
    }

    private static Vec3 tpos(HuVuongForm b) {
        LivingEntity t = b.movesA.tgt;
        if (t != null && t.isAlive()) return t.position();
        return b.position().add(b.movesA.aim.scale(6));
    }

    private static void reaim(HuVuongForm b) {
        Vec3 dir = tpos(b).subtract(b.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) b.movesA.aim = dir.normalize();
        face(b);
    }

    private static void face(HuVuongForm b) {
        Vec3 a = b.movesA.aim;
        b.setYRot((float) Math.toDegrees(Math.atan2(-a.x, a.z)));
        b.yBodyRot = b.getYRot(); b.yHeadRot = b.getYRot();
    }

    private static List<Player> marked(ServerLevel sl, HuVuongForm b) {
        List<Player> out = new ArrayList<>();
        for (UUID id : b.movesA.marked) {
            Player p = sl.getPlayerByUUID(id);
            if (p != null && p.isAlive() && !p.isSpectator() && p.distanceToSqr(b) < 64 * 64) out.add(p);
        }
        return out;
    }

    private static void hitCircle(ServerLevel sl, HuVuongForm b, Vec3 c, double r, double mul, Hit h, boolean once) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 4) continue;
            if (once && !b.movesA.hitOnce.add(p.getUUID())) continue;
            if (strike(b, p, b.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    private static List<Player> hitCone(ServerLevel sl, HuVuongForm b, Vec3 c, Vec3 aim, double rad, double half, double mul, Hit h) {
        double cos = Math.cos(Math.toRadians(half));
        List<Player> hit = new ArrayList<>();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.5 && rel.normalize().dot(aim) < cos)) continue;
            if (strike(b, p, b.moveDamage(mul))) { PhaseMoves.applyHit(p, h); hit.add(p); }
        }
        return hit;
    }

    private static List<Player> hitLine(ServerLevel sl, HuVuongForm b, Vec3 from, Vec3 dir, double len, double half, double mul, Hit h) {
        List<Player> hit = new ArrayList<>();
        for (Player p : players(sl, from, len)) {
            Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
            double along = rel.dot(dir);
            if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > half) continue;
            if (strike(b, p, b.moveDamage(mul))) { PhaseMoves.applyHit(p, h); hit.add(p); }
        }
        return hit;
    }

    private static void bolt(ServerLevel sl, Vec3 at) {
        LightningBolt lb = EntityType.LIGHTNING_BOLT.create(sl);
        if (lb == null) return;
        lb.moveTo(at.x, at.y, at.z);
        lb.setVisualOnly(true);
        sl.addFreshEntity(lb);
    }

    static void delay(ServerLevel sl, HuVuongForm b, Vec3 c, double r, int after, double mul, Hit h, int vfx, int rgb) {
        b.movesA.delayed.add(new Delayed(c, r, after, b.moveDamage(mul), h, vfx, rgb));
        Fx.telegraphCircle(sl, null, c, (float) r, after, vfx == 3 ? FROST : DANGER);
    }

    private static void tickDelayed(ServerLevel sl, HuVuongForm b) {
        for (Iterator<Delayed> it = b.movesA.delayed.iterator(); it.hasNext(); ) {
            Delayed d = it.next();
            if (--d.left > 0) continue;
            it.remove();
            for (Player p : players(sl, d.c, d.r))
                if (flat2(p.position(), d.c) <= d.r * d.r && Math.abs(p.getY() - d.c.y) < 4 && strike(b, p, d.dmg)) PhaseMoves.applyHit(p, d.hit);
            switch (d.vfx) {
                case 1 -> { bolt(sl, d.c); Fx.groundRing(sl, null, d.c, (float) d.r, d.rgb, 12); sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, d.c.x, d.c.y + 0.5, d.c.z, 20, 0.6, 0.6, 0.6, 0.3); }
                case 2 -> { Fx.elem(sl, null, Fx.E_NUKE, d.c, Vec3.ZERO, (float) d.r * 0.35f, d.rgb, 0); sl.sendParticles(ParticleTypes.EXPLOSION, d.c.x, d.c.y + 0.5, d.c.z, 2, 0.6, 0.2, 0.6, 0); b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.9f); }
                case 3 -> { Fx.elem(sl, null, Fx.E_SPIKES, d.c, new Vec3(0, 1, 0), (float) d.r * 0.5f, d.rgb, 0); sl.sendParticles(ParticleTypes.SNOWFLAKE, d.c.x, d.c.y + 1, d.c.z, 30, 0.8, 1, 0.8, 0.05); b.playSound(SoundEvents.GLASS_BREAK, 1.5f, 0.7f); }
                case 4 -> { Fx.elem(sl, null, Fx.E_SPIKES, d.c, new Vec3(0, 1, 0), (float) d.r * 0.5f, d.rgb, 0); sl.sendParticles(ParticleTypes.COMPOSTER, d.c.x, d.c.y + 0.6, d.c.z, 10, 0.5, 0.5, 0.5, 0.05); }
                default -> { Fx.elem(sl, null, Fx.E_SPIKES, d.c, new Vec3(0, 1, 0), (float) d.r * 0.5f, d.rgb, 0); Fx.ribbonHelix(sl, null, d.c, d.c.add(0, 5, 0), 0.7, d.left, 10, 0, 0.3f, FLAME); }
            }
        }
    }

    static void addZone(ServerLevel sl, HuVuongForm b, Vec3 c, double r, int life, double mul, int kind) {
        b.movesA.zones.add(new Zone(c, r, life, b.moveDamage(mul), kind));
        Fx.pool(sl, null, c, (float) r, kind == 1 ? FROST : BLOOD, life);
    }

    /** Vùng lưu: mỗi 10 tick sát thương phép; lửa = bốc cháy, băng = đóng băng + chậm. */
    private static void tickZones(ServerLevel sl, HuVuongForm b) {
        for (Iterator<Zone> it = b.movesA.zones.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            if (--z.left <= 0) { it.remove(); continue; }
            if (z.left % 5 == 0) sl.sendParticles(z.kind == 1 ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, z.c.x, z.c.y + 0.3, z.c.z, 4, z.r * 0.5, 0.2, z.r * 0.5, 0.01);
            if (z.left % 10 != 0) continue;
            for (Player pl : players(sl, z.c, z.r)) {
                if (flat2(pl.position(), z.c) > z.r * z.r || Math.abs(pl.getY() - z.c.y) > 2.5) continue;
                pl.hurt(b.damageSources().magic(), z.dmg);
                if (z.kind == 1) { pl.setTicksFrozen(Math.min(pl.getTicksFrozen() + 40, 300)); pl.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)); }
                else pl.igniteForSeconds(2);
            }
        }
    }

    // ------------------------------------------------------------------ VFX
    /** Cánh (máu / băng): dải cong từ lưng toả hai bên. */
    static void wings(ServerLevel sl, HuVuongForm b, int rgb, int life) {
        Vec3 back = b.position().add(0, b.getBbHeight() * 0.7, 0);
        float yaw = (float) Math.toRadians(b.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        int alt = rgb == FROST ? ICE : FLAME;
        for (int side = -1; side <= 1; side += 2)
            for (int i = 0; i < 4; i++) {
                Vec3 tip = back.add(right.scale(side * (3.5 + i))).add(0, 3.0 - i * 1.3, 0);
                Fx.ribbon(sl, null, back, tip, 2.0 - i * 0.4, 12 + i * 3, i * 2, 0.4f, i % 2 == 0 ? rgb : alt);
            }
        Fx.rune(sl, null, back.add(0, 1.2, 0), 2.5f, rgb, life, 6);
    }

    /** Lộ diện pha 2/3 theo dạng. */
    public static void transformFx(ServerLevel sl, HuVuongForm b, int ph) {
        Vec3 c = b.position();
        switch (b.form()) {
            case HUYET_MA -> {
                wings(sl, b, ph >= 3 ? DARK : BLOOD, 80);
                Fx.groundRing(sl, null, c, ph >= 3 ? 10f : 7f, FLAME, 40);
                Fx.pool(sl, null, c, 5f, BLOOD, 100);
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE, c.x, c.y + 2, c.z, 100, 2.5, 2, 2.5, 0.05);
            }
            case COT_LONG -> {
                Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 2.5, 0), Vec3.ZERO, 3f, ph >= 3 ? RED_BOLT : GOLD, 0);
                Fx.groundCrack(sl, null, c, 8f, BONE, 10, 50);
                if (ph >= 3) for (int i = 0; i < 4; i++) bolt(sl, c.add(Math.cos(i * 1.6) * 6, 0, Math.sin(i * 1.6) * 6));
                sl.sendParticles(ParticleTypes.WHITE_ASH, c.x, c.y + 2, c.z, 120, 3, 2, 3, 0.05);
            }
            case MOC_YEU -> {
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI / 5;
                    Fx.elem(sl, null, Fx.E_SPIKES, c.add(Math.cos(a) * 5, 0, Math.sin(a) * 5), new Vec3(0, 1, 0), 0.9f, WOOD, 0);
                }
                if (ph >= 3) Fx.pool(sl, null, c, 6f, BULB, 100);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 2, c.z, 120, 3, 2, 3, 0);
            }
            case BANG_PHACH -> {
                wings(sl, b, FROST, 80);
                Fx.groundRing(sl, null, c, ph >= 3 ? 12f : 8f, ICE, 40);
                if (ph >= 3) Fx.pool(sl, null, c, 7f, FROST, 100);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 2, c.z, 150, 3, 2, 3, 0.05);
            }
            default -> { }
        }
        b.playSound(SoundEvents.WARDEN_ROAR, 3f, ph >= 3 ? 0.7f : 0.9f);
    }

    // ------------------------------------------------------------------ cutscene (thoại: hukhong.dialog.hvf_<dạng>.*)
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    private static String pre(HuVuongForm b) { return "hukhong.dialog." + b.form().phaseId() + "."; }

    /** Giới thiệu riêng theo dạng (null = mặc định director). */
    public static List<CamPath.Key> introCam(HuVuongForm b, int T) {
        if (!handles(b.form())) return null;
        return switch (b.form()) {
            case COT_LONG -> List.of(k(0, 5.0, 0.3, 7.0, 74, 1.5, -10), k(T * 2 / 5, -5.0, 1.0, 4.0, 60, 2.0, -6),
                    k(T * 3 / 4, 0.4, 2.2, 4.0, 48, 2.0, 0), k(T, 0, 3.0, 8.0, 68, 1.8, 0));
            case BANG_PHACH -> List.of(k(0, 0, 0.3, 6.0, 72, 4.5, -22), k(T * 2 / 5, 3.5, 2.5, 3.5, 58, 3.5, -8),
                    k(T * 3 / 4, -0.6, 3.5, 3.0, 46, 3.4, 0), k(T, 0, 3.4, 7.5, 68, 3.0, 0));
            default -> List.of(k(0, 2.0, 0.4, 5.0, 70, 4.0, -18), k(T * 2 / 5, -4.0, 1.6, 2.5, 58, 3.0, -6),
                    k(T * 3 / 4, 0.6, 3.2, 3.0, 45, 3.2, 0), k(T, 0, 3.0, 7.0, 66, 2.6, 0));
        };
    }

    public static List<SubLine> introSubs(HuVuongForm b, int T) {
        if (!handles(b.form())) return null;
        String p = pre(b);
        return List.of(new SubLine(6, 36, p + "intro.1"), new SubLine(36, 72, p + "intro.2"), new SubLine(72, T - 4, p + "intro.3"));
    }

    static final int ULT_T = 70;

    private static void ultScene(ServerLevel sl, HuVuongForm b) {
        double s = Math.max(1.0, b.director().currentScale());
        String p = pre(b);
        List<CamPath.Key> cam = List.of(k(0, 3.0 * s, 0.5 * s, 4.0 * s, 62, 2.0 * s, -8), k(ULT_T / 2, -2.0 * s, 0.3 * s, 4.5 * s, 70, 4.0 * s, -20),
                k(ULT_T, 0, 1.5 * s, 9.0 * s, 80, 5.0 * s, -15));
        BossPhaseDirector.sendCutscene(sl, b, ULT_T, cam, List.of(new SubLine(2, 24, p + "ult.1"),
                new SubLine(24, 48, p + "ult.2"), new SubLine(48, ULT_T - 2, p + "ult.3")));
    }

    /** Tử trận: cutscene tĩnh tại chỗ ngã + tàn dư theo dạng. */
    public static void deathScene(ServerLevel sl, HuVuongForm b) {
        if (!handles(b.form())) return;
        cancel(b);
        b.movesA.zones.clear();
        int T = 100;
        Vec3 at = b.position();
        String p = pre(b);
        List<CamPath.Key> cam = List.of(k(0, 0, 1.8, 4.5, 55, 1.4, 0), k(50, 3.0, 2.2, 3.0, 60, 1.2, 0), k(T, 0, 5.0, 9.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, b.yBodyRot, T, cam, List.of(new SubLine(6, 36, p + "death.1"),
                new SubLine(36, 68, p + "death.2"), new SubLine(68, T - 4, p + "death.3")));
        int rgb = switch (b.form()) { case HUYET_MA -> BLOOD; case COT_LONG -> BONE; case MOC_YEU -> WOOD; default -> FROST; };
        Fx.groundRing(sl, null, at, 8f, rgb, 60);
        Fx.pool(sl, null, at, 5f, rgb, 140);
        ParticleOptions pt = switch (b.form()) {
            case HUYET_MA -> ParticleTypes.CRIMSON_SPORE; case COT_LONG -> ParticleTypes.WHITE_ASH;
            case MOC_YEU -> ParticleTypes.SPORE_BLOSSOM_AIR; default -> ParticleTypes.SNOWFLAKE;
        };
        sl.sendParticles(pt, at.x, at.y + 2, at.z, 150, 2.5, 2, 2.5, 0.05);
        sl.sendParticles(ParticleTypes.ASH, at.x, at.y + 2, at.z, 80, 2.5, 2, 2.5, 0.05);
    }
}
