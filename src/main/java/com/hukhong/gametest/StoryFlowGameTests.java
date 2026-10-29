package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.core.ModAttachments;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.objective.Objectives;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** GameTest luồng truyện: mở đầu theo xuất thân đặt mục tiêu; đột phá mốc 10 cấp sang chương mới + mục tiêu mới. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class StoryFlowGameTests {
    private StoryFlowGameTests() {}

    private static ServerPlayer player(GameTestHelper h, int level, String origin, int mode) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "flow-" + origin + level));
        CultivationData d = new CultivationData(level, 0, 2000, 2000, Set.of(), Map.of(), false)
                .withCharacter(Set.of(), Map.of(), origin, null);
        p.setData(ModAttachments.CULTIVATION, d);
        p.setData(StoryRegistry.STORY.get(), StoryData.DEFAULT.withMode(mode));
        DialogueService.setQuest(p, QuestData.DEFAULT);
        Objectives.clear(p);
        return p;
    }

    @GameTest(template = "empty")
    public static void prologueSetsObjectiveCity(GameTestHelper h) {
        ServerPlayer p = player(h, 1, "city", StoryData.MODE_STORY);
        StoryFlow.startPrologue(p);
        if (!"pro_city_raid".equals(Objectives.currentId(p))) h.fail("city prologue objective=" + Objectives.currentId(p));
        if (!"pro_city_raid".equals(DialogueService.quest(p).step())) h.fail("step=" + DialogueService.quest(p).step());
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void prologueSetsObjectiveVillageAndSect(GameTestHelper h) {
        ServerPlayer v = player(h, 1, "village", StoryData.MODE_STORY);
        StoryFlow.startPrologue(v);
        if (!"pro_vil_raid".equals(Objectives.currentId(v))) h.fail("village objective=" + Objectives.currentId(v));
        ServerPlayer s = player(h, 1, "sect", StoryData.MODE_STORY);
        StoryFlow.startPrologue(s);
        if (!"pro_sect_talk".equals(Objectives.currentId(s))) h.fail("sect objective=" + Objectives.currentId(s));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void freeModeHasNoMainObjective(GameTestHelper h) {
        ServerPlayer p = player(h, 1, "city", StoryData.MODE_FREE);
        StoryFlow.startPrologue(p);
        if (Objectives.currentId(p) != null) h.fail("free mode objective=" + Objectives.currentId(p));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void milestoneBreakthroughAdvancesChapter(GameTestHelper h) {
        ServerPlayer p = player(h, 10, "village", StoryData.MODE_STORY);
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(1).withFlag(StoryFlow.F_V2).withStep("c1_ms"));
        if (!CultivationService.get(p).milestoneLocked()) h.fail("level 10 phai bi khoa moc");
        StoryFlow.onProgress(p); // dang khoa moc: van o chuong 1
        if (DialogueService.quest(p).raw() != 1) h.fail("truoc khi mo khoa chapter=" + DialogueService.quest(p).raw());
        CultivationService.unlockMilestone(p);
        QuestData q = DialogueService.quest(p);
        if (q.raw() != 2) h.fail("sau khi mo khoa chapter=" + q.raw());
        if (!"c2_sect".equals(q.step())) h.fail("step=" + q.step());
        if (!"c2_sect".equals(Objectives.currentId(p))) h.fail("objective=" + Objectives.currentId(p));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void oldSaveMigratesByLevel(GameTestHelper h) {
        ServerPlayer p = player(h, 35, "village", StoryData.MODE_STORY);
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(3).withFlag("ch1_done").withFlag("ch2_done"));
        StoryFlow.restore(p);
        QuestData q = DialogueService.quest(p);
        if (q.raw() != 4) h.fail("chapter=" + q.raw());
        if (Objectives.currentId(p) == null) h.fail("khong co objective sau migrate");
        h.succeed();
    }

    /** Di bo moi buoc: dat buoc -> restore -> co muc tieu dung id + vi tri dich voi cac buoc can dich; storyTier cho moi cap duoc vao. */
    @GameTest(template = "empty")
    public static void everyStepSetsObjectiveWithTarget(GameTestHelper h) {
        for (String origin : new String[]{"city", "sect", "village"}) {
            ServerPlayer p = player(h, 1, origin, StoryData.MODE_STORY);
            for (com.hukhong.story.flow.FlowSteps.Step s : com.hukhong.story.flow.FlowSteps.ALL) {
                if (!s.group().equals("all") && !s.group().equals(com.hukhong.story.flow.FlowSteps.groupOf(origin))) continue;
                DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(Math.max(1, s.chapter())).withFlag(StoryFlow.F_V2).withStep(s.id()));
                StoryFlow.restore(p);
                if (!s.id().equals(Objectives.currentId(p))) h.fail(origin + " " + s.id() + " objective=" + Objectives.currentId(p));
            }
        }
        for (int lv = 1; lv <= 100; lv++) {
            var t = StoryFlow.storyTier(lv);
            if (lv <= 100 && !t.allows(lv) && lv <= 100 && t != com.hukhong.bicanh.BiCanhTier.BACH) h.fail("tier khong cho level " + lv);
        }
        if (!StoryFlow.storyTier(28).allows(28)) h.fail("level 28 phai vao duoc Bi Canh truyen");
        h.succeed();
    }
}
