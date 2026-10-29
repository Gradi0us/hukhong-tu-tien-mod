package com.hukhong.phaptu.linhtruong;

import com.hukhong.artifact.ArtifactService;
import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.client.anim.CastAnim;
import com.hukhong.client.anim.CastAnimHook;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import com.hukhong.item.ModItems;
import com.hukhong.phaptu.PhapElement;
import com.hukhong.phaptu.PhapRuntime;
import com.hukhong.phaptu.PhapTu;
import com.hukhong.skillbook.SkillBookRuntime;
import com.hukhong.stats.StatService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cấp / khôi phục Linh Trượng và tia phép đòn thường. Giai đoạn = Pháp Khí (ArtifactData.phap), trần theo cảnh giới. */
public final class LinhTruongService {
    private LinhTruongService() {}

    private static final Map<UUID, Long> READY = new HashMap<>();

    // ------------------------------------------------------------------ cấp / khôi phục

    public static boolean isPhap(ServerPlayer p) {
        return p.getData(ModAttachments.CULTIVATION).paths().contains(Path.PHAP);
    }

    /** Giai đoạn hiệu lực (0..6): Pháp Khí đã lưu, không vượt trần cảnh giới. */
    public static int stage(ServerPlayer p) {
        return BanMenhMath.effectiveStage(ArtifactService.get(p).phap(), p.getData(ModAttachments.CULTIVATION).level());
    }

