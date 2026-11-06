package com.hukhong.mixin;

import com.hukhong.currency.SpiritStoneToll;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

/**
 * Phong phap: thay kiem tra XP level (2 lan doc experienceLevel trong clickMenuButton) bang kiem tra linh thach,
 * va tru linh thach khi phong phap thanh cong (cung cho onEnchantmentPerformed, vi tri vanilla tru XP).
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void hukhong$tollBegin(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        SpiritStoneToll.beginEnchant(id);
    }

    @Redirect(method = "clickMenuButton",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;experienceLevel:I", opcode = Opcodes.GETFIELD),
            require = 2)
    private static int hukhong$tollGate(Player player) {
        return SpiritStoneToll.enchantGate(player);
    }

    @Redirect(method = "lambda$clickMenuButton$1",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onEnchantmentPerformed(Lnet/minecraft/world/item/ItemStack;I)V"),
            require = 1)
    private static void hukhong$tollCharge(Player player, ItemStack stack, int levels) {
        SpiritStoneToll.chargeEnchant(player, levels);
        player.onEnchantmentPerformed(stack, levels);
    }
}
