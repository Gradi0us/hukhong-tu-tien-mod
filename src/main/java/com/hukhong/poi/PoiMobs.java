package com.hukhong.poi;

import com.hukhong.entity.HuHoa;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Sinh / dem quai canh giu dia diem. Quai gan the "hukhong_poi" = key dia diem, bat bien mat (khong despawn). */
public final class PoiMobs {
    private PoiMobs() {}

    public static final String TAG = "hukhong_poi";

    public static BlockPos spot(ServerLevel l, PoiInstance i, String at, RandomSource r) {
        List<PoiInstance.Act> sp = i.actsOf("boss".equals(at) ? "sp_boss" : "sp_room");
        if (!sp.isEmpty()) return sp.get(r.nextInt(sp.size())).pos();
        double ang = r.nextDouble() * Math.PI * 2, rad = 4 + r.nextInt(5);
        int x = i.x + (int) Math.round(Math.cos(ang) * rad), z = i.z + (int) Math.round(Math.sin(ang) * rad);
        return new BlockPos(x, Math.max(i.y, l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)), z);
    }

    public static EntityType<?> type(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "hukhong:" + id);
        return rl == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(rl).orElse(null);
    }

    /** Cap theo vong khoang cach toi diem hoi sinh (nhu quai thuong), kep vao khoang level cua dia diem. */
    public static int levelFor(ServerLevel l, BlockPos at, PoiDef d, PoiDef.MobSpawn ms, RandomSource r) {
        int base = HuHoa.levelFor(l, at, r);
        return clampLevel(base, d.levelMin(), d.levelMax(), ms.levelOffset());
    }

    public static int clampLevel(int base, int min, int max, int offset) {
        return Math.max(1, Math.min(HuHoa.MAX_MOB_LEVEL, Math.max(min, Math.min(max, base)) + offset));
    }

    /** Sinh mot con tai vi tri chi dinh (null = tu chon theo ms.at). Tra null neu loai khong ton tai. */
    public static Mob spawn(ServerLevel l, PoiInstance i, PoiDef d, PoiDef.MobSpawn ms, BlockPos at, RandomSource r) {
        EntityType<?> t = type(ms.type());
        if (t == null) return null;
        Entity e = t.create(l);
        if (!(e instanceof Mob m)) return null;
        BlockPos p = at != null ? at : spot(l, i, ms.at(), r);
        m.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360f, 0f);
        m.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
        HuHoa.applyStats(m, levelFor(l, p, d, ms, r), ms.hpMul(), ms.dmgMul());
        if (!ms.name().isEmpty()) { m.setCustomName(Component.translatable(ms.name())); m.setCustomNameVisible(true); }
        m.setPersistenceRequired();
        m.getPersistentData().putString(TAG, i.key);
        l.addFreshEntity(m);
        return m;
    }

    public static int spawnAll(ServerLevel l, PoiInstance i, PoiDef d, RandomSource r) {
        int n = 0;
        for (PoiDef.MobSpawn ms : d.mobs()) for (int k = 0; k < ms.count(); k++) if (spawn(l, i, d, ms, null, r) != null) n++;
        return n;
    }

    public static List<Mob> alive(ServerLevel l, PoiInstance i, int radius) {
        AABB box = new AABB(i.x - radius, l.getMinBuildHeight(), i.z - radius, i.x + radius, l.getMaxBuildHeight(), i.z + radius);
        List<Mob> out = new ArrayList<>();
        for (Mob m : l.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && i.key.equals(m.getPersistentData().getString(TAG)))) out.add(m);
        return out;
    }

    public static void discardAll(ServerLevel l, PoiInstance i, int radius) {
        for (Mob m : alive(l, i, radius)) m.discard();
    }
}
