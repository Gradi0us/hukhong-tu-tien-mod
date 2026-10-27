package com.hukhong.travel;

import com.hukhong.HuKhong;
import com.hukhong.story.flow.RequiredNpcs;
import com.hukhong.story.npc.StoryNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** NPC tuyen chinh bi mat -> sinh lai (khong trung); dich chuyen bi tu choi khi dang chien dau. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class TravelGameTests {
    private TravelGameTests() {}

    private static List<StoryNpc> npcs(ServerLevel l, BlockPos c, String id) {
        return l.getEntitiesOfClass(StoryNpc.class, new AABB(c).inflate(64, 40, 64), n -> n.isAlive() && n.npcId().equals(id));
    }

    @GameTest(template = "empty")
    public static void missingStoryNpcRespawnsWithoutDuplicates(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos c = h.absolutePos(new BlockPos(2, 1, 2));
        String id = "mo_dao";
        for (StoryNpc n : npcs(l, c, id)) n.discard();
        if (RequiredNpcs.ensureAt(l, id, c, false) != RequiredNpcs.Result.SPAWNED) { h.fail("missing NPC must be spawned"); return; }
        if (npcs(l, c, id).size() != 1) { h.fail("expected exactly 1 NPC after first ensure, got " + npcs(l, c, id).size() + " all=" + l.getEntitiesOfClass(StoryNpc.class, new AABB(c).inflate(500, 400, 500)).size() + " c=" + c); return; }
        if (RequiredNpcs.ensureAt(l, id, c, false) != RequiredNpcs.Result.PRESENT) { h.fail("present NPC must not respawn"); return; }
        if (npcs(l, c, id).size() != 1) { h.fail("duplicate spawned"); return; }
        npcs(l, c, id).get(0).discard(); // NPC bi mat / chet
        if (RequiredNpcs.ensureAt(l, id, c, false) != RequiredNpcs.Result.SPAWNED || npcs(l, c, id).size() != 1) { h.fail("lost NPC must respawn once"); return; }
        RequiredNpcs.create(l, id, c); // ban sao thua -> bi don
        if (npcs(l, c, id).size() != 2) { h.fail("setup: expected 2 NPCs"); return; }
        RequiredNpcs.ensureAt(l, id, c, false);
        if (npcs(l, c, id).size() != 1) { h.fail("duplicates must be removed, got " + npcs(l, c, id).size()); return; }
        for (StoryNpc n : npcs(l, c, id)) n.discard();
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void travelRejectedInCombat(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-travel"));
        if (TravelRules.inCombat(p)) { h.fail("fresh player must not be in combat"); return; }
        TravelRules.markCombat(p);
        String err = TravelService.start(p, TravelService.OBJ, true);
        if (!"hukhong.travel.err_combat".equals(err)) { h.fail("travel in combat must be rejected with err_combat, got " + err); return; }
        if (TravelService.channeling(p)) { h.fail("no channel may start while in combat"); return; }
        h.succeed();
    }
}
