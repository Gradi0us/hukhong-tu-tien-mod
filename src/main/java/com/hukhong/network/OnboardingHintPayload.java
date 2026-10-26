package com.hukhong.network;

import com.hukhong.HuKhong;
import com.hukhong.client.onboarding.OnboardingClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: hiện gợi ý onboarding bước {@code step} (1..5); hasCity + (x,z) cho bước 4. */
public record OnboardingHintPayload(int step, boolean hasCity, int x, int z) implements CustomPacketPayload {
    public static final Type<OnboardingHintPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "onboarding_hint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OnboardingHintPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OnboardingHintPayload::step,
            ByteBufCodecs.BOOL, OnboardingHintPayload::hasCity,
            ByteBufCodecs.VAR_INT, OnboardingHintPayload::x,
            ByteBufCodecs.VAR_INT, OnboardingHintPayload::z,
            OnboardingHintPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(OnboardingHintPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> OnboardingClient.show(p));
    }

    @EventBusSubscriber(modid = HuKhong.MODID)
    public static final class Reg {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent e) {
            e.registrar("1").playToClient(TYPE, STREAM_CODEC, OnboardingHintPayload::handle);
        }
    }
}
