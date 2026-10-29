package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.core.CultivationService;
import com.hukhong.events.InvasionManager;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryDocs;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.SuDo;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Nguyet Thuc (Da Khuyet): tap kich vao thanh da chon cua su kien (khong con luon thanh goc). Tu chuong 4 co mot Su Do chi huy
 * dung gan thanh; ha Su Do chi huy chac chan roi mot Van Kien chuoi A (theo bac cap Su Do). Su Do con song khi su kien ket thuc se bien mat.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class NguyetThucHandler {
    private NguyetThucHandler() {}

    public static final int COMMANDER_CHAPTER = 4;
    public static final String TAG = "hk_nguyet_commander";

    public static void begin(ServerLevel ow, CalendarData.Act a) {
        String err = a.city() >= 0 ? InvasionManager.start(ow, a.city()) : "no_city";
        if (err != null) err = InvasionManager.start(ow, -1); // thanh da chon khong hop le (RUIN / chua dung): chon theo chuong / ngau nhien
        if (err != null || !anyAtChapter(ow)) return;
        int idx = InvasionManager.targetCity();
        BlockPos c = idx < 0 ? null : CityService.builtCenter(ow, CityDef.ALL.get(idx));
        if (c != null) spawnCommander(ow, c);
    }

    /** Thuan: cap Su Do chi huy theo cap trung binh nguoi choi (30..65, tran 65 cua Su Do). */
    public static int commanderLevel(int avgLevel) { return Math.max(30, Math.min(65, avgLevel)); }

    private static boolean anyAtChapter(ServerLevel ow) {
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator()) continue;
            if (StoryHooks.chapter(p) >= COMMANDER_CHAPTER || p.getData(StoryRegistry.STORY.get()).mode() == StoryData.MODE_FREE) return true;
        }
        return false;
    }

    private static void spawnCommander(ServerLevel ow, BlockPos c) {
        int n = 0, sum = 0;
        for (ServerPlayer p : ow.players()) { n++; sum += CultivationService.get(p).level(); }
        SuDo s = StoryRegistry.variant(ow.random.nextInt(3)).create(ow);
        if (s == null) return;
        double ang = ow.random.nextDouble() * Math.PI * 2;
        int x = c.getX() + (int) (Math.cos(ang) * 22), z = c.getZ() + (int) (Math.sin(ang) * 22);
        ow.getChunk(x >> 4, z >> 4);
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        s.moveTo(x + 0.5, y, z + 0.5, ow.random.nextFloat() * 360f, 0f);
        s.finalizeSpawn(ow, ow.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null);
        s.setLevel(commanderLevel(n == 0 ? 30 : sum / n));
        s.setPersistenceRequired();
        s.addTag(TAG);
        s.setCustomName(Component.translatable("hukhong.cal.nguyet_thuc.commander"));
        ow.addFreshEntity(s);
    }

    public static void end(ServerLevel ow, CalendarData.Act a) {
        List<SuDo> left = new ArrayList<>();
        for (var e : ow.getAllEntities()) if (e instanceof SuDo s && s.getTags().contains(TAG) && s.isAlive()) left.add(s);
        for (SuDo s : left) s.discard();
    }

    /** Thuan: Van Kien chuoi A hop bac cua Su Do cap level, theo pick (0..1). null neu khong co. */
    public static StoryDocs.Doc docFor(int level, double pick) {
        int tier = StoryDocs.tierOf(level);
        List<StoryDocs.Doc> ok = new ArrayList<>();
        for (StoryDocs.Doc d : StoryDocs.ALL) if (d.chain() == 'a' && tier >= d.minTier() && tier <= d.maxTier()) ok.add(d);
        return ok.isEmpty() ? null : ok.get(Math.min(ok.size() - 1, (int) (pick * ok.size())));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof SuDo s) || !s.getTags().contains(TAG) || !(s.level() instanceof ServerLevel sl)) return;
        StoryDocs.Doc d = docFor(s.suLevel(), sl.random.nextDouble());
        if (d != null) sl.addFreshEntity(new ItemEntity(sl, s.getX(), s.getY() + 0.5, s.getZ(), new ItemStack(StoryRegistry.DOC_ITEMS.get(d.id()).get())));
    }
}
