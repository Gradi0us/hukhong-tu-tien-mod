package com.hukhong.gametest;

import com.hukhong.HuKhong;
import com.hukhong.story.objective.ObjectiveAttachments;
import com.hukhong.story.objective.Objectives;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Mục tiêu chính: set -> attachment giữ; sai id không xóa; complete đúng id -> xóa. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ObjectiveGameTests {
    private ObjectiveGameTests() {}

    @GameTest(template = "empty")
    public static void objectiveSetAndComplete(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-objective"));
        Objectives.set(p, "ch1.find_master", Component.literal("Tim su phu"), new BlockPos(10, 64, 20), Level.OVERWORLD);
        if (!"ch1.find_master".equals(Objectives.currentId(p))) { h.fail("objective not stored"); return; }
        var d = p.getData(ObjectiveAttachments.OBJECTIVE.get());
        if (d.target().isEmpty() || d.target().get().getX() != 10 || d.dim().isEmpty()) { h.fail("target/dim not stored"); return; }
        Objectives.complete(p, "other");
        if (Objectives.currentId(p) == null) { h.fail("wrong id must not clear"); return; }
        Objectives.complete(p, "ch1.find_master");
        if (Objectives.currentId(p) != null) { h.fail("complete must clear"); return; }
        Objectives.set(p, "x", Component.literal("x"), null, null);
        Objectives.clear(p);
        if (Objectives.currentId(p) != null) { h.fail("clear failed"); return; }
        h.succeed();
    }
}
