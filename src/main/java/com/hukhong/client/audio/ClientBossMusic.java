package com.hukhong.client.audio;

import com.hukhong.HuKhong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Client: nhac boss lap (category MUSIC), fade in/out, chan nhac nen vanilla khi dang phat. */
@EventBusSubscriber(modid = HuKhong.MODID, value = Dist.CLIENT)
public final class ClientBossMusic {
    private ClientBossMusic() {}

    private static Instance cur;
    private static String curId;

    public static void handle(String track, boolean start) {
        if (cur != null && cur.isStopped()) { cur = null; curId = null; }
        if (!start) {
            if (cur != null && track.equals(curId)) { cur.fadeOut = true; cur = null; curId = null; }
            return;
        }
        if (cur != null && track.equals(curId)) { cur.fadeOut = false; return; }
        if (cur != null) cur.fadeOut = true;
        Minecraft mc = Minecraft.getInstance();
        mc.getMusicManager().stopPlaying();
        cur = new Instance(track);
        curId = track;
        mc.getSoundManager().play(cur);
    }

    @SubscribeEvent
    public static void onPlay(PlaySoundEvent e) {
        SoundInstance s = e.getSound();
        if (cur == null || s == null || s.getSource() != SoundSource.MUSIC) return;
        if (!HuKhong.MODID.equals(s.getLocation().getNamespace())) e.setSound(null);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        if (cur != null) cur.halt();
        cur = null; curId = null;
    }

    private static final class Instance extends AbstractTickableSoundInstance {
        boolean fadeOut;
        void halt() { stop(); }

        Instance(String id) {
            super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "music." + id)),
                    SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01f;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override public boolean canStartSilent() { return true; }

        @Override public void tick() {
            if (fadeOut) {
                volume -= 0.02f;
                if (volume <= 0f) stop();
            } else if (volume < 1f) {
                volume = Math.min(1f, volume + 0.02f);
            }
        }
    }
}
