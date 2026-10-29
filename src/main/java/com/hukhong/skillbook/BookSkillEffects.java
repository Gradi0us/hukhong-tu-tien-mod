package com.hukhong.skillbook;

import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.skill.SkillEffects;
import com.hukhong.skill.SkillScheduler;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxSfx;
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

    // ------------------------------------------------------------------ bảng màu theo đạo + tiện ích hình ảnh
    private static final int KIEM_W = 0xDCEBFF, KIEM_B = 0x8FD0FF, DAO_G = 0xFFD866, DAO_W = 0xFFF2C0,
            THE_O = 0xE8903C, THE_B = 0xD9A441, MA_R = 0xB01030, MA_D = 0x7A0A28, WATER = 0x9FD8FF, LEAF = 0x7CE37C, HEX = 0x9FE8FF;

    private static Vec3 chest(net.minecraft.world.entity.Entity e) { return e.position().add(0, e.getBbHeight() * 0.62, 0); }

    /** Mặt đất dưới at (tia thẳng đứng xuống tối đa 10 block); không có thì giữ nguyên. */
    private static Vec3 groundAt(ServerLevel lv, ServerPlayer p, Vec3 at) {
        BlockHitResult h = lv.clip(new ClipContext(at.add(0, 1.5, 0), at.add(0, -10, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return h.getType() == HitResult.Type.MISS ? at : h.getLocation();
    }

    private static void fx(ServerLevel lv, ServerPlayer p, com.hukhong.vfx.FxType t, Vec3 pos, float scale, int rgb, int life) {
        Fx.playOwned(lv, p, t, pos, Vec3.ZERO, scale, rgb, life);
    }

    // ------------------------------------------------------------------ Kiếm Tu
    /** Phá Không Trảm: kiếm khí xuyên 14 block; để lại vết rách không gian mở rồi khép. */
    private static void phaKhongTram(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        for (LivingEntity e : line(lv, p, eye, look, 14, 1.0)) hit(p, s, e, s.coef, m);
        Vec3 end = eye.add(look.scale(14));
        Fx.rift(lv, p, eye.add(look.scale(1.2)), look.scale(12.5), 1.6f, 0x5A6CD8, 18);          // vết rách viền trắng
        Fx.qi(lv, eye.add(look.scale(1.5)), look.scale(12), 1.8f, KIEM_W, 0, 11);                  // lưỡi kiếm khí
        Fx.ribbon(lv, p, eye.add(look.scale(0.8)), end, 0, 4, 0, 0.14f, KIEM_B);
        SkillEffects.helixBeam(lv, eye.add(look.scale(1.0)), end, 0.3, 6, 24, com.hukhong.vfx.ModParticles.KIEM_KHI.get());
        FxSfx.cast(lv, p.position(), 1.0f, 1.25f, SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.TRIDENT_THROW.value(), SoundEvents.AMETHYST_BLOCK_RESONATE);
    }

    /** Phi Kiếm Truy Hồn: 4 phi kiếm tìm 4 mục tiêu gần nhất (lặp nếu ít mục tiêu), bay tới trong vài tick rồi bật ngược. */
    private static void phiKiem(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        List<LivingEntity> ts = around(lv, p, p.position(), 14).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(p))).toList();
        Vec3 origin = p.getEyePosition().add(0, 0.4, 0);
        for (int i = 0; i < 4; i++) {
            final double ang = i * Math.PI / 2 + p.getYRot() * Math.PI / 180;
            final Vec3 from = origin.add(Math.cos(ang) * 1.2, 0.2 * (i % 2), Math.sin(ang) * 1.2);
            if (ts.isEmpty()) { // không có mục tiêu: kiếm bay theo hướng nhìn
                Vec3 to = from.add(p.getLookAngle().scale(10));
                Fx.swordFly(lv, from, to, 6 + i, i, 0.8f);
                Fx.ribbon(lv, p, from, to, i % 2 == 0 ? 0.8 : -0.8, 6 + i, i, 0.06f, KIEM_B);
                continue;
            }
            final LivingEntity t = ts.get(i % ts.size());
            final Vec3 tp = t.getBoundingBox().getCenter();
            int fl = 3 + 2 * i;
            Fx.swordFly(lv, from, tp, fl, 0, 0.8f);                                  // phi kiếm thật (model Bản Mệnh Kiếm)
            Fx.ribbon(lv, p, from, tp, i % 2 == 0 ? 0.9 : -0.9, fl, 0, 0.06f, KIEM_B);  // sợi linh quang đi kèm
            Fx.swordFly(lv, tp, from.add(0, 0.5, 0), 6, fl + 1, 0.7f);               // bật ngược sau khi xuyên
            SkillScheduler.later(fl, () -> {
                if (!t.isAlive() || p.isRemoved() || !p.isAlive()) return;
                hit(p, s, t, s.coef, m);
                Vec3 c = t.getBoundingBox().getCenter();
                fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, c.subtract(0, t.getBbHeight() * 0.4, 0), 1.3f, KIEM_W, 9);
                lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 4, 0.2, 0.2, 0.2, 0.2);
                lv.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 1.5f + 0.1f * (fl % 3));
            });
        }
        FxSfx.cast(lv, p.position(), 1.0f, 1.7f, SoundEvents.TRIDENT_THROW.value(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    /** Kiếm Vũ Hoa Lạc: mưa kiếm 3 giây tại điểm ngắm. */
    private static void kiemVuHoa(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = aim(lv, p, 14);
        Vec3 gc = c.add(0, 0.05, 0);
        Fx.rune(lv, p, gc, 6.2f, KIEM_B, 72, 8);
        Fx.playOwned(lv, p, com.hukhong.vfx.FxType.SWORD_RAIN, gc, Vec3.ZERO, 6f, KIEM_W, 64);
        FxSfx.cast(lv, c, 0.9f, 1.6f, SoundEvents.TRIDENT_THUNDER.value(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundEvents.PLAYER_ATTACK_SWEEP);
        kiemVuPulse(lv, p, s, m, c, 0);
    }

    private static void kiemVuPulse(ServerLevel lv, ServerPlayer p, BookSkill s, double m, Vec3 c, int n) {
        if (n >= 6 || p.isRemoved() || !p.isAlive()) return;
        int bloom = 0;
        for (LivingEntity e : around(lv, p, c, 6)) {
            hit(p, s, e, s.coef, m);
            if (bloom++ < 3) fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, e.position(), 1.5f, 0xFFD9EA, 12); // mỗi lần trúng nở một bông hoa
        }
        for (int i = 0; i < 4; i++) {
            double a = lv.random.nextDouble() * Math.PI * 2, r = Math.sqrt(lv.random.nextDouble()) * 6;
            lv.sendParticles(ParticleTypes.CRIT, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 2, 0.1, 0.05, 0.1, 0.1);
        }
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), 3.5f + n * 0.5f, KIEM_B, 14);
        lv.playSound(null, BlockPosHelper.of(c), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.2f + 0.05f * n);
        SkillScheduler.later(10, () -> kiemVuPulse(lv, p, s, m, c, n + 1));
    }

    /** Thiên Kiếm Giáng Thế: báo hiệu 8 tick (băng sáng + cự kiếm hạ xuống), rồi chùm kiếm quang 22 block. */
    private static void thienKiemGiang(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(22));
        Vec3 flat = flatLook(p).scale(22), g0 = groundAt(lv, p, p.position()), gEnd = groundAt(lv, p, end);
        Fx.telegraphLine(lv, p, g0, flat, 5f, 8, KIEM_B);
        Fx.giantSword(lv, p, gEnd, 8f, KIEM_B, 8, 46);                       // cự kiếm: tụ lực -> giáng đúng tick 8
        SkillEffects.helixBeam(lv, eye, end, 0.2, 6, 20, ParticleTypes.END_ROD);
        FxSfx.cast(lv, p.position(), 1.0f, 1.8f, SoundEvents.BEACON_POWER_SELECT, SoundEvents.TRIDENT_THROW.value(), null);
        SkillScheduler.later(8, () -> {
            if (p.isRemoved() || !p.isAlive()) return;
            for (LivingEntity e : line(lv, p, eye, look, 22, 2.5)) {
                hit(p, s, e, s.coef, m);
                knock(p, e, 0.8, 0.4);
            }
            Fx.ribbon(lv, p, eye, end, 0, 4, 0, 0.5f, KIEM_W);
            Fx.ribbon(lv, p, eye, end, 0, 5, 0, 0.9f, KIEM_B);
            for (int i = 1; i <= 2; i++) Fx.groundCrack(lv, p, groundAt(lv, p, eye.add(look.scale(22 * i / 3.0))), 2.6f, KIEM_B, 4, 40);
            SkillEffects.helixBeam(lv, eye, end, 0.6, 8, 50, com.hukhong.vfx.ModParticles.KIEM_KHI.get());
            Fx.shake(lv, p.position(), 1.2f, 10, 30f);
            FxSfx.cast(lv, p.position(), 0.9f, 1.5f, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.PLAYER_ATTACK_SWEEP);
        });
    }

    // ------------------------------------------------------------------ Đạo Tu
    /** Thanh Tâm Chú: gỡ debuff + hồi máu; đồng đội trong 5 block cũng được nếu người dùng không phải Ma tu. */
    private static void thanhTamChu(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        float amount = (float) Math.max(4, s.coef * m * StatService.spellPower(p));
        cleanse(p, amount);
        List<ServerPlayer> got = new java.util.ArrayList<>();
        got.add(p);
        if (!isMa(p)) for (ServerPlayer o : lv.players())
            if (o != p && o.isAlive() && !o.isSpectator() && o.distanceToSqr(p) <= 25) { cleanse(o, amount); got.add(o); }
        Vec3 hand = p.position().add(0, 1.3, 0);
        int k = 0;
        for (ServerPlayer o : got) {
            final ServerPlayer tgt = o;
            if (o == p) {
                for (int h = 0; h < 2; h++) Fx.ribbonHelix(lv, p, p.position().add(0, 0.1, 0), p.position().add(0, 2.5, 0), 0.7, h * Math.PI, 12, h * 2, 0.07f, DAO_G);
                bloomHeal(lv, p, p);
            } else if (k++ < 4) { // bùa vàng bay tới đồng đội rồi dán lên
                Fx.ribbon(lv, p, hand, chest(o), 1.0, 5, 0, 0.1f, DAO_G);
                SkillScheduler.later(5, () -> { if (tgt.isAlive()) bloomHeal(lv, p, tgt); });
            }
        }
        SkillEffects.spiral(lv, p.position(), 1.0, 0.4, 2.4, 2.5, 18, GOLD);
        FxSfx.cast(lv, p.position(), 1.0f, 1.6f, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.PLAYER_LEVELUP, SoundEvents.BEACON_ACTIVATE);
    }

    private static void bloomHeal(ServerLevel lv, ServerPlayer owner, ServerPlayer at) {
        Vec3 g = at.position().add(0, 0.06, 0);
        Fx.rune(lv, owner, g, 2.0f, DAO_G, 36, 6);
        fx(lv, owner, com.hukhong.vfx.FxType.LIGHT_PILLAR, at.position(), 0.5f, DAO_W, 22);
        Fx.groundRing(lv, owner, g, 3.4f, DAO_W, 16);
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
        Vec3 g = groundAt(lv, p, c);
        Fx.ribbon(lv, p, p.position().add(0, 1.4, 0), g.add(0, 1.2, 0), 1.5, 3, 0, 0.12f, DAO_G);      // bùa ném ra
        Fx.rune(lv, p, g.add(0, 0.06, 0), 3.4f, DAO_W, 30, 6);                                           // dấu bùa dưới đất
        Fx.playOwned(lv, p, com.hukhong.vfx.FxType.LIGHTNING_BOLT, g, Vec3.ZERO, 1f, DAO_W, 0);
        Fx.groundRing(lv, p, g.add(0, 0.06, 0), 3.2f, DAO_W, 16);
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 1, c.z, 12, 1.0, 0.8, 1.0, 0.3);
        FxSfx.cast(lv, c, 0.9f, 1.4f, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.TRIDENT_THUNDER.value());
    }

    /** Quang Trận: 8 nhịp / 4 giây, 40% Phép lực lên quái; hồi máu người trong trận (Ma tu chỉ hồi bản thân). */
    private static void quangTran(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = p.position();
        Fx.rune(lv, p, c.add(0, 0.06, 0), 6.2f, DAO_G, 96, 8);   // nhiều vòng xoay ngược chiều
        FxSfx.cast(lv, c, 0.9f, 1.5f, SoundEvents.BEACON_ACTIVATE, SoundEvents.AMETHYST_CLUSTER_BREAK, null);
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
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), 6f, DAO_W, 14);
        if (n % 2 == 0) { // cột sáng nhỏ nhô lên mỗi nhịp
            double a = lv.random.nextDouble() * Math.PI * 2, r = 1.5 + lv.random.nextDouble() * 3.5;
            fx(lv, p, com.hukhong.vfx.FxType.LIGHT_PILLAR, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), 0.45f, DAO_W, 20);
            SkillEffects.layer(lv, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.5f, 1.6f + 0.05f * n);
        }
        SkillScheduler.later(10, () -> quangTranPulse(lv, p, s, m, c, n + 1));
    }

    /** Thiên Phạt: 1,5 giây báo hiệu (trời tối, mắt trận mở, vùng vàng lấp đầy) rồi cột thánh quang bán kính 6. */
    private static void thienPhat(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = aim(lv, p, 24);
        Vec3 g = groundAt(lv, p, c).add(0, 0.06, 0);
        Fx.telegraphCircle(lv, p, g, 6f, 30, DAO_G);
        Fx.rune(lv, p, g.add(0, 15, 0), 5.5f, DAO_G, 38, 8);                    // mắt trận khổng lồ trên trời
        Fx.rune(lv, p, g, 6.2f, DAO_W, 44, 6);
        Fx.playOwned(lv, p, com.hukhong.vfx.FxType.SCREEN_TINT, g, Vec3.ZERO, 0.28f, 0x010101, 26); // trời tối lại
        SkillEffects.layer(lv, c, SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.8f);
        SkillScheduler.later(30, () -> {
            if (p.isRemoved() || !p.isAlive()) return;
            for (LivingEntity e : around(lv, p, c, 6)) {
                hit(p, s, e, s.coef, m);
                knock(p, e, 0.3, 0.6);
            }
            fx(lv, p, com.hukhong.vfx.FxType.LIGHT_PILLAR, g, 3.2f, DAO_G, 44);
            fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, g, 6.5f, DAO_W, 20);
            fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, g, 4f, 0xFFFFFF, 18);
            Fx.groundRing(lv, p, g, 6.5f, DAO_G, 22);
            Fx.playOwned(lv, p, com.hukhong.vfx.FxType.SCREEN_TINT, g, Vec3.ZERO, 0.6f, 0xFFFFFF, 14);   // chớp trắng
            Fx.playOwned(lv, p, com.hukhong.vfx.FxType.LIGHTNING_BOLT, g, Vec3.ZERO, 1.4f, DAO_W, 0);
            Fx.shake(lv, g, 1.3f, 12, 34f);
            SkillEffects.pillar(lv, c, 20, 0.6, 20, 2, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
            FxSfx.cast(lv, c, 0.9f, 1.2f, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.BEACON_DEACTIVATE);
        });
    }

    // ------------------------------------------------------------------ Thể Tu
    /** Long Hổ Quyền: quạt 4 block, hất lùi. Bóng rồng (vàng) và hổ (cam) theo hai nắm đấm. */
    private static void longHoQuyen(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        for (LivingEntity e : cone(lv, p, 4, 0.5)) {
            hit(p, s, e, s.coef, m);
            knock(p, e, 1.0, 0.3);
        }
        Vec3 f = flatLook(p), o = p.position().add(0, 1.0, 0).add(f.scale(1.5));
        Vec3 right = new Vec3(-f.z, 0, f.x), base = p.position().add(0, 1.25, 0);
        Fx.ribbonWave(lv, p, base.add(right.scale(0.45)), base.add(f.scale(4.4)).add(right.scale(0.2)), 0.5, 0.55, 7, 0, 0.17f, THE_B);   // rồng
        Fx.ribbonWave(lv, p, base.add(right.scale(-0.45)), base.add(f.scale(4.0)).add(right.scale(-0.2)), -0.4, 0.4, 6, 2, 0.21f, THE_O); // hổ
        Fx.qi(lv, base.add(f.scale(1.0)), f.scale(3.0), 1.5f, THE_O, 55, 9);
        fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, p.position().add(f.scale(2.6)), 2.0f, THE_B, 10);
        lv.sendParticles(com.hukhong.vfx.ModParticles.BA_KHI.get(), o.x, o.y, o.z, 8, 0.6, 0.4, 0.6, 0.05);
        FxSfx.cast(lv, p.position(), 1.0f, 0.85f, SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.IRON_GOLEM_ATTACK, SoundEvents.ENDER_DRAGON_FLAP);
    }

    /** Thiết Sơn Kháo: húc 7 block, giảm sát thương 3 giây. */
    private static void thietSonKhao(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 from = p.position();
        double d = dash(lv, p, s, 7, s.coef, m, 1.3);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
        Vec3 to = p.position();
        Fx.ribbon(lv, p, from.add(0, 0.9, 0), to.add(0, 0.9, 0), 0, 3, 0, 0.45f, THE_B);   // vệt vai bụi đồng
        Fx.ribbon(lv, p, from.add(0, 0.25, 0), to.add(0, 0.25, 0), 0, 4, 0, 0.3f, THE_O);
        if (d >= 0.5) {
            Fx.groundRing(lv, p, to.add(0, 0.06, 0), 3f, THE_B, 16);
            Fx.groundCrack(lv, p, to.add(Vec3.ZERO), 3.2f, THE_O, 6, 38);
            fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, to, 3.4f, THE_O, 14);                // sóng đá
            Fx.shake(lv, to, 0.8f, 6, 16f);
        }
        lv.sendParticles(ParticleTypes.POOF, from.x, from.y + 0.2, from.z, 6, 0.4, 0.1, 0.4, 0.05);
        FxSfx.cast(lv, from, 0.9f, 0.7f, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundEvents.ANVIL_LAND, SoundEvents.GENERIC_EXPLODE.value());
    }

    /** Bá Thể Chấn: dậm bán kính 6, làm chậm, lá chắn nhỏ. */
    private static void baTheChan(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        for (LivingEntity e : around(lv, p, p.position(), 6)) {
            hit(p, s, e, s.coef, m);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            knock(p, e, 0.5, 0.4);
        }
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
        slam(lv, p, p.position(), 6);
        FxSfx.cast(lv, p.position(), 1.0f, 0.5f, SoundEvents.ANVIL_LAND, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.IRON_GOLEM_DAMAGE);
    }

    /** Mặt đất nứt tròn, đá bay lên rồi rơi, sóng chấn lan ra. */
    private static void slam(ServerLevel lv, ServerPlayer p, Vec3 c, double r) {
        Vec3 g = c.add(0, 0.06, 0);
        Fx.groundCrack(lv, p, c, (float) r, THE_O, 8, 56);
        Fx.groundRing(lv, p, g, (float) r * 1.15f, THE_B, 20);
        fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, c, (float) r, THE_O, 18);
        fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, c, (float) r * 0.6f, 0xFFFFFF, 14);
        Fx.shake(lv, c, 0.9f + (float) r * 0.07f, 9, 24f);
        lv.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.2, c.z, 8, r * 0.3, 0.1, r * 0.3, 0.04);
    }

    /** Phá Thiên Quyền: bật nhảy (tụ lực quanh nắm đấm) rồi giáng xuống, bán kính 7, hất tung. */
    private static void phaThienQuyen(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 f = flatLook(p);
        p.setDeltaMovement(f.x * 0.9, 1.15, f.z * 0.9);
        p.hurtMarked = true;
        p.fallDistance = 0;
        Vec3 feet = p.position();
        Fx.rune(lv, p, feet.add(0, 0.06, 0), 2.0f, THE_B, 22, 4);                 // tụ lực dưới chân
        for (int h = 0; h < 2; h++) Fx.ribbonHelix(lv, p, feet, feet.add(0, 3.4, 0), 0.65, h * Math.PI, 6, 0, 0.1f, THE_O);   // vầng khí cuộn theo cú nhảy
        SkillEffects.layer(lv, feet, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.9f);
        leapCheck(lv, p, s, m, 0);
    }

    private static void leapCheck(ServerLevel lv, ServerPlayer p, BookSkill s, double m, int n) {
        if (p.isRemoved() || !p.isAlive()) return;
        if ((p.onGround() && n >= 3) || n >= 40) {
            for (LivingEntity e : around(lv, p, p.position(), 7)) {
                hit(p, s, e, s.coef, m);
                knock(p, e, 0.6, 1.0);
            }
            Vec3 c = p.position();
            slam(lv, p, c, 7);
            fx(lv, p, com.hukhong.vfx.FxType.LIGHT_PILLAR, c, 1.7f, THE_B, 32);        // cột khí xé mây
            fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, c, 9f, THE_B, 22);
            Fx.playOwned(lv, p, com.hukhong.vfx.FxType.SCREEN_TINT, c, Vec3.ZERO, 0.22f, 0xFFE2B0, 10);
            FxSfx.cast(lv, c, 1.0f, 0.5f, SoundEvents.GENERIC_EXPLODE.value(), SoundEvents.ANVIL_LAND, SoundEvents.IRON_GOLEM_DAMAGE);
            return;
        }
        if (n % 3 == 0) lv.sendParticles(com.hukhong.vfx.ModParticles.BA_KHI.get(), p.getX(), p.getY(), p.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
        SkillScheduler.later(2, () -> leapCheck(lv, p, s, m, n + 1));
        p.fallDistance = 0;
    }

    // ------------------------------------------------------------------ Ma Tu
    /** Huyết Tế Chú: tia huyết 10 block, hồi 30% sát thương (tối đa 5% máu tối đa). Máu bị rút thành dòng về người chơi. */
    private static void huyetTeChu(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0), look = p.getLookAngle();
        float total = 0;
        List<LivingEntity> hits = line(lv, p, eye, look, 10, 0.9);
        for (LivingEntity e : hits) total += hit(p, s, e, s.coef, m);
        if (total > 0) p.heal(Math.min(total * 0.30f, p.getMaxHealth() * 0.05f));
        Vec3 end = eye.add(look.scale(10));
        Fx.ribbonWave(lv, p, eye.add(look.scale(0.8)), end, 0, 0.25, 3, 0, 0.12f, MA_R);
        int k = 0;
        for (LivingEntity e : hits) {
            if (k++ >= 3) break;
            Vec3 ec = chest(e);
            Fx.rune(lv, p, e.position().add(0, 0.06, 0), 1.7f, MA_R, 26, 5);                        // phù văn máu dưới chân
            for (int j = 0; j < 3; j++) Fx.ribbonWave(lv, p, ec, p.position().add(0, 1.2, 0), 0.6, 0.35, 8, 2 + 2 * j, 0.07f, MA_R);   // dòng máu về chủ
        }
        if (total > 0) lv.sendParticles(BLOOD, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
        FxSfx.cast(lv, p.position(), 0.5f, 1.5f, SoundEvents.WITHER_SHOOT, SoundEvents.RAVAGER_ATTACK, SoundEvents.SOUL_ESCAPE.value());
    }

    /** Ma Ảnh: lướt 8 block, bất tử 0,4 giây; để lại phân thân bóng và vệt đỏ thẫm. */
    private static void maAnh(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 from = p.position();
        dash(lv, p, s, 8, s.coef, m, 1.2);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 8, 4, false, false));
        Vec3 to = p.position();
        Fx.playOwned(lv, p, com.hukhong.vfx.FxType.DEMON_SILHOUETTE, from, Vec3.ZERO, 0.45f, MA_D, 16);   // phân thân bóng
        Fx.ribbon(lv, p, from.add(0, 1.0, 0), to.add(0, 1.0, 0), 0, 3, 0, 0.42f, MA_R);
        Fx.ribbon(lv, p, from.add(0, 0.4, 0), to.add(0, 0.4, 0), 0, 4, 0, 0.26f, MA_D);
        Fx.rune(lv, p, from.add(0, 0.06, 0), 1.5f, MA_R, 20, 5);
        Fx.groundRing(lv, p, to.add(0, 0.06, 0), 1.8f, MA_R, 12);
        FxSfx.cast(lv, from, 0.8f, 0.6f, SoundEvents.ENDERMAN_TELEPORT, SoundEvents.SOUL_ESCAPE.value(), null);
    }

    /** Huyết Tế Đài: 10 nhịp / 5 giây, hút máu hồi cho bản thân. Đài máu trồi lên, xích máu trói kẻ địch. */
    private static void huyetTeDai(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        Vec3 c = p.position();
        Fx.rune(lv, p, c.add(0, 0.06, 0), 6.2f, MA_R, 112, 5);
        Fx.pool(lv, p, c.add(0, 0.04, 0), 5.2f, MA_D, 112);
        fx(lv, p, com.hukhong.vfx.FxType.BLOOD_VORTEX, c, 3.2f, MA_R, 40);
        SkillEffects.layer(lv, c, SoundEvents.SOUL_ESCAPE.value(), 1.0f, 0.6f);
        huyetDaiPulse(lv, p, s, m, c, 0);
    }

    private static void huyetDaiPulse(ServerLevel lv, ServerPlayer p, BookSkill s, double m, Vec3 c, int n) {
        if (n >= 10 || p.isRemoved() || !p.isAlive()) return;
        float total = 0;
        List<LivingEntity> es = around(lv, p, c, 6);
        for (LivingEntity e : es) total += hit(p, s, e, s.coef, m);
        if (total > 0) p.heal(Math.min(total * 0.30f, p.getMaxHealth() * 0.03f));
        Fx.groundRing(lv, p, c.add(0, 0.06, 0), 6f, MA_R, 12);
        if (n % 3 == 0) { // xích máu bắn ra trói kẻ địch, máu chảy ngược về chủ
            int k = 0;
            for (LivingEntity e : es) {
                if (k++ >= 4) break;
                Fx.ribbonWave(lv, p, c.add(0, 0.5, 0), chest(e), 0.3, 0.2, 4, 0, 0.07f, MA_R);
                if (total > 0) Fx.ribbonWave(lv, p, chest(e), p.position().add(0, 1.2, 0), 0.7, 0.3, 8, 3, 0.06f, MA_R);
            }
        }
        if (n % 2 == 0) SkillEffects.layer(lv, c, SoundEvents.SOUL_ESCAPE.value(), 0.5f, 0.8f + 0.04f * n);
        SkillScheduler.later(10, () -> huyetDaiPulse(lv, p, s, m, c, n + 1));
    }

    /** Thôn Thiên Ma Công: bộc phát bán kính 8 và hồi 12% máu tối đa. Hố đen đỏ hút vào, ma thần há miệng phía sau. */
    private static void thonThienMaCong(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        for (LivingEntity e : around(lv, p, p.position(), 8)) {
            hit(p, s, e, s.coef, m);
            knock(p, e, 0.8, 0.5);
        }
        p.heal((float) (p.getMaxHealth() * 0.12 * Math.min(m, 1.5)));
        Vec3 c = p.position(), mid = c.add(0, 1.0, 0);
        Fx.pool(lv, p, c.add(0, 0.04, 0), 7f, MA_D, 70);
        Fx.rune(lv, p, c.add(0, 0.06, 0), 8.2f, MA_R, 44, 5);
        fx(lv, p, com.hukhong.vfx.FxType.BLOOD_VORTEX, c, 7f, MA_R, 40);
        for (int i = 0; i < 8; i++) { // mọi thứ bị hút vào tâm
            double a = Math.PI * 2 * i / 8 + 0.2;
            Fx.ribbonWave(lv, p, c.add(Math.cos(a) * 7.5, 0.4, Math.sin(a) * 7.5), mid, 0.8, 0.4, 10, i / 3, 0.09f, MA_R);
        }
        Vec3 behind = c.subtract(flatLook(p).scale(1.4));
        Fx.playOwned(lv, p, com.hukhong.vfx.FxType.DEMON_SILHOUETTE, behind, Vec3.ZERO, 1.7f, MA_D, 34);  // ma thần
        Fx.shake(lv, c, 1.0f, 10, 24f);
        SkillEffects.spiral(lv, c, 3.5, 0.4, 7, 4, 30, ParticleTypes.SOUL_FIRE_FLAME);
        FxSfx.cast(lv, c, 0.8f, 0.5f, SoundEvents.ENDER_DRAGON_GROWL, SoundEvents.WITHER_SPAWN, SoundEvents.SOUL_ESCAPE.value());
    }

    // ------------------------------------------------------------------ Chung
    /** Lăng Ba Vi Bộ: lướt 9 block; gợn sóng dưới chân và bóng mờ các bước trước. */
    private static void langBaViBo(ServerLevel lv, ServerPlayer p) {
        Vec3 from = p.position();
        dash(lv, p, null, 9, 0, 1, 0);
        Vec3 to = p.position();
        Fx.ribbon(lv, p, from.add(0, 1.0, 0), to.add(0, 1.0, 0), 0, 3, 0, 0.35f, WATER);
        Fx.ribbon(lv, p, from.add(0, 0.15, 0), to.add(0, 0.15, 0), 0, 4, 0, 0.25f, KIEM_W);
        Vec3 d = to.subtract(from);
        int steps = (int) Math.min(5, Math.max(1, d.length() / 2));
        for (int i = 0; i <= steps; i++) { // gợn sóng nước tại từng bước
            final Vec3 at = from.add(d.scale((double) i / steps));
            SkillScheduler.later(1 + i, () -> fx(lv, p, com.hukhong.vfx.FxType.SHOCKWAVE, at, 1.5f, WATER, 14));
        }
        FxSfx.cast(lv, from, 0.6f, 1.7f, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundEvents.ENDER_DRAGON_FLAP, null);
    }

    /** Hộ Thân Cương Khí: lá chắn 20% máu tối đa trong 8 giây (nhân hệ số Cường Hóa). Vỏ lục giác bao quanh. */
    private static void hoThanCuongKhi(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        float shield = (float) (p.getMaxHealth() * s.coef * m);
        int amp = Math.max(0, (int) Math.ceil(shield / 4f) - 1);
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, amp, false, false));
        p.setAbsorptionAmount(Math.max(p.getAbsorptionAmount(), shield));
        Fx.shell(lv, p, p, 1.3f, HEX, 160);
        Fx.rune(lv, p, p.position().add(0, 0.06, 0), 1.7f, HEX, 22, 6);
        Fx.groundRing(lv, p, p.position().add(0, 0.06, 0), 2.2f, KIEM_W, 14);
        SkillEffects.spiral(lv, p.position(), 1.0, 1.0, 2.2, 2.5, 16, STEEL);
        FxSfx.cast(lv, p.position(), 0.8f, 1.6f, SoundEvents.BEACON_ACTIVATE, SoundEvents.IRON_GOLEM_REPAIR, null);
    }

    /** Hồi Xuân Thuật: hồi 25% máu tối đa cho bản thân (nhân hệ số Cường Hóa). Dây leo xanh nở quanh người. */
    private static void hoiXuanThuat(ServerLevel lv, ServerPlayer p, BookSkill s, double m) {
        p.heal((float) (p.getMaxHealth() * s.coef * m));
        Vec3 f = p.position();
        for (int h = 0; h < 3; h++) Fx.ribbonHelix(lv, p, f.add(0, 0.05, 0), f.add(0, 2.7, 0), 0.75, h * Math.PI * 2 / 3, 16, h * 2, 0.07f, LEAF);
        Fx.rune(lv, p, f.add(0, 0.06, 0), 1.7f, LEAF, 36, 6);
        Fx.groundRing(lv, p, f.add(0, 0.06, 0), 2.6f, LEAF, 16);
        SkillEffects.spiral(lv, f, 0.9, 0.3, 2.2, 3, 16, ParticleTypes.HAPPY_VILLAGER);
        SkillEffects.ringLater(lv, f.add(0, 0.2, 0), 1.4, 8, 6, ParticleTypes.HEART);
        FxSfx.cast(lv, f, 0.6f, 1.4f, SoundEvents.PLAYER_LEVELUP, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.BONE_MEAL_USE);
    }

    /** Vị trí khối từ Vec3 (tránh phụ thuộc BlockPos.containing ở nhiều nơi). */
    private static final class BlockPosHelper {
        static net.minecraft.core.BlockPos of(Vec3 v) { return net.minecraft.core.BlockPos.containing(v); }
    }
}
