package com.hukhong.world.calendar;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.core.CultivationService;
import com.hukhong.currency.Wallet;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.BossScaling;
import com.hukhong.events.CityStates;
import com.hukhong.events.CorruptionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thu Trieu (Yeu Toc): dot thu cuong (HuThi / HuCot / HuChu "nhap cuong": mat do, don manh hon, chay nhanh hon) tien vao mot thanh.
 * Don sach -> thuong Hon Tinh + EXP va gui manh moi Tam Hu Hoa gan nhat. Thanh dang RUIN / chua dung thi bo qua.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ThuTrieuHandler {
    private ThuTrieuHandler() {}

    public static final int BASE_BEASTS = 16, MAX_ALIVE = 18, REWARD_RADIUS = 96;
    public static final String TAG = "hk_enraged";
    private static final ResourceLocation RAGE = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "thu_trieu_rage");
    private static final DustParticleOptions RED_EYE = new DustParticleOptions(new Vector3f(1f, 0.05f, 0.05f), 0.8f);

    private static final class Wave {
        final ServerLevel level;
        final BlockPos center;
        final List<Mob> mobs = new ArrayList<>();
        int toSpawn, total, killed, tries;
        Wave(ServerLevel l, BlockPos c, int total) { level = l; center = c; this.total = toSpawn = total; }
    }

    private static final Map<String, Wave> WAVES = new HashMap<>();

    /** Thuan: he so don/toc/mau cua thu nhap cuong. */
    public static double rageDamage() { return 0.5; }
    public static double rageSpeed() { return 0.25; }
    public static double rageHealth() { return 0.3; }

    public static void begin(ServerLevel ow, CalendarData.Act a) {
        if (a.city() < 0) return;
        CityDef d = CityDef.ALL.get(a.city());
        BlockPos c = CityService.builtCenter(ow, d);
        if (c == null || CityStates.isRuin(ow, d)) return;
        int n = 0;
        for (ServerPlayer p : ow.players()) if (p.distanceToSqr(c.getX(), c.getY(), c.getZ()) < 200 * 200) n++;
        WAVES.put(a.key(), new Wave(ow, c, BossScaling.waveSize(BASE_BEASTS, n)));
    }

    public static void end(ServerLevel ow, CalendarData.Act a) {
        Wave w = WAVES.remove(a.key());
        if (w != null) for (Mob m : w.mobs) if (!m.isRemoved()) m.discard();
    }

    /** Bien thu thuong thanh thu nhap cuong (tag + do thuoc tinh). */
    public static void enrage(Mob m) {
        m.addTag(TAG);
        var dmg = m.getAttribute(Attributes.ATTACK_DAMAGE);
        var spd = m.getAttribute(Attributes.MOVEMENT_SPEED);
        var hp = m.getAttribute(Attributes.MAX_HEALTH);
        if (dmg != null && !dmg.hasModifier(RAGE)) dmg.addPermanentModifier(new AttributeModifier(RAGE, rageDamage(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (spd != null && !spd.hasModifier(RAGE)) spd.addPermanentModifier(new AttributeModifier(RAGE, rageSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (hp != null && !hp.hasModifier(RAGE)) hp.addPermanentModifier(new AttributeModifier(RAGE, rageHealth(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        m.setHealth(m.getMaxHealth());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) { WAVES.clear(); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (WAVES.isEmpty()) return;
        ServerLevel ow = e.getServer().overworld();
        if (ow == null) return;
        long t = ow.getGameTime();
        if (t % 5 != 0) return;
        for (var it = WAVES.entrySet().iterator(); it.hasNext(); ) {
            Wave w = it.next().getValue();
            int before = w.mobs.size();
            w.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
            w.killed += before - w.mobs.size();
            if (w.toSpawn > 0 && w.mobs.size() < MAX_ALIVE) spawn(w);
            for (Mob m : w.mobs) {
                if (t % 10 == 0) {
                    double ey = m.getEyeY(), yaw = Math.toRadians(m.getYRot()), fx = -Math.sin(yaw) * 0.25, fz = Math.cos(yaw) * 0.25;
                    w.level.sendParticles(RED_EYE, m.getX() + fx + Math.cos(yaw) * 0.12, ey, m.getZ() + fz + Math.sin(yaw) * 0.12, 1, 0, 0, 0, 0);
                    w.level.sendParticles(RED_EYE, m.getX() + fx - Math.cos(yaw) * 0.12, ey, m.getZ() + fz - Math.sin(yaw) * 0.12, 1, 0, 0, 0, 0);
                }
                if (m.getTarget() == null) m.getNavigation().moveTo(w.center.getX() + 0.5, w.center.getY(), w.center.getZ() + 0.5, 1.1);
            }
            if (w.toSpawn == 0 && w.mobs.isEmpty()) { cleared(w); it.remove(); }
        }
    }

    private static void spawn(Wave w) {
        if (++w.tries > w.total * 8) { w.total -= w.toSpawn; w.toSpawn = 0; return; }
        double ang = w.level.random.nextDouble() * Math.PI * 2, rad = 28 + w.level.random.nextDouble() * 8;
        int x = w.center.getX() + (int) (Math.cos(ang) * rad), z = w.center.getZ() + (int) (Math.sin(ang) * rad);
        if (!w.level.hasChunk(x >> 4, z >> 4)) return;
        int y = w.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos bp = new BlockPos(x, y, z);
        if (!w.level.getFluidState(bp.below()).isEmpty()) return;
        Mob m = switch (w.level.random.nextInt(3)) {
            case 0 -> ModEntities.HU_THI.get().create(w.level);
            case 1 -> ModEntities.HU_COT.get().create(w.level);
            default -> ModEntities.HU_CHU.get().create(w.level);
        };
        if (m == null) return;
        m.moveTo(x + 0.5, y, z + 0.5, w.level.random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(w.level, w.level.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
        m.setPersistenceRequired();
        enrage(m);
        w.level.addFreshEntityWithPassengers(m);
        w.mobs.add(m);
        w.toSpawn--;
    }

    private static void cleared(Wave w) {
        BlockPos heart = null;
        double best = Double.MAX_VALUE;
        for (BlockPos h : CorruptionData.get(w.level).hearts()) {
            double d = h.distSqr(w.center);
            if (d < best) { best = d; heart = h; }
        }
        for (ServerPlayer p : w.level.players()) {
            if (p.distanceToSqr(w.center.getX(), w.center.getY(), w.center.getZ()) > (double) REWARD_RADIUS * REWARD_RADIUS) continue;
            Wallet.grantHonTinh(p, 3);
            CultivationService.addExp(p, Math.max(1, CultivationService.expToNext(CultivationService.get(p).level()) / 10));
            p.sendSystemMessage(Component.translatable("hukhong.cal.thu_trieu.cleared"));
            if (heart != null) p.sendSystemMessage(Component.translatable("hukhong.cal.thu_trieu.clue", heart.getX(), heart.getZ()));
        }
    }
}
