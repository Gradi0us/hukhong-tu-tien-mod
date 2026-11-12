package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Dấu ngoại hình vĩnh viễn sau kết (PLAN-v1 §8b.6): Hấp Thụ = "Nửa Hư Không", Phong Ấn = ấn vàng trên mu bàn tay,
 * Siêu Độ = vầng sáng sau lưng. Nguồn sự thật = cờ quest (server); đồng bộ tới MỌI client bằng payload riêng
 * (khi đặt, khi đăng nhập: người mới nhận dấu của mọi người và mọi người nhận dấu của người mới).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class VoidMark {
    private VoidMark() {}

    public static final int NONE = 0, HALF_VOID = 1, SEAL = 2, HALO = 3;

    public static int of(QuestData q) {
        if (q.has(EndgameService.F_ABSORB)) return HALF_VOID;
        if (q.has(EndgameService.F_SEAL)) return SEAL;
        if (q.has(EndgameService.F_TRUTH)) return HALO;
        return NONE;
    }

    public static int of(ServerPlayer p) { return of(DialogueService.quest(p)); }

    /** player = UUID chủ dấu; fade = hiện dần 40 tick (lần đầu trong cutscene). */
    public record Sync(UUID player, int mark, boolean fade) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "void_mark"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Sync::player, ByteBufCodecs.VAR_INT, Sync::mark, ByteBufCodecs.BOOL, Sync::fade, Sync::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Gửi dấu hiện tại (theo cờ quest) của {@code p} tới mọi người chơi trên server. */
    public static void update(ServerPlayer p, boolean fade) {
        PacketDistributor.sendToAllPlayers(new Sync(p.getUUID(), of(p), fade));
    }

    /** Dev/demo: ép dấu tạm thời (không đổi cờ quest; relog sẽ trả về theo cờ). */
    public static void force(ServerPlayer p, int mark, boolean fade) {
        PacketDistributor.sendToAllPlayers(new Sync(p.getUUID(), mark, fade));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        for (ServerPlayer o : p.getServer().getPlayerList().getPlayers()) {
            if (o == p) continue;
            int m = of(o);
            if (m != NONE) PacketDistributor.sendToPlayer(p, new Sync(o.getUUID(), m, false));
        }
        update(p, false);
    }
}
