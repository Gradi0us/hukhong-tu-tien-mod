package com.hukhong.resource;

import com.hukhong.HuKhong;
import com.hukhong.world.season.Season;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Linh thao moc tu nhien quanh nguoi choi theo biome + mua (ngoai ra worldgen datapack rai san luc tao chunk).
 * Moi 10 giay, moi nguoi choi o Overworld thu mot lan o khoang cach 20-64 block.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class HerbSpawner {
    private HerbSpawner() {}

    public static final int PERIOD = 200;

    /** Biome khop mot muc cua danh sach ("#tag" hoac id). */
    public static boolean matches(Holder<Biome> biome, List<String> entries) {
        for (String s : entries) {
            if (s.startsWith("#")) {
                if (biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(s.substring(1))))) return true;
            } else if (biome.is(ResourceLocation.parse(s))) return true;
        }
        return false;
    }

    /** Cac linh thao co the moc o biome nay vao mua nay (hoac Linh Khi Trieu). */
    public static List<HerbDef> candidates(Holder<Biome> biome, Season season, boolean trieu) {
        List<HerbDef> out = new ArrayList<>();
        for (HerbDef d : HerbDefs.all())
            if (HerbMath.blooms(d.seasonMask(), season.ordinal(), trieu) && matches(biome, d.biomes())) out.add(d);
        return out;
    }

    /** Thu moc mot linh thao tai cot (x,z). true neu da dat. */
    public static boolean trySpawnAt(ServerLevel lv, int x, int z, RandomSource r) {
        BlockPos col = new BlockPos(x, 0, z);
        if (!lv.hasChunkAt(col)) return false;
        BlockPos pos = lv.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
        Holder<Biome> biome = lv.getBiome(pos);
        List<HerbDef> c = candidates(biome, Season.current(lv), HerbBlock.trieuAt(lv, pos));
        if (c.isEmpty()) return false;
        HerbDef d = c.get(r.nextInt(c.size()));
        if (r.nextInt(d.rarity()) != 0) return false;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3)))
            if (lv.getBlockState(q).getBlock() instanceof HerbBlock) return false;
        var blk = ResourceRegistry.HERB_BLOCKS.get(d.id());
        if (blk == null) return false;
        BlockState st = blk.get().defaultBlockState().setValue(HerbBlock.AGE, 1 + r.nextInt(2));
        if (!lv.isEmptyBlock(pos) || !st.canSurvive(lv, pos)) return false;
        lv.setBlock(pos, st, 2);
        return true;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        var ow = e.getServer().overworld();
        if (ow == null || ow.getGameTime() % PERIOD != 0) return;
        for (ServerPlayer p : ow.players()) {
            if (p.level().dimension() != Level.OVERWORLD || p.isSpectator()) continue;
            RandomSource r = ow.getRandom();
            if (r.nextInt(3) != 0) continue;
            int dx = (20 + r.nextInt(44)) * (r.nextBoolean() ? 1 : -1), dz = (20 + r.nextInt(44)) * (r.nextBoolean() ? 1 : -1);
            trySpawnAt(ow, p.getBlockX() + dx, p.getBlockZ() + dz, r);
        }
    }
}
