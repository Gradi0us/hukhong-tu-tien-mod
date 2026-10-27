package com.hukhong.story.objective;

import com.hukhong.HuKhong;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Theo dõi mục tiêu chính (main quest). Lưu bằng attachment, đồng bộ qua ObjectiveSyncPayload. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class Objectives {
    private Objectives() {}

    public static void set(ServerPlayer p, String id, Component text, @Nullable BlockPos target, @Nullable ResourceKey<Level> dim) {
        p.setData(ObjectiveAttachments.OBJECTIVE.get(),
                new ObjectiveData(id, text, Optional.ofNullable(target), Optional.ofNullable(dim)));
        sync(p);
    }

    public static void clear(ServerPlayer p) {
        p.setData(ObjectiveAttachments.OBJECTIVE.get(), ObjectiveData.EMPTY);
        sync(p);
    }

    public static @Nullable String currentId(ServerPlayer p) {
        ObjectiveData d = p.getData(ObjectiveAttachments.OBJECTIVE.get());
        return d.isEmpty() ? null : d.id();
    }

    public static void complete(ServerPlayer p, String id) {
        ObjectiveData d = p.getData(ObjectiveAttachments.OBJECTIVE.get());
        if (d.isEmpty() || !d.id().equals(id)) return;
        p.setData(ObjectiveAttachments.OBJECTIVE.get(), ObjectiveData.EMPTY);
        if (p.connection != null) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            PacketDistributor.sendToPlayer(p, new ObjectiveCompletePayload(Component.translatable("hukhong.objective.done", d.text())));
        }
        sync(p);
    }

    private static void sync(ServerPlayer p) {
        if (p.connection == null) return; // FakePlayer / chưa đăng nhập
        PacketDistributor.sendToPlayer(p, new ObjectiveSyncPayload(p.getData(ObjectiveAttachments.OBJECTIVE.get())));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }
}
