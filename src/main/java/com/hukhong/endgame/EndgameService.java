package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuMob;
import com.hukhong.story.StoryService;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server Bước 25-26: mở cổng, vào/ra Hư Không Bản Nguyên, boss cuối, chọn kết thúc, cutscene, Tầng Thế Giới II. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class EndgameService {
    private EndgameService() {}

    public static final String F_PENDING = "ending_pending", F_SEAL = "ending_seal", F_ABSORB = "ending_absorb";
    /** uuid -> {ending, ticksLeft}. */
    private static final Map<UUID, int[]> CUTSCENE = new HashMap<>();

    public static boolean hasEnding(QuestData q) { return q.has(F_SEAL) || q.has(F_ABSORB); }

    public static boolean eligible(ServerPlayer p) {
        return EndgameKit.gatewayEligible(StoryService.get(p).lair(), CultivationService.get(p).level());
    }

    // ------------------------------------------------------------ cổng
    /** Dựng cổng ở Overworld tại {@code ground} và ghi vị trí. */
    public static void openGateway(ServerLevel ow, BlockPos ground) {
        EndgameArena.buildGateway(ow, ground);
        WorldTierData.get(ow).setGateway(ground);
    }

    private static void gatewayTick(ServerLevel ow) {
        WorldTierData data = WorldTierData.get(ow);
        if (data.gateway() != null) return;
        for (ServerPlayer p : ow.players()) {
            if (p.isSpectator() || !eligible(p)) continue;
            BlockPos xz = p.blockPosition().offset(10, 0, 0);
            BlockPos ground = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
            openGateway(ow, ground);
            for (ServerPlayer q : ow.players())
                q.displayClientMessage(Component.translatable("hukhong.endgame.gateway.open", ground.getX(), ground.getY(), ground.getZ()), false);
            return;
        }
    }

    // ------------------------------------------------------------ vào / ra
    public static void enter(ServerPlayer p) { enter(p, false); }

    public static void enter(ServerPlayer p, boolean force) {
        if (!force && !eligible(p)) {
            p.displayClientMessage(Component.translatable("hukhong.endgame.locked", EndgameKit.REQUIRED_LEVEL), true);
            return;
        }
        MinecraftServer server = p.getServer();
        ServerLevel dim = server.getLevel(EndgameRegistry.DIM);
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (dim == null || ow == null) {
            p.displayClientMessage(Component.translatable("hukhong.endgame.nodim"), false);
            return;
        }
        WorldTierData data = WorldTierData.get(ow);
        if (!data.arenaBuilt()) {
            EndgameArena.buildIslands(dim);
            data.setArenaBuilt();
        }
        p.teleportTo(dim, EndgameArena.ARRIVE.getX() + 0.5, EndgameArena.ARRIVE.getY(), EndgameArena.ARRIVE.getZ() + 0.5, 180f, 0f);
        p.displayClientMessage(Component.translatable("hukhong.endgame.enter"), false);
        QuestData q = DialogueService.quest(p);
        if (!hasEnding(q) && !q.has(F_PENDING) && bossAlive(dim) == null) spawnBoss(dim);
        else if (q.has(F_PENDING) && !hasEnding(q)) promptChoice(p);
    }

    public static void leave(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        BlockPos g = WorldTierData.get(ow).gateway();
        BlockPos at = g != null ? g.offset(0, 1, 2) : ow.getSharedSpawnPos();
        p.teleportTo(ow, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        p.displayClientMessage(Component.translatable("hukhong.endgame.leave"), true);
    }

    // ------------------------------------------------------------ boss
    public static NguyenThuyTichDiet bossAlive(ServerLevel dim) {
        var l = dim.getEntitiesOfClass(NguyenThuyTichDiet.class, new net.minecraft.world.phys.AABB(-80, 0, -80, 80, 255, 80), e -> e.isAlive());
        return l.isEmpty() ? null : l.get(0);
    }

    public static NguyenThuyTichDiet spawnBoss(ServerLevel sl) { return spawnBoss(sl, EndgameArena.BOSS_SPOT); }

    public static NguyenThuyTichDiet spawnBoss(ServerLevel sl, BlockPos at) {
        NguyenThuyTichDiet b = EndgameRegistry.BOSS.get().create(sl);
        if (b == null) return null;
        b.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        b.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), net.minecraft.world.entity.MobSpawnType.EVENT, null);
        sl.addFreshEntity(b);
        b.begin(sl);
        return b;
    }

    /** Boss chết: dọn tay sai + Dạ Khuyết, đặt cờ chờ chọn và mở màn chọn cho người chơi quanh đó. */
    public static void onBossDefeated(ServerLevel sl, NguyenThuyTichDiet boss) {
        for (DaKhuyet d : sl.getEntitiesOfClass(DaKhuyet.class, boss.getBoundingBox().inflate(120))) d.discard();
        for (Monster m : sl.getEntitiesOfClass(Monster.class, boss.getBoundingBox().inflate(120), e -> e instanceof HuMob)) m.discard();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(boss) > 200 * 200) continue;
            p.displayClientMessage(Component.translatable("hukhong.endgame.boss.slain"), false);
            p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.flee"), false);
            QuestData q = DialogueService.quest(p);
            if (hasEnding(q)) continue;
            DialogueService.setQuest(p, q.withFlag(F_PENDING));
            promptChoice(p);
        }
    }

    // ------------------------------------------------------------ chọn kết thúc + cutscene
    public static void promptChoice(ServerPlayer p) { PacketDistributor.sendToPlayer(p, new EndgameNet.OpenChoice()); }

    public static void choose(ServerPlayer p, int ending) {
        QuestData q = DialogueService.quest(p);
        if (!EndgameKit.validEnding(ending) || !q.has(F_PENDING) || hasEnding(q)) return;
        DialogueService.setQuest(p, q.withFlag(ending == EndgameKit.ENDING_SEAL ? F_SEAL : F_ABSORB).withChapter(6));
        WorldTier.raiseTo(p.getServer(), 2);
        p.displayClientMessage(Component.translatable("hukhong.endgame.tier2"), false);
        int dur = EndgameKit.CUTSCENE_TICKS + 200;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 6, false, false));
        CUTSCENE.put(p.getUUID(), new int[]{ending, EndgameKit.CUTSCENE_TICKS});
        PacketDistributor.sendToPlayer(p, new EndgameNet.PlayCutscene(ending));
    }

    /** Client báo xong cutscene + credits (hoặc bỏ qua): trả người chơi về thế giới. */
    public static void finish(ServerPlayer p) {
        QuestData q = DialogueService.quest(p);
        if (!hasEnding(q) || p.serverLevel().dimension() != EndgameRegistry.DIM) return;
        CUTSCENE.remove(p.getUUID());
        p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        leave(p);
        p.displayClientMessage(Component.translatable(q.has(F_SEAL) ? "hukhong.endgame.return.seal" : "hukhong.endgame.return.absorb"), false);
    }

    private static void cutsceneTick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, int[]>> it = CUTSCENE.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            int[] st = en.getValue();
            if (p == null || --st[1] <= 0) { it.remove(); continue; }
            if (st[1] % 2 != 0) continue;
            ServerLevel sl = p.serverLevel();
            int t = EndgameKit.CUTSCENE_TICKS - st[1];
            double ang = t * 0.12;
            boolean seal = st[0] == EndgameKit.ENDING_SEAL;
            for (int i = 0; i < 3; i++) {
                double a = ang + i * Math.PI * 2 / 3, r = seal ? 2.5 + (t % 60) * 0.03 : 6.0 - (t % 80) * 0.07;
                sl.sendParticles(seal ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME,
                        p.getX() + Math.cos(a) * r, p.getY() + 0.3 + (t % 100) * 0.03, p.getZ() + Math.sin(a) * r, 1, 0, 0.02, 0, 0.0);
            }
            sl.sendParticles(seal ? ParticleTypes.ENCHANT : ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 6, 2, 1, 2, 0.1);
        }
    }

    // ------------------------------------------------------------ sự kiện
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        long t = s.getTickCount();
        if (!CUTSCENE.isEmpty()) cutsceneTick(s);
        ServerLevel ow = s.getLevel(Level.OVERWORLD);
        if (ow != null && t % 100 == 0 && !ow.players().isEmpty()) gatewayTick(ow);
        if (t % 10 == 0) {
            ServerLevel dim = s.getLevel(EndgameRegistry.DIM);
            if (dim != null) for (ServerPlayer p : dim.players())
                if (p.getY() < 30 && !p.isSpectator()) {  // rơi khỏi đảo: về điểm hạ + chút sát thương
                    p.teleportTo(dim, EndgameArena.ARRIVE.getX() + 0.5, EndgameArena.ARRIVE.getY(), EndgameArena.ARRIVE.getZ() + 0.5, 180f, 0f);
                    p.hurt(dim.damageSources().magic(), 4f);
                }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        QuestData q = DialogueService.quest(p);
        if (q.has(F_PENDING) && !hasEnding(q)) promptChoice(p);
        if (hasEnding(q)) { // relog giữa cutscene: bỏ trạng thái bất tử/đóng băng và đưa về Overworld
            var res = p.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (res != null && res.getAmplifier() == 4 && !res.isVisible()) p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            var slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slow != null && slow.getAmplifier() == 6 && !slow.isVisible()) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (p.serverLevel().dimension() == EndgameRegistry.DIM) leave(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { CUTSCENE.remove(e.getEntity().getUUID()); }
}
