package com.hukhong.guild;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.currency.Wallet;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.ModEntities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Hiep Hoi Mao Hiem Gia: nhan nhiem vu diet quai -> diet du -> nop -> nhan diem + Tu Tinh. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class GuildGameTests {
    private GuildGameTests() {}

    @GameTest(template = "empty")
    public static void killQuestPaysPointsAndCoins(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-guild"));
        Mob mob = h.spawn(ModEntities.HU_THI.get(), 1, 2, 1);
        int lv = ((HuMob) mob).huLevel();
        p.setData(ModAttachments.CULTIVATION, CultivationData.DEFAULT.withLevel(Math.max(1, lv)));
        GuildData d = GuildService.refresh(p);
        GuildMath.Quest q = GuildService.quests(d, p.getUUID()).get(0);
        if (q.kind() != GuildMath.K_KILL) { h.fail("slot 0 must be the kill quest, got kind " + q.kind()); return; }
        if (lv < q.lvLo() || lv > q.lvHi()) { h.fail("mob level " + lv + " outside band " + q.lvLo() + ".." + q.lvHi()); return; }
        if (!GuildService.handleButton(p, 0)) { h.fail("accept failed"); return; }
        if (GuildService.handleButton(p, 0)) { h.fail("turn-in must be refused before the kills are done"); return; }
        for (int i = 0; i < q.amount(); i++) GuildService.onKill(p, mob);
        if (GuildService.get(p).progOf(0) != q.amount()) { h.fail("progress " + GuildService.get(p).progOf(0) + " != " + q.amount()); return; }
        long before = Wallet.count(p);
        if (!GuildService.handleButton(p, 0)) { h.fail("turn-in failed"); return; }
        GuildData after = GuildService.get(p);
        long gained = Wallet.count(p) - before;
        long want = GuildMath.scale(q.ha(), false);
        if (gained != want) { h.fail("coins gained " + gained + " != " + want); return; }
        if (after.points() != q.points()) { h.fail("points " + after.points() + " != " + q.points()); return; }
        if (after.progOf(0) != GuildData.DONE) { h.fail("quest not marked done"); return; }
        h.succeed();
    }
}
