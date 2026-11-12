package com.hukhong.craft;

import com.hukhong.HuKhong;
import com.hukhong.alchemy.DanLoMenu;
import com.hukhong.profession.station.StationMenu;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/** Goi mang so cong thuc: Sync (server -> client, danh sach id da biet), Arrange (client -> server, xep nguyen lieu). */
public final class RecipeBookNet {
    private RecipeBookNet() {}

    private static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, p); }

    public record Sync(List<String> ids) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("recipe_sync"));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
                ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.STRING_UTF8), Sync::ids, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client bam "Xep nguyen lieu" cua cong thuc {@code recipe}. */
    public record Arrange(String recipe) implements CustomPacketPayload {
        public static final Type<Arrange> TYPE = new Type<>(id("recipe_arrange"));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Arrange> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Arrange::recipe, Arrange::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Sync.TYPE, Sync.CODEC, (pl, ctx) -> ctx.enqueueWork(() -> ClientRecipeBook.set(java.util.Set.copyOf(pl.ids()))));
        r.playToServer(Arrange.TYPE, Arrange.CODEC, (pl, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) ctx.enqueueWork(() -> arrangeOpen(sp, pl.recipe()));
        });
    }

    /** Dispatch toi menu tram dang mo (chi Dan Lo va 2 tram nghe). */
    static void arrangeOpen(ServerPlayer sp, String recipe) {
        AbstractContainerMenu m = sp.containerMenu;
        if (m instanceof DanLoMenu d) d.arrange(sp, recipe);
        else if (m instanceof StationMenu s) s.arrange(sp, recipe);
    }

}
