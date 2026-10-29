package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CitySavedData;
import com.hukhong.city.CityService;
import com.hukhong.entity.ModEntities;
import com.hukhong.story.objective.Objectives;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhiem vu "Tai chiem thanh" (Dot 5): thanh RUIN -> don quai Hu Hoa quanh thanh, roi giu Tam thanh (ban kinh 8 block) du thoi gian
 * de thanh tay Tam Hu -> trang thai tro lai NORMAL. Tu khoi dong khi nguoi choi den gan thanh co the tai chiem (CityStates.retakable).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CityRetake {
    private CityRetake() {}

    public static final int BASE_MOBS = 14, MAX_ALIVE = 14, PURIFY_TICKS = 200, PURIFY_RADIUS = 8, TIMEOUT = 18000,
            START_RADIUS = 100, NEAR = 160, RETRY_COOLDOWN = 3000;
    public static final String OBJECTIVE_ID = "retake";

    private enum Phase { KILL, PURIFY }

    private static final class Run {
        final ServerLevel level;
        final CityDef city;
        final BlockPos center;
        final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        final List<Mob> mobs = new ArrayList<>();
        Phase phase = Phase.KILL;
        int toSpawn, total, killed, age, purify, tries;

        Run(ServerLevel l, CityDef c, BlockPos ctr, int players) {
            level = l; city = c; center = ctr;
            total = toSpawn = BossScaling.waveSize(BASE_MOBS, players);
        }
    }

    private static Run cur;
    private static final Map<Integer, Long> COOLDOWN = new HashMap<>();

    public static boolean active() { return cur != null; }
    public static int activeCity() { return cur == null ? -1 : cur.city.index(); }

    /** Khoi dong tai chiem thanh idx (neu dang RUIN, chua co run nao, het cooldown). @return true neu dang chay cho thanh do. */
    public static boolean ensure(ServerLevel ow, int idx) {
        if (cur != null) return cur.city.index() == idx;
        CityDef d = CityDef.ALL.get(idx);
        BlockPos c = CityService.builtCenter(ow, d);
        if (c == null || !CityStates.isRuin(ow, d) || InvasionManager.active() || InvasionManager.warning()) return false;
        if (COOLDOWN.getOrDefault(idx, 0L) > ow.getGameTime()) return false;
        int n = 0;
        for (ServerPlayer p : ow.players()) if (near(p, c, NEAR)) n++;
        cur = new Run(ow, d, c, BossScaling.clamp(n));
        for (ServerPlayer p : ow.players()) p.sendSystemMessage(Component.translatable("hukhong.retake.start", Component.translatable(d.nameKey())));
        return true;
    }

    private static boolean near(ServerPlayer p, BlockPos c, double r) {
        return p.isAlive() && !p.isSpectator() && p.level().dimension() == Level.OVERWORLD
                && p.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) < r * r;
    }

    /** Doi trang thai: dua thanh tro lai NORMAL (thanh goc: +1 dem tap kich de buoc truyen c5_invasion thoa). Khong thuong. */
    public static void markRetaken(ServerLevel ow, CityDef d) {
        CityStates.setRuin(ow, d, false);
        if (d.index() == 0) CitySavedData.get(ow).addWin();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        if (cur != null) { cur.bar.removeAllPlayers(); cur = null; }
        COOLDOWN.clear();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        ServerLevel ow = e.getServer().overworld();
        if (ow == null) return;
        long t = ow.getGameTime();
        if (cur == null) {
            if (t % 100 == 41) autoStart(ow);
            return;
        }
        Run r = cur;
        r.age++;
        if (r.age > TIMEOUT || ow.players().isEmpty()) { end(r, true); return; }
        if (InvasionManager.active()) return; // nhuong tap kich
        boolean anyNear = false;
        for (ServerPlayer p : ow.players()) if (near(p, r.center, NEAR)) { anyNear = true; break; }
        if (r.phase == Phase.KILL) tickKill(r, anyNear);
        else tickPurify(r);
        if (cur == r && r.age % 20 == 0) {
            for (ServerPlayer p : ow.players()) {
                if (near(p, r.center, NEAR)) {
                    r.bar.addPlayer(p);
                    if (Objectives.currentId(p) == null)
                        Objectives.set(p, OBJECTIVE_ID, Component.translatable("hukhong.retake.objective", Component.translatable(r.city.nameKey())), r.center, Level.OVERWORLD);
                } else r.bar.removePlayer(p);
            }
        }
    }

    private static void autoStart(ServerLevel ow) {
        for (CityDef d : CityDef.ALL) {
            BlockPos c = CityService.builtCenter(ow, d);
            if (c == null || !CityStates.isRuin(ow, d)) continue;
            int ch = -1;
            for (ServerPlayer p : ow.players()) if (near(p, c, START_RADIUS)) ch = Math.max(ch, StoryHooks.chapter(p));
            if (ch >= 0 && CityStates.retakable(ow, d, ch) && ensure(ow, d.index())) return;
        }
    }

    private static void tickKill(Run r, boolean anyNear) {
        int before = r.mobs.size();
        r.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
        r.killed += before - r.mobs.size();
        if (anyNear && r.toSpawn > 0 && r.age % 10 == 0 && r.mobs.size() < MAX_ALIVE) spawnOne(r);
        if (r.age % 20 == 0) {
            r.bar.setName(Component.translatable("hukhong.retake.bar.kill", Component.translatable(r.city.nameKey()), r.killed, r.total));
            r.bar.setProgress(Math.min(1f, r.killed / (float) Math.max(1, r.total)));
        }
        if (r.toSpawn == 0 && r.mobs.isEmpty()) {
            r.phase = Phase.PURIFY;
            r.bar.setColor(BossEvent.BossBarColor.WHITE);
            for (ServerPlayer p : r.level.players()) if (near(p, r.center, NEAR))
                p.displayClientMessage(Component.translatable("hukhong.retake.purify_hint", PURIFY_RADIUS), false);
        }
    }

    private static void spawnOne(Run r) {
        if (++r.tries > r.total * 6) { r.total -= r.toSpawn; r.toSpawn = 0; return; }
        double ang = r.level.random.nextDouble() * Math.PI * 2, rad = 12 + r.level.random.nextDouble() * 12;
        int x = r.center.getX() + (int) (Math.cos(ang) * rad), z = r.center.getZ() + (int) (Math.sin(ang) * rad);
        if (!r.level.hasChunk(x >> 4, z >> 4)) return;
        int y = r.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos bp = new BlockPos(x, y, z);
        if (!r.level.getFluidState(bp.below()).isEmpty()) return;
        Mob m = switch (r.level.random.nextInt(3)) {
            case 0 -> ModEntities.HU_THI.get().create(r.level);
            case 1 -> ModEntities.HU_COT.get().create(r.level);
            default -> ModEntities.HU_CHU.get().create(r.level);
        };
        if (m == null) return;
        m.moveTo(x + 0.5, y, z + 0.5, r.level.random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(r.level, r.level.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
        m.setPersistenceRequired();
        r.level.addFreshEntityWithPassengers(m);
        r.mobs.add(m);
        r.toSpawn--;
    }

    private static void tickPurify(Run r) {
        boolean holding = false;
        for (ServerPlayer p : r.level.players()) if (near(p, r.center, PURIFY_RADIUS)) { holding = true; break; }
        r.purify = holding ? r.purify + 1 : Math.max(0, r.purify - 2);
        if (r.age % 20 == 0) {
            r.bar.setName(Component.translatable("hukhong.retake.bar.purify", Component.translatable(r.city.nameKey())));
            r.bar.setProgress(Math.min(1f, r.purify / (float) PURIFY_TICKS));
        }
        if (holding && r.age % 10 == 0)
            r.level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, r.center.getX() + 0.5, r.center.getY() + 1.2, r.center.getZ() + 0.5, 6, 0.6, 0.8, 0.6, 0.02);
        if (r.purify >= PURIFY_TICKS) win(r);
    }

    private static void win(Run r) {
        markRetaken(r.level, r.city);
        com.hukhong.story.world.HuAnh.addNear(r.level, r.center, -10);
        CorruptionData.get(r.level).cleanseNear(r.center.getX(), r.center.getZ(), 20);
        for (ServerPlayer p : r.level.players()) {
            if (!near(p, r.center, NEAR)) continue;
            com.hukhong.currency.Wallet.grantHonTinh(p, 5);
            Objectives.complete(p, OBJECTIVE_ID);
            p.sendSystemMessage(Component.translatable("hukhong.retake.win", Component.translatable(r.city.nameKey())));
        }
        end(r, false);
    }

    private static void end(Run r, boolean failed) {
        if (cur == r) cur = null;
        r.bar.removeAllPlayers();
        for (Mob m : r.mobs) if (!m.isRemoved()) m.discard();
        if (failed) {
            COOLDOWN.put(r.city.index(), r.level.getGameTime() + RETRY_COOLDOWN);
            for (ServerPlayer p : r.level.players()) {
                if (OBJECTIVE_ID.equals(Objectives.currentId(p))) Objectives.clear(p);
                p.sendSystemMessage(Component.translatable("hukhong.retake.fail", Component.translatable(r.city.nameKey())));
            }
        }
    }
}
