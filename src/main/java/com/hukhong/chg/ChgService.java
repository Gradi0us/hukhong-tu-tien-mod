package com.hukhong.chg;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server: giáng lâm, dựng hang ổ khi người chơi tới gần, boss hang ổ, sự kiện ngẫu nhiên theo cấp, lộ hang qua Văn Kiện. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ChgService {
    private ChgService() {}

    public static boolean alive(ServerLevel sl, int idx) {
        return !sl.getEntities(EntityTypeTest.forClass(ChapHanhGia.class), e -> e.idx() == idx).isEmpty();
    }

    /** Spawn CHG {@code idx} giáng lâm xuống {@code ground} (chân hạ cánh). */
    public static ChapHanhGia descend(ServerLevel sl, BlockPos ground, int idx) {
        ChapHanhGia b = ChgRegistry.type(idx).create(sl);
        if (b == null) return null;
        b.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(ground), MobSpawnType.EVENT, null);
        b.initScaling(sl);
        b.beginDescent();
        sl.addFreshEntity(b);
        ChgSavedData.get(sl).markSpawn(idx, sl.getGameTime());
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(b) < 160 * 160)
                p.displayClientMessage(Component.translatable("hukhong.chg.descend", Component.translatable("entity.hukhong.chg_" + idx)), false);
        return b;
    }

    /** Lệnh thử /hukhong chg N: giáng lâm cách người chơi 12 block. */
    public static ChapHanhGia test(ServerPlayer p, int idx) {
        ServerLevel sl = p.serverLevel();
        BlockPos xz = p.blockPosition().offset(12, 0, 0);
        return descend(sl, sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz), idx);
    }

    public static int[] lairXZ(ServerLevel ow, int idx) { return ChgKit.lairXZ(ow.getSeed(), idx); }

    /** Báo tọa độ hang ổ cho người chơi (gọi khi cờ gợi ý bật). */
    public static void revealLair(ServerPlayer p, int idx) {
        ServerLevel ow = p.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return;
        int[] xz = lairXZ(ow, idx);
        p.displayClientMessage(Component.translatable("hukhong.chg.lair.revealed",
                Component.translatable("entity.hukhong.chg_" + idx), xz[0], xz[1]), false);
    }

    /** Đủ 6 Văn Kiện cùng chuỗi: kích hoạt sớm sự kiện Chấp Hành Giả đó (nếu chưa hạ). */
    public static void earlyEvent(ServerPlayer p, int idx) {
        if (ChgKit.defeated(StoryService.get(p).lair(), idx)) return;
        ServerLevel sl = p.serverLevel();
        if (sl.dimension() != Level.OVERWORLD || alive(sl, idx)) return;
        eventNear(sl, p, idx);
    }

    private static void eventNear(ServerLevel sl, ServerPlayer p, int idx) {
        double a = p.getRandom().nextDouble() * Math.PI * 2, r = 24 + p.getRandom().nextInt(8);
        BlockPos xz = BlockPos.containing(p.getX() + Math.cos(a) * r, 0, p.getZ() + Math.sin(a) * r);
        descend(sl, sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz), idx);
        ChgSavedData.get(sl).markEvent(idx, sl.getGameTime());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        long t = e.getServer().getTickCount();
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        if (t % 40 == 0) lairTick(ow);
        if (t % ChgKit.EVENT_CHECK_TICKS == 0) eventTick(ow);
    }

    private static void lairTick(ServerLevel ow) {
        ChgSavedData data = ChgSavedData.get(ow);
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator()) continue;
            int plv = CultivationService.get(p).level();
            for (int idx = 1; idx <= ChgKit.COUNT; idx++) {
                int[] xz = lairXZ(ow, idx);
                double dx = p.getX() - xz[0], dz = p.getZ() - xz[1];
                double d2 = dx * dx + dz * dz;
                if (d2 > ChgKit.LAIR_REVEAL_DIST * (double) ChgKit.LAIR_REVEAL_DIST) continue;
                if (!data.built(idx)) {
                    int y = Math.max(ow.getHeight(Heightmap.Types.WORLD_SURFACE_WG, xz[0], xz[1]), ow.getSeaLevel() + 1);
                    ChgArena.build(ow, xz[0], y, xz[1], idx);
                    data.setBuilt(idx, y);
                }
                if (d2 > ChgKit.LAIR_TRIGGER_DIST * (double) ChgKit.LAIR_TRIGGER_DIST) continue;
                if (!ChgKit.lairEligible(plv, idx) || alive(ow, idx)) continue;
                if (ow.getGameTime() - data.lastSpawn(idx) < ChgKit.LAIR_RESPAWN_TICKS) continue;
                descend(ow, new BlockPos(xz[0], data.floorY(idx) + 2, xz[1]), idx);
            }
        }
    }

    /** Mỗi phút: người chơi cấp gần ngưỡng có xác suất gặp một CHG chưa hạ giáng xuống gần mình. */
    private static void eventTick(ServerLevel ow) {
        ChgSavedData data = ChgSavedData.get(ow);
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator()) continue;
            StoryData sd = StoryService.get(p);
            int plv = CultivationService.get(p).level();
            for (int idx = 1; idx <= ChgKit.COUNT; idx++) {
                if (!ChgKit.eventEligible(plv, idx) || ChgKit.defeated(sd.lair(), idx)) continue;
                if (ow.getGameTime() - data.lastEvent(idx) < ChgKit.EVENT_COOLDOWN_TICKS) continue;
                if (p.getRandom().nextDouble() > ChgKit.EVENT_CHANCE || alive(ow, idx)) continue;
                eventNear(ow, p, idx);
                break;
            }
        }
    }
}
