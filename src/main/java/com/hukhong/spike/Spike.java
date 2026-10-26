package com.hukhong.spike;

import com.hukhong.HuKhong;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** API spike: compile-only verification (see docs/research/R4-api-spike.md). */
public final class Spike {
    private Spike() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HuKhong.MODID);

    public static final Supplier<AttachmentType<Integer>> LINH_KHI = ATTACHMENTS.register("linh_khi",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    public record PingPayload(int value) implements CustomPacketPayload {
        public static final Type<PingPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "ping"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, PingPayload::value, PingPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RealmDef(String name) {
        public static final Codec<RealmDef> CODEC = Codec.STRING.xmap(RealmDef::new, RealmDef::name);
    }

    public static final ResourceKey<net.minecraft.core.Registry<RealmDef>> REALM_REGISTRY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "realm"));

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(Spike::onPayloads);
        modBus.addListener(Spike::onNewRegistry);
        NeoForge.EVENT_BUS.addListener(Spike::onDamage);
        NeoForge.EVENT_BUS.addListener(Spike::onFinalizeSpawn);
    }

    private static void onPayloads(RegisterPayloadHandlersEvent e) {
        PayloadRegistrar r = e.registrar("1");
        r.playToClient(PingPayload.TYPE, PingPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {}));
    }

    private static void onNewRegistry(DataPackRegistryEvent.NewRegistry e) {
        e.dataPackRegistry(REALM_REGISTRY, RealmDef.CODEC, RealmDef.CODEC);
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        e.setAmount(e.getAmount()); // mutable amount; cancelable
    }

    private static void onFinalizeSpawn(FinalizeSpawnEvent e) {
        if (e.getSpawnType() == MobSpawnType.SPAWNER) {
            e.getEntity().setCustomName(null); // e.g. mark spawner mobs
        }
    }
}
