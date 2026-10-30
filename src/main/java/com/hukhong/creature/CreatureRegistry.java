package com.hukhong.creature;

import com.hukhong.HuKhong;
import com.hukhong.creature.pet.PetData;
import com.hukhong.entity.ModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** F4 sinh vat: 14 entity type + trung sinh + vat pham nguyen lieu roi + slot linh thu cua nguoi choi. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CreatureRegistry {
    private CreatureRegistry() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HuKhong.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<PetData>> PETS = ATTACHMENTS.register("pets",
            () -> AttachmentType.builder(() -> PetData.EMPTY).serialize(PetData.CODEC).copyOnDeath().build());

    public static final Map<CreatureKind, DeferredHolder<EntityType<?>, EntityType<?>>> TYPE = new EnumMap<>(CreatureKind.class);
    public static final Map<CreatureKind, DeferredItem<SpawnEggItem>> EGGS = new EnumMap<>(CreatureKind.class);
    /** Nguyen lieu roi (id vat pham). */
    public static final Map<String, DeferredItem<Item>> MATERIALS = new LinkedHashMap<>();
    public static final java.util.List<String> MATERIAL_IDS = CreatureKind.MATERIALS;

    static {
        for (CreatureKind k : CreatureKind.values()) {
            TYPE.put(k, TYPES.register(k.id, () -> build(k)));
            int h = k.id.hashCode();
            EGGS.put(k, ITEMS.register(k.id + "_spawn_egg", () -> new SpawnEggItem((EntityType<? extends Mob>) TYPE.get(k).get(),
                    h & 0x7F7F7F, (h >> 3 | 0x808080) & 0xFFFFFF, new Item.Properties())));
        }
        for (String m : MATERIAL_IDS) MATERIALS.put(m, ITEMS.registerSimpleItem(m));
    }

    private static EntityType<?> build(CreatureKind k) {
        return switch (k.impl) {
            case THU -> EntityType.Builder.of(LinhThu::new, k.cat).sized(k.w, k.h).clientTrackingRange(8).build(k.id);
            case QUAI -> EntityType.Builder.of(LinhQuai::new, k.cat).sized(k.w, k.h).clientTrackingRange(8).build(k.id);
            case NGU -> EntityType.Builder.of(LinhNgu::new, k.cat).sized(k.w, k.h).clientTrackingRange(6).build(k.id);
        };
    }

    public static EntityType<?> type(CreatureKind k) { return TYPE.get(k).get(); }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        ITEMS.register(bus);
        ATTACHMENTS.register(bus);
        bus.addListener((RegisterGameTestsEvent e) -> e.register(CreatureGameTests.class));
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) for (var i : EGGS.values()) e.accept(i.get());
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) for (var i : MATERIALS.values()) e.accept(i.get());
        });
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void attrs(EntityAttributeCreationEvent e) {
        for (CreatureKind k : CreatureKind.values())
            e.put((EntityType<? extends LivingEntity>) type(k), CreatureCommon.attrs(k, k.impl == CreatureKind.Impl.QUAI).build());
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void placements(RegisterSpawnPlacementsEvent e) {
        for (CreatureKind k : CreatureKind.values()) {
            EntityType t = type(k);
            switch (k.impl) {
                case NGU -> e.register((EntityType<LinhNgu>) t, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR,
                        (ty, l, st, p, r) -> WaterAnimal.checkSurfaceWaterAnimalSpawnRules(ty, l, st, p, r),
                        RegisterSpawnPlacementsEvent.Operation.REPLACE);
                case QUAI -> e.register((EntityType<LinhQuai>) t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (ty, l, st, p, r) -> ModEntities.huSpawnRules(ty, l, st, p, r) && (k != CreatureKind.QUAN_HON || l.getLevel().isNight()),
                        RegisterSpawnPlacementsEvent.Operation.REPLACE);
                case THU -> e.register((EntityType<LinhThu>) t, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (ty, l, st, p, r) -> Mob.checkMobSpawnRules(ty, l, st, p, r)
                                && (k != CreatureKind.HOA_HUYNH || (l.getLevel().isNight() && l.canSeeSky(p))),
                        RegisterSpawnPlacementsEvent.Operation.REPLACE);
            }
        }
    }
}
