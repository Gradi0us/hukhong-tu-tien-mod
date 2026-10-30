package com.hukhong.arena;

import com.hukhong.HuKhong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dau truong boss: nen tron + cot + vong rune (dung bang code, luu snapshot de don sau tran) va
 * tuong chan (hat + day nguoi choi / boss vao lai) trong luc boss con song.
 * Snapshot nam trong bo nho: neu server tat giua tran thi san van con (khong crash, chi khong don).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class BossArena {
    private BossArena() {}

    /** Chu de: floor, accent (rune), pillar, light. Ten block "hukhong:..." tra theo registry, thieu thi dung fallback. */
    public enum Theme {
        HU_VUONG("hukhong:hu_khong_da", Blocks.DEEPSLATE_TILES, "hukhong:hu_khong_tinh", Blocks.AMETHYST_BLOCK,
                Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.PURPLE_STAINED_GLASS, Blocks.CRYING_OBSIDIAN),
        SU_DO(null, Blocks.POLISHED_BLACKSTONE, null, Blocks.CRYING_OBSIDIAN,
                Blocks.CHISELED_POLISHED_BLACKSTONE, null, Blocks.SOUL_LANTERN),
        DA_KHUYET("hukhong:hu_khong_da", Blocks.END_STONE_BRICKS, "hukhong:hu_khong_tinh", Blocks.AMETHYST_BLOCK,
                Blocks.PURPUR_PILLAR, null, Blocks.SEA_LANTERN);

        final String floorId, accentId;
        final Block floorFb, accentFb, pillar, light;

        Theme(String floorId, Block floorFb, String accentId, Block accentFb, Block pillar, Block unused, Block light) {
            this.floorId = floorId; this.floorFb = floorFb; this.accentId = accentId; this.accentFb = accentFb;
            this.pillar = pillar; this.light = light;
        }

        Block floor() { return lookup(floorId, floorFb); }
        Block accent() { return lookup(accentId, accentFb); }

        private static Block lookup(String id, Block fb) {
            if (id == null) return fb;
            return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).filter(b -> b != Blocks.AIR).orElse(fb);
        }
    }

    private static final class Arena {
        final ServerLevel level;
        final Mob boss;
        final double cx, cz;
        final int radius, floorY;
        final Set<UUID> engaged = new HashSet<>();
        final Map<Long, BlockState> snapshot = new HashMap<>();
        int closeIn = -1;  // dem nguoc don san sau khi boss chet

        Arena(ServerLevel level, Mob boss, double cx, double cz, int radius, int floorY) {
            this.level = level; this.boss = boss; this.cx = cx; this.cz = cz; this.radius = radius; this.floorY = floorY;
        }
    }

    private static final List<Arena> ARENAS = new ArrayList<>();
    private static final int CLEAN_DELAY = 20 * 20;

    // ------------------------------------------------------------------ public API
    /**
     * Gan tuong chan quanh boss (tam = vi tri hien tai, hoac tam co dinh neu {@code center != null}).
     * {@code theme != null} va boss moi sinh (tickCount nho) thi dung them san dau truong.
     */
    public static void attach(Mob boss, int radius, Theme theme, boolean clearAbove, BlockPos center) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        for (Arena a : ARENAS) if (a.boss == boss) return;
        BlockPos c = center != null ? center : boss.blockPosition().below();
        Arena a = new Arena(sl, boss, c.getX() + 0.5, c.getZ() + 0.5, radius, c.getY());
        ARENAS.add(a);
        boolean overlap = false;
        for (Arena o : ARENAS) if (o != a && o.level == sl && Math.hypot(o.cx - a.cx, o.cz - a.cz) < o.radius + radius) overlap = true;
        if (theme != null && !overlap && boss.tickCount < 40) build(a, theme, clearAbove);
    }

    /** Người chơi đang bị nhốt trong một đấu trường boss còn sống (cấm xuyên tường). */
    public static boolean isEngaged(ServerPlayer p) {
        for (Arena a : ARENAS) if (a.engaged.contains(p.getUUID())) return true;
        return false;
    }

    // ------------------------------------------------------------------ build / restore
    private static void put(Arena a, BlockPos.MutableBlockPos p, BlockState st) {
        BlockState old = a.level.getBlockState(p);
        if (old == st || a.level.getBlockEntity(p) != null || old.getDestroySpeed(a.level, p) < 0) return;
        a.snapshot.putIfAbsent(p.asLong(), old);
        a.level.setBlock(p, st, 18);
    }

    private static void build(Arena a, Theme th, boolean clearAbove) {
        ServerLevel sl = a.level;
        int r = a.radius, cx = (int) Math.floor(a.cx), cz = (int) Math.floor(a.cz), cy = a.floorY;
        for (int i = (cx - r) >> 4; i <= (cx + r) >> 4; i++)
            for (int j = (cz - r) >> 4; j <= (cz + r) >> 4; j++) sl.getChunk(i, j);
        BlockState floor = th.floor().defaultBlockState(), accent = th.accent().defaultBlockState(),
                pillar = th.pillar.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) continue;
                // vanh ngoai rune, vong rune r-4, ngoi sao trung tam: accent; con lai: floor
                boolean rune = d >= r - 1.2 || (d >= r - 4.5 && d <= r - 3.5)
                        || d < 2.5 || (Math.abs(dx) == Math.abs(dz) && d < r - 5 && d > 5);
                BlockState top = rune ? accent : floor;
                for (int y = cy - 5; y < cy; y++) {
                    p.set(cx + dx, y, cz + dz);
                    if (!sl.getBlockState(p).isSolid()) put(a, p, floor);
                }
                p.set(cx + dx, cy, cz + dz);
                put(a, p, top);
                for (int y = cy + 1; clearAbove && y <= cy + 10; y++) {
                    p.set(cx + dx, y, cz + dz);
                    if (!sl.getBlockState(p).isAir()) put(a, p, air);
                }
            }
        }
        BlockState light = th.light.defaultBlockState();
        for (int i = 0; i < 8; i++) {  // 8 cot da + den
            double ang = i * Math.PI / 4 + Math.PI / 8;
            int px = cx + (int) Math.round(Math.cos(ang) * (r - 2)), pz = cz + (int) Math.round(Math.sin(ang) * (r - 2));
            for (int y = cy + 1; y <= cy + 7; y++) put(a, p.set(px, y, pz), y == cy + 4 ? accent : pillar);
            put(a, p.set(px, cy + 8, pz), light);
        }
        for (int i = 0; i < 4; i++) {  // 4 tru thap phia trong
            double ang = i * Math.PI / 2;
            int px = cx + (int) Math.round(Math.cos(ang) * (r / 2.0)), pz = cz + (int) Math.round(Math.sin(ang) * (r / 2.0));
            for (int y = cy + 1; y <= cy + 3; y++) put(a, p.set(px, y, pz), pillar);
            put(a, p.set(px, cy + 4, pz), light);
        }
    }

    private static void restore(Arena a) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Map.Entry<Long, BlockState> e : a.snapshot.entrySet()) {
            p.set(e.getKey());
            if (a.level.hasChunkAt(p)) a.level.setBlock(p, e.getValue(), 18);
        }
        a.snapshot.clear();
    }

    // ------------------------------------------------------------------ barrier
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (ARENAS.isEmpty()) return;
        long t = e.getServer().getTickCount();
        for (Iterator<Arena> it = ARENAS.iterator(); it.hasNext(); ) {
            Arena a = it.next();
            boolean alive = a.boss.isAlive() && !a.boss.isRemoved();
            if (!alive) {
                if (a.snapshot.isEmpty()) { it.remove(); continue; }
                if (a.closeIn < 0) a.closeIn = CLEAN_DELAY;
                if (--a.closeIn <= 0) { restore(a); it.remove(); }
                continue;
            }
            if (a.boss.level() != a.level) continue;
            enforce(a, t);
        }
    }

    private static void enforce(Arena a, long t) {
        double r = a.radius;
        // boss ra ngoai (kite / bi day) -> keo ve gan vanh
        double bx = a.boss.getX() - a.cx, bz = a.boss.getZ() - a.cz, bd = Math.hypot(bx, bz);
        if (bd > r + 2) {
            double k = (r - 2) / bd;
            a.boss.teleportTo(a.cx + bx * k, a.boss.getY(), a.cz + bz * k);
        }
        for (ServerPlayer p : a.level.players()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) { a.engaged.remove(p.getUUID()); continue; }
            double dx = p.getX() - a.cx, dz = p.getZ() - a.cz, d = Math.hypot(dx, dz);
            if (d < r - 1 && Math.abs(p.getY() - a.floorY) < 24) {
                a.engaged.add(p.getUUID());
            } else if (d > r && a.engaged.contains(p.getUUID())) {
                if (d > r + 4) {  // da qua xa (ender pearl, ...): dua ve trong vanh
                    double k = (r - 2) / d;
                    p.teleportTo(a.cx + dx * k, p.getY(), a.cz + dz * k);
                } else {
                    double k = -0.9 / d;
                    p.setDeltaMovement(dx * k, Math.max(p.getDeltaMovement().y, 0.1), dz * k);
                    p.hurtMarked = true;
                }
            }
            // hat tuong chan khi lai gan vanh
            if (t % 4 == 0 && d > r - 7 && d < r + 8 && a.engaged.contains(p.getUUID())) wall(a, p);
        }
    }

    private static void wall(Arena a, ServerPlayer p) {
        double ang0 = Math.atan2(p.getZ() - a.cz, p.getX() - a.cx);
        for (int i = -6; i <= 6; i++) {
            double ang = ang0 + i * (1.6 / a.radius);
            double x = a.cx + Math.cos(ang) * a.radius, z = a.cz + Math.sin(ang) * a.radius;
            for (int h = 0; h < 4; h++)
                a.level.sendParticles(p, ParticleTypes.END_ROD, true, x, p.getY() + h * 1.2 - 1, z, 1, 0, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        for (Arena a : ARENAS) a.engaged.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        ARENAS.clear();
    }
}
