package com.hukhong.pvp;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CitySavedData;
import com.hukhong.currency.Wallet;
import com.hukhong.events.CityRetake;
import com.hukhong.events.CityStates;
import com.hukhong.events.InvasionTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Dot 5: escrow/chi tra/dot, thua hiep o 1 mau khong chet, phan bo chon thanh tap kich, RUIN -> tai chiem. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class PvpGameTests {
    private PvpGameTests() {}

    private static ServerPlayer fake(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), name));
    }

    @GameTest(template = "empty")
    public static void pvpEscrowPayoutBurn(GameTestHelper h) {
        ServerPlayer a = fake(h, "hk-pvp-a"), b = fake(h, "hk-pvp-b");
        Wallet.set(a, 1000);
        Wallet.set(b, 1000);
        long stake = 300;
        if (!Wallet.pay(a, stake) || !Wallet.pay(b, stake)) { h.fail("both stakes must be escrowed"); return; }
        if (Wallet.count(a) != 700 || Wallet.count(b) != 700) { h.fail("escrow must leave 700 in each wallet"); return; }
        long prize = PvpMath.duelPrize(stake), burn = PvpMath.burn(PvpMath.duelTotal(stake));
        if (prize != 570 || burn != 30) { h.fail("prize/burn math: " + prize + "/" + burn); return; }
        // chi tra qua duong ben vung (nguoi choi khong online trong danh sach -> ghi no -> tra khi dang nhap)
        PvpData pd = PvpData.get(h.getLevel().getServer().overworld());
        PvpData.credit(h.getLevel().getServer(), a.getUUID(), prize);
        if (Wallet.count(a) != 700) { h.fail("offline winner must be owed, not paid yet"); return; }
        long paid = pd.settleOwed(a);
        if (paid != prize || Wallet.count(a) != 1270) { h.fail("owed payout must land in wallet, got " + Wallet.count(a)); return; }
        long total = Wallet.count(a) + Wallet.count(b);
        if (total != 2000 - burn) { h.fail("money out must equal money in minus 5% burn, got " + total); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pvpRoundLostAtOneHpWithoutDeath(GameTestHelper h) {
        ServerLevel ow = h.getLevel();
        Zombie a = EntityType.ZOMBIE.create(ow), b = EntityType.ZOMBIE.create(ow);
        if (a == null || b == null) { h.fail("zombies"); return; }
        BlockPos at = h.absolutePos(new BlockPos(0, 2, 0));
        a.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        b.moveTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5);
        ow.addFreshEntity(a);
        ow.addFreshEntity(b);
        try {
            DuelManager.startForTest(ow, at, a, b, 0, 0, false, w -> { });
            DuelManager.setFightForTest();
            a.hurt(ow.damageSources().mobAttack(b), 1000f); // don chi mang
            if (!a.isAlive()) { h.fail("victim must not die in a duel"); return; }
            if (Math.abs(a.getHealth() - 1f) > 0.01f) { h.fail("victim must be left at 1 HP, got " + a.getHealth()); return; }
            if (!Integer.valueOf(1).equals(DuelManager.pendingLoserForTest())) { h.fail("round loss for side A must be pending"); return; }
            a.heal(10f);
            if (a.getHealth() > 1.01f) { h.fail("healing must be disabled in the match"); return; }
            DuelManager.processPendingForTest();
            int[] w = DuelManager.winsForTest();
            if (w[0] != 0 || w[1] != 1) { h.fail("B must win round 1, wins=" + w[0] + ":" + w[1]); return; }
            // ngoai tran dau khong duoc danh: nguoi thu ba bi chan
            Zombie c = EntityType.ZOMBIE.create(ow);
            if (c != null) {
                c.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5);
                ow.addFreshEntity(c);
                float hp = b.getHealth();
                b.hurt(ow.damageSources().mobAttack(c), 5f);
                c.discard();
                if (b.getHealth() < hp) { h.fail("outsiders must not hurt a duelist"); return; }
            }
            h.succeed();
        } finally {
            DuelManager.clearForTest();
            a.discard();
            b.discard();
        }
    }

    @GameTest(template = "empty")
    public static void invasionTargetDistribution(GameTestHelper h) {
        ServerLevel ow = h.getLevel();
        int n = 4000, chapterHits = 0;
        List<Integer> unlocked = List.of(0, 2, 4, 6);
        for (int i = 0; i < n; i++) {
            int c = InvasionTargets.pick(ow.random, 4, unlocked, x -> true);
            if (c == 4) chapterHits++;
            if (!unlocked.contains(c)) { h.fail("picked a city the players never unlocked: " + c); return; }
        }
        double share = chapterHits / (double) n; // ky vong 0.70 (60% chuong + 40% * 1/4)
        if (share < 0.62 || share > 0.78) { h.fail("chapter share out of range: " + share); return; }
        int pick = InvasionTargets.choose(ow);
        if (pick < -1 || pick >= CityDef.ALL.size()) { h.fail("choose() returned " + pick); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ruinRetakeRestoresCity(GameTestHelper h) {
        ServerLevel ow = h.getLevel().getServer().overworld();
        CityDef c5 = CityDef.ALL.get(5);
        CityStates.setRuin(ow, c5, true);
        if (!CityStates.isRuin(ow, c5)) { h.fail("city must be RUIN after falling"); return; }
        if (!CityStates.retakable(ow, c5, 0)) { h.fail("a fallen city must be retakable"); return; }
        CityRetake.markRetaken(ow, c5);
        if (CityStates.isRuin(ow, c5)) { h.fail("retake must restore NORMAL"); return; }
        // Hoang Do mac dinh RUIN, chi tai chiem duoc o chuong >= 10
        CityDef c9 = CityDef.ALL.get(9);
        if (CityStates.retakable(ow, c9, 9) || !CityStates.retakable(ow, c9, 10)) { h.fail("Thien Khai retake gating by chapter 10"); return; }
        // thanh goc: dung CitySavedData va tang bo dem tap kich de buoc truyen thoa
        CitySavedData cd = CitySavedData.get(ow);
        CitySavedData.State prev = cd.state();
        int won = cd.invasionsWon();
        CityStates.setRuin(ow, CityDef.ALL.get(0), true);
        if (cd.state() != CitySavedData.State.RUIN) { h.fail("city 0 RUIN must map to CitySavedData"); return; }
        CityRetake.markRetaken(ow, CityDef.ALL.get(0));
        boolean ok = cd.state() == CitySavedData.State.NORMAL && cd.invasionsWon() == won + 1;
        cd.setState(prev);
        if (!ok) { h.fail("retaking city 0 must set NORMAL and bump invasionsWon"); return; }
        h.succeed();
    }
}
