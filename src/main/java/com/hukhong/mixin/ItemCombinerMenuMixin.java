package com.hukhong.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift-click o ket qua anvil di qua quickMoveStack (khong qua mayPickup): chan khi chua du linh thach. */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Shadow protected abstract boolean mayPickup(Player player, boolean hasStack);

    @Shadow public abstract int getResultSlot();

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void hukhong$tollQuickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof AnvilMenu)) return;
        if (index != getResultSlot()) return;
        if (!mayPickup(player, true)) cir.setReturnValue(ItemStack.EMPTY);
    }
}
