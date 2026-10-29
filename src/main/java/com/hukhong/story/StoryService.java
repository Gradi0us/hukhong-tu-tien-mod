package com.hukhong.story;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.events.CorruptionData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server: chọn mode, thu Văn Kiện, mở khóa gợi ý hang ổ, Sứ Đồ xuất hiện ở vùng Hư Hóa. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class StoryService {
    private StoryService() {}

    public static final int MAX_LEVEL = 65, MIN_NATURAL_PLAYER_LEVEL = 30, SPAWN_INTERVAL = 600;
    private static final int[] MIN_LEVEL = {15, 18, 30};

    public static StoryData get(ServerPlayer p) { return p.getData(StoryRegistry.STORY.get()); }

    public static void sync(ServerPlayer p) { PacketDistributor.sendToPlayer(p, new StoryNet.Sync(get(p))); }

    /** Mở màn chọn mode nếu đã tạo nhân vật xong mà chưa chọn. */
    public static void promptIfNeeded(ServerPlayer p) {
        if (get(p).mode() == StoryData.MODE_UNSET && !CultivationService.get(p).paths().isEmpty())
            PacketDistributor.sendToPlayer(p, new StoryNet.OpenMode());
    }

    /** Chỉ chọn được một lần. */
    public static void chooseMode(ServerPlayer p, int mode) {
        StoryData d = get(p);
        if (d.mode() != StoryData.MODE_UNSET || (mode != StoryData.MODE_STORY && mode != StoryData.MODE_FREE)) return;
        p.setData(StoryRegistry.STORY.get(), d.withMode(mode));
        p.displayClientMessage(Component.translatable(mode == StoryData.MODE_STORY
                ? "hukhong.story.mode.set.story" : "hukhong.story.mode.set.free"), false);
        sync(p);
        if (mode == StoryData.MODE_STORY) com.hukhong.story.flow.StoryFlow.startPrologue(p);
    }

    /** Ghi Văn Kiện vào nhật ký. Trả true nếu mới (item bị tiêu hao). */
    public static boolean collect(ServerPlayer p, String docId) {
        StoryDocs.Doc doc = StoryDocs.get(docId);
        if (doc == null) return false;
        StoryData d = get(p);
        if (d.has(docId)) {
            p.displayClientMessage(Component.translatable("hukhong.story.already"), true);
            return false;
        }
        d = d.withDoc(docId);
        p.displayClientMessage(Component.translatable("hukhong.story.collected", Component.translatable(doc.itemKey())), false);
        p.displayClientMessage(Component.translatable(doc.textKey()), false);
        if (doc.chain() == 'c') { // Di thư Tiền Nhân: không gợi ý hang, chỉ ghi nhật ký
            p.setData(StoryRegistry.STORY.get(), d);
            sync(p);
            return true;
        }
        int bit = StoryDocs.lairBit(doc.chain());
        if ((d.lair() & bit) == 0 && d.chainCount(doc.chain()) >= StoryDocs.CLUES_FOR_LAIR) {
            d = d.withLair(bit); // chỉ lưu cờ; boss CHG ở bước sau
            p.displayClientMessage(Component.translatable("hukhong.story.lair." + doc.chain()), false);
            com.hukhong.chg.ChgService.revealLair(p, doc.chain() == 'a' ? 1 : 2);
        }
        boolean full = d.chainCount(doc.chain()) == 6; // đúng lần thứ 6 (bộ mở rộng lên 10 không kích lại)
        p.setData(StoryRegistry.STORY.get(), d);
        sync(p);
        if (full) com.hukhong.chg.ChgService.earlyEvent(p, doc.chain() == 'a' ? 1 : 2);
        return true;
    }

    public static SuDo spawn(ServerLevel sl, BlockPos pos, int variant, int level) {
        SuDo s = StoryRegistry.variant(variant).create(sl);
        if (s == null) return null;
        s.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        s.setLevel(Math.max(1, Math.min(MAX_LEVEL, level)));
        sl.addFreshEntity(s);
        return s;
    }

    /** Lệnh thử: spawn gần người chơi (variant -1 = ngẫu nhiên), cấp theo người chơi, kẹp [min, 65]. */
    public static SuDo spawnNear(ServerPlayer p, int variant) {
        int v = variant < 0 ? p.getRandom().nextInt(3) : variant;
        int lv = Math.max(MIN_LEVEL[v], Math.min(MAX_LEVEL, CultivationService.get(p).level()));
        return spawn(p.serverLevel(), p.blockPosition().offset(5, 0, 0), v, lv);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        sync(p);
        promptIfNeeded(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) sync(p);
    }

    /** Mỗi 30s: người chơi cấp >=30 đứng trong chunk Hư Hóa có xác suất gặp một Sứ Đồ. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % SPAWN_INTERVAL != 0) return;
        ServerLevel ow = e.getServer().getLevel(Level.OVERWORLD);
        if (ow == null) return;
        CorruptionData cd = CorruptionData.get(ow); // chỉ đọc
        if (cd.size() == 0) return;
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || !cd.isCorrupted(p.blockPosition())) continue;
            int plv = CultivationService.get(p).level();
            if (plv < MIN_NATURAL_PLAYER_LEVEL || p.getRandom().nextFloat() > 0.35f) continue;
            if (!ow.getEntitiesOfClass(SuDo.class, p.getBoundingBox().inflate(64)).isEmpty()) continue;
            double a = p.getRandom().nextDouble() * Math.PI * 2, r = 14 + p.getRandom().nextInt(13);
            BlockPos xz = BlockPos.containing(p.getX() + Math.cos(a) * r, 0, p.getZ() + Math.sin(a) * r);
            if (!cd.isCorrupted(xz)) continue;
            BlockPos pos = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
            int v = p.getRandom().nextInt(3);
            int lv = Math.max(MIN_LEVEL[v], Math.min(MAX_LEVEL, plv + p.getRandom().nextInt(9) - 5));
            spawn(ow, pos, v, lv);
        }
    }
}
