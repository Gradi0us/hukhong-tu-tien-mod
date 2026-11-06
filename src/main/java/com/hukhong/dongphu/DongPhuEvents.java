package com.hukhong.dongphu;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Nhịp Động Phủ: ranh giới (4 tick), công phá (5 tick), tầng/hồi phục/thúc cây (20 tick); Hộ Trận chặn quái tự sinh. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class DongPhuEvents {
    private DongPhuEvents() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.overworld() == null) return;
        long t = s.overworld().getGameTime();
        if (t % 4 == 0) DongPhuFx.tick();
        if (t % 5 == 0) DongPhuRaid.tick(s);
        if (t % 20 == 0) DongPhuService.tick(s);
    }

    /** Quái Hư Không tự sinh (không phải do công phá/lệnh) không xuất hiện trong vùng Động Phủ. */
    static boolean blocked(ServerLevel lv, Entity en, MobSpawnType t) {
        return isNatural(t) && en instanceof HuMob && DongPhuService.isWarded(lv, en.getX(), en.getZ());
    }

    static boolean isNatural(MobSpawnType t) {
        return t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.SPAWNER
                || t == MobSpawnType.STRUCTURE || t == MobSpawnType.REINFORCEMENT || t == MobSpawnType.PATROL;
    }

    @SubscribeEvent
    public static void onPosition(MobSpawnEvent.PositionCheck e) {
        if (blocked(e.getLevel().getLevel(), e.getEntity(), e.getSpawnType())) e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    @SubscribeEvent
    public static void onFinalize(FinalizeSpawnEvent e) {
        if (blocked(e.getLevel().getLevel(), e.getEntity(), e.getSpawnType())) e.setSpawnCancelled(true);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) { DongPhuRaid.clear(); }
}
