package com.hukhong.poi;

import com.hukhong.HuKhong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

/**
 * Không khí địa điểm (2026-11-06): khi người chơi trong 40 khối, mỗi địa điểm phát hạt + tiếng riêng.
 * Chiến trường: tro bay, lửa hồn, tiếng gió rít; mộ: hồn trắng lảng vảng; kiếm trủng: kiếm khí lấp lánh quanh cự kiếm;
 * ma uyên: hạt hư không trồi lên; linh tuyền: sương linh khí; ...
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PoiAmbient {
    private PoiAmbient() {}

    static final double RANGE = 40;
    static final DustParticleOptions ASH = new DustParticleOptions(new Vector3f(0.25f, 0.22f, 0.22f), 1.4f);
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.4f), 1.0f);
    static final DustParticleOptions JADE = new DustParticleOptions(new Vector3f(0.5f, 1f, 0.8f), 1.2f);
    static final DustParticleOptions BLADE = new DustParticleOptions(new Vector3f(0.75f, 0.9f, 1f), 1.0f);

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 5 != 0) return;
        ServerLevel ow = e.getServer().overworld();
        if (ow.players().isEmpty()) return;
        PoiSavedData data = PoiSavedData.get(ow);
        for (ServerPlayer p : ow.players()) {
            for (PoiInstance i : data.near(p.getX(), p.getZ(), RANGE)) {
                PoiDef d = PoiDefs.get(i.defId);
                if (d != null) emit(ow, p, d.builder(), i, e.getServer().getTickCount());
            }
        }
    }

    static void around(ServerLevel l, ServerPlayer p, ParticleOptions o, PoiInstance i, double r, double y0, double y1, int n, double speed) {
        RandomSource rn = l.random;
        for (int k = 0; k < n; k++) {
            double a = rn.nextDouble() * Math.PI * 2, d = Math.sqrt(rn.nextDouble()) * r;
            l.sendParticles(p, o, false, i.x + 0.5 + Math.cos(a) * d, i.y + y0 + rn.nextDouble() * (y1 - y0), i.z + 0.5 + Math.sin(a) * d, 1, 0, 0.02, 0, speed);
        }
    }

    static void sound(ServerLevel l, ServerPlayer p, PoiInstance i, SoundEvent s, float vol, float pitch) {
        l.playSound(null, new BlockPos(i.x, i.y + 2, i.z), s, SoundSource.AMBIENT, vol, pitch);
    }

    static void emit(ServerLevel l, ServerPlayer p, String b, PoiInstance i, int t) {
        switch (b) {
            case "battlefield" -> {
                around(l, p, ASH, i, 16, 0.2, 5, 6, 0);
                around(l, p, ParticleTypes.WHITE_ASH, i, 16, 0.5, 6, 8, 0.01);
                if (t % 20 == 0) around(l, p, ParticleTypes.SOUL_FIRE_FLAME, i, 14, 0.2, 1, 3, 0.01);
                if (t % 15 == 0) around(l, p, ParticleTypes.SOUL, i, 12, 0.3, 2, 1, 0.02);
                if (t % 200 == 0) sound(l, p, i, SoundEvents.SOUL_ESCAPE.value(), 1.4f, 0.6f);
                if (t % 340 == 0) sound(l, p, i, SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value(), 1f, 0.8f);
            }
            case "grave" -> {
                if (t % 10 == 0) around(l, p, ParticleTypes.SOUL, i, 4, 0.5, 2.5, 1, 0.01);
                around(l, p, ParticleTypes.MYCELIUM, i, 7, 0.1, 1.2, 3, 0);
                if (t % 260 == 0) sound(l, p, i, SoundEvents.SOUL_ESCAPE.value(), 1f, 0.5f);
            }
            case "sword_mound" -> {
                around(l, p, BLADE, i, 1.6, 2, 16, 4, 0);
                if (t % 10 == 0) l.sendParticles(p, ParticleTypes.ELECTRIC_SPARK, false, i.x + 0.5, i.y + 11.5, i.z + 0.5, 6, 2, 0.3, 0.2, 0.15);
                if (t % 20 == 0) around(l, p, ParticleTypes.END_ROD, i, 11, 0.5, 3, 3, 0.01);
                if (t % 300 == 0) sound(l, p, i, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 0.5f);
            }
            case "abyss" -> {
                around(l, p, ParticleTypes.REVERSE_PORTAL, i, 5, -5, 0, 8, 0.05);
                if (t % 10 == 0) around(l, p, ParticleTypes.SCULK_SOUL, i, 4, -5, -2, 2, 0.02);
                if (t % 240 == 0) sound(l, p, i, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 1.2f, 0.6f);
            }
            case "spring" -> {
                around(l, p, JADE, i, 3, 0, 2, 4, 0);
                around(l, p, ParticleTypes.GLOW, i, 3, 0.1, 1.5, 2, 0.01);
            }
            case "waterfall" -> around(l, p, JADE, i, 3, 0, 4, 4, 0);
            case "rift" -> {
                around(l, p, ParticleTypes.PORTAL, i, 3, 0, 5, 10, 0.4);
                if (t % 10 == 0) around(l, p, ParticleTypes.REVERSE_PORTAL, i, 5, 0, 4, 4, 0.05);
            }
            case "ruin" -> {
                around(l, p, ParticleTypes.WHITE_ASH, i, 18, 0.5, 6, 5, 0.01);
                if (t % 20 == 0) around(l, p, ParticleTypes.ENCHANT, i, 3, 1, 3, 6, 0.3);
            }
            case "shrine", "dao_quan", "stele" -> {
                if (t % 10 == 0) around(l, p, ParticleTypes.CAMPFIRE_COSY_SMOKE, i, 1.5, 1.2, 1.5, 1, 0.005); // khói nhang
                around(l, p, GOLD, i, 6, 0.5, 4, 2, 0);
                if (b.equals("dao_quan") && t % 400 == 0) sound(l, p, i, SoundEvents.BELL_RESONATE, 0.8f, 0.8f);
            }
            case "wuxing" -> {
                around(l, p, GOLD, i, 7, 0.2, 4, 4, 0);
                if (t % 20 == 0) around(l, p, ParticleTypes.ENCHANT, i, 2, 1, 3, 8, 0.5);
            }
            case "arena" -> around(l, p, ParticleTypes.WHITE_ASH, i, 9, 0.2, 3, 3, 0.01);
            case "cave" -> around(l, p, ParticleTypes.DRIPPING_WATER, i, 5, 3, 3.5, 2, 0);
            default -> { }
        }
    }
}
