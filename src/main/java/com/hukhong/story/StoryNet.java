package com.hukhong.story;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Payload Bước 19: mở màn chọn mode (S2C), chọn mode (C2S), đồng bộ StoryData (S2C). */
public final class StoryNet {
    private StoryNet() {}

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    public record OpenMode() implements CustomPacketPayload {
        public static final Type<OpenMode> TYPE = new Type<>(id("story_open_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenMode> CODEC = StreamCodec.unit(new OpenMode());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ChooseMode(int mode) implements CustomPacketPayload {
        public static final Type<ChooseMode> TYPE = new Type<>(id("story_choose_mode"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChooseMode> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, ChooseMode::mode, ChooseMode::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Sync(StoryData data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("story_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.composite(StoryData.STREAM_CODEC, Sync::data, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Hư Ảnh theo thành (10 số 0..100) -> client (ClientHuAnh). */
    public record HuAnhSync(int[] values) implements CustomPacketPayload {
        public static final Type<HuAnhSync> TYPE = new Type<>(id("huanh_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HuAnhSync> CODEC = StreamCodec.of(
                (b, p) -> { b.writeVarInt(p.values.length); for (int v : p.values) b.writeVarInt(v); },
                b -> { int[] a = new int[Math.min(64, b.readVarInt())]; for (int i = 0; i < a.length; i++) a[i] = b.readVarInt(); return new HuAnhSync(a); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(OpenMode.TYPE, OpenMode.CODEC,
                (p, ctx) -> ctx.enqueueWork(com.hukhong.client.story.StoryClient::openModeScreen));
        r.playToClient(Sync.TYPE, Sync.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.story.StoryClient.update(p.data())));
        r.playToClient(HuAnhSync.TYPE, HuAnhSync.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.hukhong.client.story.ClientHuAnh.update(p.values())));
        r.playToServer(ChooseMode.TYPE, ChooseMode.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) StoryService.chooseMode(sp, p.mode());
        }));
    }
}
