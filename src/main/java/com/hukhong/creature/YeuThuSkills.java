package com.hukhong.creature;

import com.hukhong.HuKhong;
import com.hukhong.vfx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Hồn kỹ của Yêu Thú (mỗi loài 1 chiêu). Báo trước WINDUP tick (vòng/đường/nón mờ), hồi chiêu ngắn dần theo năm tuổi,
 * sát thương = công của thú x hệ số. Hiệu ứng nhẹ: bụi màu, mảnh đất, vòng sóng — không loè loẹt.
 * <ul>
 *   <li>Xích Văn Hổ — Hổ Khiếu: gầm, nón 7 ô phía trước, suy yếu + chậm.</li>
 *   <li>Hắc Giáp Tê — Thiết Giác Xung: lao thẳng 10 ô, húc văng mọi người trên đường.</li>
 *   <li>Cuồng Lực Viên — Băng Sơn Quyền: nhảy tới mục tiêu, đập đất bán kính 4, hất tung.</li>
 *   <li>Bích Nhãn Thiềm — Độc Vụ: phun vũng độc bán kính 3 dưới chân mục tiêu (6 giây).</li>
 *   <li>Ngân Lân Mãng — Ngân Lân Triền: kéo mục tiêu về sát người và siết (khoá chân 2 giây).</li>
 *   <li>Huyết Nha Trư — Huyết Nha Đột: 3 lần lao ngắn liên tiếp, mỗi lần gây chảy máu.</li>
 *   <li>U Ảnh Báo — U Ảnh Tập: tàng hình, hiện ra sau lưng mục tiêu, cắn bạo kích.</li>
 *   <li>Thiết Giáp Tri Chu — Thiên La Võng: bắn tơ, mạng nhện quanh mục tiêu 4 giây.</li>
 *   <li>Bạo Viêm Tích — Viêm Tức: phun lửa nón 6 ô, đốt cháy.</li>
 * </ul>
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class YeuThuSkills {
    private YeuThuSkills() {}

    public static final int WINDUP = 20, RANGE = 14;

    /** Hồi chiêu (tick) theo năm tuổi: 10 năm 220 -> 100.000 năm 100. */
    public static int cooldown(YeuThuAge a) { return 220 - 30 * a.ordinal(); }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** Màu báo trước: trắng xám cho thú trẻ, đỏ cho thú vạn năm. */
    private static int warnColor(YeuThuAge a) { return a.ordinal() >= YeuThuAge.Y10000.ordinal() ? 0xE03A2A : 0xE8E0D0; }

    static void tick(LinhQuai m) {
        if (!(m.level() instanceof ServerLevel sl) || !m.isAlive()) return;
        YeuThuAge age = m.age();
        if (age == null) return;
        if (m.charge > 0) { chargeTick(sl, m); return; }
        if (m.skillCd > 0) m.skillCd--;
        LivingEntity t = m.getTarget();
        if (m.windup > 0) {
            if (--m.windup == 0) {
                if (t != null && t.isAlive()) cast(sl, m, t, age);
                m.skillCd = cooldown(age);
            }
            return;
        }
        if (m.skillCd > 0 || t == null || !t.isAlive() || t.distanceToSqr(m) > RANGE * RANGE || !m.getSensing().hasLineOfSight(t)) return;
        start(sl, m, t, age);
    }

    /** Bắt đầu báo trước (gọi tay được trong test). */
    public static void start(ServerLevel sl, LinhQuai m, LivingEntity t, YeuThuAge age) {
        m.windup = WINDUP;
        int c = warnColor(age);
        Vec3 at = m.position().add(0, 0.05, 0), face = t.position().subtract(m.position()).multiply(1, 0, 1).normalize();
        switch (m.kind()) {
            case XICH_VAN_HO -> Fx.telegraphCone(sl, null, at, face, 7f, 35f, WINDUP);
            case BAO_VIEM_TICH -> Fx.telegraphCone(sl, null, at, face, 6f, 30f, WINDUP);
            case HAC_GIAP_TE, HUYET_NHA_TRU -> Fx.telegraphLine(sl, null, at, face.scale(m.kind() == CreatureKind.HAC_GIAP_TE ? 10 : 6), 2.2f * (float) m.getScale(), WINDUP, c);
            case CUONG_LUC_VIEN -> Fx.telegraphCircle(sl, null, t.position().add(0, 0.05, 0), 4f, WINDUP, c);
            case BICH_NHAN_THIEM -> Fx.telegraphCircle(sl, null, t.position().add(0, 0.05, 0), 3f, WINDUP, 0x6FBF4A);
            case THIET_GIAP_TRI_CHU -> Fx.telegraphCircle(sl, null, t.position().add(0, 0.05, 0), 2.5f, WINDUP, 0xD8D8D8);
            case NGAN_LAN_MANG -> Fx.telegraphLine(sl, null, at, t.position().subtract(m.position()), 1.2f, WINDUP, c);
            case THU_YEU -> Fx.telegraphCircle(sl, null, t.position().add(0, 0.05, 0), 3.5f * YeuThuSize.skillK(m), WINDUP, 0x6A8A3A);
            case THUC_NHAN_HOA, DANG_XA -> Fx.telegraphLine(sl, null, at, t.position().subtract(m.position()), 1.0f, WINDUP, 0x4FBF4A);
            case QUY_CO -> Fx.telegraphCircle(sl, null, m.position().add(0, 0.05, 0), 4.5f, WINDUP, 0xC070C0);
            case U_ANH_BAO -> { m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, WINDUP + 4, 0, false, false));
                sl.sendParticles(ParticleTypes.SMOKE, m.getX(), m.getY() + 0.6, m.getZ(), 20, 0.4, 0.4, 0.4, 0.02); }
            default -> { }
        }
        m.getNavigation().stop();
        m.triggerAnim("attack", "skill");
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), switch (m.kind()) {
            case XICH_VAN_HO, HUYET_NHA_TRU -> SoundEvents.RAVAGER_STEP;
            case U_ANH_BAO -> SoundEvents.PHANTOM_FLAP;
            case BAO_VIEM_TICH -> SoundEvents.BLAZE_AMBIENT;
            case THIET_GIAP_TRI_CHU -> SoundEvents.SPIDER_AMBIENT;
            default -> SoundEvents.EVOKER_PREPARE_ATTACK;
        }, SoundSource.HOSTILE, 1f, 0.6f);
    }

    private static float atk(LinhQuai m) {
        var a = m.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        return a == null ? 6f : (float) a.getValue();
    }

    private static List<LivingEntity> victims(ServerLevel sl, LinhQuai m, Vec3 c, double r) {
        return sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, 3, r),
                e -> e != m && e.isAlive() && (e instanceof Player p ? !p.isSpectator() && !p.isCreative() : e == m.getTarget() || e == m.skillTarget)
                        && e.position().distanceToSqr(c) <= r * r);
    }

    private static void hit(LinhQuai m, LivingEntity e, float mult) {
        e.hurt(m.damageSources().mobAttack(m), atk(m) * mult);
    }

    static void cast(ServerLevel sl, LinhQuai m, LivingEntity t, YeuThuAge age) {
        m.skillTarget = t;
        Vec3 face = t.position().subtract(m.position()).multiply(1, 0, 1);
        face = face.lengthSqr() < 1e-4 ? m.getLookAngle().multiply(1, 0, 1).normalize() : face.normalize();
        switch (m.kind()) {
            case XICH_VAN_HO -> roar(sl, m, face);
            case HAC_GIAP_TE -> { m.charge = 14; m.chargeDir = face.scale(0.9); m.chargeHits.clear(); }
            case HUYET_NHA_TRU -> { m.charge = 8; m.chargeDir = face.scale(1.0); m.chargeHits.clear(); m.chargeLeft = 2; }
            case CUONG_LUC_VIEN -> leapSlam(sl, m, t);
            case BICH_NHAN_THIEM -> poison(sl, m, t);
            case NGAN_LAN_MANG -> constrict(sl, m, t);
            case U_ANH_BAO -> ambush(sl, m, t);
            case THIET_GIAP_TRI_CHU -> web(sl, m, t);
            case BAO_VIEM_TICH -> breath(sl, m, face);
            case THU_YEU -> roots(sl, m, t);
            case THUC_NHAN_HOA -> vinePull(sl, m, t);
            case DANG_XA -> { constrict(sl, m, t); t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)); }
            case QUY_CO -> spores(sl, m);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- Yêu Thú thực vật (2026-11-06)

    /** Thụ Yêu: rễ trồi lên quanh mục tiêu, trói chặt 2 giây. */
    private static void roots(ServerLevel sl, LinhQuai m, LivingEntity t) {
        float k = YeuThuSize.skillK(m);
        for (LivingEntity e : victims(sl, m, t.position(), 3.5 * k)) {
            hit(m, e, 1.2f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
            e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
            e.hurtMarked = true;
        }
        int n = (int) (10 * k);
        for (int i = 0; i < n; i++) { // rễ nhọn đâm lên thành cột, xoắn nhẹ
            double a = i * Math.PI * 2 / n, r = (1.2 + (i % 3) * 0.8) * k;
            double x = t.getX() + Math.cos(a) * r, z = t.getZ() + Math.sin(a) * r;
            int hgt = (int) (5 * k);
            for (int y = 0; y < hgt; y++) {
                double tw = y * 0.15;
                sl.sendParticles(dust(y < hgt - 1 ? 0x6B4A2A : 0x8FD45A, 1.4f), x + Math.cos(a + tw) * 0.15, t.getY() + y * 0.3, z + Math.sin(a + tw) * 0.15, 1, 0.03, 0, 0.03, 0);
            }
        }
        if (k > 1.5) { Fx.groundCrack(sl, null, t.position().add(0, 0.06, 0), 3.5f * k, 0x6B4A2A, 10, 40); Fx.shake(sl, t.position(), 0.6f, 10, 12 * k); }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), t.getX(), t.getY() + 0.2, t.getZ(), (int) (40 * k), 1.5 * k, 0.2, 1.5 * k, 0.1);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    /** Thực Nhân Hoa: dây leo quấn kéo mục tiêu về rồi cắn (hút máu). */
    private static void vinePull(ServerLevel sl, LinhQuai m, LivingEntity t) {
        for (int i = 0; i < 14; i++) {
            Vec3 p = m.getEyePosition().lerp(t.position().add(0, 0.8, 0), i / 14.0);
            sl.sendParticles(dust(0x3F9F3A, 1.0f), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        Vec3 pull = m.position().subtract(t.position()).multiply(1, 0, 1);
        if (pull.lengthSqr() > 1e-4) {
            t.setDeltaMovement(pull.normalize().scale(Math.min(1.6, pull.length() * 0.25)).add(0, 0.25, 0));
            t.hurtMarked = true;
        }
        hit(m, t, 1.1f);
        m.heal(atk(m) * 0.5f);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    /** Quỷ Cô: phun đám bào tử quanh mình (độc + mù nhẹ). */
    private static void spores(ServerLevel sl, LinhQuai m) {
        AreaEffectCloud c = new AreaEffectCloud(sl, m.getX(), m.getY(), m.getZ());
        c.setOwner(m);
        c.setRadius(4.5f);
        c.setDuration(100);
        c.setRadiusPerTick(0);
        c.setWaitTime(0);
        c.setParticle(dust(0xC070C0, 1.3f));
        c.addEffect(new MobEffectInstance(MobEffects.POISON, 60, Math.min(2, m.age().ordinal() / 2)));
        c.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
        sl.addFreshEntity(c);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, m.getX(), m.getY() + 1.5, m.getZ(), 60, 2.5, 1, 2.5, 0.02);
        for (LivingEntity e : victims(sl, m, m.position(), 4.5)) hit(m, e, 0.6f);
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    // ---------------------------------------------------------------- chiêu

    private static void roar(ServerLevel sl, LinhQuai m, Vec3 face) {
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.8f);
        for (int ring = 1; ring <= 3; ring++) {
            double rr = ring * 2.2;
            for (int i = -6; i <= 6; i++) {
                double a = Math.atan2(face.z, face.x) + Math.toRadians(i * 6);
                sl.sendParticles(dust(0xD9C7A8, 1.4f), m.getX() + Math.cos(a) * rr, m.getEyeY() - 0.3, m.getZ() + Math.sin(a) * rr, 1, 0, 0.1, 0, 0);
            }
        }
        for (LivingEntity e : victims(sl, m, m.position(), 7)) {
            Vec3 d = e.position().subtract(m.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.01 && d.normalize().dot(face) < Math.cos(Math.toRadians(35))) continue;
            hit(m, e, 1.2f);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    }

    private static void chargeTick(ServerLevel sl, LinhQuai m) {
        m.charge--;
        m.setDeltaMovement(m.chargeDir.x, m.getDeltaMovement().y, m.chargeDir.z);
        m.hurtMarked = true;
        m.setYRot((float) Math.toDegrees(Math.atan2(-m.chargeDir.x, m.chargeDir.z)));
        BlockPos below = m.blockPosition().below();
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sl.getBlockState(below)), m.getX(), m.getY() + 0.1, m.getZ(), 4, 0.4, 0.05, 0.4, 0.1);
        boolean boar = m.kind() == CreatureKind.HUYET_NHA_TRU;
        for (LivingEntity e : victims(sl, m, m.position(), m.getBbWidth() * 0.8 + 0.8)) {
            if (!m.chargeHits.add(e.getId())) continue;
            hit(m, e, boar ? 0.8f : 1.6f);
            e.knockback(boar ? 0.6 : 1.6, -m.chargeDir.x, -m.chargeDir.z);
            e.push(0, boar ? 0.15 : 0.4, 0);
            e.hurtMarked = true;
            if (boar) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            sl.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 0.8f, 0.7f);
        }
        if (m.charge == 0 && boar && m.chargeLeft > 0) { // lao tiếp, chỉnh hướng theo mục tiêu
            m.chargeLeft--;
            LivingEntity t = m.getTarget();
            if (t != null) m.chargeDir = t.position().subtract(m.position()).multiply(1, 0, 1).normalize().scale(1.0);
            m.charge = 8;
            m.chargeHits.clear();
        }
    }

    private static void leapSlam(ServerLevel sl, LinhQuai m, LivingEntity t) {
        Vec3 land = t.position();
        m.teleportTo(land.x, land.y, land.z);
        sl.playSound(null, land.x, land.y, land.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9f, 0.6f);
        Fx.groundCrack(sl, null, land.add(0, 0.05, 0), 4f, 0x6B5A48, 8, 30);
        Fx.shake(sl, land, 0.5f, 10, 14);
        BlockPos below = BlockPos.containing(land).below();
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sl.getBlockState(below)), land.x, land.y + 0.2, land.z, 40, 2, 0.2, 2, 0.15);
        for (LivingEntity e : victims(sl, m, land, 4)) {
            hit(m, e, 1.4f);
            e.push(0, 0.8, 0);
            e.hurtMarked = true;
        }
    }

    private static void poison(ServerLevel sl, LinhQuai m, LivingEntity t) {
        AreaEffectCloud c = new AreaEffectCloud(sl, t.getX(), t.getY(), t.getZ());
        c.setOwner(m);
        c.setRadius(3f);
        c.setDuration(120);
        c.setRadiusPerTick(0);
        c.setWaitTime(0);
        c.setParticle(dust(0x6FBF4A, 1.2f));
        c.addEffect(new MobEffectInstance(MobEffects.POISON, 60, Math.min(2, m.age().ordinal() / 2)));
        c.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        sl.addFreshEntity(c);
        for (int i = 0; i < 10; i++) {
            double k = i / 10.0;
            Vec3 p = m.getEyePosition().lerp(t.position().add(0, 0.5, 0), k);
            sl.sendParticles(dust(0x8FD45A, 1.0f), p.x, p.y + Math.sin(k * Math.PI) * 1.5, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 1f, 0.6f);
        hit(m, t, 0.6f);
    }

    private static void constrict(ServerLevel sl, LinhQuai m, LivingEntity t) {
        Vec3 front = m.position().add(m.getLookAngle().multiply(1, 0, 1).normalize().scale(m.getBbWidth() * 0.6 + 0.8));
        if (t instanceof net.minecraft.server.level.ServerPlayer sp) sp.connection.teleport(front.x, t.getY(), front.z, sp.getYRot(), sp.getXRot());
        else t.teleportTo(front.x, t.getY(), front.z);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 6));
        hit(m, t, 1.3f);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 6, y = i * 0.08;
            sl.sendParticles(dust(0xC8D2E0, 1.1f), t.getX() + Math.cos(a) * 0.7, t.getY() + y, t.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
        }
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.HOSTILE, 1f, 0.5f);
    }

    private static void ambush(ServerLevel sl, LinhQuai m, LivingEntity t) {
        Vec3 back = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 0.5, m.getZ(), 12, 0.3, 0.4, 0.3, 0.01);
        m.teleportTo(back.x, t.getY(), back.z);
        m.lookAt(t, 180, 180);
        m.removeEffect(MobEffects.INVISIBILITY);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, back.x, t.getY() + 0.5, back.z, 12, 0.3, 0.4, 0.3, 0.01);
        sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + 1, t.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
        hit(m, t, 2.0f);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1f, 0.7f);
    }

    /** Mạng nhện tạm: vị trí + tick hết hạn (gỡ khi hết hạn, chỉ gỡ nếu vẫn là mạng nhện). */
    private record Web(ServerLevel level, BlockPos pos, long until) {}

    private static final List<Web> WEBS = new ArrayList<>();

    private static void web(ServerLevel sl, LinhQuai m, LivingEntity t) {
        for (int i = 0; i < 12; i++) {
            Vec3 p = m.getEyePosition().lerp(t.position().add(0, 0.8, 0), i / 12.0);
            sl.sendParticles(dust(0xF0F0F0, 0.7f), p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        BlockPos c = t.blockPosition();
        int placed = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) {
            if ((dx != 0 || dz != 0) && (Math.abs(dx) + Math.abs(dz) == 2 || dy == 1)) continue;
            BlockPos p = c.offset(dx, dy, dz);
            if (!sl.getBlockState(p).isAir()) continue;
            sl.setBlock(p, Blocks.COBWEB.defaultBlockState(), 3);
            WEBS.add(new Web(sl, p, sl.getGameTime() + 80));
            placed++;
        }
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        hit(m, t, 0.5f);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.SPIDER_HURT, SoundSource.HOSTILE, 1f, 0.5f);
        if (placed == 0) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4));
    }

    private static void breath(ServerLevel sl, LinhQuai m, Vec3 face) {
        sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2f, 0.7f);
        Vec3 mouth = m.getEyePosition();
        for (int i = 0; i < 40; i++) {
            double a = Math.atan2(face.z, face.x) + Math.toRadians((m.getRandom().nextDouble() - 0.5) * 60), sp = 0.25 + m.getRandom().nextDouble() * 0.25;
            sl.sendParticles(m.getRandom().nextInt(3) == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, mouth.x, mouth.y - 0.2, mouth.z, 0,
                    Math.cos(a), -0.05, Math.sin(a), sp);
        }
        for (LivingEntity e : victims(sl, m, m.position(), 6)) {
            Vec3 d = e.position().subtract(m.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 0.01 && d.normalize().dot(face) < Math.cos(Math.toRadians(30))) continue;
            hit(m, e, 1.1f);
            e.igniteForSeconds(4 + m.age().ordinal());
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (WEBS.isEmpty()) return;
        for (Iterator<Web> it = WEBS.iterator(); it.hasNext(); ) {
            Web w = it.next();
            if (w.level.getGameTime() < w.until) continue;
            if (w.level.getBlockState(w.pos).is(Blocks.COBWEB)) w.level.setBlock(w.pos, Blocks.AIR.defaultBlockState(), 3);
            it.remove();
        }
    }
}
