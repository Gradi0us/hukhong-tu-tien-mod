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

    /** Đòn tay Thể Tu = Công x (1 + FIST_AURA_COEF x Bá Khí%). */
    public static final double FIST_AURA_COEF = 1.80;

    private static boolean skillActive = false;

    public static boolean isSkill() { return skillActive; }

    /** Gây sát thương phép/kỹ năng từ caster (bỏ qua luật đòn thường = 0). */
    public static void hurtBySkill(ServerPlayer caster, LivingEntity target, float amount) {
        skillActive = true;
        try {
            amount = com.hukhong.skillbook.SkillBookRuntime.scale(caster, amount); // hệ số Cường Hóa kỹ năng nền
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

    /** Đòn tay Thể Tu = Công x (1 + 0.3 x Bá Khí%). */
    public static float fistDamage(ServerPlayer p, CultivationData d) {
        float aura = d.resourceMax() <= 0 ? 0 : (float) d.resource() / d.resourceMax();
        return (float) (com.hukhong.stats.StatService.attackPower(p) * (1f + FIST_AURA_COEF * aura));
    }

    /** Trả sát thương cuối cho đòn của người chơi attacker (đã gồm Công, Bạo Kích, Hấp Huyết). */
    public static float resolve(ServerPlayer atk, LivingEntity target, DamageSource src, float amount) {
        CultivationData d = atk.getData(ModAttachments.CULTIVATION);
        float out = amount;
        if (!skillActive) {
            boolean melee = src.getDirectEntity() == atk;
            if (d.paths().contains(Path.THE)) {
                if (melee) {
                    out = isWeapon(atk.getMainHandItem()) ? 0f : Math.max(amount, fistDamage(atk, d));
                } else if (src.getDirectEntity() != null) {
                    out = 0f; // cung/nỏ/phi tiêu...
                }
            } else if (melee && out > 0f) {
                out += (float) com.hukhong.stats.StatService.attackPower(atk); // Công cộng vào đòn cận chiến
            }
            if (d.paths().contains(Path.DAO) && !(target instanceof Player)) out = 0f;
        }
        if (out > 0f && !d.paths().isEmpty()) {
            double cc = com.hukhong.stats.StatService.value(atk, com.hukhong.stats.ModAttributes.CRIT_CHANCE);
            if (cc > 0 && atk.getRandom().nextDouble() < cc) {
                out *= (float) com.hukhong.stats.StatService.value(atk, com.hukhong.stats.ModAttributes.CRIT_DAMAGE);
            }
            double ls = com.hukhong.stats.StatService.value(atk, com.hukhong.stats.ModAttributes.LIFESTEAL);
            if (ls > 0 && !com.hukhong.pathrules.MaRules.noLifesteal(atk)) atk.heal(Math.min((float) (out * ls), atk.getMaxHealth() * 0.04f));
        }
        return out;
    }
}
