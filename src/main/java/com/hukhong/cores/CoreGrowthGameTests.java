package com.hukhong.cores;

import com.hukhong.HuKhong;
import com.hukhong.bicanh.BiCanhVariety;
import com.hukhong.core.Aptitude;
import com.hukhong.core.CultivationData;
import com.hukhong.core.ModAttachments;
import com.hukhong.core.Path;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 2026-11-06: Lõi Uẩn Dưỡng + cộng hưởng, Phá Chấp, đan đột phá 69/79/89, đa dạng Bí Cảnh. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGrowthGameTests {
    private CoreGrowthGameTests() {}

    @GameTest(template = "empty")
    public static void coreGrowthScales(GameTestHelper h) {
        if (CoreService.growth(10, 1) != 1.0) { h.fail("loi moi chon phai x1"); return; }
        if (Math.abs(CoreService.growth(35, 1) - 2.0) > 1e-9) { h.fail("25 cap sau phai x2"); return; }
        if (CoreService.growth(100, 1) != CoreService.GROWTH_CAP) { h.fail("tran x3"); return; }
        if (CoreService.setMult(2) != 1.0 || CoreService.setMult(4) != 2.0 || CoreService.setMult(6) != 3.5) { h.fail("cong huong sai"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakthroughPillsExist(GameTestHelper h) {
        for (int lv : new int[]{69, 79, 89})
            if (com.hukhong.story.freequest.FreeQuest.pillFor(lv) == null) { h.fail("khong co dan cho cap " + lv); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void biCanhRoomTypesValid(GameTestHelper h) {
        boolean sawOther = false;
        for (long seed = 0; seed < 200; seed++) {
            var rt = BiCanhVariety.roomTypes(seed, 5);
            if (rt[1] != BiCanhVariety.RoomType.SONG) { h.fail("tang 1 phai la song thuong"); return; }
            if (rt[5] == BiCanhVariety.RoomType.LINH_TUYEN) { h.fail("linh tuyen o tang cuoi"); return; }
            int springs = 0;
            for (var t : rt) { if (t == BiCanhVariety.RoomType.LINH_TUYEN) springs++; if (t != BiCanhVariety.RoomType.SONG) sawOther = true; }
            if (springs > 1) { h.fail("qua 1 linh tuyen"); return; }
        }
        if (!sawOther) { h.fail("khong co kieu phong khac"); return; }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void phaChapClearsStuck(GameTestHelper h) {
        ServerPlayer p = FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "hk-phachap"));
        p.setData(ModAttachments.CULTIVATION, new CultivationData(30, 0, 100, 100, Set.of(Path.KIEM), Map.of(Path.KIEM, Aptitude.B), false, "", null, false));
        int m = CoreService.milestoneOf(p.getData(ModAttachments.CULTIVATION));
        p.setData(CoreAttachments.CORE_SLOTS, CoreSlots.EMPTY.withStuck(m));
        PhaChap.begin(p);
        if (!PhaChap.stuck(p)) { h.fail("khong o trang thai mac ket"); return; }
        for (int i = 0; i < PhaChap.need(m); i++) {
            Zombie z = h.spawn(EntityType.ZOMBIE, 1, 2, 1);
            z.die(p.damageSources().playerAttack(p));
        }
        if (PhaChap.stuck(p)) { h.fail("ha du quai van mac ket: " + p.getData(CoreAttachments.PHA_CHAP)); return; }
        h.succeed();
    }
}
