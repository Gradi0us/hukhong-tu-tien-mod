package com.hukhong.skillbook;

import com.hukhong.HuKhong;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: danh sách kỹ năng đã học, cấp, gán phím. */
public record BookSyncPayload(SkillBookData data) implements CustomPacketPayload {
    public static final Type<BookSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "book_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BookSyncPayload> STREAM_CODEC =
            StreamCodec.composite(SkillBookData.STREAM_CODEC, BookSyncPayload::data, BookSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BookSyncPayload p, IPayloadContext ctx) { ctx.enqueueWork(() -> ClientSkillBook.set(p.data())); }
}
