package com.hukhong.pathrules;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Thân thể Thể Tu: đi trên mặt nước (giữ Shift để lặn), nín thở x5, chạy nhanh hơn, nhảy cao hơn, giảm sát thương rơi.
 * Đi trên nước chạy ở cả client (người chơi tự điều khiển chuyển động) lẫn server.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TheBody {
    private TheBody() {}

    /** Nín thở lâu gấp BREATH_MULT lần. */
    public static final int BREATH_MULT = 5;
    /** Tốc chạy: +SPEED_BASE, thêm SPEED_PER_LV mỗi cấp, trần SPEED_MAX (nhân base). */
    public static final double SPEED_BASE = 0.10, SPEED_PER_LV = 0.001, SPEED_MAX = 0.20;
    /** Lực nhảy: +JUMP_BASE .. JUMP_MAX theo cấp (0.42 vanilla -> ~1.9-2.6 khối). */
    public static final double JUMP_BASE = 0.20, JUMP_PER_LV = 0.002, JUMP_MAX = 0.40;
    public static final double SAFE_FALL = 4.0;

    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "the_body_speed");
    private static final ResourceLocation JUMP_ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "the_body_jump");
    private static final ResourceLocation FALL_ID = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "the_body_fall");

    public static double speedBonus(int level) { return Math.min(SPEED_MAX, SPEED_BASE + SPEED_PER_LV * Math.max(0, level - 1)); }
    public static double jumpBonus(int level) { return Math.min(JUMP_MAX, JUMP_BASE + JUMP_PER_LV * Math.max(0, level - 1)); }

    static boolean isThe(Player p) {
        CultivationData d = p.level().isClientSide ? com.hukhong.client.ClientCache.cultivation() : p.getData(ModAttachments.CULTIVATION);
        return d != null && d.paths().contains(Path.THE);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Pre e) {
        Player p = e.getEntity();
        boolean the = isThe(p);
        if (the || phapSprint(p)) walkOnWater(p); // The tu, hoac Phap Quang/Loi dang giu chay toc (Thiem Bo / Loi Tran)
        if (p instanceof ServerPlayer sp && sp.tickCount % 20 == 0) {
            int lv = sp.getData(ModAttachments.CULTIVATION).level();
            set(sp, Attributes.MOVEMENT_SPEED, SPEED_ID, the ? speedBonus(lv) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            set(sp, Attributes.JUMP_STRENGTH, JUMP_ID, the ? jumpBonus(lv) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            set(sp, Attributes.SAFE_FALL_DISTANCE, FALL_ID, the ? SAFE_FALL : 0, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    static boolean phapSprint(Player p) {
        if (p.level().isClientSide) return com.hukhong.client.fx.ThiemBoClient.sprintingLocal(p);
        return p instanceof ServerPlayer sp && com.hukhong.phaptu.ThiemBoService.sprinting(sp);
    }

    /** Đứng/chạy trên mặt nước: chân chạm mặt nước, đầu không ngập, không giữ Shift, không bơi/bay. */
    static void walkOnWater(Player p) {
        if (p.isShiftKeyDown() || p.isSpectator() || p.getAbilities().flying || p.isPassenger() || p.isFallFlying()) return;
        BlockPos feet = BlockPos.containing(p.getX(), p.getY() + 0.05, p.getZ());
        boolean feetWater = p.level().getFluidState(feet).is(FluidTags.WATER);
        boolean belowWater = p.level().getFluidState(feet.below()).is(FluidTags.WATER);
        if (p.level().getFluidState(feet.above()).is(FluidTags.WATER)) return; // đã lặn sâu: để bơi bình thường
        Vec3 v = p.getDeltaMovement();
        if (feetWater) {
            // vừa chạm/chìm nhẹ: đẩy lên mặt nước
            double surface = feet.getY() + p.level().getFluidState(feet).getHeight(p.level(), feet);
            if (p.getY() < surface) {
                p.setPos(p.getX(), surface, p.getZ());
                p.setDeltaMovement(v.x, Math.max(0, v.y), v.z);
                p.setOnGround(true);
                p.resetFallDistance();
            }
        } else if (belowWater && v.y <= 0 && p.getY() - feet.getY() < 0.05) {
            p.setDeltaMovement(v.x, 0, v.z);
            p.setOnGround(true);
            p.resetFallDistance();
        }
    }

    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent e) {
        if (!(e.getEntity() instanceof Player p) || e.canBreathe() || !isThe(p)) return;
        if (p.tickCount % BREATH_MULT != 0) e.setConsumeAirAmount(0);
    }

    private static void set(ServerPlayer p, Holder<Attribute> a, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) { if (cur != null) inst.removeModifier(id); return; }
        if (cur != null && cur.amount() == amount) return;
        inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
    }
}
