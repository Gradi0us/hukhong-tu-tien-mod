package com.hukhong.voidblock;

import com.hukhong.HuKhong;
import com.hukhong.bicanh.BiCanhCave;
import com.hukhong.bicanh.BiCanhRegistry;
import com.hukhong.bicanh.BiCanhTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class VoidGameTests {
    @GameTest(template = "empty")
    public static void hangKhongGianXayVaSap(GameTestHelper h) {
        var l = h.getLevel();
        BlockPos o = h.absolutePos(new BlockPos(4, 20, 4));
        for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++)
            for (int y = -12; y <= 0; y++) l.setBlock(o.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
        BlockPos portal = BiCanhCave.plan(l, o.getX(), o.getZ(), o.getY() + 1, Direction.SOUTH);
        if (portal == null) { h.fail("plan null"); return; }
        BiCanhCave.build(l, portal, Direction.NORTH, BiCanhTier.BACH);
        if (!l.getBlockState(portal).is(BiCanhRegistry.PORTAL.get())) { h.fail("khong co mat cong"); return; }
        if (!l.getBlockState(portal.north(2)).isAir()) { h.fail("long hang khong rong"); return; }
        BiCanhCave.collapse(l, portal, Direction.NORTH);
        if (l.getBlockState(portal).is(BiCanhRegistry.PORTAL.get())) { h.fail("cong con sau sap"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void huKhongChuyenDoiHaiChieu(GameTestHelper h) {
        var g = Blocks.GRASS_BLOCK.defaultBlockState();
        var c = VoidCorruptor.corrupt(g);
        if (c == null || !c.is(VoidBlocks.CO.get())) { h.fail("co khong chuyen"); return; }
        var back = VoidCorruptor.revert(c, true);
        if (back == null || !back.is(Blocks.GRASS_BLOCK)) { h.fail("khong hoan nguyen"); return; }
        if (VoidCorruptor.corrupt(Blocks.SAND.defaultBlockState()) != null) { h.fail("cat bi chuyen"); return; }
        h.succeed();
    }
}
