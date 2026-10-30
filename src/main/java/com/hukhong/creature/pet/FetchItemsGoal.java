package com.hukhong.creature.pet;

import com.hukhong.creature.LinhThu;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.EnumSet;

/** Ngoc Tho (linh sung): chay toi do roi gan chu, nhat roi chuyen thang vao tui chu (day tui thi tha duoi chan chu). */
public class FetchItemsGoal extends Goal {
    private final LinhThu pet;
    private ItemEntity target;
    private int cooldown;

    public FetchItemsGoal(LinhThu pet) {
        this.pet = pet;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (!pet.isTame() || pet.isOrderedToSit() || !(pet.getOwner() instanceof Player owner)) return false;
        if (cooldown > 0) { cooldown--; return false; }
        cooldown = 10;
        target = pet.level().getEntitiesOfClass(ItemEntity.class, owner.getBoundingBox().inflate(10),
                        i -> PetMath.pickable(i.getItem().isEmpty(), i.hasPickUpDelay() ? 1 : 0, i.getAge()))
                .stream().min(Comparator.comparingDouble(pet::distanceToSqr)).orElse(null);
        return target != null;
    }

    @Override public boolean canContinueToUse() { return target != null && target.isAlive() && pet.isTame() && !pet.isOrderedToSit(); }

    @Override public void stop() { target = null; pet.getNavigation().stop(); }

    @Override public void tick() {
        if (target == null) return;
        pet.getNavigation().moveTo(target, 1.3);
        if (pet.distanceToSqr(target) < 2.5 && pet.getOwner() instanceof Player owner) {
            ItemStack s = target.getItem().copy();
            target.discard();
            if (!owner.getInventory().add(s)) owner.drop(s, false);
            target = null;
        }
    }
}
