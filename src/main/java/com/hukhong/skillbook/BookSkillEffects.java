package com.hukhong.skillbook;

import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.skill.SkillEffects;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

/**
 * Hiệu ứng các kỹ năng học từ bí tịch. Sát thương = coef x hệ số Cường Hóa x Công (hoặc Phép lực của Đạo).
 * Hình ảnh dùng helper công khai của {@link SkillEffects} (spiral, helixBeam, pillar, ringLater, ring, layer).
 */
public final class BookSkillEffects {
    private BookSkillEffects() {}

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), 1.2f);
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.7f, 0.05f, 0.08f), 1.1f);
    private static final DustParticleOptions STEEL = new DustParticleOptions(new Vector3f(0.75f, 0.9f, 1f), 1.0f);

    public static void run(BookSkill s, ServerPlayer p, int level) {
        if (!(p.level() instanceof ServerLevel lv)) return;
        double m = SkillBookMath.dmgMult(level);
        switch (s) {
            case PHA_KHONG_TRAM -> phaKhongTram(lv, p, s, m);
            case PHI_KIEM_TRUY_HON -> phiKiem(lv, p, s, m);
            case KIEM_VU_HOA -> kiemVuHoa(lv, p, s, m);
            case THIEN_KIEM_GIANG -> thienKiemGiang(lv, p, s, m);
            case THANH_TAM_CHU -> thanhTamChu(lv, p, s, m);
            case THIEN_LOI_CHU -> thienLoiChu(lv, p, s, m);
            case QUANG_TRAN -> quangTran(lv, p, s, m);
            case THIEN_PHAT -> thienPhat(lv, p, s, m);
            case LONG_HO_QUYEN -> longHoQuyen(lv, p, s, m);
            case THIET_SON_KHAO -> thietSonKhao(lv, p, s, m);
            case BA_THE_CHAN -> baTheChan(lv, p, s, m);
            case PHA_THIEN_QUYEN -> phaThienQuyen(lv, p, s, m);
            case HUYET_TE_CHU -> huyetTeChu(lv, p, s, m);
            case MA_ANH -> maAnh(lv, p, s, m);
            case HUYET_TE_DAI -> huyetTeDai(lv, p, s, m);
            case THON_THIEN_MA_CONG -> thonThienMaCong(lv, p, s, m);
            case LANG_BA_VI_BO -> langBaViBo(lv, p);
            case HO_THAN_CUONG_KHI -> hoThanCuongKhi(lv, p, s, m);
            case HOI_XUAN_THUAT -> hoiXuanThuat(lv, p, s, m);
        }
    }

    // ------------------------------------------------------------------ tiện ích
    private static boolean isMa(ServerPlayer p) { return p.getData(ModAttachments.CULTIVATION).paths().contains(Path.MA); }

    private static double power(ServerPlayer p, BookSkill s) { return s.spell() ? StatService.spellPower(p) : StatService.attackPower(p); }

    private static boolean valid(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.npc.Npc)
                && !(e instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p));
    }

    /** Một nhát kỹ năng: coef x m x Công/Phép lực; bỏ khung bất tử cho đòn nhiều nhịp. Trả sát thương đã gây. */
    private static float hit(ServerPlayer p, BookSkill s, LivingEntity t, double coef, double m) {
        float dmg = (float) (coef * m * power(p, s));
        t.invulnerableTime = 0;
        SkillBookRuntime.hurt(p, t, dmg);
        return dmg;
    }

    private static void knock(ServerPlayer p, LivingEntity t, double force, double up) {
        Vec3 d = t.position().subtract(p.position());
        Vec3 h = new Vec3(d.x, 0, d.z);
        h = h.lengthSqr() < 1e-4 ? p.getLookAngle().multiply(1, 0, 1) : h.normalize();
        t.setDeltaMovement(t.getDeltaMovement().add(h.x * force, up, h.z * force));
        t.hurtMarked = true;
    }

    private static List<LivingEntity> around(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        AABB box = new AABB(c.x - r, c.y - 2, c.z - r, c.x + r, c.y + 3, c.z + r);
        return lv.getEntitiesOfClass(LivingEntity.class, box, e -> valid(p, e) && e.position().subtract(c).multiply(1, 0, 1).lengthSqr() <= r * r);
    }

    private static List<LivingEntity> line(ServerLevel lv, ServerPlayer p, Vec3 from, Vec3 dir, double range, double width) {
        AABB box = new AABB(from, from.add(dir.scale(range))).inflate(width + 1);
        return lv.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!valid(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter().subtract(from);
            double t = c.dot(dir);
            if (t < 0 || t > range + 0.5) return false;
            return c.subtract(dir.scale(t)).length() <= width + e.getBbWidth() / 2;
        });
    }

    private static List<LivingEntity> cone(ServerLevel lv, ServerPlayer p, double range, double cosHalf) {
        Vec3 look = p.getLookAngle(), eye = p.getEyePosition();
        return lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range, 1.5, range), e -> {
            if (!valid(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter().subtract(eye);
            double d = c.length();
            return d <= range + e.getBbWidth() / 2 && (d < 0.8 || c.normalize().dot(look) >= cosHalf);
        });
    }

    /** Điểm ngắm: thực thể sống đầu tiên hoặc khối đầu tiên trên tia nhìn, tối đa range. */
    private static Vec3 aim(ServerLevel lv, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(lv, p, eye, end, box, e -> e instanceof LivingEntity le && valid(p, le));
        return eh != null ? eh.getLocation() : end;
    }

    private static Vec3 flatLook(ServerPlayer p) {
        Vec3 l = p.getLookAngle();
        Vec3 f = new Vec3(l.x, 0, l.z);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    /** Lướt tối đa dist block theo hướng ngang, dừng trước vật cản. Trả quãng đã lướt. Gây sát thương trên đường nếu coef > 0. */
    private static double dash(ServerLevel lv, ServerPlayer p, BookSkill s, double dist, double coef, double m, double width) {
        Vec3 dir = flatLook(p), start = p.position();
        double ok = 0;
        for (double d = 0.5; d <= dist + 1e-6; d += 0.5) {
            AABB moved = p.getBoundingBox().move(dir.scale(d));
            if (!lv.noCollision(p, moved)) break;
            ok = d;
        }
        if (ok < 0.5) return 0;
        Vec3 end = start.add(dir.scale(ok));
        if (coef > 0) for (LivingEntity e : line(lv, p, start.add(0, 0.5, 0), dir, ok, width)) {
            hit(p, s, e, coef, m);
            knock(p, e, 0.6, 0.25);
        }
        p.connection.teleport(end.x, end.y, end.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        return ok;
    }

    private static void trail(ServerLevel lv, Vec3 from, Vec3 to, ParticleOptions type) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.3) return;
        Vec3 u = d.scale(1 / len);
        for (double t = 0; t <= len; t += 0.4) {
            Vec3 q = from.add(u.scale(t));
            lv.sendParticles(type, q.x, q.y + 1.0, q.z, 1, 0.1, 0.3, 0.1, 0.01);
        }
    }

    // ------------------------------------------------------------------ Kiếm Tu
    /** Phá Không Trảm: kiếm khí xuyên 14 block. */
    private static void phaKhongTram(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        for (LivingEntity e : line(lv, p, eye, look, 14, 1.0)) hit(p, s, e, s.coef, m);
        Vec3 end = eye.add(look.scale(14));
        SkillEffects.helixBeam(lv, eye.add(look.scale(1.0)), end, 0.35, 8, 48, com.hukhong.vfx.ModParticles.KIEM_KHI.get());
        for (double t = 1; t <= 14; t += 0.6) {
            Vec3 q = eye.add(look.scale(t));
            lv.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0);
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        SkillEffects.layer(lv, p.position(), SoundEvents.TRIDENT_THROW.value(), 0.6f, 1.4f);
    }

    /** Phi Kiếm Truy Hồn: 4 phi kiếm tìm 4 mục tiêu gần nhất (lặp nếu ít mục tiêu), bay tới trong vài tick. */
    private static void phiKiem(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        List<LivingEntity> ts = around(lv, p, p.position(), 14).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(p))).toList();
        Vec3 origin = p.getEyePosition().add(0, 0.4, 0);
        for (int i = 0; i < 4; i++) {
            final double ang = i * Math.PI / 2 + p.getYRot() * Math.PI / 180;
            final Vec3 from = origin.add(Math.cos(ang) * 1.2, 0.2 * (i % 2), Math.sin(ang) * 1.2);
            if (ts.isEmpty()) { // không có mục tiêu: kiếm bay theo hướng nhìn
                Vec3 to = from.add(p.getLookAngle().scale(10));
                trail(lv, from.subtract(0, 1, 0), to.subtract(0, 1, 0), com.hukhong.vfx.ModParticles.KIEM_KHI.get());
                continue;
            }
            final LivingEntity t = ts.get(i % ts.size());
            final Vec3 tp = t.getBoundingBox().getCenter();
            SkillEffects.helixBeam(lv, from, tp, 0.18, 3, 20, STEEL);
            trail(lv, from.subtract(0, 1, 0), tp.subtract(0, 1, 0), ParticleTypes.END_ROD);
            SkillScheduler.later(3 + 2 * i, () -> {
                if (!t.isAlive() || p.isRemoved() || !p.isAlive()) return;
                hit(p, s, t, s.coef, m);
                lv.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 8, 0.25, 0.25, 0.25, 0.2);
                lv.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7f, 1.5f);
            });
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.7f);
        SkillEffects.layer(lv, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
    }

    /** Kiếm Vũ Hoa Lạc: mưa kiếm 3 giây tại điểm ngắm. */
    private static void kiemVuHoa(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = aim(lv, p, 14);
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 6, 48, com.hukhong.vfx.ModParticles.KIEM_KHI.get());
        SkillEffects.layer(lv, c, SoundEvents.TRIDENT_THUNDER.value(), 0.8f, 1.6f);
        kiemVuPulse(lv, p, s, m, c, 0);
    }

    private static void kiemVuPulse(ServerLevel lv, ServerPlayer p, BookSkill s, double m, Vec3 c, int n) {
        if (n >= 6 || p.isRemoved() || !p.isAlive()) return;
        for (LivingEntity e : around(lv, p, c, 6)) hit(p, s, e, s.coef, m);
        for (int i = 0; i < 14; i++) { // kiếm rơi
            double a = lv.random.nextDouble() * Math.PI * 2, r = Math.sqrt(lv.random.nextDouble()) * 6;
            double x = c.x + Math.cos(a) * r, z = c.z + Math.sin(a) * r;
            for (double h = 6; h > 0; h -= 1.0) lv.sendParticles(ParticleTypes.END_ROD, x, c.y + h, z, 1, 0, 0, 0, 0);
            lv.sendParticles(ParticleTypes.CRIT, x, c.y + 0.2, z, 2, 0.1, 0.05, 0.1, 0.1);
        }
        SkillEffects.ringLater(lv, c.add(0, 0.2, 0), 3 + n * 0.5, 24, 2, STEEL);
        lv.playSound(null, BlockPosHelper.of(c), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.2f + 0.05f * n);
        SkillScheduler.later(10, () -> kiemVuPulse(lv, p, s, m, c, n + 1));
    }

    /** Thiên Kiếm Giáng Thế: chùm kiếm quang 22 block. */
    private static void thienKiemGiang(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        // báo hiệu rồi bắn sau 8 tick
        Vec3 end = eye.add(look.scale(22));
        SkillEffects.helixBeam(lv, eye, end, 0.2, 6, 40, ParticleTypes.END_ROD);
        SkillEffects.layer(lv, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.8f);
        SkillScheduler.later(8, () -> {
            if (p.isRemoved() || !p.isAlive()) return;
            for (LivingEntity e : line(lv, p, eye, look, 22, 2.5)) {
                hit(p, s, e, s.coef, m);
                knock(p, e, 0.8, 0.4);
            }
            SkillEffects.helixBeam(lv, eye, end, 0.9, 10, 160, com.hukhong.vfx.ModParticles.KIEM_KHI.get());
            SkillEffects.helixBeam(lv, eye, end, 0.4, 5, 100, ParticleTypes.END_ROD);
            for (double t = 0; t <= 22; t += 0.5) {
                Vec3 q = eye.add(look.scale(t));
                lv.sendParticles(ParticleTypes.FLASH, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            }
            SkillEffects.ringLater(lv, end, 3, 30, 2, ParticleTypes.END_ROD);
            lv.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.6f);
            SkillEffects.layer(lv, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
        });
    }

    // ------------------------------------------------------------------ Đạo Tu
    /** Thanh Tâm Chú: gỡ debuff + hồi máu; đồng đội trong 5 block cũng được nếu người dùng không phải Ma tu. */
    private static void thanhTamChu(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        float amount = (float) Math.max(4, s.coef * m * StatService.spellPower(p));
        cleanse(p, amount);
        if (!isMa(p)) for (ServerPlayer o : lv.players())
            if (o != p && o.isAlive() && !o.isSpectator() && o.distanceToSqr(p) <= 25) cleanse(o, amount);
        SkillEffects.ring(lv, p.position().add(0, 0.2, 0), 5, 36, ParticleTypes.END_ROD);
        SkillEffects.spiral(lv, p.position(), 1.0, 0.4, 2.4, 2.5, 30, GOLD);
        SkillEffects.pillar(lv, p.position(), 3, 0.3, 8, 1, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
        SkillEffects.layer(lv, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.6f);
        SkillEffects.layer(lv, p.position(), SoundEvents.PLAYER_LEVELUP, 0.5f, 1.8f);
    }

    private static void cleanse(ServerPlayer o, float heal) {
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = o.getActiveEffects().stream()
                .filter(e -> !e.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect).toList();
        for (var e : bad) o.removeEffect(e);
        o.heal(heal);
    }

    /** Thiên Lôi Chú: sét giáng xuống điểm ngắm (tầm 20), bán kính 3. */
    private static void thienLoiChu(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = aim(lv, p, 20);
        for (LivingEntity e : around(lv, p, c, 3)) hit(p, s, e, s.coef, m);
        bolt(lv, c);
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 3, 28, ParticleTypes.ELECTRIC_SPARK);
        SkillEffects.ringLater(lv, c.add(0, 0.2, 0), 1.5, 16, 3, ParticleTypes.END_ROD);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1, c.z, 30, 1.2, 1.0, 1.2, 0.3);
        SkillEffects.layer(lv, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 1.4f);
    }

    private static void bolt(ServerLevel lv, Vec3 c) {
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(lv);
        if (b == null) return;
        b.moveTo(c.x, c.y, c.z);
        b.setVisualOnly(true);
        lv.addFreshEntity(b);
    }

    /** Quang Trận: 8 nhịp / 4 giây, 40% Phép lực lên quái; hồi máu người trong trận (Ma tu chỉ hồi bản thân). */
    private static void quangTran(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = p.position();
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 6, 56, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
        SkillEffects.layer(lv, c, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.5f);
        quangTranPulse(lv, p, s, m, c, 0);
    }

    private static void quangTranPulse(ServerLevel lv, ServerPlayer p, BookSkill s, double m, Vec3 c, int n) {
        if (n >= 8 || p.isRemoved() || !p.isAlive()) return;
        for (LivingEntity e : around(lv, p, c, 6)) hit(p, s, e, s.coef, m);
        float heal = (float) (0.15 * m * StatService.spellPower(p));
        boolean ma = isMa(p);
        for (ServerPlayer o : lv.players()) {
            if (!o.isAlive() || o.isSpectator() || (ma && o != p)) continue;
            if (o.position().subtract(c).multiply(1, 0, 1).lengthSqr() <= 36) o.heal(Math.max(1f, heal));
        }
        SkillEffects.spiral(lv, c, 5.5, 0.5, 4, 2.5, 60, GOLD);
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 6 - n * 0.4, 36, ParticleTypes.END_ROD);
        if (n % 2 == 0) SkillEffects.pillar(lv, c, 4, 0.8, 8, 1, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
        if (n % 2 == 0) SkillEffects.layer(lv, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.6f, 1.6f);
        SkillScheduler.later(10, () -> quangTranPulse(lv, p, s, m, c, n + 1));
    }

    /** Thiên Phạt: 1,5 giây báo hiệu rồi cột thánh quang bán kính 6. */
    private static void thienPhat(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = aim(lv, p, 24);
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 6, 56, GOLD);
        SkillEffects.ringLater(lv, c.add(0, 0.2, 0), 4, 40, 10, GOLD);
        SkillEffects.ringLater(lv, c.add(0, 0.2, 0), 2, 24, 20, GOLD);
        SkillEffects.layer(lv, c, SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.8f);
        SkillScheduler.later(30, () -> {
            if (p.isRemoved() || !p.isAlive()) return;
            for (LivingEntity e : around(lv, p, c, 6)) {
                hit(p, s, e, s.coef, m);
                knock(p, e, 0.3, 0.6);
            }
            SkillEffects.pillar(lv, c, 20, 0.9, 40, 3, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
            SkillEffects.pillar(lv, c, 20, 0.4, 40, 2, ParticleTypes.END_ROD);
            SkillEffects.spiral(lv, c, 6, 0.5, 14, 4, 120, GOLD);
            SkillEffects.ring(lv, c.add(0, 0.2, 0), 6, 56, ParticleTypes.END_ROD);
            bolt(lv, c);
            SkillEffects.layer(lv, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 1.2f);
            SkillEffects.layer(lv, c, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.4f);
        });
    }

    // ------------------------------------------------------------------ Thể Tu
    /** Long Hổ Quyền: quạt 4 block, hất lùi. */
    private static void longHoQuyen(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        for (LivingEntity e : cone(lv, p, 4, 0.5)) {
            hit(p, s, e, s.coef, m);
            knock(p, e, 1.0, 0.3);
        }
        Vec3 f = flatLook(p), o = p.position().add(0, 1.0, 0).add(f.scale(1.5));
        lv.sendParticles(ParticleTypes.SWEEP_ATTACK, o.x, o.y, o.z, 2, 0.4, 0.2, 0.4, 0);
        lv.sendParticles(com.hukhong.vfx.ModParticles.BA_KHI.get(), o.x, o.y, o.z, 14, 0.7, 0.4, 0.7, 0.05);
        SkillEffects.spiral(lv, p.position().add(f.scale(0.8)), 0.8, 1.4, 1.8, 1.5, 26, ParticleTypes.CRIT);
        lv.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.8f);
        SkillEffects.layer(lv, p.position(), SoundEvents.IRON_GOLEM_ATTACK, 0.6f, 1.2f);
    }

    /** Thiết Sơn Kháo: húc 7 block, giảm sát thương 3 giây. */
    private static void thietSonKhao(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 from = p.position();
        double d = dash(lv, p, s, 7, s.coef, m, 1.3);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
        trail(lv, from, from.add(flatLook(p).scale(Math.max(d, 1))), com.hukhong.vfx.ModParticles.BA_KHI.get());
        SkillEffects.ring(lv, p.position().add(0, 0.2, 0), 1.6, 20, ParticleTypes.POOF);
        lv.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.8, p.getZ(), 1, 0, 0, 0, 0);
        lv.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.6f);
        SkillEffects.layer(lv, p.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.7f);
    }

    /** Bá Thể Chấn: dậm bán kính 6, làm chậm, lá chắn nhỏ. */
    private static void baTheChan(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        for (LivingEntity e : around(lv, p, p.position(), 6)) {
            hit(p, s, e, s.coef, m);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            knock(p, e, 0.5, 0.4);
        }
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
        slam(lv, p.position(), 6);
        SkillEffects.layer(lv, p.position(), SoundEvents.ANVIL_LAND, 1.0f, 0.5f);
        SkillEffects.layer(lv, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 0.7f);
    }

    private static void slam(ServerLevel lv, Vec3 c, double r) {
        SkillEffects.ring(lv, c.add(0, 0.2, 0), r, 48, com.hukhong.vfx.ModParticles.BA_KHI.get());
        SkillEffects.ringLater(lv, c.add(0, 0.2, 0), r * 0.6, 32, 3, ParticleTypes.POOF);
        SkillEffects.ringLater(lv, c.add(0, 0.2, 0), r * 0.3, 20, 6, ParticleTypes.CAMPFIRE_COSY_SMOKE);
        lv.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5;
            for (double rr = 1; rr <= r; rr += 1.0) lv.sendParticles(ParticleTypes.CRIT, c.x + Math.cos(a) * rr, c.y + 0.15, c.z + Math.sin(a) * rr, 1, 0.05, 0, 0.05, 0);
        }
    }

    /** Phá Thiên Quyền: bật nhảy rồi giáng xuống, bán kính 7, hất tung. */
    private static void phaThienQuyen(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 f = flatLook(p);
        p.setDeltaMovement(f.x * 0.9, 1.15, f.z * 0.9);
        p.hurtMarked = true;
        p.fallDistance = 0;
        SkillEffects.spiral(lv, p.position(), 1.2, 0.3, 3, 2, 30, com.hukhong.vfx.ModParticles.BA_KHI.get());
        SkillEffects.layer(lv, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.9f);
        leapCheck(lv, p, s, m, 0);
    }

    private static void leapCheck(ServerLevel lv, ServerPlayer p, BookSkill s, double m, int n) {
        if (p.isRemoved() || !p.isAlive()) return;
        if ((p.onGround() && n >= 3) || n >= 40) {
            for (LivingEntity e : around(lv, p, p.position(), 7)) {
                hit(p, s, e, s.coef, m);
                knock(p, e, 0.6, 1.0);
            }
            slam(lv, p.position(), 7);
            SkillEffects.pillar(lv, p.position(), 5, 1.2, 14, 3, com.hukhong.vfx.ModParticles.BA_KHI.get());
            SkillEffects.layer(lv, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.5f);
            SkillEffects.layer(lv, p.position(), SoundEvents.ANVIL_LAND, 1.0f, 0.4f);
            return;
        }
        if (n % 3 == 0) lv.sendParticles(com.hukhong.vfx.ModParticles.BA_KHI.get(), p.getX(), p.getY(), p.getZ(), 4, 0.3, 0.1, 0.3, 0.02);
        SkillScheduler.later(2, () -> leapCheck(lv, p, s, m, n + 1));
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------ Ma Tu
    /** Huyết Tế Chú: tia huyết 10 block, hồi 30% sát thương (tối đa 5% máu tối đa). */
    private static void huyetTeChu(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        float total = 0;
        for (LivingEntity e : line(lv, p, eye, look, 10, 0.9)) total += hit(p, s, e, s.coef, m);
        if (total > 0) p.heal(Math.min(total * 0.30f, p.getMaxHealth() * 0.05f));
        Vec3 end = eye.add(look.scale(10));
        SkillEffects.helixBeam(lv, eye.add(look.scale(0.8)), end, 0.3, 6, 40, BLOOD);
        SkillEffects.helixBeam(lv, eye.add(look.scale(0.8)), end, 0.15, 3, 20, com.hukhong.vfx.ModParticles.MA_KHI.get());
        if (total > 0) lv.sendParticles(BLOOD, p.getX(), p.getY() + 1.2, p.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        lv.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.4f, 1.6f);
        SkillEffects.layer(lv, p.position(), SoundEvents.RAVAGER_ATTACK, 0.4f, 1.6f);
    }

    /** Ma Ảnh: lướt 8 block, bất tử 0,4 giây. */
    private static void maAnh(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 from = p.position();
        dash(lv, p, s, 8, s.coef, m, 1.2);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 8, 4, false, false));
        trail(lv, from, p.position(), com.hukhong.vfx.ModParticles.MA_KHI.get());
        trail(lv, from, p.position(), BLOOD);
        SkillEffects.ring(lv, from.add(0, 0.2, 0), 1.0, 16, ParticleTypes.SOUL_FIRE_FLAME);
        SkillEffects.ring(lv, p.position().add(0, 0.2, 0), 1.0, 16, ParticleTypes.SOUL_FIRE_FLAME);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    /** Huyết Tế Đài: 10 nhịp / 5 giây, hút máu hồi cho bản thân. */
    private static void huyetTeDai(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = p.position();
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 6, 56, BLOOD);
        SkillEffects.layer(lv, c, SoundEvents.SOUL_ESCAPE.value(), 1.0f, 0.6f);
        huyetDaiPulse(lv, p, s, m, c, 0);
    }

    private static void huyetDaiPulse(ServerLevel lv, ServerPlayer p, BookSkill s, double m, Vec3 c, int n) {
        if (n >= 10 || p.isRemoved() || !p.isAlive()) return;
        float total = 0;
        for (LivingEntity e : around(lv, p, c, 6)) total += hit(p, s, e, s.coef, m);
        if (total > 0) p.heal(Math.min(total * 0.30f, p.getMaxHealth() * 0.03f));
        SkillEffects.spiral(lv, c, 5.5, 0.4, 3, 2.5, 50, BLOOD);
        SkillEffects.ring(lv, c.add(0, 0.2, 0), 6, 36, com.hukhong.vfx.ModParticles.MA_KHI.get());
        if (n % 2 == 0) SkillEffects.layer(lv, c, SoundEvents.SOUL_ESCAPE.value(), 0.5f, 0.8f + 0.04f * n);
        SkillScheduler.later(10, () -> huyetDaiPulse(lv, p, s, m, c, n + 1));
    }

    /** Thôn Thiên Ma Công: bộc phát bán kính 8 và hồi 12% máu tối đa. */
    private static void thonThienMaCong(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        for (LivingEntity e : around(lv, p, p.position(), 8)) {
            hit(p, s, e, s.coef, m);
            knock(p, e, 0.8, 0.5);
        }
        p.heal((float) (p.getMaxHealth() * 0.12 * Math.min(m, 1.5)));
        SkillEffects.pillar(lv, p.position(), 8, 0.5, 20, 3, BLOOD);
        SkillEffects.spiral(lv, p.position(), 3.5, 0.4, 7, 4, 80, ParticleTypes.SOUL_FIRE_FLAME);
        SkillEffects.ring(lv, p.position().add(0, 0.2, 0), 8, 56, BLOOD);
        SkillEffects.ringLater(lv, p.position().add(0, 0.2, 0), 5, 40, 4, com.hukhong.vfx.ModParticles.MA_KHI.get());
        SkillEffects.ringLater(lv, p.position().add(0, 0.2, 0), 2.5, 24, 8, ParticleTypes.SOUL_FIRE_FLAME);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 0.5f);
        SkillEffects.layer(lv, p.position(), SoundEvents.WITHER_SPAWN, 0.35f, 1.4f);
    }

    // ------------------------------------------------------------------ Chung
    /** Lăng Ba Vi Bộ: lướt 9 block. */
    private static void langBaViBo(ServerLevel lv, ServerPlayer p) {
        Vec3 from = p.position();
        dash(lv, p, null, 9, 0, 1, 0);
        trail(lv, from, p.position(), ParticleTypes.CLOUD);
        trail(lv, from, p.position(), ParticleTypes.END_ROD);
        SkillEffects.ring(lv, from.add(0, 0.2, 0), 0.9, 14, ParticleTypes.POOF);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    /** Hộ Thân Cương Khí: lá chắn 20% máu tối đa trong 8 giây (nhân hệ số Cường Hóa). */
    private static void hoThanCuongKhi(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        float shield = (float) (p.getMaxHealth() * s.coef * m);
        int amp = Math.max(0, (int) Math.ceil(shield / 4f) - 1);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, amp, false, false));
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), shield));
        for (int h = 0; h < 3; h++) SkillEffects.ring(lv, p.position().add(0, 0.3 + 0.7 * h, 0), h == 1 ? 1.1 : 0.8, 14, ParticleTypes.END_ROD);
        SkillEffects.spiral(lv, p.position(), 1.0, 1.0, 2.2, 2.5, 32, STEEL);
        SkillEffects.layer(lv, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f);
        SkillEffects.layer(lv, p.position(), SoundEvents.IRON_GOLEM_REPAIR, 0.7f, 1.4f);
    }

    /** Hồi Xuân Thuật: hồi 25% máu tối đa cho bản thân (nhân hệ số Cường Hóa). */
    private static void hoiXuanThuat(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        p.heal((float) (p.getMaxHealth() * s.coef * m));
        SkillEffects.spiral(lv, p.position(), 0.9, 0.3, 2.2, 3, 40, ParticleTypes.HAPPY_VILLAGER);
        SkillEffects.pillar(lv, p.position(), 3, 0.4, 10, 1, com.hukhong.vfx.ModParticles.LINH_KHI.get());
        SkillEffects.ringLater(lv, p.position().add(0, 0.2, 0), 1.4, 20, 4, ParticleTypes.HEART);
        SkillEffects.layer(lv, p.position(), SoundEvents.PLAYER_LEVELUP, 0.6f, 1.4f);
        SkillEffects.layer(lv, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
    }

    /** Vị trí khối từ Vec3 (tránh phụ thuộc BlockPos.containing ở nhiều nơi). */
    private static final class BlockPosHelper {
        static net.minecraft.core.BlockPos of(Vec3 v) { return net.minecraft.core.BlockPos.containing(v); }
    }
}
