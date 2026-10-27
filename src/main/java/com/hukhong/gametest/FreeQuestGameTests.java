package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryRegistry;
import com.hukhong.story.freequest.FreeQuest;
import com.hukhong.story.objective.Objectives;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Quest Đột Phá (chế độ Tự do): người chơi cấp 9 bị khóa -> nhận mục tiêu freequest. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class FreeQuestGameTests {
    private FreeQuestGameTests() {}

    @GameTest(template = "empty")
    public static void freeModeLevel9GetsObjective(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-freequest"));
        p.setData(StoryRegistry.STORY.get(), new StoryData(StoryData.MODE_FREE, List.of(), 0));
        p.setData(ModAttachments.CULTIVATION, CultivationData.DEFAULT.withLevel(9));
        FreeQuest.tick(p);
        String id = Objectives.currentId(p);
        if (id == null || !id.startsWith(FreeQuest.OBJ_PREFIX)) { h.fail("expected freequest objective, got " + id); return; }
        if (FreeQuest.data(p).active() != 9) { h.fail("quest not active"); return; }
        h.succeed();
    }
}
