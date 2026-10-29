package com.hukhong.boss.phase;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> client: bắt đầu một cutscene camera. Chỉ gửi cho người trong 60 block; mỗi client tự chạy/tự bỏ qua,
 * server không khoá ai. entityId &lt; 0 = neo tĩnh tại (ax, ay, az) (cutscene giải phóng vùng).
 */
public record CutscenePayload(int entityId, double ax, double ay, double az, float refYaw, int ticks,
                              List<CamPath.Key> keys, List<SubLine> subs) implements CustomPacketPayload {
    public static final Type<CutscenePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "boss_cutscene"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CutscenePayload> STREAM_CODEC = StreamCodec.of(
            (b, p) -> {
                b.writeVarInt(p.entityId); b.writeDouble(p.ax); b.writeDouble(p.ay); b.writeDouble(p.az);
                b.writeFloat(p.refYaw); b.writeVarInt(p.ticks);
                b.writeVarInt(p.keys.size());
                for (CamPath.Key k : p.keys) {
                    b.writeVarInt(k.t()); b.writeFloat(k.dx()); b.writeFloat(k.dy()); b.writeFloat(k.dz());
                    b.writeFloat(k.yaw()); b.writeFloat(k.pitch()); b.writeFloat(k.fov()); b.writeFloat(k.lookH());
                }
                b.writeVarInt(p.subs.size());
                for (SubLine s : p.subs) { b.writeVarInt(s.from()); b.writeVarInt(s.to()); b.writeUtf(s.key(), 128); }
            },
            b -> {
                int id = b.readVarInt();
                double ax = b.readDouble(), ay = b.readDouble(), az = b.readDouble();
                float yaw = b.readFloat();
                int ticks = b.readVarInt();
                int nk = Math.min(64, b.readVarInt());
                List<CamPath.Key> keys = new ArrayList<>();
                for (int i = 0; i < nk; i++)
                    keys.add(new CamPath.Key(b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat()));
                int ns = Math.min(32, b.readVarInt());
                List<SubLine> subs = new ArrayList<>();
                for (int i = 0; i < ns; i++) subs.add(new SubLine(b.readVarInt(), b.readVarInt(), b.readUtf(128)));
                return new CutscenePayload(id, ax, ay, az, yaw, ticks, keys, subs);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CutscenePayload pl, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.boss.CutsceneClient.start(pl));
    }
}
