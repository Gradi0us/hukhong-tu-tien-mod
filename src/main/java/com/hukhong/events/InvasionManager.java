package com.hukhong.events;

import com.hukhong.HuKhong;
import com.hukhong.city.CityBuilder;
import com.hukhong.city.CitySavedData;
import com.hukhong.entity.ModEntities;
import com.hukhong.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Tập kích thử: 3 đợt quái Hư Hóa quanh tường, bảo vệ Trận Nhãn (Ấn Hồn Đài).
 * Ngân sách: tổng 36 quái (cap sống ≤ 40), tối đa 2 spawn / 5 tick, tối đa 60 lần thử vị trí, timeout 10 phút.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class InvasionManager {
    private InvasionManager() {}

    private static final int[] WAVE_SIZE = {8, 12, 16};
    private static final int MAX_ALIVE = 40;
    private static final int TIMEOUT_TICKS = 12000;
    private static final int LOSE_TICKS = 400;   // quái ≥2 con gần đài liên tục ~20s
    private static final int NEAR_ALTAR_SQ = 7 * 7;
    private static final int MAX_SPAWN_TRIES = 60;

    private static final class Invasion {
        final ServerLevel level;
        final BlockPos center;
        final BlockPos altar;
        final ServerBossEvent bar = new ServerBossEvent(Component.translatable("hukhong.invade.bossbar", 1, WAVE_SIZE.length),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        final List<Mob> mobs = new ArrayList<>();
        int wave = 1, toSpawn = WAVE_SIZE[0], killedTotal, plannedTotal = WAVE_SIZE[0] + WAVE_SIZE[1] + WAVE_SIZE[2];
        int age, lossTicks, waveDelay, tries;

        Invasion(ServerLevel l, CitySavedData d) { level = l; center = d.center(); altar = d.altarPos(); }
    }

    private static Invasion cur;

    public static boolean active() { return cur != null; }

    /** @return null nếu OK, hoặc translation key lỗi. */
    public static String start(ServerLevel ow) {
        if (cur != null) return "hukhong.invade.err_active";
        CitySavedData d = CitySavedData.get(ow);
        if (!d.isBuilt()) return "hukhong.city.not_built";
        if (d.state() == CitySavedData.State.RUIN) return "hukhong.invade.err_ruin";
        cur = new Invasion(ow, d);
        title(ow, Component.translatable("hukhong.invade.title"), Component.translatable("hukhong.invade.subtitle"));
        return null;
    }

    public static void cancel() { if (cur != null) end(null); }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        if (cur != null) { cur.bar.removeAllPlayers(); cur = null; }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        Invasion inv = cur;
        if (inv == null) return;
        inv.age++;
        if (inv.level.players().isEmpty() || inv.age > TIMEOUT_TICKS) { end(Component.translatable("hukhong.invade.timeout")); return; }

        if (inv.toSpawn > 0 && inv.waveDelay <= 0 && inv.age % 5 == 0) spawnBatch(inv);
        if (inv.waveDelay > 0) inv.waveDelay--;
        if (inv.age % 20 != 0) return;

        // dọn xác + cập nhật thanh
        int before = inv.mobs.size();
        inv.mobs.removeIf(m -> m.isRemoved() || !m.isAlive());
        inv.killedTotal += before - inv.mobs.size();
        inv.bar.setName(Component.translatable("hukhong.invade.bossbar", inv.wave, WAVE_SIZE.length));
        inv.bar.setProgress(Math.min(1f, (float) inv.killedTotal / inv.plannedTotal));
        for (ServerPlayer p : inv.level.players()) {
            if (p.distanceToSqr(inv.altar.getX(), inv.altar.getY(), inv.altar.getZ()) < 160 * 160) inv.bar.addPlayer(p);
            else inv.bar.removePlayer(p);
        }

        // quái chưa có mục tiêu → tiến về Trận Nhãn; đếm quái sát đài
        int near = 0;
        for (Mob m : inv.mobs) {
            if (m.getTarget() == null) m.getNavigation().moveTo(inv.altar.getX() + 0.5, inv.altar.getY(), inv.altar.getZ() + 0.5, 1.0);
            if (m.distanceToSqr(inv.altar.getX() + 0.5, inv.altar.getY(), inv.altar.getZ() + 0.5) < NEAR_ALTAR_SQ) near++;
        }
        inv.lossTicks = near >= 2 ? inv.lossTicks + 20 : Math.max(0, inv.lossTicks - 20);
        if (inv.lossTicks >= LOSE_TICKS) { lose(inv); return; }

        // hết đợt?
        if (inv.toSpawn == 0 && inv.mobs.isEmpty() && inv.waveDelay <= 0) {
            if (inv.wave >= WAVE_SIZE.length) { win(inv); return; }
            inv.wave++;
            inv.toSpawn = WAVE_SIZE[inv.wave - 1];
            inv.waveDelay = 100;
            broadcast(inv, Component.translatable("hukhong.invade.wave", inv.wave, WAVE_SIZE.length));
        }
    }

    private static void spawnBatch(Invasion inv) {
        int spawned = 0;
        for (int attempt = 0; attempt < 4 && spawned < 2 && inv.toSpawn > 0; attempt++) {
            if (inv.mobs.size() >= MAX_ALIVE) return;
            if (++inv.tries > MAX_SPAWN_TRIES + inv.killedTotal + inv.plannedTotal) { // hết ngân sách thử: bỏ phần còn lại
                inv.plannedTotal -= inv.toSpawn;
                inv.toSpawn = 0;
                return;
            }
            double ang = inv.level.random.nextDouble() * Math.PI * 2;
            double r = 26 + inv.level.random.nextDouble() * 8;
            int x = inv.center.getX() + (int) (Math.cos(ang) * r), z = inv.center.getZ() + (int) (Math.sin(ang) * r);
            if (!inv.level.hasChunk(x >> 4, z >> 4)) continue;
            int y = inv.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos bp = new BlockPos(x, y, z);
            if (!inv.level.getFluidState(bp.below()).isEmpty()) continue;
            Mob m = switch (inv.level.random.nextInt(3)) {
                case 0 -> ModEntities.HU_THI.get().create(inv.level);
                case 1 -> ModEntities.HU_COT.get().create(inv.level);
                default -> ModEntities.HU_CHU.get().create(inv.level);
            };
            if (m == null) continue;
            m.moveTo(x + 0.5, y, z + 0.5, inv.level.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(inv.level, inv.level.getCurrentDifficultyAt(bp), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            inv.level.addFreshEntityWithPassengers(m);
            inv.mobs.add(m);
            inv.toSpawn--;
            spawned++;
        }
    }

    private static void win(Invasion inv) {
        CitySavedData.get(inv.level).addWin();
        for (ServerPlayer p : inv.level.players()) {
            if (p.distanceToSqr(inv.altar.getX(), inv.altar.getY(), inv.altar.getZ()) > 160 * 160) continue;
            ItemStack st = new ItemStack(ModItems.HON_TINH.get(), 5);
            if (!p.getInventory().add(st)) p.drop(st, false);
        }
        title(inv.level, Component.translatable("hukhong.invade.win"), Component.translatable("hukhong.invade.win_sub"));
        end(null);
    }

    private static void lose(Invasion inv) {
        CitySavedData d = CitySavedData.get(inv.level);
        d.setState(CitySavedData.State.RUIN);
        CityBuilder.ruin(inv.level, inv.center);
        title(inv.level, Component.translatable("hukhong.invade.lose"), Component.translatable("hukhong.invade.lose_sub"));
        end(null);
    }

    private static void end(Component msg) {
        Invasion inv = cur;
        cur = null;
        if (inv == null) return;
        inv.bar.removeAllPlayers();
        inv.bar.setVisible(false);
        for (Mob m : inv.mobs) if (!m.isRemoved()) m.discard();
        if (msg != null) inv.level.getServer().getPlayerList().broadcastSystemMessage(msg, false);
    }

    private static void broadcast(Invasion inv, Component c) {
        for (ServerPlayer p : inv.level.players()) p.displayClientMessage(c, true);
    }

    private static void title(ServerLevel l, Component t, Component sub) {
        for (ServerPlayer p : l.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(t));
        }
    }
}
