package com.hukhong.mixin;

import com.hukhong.currency.SpiritStoneToll;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ban (anvil): phi = cap vanilla x SpiritStoneToll.ANVIL_PER_LEVEL vien Ha Pham Linh Thach, thay cho XP level. */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void hukhong$tollPickup(Player player, boolean hasStack, CallbackInfoReturnable<Boolean> cir) {
        int levels = ((AnvilMenu) (Object) this).getCost();
        if (levels <= 0) { cir.setReturnValue(false); return; }
        if (player.hasInfiniteMaterials()) { cir.setReturnValue(true); return; }
        int need = SpiritStoneToll.anvilCost(levels);
        boolean ok = SpiritStoneToll.canPay(player, need);
        if (!ok && player instanceof ServerPlayer sp) SpiritStoneToll.notifyNeed(sp, need);
        cir.setReturnValue(ok);
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void hukhong$tollTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player.getAbilities().instabuild) return;
        SpiritStoneToll.pay(player, SpiritStoneToll.anvilCost(((AnvilMenu) (Object) this).getCost()));
    }
}
