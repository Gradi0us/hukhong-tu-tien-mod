package com.hukhong.resource;

import com.hukhong.HuKhong;
import com.hukhong.world.season.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** F6: tai sinh linh thao sau thu hoach, rang buoc mua, loc ung vien theo biome + mua. */
@GameTestHolder(HuKhong.MODID)
@PrefixGameTestTemplate(false)
public final class ResourceGameTests {
    private ResourceGameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static HerbBlock setup(GameTestHelper h, String herb, int age, boolean cultivated) {
        h.setBlock(AT.below(), Blocks.DIRT);
        HerbBlock b = ResourceRegistry.HERB_BLOCKS.get(herb).get();
        h.setBlock(AT, b.defaultBlockState().setValue(HerbBlock.AGE, age).setValue(HerbBlock.CULTIVATED, cultivated));
        return b;
    }

    private static Season inSeason(HerbDef d) {
        for (Season s : Season.values()) if ((d.seasonMask() & s.mask()) != 0) return s;
        throw new IllegalStateException();
    }

    private static Season offSeason(HerbDef d) {
        for (Season s : Season.values()) if ((d.seasonMask() & s.mask()) == 0) return s;
        return null;
    }

    @GameTest(template = "empty")
    public static void herbHarvestAndRegrowth(GameTestHelper h) {
        HerbDef d = HerbDefs.get("huyet_tham");
        if (d == null) { h.fail("hukhong_herbs/huyet_tham.json must be loaded"); return; }
        HerbBlock b = setup(h, "huyet_tham", 2, false);
        var lv = h.getLevel();
        BlockPos abs = h.absolutePos(AT);
        if (!b.harvest(lv, abs, lv.getBlockState(abs))) { h.fail("mature herb must be harvestable"); return; }
        BlockState after = lv.getBlockState(abs);
        if (after.getValue(HerbBlock.AGE) != 0 || !after.getValue(HerbBlock.CULTIVATED)) { h.fail("harvest must reset to age 0 and mark cultivated"); return; }
        if (lv.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(2)).isEmpty()) { h.fail("harvest must drop the herb item"); return; }
        if (b.harvest(lv, abs, after)) { h.fail("a sprout must not be harvestable"); return; }
        RandomSource r = RandomSource.create(7);
        for (int i = 0; i < 400 && lv.getBlockState(abs).getValue(HerbBlock.AGE) < 2; i++)
            b.tickWith(lv.getBlockState(abs), lv, abs, r, inSeason(d), false);
        if (lv.getBlockState(abs).getValue(HerbBlock.AGE) != 2) { h.fail("herb must regrow to age 2 in its bloom season"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void herbSeasonGating(GameTestHelper h) {
        HerbDef d = HerbDefs.get("bang_lien");
        if (d == null) { h.fail("hukhong_herbs/bang_lien.json must be loaded"); return; }
        Season off = offSeason(d);
        if (off == null) { h.fail("bang_lien must be seasonal"); return; }
        HerbBlock b = setup(h, "bang_lien", 1, true);
        var lv = h.getLevel();
        BlockPos abs = h.absolutePos(AT);
        RandomSource r = RandomSource.create(11);
        for (int i = 0; i < 300; i++) {
            BlockState s = lv.getBlockState(abs);
            b.tickWith(s, lv, abs, r, off, false);
            if (lv.getBlockState(abs).getValue(HerbBlock.AGE) > 1) { h.fail("herb must never grow outside its season"); return; }
        }
        if (lv.getBlockState(abs).getValue(HerbBlock.AGE) != 0) { h.fail("out-of-season herb must wilt to age 0"); return; }
        // Linh Khi Trieu bo qua rang buoc mua
        for (int i = 0; i < 300 && lv.getBlockState(abs).getValue(HerbBlock.AGE) < 2; i++)
            b.tickWith(lv.getBlockState(abs), lv, abs, r, off, true);
        if (lv.getBlockState(abs).getValue(HerbBlock.AGE) != 2) { h.fail("Linh Khi Trieu must let herbs grow out of season"); return; }
        // cay hoang tuoi 0 ngoai mua tan; cay da cham soc thi khong
        setup(h, "bang_lien", 0, false);
        boolean gone = false;
        for (int i = 0; i < 400 && !gone; i++) {
            b.tickWith(lv.getBlockState(abs), lv, abs, r, off, false);
            gone = !(lv.getBlockState(abs).getBlock() instanceof HerbBlock);
        }
        if (!gone) { h.fail("wild sprout out of season must wither away"); return; }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void herbSpawnCandidatesFollowBiomeAndSeason(GameTestHelper h) {
        HerbDef d = HerbDefs.get("linh_thao");
        if (d == null) { h.fail("hukhong_herbs/linh_thao.json must be loaded"); return; }
        var forest = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST);
        boolean in = HerbSpawner.candidates(forest, inSeason(d), false).stream().anyMatch(x -> x.id().equals("linh_thao"));
        if (!in) { h.fail("linh_thao must be a candidate in a forest during its season"); return; }
        Season off = offSeason(d);
        if (off != null && HerbSpawner.candidates(forest, off, false).stream().anyMatch(x -> x.id().equals("linh_thao"))) {
            h.fail("linh_thao must not be a candidate out of season"); return;
        }
        var desert = h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT);
        if (HerbSpawner.candidates(desert, inSeason(d), false).stream().anyMatch(x -> x.id().equals("linh_thao"))) {
            h.fail("linh_thao must not grow in desert"); return;
        }
        h.succeed();
    }
}
