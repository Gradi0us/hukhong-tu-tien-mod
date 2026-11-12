package com.hukhong.profession.khi;

import com.hukhong.HuKhong;
import com.hukhong.profession.station.StationMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Goi mang Doan Tao Dai: Act (client -> server, thao tac + tick bam), State (server -> client, phien dang chay). */
public final class ForgeNet {
    private ForgeNet() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    /** Client -> server. tick = so tick ke tu luc Nung (client tu do). */
    public record Act(int op, long tick) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(id("forge_act"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Act> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Act::op, ByteBufCodecs.VAR_LONG, Act::tick, Act::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server -> client: trang thai phien rèn (client tu tinh kim/nhiet tu seed + startTick). */
    public record State(int phase, long startTick, long seed, int tier, int level, int strikes, int heatPts, int hammerSum)
            implements CustomPacketPayload {
        public static final State IDLE = new State(ForgeService.PH_IDLE, 0, 0, 1, 1, 0, 0, 0);
        public static final Type<State> TYPE = new Type<>(id("forge_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, s) -> {
                    buf.writeVarInt(s.phase()).writeVarLong(s.startTick()).writeVarLong(s.seed())
                            .writeVarInt(s.tier()).writeVarInt(s.level()).writeVarInt(s.strikes())
                            .writeVarInt(s.heatPts()).writeVarInt(s.hammerSum());
                },
                buf -> new State(buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Act.TYPE, Act.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp)
                ctx.enqueueWork(() -> {
                    if (sp.containerMenu instanceof StationMenu m) m.forge(sp, pl.op(), pl.tick());
                });
        });
        r.playToClient(State.TYPE, State.CODEC, (pl, ctx) -> ctx.enqueueWork(() -> ForgeClient.set(pl)));
    }

    /** Client: gui thao tac len server. */
    public static void act(int op, long tick) { PacketDistributor.sendToServer(new Act(op, tick)); }

    /** Server: dong bo trang thai phien cho nguoi choi. */
    public static void sendTo(ServerPlayer sp, State st) { PacketDistributor.sendToPlayer(sp, st); }
}
