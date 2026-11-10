package com.hukhong.chg;

import com.hukhong.HuKhong;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bộ chiêu riêng của Chấp Hành Giả II "Bạch Từ Âm" (kiểu Maliketh: thú tăng nhanh nhẹn -> Hắc Kiếm, chủ đề chuông tử).
 * Mỗi chiêu là dòng thời gian theo tick trong {@link #step}; mọi chiêu có telegraph trước sát thương.
 * Đòn Hắc Kiếm gây "Tử Mệnh" (Destined Death): hồi máu giảm {@link #DOOM_HEAL_MUL} trong vài giây.
 * <ul>
 *   <li>chg2_trao (Thú Trảo Liên Hoàn): 3 nhát vồ nhanh như dao găm.</li>
 *   <li>chg2_nhay (Thú Dược): nhảy vồ xuống vòng cảnh báo.</li>
 *   <li>chg2_nem_da (Phi Thạch): ném tảng đá vào vòng cảnh báo.</li>
 *   <li>chg2_hac_kiem (Hắc Kiếm Hoành Tảo): chém quạt rộng + nhát quét ngược.</li>
 *   <li>chg2_tu_tram (Tử Mệnh Phi Trảm): 3 đợt kiếm khí bay theo vạch.</li>
 *   <li>chg2_ha_khong (Hắc Ưng Giáng): bay lên rồi bổ xuống, sóng bóng lan ra.</li>
 *   <li>chg2_chuong_tu (Tử Chung): chuông tử ngân 3 hồi, vòng sóng quanh thân.</li>
 *   <li>chg2_hac_vu (Hắc Kiếm Thiên Vũ, tối thượng): mưa kiếm bóng từ trời + bổ xuống tâm.</li>
 * </ul>
 */
public final class ChgMaliketh {
    public static final int SHADOW = 0x3A1F4A, DOOM = 0xC0183A, BELL = 0xE8D8A0, BEAST = 0x9A8A6A, DANGER = 0xFF2030;
    public static final List<String> IDS = List.of("chg2_trao", "chg2_nhay", "chg2_nem_da", "chg2_hac_kiem", "chg2_tu_tram",
            "chg2_ha_khong", "chg2_chuong_tu", "chg2_hac_vu");
    public static final String DOOM_KEY = "hk_chg2_doom";
    public static final float DOOM_HEAL_MUL = 0.4f;

    public static boolean handles(String id) { return IDS.contains(id); }

    private final ChapHanhGia b;
    String mv;
    int mt, ultCd = 500;
    Vec3 aim = new Vec3(0, 0, 1), spot, from, to;
    LivingEntity target;
    final Set<UUID> hitOnce = new HashSet<>();
    final List<Vec3> swords = new ArrayList<>();

    ChgMaliketh(ChapHanhGia b) { this.b = b; }

    public String currentMove() { return mv; }
    public boolean busy() { return mv != null; }

    // ------------------------------------------------------------------ vòng đời
    public boolean start(ServerLevel sl, String id, LivingEntity t) {
        if (mv != null || !b.isAlive() || !handles(id)) return false;
        mv = id; mt = 0; target = t; spot = null; from = null; to = null;
        hitOnce.clear(); swords.clear();
        b.getNavigation().stop();
        Vec3 dir = t != null ? t.position().subtract(b.position()).multiply(1, 0, 1) : b.getViewVector(1f).multiply(1, 0, 1);
        aim = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = id.equals("chg2_hac_vu");
        Component name = Component.translatable("hukhong.boss.move." + id);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD) : name, true);
        if (ult) { ultCd = 900; ultScene(sl); }
        return true;
    }

    public void end() {
        if (mv == null) return;
        mv = null; target = null;
        b.setNoGravity(false);
    }

    /** Gọi mỗi tick server; true nếu chiêu vừa kết thúc. */
    boolean tick(ServerLevel sl) {
        if (ultCd > 0) ultCd--;
        if (mv == null) return false;
        b.getNavigation().stop();
        if (!step(sl, mt++)) { end(); return true; }
        return false;
    }

    String pick(RandomSource rnd, int phase, float dist) {
        if (phase >= 3 && ultCd <= 0 && rnd.nextInt(5) == 0) return "chg2_hac_vu";
        if (dist > 10 && rnd.nextBoolean()) return phase >= 2 ? (rnd.nextBoolean() ? "chg2_tu_tram" : "chg2_nem_da") : "chg2_nem_da";
        List<String> pool = phase >= 3 ? List.of("chg2_hac_kiem", "chg2_tu_tram", "chg2_ha_khong", "chg2_chuong_tu", "chg2_trao")
                : phase == 2 ? List.of("chg2_trao", "chg2_nhay", "chg2_hac_kiem", "chg2_tu_tram", "chg2_nem_da")
                : List.of("chg2_trao", "chg2_nhay", "chg2_nem_da");
        return pool.get(rnd.nextInt(pool.size()));
    }

    static double mult(String id) {
        return switch (id) {
            case "chg2_trao" -> 0.6;
            case "chg2_nhay" -> 1.2;
            case "chg2_nem_da" -> 1.0;
            case "chg2_hac_kiem" -> 1.3;
            case "chg2_tu_tram" -> 0.9;
            case "chg2_ha_khong" -> 1.5;
            case "chg2_chuong_tu" -> 1.0;
            case "chg2_hac_vu" -> 0.8;
            default -> 1.0;
        };
    }

    private boolean step(ServerLevel sl, int t) {
        return switch (mv) {
            case "chg2_trao" -> swipes(sl, t);
            case "chg2_nhay" -> leap(sl, t);
            case "chg2_nem_da" -> rock(sl, t);
            case "chg2_hac_kiem" -> blade(sl, t);
            case "chg2_tu_tram" -> flyingSlashes(sl, t);
            case "chg2_ha_khong" -> dive(sl, t);
            case "chg2_chuong_tu" -> bell(sl, t);
            case "chg2_hac_vu" -> volley(sl, t);
            default -> false;
        };
    }

    private Vec3 tpos() {
        if (target != null && target.isAlive()) return target.position();
        return b.position().add(aim.scale(6));
    }

    private void reaim() {
        Vec3 dir = tpos().subtract(b.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) aim = dir.normalize();
        b.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
        b.yBodyRot = b.getYRot(); b.yHeadRot = b.getYRot();
    }

    private void lunge(double v) { b.setDeltaMovement(aim.x * v, 0.1, aim.z * v); b.hurtMarked = true; }

    // ---- Thú Trảo: cảnh báo quạt 12 tick -> 3 nhát vồ (t12/18/24), giữa các nhát xoay theo mục tiêu
    private boolean swipes(ServerLevel sl, int t) {
        if (t == 0) { reaim(); Fx.telegraphCone(sl, null, b.position(), aim, 5f, 60, 12); b.playSound(SoundEvents.WOLF_GROWL, 2.5f, 0.5f); }
        if (t == 12 || t == 18 || t == 24) {
            int n = (t - 12) / 6;
            reaim();
            lunge(0.7);
            b.triggerAnim("skill", n == 1 ? "skill_a" : "attack");
            int rgb = b.phase() >= 2 ? SHADOW : BEAST;
            cone(sl, 5, 60, mult(mv), Hit.NONE, false, 0);
            Fx.qi(sl, b.position().add(0, 1.4, 0), aim.scale(4), 3f, rgb, n == 1 ? -40 : 40, 8);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, b.getX() + aim.x * 2, b.getY() + 1.4, b.getZ() + aim.z * 2, 2, 0.6, 0.3, 0.6, 0);
            sl.sendParticles(ParticleTypes.CRIT, b.getX() + aim.x * 2, b.getY() + 1.4, b.getZ() + aim.z * 2, 12, 0.8, 0.4, 0.8, 0.3);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 1.2f + n * 0.15f);
        }
        return t < 34;
    }

    // ---- Thú Dược: vòng 24 tick tại mục tiêu, bật lên rồi vồ xuống ở t24
    private boolean leap(ServerLevel sl, int t) {
        double r = 4;
        if (t == 0) {
            spot = tpos();
            Fx.telegraphCircle(sl, null, spot, (float) r, 24, DANGER);
            b.setNoGravity(true);
            b.triggerAnim("skill", "skill_c");
            sl.sendParticles(ParticleTypes.CLOUD, b.getX(), b.getY(), b.getZ(), 16, 0.8, 0.1, 0.8, 0.05);
            b.playSound(SoundEvents.RAVAGER_ROAR, 2f, 1.3f);
        }
        if (t < 10) { b.setDeltaMovement(0, 0.6, 0); b.hurtMarked = true; }
        else if (t == 18) { b.teleportTo(spot.x, spot.y + 6, spot.z); b.setDeltaMovement(Vec3.ZERO); }
        else if (t > 18 && t < 24) b.setDeltaMovement(Vec3.ZERO);
        else if (t == 24) {
            b.teleportTo(spot.x, spot.y, spot.z);
            b.setDeltaMovement(Vec3.ZERO);
            b.setNoGravity(false);
            circle(sl, spot, r, mult(mv), Hit.KNOCK_UP, false);
            Fx.groundCrack(sl, null, spot, (float) r, BEAST, 7, 30);
            Fx.shake(sl, spot, 1.5f, 10, 24f);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spot.x, spot.y + 0.3, spot.z, 20, r * 0.4, 0.1, r * 0.4, 0.02);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.9f);
        }
        return t < 32;
    }

    // ---- Phi Thạch: vòng 26 tick, tảng đá bay ở t10, chạm đất t26
    private boolean rock(ServerLevel sl, int t) {
        double r = 3.5;
        if (t == 0) {
            reaim();
            spot = tpos();
            Fx.telegraphCircle(sl, null, spot, (float) r, 26, DANGER);
            b.triggerAnim("skill", "skill_a");
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, b.getX() + aim.x, b.getY() + 0.2, b.getZ() + aim.z, 10, 0.5, 0.1, 0.5, 0.02);
            b.playSound(SoundEvents.STONE_BREAK, 2.5f, 0.5f);
        }
        if (t == 10) {
            Vec3 hand = b.position().add(0, b.getBbHeight() * 0.8, 0);
            Fx.elem(sl, null, Fx.E_BOULDER, hand, spot.subtract(hand), 1.6f, BEAST, 16);
            b.playSound(SoundEvents.WITHER_SHOOT, 2f, 0.5f);
        }
        if (t == 26) {
            circle(sl, spot, r, mult(mv), Hit.SLOW, false);
            Fx.groundCrack(sl, null, spot, (float) r, BEAST, 6, 30);
            sl.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 3, 0.8, 0.3, 0.8, 0);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.7f);
        }
        return t < 34;
    }

    // ---- Hắc Kiếm Hoành Tảo: quạt 8 block/200 độ (22 tick) -> chém; quạt ngược 7 block (10 tick) -> chém
    private boolean blade(ServerLevel sl, int t) {
        if (t == 0) {
            reaim();
            Fx.telegraphCone(sl, null, b.position(), aim, 8f, 100, 22);
            Fx.rift(sl, null, b.position().add(0, 1.5, 0), new Vec3(0, 1.5, 0), 0.6f, SHADOW, 22);
            b.playSound(SoundEvents.WITHER_AMBIENT, 2f, 0.6f);
        }
        if (t == 14) b.triggerAnim("skill", "skill_b");
        if (t == 22) {
            cone(sl, 8, 100, mult(mv), Hit.NONE, true, 100);
            for (int i = -2; i <= 2; i++) Fx.qi(sl, b.position().add(0, 1.3, 0), rot(aim, i * 35).scale(6), 4f, SHADOW, 10, 12);
            Fx.ribbonWave(sl, null, b.position().add(rot(aim, -90).scale(7)).add(0, 1.2, 0), b.position().add(rot(aim, 90).scale(7)).add(0, 1.2, 0), 5, 0.6, 8, 0, 0.8f, DOOM);
            sl.sendParticles(ParticleTypes.SQUID_INK, b.getX() + aim.x * 4, b.getY() + 1, b.getZ() + aim.z * 4, 40, 3, 0.5, 3, 0.05);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.4f);
            reaim();
            Fx.telegraphCone(sl, null, b.position(), aim, 7f, 70, 10);
        }
        if (t == 32) {
            b.triggerAnim("skill", "attack");
            cone(sl, 7, 70, mult(mv) * 0.8, Hit.SLOW, true, 80);
            Fx.qi(sl, b.position().add(0, 1.3, 0), aim.scale(5), 4f, DOOM, -30, 10);
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.6f);
        }
        return t < 42;
    }

    // ---- Tử Mệnh Phi Trảm: 3 đợt, mỗi đợt vạch 18 block (12 tick) -> kiếm khí bay
    static final int FS_CYC = 16, FS_TELE = 12;

    private boolean flyingSlashes(ServerLevel sl, int t) {
        int k = t / FS_CYC, e = t % FS_CYC;
        if (k >= 3) return t < 3 * FS_CYC + 6;
        double len = 18;
        if (e == 0) {
            reaim();
            from = b.position();
            Vec3 dir = rot(aim, (k - 1) * 12);
            to = from.add(dir.scale(len));
            Fx.telegraphLine(sl, null, from.add(0, 0.05, 0), to.subtract(from), 2.4f, FS_TELE, DOOM);
            hitOnce.clear();
            b.playSound(SoundEvents.WITHER_SKELETON_AMBIENT, 2f, 0.5f + k * 0.15f);
        }
        if (e == FS_TELE) {
            b.triggerAnim("skill", k % 2 == 0 ? "skill_b" : "attack");
            Vec3 v = to.subtract(from);
            Fx.rift(sl, null, from.add(0, 1.2, 0), v, 1.4f, SHADOW, 24);
            Fx.qi(sl, from.add(0, 1.2, 0), v, 2.4f, DOOM, k % 2 == 0 ? 80 : -80, 12);
            Vec3 dir = v.normalize();
            for (Player p : players(sl, from, len)) {
                Vec3 rel = p.position().subtract(from).multiply(1, 0, 1);
                double along = rel.dot(dir);
                if (along < 0 || along > len || rel.subtract(dir.scale(along)).length() > 1.4) continue;
                if (hitOnce.add(p.getUUID()) && strike(p, mult(mv))) { PhaseMoves.applyHit(p, Hit.NONE); doom(sl, p, 100); }
            }
            b.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5f, 0.5f);
        }
        return true;
    }

    // ---- Hắc Ưng Giáng: bay lên (12) -> vòng 5 block 22 tick -> bổ xuống t34 -> sóng bóng 5..9 ở t42
    private boolean dive(ServerLevel sl, int t) {
        double r = 5, outer = 9;
        if (t == 0) {
            b.setNoGravity(true);
            b.triggerAnim("skill", "skill_c");
            Fx.rune(sl, null, b.position().add(0, 0.1, 0), 3f, SHADOW, 20, 6);
            b.playSound(SoundEvents.PHANTOM_FLAP, 2.5f, 0.5f);
        }
        if (t < 12) { b.setDeltaMovement(0, 0.7, 0); b.hurtMarked = true; return true; }
        if (t == 12) {
            spot = tpos();
            b.setDeltaMovement(Vec3.ZERO);
            Fx.telegraphCircle(sl, null, spot, (float) r, 22, DANGER);
            Fx.telegraphCircle(sl, null, spot, (float) outer, 30, DOOM);
        }
        if (t > 12 && t < 34) {
            b.setDeltaMovement(Vec3.ZERO);
            if (t == 28) b.teleportTo(spot.x, spot.y + 9, spot.z);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.SQUID_INK, b.getX(), b.getY() + 1, b.getZ(), 6, 0.6, 0.6, 0.6, 0.02);
            return true;
        }
        if (t == 34) {
            Fx.ribbon(sl, null, b.position(), spot, 0, 3, 0, 0.8f, SHADOW);
            b.teleportTo(spot.x, spot.y, spot.z);
            b.setDeltaMovement(Vec3.ZERO);
            b.setNoGravity(false);
            circle(sl, spot, r, mult(mv), Hit.KNOCK_UP, true);
            Fx.groundCrack(sl, null, spot, (float) r, SHADOW, 8, 40);
            Fx.shake(sl, spot, 2.5f, 14, 32f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.x, spot.y + 0.5, spot.z, 1, 0, 0, 0, 0);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
        }
        if (t == 42) {
            for (Player p : players(sl, spot, outer)) {
                double d2 = flat2(p.position(), spot);
                if (d2 < r * r || d2 > outer * outer) continue;
                if (strike(p, mult(mv) * 0.5)) doom(sl, p, 80);
            }
            Fx.groundRing(sl, null, spot, (float) outer, DOOM, 20);
            sl.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.4, spot.z, 40, outer * 0.5, 0.2, outer * 0.5, 0.02);
        }
        return t < 48;
    }

    // ---- Tử Chung: chuông ngân 3 hồi (t0/10/20), vòng 9 block nổ ở t30
    private boolean bell(ServerLevel sl, int t) {
        double r = 9;
        if (t == 0) {
            Fx.telegraphCircle(sl, null, b.position(), (float) r, 30, BELL);
            Fx.rune(sl, null, b.position().add(0, 0.1, 0), 4f, BELL, 34, 8);
            b.triggerAnim("skill", "skill_b");
        }
        if (t == 0 || t == 10 || t == 20) {
            b.playSound(SoundEvents.BELL_BLOCK, 4f, 0.5f + t * 0.01f);
            Fx.groundRing(sl, null, b.position(), (float) (3 + t * 0.2), BELL, 12);
            Fx.elem(sl, null, Fx.E_HALOS, b.position().add(0, b.getBbHeight() + 0.5, 0), Vec3.ZERO, 1.5f, BELL, 14);
        }
        if (t == 30) {
            circle(sl, b.position(), r, mult(mv), Hit.SLOW, true);
            for (int i = 0; i < 3; i++) Fx.groundRing(sl, null, b.position(), (float) (r * (0.5 + i * 0.25)), i == 2 ? DOOM : BELL, 16 + i * 6);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, b.getX(), b.getY() + 1.5, b.getZ(), 1, 0, 0, 0, 0);
            Fx.shake(sl, b.position(), 1.5f, 12, 24f);
            b.playSound(SoundEvents.BELL_RESONATE, 4f, 0.5f);
        }
        return t < 40;
    }

    // ---- Hắc Kiếm Thiên Vũ (tối thượng): bay lên (24) -> vòng 16 block + 14 kiếm bóng rơi lần lượt (t40..) -> bổ xuống tâm t90
    public static final int HV_RISE = 24, HV_FALL0 = 40, HV_SWORDS = 14, HV_DIVE = 90, HV_END = 104;
    public static final double HV_RAD = 16;

    private boolean volley(ServerLevel sl, int t) {
        if (t == 0) {
            b.setNoGravity(true);
            b.triggerAnim("skill", "phase");
            Fx.rift(sl, null, b.position().add(0, 1, 0), new Vec3(0, 3, 0), 1.5f, SHADOW, 40);
            b.playSound(SoundEvents.BELL_BLOCK, 4f, 0.4f);
            b.playSound(SoundEvents.WITHER_SPAWN, 2f, 0.7f);
        }
        if (t < HV_RISE) {
            b.setDeltaMovement(0, 0.35, 0);
            b.hurtMarked = true;
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.SQUID_INK, b.getX(), b.getY() + 1, b.getZ(), 8, 1.2, 1, 1.2, 0.02);
            return true;
        }
        if (t == HV_RISE) {
            b.setDeltaMovement(Vec3.ZERO);
            spot = tpos();
            Fx.telegraphCircle(sl, null, spot, (float) HV_RAD, HV_DIVE - HV_RISE, DANGER);
            Fx.rune(sl, null, spot.add(0, 0.1, 0), (float) HV_RAD * 0.6f, SHADOW, HV_DIVE - HV_RISE, 8);
            RandomSource rnd = b.getRandom();
            for (int i = 0; i < HV_SWORDS; i++) {
                Vec3 c;
                if (i % 4 == 0) c = tpos();  // mỗi kiếm thứ 4 nhắm thẳng mục tiêu
                else { double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * HV_RAD; c = spot.add(Math.cos(a) * d, 0, Math.sin(a) * d); }
                swords.add(c);
            }
        }
        if (t > HV_RISE && t < HV_DIVE) {
            b.setDeltaMovement(Vec3.ZERO);
            for (int i = 0; i < swords.size(); i++) {
                int fall = HV_FALL0 + i * 3;
                Vec3 c = swords.get(i);
                if (t == fall - 14) {  // cảnh báo riêng từng kiếm + kiếm hiện trên trời
                    Fx.telegraphCircle(sl, null, c, 2.5f, 14, DOOM);
                    Fx.giantSword(sl, null, c, 2.2f, SHADOW, 14, 30);
                }
                if (t == fall) {
                    circle(sl, c, 2.5, mult(mv), Hit.NONE, true);
                    sl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 0.4, c.z, 14, 0.8, 0.2, 0.8, 0.05);
                    b.playSound(SoundEvents.TRIDENT_HIT_GROUND, 2f, 0.5f);
                }
            }
            if (t == HV_DIVE - 6) { b.teleportTo(spot.x, spot.y + 10, spot.z); b.triggerAnim("skill", "skill_c"); }
            return true;
        }
        if (t == HV_DIVE) {
            Fx.ribbon(sl, null, b.position(), spot, 0, 3, 0, 1.2f, SHADOW);
            b.teleportTo(spot.x, spot.y, spot.z);
            b.setDeltaMovement(Vec3.ZERO);
            for (Player p : players(sl, spot, HV_RAD))
                if (flat2(p.position(), spot) <= HV_RAD * HV_RAD && strike(p, 1.8)) { PhaseMoves.applyHit(p, Hit.KNOCK_UP); doom(sl, p, 200); }
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                Fx.ribbonWave(sl, null, spot.add(0, 0.6, 0), spot.add(Math.cos(a) * HV_RAD, 0.4, Math.sin(a) * HV_RAD), 2.0, 1.0, 14, 0, 0.9f, SHADOW);
            }
            Fx.groundCrack(sl, null, spot, (float) HV_RAD, DOOM, 16, 60);
            Fx.groundRing(sl, null, spot, (float) HV_RAD, SHADOW, 30);
            Fx.elem(sl, null, Fx.E_NUKE, spot, Vec3.ZERO, (float) HV_RAD * 0.4f, SHADOW, 0);
            Fx.shake(sl, spot, 4f, 30, 60f);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.x, spot.y + 1, spot.z, 2, 1, 0.2, 1, 0);
            b.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.5f);
            b.playSound(SoundEvents.BELL_RESONATE, 4f, 0.4f);
        }
        if (t == HV_DIVE + 4) b.setNoGravity(false);
        return t < HV_END;
    }

    // ------------------------------------------------------------------ trợ giúp sát thương
    private boolean strike(LivingEntity p, double mul) {
        return p.hurt(b.damageSources().mobAttack(b), b.moveDamage(mul));
    }

    private void cone(ServerLevel sl, double rad, double half, double mul, Hit h, boolean doom, int doomTicks) {
        double cos = Math.cos(Math.toRadians(half));
        Vec3 c = b.position();
        for (Player p : players(sl, c, rad)) {
            Vec3 rel = p.position().subtract(c).multiply(1, 0, 1);
            double dist = rel.length();
            if (dist > rad || (dist > 1.2 && rel.normalize().dot(aim) < cos)) continue;
            if (strike(p, mul)) { PhaseMoves.applyHit(p, h); if (doom) doom(sl, p, doomTicks); }
        }
    }

    private void circle(ServerLevel sl, Vec3 c, double r, double mul, Hit h, boolean doom) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (strike(p, mul)) { PhaseMoves.applyHit(p, h); if (doom) doom(sl, p, 120); }
        }
    }

    private static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    private static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static Vec3 rot(Vec3 v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(v.x * c - v.z * s, 0, v.x * s + v.z * c);
    }

    // ------------------------------------------------------------------ Tử Mệnh (Destined Death)
    /** Đánh dấu Tử Mệnh: hồi máu x{@link #DOOM_HEAL_MUL} tới hết ticks. */
    public static void doom(ServerLevel sl, LivingEntity p, int ticks) {
        long until = sl.getGameTime() + ticks;
        p.getPersistentData().putLong(DOOM_KEY, Math.max(until, p.getPersistentData().getLong(DOOM_KEY)));
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 0));
        sl.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 8, 0.3, 0.6, 0.3, 0.02);
        if (p instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable("hukhong.chg2.doom").withStyle(ChatFormatting.DARK_RED), true);
    }

    public static boolean doomed(LivingEntity p) {
        return p.getPersistentData().getLong(DOOM_KEY) > p.level().getGameTime();
    }

    @EventBusSubscriber(modid = HuKhong.MODID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onHeal(LivingHealEvent e) {
            if (!e.getEntity().level().isClientSide && doomed(e.getEntity())) e.setAmount(e.getAmount() * DOOM_HEAL_MUL);
        }
    }

    // ------------------------------------------------------------------ VFX pha + cutscene
    /** Lộ diện pha: P2 rút Hắc Kiếm (vết rách bóng), P3 chuông tử ngân. */
    void transformFx(ServerLevel sl, int ph) {
        Vec3 c = b.position();
        if (ph == 2) {
            Fx.rift(sl, null, c.add(0, 0.5, 0), new Vec3(0, 4, 0), 1.2f, SHADOW, 60);
            Fx.ribbonHelix(sl, null, c, c.add(0, 5, 0), 1.5, 0, 30, 0, 0.5f, DOOM);
            sl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 2, c.z, 80, 1.5, 2, 1.5, 0.05);
            b.playSound(SoundEvents.WITHER_SPAWN, 2f, 0.8f);
        } else if (ph >= 3) {
            for (int i = 0; i < 3; i++) Fx.groundRing(sl, null, c, 6f + i * 4f, i == 1 ? DOOM : BELL, 30 + i * 10);
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 3, 0), Vec3.ZERO, 3f, BELL, 0);
            b.playSound(SoundEvents.BELL_BLOCK, 5f, 0.4f);
            b.playSound(SoundEvents.BELL_RESONATE, 5f, 0.5f);
        }
    }

    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: dưới chân nhìn lên tháp chuông -> vòng ra trước -> cận mặt. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 2.0, 0.3, 6.0, 72, 4.0, -18), k(T * 2 / 5, -3.5, 1.0, 3.5, 60, 2.6, -4),
                k(T * 3 / 4, -0.6, 2.6, 2.4, 46, 3.0, 0), k(T, 0, 2.8, 6.5, 66, 2.4, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.chg_2"), new SubLine(36, 72, "hukhong.dialog.chg2.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.chg2.intro.2"));
    }

    static final int ULT_T = 60;

    private void ultScene(ServerLevel sl) {
        double s = Math.max(1.0, b.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 2.5 * s, 0.4 * s, 3.5 * s, 62, 1.5 * s, -6), k(ULT_T / 2, -1.5 * s, 0.2 * s, 4.0 * s, 70, 4.0 * s, -20),
                k(ULT_T, 0, 1.0 * s, 8.0 * s, 80, 6.0 * s, -18));
        BossPhaseDirector.sendCutscene(sl, b, ULT_T, cam, List.of(new SubLine(2, 28, "hukhong.dialog.chg2.ult.1"),
                new SubLine(28, ULT_T - 2, "hukhong.dialog.chg2.ult.2")));
    }

    void deathScene(ServerLevel sl) {
        int T = 90;
        Vec3 at = b.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.6, 4.0, 55, 1.2, 0), k(45, 2.5, 2.0, 3.0, 60, 1.0, 0), k(T, 0, 4.5, 8.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, b.yBodyRot, T, cam, List.of(new SubLine(6, 46, "hukhong.dialog.chg2.death.1"),
                new SubLine(46, T - 4, "hukhong.dialog.chg2.death.2")));
        Fx.groundRing(sl, null, at, 8f, BELL, 60);
        Fx.rift(sl, null, at.add(0, 0.5, 0), new Vec3(0, 3, 0), 1.0f, SHADOW, 60);
        sl.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.5, at.z, 60, 1.5, 1.5, 1.5, 0.03);
        b.playSound(SoundEvents.BELL_RESONATE, 4f, 0.4f);
    }
}
