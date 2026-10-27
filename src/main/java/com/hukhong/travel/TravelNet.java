package com.hukhong.travel;

import com.hukhong.HuKhong;
import com.hukhong.story.flow.RequiredNpcs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** C2S: yeu cau dich chuyen (id diem den hoac "obj") va trieu hoi lai NPC tuyen chinh. */
public final class TravelNet {
    private TravelNet() {}

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    public record Go(String dest) implements CustomPacketPayload {
        public static final Type<Go> TYPE = new Type<>(id("travel_go"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Go> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Go::dest, Go::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Resummon() implements CustomPacketPayload {
        public static final Type<Resummon> TYPE = new Type<>(id("quest_npc_resummon"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Resummon> CODEC = StreamCodec.unit(new Resummon());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToServer(Go.TYPE, Go.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                String err = TravelService.start(sp, p.dest(), false);
                if (err != null) sp.displayClientMessage(net.minecraft.network.chat.Component.translatable(err), true);
            }
        }));
        r.playToServer(Resummon.TYPE, Resummon.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) RequiredNpcs.resummon(sp);
        }));
    }
}
