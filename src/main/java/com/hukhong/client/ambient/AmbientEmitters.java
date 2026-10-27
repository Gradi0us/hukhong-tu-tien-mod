package com.hukhong.client.ambient;

import com.hukhong.client.minimap.MinimapState;
import com.hukhong.network.MapMarkersPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Nơi sinh hạt khí quyển theo vùng: Hư Hóa, chiến sự, thành/cảng/tông yên bình, hoang dã ban đêm, Bí Cảnh/Tâm Thức. */
final class AmbientEmitters {
    private AmbientEmitters() {}

    private static final int TAG_FIREFLY = 1;
    private static final Set<Long> LANTERNS = new HashSet<>(), FURNACES = new HashSet<>();

    static void tick(Minecraft mc, ClientLevel lv) {
        RandomSource rnd = lv.random;
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        float c = AmbientClient.corruption(1f), w = AmbientClient.war(1f), p = AmbientClient.peace(1f), d = AmbientClient.dust(1f);
        float night = AmbientClient.night();

        if (c > 0.02f) corruption(lv, rnd, px, py, pz, c);
        if (AmbientClient.overworld() && !AmbientState.hearts().isEmpty()) hearts(lv, rnd, px, py, pz);
        if (w > 0.02f) war(lv, rnd, px, py, pz, w);
        if (d > 0.02f) dust(rnd, px, py, pz, d);

        boolean scan = AmbientClient.overworld() && (night > 0.3f || p > 0.1f);
        if (scan) {
            scanBlocks(lv, px, py, pz);
            lanterns(lv, rnd, p, night);
            if (p > 0.25f) chimneys(lv, rnd, p);
        } else if (!LANTERNS.isEmpty() || !FURNACES.isEmpty()) { LANTERNS.clear(); FURNACES.clear(); }

        if (p > 0.02f) peace(mc, lv, rnd, px, py, pz, p);

        float wild = AmbientClient.overworld() ? (float) Math.pow(night, 1.5) * (1f - AmbientClient.rain()) * (1f - Math.max(Math.max(c, w), 0f)) : 0f;
        if (wild > 0.1f) fireflies(lv, rnd, px, py, pz, wild);
    }

