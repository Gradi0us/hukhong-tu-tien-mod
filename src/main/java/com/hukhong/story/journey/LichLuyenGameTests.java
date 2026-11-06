package com.hukhong.story.journey;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.creature.CreatureKind;
import com.hukhong.creature.LinhQuai;
import com.hukhong.creature.YeuThuAge;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.flow.FlowSteps;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.objective.Objectives;
import com.hukhong.world.yeulam.DaiSamLam;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 2026-11-06: Lịch Luyện - nhiệm vụ dẫn truyện giữa các mốc cảnh giới. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class LichLuyenGameTests {
    private LichLuyenGameTests() {}

    static ServerPlayer player(GameTestHelper h, int level) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "lj-" + level));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(level, 0, 2000, 2000, Set.of(), Map.of(), false).withCharacter(Set.of(), Map.of(), "village", null));
        p.setData(StoryRegistry.STORY.get(), StoryData.DEFAULT.withMode(StoryData.MODE_STORY));
        Objectives.clear(p);
        p.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        return p;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void journeyStepAssignsStoryTaskAndAdvances(GameTestHelper h) {
        ServerPlayer p = player(h, 5);
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(1).withFlag(StoryFlow.F_V2).withStep("c1_talk"));
        StoryFlow.complete(p, "c1_talk"); // vào c1_journey
        FlowSteps.Step s = StoryFlow.current(p);
        if (s == null || s.kind() != FlowSteps.Kind.JOURNEY) { h.fail("khong vao buoc Lich Luyen: " + s); return; }
        LichLuyen.Cur c = LichLuyen.current(p);
        if (c == null || c.task() != LichLuyen.Task.SAN_YEU) { h.fail("nhiem vu cot truyen chuong 1 sai: " + c); return; }
        if (!"c1_journey".equals(Objectives.currentId(p))) { h.fail("objective=" + Objectives.currentId(p)); return; }
        var sl = h.getLevel();
        for (int i = 0; i < c.need(); i++) {
            LinhQuai m = DaiSamLam.spawnBeast(sl, CreatureKind.COMMON_BEASTS.get(0), h.absolutePos(new BlockPos(3, 2, 3)), YeuThuAge.Y10, MobSpawnType.COMMAND);
            m.die(sl.damageSources().playerAttack(p));
            m.discard();
        }
        if (DialogueService.quest(p).prog() != 1) { h.fail("chua tinh 1 nhiem vu: prog=" + DialogueService.quest(p).prog()); return; }
        LichLuyen.Cur c2 = LichLuyen.current(p);
        if (c2 == null) { h.fail("khong giao nhiem vu thu 2"); return; }
        LichLuyen.put(p, new LichLuyen.Cur(LichLuyen.Task.TRU_MA, 1, 0, 0, 0));
        var z = EntityType.ZOMBIE.create(sl);
        z.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4, 2, 4)));
        sl.addFreshEntity(z);
        z.die(sl.damageSources().playerAttack(p));
        z.discard();
        FlowSteps.Step after = StoryFlow.current(p);
        if (after == null || after.kind() == FlowSteps.Kind.JOURNEY) { h.fail("sau 2 nhiem vu chua sang c1_grind: " + after); return; }
        if (LichLuyen.current(p) == null) { h.fail("buoc cay cap khong co nhiem vu tuy chon"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void liberatingForestStopsCorruption(GameTestHelper h) {
        var sl = h.getLevel();
        ServerPlayer p = player(h, 30);
        DialogueService.setQuest(p, QuestData.DEFAULT.withChapter(3).withFlag(StoryFlow.F_V2).withStep("c3_grind"));
        DaiSamLam.Region r = null;
        for (int i = -6; i <= 6 && r == null; i++) for (int j = -6; j <= 6 && r == null; j++) {
            DaiSamLam.Region c = DaiSamLam.cell(sl.getSeed(), i, j);
            if (c != null && DaiSamLam.corruptedLive(sl, c)) r = c;
        }
        if (r == null) { h.fail("khong tim thay rung xam thuc"); return; }
        LichLuyen.Cur c = new LichLuyen.Cur(LichLuyen.Task.GIAI_PHONG, 1, 1, r.cx(), r.cz());
        LichLuyen.finish(p, FlowSteps.get("c3_grind"), c);
        if (DaiSamLam.corruptedLive(sl, r)) { h.fail("rung van bi xam thuc sau khi giai phong"); return; }
        if (!DaiSamLam.corrupted(sl.getSeed(), r)) { h.fail("ham goc doi ket qua"); return; }
        h.succeed();
    }
}
