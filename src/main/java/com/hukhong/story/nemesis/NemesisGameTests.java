package com.hukhong.story.nemesis;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.flow.FlowSteps;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.objective.Objectives;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Phản Thần Dạ Khuyết (tuyến nemesis): thứ tự bước và đường hoàn thành trận đánh qua rút lui. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class NemesisGameTests {
    private NemesisGameTests() {}

    static ServerPlayer player(GameTestHelper h, int level) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "nm-" + level));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 2000, 2000, Set.of(), Map.of(), false).withCharacter(Set.of(), Map.of(), "village", null));
        p.setData(StoryRegistry.STORY.get(), StoryData.DEFAULT.withMode(StoryData.MODE_STORY));
        Objectives.clear(p);
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        return p;
    }

    @GameTest(template = "empty")
    public static void nemesisStepsSitInFlowOrder(GameTestHelper h) {
        List<String> ids = FlowSteps.ALL.stream().map(FlowSteps.Step::id).toList();
        // {truoc, nemesis, sau} - moi lan xuat hien nam dung giua hai buoc can ke.
        String[][] spots = {
                {"c1_talk", "c1_nemesis", "c1_journey"},
                {"c4_docs", "c4_nemesis", "c4_hua_co"},
                {"c6_lair", "c6_nemesis", "c6_modao"},
                {"c9_chg3", "c9_nemesis", "c9_modao"}};
        for (String[] t : spots) {
            int a = ids.indexOf(t[0]), b = ids.indexOf(t[1]), c = ids.indexOf(t[2]);
            if (a < 0 || b != a + 1 || c != b + 1) { h.fail("thu tu buoc sai quanh " + t[1] + ": " + a + "," + b + "," + c); return; }
            FlowSteps.Step s = FlowSteps.get(t[1]);
            if (s == null || s.kind() != FlowSteps.Kind.NEMESIS) { h.fail("buoc " + t[1] + " khong phai NEMESIS"); return; }
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1800)
    public static void fightRetreatCompletesStep(GameTestHelper h) {
        ServerPlayer p = player(h, 40);
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(6).withFlag(StoryFlow.F_V2).withStep("c6_nemesis"));
        NemesisService.resume(p, FlowSteps.get("c6_nemesis"));
        // sau mở đầu (80 tick) trận phải đang diễn ra; hạ Ảnh xuống dưới 35% -> rút lui.
        h.runAfterDelay(NemesisService.INTRO_TICKS + 20, () -> {
            if (!NemesisService.fighting(p)) { h.fail("tran chua mo sau phan mo dau"); return; }
            LivingEntity a = NemesisService.avatar(p);
            if (a == null) { h.fail("khong co Anh Dạ Khuyết trong tran"); return; }
            a.setHealth(a.getMaxHealth() * 0.2f);
        });
        // rút lui 60 tick: sau đó bước phải được đánh dấu và luồng sang bước kế tiếp.
        h.runAfterDelay(NemesisService.INTRO_TICKS + 20 + NemesisService.RETREAT_TICKS + 30, () -> {
            QuestData q = DialogueService.quest(p);
            if (!q.has("d:c6_nemesis")) { h.fail("chua danh dau d:c6_nemesis sau khi rut lui"); return; }
            if (NemesisService.avatar(p) != null) { h.fail("Anh van ton tai sau khi rut lui"); return; }
            if (NemesisService.fighting(p)) { h.fail("tran van dang chay sau khi rut lui"); return; }
            h.succeed();
        });
    }
}
