package com.hukhong.stats;

import com.hukhong.HuKhong;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Attribute tu tiên (ID cố định hukhong:*). Gắn cho Player qua EntityAttributeModificationEvent. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ModAttributes {
    private ModAttributes() {}

    public static final String ATTACK_POWER = "attack_power";
    public static final String DEFENSE = "defense";
    public static final String SPELL_POWER = "spell_power";
    public static final String CRIT_CHANCE = "crit_chance";
    public static final String CRIT_DAMAGE = "crit_damage";
    public static final String LIFESTEAL = "lifesteal";
    public static final String RESOURCE_REGEN = "resource_regen";
    public static final String MAX_RESOURCE = "max_resource";

    /** crit_damage là hệ số nhân sát thương (1.5 = x1.5). */
    public static final double DEFAULT_CRIT_DAMAGE = 1.5;

    /** id -> {default, min, max}. crit_chance / lifesteal là PHÂN SỐ (0.15 = 15%). */
    private static final Map<String, double[]> DEFS = new LinkedHashMap<>();
    static {
        DEFS.put(ATTACK_POWER, new double[]{0, 0, 100000});
        DEFS.put(DEFENSE, new double[]{0, 0, 100000});
        DEFS.put(SPELL_POWER, new double[]{0, 0, 100000});
        DEFS.put(CRIT_CHANCE, new double[]{0, 0, 1});
        DEFS.put(CRIT_DAMAGE, new double[]{DEFAULT_CRIT_DAMAGE, 1, 20});
        DEFS.put(LIFESTEAL, new double[]{0, 0, 1});
        DEFS.put(RESOURCE_REGEN, new double[]{1, 0, 20});
        DEFS.put(MAX_RESOURCE, new double[]{100, 1, 1000000});
    }

    public static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, name); }

    public static Iterable<String> names() { return DEFS.keySet(); }

    public static Holder<Attribute> holder(String name) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id(name)).orElseThrow();
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent e) {
        e.register(Registries.ATTRIBUTE, h -> DEFS.forEach((name, d) ->
                h.register(id(name), new RangedAttribute("attribute.name." + HuKhong.MODID + "." + name, d[0], d[1], d[2])
                        .setSyncable(true))));
    }

    @SubscribeEvent
    public static void onAttach(EntityAttributeModificationEvent e) {
        for (String name : DEFS.keySet()) e.add(EntityType.PLAYER, holder(name));
    }
}
