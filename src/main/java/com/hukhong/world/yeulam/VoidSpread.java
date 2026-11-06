package com.hukhong.world.yeulam;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hư Không lan rộng: mỗi {@link #SPREAD_INTERVAL} tick game, vùng Đại Sâm Lâm đang bị xâm thực (tĩnh hoặc đã lan) có
 * {@link #SPREAD_CHANCE} để nhiễm MỘT ô lưới láng giềng. Chỉ xét các ô gần người chơi; tối đa {@link #MAX_SPREAD} vùng lan.
 * Giải phóng (Lịch Luyện) làm vùng đó sạch: {@link DaiSamLam#corruptedLive} trả false.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class VoidSpread extends SavedData {
    private static final String NAME = "hukhong_void_spread";

    public static final int SPREAD_INTERVAL = 72000, MAX_SPREAD = 12, NEAR_CELLS = 4, CITY_WARN_DIST = 600;
    public static final double SPREAD_CHANCE = 0.5;
    /** Cỏ gần người chơi trong vùng bị xâm thực dần hóa Podzol: cứ 20 tick, mỗi người chơi có BLIGHT_CHANCE (tối đa ~1 khối/giây/người). */
    private static final int BLIGHT_PERIOD = 20, BLIGHT_RANGE = 32;
    private static final double BLIGHT_CHANCE = 0.25;

    public record Entry(DaiSamLam.Region region, long time) {}

    private final Map<Long, Entry> infected = new HashMap<>();
    /** Mốc game-time của bước lan gần nhất; -1 = chưa khởi tạo. */
    private long lastStep = -1;

    public static VoidSpread get(ServerLevel ow) {
        return ow.getDataStorage().computeIfAbsent(new SavedData.Factory<>(VoidSpread::new, VoidSpread::load, null), NAME);
    }

    public boolean has(DaiSamLam.Region r) { return r != null && infected.containsKey(CleansedForests.key(r)); }

    public int count() { return infected.size(); }

    public List<Entry> entries() { return new ArrayList<>(infected.values()); }

    public void add(DaiSamLam.Region r, long time) {
        if (r != null && !infected.containsKey(CleansedForests.key(r))) {
            infected.put(CleansedForests.key(r), new Entry(r, time));
            setDirty();
        }
    }

    public void remove(DaiSamLam.Region r) {
        if (r != null && infected.remove(CleansedForests.key(r)) != null) setDirty();
    }

    public static VoidSpread load(CompoundTag tag, HolderLookup.Provider reg) {
        VoidSpread d = new VoidSpread();
        if (tag.contains("last")) d.lastStep = tag.getLong("last");
        ListTag list = tag.getList("inf", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            d.add(new DaiSamLam.Region(c.getInt("cx"), c.getInt("cz"), c.getInt("r")), c.getLong("t"));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (Entry e : infected.values()) {
            CompoundTag c = new CompoundTag();
            c.putInt("cx", e.region().cx());
            c.putInt("cz", e.region().cz());
            c.putInt("r", e.region().r());
            c.putLong("t", e.time());
            list.add(c);
        }
        tag.put("inf", list);
        if (lastStep >= 0) tag.putLong("last", lastStep);
        return tag;
    }

    private record Src(int gx, int gz, DaiSamLam.Region r) {}

    /** Một bước lan quanh người chơi đang online, chance mặc định. Trả các vùng vừa bị nhiễm. */
    public static List<DaiSamLam.Region> spreadOnce(ServerLevel ow, long gameTime) {
        List<int[]> centers = new ArrayList<>();
        for (ServerPlayer p : ow.players()) {
            int gx = Math.floorDiv((int) Math.floor(p.getX()), DaiSamLam.CELL), gz = Math.floorDiv((int) Math.floor(p.getZ()), DaiSamLam.CELL);
            centers.add(new int[]{gx, gz});
        }
        return spreadFrom(ow, gameTime, SPREAD_CHANCE, centers);
    }

    /** Lõi lan: xét các ô trong NEAR_CELLS quanh mỗi tâm ô trong centers. chance = 1 dùng cho test. */
    public static List<DaiSamLam.Region> spreadFrom(ServerLevel ow, long gameTime, double chance, List<int[]> centers) {
        long seed = ow.getSeed();
        VoidSpread vs = get(ow);
        RandomSource rnd = RandomSource.create(seed ^ gameTime);
        Set<Long> seen = new LinkedHashSet<>();
        List<Src> sources = new ArrayList<>();
        for (int[] c : centers)
            for (int i = -NEAR_CELLS; i <= NEAR_CELLS; i++)
                for (int j = -NEAR_CELLS; j <= NEAR_CELLS; j++) {
                    int gx = c[0] + i, gz = c[1] + j;
                    if (!seen.add(((long) gx << 32) ^ (gz & 0xFFFFFFFFL))) continue;
                    DaiSamLam.Region r = DaiSamLam.cell(seed, gx, gz);
                    if (r != null && DaiSamLam.corruptedLive(ow, r)) sources.add(new Src(gx, gz, r));
                }
        List<DaiSamLam.Region> fresh = new ArrayList<>();
        for (Src s : sources) {
            if (vs.count() >= MAX_SPREAD) break;
            if (rnd.nextDouble() >= chance) continue;
            List<DaiSamLam.Region> cand = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    DaiSamLam.Region n = DaiSamLam.cell(seed, s.gx() + dx, s.gz() + dz);
                    // loại: không có rừng, đã bị xâm thực (tĩnh/lan), đã giải phóng
                    if (n != null && !DaiSamLam.corruptedLive(ow, n)) cand.add(n);
                }
            if (cand.isEmpty()) continue;
            DaiSamLam.Region pick = cand.get(rnd.nextInt(cand.size()));
            vs.add(pick, gameTime);
            fresh.add(pick);
        }
        return fresh;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ServerLevel ow = server.overworld();
        long t = ow.getGameTime();
        if (t % BLIGHT_PERIOD != 0 || ow.players().isEmpty()) return;
        VoidSpread vs = get(ow);
        if (vs.lastStep < 0) { vs.lastStep = t; vs.setDirty(); }
        if (t - vs.lastStep >= SPREAD_INTERVAL) {
            vs.lastStep = t;
            vs.setDirty();
            for (DaiSamLam.Region r : spreadOnce(ow, t)) announce(server, ow, r);
        }
        blight(ow);
    }

    private static void announce(MinecraftServer server, ServerLevel ow, DaiSamLam.Region r) {
        CityDef city = CityService.nearest(ow, r.cx(), r.cz(), CITY_WARN_DIST);
        Component msg = city != null
                ? Component.translatable("hukhong.void.spread_city", Component.translatable(city.nameKey()), r.cx(), r.cz())
                : Component.translatable("hukhong.void.spread", r.cx(), r.cz());
        server.getPlayerList().broadcastSystemMessage(msg.copy().withColor(0xB060FF), false);
    }

    private static void blight(ServerLevel ow) {
        long seed = ow.getSeed();
        RandomSource rnd = ow.random;
        for (ServerPlayer p : ow.players()) {
            if (rnd.nextDouble() >= BLIGHT_CHANCE) continue;
            int x = (int) Math.floor(p.getX()) + rnd.nextInt(2 * BLIGHT_RANGE + 1) - BLIGHT_RANGE;
            int z = (int) Math.floor(p.getZ()) + rnd.nextInt(2 * BLIGHT_RANGE + 1) - BLIGHT_RANGE;
            if (!ow.hasChunkAt(new BlockPos(x, 64, z))) continue;
            DaiSamLam.Region r = DaiSamLam.at(seed, x, z);
            if (r == null || !DaiSamLam.corruptedLive(ow, r)) continue;
            int top = ow.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            for (int y = top - 1; y >= top - 2; y--) {
                BlockPos bp = new BlockPos(x, y, z);
                if (ow.getBlockState(bp).is(Blocks.GRASS_BLOCK)) { ow.setBlockAndUpdate(bp, Blocks.PODZOL.defaultBlockState()); break; }
            }
        }
    }
}
