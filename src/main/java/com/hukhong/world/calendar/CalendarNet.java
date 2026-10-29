package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/** Dong bo lich: S2C danh sach (loc theo nguoi choi) + C2S nut "Den". */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CalendarNet {
    private CalendarNet() {}

    private static ResourceLocation id(String s) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, s); }

    /** Mot dong lich gui xuong client. city = chi so thanh (-1 neu toa do co dinh); faction = ordinal. */
    public record Entry(String key, String defId, boolean story, int faction, int levelMin, int levelMax, long start, long end,
                        int city, boolean active, boolean used) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(key); b.writeUtf(defId); b.writeBoolean(story); b.writeVarInt(faction); b.writeVarInt(levelMin);
            b.writeVarInt(levelMax); b.writeLong(start); b.writeLong(end); b.writeVarInt(city + 1); b.writeBoolean(active); b.writeBoolean(used);
        }

        static Entry read(FriendlyByteBuf b) {
            return new Entry(b.readUtf(), b.readUtf(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readLong(),
                    b.readLong(), b.readVarInt() - 1, b.readBoolean(), b.readBoolean());
        }

        public String nameKey() { return "hukhong.cal.ev." + defId; }
    }

    public record Sync(List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("calendar_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of(
                (b, s) -> { b.writeVarInt(s.entries.size()); for (Entry e : s.entries) e.write(b); },
                b -> { int n = b.readVarInt(); List<Entry> l = new ArrayList<>(); for (int i = 0; i < n && i < 256; i++) l.add(Entry.read(b)); return new Sync(l); });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Go(String key) implements CustomPacketPayload {
        public static final Type<Go> TYPE = new Type<>(id("calendar_go"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Go> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Go::key, Go::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent e) {
        var r = e.registrar("1");
        r.playToClient(Sync.TYPE, Sync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> CalendarClient.set(p.entries())));
        r.playToServer(Go.TYPE, Go.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                String err = CalendarService.go(sp, p.key());
                if (err != null) sp.displayClientMessage(Component.translatable(err), true);
                else send(sp);
            }
        }));
    }

    /** Danh sach cho mot nguoi: dang dien ra + sap toi (3 ngay Lich); lan story chi hien khi StoryHooks.allowed. */
    public static List<Entry> entriesFor(ServerPlayer p, CalendarData data, long now) {
        List<Entry> out = new ArrayList<>();
        for (CalendarData.Act a : data.active().values()) {
            EventDef d = EventDefs.get(a.defId());
            if (d == null || (d.story() && !StoryHooks.allowed(p, d.lore()))) continue;
            out.add(entry(d, a.key(), a.start(), a.end(), a.city(), true, data.isUsed(a.key(), p.getUUID())));
        }
        for (CalendarScheduler.Occ o : CalendarScheduler.occurrences(now, now + CalendarScheduler.HORIZON)) {
            if (o.start() <= now || data.active().containsKey(o.key())) continue;
            if (o.def().story() && !StoryHooks.allowed(p, o.def().lore())) continue;
            out.add(entry(o.def(), o.key(), o.start(), o.end(), o.def().cityOf(o.start(), com.hukhong.city.CityDef.ALL.size()), false,
                    data.isUsed(o.key(), p.getUUID())));
            if (out.size() >= 200) break;
        }
        return out;
    }

    private static Entry entry(EventDef d, String key, long s, long e, int city, boolean active, boolean used) {
        return new Entry(key, d.id(), d.story(), d.lore().faction().ordinal(), d.levelMin(), d.levelMax(), s, e, city, active, used);
    }

    public static void send(ServerPlayer p) {
        ServerLevel ow = p.server.overworld();
        PacketDistributor.sendToPlayer(p, new Sync(entriesFor(p, CalendarData.get(ow), ow.getGameTime())));
    }

    static void syncAll(ServerLevel ow) {
        for (ServerPlayer p : ow.getServer().getPlayerList().getPlayers()) send(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) send(p);
    }
}
