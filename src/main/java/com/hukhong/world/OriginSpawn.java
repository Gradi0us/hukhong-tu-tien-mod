package com.hukhong.world;

import com.hukhong.charcreate.Origin;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.core.Path;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;
import java.util.Set;

/** Xuat than -> noi xuat hien lan dau (goi 1 lan luc xac nhan tao nhan vat) + dat diem hoi sinh. */
public final class OriginSpawn {
    private OriginSpawn() {}

    public enum Kind { CITY, SECT, VILLAGE, NONE }

    /** CITY/NOBLE -> thanh; SECT -> son mon; BASTARD (con hoang)/VILLAGE -> lang ngau nhien; RUINS -> giu nguyen. */
    public static Kind kindOf(Origin o) {
        return switch (o) {
            case CITY, NOBLE -> Kind.CITY;
            case SECT -> Kind.SECT;
            case BASTARD, VILLAGE -> Kind.VILLAGE;
            case RUINS -> Kind.NONE;
        };
    }

    public static void apply(ServerPlayer p, Origin origin, Set<Path> paths, long seed) {
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        Kind k = kindOf(origin);
        BlockPos target = switch (k) {
            case CITY -> nearestCity(ow);
            case SECT -> pickSect(ow, paths);
            case VILLAGE -> randomVillage(ow, seed);
            case NONE -> null;
        };
        if (target == null) return;
        place(p, ow, target);
        p.sendSystemMessage(Component.translatable("hukhong.spawn.origin." + k.name().toLowerCase(java.util.Locale.ROOT)));
        // Thanh/son mon dung lazy: neu chua xay, dat lai vi tri khi da xay (tranh ket trong cong trinh).
        if (k != Kind.VILLAGE) pendingRecheck(p, ow, target, k);
    }

    /** Thanh gan diem spawn the gioi nhat (dung tam that neu da dung, neu khong dung toa do du kien). */
    static BlockPos nearestCity(ServerLevel ow) {
        BlockPos sp = ow.getSharedSpawnPos();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (CityDef d : CityDef.ALL) {
            BlockPos c = CityService.builtCenter(ow, d);
            if (c == null) c = d.index() == 0 ? new BlockPos(sp.getX() + d.ax(), 64, sp.getZ() + d.az()) : CityService.site(ow, d);
            double dd = c.distSqr(new BlockPos(sp.getX(), c.getY(), sp.getZ()));
            if (dd < bd) { bd = dd; best = c; }
        }
        return best;
    }

    /** Son mon hop dao da chon (hoac khong rang buoc dao), gan spawn nhat; bo qua tong an. */
    static BlockPos pickSect(ServerLevel ow, Set<Path> paths) {
        BlockPos sp = ow.getSharedSpawnPos();
        SectDef best = null;
        double bd = Double.MAX_VALUE;
        for (int pass = 0; pass < 2 && best == null; pass++) {
            for (SectDef d : SectDef.ALL) {
                if (d.hidden() || !d.buildable()) continue;
                if (pass == 0 && !d.paths().isEmpty() && java.util.Collections.disjoint(d.paths(), paths)) continue;
                BlockPos c = SectEvents.site(ow, d);
                double dd = c.distSqr(new BlockPos(sp.getX(), c.getY(), sp.getZ()));
                if (dd < bd) { bd = dd; best = d; }
            }
        }
        return best == null ? null : SectEvents.site(ow, best);
    }

    /** Lang gan mot diem ngau nhien (0..1500 block quanh spawn, theo seed) nhat. */
    static BlockPos randomVillage(ServerLevel ow, long seed) {
        Random r = new Random(seed ^ 0x5649L);
        BlockPos sp = ow.getSharedSpawnPos();
        double ang = r.nextDouble() * Math.PI * 2, dist = r.nextDouble() * 1500;
        BlockPos from = new BlockPos(sp.getX() + (int) (Math.cos(ang) * dist), 64, sp.getZ() + (int) (Math.sin(ang) * dist));
        return ow.findNearestMapStructure(StructureTags.VILLAGE, from, 100, false);
    }

    private static void place(ServerPlayer p, ServerLevel ow, BlockPos xz) {
        ow.getChunk(xz.getX() >> 4, xz.getZ() >> 4); // nap chunk dong bo
        int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz.getX(), xz.getZ());
        p.teleportTo(ow, xz.getX() + 0.5, y + 0.1, xz.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.setRespawnPosition(Level.OVERWORLD, new BlockPos(xz.getX(), y, xz.getZ()), 0f, true, false);
    }

    private static void pendingRecheck(ServerPlayer p, ServerLevel ow, BlockPos target, Kind k) {
        // Sau ~8s (thanh lazy xay khi nguoi choi den gan), dat lai Y an toan theo heightmap va cap nhat diem hoi sinh.
        p.server.tell(new net.minecraft.server.TickTask(p.server.getTickCount() + 160, () -> {
            if (p.hasDisconnected() || p.level() != ow) return;
            if (Math.abs(p.getX() - target.getX()) > 48 || Math.abs(p.getZ() - target.getZ()) > 48) return;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
            if (p.getY() < y - 1) p.teleportTo(ow, target.getX() + 0.5, y + 0.1, target.getZ() + 0.5, p.getYRot(), p.getXRot());
            p.setRespawnPosition(Level.OVERWORLD, new BlockPos(target.getX(), y, target.getZ()), 0f, true, false);
        }));
    }
}
