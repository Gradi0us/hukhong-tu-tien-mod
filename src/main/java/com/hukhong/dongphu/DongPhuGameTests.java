package com.hukhong.dongphu;

import com.hukhong.HuKhong;
import com.hukhong.currency.SpiritStoneToll;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Động Phủ: lập trận, nâng tầng trừ Linh Thạch, hệ số Tụ Linh trong/ngoài vùng, từ chối trận bàn thứ hai. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class DongPhuGameTests {
    private DongPhuGameTests() {}

    private static ServerPlayer fake(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    /** Đặt lõi tại vị trí tương đối rồi lập trận (đúng thứ tự Block.setPlacedBy). */
    private static BlockPos core(GameTestHelper h, ServerPlayer p, BlockPos rel) {
        BlockPos abs = h.absolutePos(rel);
        h.getLevel().setBlock(abs, DongPhuRegistry.TRAN_BAN.get().defaultBlockState(), 3);
        DongPhuService.claim(p, h.getLevel(), abs);
        return abs;
    }

    @GameTest(template = "empty")
    public static void dongPhuPlaceAndClaim(GameTestHelper h) {
        ServerPlayer p = fake(h, "hk-dp1");
        BlockPos abs = core(h, p, new BlockPos(2, 2, 2));
        DongPhuData.Claim c = DongPhuData.get(h.getLevel().getServer()).of(p.getUUID());
        if (c == null) { h.fail("claim not stored"); return; }
        if (c.tier() != 1) { h.fail("new core must be tier 1, got " + c.tier()); return; }
        if (!c.pos().equals(abs)) { h.fail("claim pos mismatch " + c.pos() + " vs " + abs); return; }
        if (DongPhuService.radius(1) != 16 || DongPhuService.radius(5) != 32) { h.fail("radius table wrong"); return; }
        if (!h.getLevel().getBlockState(abs).is(DongPhuRegistry.TRAN_BAN.get())) { h.fail("core block missing"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dongPhuUpgradeConsumesStones(GameTestHelper h) {
        ServerPlayer p = fake(h, "hk-dp2");
        BlockPos abs = core(h, p, new BlockPos(2, 2, 2));
        p.getInventory().add(new ItemStack(SpiritStoneToll.stone(), 60));
        if (DongPhuService.upgrade(p, h.getLevel(), abs) != null) { h.fail("tier 1->2 must succeed with 60 stones"); return; }
        if (DongPhuData.get(h.getLevel().getServer()).of(p.getUUID()).tier() != 2) { h.fail("tier must become 2"); return; }
        if (SpiritStoneToll.count(p) != 10) { h.fail("50 stones must be consumed, left " + SpiritStoneToll.count(p)); return; }
        if (DongPhuService.upgrade(p, h.getLevel(), abs) == null) { h.fail("2->3 must be refused with only 10 stones"); return; }
        if (DongPhuData.get(h.getLevel().getServer()).of(p.getUUID()).tier() != 2) { h.fail("refused upgrade must not change tier"); return; }
        if (SpiritStoneToll.count(p) != 10) { h.fail("refused upgrade must not consume stones"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dongPhuBonusInsideOutside(GameTestHelper h) {
        ServerPlayer p = fake(h, "hk-dp3");
        BlockPos abs = core(h, p, new BlockPos(2, 2, 2));
        p.setPos(abs.getX() + 3.5, abs.getY(), abs.getZ() + 0.5);
        double inside = DongPhuService.bonus(p);
        if (!(inside > 1.0)) { h.fail("owner inside radius must get bonus > 1, got " + inside); return; }
        p.setPos(abs.getX() + 100.5, abs.getY(), abs.getZ() + 0.5);
        if (DongPhuService.bonus(p) != 1.0) { h.fail("outside radius must be exactly 1.0"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dongPhuSecondCoreRejected(GameTestHelper h) {
        ServerPlayer p = fake(h, "hk-dp4");
        core(h, p, new BlockPos(2, 2, 2));
        if (DongPhuService.validatePlace(p) == null) { h.fail("second core must be rejected while first stands"); return; }
        h.succeed();
    }
}
