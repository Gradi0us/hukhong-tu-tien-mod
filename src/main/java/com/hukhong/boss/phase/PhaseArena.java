package com.hukhong.boss.phase;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.ArenaEffect;
import com.hukhong.boss.phase.BossPhaseConfig.Pal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Biến đổi đấu trường theo pha: trời tím/đỏ (client tô, server làm tươi gói), mưa máu (hạt phía client), sàn nứt và cột mọc
 * (khối tạm: lưu snapshot, khôi phục sau trận ~8s khi boss chết/biến mất, và khi server dừng). Chỉ đổi khối tự nhiên rắn,
 * không động vào tile entity. Snapshot nằm trong bộ nhớ (giống BossArena): crash giữa trận thì khối tạm còn lại.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class PhaseArena {
    private PhaseArena() {}

    public static final int RADIUS = 14, MAX_BLOCKS = 170, RESTORE_DELAY = 160, SKY_REFRESH = 40;

    private static final class Active {
        final ServerLevel level;
        final Mob boss;
        final Pal pal;
        final double cx, cz;
        final Set<ArenaEffect> on = EnumSet.noneOf(ArenaEffect.class);
        final Map<Long, BlockState> snap = new HashMap<>();
        int closeIn = -1;

        Active(ServerLevel level, Mob boss, Pal pal) {
            this.level = level; this.boss = boss; this.pal = pal; this.cx = boss.getX(); this.cz = boss.getZ();
        }
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    // ------------------------------------------------------------------ API
    /** Bật các hiệu ứng (idempotent). {@code blocks=false} (nạp lại từ save) chỉ bật hiệu ứng không đổi khối. */
    public static void enable(Mob boss, Pal pal, Set<ArenaEffect> effects, boolean blocks) {
        if (!(boss.level() instanceof ServerLevel sl) || effects.isEmpty()) return;
        Active a = find(boss);
        if (a == null) { a = new Active(sl, boss, pal); ACTIVE.add(a); }
        for (ArenaEffect e : effects) {
            if (a.on.contains(e)) continue;
            boolean changesBlocks = e == ArenaEffect.CRACKED_FLOOR || e == ArenaEffect.VOID_PILLARS;
            if (changesBlocks && !blocks) continue;
            a.on.add(e);
            if (e == ArenaEffect.CRACKED_FLOOR) crack(a);
            else if (e == ArenaEffect.VOID_PILLARS) pillars(a);
        }
        sendSky(a);
    }

    public static Set<ArenaEffect> active(Mob boss) {
        Active a = find(boss);
        Set<ArenaEffect> out = EnumSet.noneOf(ArenaEffect.class);
        if (a != null) out.addAll(a.on);
        return out;
    }

    /** Số khối tạm đang giữ cho boss (GameTest). */
    public static int tempBlocks(Mob boss) {
        Active a = find(boss);
        return a == null ? 0 : a.snap.size();
    }

    /** Khôi phục ngay (GameTest / dọn thủ công). */
    public static void restoreNow(Mob boss) {
        Active a = find(boss);
        if (a != null) { restore(a); ACTIVE.remove(a); }
    }

    private static Active find(Mob boss) {
        for (Active a : ACTIVE) if (a.boss == boss) return a;
        return null;
    }

    // ------------------------------------------------------------------ khối
    private static Block floorBlock(Pal p) {
        return switch (p) {
            case VOID -> Blocks.CRACKED_DEEPSLATE_TILES; case FROST -> Blocks.PACKED_ICE; case TOXIC -> Blocks.MOSS_BLOCK;
            case SAND -> Blocks.SOUL_SAND; case HOLY -> Blocks.CRACKED_STONE_BRICKS; case BLOOD -> Blocks.RED_NETHER_BRICKS;
            case MOON -> Blocks.CRACKED_DEEPSLATE_BRICKS;
        };
    }

    private static Block pillarBlock(Pal p) {
        return switch (p) {
            case VOID -> Blocks.CRYING_OBSIDIAN; case FROST -> Blocks.BLUE_ICE; case TOXIC -> Blocks.MOSSY_COBBLESTONE;
            case SAND -> Blocks.CUT_SANDSTONE; case HOLY -> Blocks.QUARTZ_PILLAR; case BLOOD -> Blocks.NETHER_WART_BLOCK;
            case MOON -> Blocks.AMETHYST_BLOCK;
        };
    }

    private static boolean put(Active a, BlockPos p, BlockState st, boolean needSolid) {
        if (a.snap.size() >= MAX_BLOCKS || !a.level.hasChunkAt(p)) return false;
        BlockState old = a.level.getBlockState(p);
        if (old == st || a.level.getBlockEntity(p) != null || old.getDestroySpeed(a.level, p) < 0 || !old.getFluidState().isEmpty()) return false;
        if (needSolid ? !old.isSolid() : !old.isAir()) return false;
        a.snap.putIfAbsent(p.asLong(), old);
        a.level.setBlock(p, st, 18);
        return true;
    }

    private static BlockPos surface(Active a, int x, int z) {
        return new BlockPos(x, a.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
    }

    /** Sàn nứt: 8 vết nứt toả tia từ tâm, rìa lởm chởm. */
    private static void crack(Active a) {
        BlockState st = floorBlock(a.pal).defaultBlockState();
        RandomSource rnd = a.level.random;
        for (int s = 0; s < 8; s++) {
            double ang = s * Math.PI / 4 + (rnd.nextDouble() - 0.5) * 0.3;
            for (int r = 2; r <= RADIUS; r++) {
                if (rnd.nextInt(4) == 0) continue;
                int x = (int) Math.floor(a.cx + Math.cos(ang) * r), z = (int) Math.floor(a.cz + Math.sin(ang) * r);
                put(a, surface(a, x, z), st, true);
                if (rnd.nextInt(3) == 0) put(a, surface(a, x + 1, z), st, true);
            }
        }
        a.level.sendParticles(ParticleTypes.POOF, a.cx, a.boss.getY() + 0.3, a.cz, 40, RADIUS * 0.4, 0.2, RADIUS * 0.4, 0.05);
    }

    /** Cột mọc quanh vành đấu trường (cao 4-6). */
    private static void pillars(Active a) {
        BlockState st = pillarBlock(a.pal).defaultBlockState();
        RandomSource rnd = a.level.random;
        int n = 6;
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI * 2 / n + rnd.nextDouble() * 0.4;
            int x = (int) Math.floor(a.cx + Math.cos(ang) * RADIUS * 0.75), z = (int) Math.floor(a.cz + Math.sin(ang) * RADIUS * 0.75);
            BlockPos base = surface(a, x, z).above();
            int h = 4 + rnd.nextInt(3);
            for (int y = 0; y < h; y++) if (!put(a, base.above(y), st, false)) break;
            a.level.sendParticles(ParticleTypes.REVERSE_PORTAL, x + 0.5, base.getY() + 1, z + 0.5, 20, 0.3, 1.5, 0.3, 0.1);
        }
    }

    private static void restore(Active a) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Map.Entry<Long, BlockState> e : a.snap.entrySet()) {
            p.set(e.getKey());
            if (a.level.hasChunkAt(p)) a.level.setBlock(p, e.getValue(), 18);
        }
        a.snap.clear();
    }

    // ------------------------------------------------------------------ trời / mưa
    private static void sendSky(Active a) {
        boolean sky = a.on.contains(ArenaEffect.PURPLE_SKY), rain = a.on.contains(ArenaEffect.BLOOD_RAIN);
        if (!sky && !rain) return;
        float level = sky ? 0.85f : 0.55f;
        float war = rain ? 1f : 0f;  // mưa máu đi kèm trời đỏ; không mưa thì trời tím Hư Hoá
        int kind = !rain ? 0 : a.pal == Pal.TOXIC ? 3 : a.pal == Pal.VOID || a.pal == Pal.MOON ? 2 : 1;
        BossSkyPayload pl = new BossSkyPayload(level, war, kind, SKY_REFRESH + 30, 60);
        for (ServerPlayer p : a.level.players())
            if (p.distanceToSqr(a.boss) < 96 * 96) {
                try { PacketDistributor.sendToPlayer(p, pl); } catch (RuntimeException ignored) { }
            }
    }

    // ------------------------------------------------------------------ vòng đời
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (ACTIVE.isEmpty()) return;
        long t = e.getServer().getTickCount();
        for (Iterator<Active> it = ACTIVE.iterator(); it.hasNext(); ) {
            Active a = it.next();
            boolean alive = a.boss.isAlive() && !a.boss.isRemoved();
            if (!alive) {
                if (a.snap.isEmpty()) { it.remove(); continue; }
                if (a.closeIn < 0) a.closeIn = RESTORE_DELAY;
                if (--a.closeIn <= 0) { restore(a); it.remove(); }
                continue;
            }
            if (t % SKY_REFRESH == 0) sendSky(a);
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (Active a : ACTIVE) restore(a);
        ACTIVE.clear();
    }
}
