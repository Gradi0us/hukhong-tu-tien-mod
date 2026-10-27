package com.hukhong.client.render.orbit;

import com.hukhong.HuKhong;
import com.hukhong.banmenh.BanMenhKiemItem;
import com.hukhong.banmenh.BanMenhMath;
import com.hukhong.banmenh.BanMenhStrikePayload;
import com.hukhong.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Chuột trái khi cầm Bản Mệnh Kiếm: raycast tầm phi kiếm (8-12 block, bị block chắn), nếu trúng sinh vật thì thay
 * đòn vanilla bằng phi kiếm (server áp sát thương sau ~6 tick). Không trúng gì thì giữ hành vi vanilla (đào/vung).
 */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class OrbitInput {
    private OrbitInput() {}

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered e) {
        if (!e.isAttack() || e.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.screen != null || p.isSpectator()) return;
        ItemStack s = p.getMainHandItem();
        if (!s.is(ModItems.BAN_MENH_KIEM.get()) || BanMenhKiemItem.typeOf(s) == null || !BanMenhKiemItem.isOwner(s, p)) return;

        double range = BanMenhMath.strikeRange(BanMenhKiemItem.stageOf(s));
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        HitResult block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double maxSq = block.getType() == HitResult.Type.MISS ? range * range : block.getLocation().distanceToSqr(eye);
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                ent -> !ent.isSpectator() && ent.isPickable() && ent instanceof LivingEntity, maxSq);
        if (hit == null) return;
        e.setCanceled(true); // vẫn vung tay (setSwingHand mặc định true)
        PacketDistributor.sendToServer(new BanMenhStrikePayload(hit.getEntity().getId()));
    }
}
