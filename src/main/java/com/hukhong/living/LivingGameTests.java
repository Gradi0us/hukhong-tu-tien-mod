package com.hukhong.living;

import com.hukhong.HuKhong;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.currency.Wallet;
import com.hukhong.story.world.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Random;
import java.util.UUID;

/** Dot 7c: thien cam -> giam gia, sinh va hoan thanh 10 mau nhiem vu ngau nhien. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class LivingGameTests {
    private LivingGameTests() {}

    private static ServerPlayer player(GameTestHelper h, long wallet) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "liv-" + System.nanoTime() % 100000));
        Wallet.set(p, wallet);
        p.getInventory().clearContent();
        return p;
    }

    @GameTest(template = "empty")
    public static void affectionDiscountAndCap(GameTestHelper h) {
        ServerPlayer p = player(h, 0);
        if (AffectionService.adjustPrice(p, 1000) != 1000) h.fail("neutral affection must not change price");
        AffectionService.change(p, AffectionService.factionKey(Faction.THANH), 65);
        if (AffectionService.adjustPrice(p, 1000) != 900) h.fail("affection 65 should give 10% off, got " + AffectionService.adjustPrice(p, 1000));
        AffectionService.change(p, AffectionService.factionKey(Faction.THANH), -120);
        if (AffectionService.faction(p, Faction.THANH) != -55) h.fail("clamped value wrong: " + AffectionService.faction(p, Faction.THANH));
        if (AffectionService.adjustPrice(p, 1000) <= 1000) h.fail("hostile city should add a surcharge");
        AffectionService.change(p, AffectionService.factionKey(Faction.THANH), 500);
        if (AffectionService.faction(p, Faction.THANH) != 100) h.fail("affection must clamp at 100");
        // per-NPC + phe: change() tang ca hai
        UUID npc = UUID.randomUUID();
        AffectionService.change(p, npc, Faction.TONG_MON, 10);
        if (AffectionService.get(p, AffectionService.npcKey(npc)) != 10 || AffectionService.faction(p, Faction.TONG_MON) != 5) h.fail("npc/faction split wrong");
        // gioi han 64 khoa NPC (cat khoa cu nhat)
        UUID first = UUID.randomUUID();
        AffectionService.change(p, AffectionService.npcKey(first), 7);
        for (int i = 0; i < 70; i++) AffectionService.change(p, AffectionService.npcKey(UUID.randomUUID()), 1);
        if (AffectionService.get(p, AffectionService.npcKey(first)) != 0) h.fail("oldest NPC key should have been evicted");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allTenQuestTemplatesGenerateAndComplete(GameTestHelper h) {
        ServerPlayer p = player(h, 0);
        ServerLevel sl = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(2, 2, 2));
        p.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
        Random rnd = new Random(42);
        long oldDay = sl.getDayTime();
        int done = 0;
        try {
            for (Quest.Tmpl t : Quest.Tmpl.values()) {
                Quest q = QuestService.generate(p, rnd, origin, null, false, t);
                if (q.reward <= 0 || q.expire <= sl.getGameTime()) h.fail(t + ": bad reward/expiry");
                if (t != Quest.Tmpl.GATHER && t != Quest.Tmpl.HUNT && t != Quest.Tmpl.GUARD_NIGHT && origin.distSqr(q.target) < 50 * 50) h.fail(t + ": target too close " + q.target);
                if (!QuestService.accept(p, q)) h.fail(t + ": accept refused");
                if (QuestService.describe(q, p).getString().isEmpty()) h.fail(t + ": empty description");
                long w0 = Wallet.count(p);
                switch (t) {
                    case HUNT -> { for (int i = 0; i < q.need; i++) QuestService.onKill(p, h.spawn(EntityType.ZOMBIE, 1, 2, 1)); }
                    case GATHER -> {
                        Item it = BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.item));
                        p.getInventory().add(new ItemStack(it, q.need));
                        if (!QuestService.turnInGather(p, q)) h.fail("GATHER turn-in failed");
                    }
                    case ELITE -> {
                        Mob m = QuestService.spawnBrigand(sl, origin.above(), q, true, 10);
                        QuestService.onKill(p, m);
                    }
                    case STOLEN -> {
                        Mob m = QuestService.spawnBrigand(sl, origin.above(), q, false, 10);
                        QuestService.onKill(p, m);
                        if (q.stage != 1) h.fail("STOLEN: thief kill did not advance");
                        if (!QuestService.turnInStolen(p, q)) h.fail("STOLEN: turn-in failed");
                    }
                    case RUMOR -> { p.setPos(q.target.getX() + 0.5, origin.getY(), q.target.getZ() + 0.5); QuestService.tick(p, q); }
                    case ESCORT, GUIDE -> {
                        p.setPos(q.target.getX() + 0.5, origin.getY(), q.target.getZ() + 0.5);
                        QuestService.tick(p, q);
                        QuestService.tick(p, q);
                    }
                    case DELIVER, FIND_LOST -> {
                        q.target = origin.offset(3, 0, 3);
                        QuestService.tick(p, q); // sinh nguoi nhan / nguoi lac
                        var e = q.entity == null ? null : sl.getEntity(q.entity);
                        if (!(e instanceof AmbientNpc n)) h.fail(t + ": target NPC not spawned");
                        else if (!QuestService.interact(p, n)) h.fail(t + ": interact did not complete");
                    }
                    case GUARD_NIGHT -> {
                        sl.setDayTime(18000);
                        for (int i = 0; i < q.need; i++) QuestService.tick(p, q);
                    }
                }
                p.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                if (QuestService.quests(p).contains(q)) h.fail(t + ": quest not completed");
                if (Wallet.count(p) != w0 + q.reward) h.fail(t + ": reward not paid (" + (Wallet.count(p) - w0) + " vs " + q.reward + ")");
                done++;
            }
        } finally {
            sl.setDayTime(oldDay);
        }
        if (done != 10) h.fail("expected 10 templates, ran " + done);
        h.succeed();
    }
}
