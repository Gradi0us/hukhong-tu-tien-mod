package com.hukhong.client.ambient;

import com.hukhong.HuKhong;
import com.hukhong.client.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

/** Vòng lặp âm khí quyển (mờ vào/ra theo trọng số) + tiếng thì thầm, sấm xa. Âm thanh tổng hợp gốc, xem docs/research/LICENSES-ambient.md. */
final class AmbientSounds {
    private AmbientSounds() {}

    private static SoundEvent ev(String id) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, id));
    }

    private static final SoundEvent DRONE = ev("amb.void_drone"), DRUM = ev("amb.war_drum"), TOWN = ev("amb.town_day"),
            PORT = ev("amb.port_water"), HUM = ev("amb.dust_hum"), NIGHT = ev("amb.wild_night"),
            WHISPER = ev("amb.void_whisper"), THUNDER = ev("amb.thunder_far");

    private static final class Loop extends AbstractTickableSoundInstance {
        float target;
        boolean stopping;
        private final float fadeSpeed;

        Loop(SoundEvent e, float pitch, float fadeSec) {
            super(e, SoundSource.AMBIENT, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0001f;
            this.pitch = pitch;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.fadeSpeed = 1f / (fadeSec * 20f);
        }

        @Override
        public void tick() {
            float t = stopping ? 0f : target;
            volume = volume < t ? Math.min(t, volume + fadeSpeed * 0.7f) : Math.max(t, volume - fadeSpeed);
            if (stopping && volume <= 0.002f) stop();
        }
    }

    private static final Map<SoundEvent, Loop> LOOPS = new HashMap<>();
    private static int whisperCd = 200;

    static void clear() {
        Minecraft mc = Minecraft.getInstance();
        for (Loop l : LOOPS.values()) mc.getSoundManager().stop(l);
        LOOPS.clear();
    }

    private static void drive(Minecraft mc, SoundEvent e, float vol, float pitch, float fadeSec) {
        Loop l = LOOPS.get(e);
        if (l != null && l.isStopped()) { LOOPS.remove(e); l = null; }
        if (vol > 0.012f) {
            if (l == null) {
                l = new Loop(e, pitch, fadeSec);
                LOOPS.put(e, l);
                l.target = vol;
                mc.getSoundManager().play(l);
            } else { l.target = vol; l.stopping = false; }
        } else if (l != null) {
            l.stopping = true;
        }
    }

    static void tick(Minecraft mc, ClientLevel lv) {
        float vol = ClientConfig.ambientVolume() * (ClientConfig.ambientVfx() ? 1f : 0f);
        float pt = 1f;
        float c = AmbientClient.corruption(pt), w = AmbientClient.war(pt), p = AmbientClient.peace(pt), d = AmbientClient.dust(pt);
        float day = AmbientSky.dayFactor(lv, 1f);
        boolean overworld = AmbientClient.overworld();
        int zk = AmbientClient.zoneKind();
        boolean port = zk == AmbientClient.Z_PORT;

        drive(mc, DRONE, vol * c * 0.62f, 1f, 6f);
        drive(mc, DRUM, vol * w * 0.55f, 1f, 5f);
        drive(mc, TOWN, vol * p * (port ? 0.6f : 1f) * (0.25f + 0.75f * day) * 0.5f, 1f, 5f);
        drive(mc, PORT, port ? vol * p * 0.55f : 0f, 1f, 5f);
        float tierPitch = AmbientClient.inBiCanh() ? 0.8f + 0.1f * Math.max(0, AmbientState.bicanhTier()) : 1.0f;
        drive(mc, HUM, vol * d * (AmbientClient.inBiCanh() ? 0.4f : 0.3f), tierPitch, 6f);
        float wild = overworld ? AmbientClient.night() * (1f - AmbientClient.rain()) * (1f - Math.max(Math.max(c, w), p)) : 0f;
        drive(mc, NIGHT, vol * wild * 0.35f, 1f, 6f);

        RandomSource rnd = lv.random;
        // thì thầm hiếm trong vùng nhiễm
        if (c > 0.45f && vol > 0.01f && --whisperCd <= 0) {
            whisperCd = 260 + rnd.nextInt(520);
            double ang = rnd.nextDouble() * Math.PI * 2, dist = 4 + rnd.nextDouble() * 8;
            mc.getSoundManager().play(new SimpleSoundInstance(WHISPER, SoundSource.AMBIENT, 0.55f * vol * c, 0.8f + rnd.nextFloat() * 0.35f, rnd,
                    mc.player.getX() + Math.cos(ang) * dist, mc.player.getY() + 1 + rnd.nextDouble() * 2, mc.player.getZ() + Math.sin(ang) * dist));
        }
        // sấm xa khi bão
        if (overworld && lv.isThundering() && vol > 0.01f && rnd.nextInt(20 * 14) == 0) thunder(mc, 0.5f * vol, 0.85f + rnd.nextFloat() * 0.3f);
    }

    /** Sấm xa, không định vị (nghe như từ rất xa). */
    static void thunder(Minecraft mc, float volume, float pitch) {
        if (volume <= 0.01f * 0.5f) return;
        float v = volume * Mth.clamp(ClientConfig.ambientVolume(), 0f, 1f);
        if (v <= 0.005f) return;
        mc.getSoundManager().play(new SimpleSoundInstance(THUNDER.getLocation(), SoundSource.AMBIENT, v, pitch, RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }
}
