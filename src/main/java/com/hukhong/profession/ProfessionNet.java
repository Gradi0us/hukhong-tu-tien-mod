package com.hukhong.profession;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Goi mang Nghe phu: Sync (server -> client), Choose (client -> server). */
public final class ProfessionNet {
    private ProfessionNet() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    /** Server -> client: nghe + cap + xp hien tai. */
    public record Sync(int prof, int level, long xp) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("profession_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Sync::prof, ByteBufCodecs.VAR_INT, Sync::level, ByteBufCodecs.VAR_LONG, Sync::xp, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client -> server: chon nghe (server kiem tra lai: chi khi dang NONE). */
    public record Choose(int prof) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(id("profession_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Choose::prof, Choose::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void send(ServerPlayer p) {
        ProfessionData d = ProfessionService.get(p);
        PacketDistributor.sendToPlayer(p, new Sync(d.profession().ordinal(), d.level(), d.xp()));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Sync.TYPE, Sync.CODEC, (pl, ctx) -> ctx.enqueueWork(() ->
                ClientProfession.set(new ProfessionData(Profession.byOrdinal(pl.prof()), pl.level(), pl.xp()))));
        r.playToServer(Choose.TYPE, Choose.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp)
                ctx.enqueueWork(() -> ProfessionService.choose(sp, Profession.byOrdinal(pl.prof())));
        });
    }
}
