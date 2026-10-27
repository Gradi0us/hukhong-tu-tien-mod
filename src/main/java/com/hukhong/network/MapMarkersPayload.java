package com.hukhong.network;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Server -> client: vị trí thành/tông (Overworld) cho minimap. Chỉ đọc dữ liệu có sẵn, không đổi trạng thái server. */
public record MapMarkersPayload(List<Marker> markers) implements CustomPacketPayload {
    public static final byte CITY = 0, SECT = 1;

    /** kind: CITY/SECT; id: id CityDef/SectDef; built=false -> vị trí xấp xỉ (neo + lệch tất định). */
    public record Marker(byte kind, String id, int x, int z, boolean built) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, Marker::kind,
                ByteBufCodecs.STRING_UTF8, Marker::id,
                ByteBufCodecs.VAR_INT, Marker::x,
                ByteBufCodecs.VAR_INT, Marker::z,
                ByteBufCodecs.BOOL, Marker::built,
                Marker::new);

        public String nameKey() { return (kind == CITY ? "hukhong.city.n." : "hukhong.sect.n.") + id; }
    }

    public static final Type<MapMarkersPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "map_markers"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MapMarkersPayload> STREAM_CODEC = StreamCodec.composite(
            Marker.STREAM_CODEC.apply(ByteBufCodecs.list(64)), MapMarkersPayload::markers,
            MapMarkersPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MapMarkersPayload pl, net.neoforged.neoforge.network.handling.IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.hukhong.client.minimap.MinimapState.setMarkers(pl.markers()));
    }
}
