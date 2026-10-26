package com.hukhong.gates;

import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/** Luật sát thương theo đạo. Kỹ năng (đi qua hurtBySkill) không bị chặn. */
public final class DamageRules {
    private DamageRules() {}

    private static boolean skillActive = false;

    public static boolean isSkill() { return skillActive; }

    /** Gây sát thương phép/kỹ năng từ caster (bỏ qua luật đòn thường = 0). */
    public static void hurtBySkill(ServerPlayer caster, LivingEntity target, float amount) {
        skillActive = true;
        try {
            target.hurt(caster.damageSources().indirectMagic(caster, caster), amount);
        } finally {
            skillActive = false;
        }
    }

    public static boolean isWeapon(ItemStack s) {
        var i = s.getItem();
        return i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem
                || i instanceof MaceItem || i instanceof ProjectileWeaponItem;
    }

    public static float fistDamage(CultivationData d) {
        float aura = d.resourceMax() <= 0 ? 0 : (float) d.resource() / d.resourceMax();
        return (2f + 0.15f * d.level()) * (1f + 0.3f * aura);
    }

    /** Trả sát thương cuối cho đòn thường của người chơi attacker. */
    public static float resolve(ServerPlayer atk, LivingEntity target, DamageSource src, float amount) {
        if (skillActive) return amount;
        CultivationData d = atk.getData(ModAttachments.CULTIVATION);
        float out = amount;
        if (d.paths().contains(Path.THE)) {
            if (src.getDirectEntity() == atk) {
                out = isWeapon(atk.getMainHandItem()) ? 0f : Math.max(amount, fistDamage(d));
            } else if (src.getDirectEntity() != null) {
                out = 0f; // cung/nỏ/phi tiêu...
            }
        }
        if (d.paths().contains(Path.DAO) && !(target instanceof Player)) out = 0f;
        return out;
    }
}
