package com.hukhong.phaptu;

import com.hukhong.HuKhong;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.serialization.Codec;

import java.util.function.Supplier;

/** Đăng ký riêng của Pháp Tu: attachment nguyên tố (đồng bộ cho client) + thực thể Tháp Thổ. Gọi {@link #register} từ HuKhong. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhapTuRegistry {
    private PhapTuRegistry() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, HuKhong.MODID);

    /** Id nguyên tố ("" = chưa có). Đồng bộ cho mọi client theo dõi (HUD, hào quang, icon). */
    public static final Supplier<AttachmentType<String>> ELEMENT = ATTACHMENTS.register("phap_element",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath().sync(ByteBufCodecs.STRING_UTF8).build());

    public static final DeferredHolder<EntityType<?>, EntityType<PhapTurret>> TURRET = ENTITIES.register("phap_turret",
            () -> EntityType.Builder.of(PhapTurret::new, MobCategory.MISC).sized(0.8f, 1.3f).clientTrackingRange(8)
                    .fireImmune().build("phap_turret"));

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        ENTITIES.register(bus);
    }

    @SubscribeEvent
    public static void attrs(EntityAttributeCreationEvent e) {
        e.put(TURRET.get(), PhapTurret.attributes().build());
    }
}
