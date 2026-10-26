package com.hukhong.story.npc;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

/** Payload Bước 22: hiển thị hộp thoại (S2C), chọn đáp án (C2S), đồng bộ tiến trình chương (S2C). */
public final class NpcNet {
    private NpcNet() {}

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    private static final StreamCodec<io.netty.buffer.ByteBuf, List<String>> STRS =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    /** pages rỗng = đóng hộp thoại. */
    public record View(String nameKey, List<String> pages, List<String> choices) implements CustomPacketPayload {
        public static final Type<View> TYPE = new Type<>(id("npc_dialogue_view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, View::nameKey, STRS, View::pages, STRS, View::choices, View::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Choose(int index) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(id("npc_dialogue_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Choose::index, Choose::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Sync(QuestData data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("npc_quest_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.composite(QuestData.STREAM_CODEC, Sync::data, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(View.TYPE, View.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.story.NpcClient.view(p)));
        r.playToClient(Sync.TYPE, Sync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.story.NpcClient.update(p.data())));
        r.playToServer(Choose.TYPE, Choose.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) DialogueService.choose(sp, p.index());
        }));
    }
}
