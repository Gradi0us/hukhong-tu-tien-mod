package com.hukhong.city;

import com.hukhong.auction.AuctionData;
import com.hukhong.city.ambient.AmbientNpc;
import com.hukhong.city.ambient.AmbientRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import static com.hukhong.structures.ProcKit.set;

/**
 * Dau Gia Hanh: dinh dau gia 7x7 nho trong Khu Cho (goc u=9, v=9 - o trong cua luoi sap thuong nhan), dung theo toa do cuc bo
 * cua {@link MarketDistrict}. Co Dau Gia Su (nhan Dau Gia Hanh + Dau Gia Hoi) va Thay Boi (bao lich). Goi tu {@link MarketDistrict#ensure}.
 */
public final class MarketAuctionHall {
    private MarketAuctionHall() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Tam dinh o toa do cuc bo (u, v) cua Khu Cho; Thay Boi dung lech ve mot goc. */
    public static final int U = 9, V = 9;

    private static int wx(BlockPos c, Direction g, int u, int v) { return c.getX() + g.getStepX() * v + g.getClockWise().getStepX() * u; }
    private static int wz(BlockPos c, Direction g, int u, int v) { return c.getZ() + g.getStepZ() * v + g.getClockWise().getStepZ() * u; }

    private static BlockPos at(BlockPos c, Direction g, int u, int v) { return new BlockPos(wx(c, g, u, v), c.getY(), wz(c, g, u, v)); }

    /** Vi tri Dau Gia Su (y = chan) cho khu cho tam c cua thanh d. */
    public static BlockPos auctioneerPos(ServerLevel l, CityDef d, BlockPos c) {
        return at(c, MarketDistrict.gateDir(l, c, d), U, V).above();
    }

    /** Bao dam dinh + 2 NPC. Idempotent; chi chay khi chunk da nap. */
    public static void ensure(ServerLevel ow, CityDef d, BlockPos c) {
        BlockPos pos = auctioneerPos(ow, d, c);
        if (!ow.hasChunkAt(pos)) return;
        AuctionData data = AuctionData.get(ow);
        if (!data.halls.containsKey(d.id())) {
            Direction g = MarketDistrict.gateDir(ow, c, d);
            for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2)
                if (!ow.hasChunkAt(at(c, g, U + 3 * sx, V + 3 * sz))) return; // dinh nam tren chunk chua nap: thu lai lan sau
            build(ow, d, c);
            data.halls.put(d.id(), pos);
            data.setDirty();
            LOG.info("Auction hall of {} built at {}", d.id(), pos);
        }
        AABB box = new AABB(pos).inflate(6, 4, 6);
        boolean hasA = false, hasS = false;
        for (AmbientNpc n : ow.getEntitiesOfClass(AmbientNpc.class, box)) {
            if (n.role() == AmbientNpc.AUCTIONEER) hasA = true;
            if (n.role() == AmbientNpc.SEER) hasS = true;
        }
        if (!hasA) spawn(ow, pos, AmbientNpc.AUCTIONEER, 5);
        if (!hasS) spawn(ow, at(c, MarketDistrict.gateDir(ow, c, d), U - 2, V + 2).above(), AmbientNpc.SEER, 3);
    }

    private static void spawn(ServerLevel l, BlockPos p, int role, int skin) {
        AmbientNpc n = AmbientRegistry.AMBIENT.get().create(l);
        if (n == null) return;
        n.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, l.random.nextFloat() * 360f, 0f);
        n.setup(role, skin, p, 6);
        n.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        l.addFreshEntity(n);
    }

    private static void build(ServerLevel l, CityDef d, BlockPos c) {
        Direction g = MarketDistrict.gateDir(l, c, d);
        int y0 = c.getY();
        for (int u = U - 3; u <= U + 3; u++) for (int v = V - 3; v <= V + 3; v++) {
            int x = wx(c, g, u, v), z = wz(c, g, u, v);
            boolean edge = Math.abs(u - U) == 3 || Math.abs(v - V) == 3;
            set(l, x, y0, z, edge ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE);
            for (int h = 1; h <= 6; h++) set(l, x, y0 + h, z, Blocks.AIR);
            set(l, x, y0 + 5, z, Blocks.CRIMSON_PLANKS);
            if (edge) set(l, x, y0 + 6, z, slab(Blocks.CRIMSON_SLAB));
        }
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            int x = wx(c, g, U + 3 * sx, V + 3 * sz), z = wz(c, g, U + 3 * sx, V + 3 * sz);
            for (int h = 1; h <= 4; h++) set(l, x, y0 + h, z, Blocks.STRIPPED_CRIMSON_STEM);
            set(l, wx(c, g, U + 2 * sx, V + 2 * sz), y0 + 4, wz(c, g, U + 2 * sx, V + 2 * sz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        // ban dau gia (lectern) truoc mat Dau Gia Su, thang ve phia tam khu cho
        set(l, wx(c, g, U, V - 2), y0 + 1, wz(c, g, U, V - 2), Blocks.LECTERN);
        set(l, wx(c, g, U, V), y0, wz(c, g, U, V), Blocks.GOLD_BLOCK);
        int rot = (g.get2DDataValue() + 2) % 4 * 4; // bien nhin ra phia tam khu cho (nguoc voi huong cong)
        CityBuilder.sign(l, wx(c, g, U - 2, V - 3), y0 + 1, wz(c, g, U - 2, V - 3), rot, "hukhong.auction.sign");
    }

    private static net.minecraft.world.level.block.state.BlockState slab(Block b) { return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM); }
}
