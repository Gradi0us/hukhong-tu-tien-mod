package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.creature.LinhThu;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Tien hoa + than mat: quy tac cap/than mat va ap chi so theo tang nhanh. Modifier dung id rieng (pet_*) nen khong bi
 * HuHoa.applyStats ghi de khi dong bo cap.
 */
public final class PetForm {
    private PetForm() {}

    public static final int EVO1_LEVEL = 30, EVO2_LEVEL = 60, EVO2_BOND = 50, BOND_MAX = 100, MAX_STAGE = 2;

    /** Cap can de len tang {@code target} (1 hoac 2). */
    public static int requiredLevel(int target) { return target >= 2 ? EVO2_LEVEL : EVO1_LEVEL; }

    /** Tang tiep theo neu du dieu kien (cap thu, than mat), hoac -1. */
    public static int nextStage(int stage, int petLevel, int bond) {
        if (stage == 0 && petLevel >= EVO1_LEVEL) return 1;
        if (stage == 1 && petLevel >= EVO2_LEVEL && bond >= EVO2_BOND) return 2;
        return -1;
    }

    public static void addBond(LinhThu t, int amount) {
        t.setBond(Math.max(0, Math.min(BOND_MAX, t.bond() + amount)));
        apply(t);
    }

    /** Ap modifier tang + than mat + scale; giu ty le mau cu. */
    public static void apply(LinhThu t) {
        float ratio = t.getHealth() / Math.max(1f, t.getMaxHealth());
        int st = t.petStage();
        PetBranch b = t.petBranch();
        double m = st >= 2 ? 1.6 : st == 1 ? 1.0 : 0.0;
        double bondK = t.bond() * 0.0015; // than mat 100 = +15% mau va sat thuong
        double hp = b == null ? 0 : b.hp * m, dmg = b == null ? 0 : b.dmg * m;
        double spd = b == null ? 0 : b.spd * m, armor = b == null ? 0 : b.armor * m;
        put(t, Attributes.MAX_HEALTH, "pet_hp", hp + bondK, Operation.ADD_MULTIPLIED_BASE);
        put(t, Attributes.ATTACK_DAMAGE, "pet_dmg", dmg + bondK, Operation.ADD_MULTIPLIED_BASE);
        put(t, Attributes.MOVEMENT_SPEED, "pet_spd", spd, Operation.ADD_MULTIPLIED_BASE);
        put(t, Attributes.ARMOR, "pet_armor", armor, Operation.ADD_VALUE);
        put(t, Attributes.SCALE, "pet_scale", st >= 2 ? 0.20 : st == 1 ? 0.10 : 0.0, Operation.ADD_MULTIPLIED_BASE);
        if (t.isAlive()) t.setHealth(Math.max(1f, Math.min(t.getMaxHealth(), ratio * t.getMaxHealth())));
    }

    private static void put(LinhThu t, Holder<Attribute> a, String id, double amount, Operation op) {
        AttributeInstance inst = t.getAttribute(a);
        if (inst == null) return;
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id);
        inst.removeModifier(rl);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(rl, amount, op));
    }
}
