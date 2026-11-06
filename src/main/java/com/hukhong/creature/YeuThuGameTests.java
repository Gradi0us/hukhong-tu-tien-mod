package com.hukhong.creature;

import com.hukhong.HuKhong;
import com.hukhong.world.calendar.ThuTrieuHandler;
import com.hukhong.world.yeulam.CoThuFeature;
import com.hukhong.world.yeulam.DaiSamLam;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Yêu Thú: năm tuổi đổi chỉ số / tên / giữ lại, Yêu Thú Triều dùng Yêu Thú, cây cổ thụ Đại Sâm Lâm dựng được. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class YeuThuGameTests {
    private YeuThuGameTests() {}

    private static LinhQuai beast(GameTestHelper h, CreatureKind k, YeuThuAge a, int x) {
        LinhQuai m = DaiSamLam.spawnBeast(h.getLevel(), k, h.absolutePos(new BlockPos(x, 2, 2)), a, MobSpawnType.COMMAND);
        m.setNoAi(true);
        return m;
    }

    private static double ageMod(LinhQuai m, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> at) {
        var md = m.getAttribute(at).getModifier(LinhQuai.AGE_MOD);
        return md == null ? 0 : md.amount();
    }

    @GameTest(template = "empty")
    public static void yeuThuAgeScalesStats(GameTestHelper h) {
        LinhQuai young = beast(h, CreatureKind.XICH_VAN_HO, YeuThuAge.Y10, 1);
        LinhQuai old = beast(h, CreatureKind.XICH_VAN_HO, YeuThuAge.Y10000, 3);
        if (old.age() != YeuThuAge.Y10000 || young.age() != YeuThuAge.Y10) { h.fail("tuoi sai"); return; }
        if (Math.abs(ageMod(old, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) - (YeuThuAge.Y10000.hpMul - 1)) > 1e-6) { h.fail("he so mau 10000 nam sai"); return; }
        if (Math.abs(ageMod(old, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) - (YeuThuAge.Y10000.dmgMul - 1)) > 1e-6) { h.fail("he so cong sai"); return; }
        if (ageMod(young, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) != 0) { h.fail("10 nam khong duoc tang mau"); return; }
        if (old.getScale() <= young.getScale()) { h.fail("khong to hon"); return; }
        if (old.getMaxHealth() >= 1023 && old.toughness() <= 1.01f) { h.fail("tran mau nhung khong co giam sat thuong"); return; }
        if (!old.isPersistenceRequired()) { h.fail("10000 nam phai giu lai"); return; }
        if (old.getHealth() < old.getMaxHealth() - 0.01) { h.fail("chua hoi day mau"); return; }
        old.setAge(YeuThuAge.Y100); // đổi bậc: ghi đè, không cộng dồn
        if (Math.abs(ageMod(old, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) - (YeuThuAge.Y100.hpMul - 1)) > 1e-6) { h.fail("doi bac bi cong don"); return; }
        if (!old.getDisplayName().getString().contains("100")) { h.fail("ten thieu nam tuoi: " + old.getDisplayName().getString()); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void yeuThuTrieuUsesBeasts(GameTestHelper h) {
        if (ThuTrieuHandler.waveAge(15, 16, 0.5) != YeuThuAge.Y10000) { h.fail("thu linh phai 10000 nam"); return; }
        for (double u = 0; u < 1; u += 0.05)
            if (ThuTrieuHandler.waveAge(0, 16, u).ordinal() > YeuThuAge.Y1000.ordinal()) { h.fail("thu thuong qua manh"); return; }
        for (CreatureKind k : CreatureKind.BEASTS) if (!k.beast() || !k.hostile()) { h.fail(k + " khong phai yeu thu"); return; }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void coThuGrowsGiantTree(GameTestHelper h) {
        BlockPos o = h.absolutePos(new BlockPos(5, 2, 5));
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) h.getLevel().setBlock(o.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        CoThuFeature.grow(h.getLevel(), o, RandomSource.create(7), 1.0);
        int logs = 0, leaves = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -8), o.offset(8, 50, 8))) {
            var s = h.getLevel().getBlockState(p);
            if (s.is(BlockTags.LOGS)) logs++;
            else if (s.is(BlockTags.LEAVES)) leaves++;
        }
        if (logs < 80 || leaves < 150) { h.fail("cay qua nho: go " + logs + " la " + leaves); return; }
        if (!h.getLevel().getBlockState(o.above(25)).is(BlockTags.LOGS)) { h.fail("than khong cao toi 25"); return; }
        h.succeed();
    }
}