    static int groundY(ClientLevel lv, int x, int z, int fallback) {
        if (!lv.getChunkSource().hasChunk(x >> 4, z >> 4)) return fallback;
        return lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    // ---------------------------------------------------------------- Hư Hóa
    private static void corruption(ClientLevel lv, RandomSource rnd, double px, double py, double pz, float c) {
        for (int n = MoteSystem.roll(rnd, 1.6 * c); n-- > 0; ) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 4 + rnd.nextDouble() * 26;
            float pink = rnd.nextFloat();
            MoteSystem.spawn(new MoteSystem.Mote()
                    .at(px + Math.cos(ang) * rad, py - 3 + rnd.nextDouble() * 13, pz + Math.sin(ang) * rad)
                    .vel((rnd.nextDouble() - .5) * .01, 0.004 + rnd.nextDouble() * .01, (rnd.nextDouble() - .5) * .01)
                    .color(pink < .25f ? 0.92f : 0.52f + rnd.nextFloat() * .2f, 0.22f + rnd.nextFloat() * .1f, pink < .25f ? 0.85f : 1f, 0.5f + rnd.nextFloat() * .35f)
                    .size(0.07f + rnd.nextFloat() * 0.14f).life(140 + rnd.nextInt(120)).drift(0.0024f, 0.97f).flick(0.35f));
        }
        for (int n = MoteSystem.roll(rnd, 2.2 * c); n-- > 0; ) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 28;
            int x = Mth.floor(px + Math.cos(ang) * rad), z = Mth.floor(pz + Math.sin(ang) * rad);
            int gy = groundY(lv, x, z, (int) py);
            if (Math.abs(gy - py) > 14) gy = (int) py;
            MoteSystem.spawn(new MoteSystem.Mote().kind(MoteSystem.ASH).at(x + rnd.nextDouble(), gy + rnd.nextDouble() * 0.6, z + rnd.nextDouble())
                    .vel((rnd.nextDouble() - .5) * .01, 0.016 + rnd.nextDouble() * .02, (rnd.nextDouble() - .5) * .01)
                    .color(0.30f + rnd.nextFloat() * .1f, 0.20f, 0.38f, 0.5f).size(0.05f + rnd.nextFloat() * 0.06f)
                    .life(120 + rnd.nextInt(110)).drift(0.004f, 0.985f).flick(0.1f));
        }
    }

    /** Dòng xoáy hạt quanh Tâm Hư Hóa khi ở gần. */
    private static void hearts(ClientLevel lv, RandomSource rnd, double px, double py, double pz) {
        for (BlockPos h : AmbientState.hearts()) {
            double dx = h.getX() + .5 - px, dz = h.getZ() + .5 - pz;
            if (dx * dx + dz * dz > 80 * 80) continue;
            for (int n = MoteSystem.roll(rnd, 2.4); n-- > 0; ) {
                double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 4;
                double y = h.getY() + rnd.nextDouble() * 3;
                MoteSystem.spawn(new MoteSystem.Mote()
                        .at(h.getX() + .5 + Math.cos(ang) * rad, y, h.getZ() + .5 + Math.sin(ang) * rad)
                        .vel(0, 0.06 + rnd.nextDouble() * .05, 0)
                        .target(h.getX() + .5, y + 6, h.getZ() + .5, 0.004f, 0.012f)
                        .color(0.68f, 0.35f, 1f, 0.8f).size(0.1f + rnd.nextFloat() * .1f).life(70 + rnd.nextInt(40)).drift(0.001f, 0.95f).flick(0.3f));
            }
        }
    }

    // ---------------------------------------------------------------- chiến sự
    private static void war(ClientLevel lv, RandomSource rnd, double px, double py, double pz, float w) {
        for (int n = MoteSystem.roll(rnd, 2.8 * w); n-- > 0; ) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 24;
            MoteSystem.spawn(new MoteSystem.Mote()
                    .at(px + Math.cos(ang) * rad, py - 2 + rnd.nextDouble() * 9, pz + Math.sin(ang) * rad)
                    .vel((rnd.nextDouble() - .3) * .02, 0.025 + rnd.nextDouble() * .035, (rnd.nextDouble() - .5) * .02)
                    .color(1f, 0.42f + rnd.nextFloat() * .25f, 0.12f, 0.85f).size(0.045f + rnd.nextFloat() * .06f)
                    .life(60 + rnd.nextInt(80)).drift(0.006f, 0.98f).flick(0.55f));
        }
        for (int n = MoteSystem.roll(rnd, 1.4 * w); n-- > 0; ) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 26;
            MoteSystem.spawn(new MoteSystem.Mote().kind(MoteSystem.ASH)
                    .at(px + Math.cos(ang) * rad, py + rnd.nextDouble() * 10, pz + Math.sin(ang) * rad)
                    .vel(0.012, 0.004 + rnd.nextDouble() * .01, (rnd.nextDouble() - .5) * .01)
                    .color(0.34f, 0.12f, 0.12f, 0.55f).size(0.05f + rnd.nextFloat() * .06f).life(100 + rnd.nextInt(80)).drift(0.004f, 0.985f));
        }
    }

    // ---------------------------------------------------------------- Bí Cảnh / Tâm Thức
    private static void dust(RandomSource rnd, double px, double py, double pz, float d) {
        float[] pal = AmbientClient.inBiCanh() ? AmbientSky.tierPalette(AmbientState.bicanhTier()) : AmbientSky.tierPalette(0);
        for (int n = MoteSystem.roll(rnd, 1.1 * d); n-- > 0; ) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 2 + rnd.nextDouble() * 14;
            MoteSystem.spawn(new MoteSystem.Mote()
                    .at(px + Math.cos(ang) * rad, py - 4 + rnd.nextDouble() * 10, pz + Math.sin(ang) * rad)
                    .vel((rnd.nextDouble() - .5) * .004, 0.002 + rnd.nextDouble() * .004, (rnd.nextDouble() - .5) * .004)
                    .color(pal[0], pal[1], pal[2], 0.3f + rnd.nextFloat() * .3f).size(0.035f + rnd.nextFloat() * .07f)
                    .life(160 + rnd.nextInt(140)).drift(0.0018f, 0.97f).flick(0.25f));
        }
    }

    // ---------------------------------------------------------------- yên bình
    private static void peace(Minecraft mc, ClientLevel lv, RandomSource rnd, double px, double py, double pz, float p) {
        MapMarkersPayload.Marker z = AmbientClient.zone();
        int zk = AmbientClient.zoneKind();
        float day = AmbientSky.dayFactor(lv, 1f);
        float dens = MoteSystem.density();
        if (z == null || dens <= 0f) return;

        // cánh hoa / lá bay ở tông môn (nhẹ hơn ở thành)
        double leafRate = (zk == AmbientClient.Z_SECT ? 0.2 : zk == AmbientClient.Z_CITY ? 0.04 : 0.0) * p * dens;
        if (rnd.nextDouble() < leafRate) {
            lv.addParticle(ParticleTypes.CHERRY_LEAVES, px + (rnd.nextDouble() - .5) * 30, py + 4 + rnd.nextDouble() * 6, pz + (rnd.nextDouble() - .5) * 30, 0, -0.01, 0);
        }

        // dòng linh khí chảy vào Linh Khí Đài (tâm thành / tông)
        if (zk == AmbientClient.Z_SECT || zk == AmbientClient.Z_CITY) {
            double cx = z.x() + 0.5, cz = z.z() + 0.5;
            if (Math.hypot(cx - px, cz - pz) < 72) {
                int gy = groundY(lv, z.x(), z.z(), (int) py);
                for (int n = MoteSystem.roll(rnd, 1.6 * p); n-- > 0; ) {
                    double ang = rnd.nextDouble() * Math.PI * 2, rad = 11 + rnd.nextDouble() * 6;
                    boolean gold = rnd.nextFloat() < 0.2f;
                    MoteSystem.spawn(new MoteSystem.Mote()
                            .at(cx + Math.cos(ang) * rad, gy + 0.3 + rnd.nextDouble() * 2.5, cz + Math.sin(ang) * rad)
                            .vel(0, 0.01, 0)
                            .target(cx, gy + 3.2, cz, 0.008f, 0.006f)
                            .color(gold ? 0.95f : 0.45f, gold ? 0.9f : 1f, gold ? 0.55f : 0.85f, 0.75f)
                            .size(0.08f + rnd.nextFloat() * 0.09f).life(150).drift(0.0012f, 0.93f).flick(0.25f));
                }
            }
        }

        // nước lấp lánh ở cảng
        if (zk == AmbientClient.Z_PORT) {
            for (int n = MoteSystem.roll(rnd, 1.8 * p * (0.25 + day)); n-- > 0; ) {
                double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 22;
                int x = Mth.floor(px + Math.cos(ang) * rad), zz = Mth.floor(pz + Math.sin(ang) * rad);
                BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos(x, Mth.floor(py) + 2, zz);
                for (int dy = 0; dy < 9; dy++, bp.move(0, -1, 0)) {
                    if (lv.getFluidState(bp).is(FluidTags.WATER) && lv.getBlockState(bp.above()).isAir()) {
                        MoteSystem.spawn(new MoteSystem.Mote().at(x + rnd.nextDouble(), bp.getY() + 0.9, zz + rnd.nextDouble())
                                .vel(0, 0.002, 0).color(0.8f, 0.95f, 1f, 0.9f).size(0.045f + rnd.nextFloat() * .05f)
                                .life(14 + rnd.nextInt(16)).drift(0f, 1f).flick(0.85f));
                        break;
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- đèn lồng / lò
    private static void scanBlocks(ClientLevel lv, double px, double py, double pz) {
        int y = Mth.floor(py) - 10 + (int) (AmbientClient.ticks() % 21);
        int cx = Mth.floor(px), cz = Mth.floor(pz);
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        for (int x = cx - 22; x <= cx + 22; x++) {
            for (int z = cz - 22; z <= cz + 22; z++) {
                if (!lv.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
                bp.set(x, y, z);
                BlockState s = lv.getBlockState(bp);
                if (s.isAir()) continue;
                if (s.getBlock() instanceof LanternBlock || s.getBlock() instanceof TorchBlock || s.getBlock() instanceof WallTorchBlock) {
                    if (LANTERNS.size() < 500) LANTERNS.add(bp.asLong());
                } else if (s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(AbstractFurnaceBlock.LIT)) {
                    if (FURNACES.size() < 80) FURNACES.add(bp.asLong());
                }
            }
        }
        double r2 = 30 * 30;
        trim(LANTERNS, px, py, pz, r2);
        trim(FURNACES, px, py, pz, r2);
    }

    private static void trim(Set<Long> set, double px, double py, double pz, double r2) {
        for (Iterator<Long> it = set.iterator(); it.hasNext(); ) {
            BlockPos q = BlockPos.of(it.next());
            double dx = q.getX() - px, dy = q.getY() - py, dz = q.getZ() - pz;
            if (dx * dx + dy * dy + dz * dz > r2) it.remove();
        }
    }

    private static void lanterns(ClientLevel lv, RandomSource rnd, float p, float night) {
        float f = Math.max(night * 0.95f, 0f) + p * 0.12f;
        f = Math.min(1f, f) * Math.max(0.05f, 1f - AmbientClient.rain() * 0.4f);
        if (f <= 0.05f || LANTERNS.isEmpty()) return;
        for (Iterator<Long> it = LANTERNS.iterator(); it.hasNext(); ) {
            BlockPos q = BlockPos.of(it.next());
            BlockState s = lv.getBlockState(q);
            boolean lantern = s.getBlock() instanceof LanternBlock;
            if (!lantern && !(s.getBlock() instanceof TorchBlock) && !(s.getBlock() instanceof WallTorchBlock)) { it.remove(); continue; }
            boolean soul = s.is(Blocks.SOUL_LANTERN) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH);
            float[] col = soul ? new float[]{0.45f, 0.85f, 1f} : new float[]{1f, 0.72f, 0.32f};
            double cy = q.getY() + (lantern ? 0.45 : 0.75);
            if (rnd.nextFloat() < 0.045f * f * MoteSystem.density()) {
                MoteSystem.spawn(new MoteSystem.Mote().at(q.getX() + .5 + (rnd.nextDouble() - .5) * .3, cy, q.getZ() + .5 + (rnd.nextDouble() - .5) * .3)
                        .vel((rnd.nextDouble() - .5) * .004, 0.006 + rnd.nextDouble() * .006, (rnd.nextDouble() - .5) * .004)
                        .color(col[0], col[1], col[2], 0.85f).size(0.035f + rnd.nextFloat() * .035f).life(70 + rnd.nextInt(70)).drift(0.0016f, 0.98f).flick(0.5f));
            }
            if (rnd.nextFloat() < 0.07f * f * MoteSystem.density()) { // quầng sáng mềm
                MoteSystem.spawn(new MoteSystem.Mote().at(q.getX() + .5, cy, q.getZ() + .5).vel(0, 0, 0)
                        .color(col[0], col[1] * 0.9f, col[2], 0.13f).size(0.5f + rnd.nextFloat() * .2f).life(46).drift(0f, 1f).flick(0.2f));
            }
        }
    }

    private static void chimneys(ClientLevel lv, RandomSource rnd, float p) {
        if (MoteSystem.density() <= 0f) return;
        for (Iterator<Long> it = FURNACES.iterator(); it.hasNext(); ) {
            BlockPos q = BlockPos.of(it.next());
            BlockState s = lv.getBlockState(q);
            if (!(s.getBlock() instanceof AbstractFurnaceBlock) || !s.getValue(AbstractFurnaceBlock.LIT)) { it.remove(); continue; }
            if (rnd.nextFloat() < 0.05f * p) {
                lv.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, q.getX() + .5 + (rnd.nextDouble() - .5) * .3, q.getY() + 1.1, q.getZ() + .5 + (rnd.nextDouble() - .5) * .3,
                        (rnd.nextDouble() - .5) * .002, 0.06, (rnd.nextDouble() - .5) * .002);
            }
        }
    }

    // ---------------------------------------------------------------- đom đóm
    private static void fireflies(ClientLevel lv, RandomSource rnd, double px, double py, double pz, float wild) {
        if (MoteSystem.count(TAG_FIREFLY) > 90) return;
        for (int n = MoteSystem.roll(rnd, 0.4 * wild); n-- > 0; ) {
            double ang = rnd.nextDouble() * Math.PI * 2, rad = 3 + rnd.nextDouble() * 14;
            int x = Mth.floor(px + Math.cos(ang) * rad), z = Mth.floor(pz + Math.sin(ang) * rad);
            int gy = groundY(lv, x, z, (int) py);
            if (py - gy > 3 || gy - py > 8) continue;
            BlockPos below = new BlockPos(x, gy - 1, z);
            BlockState bs = lv.getBlockState(below);
            boolean water = lv.getFluidState(below).is(FluidTags.WATER) || nearWater(lv, below);
            boolean grass = bs.is(Blocks.GRASS_BLOCK) || bs.is(BlockTags.LEAVES) || bs.is(Blocks.MOSS_BLOCK);
            if (!water && !(grass && rnd.nextFloat() < 0.35f)) continue;
            double y = gy + 0.3 + rnd.nextDouble() * 1.6 + (water ? 0.2 : 0);
            MoteSystem.spawn(new MoteSystem.Mote().tag(TAG_FIREFLY).at(x + rnd.nextDouble(), y, z + rnd.nextDouble())
                    .vel((rnd.nextDouble() - .5) * .006, (rnd.nextDouble() - .5) * .003, (rnd.nextDouble() - .5) * .006)
                    .color(0.78f + rnd.nextFloat() * .15f, 1f, 0.35f, 0.95f).size(0.05f + rnd.nextFloat() * .03f)
                    .life(200 + rnd.nextInt(220)).drift(0.0035f, 0.965f).flick(0.9f));
        }
    }

    private static boolean nearWater(ClientLevel lv, BlockPos b) {
        for (int dx = -3; dx <= 3; dx += 3) for (int dz = -3; dz <= 3; dz += 3)
            if (lv.getFluidState(b.offset(dx, 0, dz)).is(FluidTags.WATER)) return true;
        return false;
    }
}
