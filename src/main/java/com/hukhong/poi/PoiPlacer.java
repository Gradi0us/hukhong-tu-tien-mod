package com.hukhong.poi;

import com.hukhong.HuKhong;
import com.hukhong.city.DrySites;
import com.hukhong.world.SafeZones;
import com.hukhong.zone.ZoneData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dat dia diem khi chunk nap (khong chan tao the gioi): chunk chua ung vien (PoiMath) -> xep hang doi; moi 10 tick dung toi da 1 dia diem,
 * chi khi toan bo chunk trong dien tich da nap day du. Kiem tra: dat kho, bioma, khong de len thanh / vung xam thuc / diem hoi sinh, khoang cach toi thieu.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PoiPlacer {
    private PoiPlacer() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final int SPAWN_CLEAR = 192, CROWD = 64, MAX_SLOPE = 6, WAIT_TICKS = 3600;

    private record Pending(PoiDef def, int x, int z, long expire) {}
    private static final Map<String, Pending> QUEUE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel l) || l.dimension() != Level.OVERWORLD) return;
        int cx = e.getChunk().getPos().x, cz = e.getChunk().getPos().z;
        long seed = l.getSeed();
        for (PoiDef d : PoiDefs.all()) {
            int[] c = PoiMath.candidateInChunk(seed, d.id(), d.rarity(), cx, cz);
            if (c == null) continue;
            QUEUE.putIfAbsent(PoiInstance.keyOf(d.id(), c[0], c[1]), new Pending(d, c[0], c[1], l.getGameTime() + WAIT_TICKS));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (QUEUE.isEmpty() || e.getServer().getTickCount() % 10 != 3) return;
        ServerLevel ow = e.getServer().overworld();
        PoiSavedData data = PoiSavedData.get(ow);
        long now = ow.getGameTime();
        for (var en : QUEUE.entrySet()) {
            Pending p = en.getValue();
            if (now > p.expire() || data.known(en.getKey())) { QUEUE.remove(en.getKey()); continue; }
            int half = PoiBuilders.half(p.def().builder());
            if (!ow.hasChunksAt(new BlockPos(p.x() - half, 0, p.z() - half), new BlockPos(p.x() + half, 0, p.z() + half))) continue;
            QUEUE.remove(en.getKey());
            String why = evaluate(ow, p.def(), p.x(), p.z(), data);
            if (why != null) { data.markSkipped(en.getKey()); LOG.debug("POI {} skipped: {}", en.getKey(), why); continue; }
            try {
                place(ow, data, p.def(), p.x(), p.z());
            } catch (Exception ex) {
                LOG.error("POI {} build failed: {}", en.getKey(), ex.toString());
                data.markSkipped(en.getKey());
            }
            return; // toi da 1 cong trinh / lan
        }
    }

    /** Dung ngay (khong kiem tra) va ghi vao du lieu. Tra ve instance. */
    public static PoiInstance place(ServerLevel ow, PoiSavedData data, PoiDef d, int x, int z) {
        int seed = (int) (PoiMath.hash(ow.getSeed(), d.id(), x, z) >>> 20);
        PoiBuilders.Built b = PoiBuilders.build(ow, d, x, z, seed);
        PoiInstance inst = new PoiInstance(d.id(), x, b.y0(), z, seed);
        inst.acts.addAll(b.acts());
        inst.stateTick = ow.getGameTime();
        data.add(inst);
        PoiService.afterPlace(ow, inst, d);
        LOG.info("POI {} built at {} {} {}", inst.key, x, b.y0(), z);
        return inst;
    }

    /** null = hop le; con lai = ly do tu choi (spawn / zone / city / biome / terrain / spacing / crowded). */
    public static String evaluate(ServerLevel ow, PoiDef d, int x, int z, PoiSavedData data) {
        int half = PoiBuilders.half(d.builder());
        BlockPos sp = ow.getSharedSpawnPos();
        if (!PoiMath.farEnough(x, z, sp.getX(), sp.getZ(), SPAWN_CLEAR)) return "spawn";
        for (int dx = -half; dx <= half; dx += half) for (int dz = -half; dz <= half; dz += half) {
            if (ZoneData.at(x + dx, z + dz) != null) return "zone";
            if (SafeZones.isSafe(ow, new BlockPos(x + dx, ow.getSeaLevel(), z + dz))) return "city";
        }
        if (!biomeOk(ow, d, x, z)) return "biome";
        if (!DrySites.suitable(ow, x, z, Math.max(6, half), MAX_SLOPE)) return "terrain";
        if (data.anyWithin(d.id(), x, z, d.minSpacing())) return "spacing";
        if (data.anyWithin(null, x, z, CROWD)) return "crowded";
        return null;
    }

    public static boolean biomeOk(ServerLevel ow, PoiDef d, int x, int z) {
        var b = ow.getBiome(new BlockPos(x, ow.getSeaLevel(), z));
        if (b.is(BiomeTags.IS_OCEAN) || b.is(BiomeTags.IS_RIVER)) return false;
        for (String spec : d.biomes()) {
            if (spec.equals("*")) return true;
            if (spec.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(spec.substring(1));
                if (rl != null && b.is(TagKey.<Biome>create(Registries.BIOME, rl))) return true;
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(spec);
                if (rl != null && b.is(rl)) return true;
            }
        }
        return false;
    }
}
