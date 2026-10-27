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
            net.minecraft.world.phys.Vec3 base = p.position();
            for (int k = 0; k < 4; k++)
                com.hukhong.skill.SkillEffects.ringLater(sl, base.add(0, 0.15, 0), 1.5 + 1.8 * k, 24 + 6 * k, 3 * k + 1, ParticleTypes.END_ROD);
            com.hukhong.skill.SkillEffects.spiral(sl, base, 1.8, 0.3, 7, 3, 50, ParticleTypes.ENCHANT);
            com.hukhong.skill.SkillEffects.spiral(sl, base, 0.3, 1.8, 7, 3, 40, com.hukhong.vfx.ModParticles.THANH_QUANG.get());
            sl.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 24; i++) {
                double y = p.getY() + i * 0.25;
                sl.sendParticles(com.hukhong.vfx.ModParticles.THANH_QUANG.get(), p.getX(), y, p.getZ(), 3, 0.25, 0.05, 0.25, 0.01);
                sl.sendParticles(com.hukhong.vfx.ModParticles.LINH_KHI.get(), p.getX(), y, p.getZ(), 4, 0.5, 0.05, 0.5, 0.2);
            }
            com.hukhong.vfx.Fx.play(sl, com.hukhong.vfx.FxType.BREAKTHROUGH, base, net.minecraft.world.phys.Vec3.ZERO, 1f, 0xFFE9A0);
            com.hukhong.vfx.ModSounds.play(sl, p, com.hukhong.vfx.ModSounds.BREAKTHROUGH, 1.2f, 1.0f);
            sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.4f);
            sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f);
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("hukhong.breakthrough.title", Component.translatable(Realm.langKey(d.level())))));
        PacketDistributor.sendToPlayer(p, new BreakthroughPayload(20, 0.6f));
    }
}
