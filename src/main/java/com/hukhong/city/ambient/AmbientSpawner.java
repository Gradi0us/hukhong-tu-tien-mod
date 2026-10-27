package com.hukhong.city.ambient;

import com.hukhong.HuKhong;
import com.hukhong.city.WorldBuildData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Sinh dan cu trang tri khi nguoi choi o gan khu dan cu (thanh/cang/tong), toi da {@link #cap} moi noi.
 * Khong persistent: roi xa nguoi choi la vanilla tu despawn (removeWhenFarAway), nen khong don dong qua thoi gian.
 * Vi tri: cot dat o cao do "mat pho" (gan ban tho), thay bau troi, trong 64 o quanh nguoi choi, khong phai nuoc/la/mai.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class AmbientSpawner {
    private AmbientSpawner() {}

    public static final int NEAR = 96, PER_PASS = 4;

    public static int cap(WorldBuildData.Settlement s) {
        return switch (s.kind()) { case "port" -> 32; case "sect" -> 22; case "ruin" -> 0; default -> 36; };
    }

    /** Pha vai tro theo loai khu (tong ti le 100). */
    private static int roleFor(String kind, int r) {
        return switch (kind) {
            case "port" -> r < 35 ? AmbientNpc.DOCK : r < 65 ? AmbientNpc.CITIZEN : r < 85 ? AmbientNpc.COURIER : r < 95 ? AmbientNpc.GUARD : AmbientNpc.CHILD;
            case "sect" -> r < 75 ? AmbientNpc.DISCIPLE : r < 85 ? AmbientNpc.CHILD : r < 95 ? AmbientNpc.GUARD : AmbientNpc.CITIZEN;
            default -> r < 50 ? AmbientNpc.CITIZEN : r < 65 ? AmbientNpc.GUARD : r < 85 ? AmbientNpc.COURIER : AmbientNpc.CHILD;
        };
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 60 != 23) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        var settlements = WorldBuildData.get(ow).settlements().values();
        for (var s : settlements) {
            int cap = cap(s);
            if (cap <= 0) continue;
            ServerPlayer near = null;
            for (ServerPlayer p : ow.players()) {
                double dx = Math.max(0, Math.max(s.minX() - p.getX(), p.getX() - s.maxX()));
                double dz = Math.max(0, Math.max(s.minZ() - p.getZ(), p.getZ() - s.maxZ()));
                if (dx * dx + dz * dz <= NEAR * NEAR) { near = p; break; }
            }
            if (near == null) continue;
            AABB box = new AABB(s.minX(), s.y() - 24, s.minZ(), s.maxX() + 1, s.y() + 80, s.maxZ() + 1);
            int have = ow.getEntitiesOfClass(AmbientNpc.class, box).size();
            for (int n = 0; n < PER_PASS && have < cap; n++) if (trySpawn(ow, s, near)) have++;
        }
    }

    private static boolean trySpawn(ServerLevel l, WorldBuildData.Settlement s, ServerPlayer p) {
        int window = switch (s.kind()) { case "port" -> 8; case "city" -> 4; default -> 3; };
        for (int attempt = 0; attempt < 10; attempt++) {
            int x0 = Math.max(s.minX(), (int) p.getX() - 64), x1 = Math.min(s.maxX(), (int) p.getX() + 64);
            int z0 = Math.max(s.minZ(), (int) p.getZ() - 64), z1 = Math.min(s.maxZ(), (int) p.getZ() + 64);
            if (x1 <= x0 || z1 <= z0) return false;
            int x = x0 + l.random.nextInt(x1 - x0 + 1), z = z0 + l.random.nextInt(z1 - z0 + 1);
            if (!l.hasChunk(x >> 4, z >> 4)) continue;
            if (Math.abs(p.getX() - x) < 10 && Math.abs(p.getZ() - z) < 10) continue; // khong hien ngay truoc mat
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = l.getBlockState(pos.below());
            if (Math.abs(y - s.y()) > window + 1 || !below.blocksMotion() || below.is(BlockTags.LEAVES)
                    || !l.getFluidState(pos.below()).isEmpty() || !l.getFluidState(pos).isEmpty()
                    || below.is(BlockTags.SLABS) || below.is(BlockTags.STAIRS)) continue;
            int role = roleFor(s.kind(), l.random.nextInt(100));
            AmbientNpc n = AmbientRegistry.AMBIENT.get().create(l);
            if (n == null) return false;
            n.moveTo(x + 0.5, y, z + 0.5, l.random.nextFloat() * 360f, 0f);
            n.setup(role, l.random.nextInt(AmbientRegistry.SKINS), new BlockPos(s.cx(), s.y(), s.cz()),
                    Math.max(24, Math.min(110, Math.max(s.maxX() - s.minX(), s.maxZ() - s.minZ()) / 2)));
            n.finalizeSpawn(l, l.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            l.addFreshEntity(n);
            return true;
        }
        return false;
    }

    /** Kich hoat lai cho /kill dan cu khi bao tri (dung boi test). */
    public static List<AmbientNpc> in(ServerLevel l, AABB box) { return l.getEntitiesOfClass(AmbientNpc.class, box); }

}
