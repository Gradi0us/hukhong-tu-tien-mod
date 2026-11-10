package com.hukhong.dodge;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Gói mạng Lách Né / Thể Lực. */
public final class DodgeNet {
    private DodgeNet() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    /** Client -> server: bấm phím Lách Né với input di chuyển hiện tại. */
    public record Dodge(float forward, float strafe) implements CustomPacketPayload {
        public static final Type<Dodge> TYPE = new Type<>(id("dodge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Dodge> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Dodge::forward, ByteBufCodecs.FLOAT, Dodge::strafe, Dodge::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: Thể Lực hiện tại / tối đa. */
    public record Stamina(float cur, float max) implements CustomPacketPayload {
        public static final Type<Stamina> TYPE = new Type<>(id("stamina_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Stamina> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Stamina::cur, ByteBufCodecs.FLOAT, Stamina::max, Stamina::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client (chính chủ): kích hoạt hiệu ứng Né Hoàn Hảo. */
    public record Perfect() implements CustomPacketPayload {
        public static final Type<Perfect> TYPE = new Type<>(id("perfect_dodge"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Perfect> CODEC = StreamCodec.unit(new Perfect());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Dodge.TYPE, Dodge.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> DodgeService.tryDodge(sp, pl.forward(), pl.strafe()));
        });
        // Lớp client chỉ được tham chiếu trong lambda -> chỉ nạp khi nhận gói ở client.
        r.playToClient(Stamina.TYPE, Stamina.CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> com.hukhong.dodge.client.DodgeClient.onStamina(pl.cur(), pl.max())));
        r.playToClient(Perfect.TYPE, Perfect.CODEC,
                (pl, ctx) -> ctx.enqueueWork(() -> com.hukhong.dodge.client.DodgeClient.onPerfect()));
    }
}
