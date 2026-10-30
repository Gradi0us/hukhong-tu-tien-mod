package com.hukhong.creature;

import com.hukhong.entity.HuHoa;
import com.hukhong.events.CorruptBuff;
import com.hukhong.gates.RewardGate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/** Phan chung cua 3 lop sinh vat: thuoc tinh goc, cap luc sinh, do roi (JSON), animation. */
public final class CreatureCommon {
    private CreatureCommon() {}

    public static AttributeSupplier.Builder attrs(CreatureKind k, boolean monster) {
        AttributeSupplier.Builder b = monster ? net.minecraft.world.entity.monster.Monster.createMonsterAttributes() : Mob.createMobAttributes();
        b.add(Attributes.MAX_HEALTH, k.hp).add(Attributes.MOVEMENT_SPEED, k.speed).add(Attributes.ATTACK_DAMAGE, Math.max(0.0, k.dmg))
                .add(Attributes.FOLLOW_RANGE, k == CreatureKind.MOC_YEU ? 7 : 24);
        if (k.fly) b.add(Attributes.FLYING_SPEED, k.speed);
        if (k == CreatureKind.THIET_BOI_HUNG || k == CreatureKind.THIET_BOI_HUNG_CON || k == CreatureKind.QUAN_HON)
            b.add(Attributes.KNOCKBACK_RESISTANCE, k == CreatureKind.THIET_BOI_HUNG ? 0.7 : 0.3).add(Attributes.ARMOR, k == CreatureKind.THIET_BOI_HUNG ? 6 : 2);
        return b;
    }

    /** Cap + chi so luc sinh (theo khoang cach/tuoi the gioi/vung nhiem); he so HP/sat thuong lay tu JSON. Tra ve cap. */
    public static int scaleOnSpawn(Mob m, CreatureKind k, ServerLevelAccessor l) {
        boolean cor = CorruptBuff.at(l, m.blockPosition());
        int lv = CorruptBuff.level(cor, HuHoa.levelFor(l, m.blockPosition(), m.getRandom()));
        CreatureDef d = k.def();
        double cm = CorruptBuff.mul(cor);
        HuHoa.applyStats(m, lv, (d == null ? 1.0 : d.hpMul()) * cm, (d == null ? 1.0 : d.dmgMul()) * cm);
        return lv;
    }

    /** Roi vat pham mod theo JSON (khong roi neu quai tu spawner). */
    public static void rollDrops(Mob m, CreatureKind k) {
        CreatureDef d = k.def();
        if (d == null || !RewardGate.canReward(m)) return;
        for (CreatureDef.Drop dr : d.drops()) {
            if (m.getRandom().nextDouble() >= dr.chance()) continue;
            Item it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(dr.item())).orElse(null);
            if (it == null) continue;
            m.spawnAtLocation(new ItemStack(it, dr.min() + m.getRandom().nextInt(dr.max() - dr.min() + 1)));
        }
    }

    public static double weight(CreatureKind k) { CreatureDef d = k.def(); return d == null ? 0.5 : d.expWeight(); }

    public static float honTinh(CreatureKind k) { CreatureDef d = k.def(); return d == null ? 0f : d.honTinh(); }

    public static <E extends LivingEntity & GeoEntity> void controllers(E e, String id, AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<E>(e, "move", 3, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation." + id + ".walk")
                : RawAnimation.begin().thenLoop("animation." + id + ".idle"))));
        c.add(new AnimationController<E>(e, "attack", 0, s -> PlayState.STOP)
                .triggerableAnim("attack", RawAnimation.begin().then("animation." + id + ".attack", Animation.LoopType.PLAY_ONCE))
                .triggerableAnim("skill", RawAnimation.begin().then("animation." + id + ".skill", Animation.LoopType.PLAY_ONCE)));
    }
}
