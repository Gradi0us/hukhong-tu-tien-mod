package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.entity.BangNhan;
import com.hukhong.entity.DocMau;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.SaHatVuong;
import com.hukhong.entity.V3Entities;
import com.hukhong.entity.WorldBoss;
import com.hukhong.world.SafeZones;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/** Boss the gioi theo bioma: sa mac/dat hoang -> Sa Hat Vuong, tuyet/nui -> Bang Nhan, dam lay/rung ram -> Doc Mau. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class WorldBossManager {
    private WorldBossManager() {}

    public static final int CHECK_EVERY = 1200;           // 1 phut
    public static final double CHANCE = 0.03;             // moi nguoi / phut -> ~ 1 boss moi 33 phut / nguoi
    public static final int MAX_ALIVE = 3;
    private static final long TYPE_COOLDOWN = 24000;      // 20 phut / loai
    private static final Map<Integer, Long> LAST = new HashMap<>();

    public static final int DESERT = 0, SNOW = 1, SWAMP = 2;

    /** Thuan: id bioma (path) -> loai boss, hoac -1. */
    public static int kindOf(String biomePath) {
        String b = biomePath;
        if (b.contains("desert") || b.contains("badlands") || b.equals("eroded_badlands")) return DESERT;
        if (b.contains("snowy") || b.contains("frozen") || b.contains("ice_spikes") || b.equals("grove")
                || b.contains("jagged_peaks") || b.contains("stony_peaks")) return SNOW;
        if (b.contains("swamp") || b.contains("jungle") || b.contains("mangrove")) return SWAMP;
        return -1;
    }

    public static WorldBoss create(ServerLevel sl, int kind) {
        return switch (kind) {
            case DESERT -> V3Entities.SA_HAT_VUONG.get().create(sl);
            case SNOW -> V3Entities.BANG_NHAN.get().create(sl);
            case SWAMP -> V3Entities.DOC_MAU.get().create(sl);
            default -> null;
        };
    }

    /** Sinh boss loai {@code kind} tai {@code pos}. */
    public static WorldBoss spawn(ServerLevel sl, int kind, BlockPos pos) {
        WorldBoss b = create(sl, kind);
        if (b == null) return null;
        b.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        sl.addFreshEntity(b);
        b.initBoss(sl, b.huLevel());
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 128 * 128) p.displayClientMessage(Component.translatable("hukhong.boss.world_appear", b.getType().getDescription()), false);
        return b;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().overworld();
        if (ow == null || ow.players().isEmpty() || ow.getGameTime() % CHECK_EVERY != 0) return;
        long t = ow.getGameTime();
        if (ow.getEntitiesOfClass(WorldBoss.class, new net.minecraft.world.phys.AABB(-3.0E7, -64, -3.0E7, 3.0E7, 320, 3.0E7), WorldBoss::isAlive).size() >= MAX_ALIVE) return;
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || ow.random.nextDouble() >= CHANCE) continue;
            String path = ow.getBiome(p.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("");
            int kind = kindOf(path);
            if (kind < 0 || LAST.getOrDefault(kind, -TYPE_COOLDOWN * 2) + TYPE_COOLDOWN > t) continue;
            if (SafeZones.isSafe(ow, p.blockPosition())) continue;
            if (!ow.getEntitiesOfClass(WorldBoss.class, p.getBoundingBox().inflate(160)).isEmpty()) continue;
            double a = ow.random.nextDouble() * Math.PI * 2, d = 34 + ow.random.nextDouble() * 12;
            int x = (int) (p.getX() + Math.cos(a) * d), z = (int) (p.getZ() + Math.sin(a) * d);
            if (!ow.hasChunk(x >> 4, z >> 4)) continue;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!ow.getFluidState(bp.below()).isEmpty() || SafeZones.isSafe(ow, bp)) continue;
            LAST.put(kind, t);
            spawn(ow, kind, bp);
            return;
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) { LAST.clear(); }
}
