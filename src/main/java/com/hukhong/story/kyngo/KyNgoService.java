package com.hukhong.story.kyngo;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kỳ Ngộ: mỗi 10 giây mỗi người chơi được roll một lần khi đang lang thang ngoài hoang dã Thượng Giới.
 * Tỉ lệ: 2,5% mỗi lần roll (trung bình ~40 lần ~ 6,7 phút sau khi hết hồi), cộng hồi 15-20 phút
 * -> trung bình khoảng 1 kỳ ngộ / 25-30 phút đi đường (thực tế thấp hơn vì bị chặn bởi quái, thành, nước).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class KyNgoService {
    private KyNgoService() {}

    public enum Result { OK, NO_SUCH, NOT_OWNER, TOO_FAR, BAD_CHOICE, ALREADY_DONE, NO_PRICE }

    public static final int CHECK_INTERVAL = 200;          // 10 giây
    public static final double TRIGGER_CHANCE = 0.025;
    public static final long LIFETIME_TICKS = 5 * 60 * 20; // 5 phút
    public static final int MIN_DIST = 15, MAX_DIST = 25;
    public static final int CITY_CLEAR = 150;
    public static final int HOSTILE_RANGE = 16;
    public static final double CMD_RANGE = 24;
    static final String KEY_LAST = "hukhong_kyngo_last";
    static final long COOLDOWN_MIN_MS = 15 * 60_000L;
    static final long COOLDOWN_JITTER_S = 300L;

    /** Kỳ ngộ đang mở theo token (chỉ server). */
    static final Map<String, KyNgoInstance> BY_TOKEN = new ConcurrentHashMap<>();

    public static KyNgoInstance byToken(String token) { return BY_TOKEN.get(token); }

    // ---- vòng tick ----

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        long now = server.getTickCount();
        if (!BY_TOKEN.isEmpty()) {
            for (KyNgoInstance i : List.copyOf(BY_TOKEN.values())) {
                if (now - i.startTick >= LIFETIME_TICKS) finish(i);
                else if (!i.resolved) KyNgoEncounters.tick(i);
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (((now + p.getId()) % CHECK_INTERVAL) != 0) continue;
            try {
                tryTrigger(p);
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().warn("kyngo trigger failed for {}", p.getGameProfile().getName(), ex);
            }
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        BY_TOKEN.clear();
    }

    static long cooldownMs(long last) {
        return COOLDOWN_MIN_MS + Math.floorMod(last / 1000L, COOLDOWN_JITTER_S) * 1000L;
    }

    static void tryTrigger(ServerPlayer p) {
        if (p instanceof FakePlayer || !p.isAlive() || p.isSpectator() || p.isCreative()) return;
        if (!(p.level() instanceof ServerLevel ow) || ow.dimension() != Level.OVERWORLD) return;
        for (KyNgoInstance i : BY_TOKEN.values()) if (i.owner.getUUID().equals(p.getUUID())) return;
        long now = System.currentTimeMillis();
        long last = p.getPersistentData().getLong(KEY_LAST);
        if (last > 0 && now - last < cooldownMs(last)) return;
        if (p.getRandom().nextDouble() >= TRIGGER_CHANCE) return;
        if (!onSurface(ow, p)) return;
        if (!ow.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(HOSTILE_RANGE)).isEmpty()) return;
        if (nearCity(ow, p)) return;
        startRandom(ow, p);
    }

    /** Ngoài trời, không trong nước/dung nham, đứng trên mặt đất (không trong hang). */
    static boolean onSurface(ServerLevel ow, ServerPlayer p) {
        if (p.isInWater() || p.isUnderWater() || p.isInLava()) return false;
        BlockPos at = p.blockPosition();
        if (!ow.canSeeSky(at.above())) return false;
        int surf = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        if (Math.abs(p.getY() - surf) > 3) return false;
        return ow.getFluidState(new BlockPos(at.getX(), surf - 1, at.getZ())).isEmpty();
    }

    /** Trong vòng 150 block (XZ) của bất kỳ thành nào (đã dựng hoặc vị trí dự kiến). */
    static boolean nearCity(ServerLevel ow, ServerPlayer p) {
        for (CityDef d : CityDef.ALL) {
            BlockPos c = CityService.site(ow, d);
            double dx = c.getX() - p.getX(), dz = c.getZ() - p.getZ();
            if (dx * dx + dz * dz <= (double) CITY_CLEAR * CITY_CLEAR) return true;
        }
        return false;
    }

    static boolean startRandom(ServerLevel ow, ServerPlayer p) {
        RandomSource r = p.getRandom();
        KyNgoType t = KyNgoType.values()[r.nextInt(KyNgoType.values().length)];
        for (int k = 0; k < 8; k++) {
            double ang = r.nextDouble() * Math.PI * 2;
            double d = MIN_DIST + r.nextDouble() * (MAX_DIST - MIN_DIST);
            int x = Mth.floor(p.getX() + Math.cos(ang) * d), z = Mth.floor(p.getZ() + Math.sin(ang) * d);
            if (!ow.isLoaded(new BlockPos(x, 64, z))) continue;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos o = new BlockPos(x, y, z);
            BlockPos ground = o.below();
            if (!ow.getBlockState(o).isAir() || !ow.getFluidState(o).isEmpty()) continue;
            if (!ow.getFluidState(ground).isEmpty() || !ow.getBlockState(ground).isSolidRender(ow, ground)) continue;
            startAt(p, t, o);
            return true;
        }
        return false;
    }

    // ---- vòng đời ----

    /** Mở một kỳ ngộ tại vị trí cho trước (không ngẫu nhiên về vị trí; dùng cho test). */
    public static KyNgoInstance startAt(ServerPlayer p, KyNgoType t, BlockPos origin) {
        ServerLevel sl = p.serverLevel();
        RandomSource r = p.getRandom();
        String token;
        do { token = String.format("%06x", r.nextInt(0x1000000)); } while (BY_TOKEN.containsKey(token));
        KyNgoInstance i = new KyNgoInstance(token, t, p, sl, origin.immutable(), sl.getServer().getTickCount());
        BY_TOKEN.put(token, i);
        p.getPersistentData().putLong(KEY_LAST, System.currentTimeMillis());
        KyNgoEncounters.spawn(i);
        KyNgoEncounters.intro(i);
        return i;
    }

    /** Kết thúc: dọn thực thể + khối, bỏ khỏi bảng. */
    public static void finish(KyNgoInstance i) {
        i.resolved = true;
        BY_TOKEN.remove(i.token);
        KyNgoEncounters.cleanup(i);
    }

    /** Xử lý lệnh /hkkyngo &lt;token&gt; &lt;lựa chọn&gt;: kiểm tra token, chủ nhân, khoảng cách rồi áp dụng lựa chọn. */
    public static Result resolve(ServerPlayer p, String token, String choice) {
        KyNgoInstance i = BY_TOKEN.get(token);
        if (i == null) return Result.NO_SUCH;
        if (!i.owner.getUUID().equals(p.getUUID())) return Result.NOT_OWNER;
        double dx = p.getX() - (i.origin.getX() + 0.5), dy = p.getY() - i.origin.getY(), dz = p.getZ() - (i.origin.getZ() + 0.5);
        if (p.level() != i.level || dx * dx + dy * dy + dz * dz > CMD_RANGE * CMD_RANGE) return Result.TOO_FAR;
        if (i.resolved) return Result.ALREADY_DONE;
        if (!i.type.choices().contains(choice)) return Result.BAD_CHOICE;
        return KyNgoEncounters.choose(i, p, choice);
    }
}
