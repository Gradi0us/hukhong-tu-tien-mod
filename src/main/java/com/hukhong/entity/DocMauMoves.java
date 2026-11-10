package com.hukhong.entity;

import com.hukhong.boss.phase.BossPhaseConfig.Hit;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.PhaseMoves;
import com.hukhong.vfx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Bộ chiêu riêng của Độc Mẫu (mẹ độc, đàn con): lấy cảm hứng từ hủ thối đỏ của Malenia (hoa hủ, bướm hủ, vũng hủ đọng)
 * và huyết tế của Mohg (hiến tế, ấn chú) — nhưng sắc lục/tím độc, trọng tâm là bầy đàn. Mọi chiêu đều có cảnh báo.
 * <ul>
 *   <li>dm_phun_doc: 3 loạt phun độc xoè quạt (vạch cảnh báo trước mỗi loạt).</li>
 *   <li>dm_diep_hu: đàn bướm hủ bay đuổi theo người chơi, chạm thì nổ độc.</li>
 *   <li>dm_au_trung: đẻ ấu trùng — vài Hư Trùng yếu (tự biến mất khi Độc Mẫu chết).</li>
 *   <li>dm_hoa_hu: hoa hủ nở dưới chân người chơi 2 đợt, để lại vũng hủ.</li>
 *   <li>dm_van_doc_khai: Vạn Độc Khai Hoa — tối thượng (cutscene), phủ độc cả đấu trường trừ 3 vòng an toàn.</li>
 *   <li>dm_huyet_te: hiến tế ấu trùng — mỗi con nổ độc + hồi máu mẹ; không có con thì ấn chú dưới chân người chơi.</li>
 *   <li>dm_an_chu: chuỗi ấn chú tiến về mục tiêu, nổ nối tiếp.</li>
 *   <li>dm_trieu_doc: 2 vòng triều độc lan ra (nhảy qua được).</li>
 * </ul>
 */
public final class DocMauMoves {
    private DocMauMoves() {}

    public static final int ROT = 0x6FCF3A, VIOLET = 0x9B3FD0, BILE = 0xC8F050, DARK = 0x3A1050, DANGER = 0xB040FF, SAFE = 0xE8FFF0;
    public static final String BROOD_TAG = "hk_docmau_brood";
    public static final int BROOD_MAX = 6;
    public static final List<String> IDS = List.of("dm_phun_doc", "dm_diep_hu", "dm_au_trung", "dm_hoa_hu",
            "dm_van_doc_khai", "dm_huyet_te", "dm_an_chu", "dm_trieu_doc");

    private static final DustParticleOptions D_ROT = new DustParticleOptions(new Vector3f(0.44f, 0.81f, 0.23f), 1.4f);
    private static final DustParticleOptions D_VIOLET = new DustParticleOptions(new Vector3f(0.61f, 0.25f, 0.82f), 1.2f);

    public static boolean handles(String id) { return IDS.contains(id); }

    /** Vũng hủ đọng. */
    static final class Pool {
        final Vec3 c; final double r; int left; final float dmg;
        Pool(Vec3 c, double r, int left, float dmg) { this.c = c; this.r = r; this.left = left; this.dmg = dmg; }
    }

    /** Bướm hủ: bay đuổi theo người chơi gần nhất. */
    static final class Moth {
        Vec3 pos, vel; int life; final float dmg; final boolean violet;
        Moth(Vec3 pos, Vec3 vel, int life, float dmg, boolean violet) { this.pos = pos; this.vel = vel; this.life = life; this.dmg = dmg; this.violet = violet; }
    }

    static double mult(String id) {
        return switch (id) {
            case "dm_phun_doc" -> 0.7;
            case "dm_diep_hu" -> 0.45;
            case "dm_au_trung" -> 0.6;
            case "dm_hoa_hu" -> 1.0;
            case "dm_van_doc_khai" -> 2.2;
            case "dm_huyet_te" -> 1.1;
            case "dm_an_chu" -> 1.0;
            case "dm_trieu_doc" -> 0.9;
            default -> 0.8;
        };
    }

