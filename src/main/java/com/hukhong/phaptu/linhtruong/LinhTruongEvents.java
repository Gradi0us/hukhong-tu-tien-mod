package com.hukhong.phaptu.linhtruong;

import com.hukhong.HuKhong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/** Ràng buộc chủ, không vứt / không cất vào vật chứa / không rơi khi chết, tự khôi phục; đòn thường = tia phép. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class LinhTruongEvents {
    private LinhTruongEvents() {}

    private static boolean isStaff(ItemStack s) { return s.is(LinhTruongRegistry.LINH_TRUONG.get()); }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity() instanceof ServerPlayer) e.getDrops().removeIf(ie -> isStaff(ie.getItem()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LinhTruongService.forget(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { LinhTruongService.clear(); }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) LinhTruongService.grant(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !e.isEndConquered()) LinhTruongService.grant(p);
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre e) {
        ItemStack s = e.getItemEntity().getItem();
        if (isStaff(s) && !LinhTruongItem.isOwner(s, e.getPlayer())) e.setCanPickup(TriState.FALSE);
    }

    /** Phím Q / kéo ra ngoài GUI: hủy việc vứt, trả trượng vào túi. */
    @SubscribeEvent
    public static void onToss(ItemTossEvent e) {
        ItemStack s = e.getEntity().getItem();
        if (!isStaff(s) || !(e.getPlayer() instanceof ServerPlayer p) || !p.isAlive()) return;
        ItemStack copy = s.copy();
        var inv = p.getInventory();
        e.setCanceled(true);
        if (inv.getItem(inv.selected).isEmpty()) inv.setItem(inv.selected, copy);
        else if (!inv.add(copy)) {
            if (p.containerMenu.getCarried().isEmpty()) p.containerMenu.setCarried(copy);
            else e.setCanceled(false);
        }
        p.containerMenu.sendAllDataToRemote();
        p.displayClientMessage(Component.translatableWithFallback("hukhong.linhtruong.msg.no_drop",
                "Linh Trượng gắn liền với chủ nhân, không thể vứt bỏ."), true);
    }

    /** Lưới cuối: mọi ItemEntity chứa Linh Trượng bị hủy khi xuất hiện, trả cho chủ nếu online. */
    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof ItemEntity ie)) return;
        ItemStack s = ie.getItem();
        if (!isStaff(s)) return;
        UUID owner = s.get(com.hukhong.item.ModItems.BOUND_TO.get());
        if (owner == null) return;
        e.setCanceled(true);
        var server = e.getLevel().getServer();
        ServerPlayer p = server == null ? null : server.getPlayerList().getPlayer(owner);
        if (p != null && p.isAlive() && !LinhTruongService.hasStaff(p)) {
            ItemStack back = s.copy();
            if (!p.getInventory().add(back) && p.containerMenu.getCarried().isEmpty()) p.containerMenu.setCarried(back);
            p.containerMenu.sendAllDataToRemote();
        }
    }

    /** Định kỳ: Pháp Tu thiếu trượng -> tái tạo; không còn là Pháp Tu -> thu hồi trượng của mình; dọn ItemEntity sót. */
    @SubscribeEvent
    public static void onRecoveryTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 11 || !p.isAlive() || p.isSpectator()) return;
        if (LinhTruongService.isPhap(p)) {
            LinhTruongService.grant(p);
            LinhTruongService.discardDropped(p, 16);
        } else {
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++)
                if (LinhTruongService.isStaff(inv.getItem(i), p.getUUID())) inv.setItem(i, ItemStack.EMPTY);
        }
    }

    /** Mỗi 2 tick quét ô ngoài túi người chơi: trượng lạc vào vật chứa bị trả về túi. */
    @SubscribeEvent
    public static void onContainerScan(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 2 != 0 || p.isCreative()) return;
        var menu = p.containerMenu;
        for (var slot : menu.slots) {
            if (slot.container == p.getInventory() || !isStaff(slot.getItem())) continue;
            ItemStack s = slot.getItem().copy();
            if (p.getInventory().add(s)) {
                slot.set(ItemStack.EMPTY);
                slot.setChanged();
            } else if (menu.getCarried().isEmpty()) {
                slot.set(ItemStack.EMPTY);
                menu.setCarried(s);
            }
        }
    }

    /** Đòn đánh trực tiếp bằng trượng (client không chặn): hủy đòn cận chiến 2 sát thương, đổi thành tia phép. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isStaff(p.getMainHandItem())) return;
        e.setCanceled(true);
        LinhTruongService.fire(p);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (!isStaff(e.getItemStack())) return;
        var t = e.getTarget();
        if (t instanceof net.minecraft.world.entity.decoration.ItemFrame || t instanceof net.minecraft.world.entity.animal.allay.Allay) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific e) {
        if (isStaff(e.getItemStack()) && e.getTarget() instanceof net.minecraft.world.entity.decoration.ArmorStand) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onPotInteract(PlayerInteractEvent.RightClickBlock e) {
        if (!isStaff(e.getItemStack())) return;
        if (e.getLevel().getBlockState(e.getPos()).getBlock() instanceof net.minecraft.world.level.block.DecoratedPotBlock) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
