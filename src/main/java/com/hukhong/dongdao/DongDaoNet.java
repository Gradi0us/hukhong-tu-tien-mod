package com.hukhong.dongdao;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Dong bo "Dong Dao": C2S Request (lam moi danh sach), C2S Go (den), S2C Sync (uuid, ten, chieu khong gian). */
public final class DongDaoNet {
    private DongDaoNet() {}

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    public record Entry(UUID id, String name, String dim) {}

    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(id("dongdao_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.unit(new Request());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Go(UUID target) implements CustomPacketPayload {
        public static final Type<Go> TYPE = new Type<>(id("dongdao_go"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Go> CODEC = StreamCodec.composite(net.minecraft.core.UUIDUtil.STREAM_CODEC, Go::target, Go::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Sync(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("dongdao_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of(
                (b, s) -> { b.writeVarInt(s.entries.size()); for (Entry e : s.entries) { b.writeUUID(e.id); b.writeUtf(e.name, 64); b.writeUtf(e.dim, 128); } },
                b -> { int n = b.readVarInt(); List<Entry> l = new ArrayList<>(); for (int i = 0; i < n && i < 512; i++) l.add(new Entry(b.readUUID(), b.readUtf(64), b.readUtf(128))); return new Sync(l); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Vi tri chet cho client: has=false neu khong co. ageTicks<0: khong ro. */
    public record Death(boolean has, String dim, int x, int y, int z, long ageTicks) implements CustomPacketPayload {
        public static final Type<Death> TYPE = new Type<>(id("chochet_info"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Death> CODEC = StreamCodec.of(
                (b, d) -> { b.writeBoolean(d.has); b.writeUtf(d.dim, 128); b.writeInt(d.x); b.writeInt(d.y); b.writeInt(d.z); b.writeLong(d.ageTicks); },
                b -> new Death(b.readBoolean(), b.readUtf(128), b.readInt(), b.readInt(), b.readInt(), b.readLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record GoDeath() implements CustomPacketPayload {
        public static final Type<GoDeath> TYPE = new Type<>(id("chochet_go"));
        public static final StreamCodec<RegistryFriendlyByteBuf, GoDeath> CODEC = StreamCodec.unit(new GoDeath());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(Sync.TYPE, Sync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> DongDaoClient.set(p.entries())));
        r.playToClient(Death.TYPE, Death.CODEC, (p, ctx) -> ctx.enqueueWork(() -> DongDaoClient.setDeath(p)));
        r.playToServer(GoDeath.TYPE, GoDeath.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                String err = VeChoChet.go(sp);
                if (err != null) sp.displayClientMessage(Component.translatable(err), true);
                send(sp);
            }
        }));
        r.playToServer(Request.TYPE, Request.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) send(sp);
        }));
        r.playToServer(Go.TYPE, Go.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                String err = DongDao.go(sp, p.target());
                if (err != null) sp.displayClientMessage(Component.translatable(err), true);
            }
        }));
    }

    public static List<Entry> entriesFor(ServerPlayer p) {
        List<Entry> out = new ArrayList<>();
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o == p) continue;
            out.add(new Entry(o.getUUID(), o.getGameProfile().getName(), o.level().dimension().location().toString()));
        }
        return out;
    }

    public static void send(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new Sync(entriesFor(p)));
        var d = VeChoChet.valid(p);
        PacketDistributor.sendToPlayer(p, d.isEmpty() ? new Death(false, "", 0, 0, 0, -1)
                : new Death(true, d.get().dimension().location().toString(), d.get().pos().getX(), d.get().pos().getY(), d.get().pos().getZ(), VeChoChet.ageTicks(p)));
    }
}