    // ------------------------------------------------------------------ bắt đầu
    static void begin(ServerLevel sl, DocMau d) {
        LivingEntity t = d.mTarget;
        Vec3 dir = t != null ? t.position().subtract(d.position()).multiply(1, 0, 1) : d.getViewVector(1f).multiply(1, 0, 1);
        d.aimDir = dir.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : dir.normalize();
        boolean ult = d.mv.equals("dm_van_doc_khai");
        Component name = Component.translatable("hukhong.boss.move." + d.mv);
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(d) < 48 * 48) p.displayClientMessage(ult ? name.copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD) : name.copy().withStyle(ChatFormatting.GREEN), true);
        d.playSound(SoundEvents.SLIME_SQUISH, 2.5f, ult ? 0.3f : 0.6f);
        if (ult) ultScene(sl, d);
    }

    private static Vec3 tpos(DocMau d) {
        LivingEntity t = d.mTarget;
        if (t != null && t.isAlive()) return t.position();
        return d.position().add(d.aimDir.scale(6));
    }

    private static void reaim(DocMau d) {
        Vec3 dir = tpos(d).subtract(d.position()).multiply(1, 0, 1);
        if (dir.lengthSqr() > 1e-3) d.aimDir = dir.normalize();
        d.setYRot((float) Math.toDegrees(Math.atan2(-d.aimDir.x, d.aimDir.z)));
        d.yBodyRot = d.getYRot(); d.yHeadRot = d.getYRot();
    }

    /** Mỗi tick; false = chiêu xong. */
    static boolean step(ServerLevel sl, DocMau d, int t) {
        d.getNavigation().stop();
        return switch (d.mv) {
            case "dm_phun_doc" -> spit(sl, d, t);
            case "dm_diep_hu" -> moths(sl, d, t);
            case "dm_au_trung" -> brood(sl, d, t);
            case "dm_hoa_hu" -> bloom(sl, d, t);
            case "dm_van_doc_khai" -> ultimate(sl, d, t);
            case "dm_huyet_te" -> offering(sl, d, t);
            case "dm_an_chu" -> sigils(sl, d, t);
            case "dm_trieu_doc" -> tide(sl, d, t);
            default -> false;
        };
    }

    // ---- Phun Độc: 3 loạt quạt 5 viên; mỗi loạt có vạch cảnh báo 12 tick
    private static boolean spit(ServerLevel sl, DocMau d, int t) {
        if (t == 0 || t == 16 || t == 32) {
            reaim(d);
            d.triggerAnim("skill", "skill_a");
            double base = Math.atan2(d.aimDir.z, d.aimDir.x);
            for (int i = -2; i <= 2; i++) {
                double a = base + Math.toRadians(i * 14 + (t == 16 ? 7 : 0));
                Fx.telegraphLine(sl, null, d.position().add(0, 0.05, 0), new Vec3(Math.cos(a), 0, Math.sin(a)).scale(14), 1.2f, 12, ROT);
            }
            d.playSound(SoundEvents.LLAMA_SPIT, 2.5f, 0.5f);
        }
        if (t == 12 || t == 28 || t == 44) {
            Vec3 from = d.position().add(0, d.getBbHeight() * 0.7, 0);
            double base = Math.atan2(d.aimDir.z, d.aimDir.x);
            for (int i = -2; i <= 2; i++) {
                double a = base + Math.toRadians(i * 14 + (t == 28 ? 7 : 0));
                PoisonSpit.launchDir(d, from, new Vec3(Math.cos(a), 0.12, Math.sin(a)), d.moveDamage(mult(d.mv)), 2.0, 1.3f);
            }
            Fx.qi(sl, from, d.aimDir.scale(3), 1.6f, BILE, 0, 8);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, from.x, from.y, from.z, 20, 0.5, 0.3, 0.5, 0.1);
            d.playSound(SoundEvents.LLAMA_SPIT, 3f, 0.7f);
        }
        return t < 52;
    }

    // ---- Điệp Hủ: bướm hủ bung từ lưng (t10..30), bay đuổi người chơi độc lập với chiêu
    private static boolean moths(ServerLevel sl, DocMau d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "skill_b");
            Fx.rune(sl, null, d.position().add(0, 0.1, 0), 4f, VIOLET, 40, 6);
            Fx.groundRing(sl, null, d.position(), 5f, ROT, 30);
            d.playSound(SoundEvents.BEEHIVE_WORK, 3f, 0.5f);
        }
        int n = d.currentPhase() >= 3 ? 14 : 10;
        if (t >= 10 && t < 30 && t % 2 == 0) {
            Vec3 back = d.position().add(0, d.getBbHeight() * 0.8, 0);
            for (int k = 0; k < n / 10 + 1 && d.moths.size() < 40; k++) {
                double a = d.getRandom().nextDouble() * Math.PI * 2;
                Vec3 v = new Vec3(Math.cos(a) * 0.25, 0.3 + d.getRandom().nextDouble() * 0.2, Math.sin(a) * 0.25);
                d.moths.add(new Moth(back, v, 120, d.moveDamage(mult(d.mv)), (t / 2) % 2 == 0));
            }
            Fx.ribbon(sl, null, back, back.add(d.getRandom().nextGaussian() * 3, 2.5, d.getRandom().nextGaussian() * 3), 1.0, 8, 0, 0.2f, VIOLET);
        }
        return t < 40;
    }

    /** Bướm hủ: bẻ lái về người chơi gần nhất, chạm thì nổ độc. */
    static void tickMoths(ServerLevel sl, DocMau d) {
        for (Iterator<Moth> it = d.moths.iterator(); it.hasNext(); ) {
            Moth m = it.next();
            if (--m.life <= 0) { sl.sendParticles(D_VIOLET, m.pos.x, m.pos.y, m.pos.z, 4, 0.2, 0.2, 0.2, 0); it.remove(); continue; }
            Player tgt = null; double best = 24 * 24;
            for (Player p : players(sl, m.pos, 24)) {
                double ds = p.position().add(0, 1, 0).distanceToSqr(m.pos);
                if (ds < best) { best = ds; tgt = p; }
            }
            if (tgt != null && m.life < 110) {
                Vec3 want = tgt.position().add(0, 1.1, 0).subtract(m.pos).normalize().scale(0.42);
                m.vel = m.vel.scale(0.85).add(want.scale(0.15));
            } else m.vel = m.vel.scale(0.95);
            double flutter = Math.sin((m.life + m.pos.x) * 0.9) * 0.06;
            m.pos = m.pos.add(m.vel).add(0, flutter, 0);
            sl.sendParticles(m.violet ? D_VIOLET : D_ROT, m.pos.x, m.pos.y, m.pos.z, 2, 0.12, 0.05, 0.12, 0);
            if (m.life % 4 == 0) sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, m.pos.x, m.pos.y, m.pos.z, 1, 0, 0, 0, 0);
            if (tgt != null && best < 1.3 * 1.3) {
                if (tgt.hurt(d.damageSources().indirectMagic(d, d), m.dmg)) tgt.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                sl.sendParticles(ParticleTypes.ITEM_SLIME, m.pos.x, m.pos.y, m.pos.z, 10, 0.3, 0.3, 0.3, 0.05);
                sl.sendParticles(D_VIOLET, m.pos.x, m.pos.y, m.pos.z, 12, 0.4, 0.4, 0.4, 0);
                it.remove();
            }
        }
    }

    // ---- Đẻ Ấu Trùng: vòng ổ trứng cảnh báo -> nở 2-3 Hư Trùng (giới hạn BROOD_MAX)
    private static boolean brood(ServerLevel sl, DocMau d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "skill_b");
            d.spots.clear();
            int n = d.currentPhase() >= 3 ? 3 : 2;
            for (int i = 0; i < n; i++) {
                double a = d.getRandom().nextDouble() * Math.PI * 2;
                Vec3 s = d.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4);
                d.spots.add(s);
                Fx.telegraphCircle(sl, null, s, 1.6f, 24, ROT);
                Fx.pool(sl, null, s, 1.4f, VIOLET, 40);
            }
            d.playSound(SoundEvents.TURTLE_LAY_EGG, 3f, 0.5f);
        }
        if (t < 24 && t % 4 == 0) for (Vec3 s : d.spots) sl.sendParticles(ParticleTypes.ITEM_SLIME, s.x, s.y + 0.3, s.z, 3, 0.4, 0.1, 0.4, 0);
        if (t == 24) {
            for (Vec3 s : d.spots) {
                hitAround(sl, d, s, 1.6, mult(d.mv), Hit.POISON);
                spawnBrood(sl, d, s, d.mTarget);
                Fx.elem(sl, null, Fx.E_SPIKES, s, new Vec3(0, 1, 0), 0.6f, ROT, 0);
            }
            d.playSound(SoundEvents.TURTLE_EGG_HATCH, 3f, 0.6f);
        }
        return t < 34;
    }

    /** Sinh một ấu trùng (Hư Trùng gắn tag) nếu chưa đủ đàn. */
    static void spawnBrood(ServerLevel sl, DocMau d, Vec3 at, LivingEntity tgt) {
        d.brood.removeIf(id -> { Entity e = sl.getEntity(id); return e == null || !e.isAlive(); });
        if (d.brood.size() >= BROOD_MAX) return;
        HuTrung c = V3Entities.HU_TRUNG.get().create(sl);
        if (c == null) return;
        c.moveTo(at.x, at.y, at.z, d.getRandom().nextFloat() * 360f, 0f);
        c.finalizeSpawn(sl, sl.getCurrentDifficultyAt(c.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        c.addTag(BROOD_TAG);
        if (tgt != null) c.setTarget(tgt);
        if (sl.addFreshEntity(c)) d.brood.add(c.getUUID());
    }

    static List<HuTrung> liveBrood(ServerLevel sl, DocMau d) {
        List<HuTrung> out = new ArrayList<>();
        for (UUID id : d.brood) if (sl.getEntity(id) instanceof HuTrung h && h.isAlive()) out.add(h);
        return out;
    }

    /** Đàn con tan biến khi mẹ chết. */
    static void despawnBrood(ServerLevel sl, DocMau d) {
        for (HuTrung h : liveBrood(sl, d)) {
            sl.sendParticles(D_ROT, h.getX(), h.getY() + 0.4, h.getZ(), 10, 0.3, 0.3, 0.3, 0);
            h.discard();
        }
        d.brood.clear();
        for (HuTrung h : sl.getEntitiesOfClass(HuTrung.class, d.getBoundingBox().inflate(48), e -> e.getTags().contains(BROOD_TAG))) h.discard();
    }

    // ---- Hoa Hủ: vòng dưới chân từng người chơi -> nở hoa độc + vũng hủ; 2 đợt
    private static boolean bloom(ServerLevel sl, DocMau d, int t) {
        if (t == 0 || t == 26) {
            d.spots.clear();
            for (Player p : players(sl, d.position(), 24)) d.spots.add(p.position());
            if (d.spots.isEmpty()) d.spots.add(tpos(d));
            for (Vec3 s : d.spots) { Fx.telegraphCircle(sl, null, s, 3f, 22, DANGER); Fx.rune(sl, null, s.add(0, 0.05, 0), 2f, ROT, 22, 5); }
            d.triggerAnim("skill", "skill_c");
            d.playSound(SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 3f, 0.5f);
        }
        if (t == 22 || t == 48) {
            for (Vec3 s : d.spots) {
                hitAround(sl, d, s, 3, mult(d.mv), Hit.POISON);
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3 + t;
                    Fx.ribbon(sl, null, s.add(0, 0.2, 0), s.add(Math.cos(a) * 2.6, 1.6, Math.sin(a) * 2.6), 1.2, 8, 0, 0.35f, i % 2 == 0 ? ROT : VIOLET);
                }
                Fx.elem(sl, null, Fx.E_TONGUES, s, Vec3.ZERO, 1.0f, ROT, 0);
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, s.x, s.y + 1, s.z, 30, 1.2, 1, 1.2, 0);
                addPool(sl, d, s, 2.4, 80, 0.15);
            }
            d.playSound(SoundEvents.AZALEA_LEAVES_BREAK, 3f, 0.4f);
        }
        return t < 56;
    }

    // ---- Vạn Độc Khai Hoa: lên cao, vòng 20 + 3 vòng an toàn -> t40 cảnh báo dày -> t100 nở hoa độc khắp đấu trường
    /** Cutscene dài ULT_T (60) tick; nở hoa ở ULT_BLOOM => người chơi có ULT_FREE = 70 tick (3.5 giây) tự do chạy vào vòng an toàn. */
    public static final int ULT_BLOOM = 130, ULT_END = 150, ULT_FREE = ULT_BLOOM - 60;
    public static final double ULT_RAD = 20, SAFE_RAD = 3.0;

    private static boolean ultimate(ServerLevel sl, DocMau d, int t) {
        Vec3 c = d.ultCenter != null ? d.ultCenter : d.position();
        if (t == 0) {
            d.ultCenter = c = d.position();
            d.safe.clear();
            double off = d.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < 3; i++) {
                double a = off + i * Math.PI * 2 / 3, r = 9 + d.getRandom().nextDouble() * 5;
                d.safe.add(c.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
            d.triggerAnim("skill", "phase");
            d.setNoGravity(true);
            Fx.telegraphCircle(sl, null, c, (float) ULT_RAD, ULT_BLOOM, DANGER);
            d.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.4f);
        }
        if (t < 20) { d.setDeltaMovement(0, 0.15, 0); d.hurtMarked = true; } else d.setDeltaMovement(Vec3.ZERO);
        if (t == 0 || t == 60 || t == 100) for (Vec3 s : d.safe) {
            Fx.telegraphCircle(sl, null, s, (float) SAFE_RAD, ULT_BLOOM - t, SAFE);
            Fx.rune(sl, null, s.add(0, 0.1, 0), (float) SAFE_RAD, SAFE, ULT_BLOOM - t, 6);
        }
        if (t % 5 == 0 && t < ULT_BLOOM) {
            for (Vec3 s : d.safe) sl.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 0.3, s.z, 3, 0.8, 0.6, 0.8, 0.01);
            for (int i = 0; i < 6; i++) {
                double a = d.getRandom().nextDouble() * Math.PI * 2, r = d.getRandom().nextDouble() * ULT_RAD;
                sl.sendParticles(D_ROT, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 2, 0.2, 0.1, 0.2, 0);
            }
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, d.getX(), d.getY() + 2, d.getZ(), 10, 2, 1.5, 2, 0);
        }
        if (t == 60) {
            Fx.groundRing(sl, null, c, (float) ULT_RAD, VIOLET, ULT_BLOOM - 60);
            for (int i = 0; i < 10; i++) {
                double a = i * Math.PI / 5;
                Fx.ribbonWave(sl, null, d.position().add(0, 2, 0), c.add(Math.cos(a) * ULT_RAD, 0.2, Math.sin(a) * ULT_RAD), 2.0, 0.8, 16, i, 0.35f, i % 2 == 0 ? ROT : VIOLET);
            }
            d.playSound(SoundEvents.BEACON_POWER_SELECT, 3f, 0.4f);
        }
        if (t == ULT_BLOOM) {
            for (Player p : players(sl, c, ULT_RAD)) {
                if (flat2(p.position(), c) > ULT_RAD * ULT_RAD || inSafe(d, p.position())) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult(d.mv)))) {
                    PhaseMoves.applyHit(p, Hit.POISON);
                    p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                }
            }
            for (int i = 0; i < 14; i++) {
                double a = i * Math.PI * 2 / 14, r = 4 + (i % 3) * 5;
                Vec3 s = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                if (inSafe(d, s)) continue;
                Fx.elem(sl, null, Fx.E_TONGUES, s, Vec3.ZERO, 1.6f, i % 2 == 0 ? ROT : VIOLET, 0);
            }
            Fx.elem(sl, null, Fx.E_NUKE, c, Vec3.ZERO, 3f, ROT, 0);
            Fx.groundCrack(sl, null, c, (float) ULT_RAD, VIOLET, 12, 50);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 2, c.z, 200, ULT_RAD * 0.6, 2, ULT_RAD * 0.6, 0);
            Fx.shake(sl, c, 3f, 20, 48f);
            d.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4f, 0.4f);
            for (int i = 0; i < 4; i++) {
                double a = d.getRandom().nextDouble() * Math.PI * 2, r = 5 + d.getRandom().nextDouble() * 12;
                Vec3 s = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                if (!inSafe(d, s)) addPool(sl, d, s, 3, 120, 0.2);
            }
        }
        if (t == ULT_END - 6) d.setNoGravity(false);
        return t < ULT_END;
    }

    static boolean inSafe(DocMau d, Vec3 p) {
        for (Vec3 s : d.safe) if (flat2(p, s) <= SAFE_RAD * SAFE_RAD) return true;
        return false;
    }

    // ---- Huyết Tế: dây máu-độc nối ấu trùng -> mỗi con nổ (r 3.5) + mẹ hồi máu; không có con thì ấn dưới chân người chơi
    private static boolean offering(ServerLevel sl, DocMau d, int t) {
        if (t == 0) {
            d.triggerAnim("skill", "phase");
            d.spots.clear();
            d.offered.clear();
            for (HuTrung h : liveBrood(sl, d)) { d.offered.add(h.getUUID()); d.spots.add(h.position()); }
            if (d.offered.isEmpty()) for (Player p : players(sl, d.position(), 24)) d.spots.add(p.position());
            if (d.spots.isEmpty()) d.spots.add(tpos(d));
            for (Vec3 s : d.spots) {
                Fx.telegraphCircle(sl, null, s, 3.5f, 30, DANGER);
                Fx.rune(sl, null, s.add(0, 0.05, 0), 2.5f, VIOLET, 30, 7);
                Fx.ribbonWave(sl, null, d.position().add(0, 2, 0), s.add(0, 0.6, 0), 1.2, 0.8, 14, 0, 0.25f, Fx.RED);
            }
            Fx.elem(sl, null, Fx.E_HALOS, d.position().add(0, 2.5, 0), Vec3.ZERO, 2.5f, VIOLET, 0);
            d.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3f, 0.7f);
        }
        if (t < 30 && t % 3 == 0) for (Vec3 s : d.spots) sl.sendParticles(ParticleTypes.CRIMSON_SPORE, s.x, s.y + 0.6, s.z, 4, 0.4, 0.4, 0.4, 0.01);
        if (t == 30) {
            int fed = 0;
            for (UUID id : d.offered) if (sl.getEntity(id) instanceof HuTrung h && h.isAlive()) { h.discard(); fed++; }
            d.brood.removeAll(d.offered);
            Vec3 core = d.position().add(0, d.getBbHeight() * 0.6, 0);
            for (Vec3 s : d.spots) {
                hitAround(sl, d, s, 3.5, mult(d.mv), Hit.WEAK);
                Fx.elem(sl, null, Fx.E_SPIKES, s, new Vec3(0, 1, 0), 1.2f, VIOLET, 0);
                Fx.ribbonHelix(sl, null, s.add(0, 0.5, 0), core, 0.5, t * 0.2, 12, 0, 0.25f, ROT);
                sl.sendParticles(ParticleTypes.ITEM_SLIME, s.x, s.y + 0.5, s.z, 20, 0.8, 0.5, 0.8, 0.05);
            }
            if (fed > 0) {
                d.heal(d.getMaxHealth() * 0.03f * fed);
                sl.sendParticles(ParticleTypes.HEART, d.getX(), d.getY() + d.getBbHeight(), d.getZ(), 3 + fed, 0.6, 0.4, 0.6, 0);
            }
            Fx.shake(sl, d.position(), 1.5f, 10, 32f);
            d.playSound(SoundEvents.WARDEN_HEARTBEAT, 4f, 0.7f);
        }
        return t < 40;
    }

    // ---- Ấn Chú: 6 ấn nối tiếp tiến về mục tiêu, mỗi ấn nổ trễ 14 tick sau khi hiện
    private static boolean sigils(ServerLevel sl, DocMau d, int t) {
        if (t == 0) { reaim(d); d.from = d.position(); d.triggerAnim("skill", "skill_c"); d.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 3f, 0.5f); }
        for (int i = 0; i < 6; i++) {
            Vec3 s = d.from.add(d.aimDir.scale(3 + i * 2.6));
            if (t == i * 5) { Fx.telegraphCircle(sl, null, s, 2f, 14, DANGER); Fx.rune(sl, null, s.add(0, 0.05, 0), 1.6f, VIOLET, 14, 5); }
            if (t == i * 5 + 14) {
                hitAround(sl, d, s, 2, mult(d.mv), Hit.WEAK);
                Fx.elem(sl, null, Fx.E_SPIKES, s, new Vec3(0, 1, 0), 0.9f, i % 2 == 0 ? VIOLET : ROT, 0);
                sl.sendParticles(D_VIOLET, s.x, s.y + 0.8, s.z, 14, 0.5, 0.6, 0.5, 0);
                d.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 2f, 0.6f + i * 0.08f);
            }
        }
        return t < 48;
    }

    // ---- Triều Độc: 2 vòng sóng lan từ 1 -> 16 (nhảy qua được)
    private static boolean tide(ServerLevel sl, DocMau d, int t) {
        Vec3 c = d.position();
        if (t == 0 || t == 30) {
            d.hitOnce.clear();
            Fx.telegraphCircle(sl, null, c, 16f, 14, ROT);
            Fx.groundRing(sl, null, c, 2.5f, VIOLET, 14);
            d.triggerAnim("skill", "skill_a");
            d.playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 3f, 0.5f);
        }
        int w = t >= 30 ? t - 30 : t;
        if (w >= 14 && w < 30) {
            double r = 1 + (w - 14) * 1.0;
            for (int i = 0; i < 32; i++) {
                double a = i * Math.PI / 16;
                sl.sendParticles(i % 2 == 0 ? D_ROT : D_VIOLET, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0.05, 0.15, 0.05, 0);
            }
            for (Player p : players(sl, c, r + 1)) {
                double dist = Math.sqrt(flat2(p.position(), c));
                if (dist < r - 1 || dist > r + 1 || p.getY() - c.y > 1.0 || !d.hitOnce.add(p.getUUID())) continue;
                if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mult(d.mv)))) PhaseMoves.applyHit(p, Hit.SLOW);
            }
        }
        return t < 62;
    }

    // ------------------------------------------------------------------ trợ giúp
    static List<Player> players(ServerLevel sl, Vec3 c, double r) {
        return sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(r, 4, r), p -> !p.isSpectator() && p.isAlive());
    }

    static double flat2(Vec3 a, Vec3 b) { double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    private static void hitAround(ServerLevel sl, DocMau d, Vec3 c, double r, double mul, Hit h) {
        for (Player p : players(sl, c, r)) {
            if (flat2(p.position(), c) > r * r || Math.abs(p.getY() - c.y) > 3) continue;
            if (p.hurt(d.damageSources().mobAttack(d), d.moveDamage(mul))) PhaseMoves.applyHit(p, h);
        }
    }

    static void addPool(ServerLevel sl, DocMau d, Vec3 c, double r, int life, double mul) {
        d.pools.add(new Pool(c, r, life, d.moveDamage(mul)));
        Fx.pool(sl, null, c, (float) r, ROT, life);
    }

    /** Vũng hủ: mỗi 10 tick sát thương phép + độc. */
    static void tickPools(ServerLevel sl, DocMau d) {
        for (Iterator<Pool> it = d.pools.iterator(); it.hasNext(); ) {
            Pool p = it.next();
            if (--p.left <= 0) { it.remove(); continue; }
            if (p.left % 6 == 0) sl.sendParticles(D_ROT, p.c.x, p.c.y + 0.2, p.c.z, 3, p.r * 0.5, 0.05, p.r * 0.5, 0);
            if (p.left % 10 != 0) continue;
            for (Player pl : players(sl, p.c, p.r)) {
                if (flat2(pl.position(), p.c) > p.r * p.r || Math.abs(pl.getY() - p.c.y) > 2) continue;
                pl.hurt(d.damageSources().magic(), p.dmg);
                pl.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 1));
            }
        }
    }

    // ------------------------------------------------------------------ VFX lộ diện
    static void transformFx(ServerLevel sl, DocMau d, int ph) {
        d.triggerAnim("skill", "phase");
        Vec3 c = d.position();
        if (ph == 2) {
            Fx.groundRing(sl, null, c, 8f, ROT, 60);
            Fx.pool(sl, null, c, 5f, VIOLET, 100);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                Fx.ribbon(sl, null, c.add(0, 2.5, 0), c.add(Math.cos(a) * 7, 4, Math.sin(a) * 7), 2.0, 14, i, 0.25f, VIOLET);
            }
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 3, c.z, 120, 4, 2, 4, 0);
            d.playSound(SoundEvents.BEEHIVE_EXIT, 3f, 0.4f);
        } else if (ph >= 3) {
            Fx.elem(sl, null, Fx.E_TONGUES, c, Vec3.ZERO, 3.5f, ROT, 0);
            Fx.groundCrack(sl, null, c, 10f, VIOLET, 10, 60);
            Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 3, 0), Vec3.ZERO, 3f, VIOLET, 0);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, c.x, c.y + 2, c.z, 150, 3, 2, 3, 0.1);
            d.playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.6f);
        }
    }

    // ------------------------------------------------------------------ cutscene (thoại: hukhong.dialog.docmau.*)
    private static CamPath.Key k(int t, double dx, double dy, double dz, double fov, double lookH, double pitch) {
        return new CamPath.Key(t, (float) dx, (float) dy, (float) dz, 0f, (float) pitch, (float) fov, (float) lookH);
    }

    /** Giới thiệu: sát mặt đầm nhìn lên mẹ độc giữa bầy bướm -> vòng ngang -> lùi xa. */
    public static List<CamPath.Key> introCam(int T) {
        return List.of(k(0, 1.5, 0.3, 5.5, 72, 3.0, -16), k(T * 2 / 5, -4.5, 1.4, 2.8, 60, 2.6, -6),
                k(T * 3 / 4, 0.8, 2.8, 3.2, 46, 2.8, 0), k(T, 0, 3.2, 7.5, 68, 2.4, 0));
    }

    public static List<SubLine> introSubs(int T) {
        return List.of(new SubLine(6, 36, "entity.hukhong.doc_mau"), new SubLine(36, 72, "hukhong.dialog.docmau.intro.1"),
                new SubLine(72, T - 4, "hukhong.dialog.docmau.intro.2"));
    }

    static final int ULT_T = 60;

    private static void ultScene(ServerLevel sl, DocMau d) {
        double s = Math.max(1.0, d.director().currentScale());
        List<CamPath.Key> cam = List.of(k(0, 2.5 * s, 0.4 * s, 4.0 * s, 60, 2.2 * s, -10), k(ULT_T / 2, -2.5 * s, 0.4 * s, 4.5 * s, 70, 3.5 * s, -18),
                k(ULT_T, 0, 6.0 * s, 10.0 * s, 82, 0.5 * s, -30));
        BossPhaseDirector.sendCutscene(sl, d, ULT_T, cam, List.of(new SubLine(2, 22, "hukhong.dialog.docmau.ult.1"),
                new SubLine(22, 42, "hukhong.dialog.docmau.ult.2"), new SubLine(42, ULT_T - 2, "hukhong.dialog.docmau.ult.3")));
    }

    static void deathScene(ServerLevel sl, DocMau d) {
        int T = 100;
        Vec3 at = d.position();
        List<CamPath.Key> cam = List.of(k(0, 0, 1.8, 4.5, 55, 1.4, 0), k(50, -3.0, 2.0, 3.0, 60, 1.2, 0), k(T, 0, 5.0, 9.0, 72, 1.0, -8));
        BossPhaseDirector.sendCutscene(sl, null, at, d.yBodyRot, T, cam, List.of(new SubLine(6, 36, "hukhong.dialog.docmau.death.1"),
                new SubLine(36, 68, "hukhong.dialog.docmau.death.2"), new SubLine(68, T - 4, "hukhong.dialog.docmau.death.3")));
        Fx.pool(sl, null, at, 5f, ROT, 140);
        Fx.groundRing(sl, null, at, 8f, VIOLET, 60);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 2, at.z, 150, 3, 2, 3, 0);
        sl.sendParticles(D_VIOLET, at.x, at.y + 2, at.z, 80, 2.5, 2, 2.5, 0);
    }
}
