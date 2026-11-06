package com.hukhong.creature.pet;

import com.hukhong.HuKhong;
import com.hukhong.client.hud.PetHud;
import com.hukhong.creature.LinhThu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Server -> chu: trang thai thu chien dau + linh sung (HP, cap, tang, nhanh, than mat, trang thai) cho HUD ben trai. Gui moi 5 tick. */
public record PetSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<PetSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "pet_sync"));

    /** status: 0 = theo sat, 1 = ngoi yen, 2 = da chet (reviveSec = giay con lai). */
    public record Entry(String slot, String kind, int level, int stage, String branch, float hp, float maxHp, int bond, int status, int reviveSec) {}

    private static final Set<UUID> ACTIVE = new HashSet<>();

    public static final StreamCodec<FriendlyByteBuf, PetSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, pl) -> {
                buf.writeVarInt(pl.entries.size());
                for (Entry e : pl.entries) {
                    buf.writeUtf(e.slot); buf.writeUtf(e.kind); buf.writeVarInt(e.level); buf.writeVarInt(e.stage); buf.writeUtf(e.branch);
                    buf.writeFloat(e.hp); buf.writeFloat(e.maxHp); buf.writeVarInt(e.bond); buf.writeVarInt(e.status); buf.writeVarInt(e.reviveSec);
                }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Entry> l = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    l.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(),
                            buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
                }
                return new PetSyncPayload(List.copyOf(l));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(PayloadRegistrar r) {
        r.playToClient(TYPE, STREAM_CODEC, (pl, ctx) -> ctx.enqueueWork(() -> PetHud.update(pl.entries())));
    }

    /** Gui trang thai cho chu (goi moi 5 tick). Khong gui goi rong lap lai khi khong co thu. */
    public static void sync(ServerPlayer p) {
        List<Entry> out = build(p);
        if (out.isEmpty()) {
            if (ACTIVE.remove(p.getUUID())) PacketDistributor.sendToPlayer(p, new PetSyncPayload(List.of()));
            return;
        }
        ACTIVE.add(p.getUUID());
        PacketDistributor.sendToPlayer(p, new PetSyncPayload(out));
    }

    static List<Entry> build(ServerPlayer p) {
        PetData d = PetService.data(p);
        List<Entry> out = new ArrayList<>();
        long now = p.serverLevel().getGameTime();
        for (String s : new String[]{"battle", "spirit"}) {
            Optional<UUID> id = d.get(s);
            if (id.isPresent() && p.serverLevel().getEntity(id.get()) instanceof LinhThu t && t.isAlive()) {
                PetBranch b = t.petBranch();
                out.add(new Entry(s, t.kind().id, PetService.ownerLevel(p), t.petStage(), b == null ? "" : b.id,
                        t.getHealth(), t.getMaxHealth(), t.bond(), t.isOrderedToSit() ? 1 : 0, 0));
            } else if (d.fallen(s).isPresent()) {
                PetData.Fallen f = d.fallen(s).get();
                out.add(new Entry(s, f.kind(), f.level(), f.stage(), f.branch(), 0, 1, f.bond(), 2,
                        (int) Math.max(0, (f.reviveAt() - now) / 20)));
            }
        }
        return out;
    }
}
