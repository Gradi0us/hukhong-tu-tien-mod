package com.hukhong.world;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Luat sinh quai: ngoai vung an toan chi quai Hu Hoa sinh tu nhien (quai vanilla thu dich bi chan);
 * trong thanh/son mon khong sinh quai thu dich nao. Dong vat hien lanh giu nguyen. Do kho khoa HARD.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SpawnRules {
    private SpawnRules() {}

    /** Nguon sinh "tu nhien" bi luat nay quan ly (spawner, trung, lenh, su kien... khong bi chan). */
    static boolean isNatural(MobSpawnType t) {
        return t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.STRUCTURE
                || t == MobSpawnType.REINFORCEMENT || t == MobSpawnType.PATROL;
    }

    /** Thuan: chan hay khong. */
    public static boolean blocked(boolean natural, boolean hostile, boolean huMob, boolean safeZone) {
        if (!natural || !hostile) return false;
        return safeZone || !huMob;
    }

    private static boolean blocked(ServerLevel l, Entity e, MobSpawnType t) {
        return blocked(isNatural(t), e instanceof Enemy, e instanceof HuMob, SafeZones.isSafe(l, e.blockPosition()));
    }

    @SubscribeEvent
    public static void onPosition(MobSpawnEvent.PositionCheck e) {
        if (blocked(e.getLevel().getLevel(), e.getEntity(), e.getSpawnType()))
            e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    /** Lop du phong (sinh khi tao chunk / nguon khong qua PositionCheck). */
    @SubscribeEvent
    public static void onFinalize(FinalizeSpawnEvent e) {
        if (blocked(e.getLevel().getLevel(), e.getEntity(), e.getSpawnType())) e.setSpawnCancelled(true);
    }

    // ---- do kho HARD ----

    private static void lock(MinecraftServer s) {
        if (s.getWorldData().getDifficulty() != Difficulty.HARD) s.setDifficulty(Difficulty.HARD, true);
        if (!s.getWorldData().isDifficultyLocked()) s.setDifficultyLocked(true);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) { lock(e.getServer()); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 == 0) lock(e.getServer()); // chan /difficulty ha thap
    }
}