    public static ItemStack create(ServerPlayer p) {
        ItemStack s = new ItemStack(LinhTruongRegistry.LINH_TRUONG.get());
        PhapElement el = PhapTu.element(p);
        if (el != null) {
            s.set(ModItems.BAN_MENH_TYPE.get(), el.id());
            s.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(el.ordinal() + 1));
        }
        s.set(ModItems.BAN_MENH_STAGE.get(), stage(p));
        s.set(ModItems.BOUND_TO.get(), p.getUUID());
        return s;
    }

    public static boolean isStaff(ItemStack s, UUID owner) {
        return s.is(LinhTruongRegistry.LINH_TRUONG.get()) && owner.equals(s.get(ModItems.BOUND_TO.get()));
    }

    public static boolean hasStaff(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (isStaff(inv.getItem(i), p.getUUID())) return true;
        if (isStaff(p.containerMenu.getCarried(), p.getUUID())) return true;
        for (var slot : p.containerMenu.slots) if (isStaff(slot.getItem(), p.getUUID())) return true;
        return false;
    }

    /** Pháp Tu chọn đạo / đăng nhập / hồi sinh: trao trượng nếu chưa có. Túi đầy thì chờ vòng kiểm tra định kỳ. */
    public static boolean grant(ServerPlayer p) {
        if (!isPhap(p) || PhapTu.element(p) == null || hasStaff(p)) return false;
        return p.getInventory().add(create(p));
    }

    public static void syncStack(ServerPlayer p, ItemStack stack) {
        int st = stage(p);
        if (LinhTruongItem.stageOf(stack) != st) stack.set(ModItems.BAN_MENH_STAGE.get(), st);
        PhapElement el = PhapTu.element(p);
        if (el != null && el != LinhTruongItem.elementOf(stack)) {
            stack.set(ModItems.BAN_MENH_TYPE.get(), el.id());
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(el.ordinal() + 1));
        }
    }

    /** Trượng hợp lệ ở tay chính (đúng chủ, Pháp Tu có nguyên tố), hoặc rỗng. */
    public static ItemStack held(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (!isStaff(s, p.getUUID()) || !isPhap(p) || PhapTu.element(p) == null) return ItemStack.EMPTY;
        return s;
    }

    public static void forget(UUID id) { READY.remove(id); }

    public static void clear() { READY.clear(); }

    // ------------------------------------------------------------------ đầu trượng

    /** Đầu trượng nếu đang cầm Linh Trượng ở tay chính, ngược lại null (kỹ năng Pháp phát ra từ đây). */
    public static Vec3 tipIfHeld(ServerPlayer p) {
        if (held(p).isEmpty()) return null;
        return LinhTruongMath.tip(p.getEyePosition(), p.getYRot(), p.getXRot());
    }

    // ------------------------------------------------------------------ tia phép

    /** Bắn tia đòn thường. Trả true nếu đã bắn (qua kiểm tra hồi chiêu). */
    public static boolean fire(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || !(p.level() instanceof ServerLevel lv)) return false;
        ItemStack staff = held(p);
        if (staff.isEmpty()) return false;
        long now = lv.getGameTime();
        Long ready = READY.get(p.getUUID());
        if (ready != null && now < ready) return false;
        READY.put(p.getUUID(), now + LinhTruongMath.COOLDOWN_TICKS);

        PhapElement el = PhapTu.element(p);
        int stage = stage(p);
        float dmg = LinhTruongMath.boltDamage(StatService.spellPower(p), stage);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 tip = LinhTruongMath.tip(eye, p.getYRot(), p.getXRot());
        Vec3 end = eye.add(look.scale(LinhTruongMath.RANGE));
        HitResult bh = lv.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();

        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        AABB box = new AABB(eye, end).inflate(LinhTruongMath.BEAM_WIDTH + 1);
        for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, box, x -> PhapRuntime.valid(p, x))) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(LinhTruongMath.BEAM_WIDTH).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceToSqr(hit.get());
            if (d < best) { best = d; target = e; }
        }
        Vec3 impact = end;
        if (target != null) {
            impact = target.getBoundingBox().getCenter();
            target.invulnerableTime = 0;
            SkillBookRuntime.hurt(p, target, dmg);
        }
        castFx(lv, p, el, tip, impact, stage, target != null);
        CastAnimHook.send(p, CastAnim.SWORD_THRUST);
        return true;
    }

    // ------------------------------------------------------------------ VFX

    private static DustParticleOptions dust(PhapElement el, float size) {
        return new DustParticleOptions(new Vector3f(((el.rgb >> 16) & 255) / 255f, ((el.rgb >> 8) & 255) / 255f, (el.rgb & 255) / 255f), size);
    }

    private static ParticleOptions flavor(PhapElement el) {
        return switch (el) {
            case QUANG -> ParticleTypes.END_ROD;
            case LOI -> ParticleTypes.ELECTRIC_SPARK;
            case HOA -> ParticleTypes.FLAME;
            case THUY -> ParticleTypes.SPLASH;
            case THO -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case KIM -> ParticleTypes.CRIT;
        };
    }

    private static SoundEvent castSound(PhapElement el) {
        return switch (el) {
            case QUANG -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case LOI -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case HOA -> SoundEvents.FIRECHARGE_USE;
            case THUY -> SoundEvents.PLAYER_SPLASH;
            case THO -> SoundEvents.GRAVEL_BREAK;
            case KIM -> SoundEvents.IRON_GOLEM_ATTACK;
        };
    }

    private static void castFx(ServerLevel lv, ServerPlayer p, PhapElement el, Vec3 tip, Vec3 impact, int stage, boolean hit) {
        int col = el.rgb;
        Vec3 dir = impact.subtract(tip);
        double len = Math.max(0.5, dir.length());
        Vec3 n = dir.scale(1.0 / len);
        // nạp lực: hạt tụ + chớp trắng nhỏ tại đầu trượng, rune nhỏ
        lv.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, 3, 0.05, 0.05, 0.05, 0.02);
        lv.sendParticles(dust(el, 1.4f), tip.x, tip.y, tip.z, 8 + stage, 0.12, 0.12, 0.12, 0.02);
        Fx.rune(lv, p, tip, 0.45f + 0.05f * stage, col, 8, 6);
        // vệt tia: dải sáng + chùm hạt dọc đường
        Fx.ribbon(lv, p, tip, impact, 0, 3, 0, 0.10f + 0.015f * stage, col);
        Fx.play(lv, FxType.BEAM, tip, n, (float) len, col, 5);
        for (double t = 0; t <= len; t += 0.9) {
            Vec3 q = tip.add(n.scale(t));
            lv.sendParticles(dust(el, 0.9f), q.x, q.y, q.z, 1, 0.04, 0.04, 0.04, 0);
            if (((int) (t / 0.9)) % 2 == 0) lv.sendParticles(flavor(el), q.x, q.y, q.z, 1, 0.06, 0.06, 0.06, 0.01);
        }
        // va chạm: sóng nhỏ + hạt nguyên tố
        Fx.play(lv, FxType.SHOCKWAVE, impact, Vec3.ZERO, hit ? 1.5f + 0.1f * stage : 0.8f, col, 8);
        lv.sendParticles(flavor(el), impact.x, impact.y, impact.z, hit ? 14 : 5, 0.25, 0.25, 0.25, 0.08);
        lv.sendParticles(dust(el, 1.2f), impact.x, impact.y, impact.z, hit ? 12 : 4, 0.3, 0.3, 0.3, 0.05);
        lv.playSound(null, tip.x, tip.y, tip.z, castSound(el), SoundSource.PLAYERS, 0.5f, 1.5f);
        lv.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.2f + 0.1f * stage);
    }

    /** Dọn bản trượng rơi trong thế giới quanh người chơi (summon/khôi phục không nhân đôi). */
    public static void discardDropped(ServerPlayer p, double r) {
        for (ItemEntity ie : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(r),
                x -> isStaff(x.getItem(), p.getUUID()))) ie.discard();
    }
}
