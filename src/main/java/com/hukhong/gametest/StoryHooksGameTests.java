package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryDocs;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.world.Faction;
import com.hukhong.story.world.HuAnh;
import com.hukhong.story.world.Lore;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** GameTest StoryHooks: allowed() theo chuong/co/che do; complete() dat co + trao Di thu + xep loi Mo Dao 1 lan; Hu Anh. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class StoryHooksGameTests {
    private StoryHooksGameTests() {}

    private static final Lore LORE = new Lore(Faction.TIEN_NHAN, 3, 5, List.of("req.x"), List.of("wl.test_done"), "c1", null, "hukhong.test.modao");

    private static ServerPlayer player(GameTestHelper h, int mode, int chapter) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hooks-" + mode + chapter));
        p.setData(StoryRegistry.STORY.get(), StoryData.DEFAULT.withMode(mode));
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(chapter));
        return p;
    }

    @GameTest(template = "empty")
    public static void allowedRespectsChapterFlagsAndMode(GameTestHelper h) {
        ServerPlayer p = player(h, StoryData.MODE_STORY, 4);
        if (StoryHooks.allowed(p, LORE)) h.fail("allowed without required flag");
        DialogueService.setQuest(p, DialogueService.quest(p).withFlag("req.x"));
        if (!StoryHooks.allowed(p, LORE)) h.fail("chapter 4 + flag should be allowed");
        ServerPlayer lo = player(h, StoryData.MODE_STORY, 2);
        DialogueService.setQuest(lo, DialogueService.quest(lo).withFlag("req.x"));
        if (StoryHooks.allowed(lo, LORE)) h.fail("chapter 2 < min should be rejected");
        ServerPlayer hi = player(h, StoryData.MODE_STORY, 8);
        DialogueService.setQuest(hi, DialogueService.quest(hi).withFlag("req.x"));
        if (StoryHooks.allowed(hi, LORE)) h.fail("chapter 8 > max should be rejected");
        ServerPlayer free = player(h, StoryData.MODE_FREE, 1);
        DialogueService.setQuest(free, DialogueService.quest(free).withFlag("req.x"));
        if (!StoryHooks.allowed(free, LORE)) h.fail("free mode must ignore chapter filter");
        if (StoryHooks.chapter(p) != 4) h.fail("chapter()=" + StoryHooks.chapter(p));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void completeSetsFlagsGivesClueAndQueuesModaoOnce(GameTestHelper h) {
        ServerPlayer p = player(h, StoryData.MODE_STORY, 4);
        StoryHooks.complete(p, LORE);
        QuestData q = DialogueService.quest(p);
        if (!q.has("wl.test_done")) h.fail("sets_flags not applied");
        if (!p.getData(StoryRegistry.STORY.get()).has("c1")) h.fail("clue c1 not granted");
        if (StoryDocs.countSet(p, 'c') != 1) h.fail("countSet c=" + StoryDocs.countSet(p, 'c'));
        if (!"hukhong.test.modao".equals(StoryHooks.pendingModaoLine(p))) h.fail("modao line not queued");
        StoryHooks.markTold(p, "hukhong.test.modao");
        StoryHooks.complete(p, LORE);
        if (StoryHooks.pendingModaoLine(p) != null) h.fail("modao line queued twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void huAnhClampsAndFindsNearestCity(GameTestHelper h) {
        var ow = h.getLevel().getServer().overworld();
        int before = HuAnh.get(ow, 3);
        HuAnh.add(ow, 3, 500);
        if (HuAnh.get(ow, 3) != 100) h.fail("not clamped high: " + HuAnh.get(ow, 3));
        HuAnh.add(ow, 3, -500);
        if (HuAnh.get(ow, 3) != 0) h.fail("not clamped low");
        HuAnh.add(ow, 3, before);
        int idx = HuAnh.nearestCity(ow, new BlockPos(ow.getSharedSpawnPos().getX() + 6600, 64, ow.getSharedSpawnPos().getZ() - 5000));
        if (idx != 9) h.fail("nearestCity=" + idx);
        h.succeed();
    }
}
