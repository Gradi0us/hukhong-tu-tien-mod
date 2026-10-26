package com.hukhong.breakthrough;

import com.hukhong.core.CultivationData;
import com.hukhong.core.Realm;
import com.hukhong.network.BreakthroughPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;

/** Hiệu ứng khi qua mốc: cột particle, âm thanh, title, rung camera (client). */
public final class BreakthroughEffects {
    private BreakthroughEffects() {}

    public static void play(ServerPlayer p, CultivationData d) {
        if (p.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 24; i++) {
                double y = p.getY() + i * 0.25;
                sl.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), p.getX(), y, p.getZ(), 3, 0.25, 0.05, 0.25, 0.01);
                sl.sendParticles(com.hukhong.vfx.ModParticles.LINH_KHI.get(), p.getX(), y, p.getZ(), 4, 0.5, 0.05, 0.5, 0.2);
            }
            com.hukhong.vfx.ModSounds.play(sl, p, com.hukhong.vfx.ModSounds.BREAKTHROUGH, 1.2f, 1.0f);
            sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("hukhong.breakthrough.title", Component.translatable(Realm.langKey(d.level())))));
        PacketDistributor.sendToPlayer(p, new BreakthroughPayload(20, 0.6f));
    }
}
