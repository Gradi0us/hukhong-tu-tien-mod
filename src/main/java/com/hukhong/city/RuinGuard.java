package com.hukhong.city;

import com.hukhong.HuKhong;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.HuMob;
import com.hukhong.entity.HuVuong;
import com.hukhong.entity.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Tan tich (city_ruined): 1 boss Hu Vuong (cap 30) o tam + quai Hu Hoa cap cao sinh lien tuc trong ranh gioi khi co nguoi choi gan.
 * Quai khong persistent (roi xa la despawn) va duoc bo sung toi {@link #MOBS} con.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class RuinGuard {
    private RuinGuard() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String TAG = "hukhong_ruin_mob", BOSS_TAG = "hukhong_ruin_boss";
    public static final int MOBS = 24, MIN_LV = 55, MAX_LV = 85, NEAR = 110;

    /** Goi sau khi dat xong: dat boss (neu chua co) o tam. */
    public static void spawnBoss(ServerLevel l, CityDef d, BlockPos center) {
        AABB box = new AABB(center).inflate(160, 80, 160);
        if (!l.getEntitiesOfClass(HuVuong.class, box, e -> e.getTags().contains(BOSS_TAG)).isEmpty()) return;
        HuVuong b = ModEntities.HU_VUONG.get().create(l);
        if (b == null) return;
        BlockPos g = CityMarket.ground(l, center.above());
        b.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, l.random.nextFloat() * 360f, 0f);
        b.addTag(BOSS_TAG);
        b.finalizeSpawn(l, l.getCurrentDifficultyAt(g), MobSpawnType.STRUCTURE, null);
        l.addFreshEntity(b);
        LOG.info("Ruin {}: boss Hu Vuong at {}", d.id(), g);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 100 != 41) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || ow.players().isEmpty()) return;
        for (var s : WorldBuildData.get(ow).settlements().values()) {
            if (!s.kind().equals("ruin")) continue;
            var near = ow.players().stream().filter(p -> {
                double dx = Math.max(0, Math.max(s.minX() - p.getX(), p.getX() - s.maxX()));
                double dz = Math.max(0, Math.max(s.minZ() - p.getZ(), p.getZ() - s.maxZ()));
                return dx * dx + dz * dz <= NEAR * NEAR;
            }).findFirst().orElse(null);
            if (near == null) continue;
            AABB box = new AABB(s.minX(), s.y() - 40, s.minZ(), s.maxX() + 1, s.y() + 120, s.maxZ() + 1);
            int have = ow.getEntitiesOfClass(Mob.class, box, m -> m.getTags().contains(TAG)).size();
            for (int n = 0; n < 4 && have < MOBS; n++) if (spawnMob(ow, s, near)) have++;
        }
    }

    private static boolean spawnMob(ServerLevel l, WorldBuildData.Settlement s, net.minecraft.server.level.ServerPlayer p) {
        for (int a = 0; a < 8; a++) {
            int x0 = Math.max(s.minX(), (int) p.getX() - 70), x1 = Math.min(s.maxX(), (int) p.getX() + 70);
            int z0 = Math.max(s.minZ(), (int) p.getZ() - 70), z1 = Math.min(s.maxZ(), (int) p.getZ() + 70);
            if (x1 <= x0 || z1 <= z0) return false;
            int x = x0 + l.random.nextInt(x1 - x0 + 1), z = z0 + l.random.nextInt(z1 - z0 + 1);
            if (!l.hasChunk(x >> 4, z >> 4) || Math.abs(p.getX() - x) < 14 && Math.abs(p.getZ() - z) < 14) continue;
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (!l.getBlockState(pos.below()).blocksMotion() || !l.getFluidState(pos.below()).isEmpty()) continue;
            int r = l.random.nextInt(3);
            EntityType<? extends Mob> t = r == 0 ? ModEntities.HU_THI.get() : r == 1 ? ModEntities.HU_COT.get() : ModEntities.HU_CHU.get();
            Mob m = t.create(l);
            if (m == null) return false;
            m.moveTo(x + 0.5, y, z + 0.5, l.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(l, l.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            if (m instanceof HuMob) { // dat cap cao (finalizeSpawn chon cap theo khoang cach), tinh lai chi so
                int lv = MIN_LV + l.random.nextInt(MAX_LV - MIN_LV + 1);
                CompoundTag tag = new CompoundTag();
                tag.putInt(HuHoa.TAG_LEVEL, lv);
                m.readAdditionalSaveData(tag);
                HuHoa.applyStats(m, lv, r == 0 ? 1.45 : r == 1 ? 1.5 : 1.8, r == 2 ? 1.2 : 1.0);
            }
            m.addTag(TAG);
            l.addFreshEntity(m);
            return true;
        }
        return false;
    }
}
