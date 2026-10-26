package com.hukhong.society;

import com.hukhong.core.CultivationService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đại Bỉ: đấu trường PvE tính giờ ngay sân sơn môn. Hạ đủ số quái trong thời hạn, không chết, không rời sân
 * thì daiBi +1 (mở khóa Chân Truyền / Trưởng Lão, xem SectMath.rankFor). Trạng thái chỉ giữ trong RAM (mất khi tắt server = thất bại).
 */
public final class SectTrial {
    private SectTrial() {}

    public static final String MOB_TAG = "hukhong_daibi";
    private static final int ARENA_RADIUS = 14;
    private static final int MAX_ALIVE = 5;

    private static final class Trial {
        final String sect;
        final BlockPos center;
        final int need;
        final long endTick;
        int kills, spawned;
        final Set<UUID> mobs = new HashSet<>();

        Trial(String sect, BlockPos center, int need, long endTick) {
            this.sect = sect; this.center = center; this.need = need; this.endTick = endTick;
        }
    }

    private static final Map<UUID, Trial> ACTIVE = new ConcurrentHashMap<>();

    public static boolean active(ServerPlayer p) { return ACTIVE.containsKey(p.getUUID()); }

    public static boolean isTrialMob(Entity e) { return e.getTags().contains(MOB_TAG); }

    /** Bắt đầu Đại Bỉ tại sơn môn của người chơi. */
    public static boolean start(ServerPlayer sp, ServerLevel ow, SectData d, SectDef def) {
        if (active(sp)) return false;
        var target = SectMath.trialTarget(def.tier(), CultivationService.get(sp).level(), d.totalCh(), d.daiBi());
        BlockPos c = SectSavedData.get(ow).center(def.id());
        if (target == null || c == null) return false;
        if (sp.distanceToSqr(c.getX() + .5, c.getY(), c.getZ() + .5) > ARENA_RADIUS * ARENA_RADIUS) return false;
        int need = SectMath.trialKills(def.tier(), d.daiBi());
        ACTIVE.put(sp.getUUID(), new Trial(def.id(), c, need, ow.getGameTime() + SectMath.trialTicks(d.daiBi())));
        sp.sendSystemMessage(Component.translatable("hukhong.sect.trial.start", need, SectMath.trialTicks(d.daiBi()) / 20));
        return true;
    }

    /** Gọi mỗi 20 tick từ SectEvents. */
    public static void tick(MinecraftServer server) {
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null || ACTIVE.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Trial>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Trial t = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            String why = null;
            if (p == null || !p.isAlive()) why = "died";
            else if (p.level() != ow || p.distanceToSqr(t.center.getX() + .5, t.center.getY(), t.center.getZ() + .5) > (ARENA_RADIUS + 4) * (ARENA_RADIUS + 4)) why = "left";
            else if (ow.getGameTime() >= t.endTick) why = "time";
            if (why != null) { it.remove(); finish(server, ow, p, t, false, why); continue; }
            t.mobs.removeIf(id -> ow.getEntity(id) == null);
            while (t.mobs.size() < MAX_ALIVE && t.spawned < t.need) spawn(ow, p, t);
            p.displayClientMessage(Component.translatable("hukhong.sect.trial.hud", t.kills, t.need, (t.endTick - ow.getGameTime()) / 20), true);
        }
    }

    private static void spawn(ServerLevel ow, ServerPlayer p, Trial t) {
        var type = t.spawned % 3 == 2 ? EntityType.SKELETON : EntityType.ZOMBIE;
        double a = p.getRandom().nextDouble() * Math.PI * 2, r = 6 + p.getRandom().nextInt(6);
        Mob m = type.create(ow);
        if (m == null) { t.spawned++; return; }
        m.moveTo(t.center.getX() + .5 + Math.cos(a) * r, t.center.getY() + 1, t.center.getZ() + .5 + Math.sin(a) * r, p.getRandom().nextFloat() * 360, 0);
        m.addTag(MOB_TAG);
        m.setPersistenceRequired();
        m.finalizeSpawn(ow, ow.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
        m.setTarget(p);
        ow.addFreshEntity(m);
        t.mobs.add(m.getUUID());
        t.spawned++;
    }

    /** Một quái Đại Bỉ chết. */
    public static void onMobDeath(Entity mob) {
        if (!isTrialMob(mob) || !(mob.level() instanceof ServerLevel ow)) return;
        for (var e : ACTIVE.entrySet()) {
            Trial t = e.getValue();
            if (!t.mobs.remove(mob.getUUID())) continue;
            t.kills++;
            if (t.kills >= t.need) {
                ACTIVE.remove(e.getKey());
                finish(ow.getServer(), ow, ow.getServer().getPlayerList().getPlayer(e.getKey()), t, true, "");
            }
            return;
        }
    }

    public static void onPlayerGone(ServerPlayer p) {
        Trial t = ACTIVE.remove(p.getUUID());
        if (t != null && p.level() instanceof ServerLevel ow) finish(p.server, ow, p, t, false, "left");
    }

    private static void finish(MinecraftServer server, ServerLevel ow, ServerPlayer p, Trial t, boolean win, String why) {
        for (UUID id : t.mobs) {
            Entity m = ow.getEntity(id);
            if (m != null) m.discard();
        }
        t.mobs.clear();
        if (p == null) return;
        if (!win) { p.sendSystemMessage(Component.translatable("hukhong.sect.trial.fail." + why)); return; }
        SectData d = SectService.get(p);
        if (!d.sect().equals(t.sect)) return;
        p.setData(SocietyRegistry.SECT, d.withDaiBi(d.daiBi() + 1));
        SectService.afterRankChange(p, d);
        p.sendSystemMessage(Component.translatable("hukhong.sect.trial.win"));
    }
}
