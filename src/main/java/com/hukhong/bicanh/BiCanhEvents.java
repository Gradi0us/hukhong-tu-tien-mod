package com.hukhong.bicanh;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/** Móc sự kiện Bí Cảnh: tick instance, cổng định kỳ, chống chết / phá khối trong instance. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class BiCanhEvents {
    private BiCanhEvents() {}

    public static final int PORTAL_LIFETIME = 30 * 60 * 20;
    public static final int MAX_PORTALS = 3;
    /** Server 1 cổng / 45 phút; singleplayer 1 cổng / 2 giờ (S4 §5.1). */
    public static long intervalTicks(boolean singleplayer) { return singleplayer ? 2L * 60 * 60 * 20 : 45L * 60 * 20; }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        BiCanhManager.tick(s);
        int t = s.getTickCount();
        if (t % 100 != 0) return;
        ServerLevel ow = s.overworld();
        BiCanhSaved d = BiCanhSaved.get(ow);
        long now = ow.getGameTime();
        for (BiCanhSaved.Portal p : new ArrayList<>(d.portals)) {
            if (now < p.expire()) continue;
            if (ow.hasChunkAt(p.pos()) && ow.getBlockState(p.pos()).is(BiCanhRegistry.PORTAL.get()))
                ow.setBlock(p.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            d.remove(p.pos());
        }
        if (d.nextSpawn < 0) d.setNext(now + intervalTicks(s.isSingleplayer()));
        if (now >= d.nextSpawn) {
            d.setNext(now + intervalTicks(s.isSingleplayer()));
            if (d.portals.size() < MAX_PORTALS && !ow.players().isEmpty()) {
                ServerPlayer p = ow.players().get(ow.random.nextInt(ow.players().size()));
                spawnPortalNear(ow, p, null);
            }
        }
    }

    /** Sinh cổng quanh người chơi (tier null = tung theo cấp người chơi). Trả vị trí hoặc null. */
    public static BlockPos spawnPortalNear(ServerLevel ow, ServerPlayer p, BiCanhTier tier) {
        BiCanhTier t = tier != null ? tier : BiCanhTier.roll(ow.random.nextDouble(), CultivationService.get(p).level());
        for (int i = 0; i < 12; i++) {
            double ang = ow.random.nextDouble() * Math.PI * 2, dist = 24 + ow.random.nextInt(41);
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * dist), z = (int) Math.floor(p.getZ() + Math.sin(ang) * dist);
            if (!ow.hasChunk(x >> 4, z >> 4)) continue;
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            var below = ow.getBlockState(pos.below());
            if (below.isAir() || !below.getFluidState().isEmpty() || !ow.getBlockState(pos).canBeReplaced()) continue;
            ow.setBlock(pos, BiCanhRegistry.PORTAL.get().stateFor(t), 3);
            BiCanhSaved.get(ow).add(new BiCanhSaved.Portal(pos, t.ordinal(), ow.getGameTime() + PORTAL_LIFETIME));
            p.sendSystemMessage(Component.translatable("hukhong.bicanh.portal_appeared", Component.translatable(t.nameKey()),
                    pos.getX(), pos.getY(), pos.getZ()));
            return pos;
        }
        return null;
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().dimension() == BiCanhRegistry.DIM
                && p.getHealth() - e.getAmount() <= 0f) {
            e.setCanceled(true);
            BiCanhManager.rescue(p);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().dimension() == BiCanhRegistry.DIM) {
            e.setCanceled(true);
            BiCanhManager.rescue(p);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().dimension() == BiCanhRegistry.DIM && !BiCanhManager.inRun(p))
            BiCanhManager.leave(p, "hukhong.bicanh.stale");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) BiCanhManager.dropBar(p);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof ServerPlayer p && p.level().dimension() == BiCanhRegistry.DIM && !p.isCreative()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.level().dimension() == BiCanhRegistry.DIM && !p.isCreative()) e.setCanceled(true);
    }

    @SubscribeEvent public static void onStarting(ServerStartingEvent e) { BiCanhManager.reset(); }
    @SubscribeEvent public static void onStopped(ServerStoppedEvent e) { BiCanhManager.reset(); }
}
