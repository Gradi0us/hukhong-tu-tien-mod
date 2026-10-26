package com.hukhong.endgame;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Payload Bước 26: mở màn chọn kết thúc (S2C), chọn (C2S), phát cutscene (S2C), xong cutscene+credits (C2S). */
public final class EndgameNet {
    private EndgameNet() {}

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    public record OpenChoice() implements CustomPacketPayload {
        public static final Type<OpenChoice> TYPE = new Type<>(id("endgame_open_choice"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenChoice> CODEC = StreamCodec.unit(new OpenChoice());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChooseEnding(int ending) implements CustomPacketPayload {
        public static final Type<ChooseEnding> TYPE = new Type<>(id("endgame_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChooseEnding> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ChooseEnding::ending, ChooseEnding::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PlayCutscene(int ending) implements CustomPacketPayload {
        public static final Type<PlayCutscene> TYPE = new Type<>(id("endgame_cutscene"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayCutscene> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, PlayCutscene::ending, PlayCutscene::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Finish() implements CustomPacketPayload {
        public static final Type<Finish> TYPE = new Type<>(id("endgame_finish"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Finish> CODEC = StreamCodec.unit(new Finish());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(OpenChoice.TYPE, OpenChoice.CODEC,
                (p, ctx) -> ctx.enqueueWork(com.hukhong.client.endgame.EndgameClient::openChoice));
        r.playToClient(PlayCutscene.TYPE, PlayCutscene.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.endgame.EndgameClient.playCutscene(p.ending())));
        r.playToServer(ChooseEnding.TYPE, ChooseEnding.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) EndgameService.choose(sp, p.ending());
        }));
        r.playToServer(Finish.TYPE, Finish.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) EndgameService.finish(sp);
        }));
    }
}
