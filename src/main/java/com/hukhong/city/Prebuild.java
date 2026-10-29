package com.hukhong.city;

import com.hukhong.HuKhong;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectEvents;
import com.hukhong.society.SectBuilder;
import com.hukhong.society.SectSavedData;
import com.hukhong.structures.LoadedStructure;
import com.hukhong.structures.StructureLibrary;
import com.hukhong.structures.StructurePlacer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dung san toan bo thanh/cang/tong khi vao the gioi moi lan dau (WorldBuildData.done chua bat). Khong chan luong server:
 * (1) doc file cau truc tren luong nen, (2) tim bo bien cho cang tung buoc theo ngan sach 8 ms/tick, (3) xin ticket nap chunk vung dat
 * (chunk sinh bat dong bo), (4) goi builder (StructurePlacer dat 6 ms/tick), (5) tha ticket, sang muc tieu ke tiep.
 * Tien do: log + thanh boss + tin nhan chat. Muc tieu da dung roi thi bo qua (the gioi cu chi dung phan con thieu).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class Prebuild {
    private Prebuild() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final TicketType<ChunkPos> TICKET = TicketType.create("hukhong_prebuild", Comparator.comparingLong(ChunkPos::toLong));
    private static final long FIND_BUDGET_NS = 8_000_000L;
    private static final int LOAD_TIMEOUT = 6000;

    private enum State { UNINIT, RUNNING, DONE }
    private enum Phase { PRELOAD, FIND, LOAD, DRAIN }

    private record Target(CityDef city, SectDef sect) {
        String id() { return city != null ? city.id() : sect.id(); }
        String nameKey() { return city != null ? city.nameKey() : sect.nameKey(); }
        String category() { return city != null ? city.category() : sect.category(); }
    }

    private static State state = State.UNINIT;
    private static Phase phase;
    private static List<Target> targets = List.of();
    private static int idx, total;
    private static CompletableFuture<LoadedStructure> preload;
    private static LoadedStructure st;
    private static PortSites finder;
    private static BlockPos site, center;
    private static List<ChunkPos> held = new ArrayList<>();
    private static int phaseTicks;
    private static ServerBossEvent bar;
    private static boolean blocking;
    private static int failedTotal, attempts, builtNow;
    /** Van ban tien do cho man hinh tai (client luong tich hop doc); null = khong dang dung. */
    public static volatile String overlayText;
    public static volatile float overlayFraction;

    /** Luon bat dau tu trang thai sach: bien tinh song qua nhieu the gioi trong 1 JVM (singleplayer) la nguyen nhan "luc duoc luc khong". */
    private static void reset() {
        state = State.UNINIT;
        phase = null;
        targets = List.of();
        idx = 0; total = 0; failedTotal = 0; attempts = 0; builtNow = 0;
        preload = null; st = null; finder = null; site = null; center = null;
        held = new ArrayList<>();
        blocking = false;
        overlayText = null;
        if (bar != null) bar.removeAllPlayers();
        bar = null;
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { reset(); }

    /** Dung toan bo truoc khi nguoi choi vao the gioi moi (chay tren luong server, dong bo). */
    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) { LOG.error("Prebuild: no overworld at ServerStarted"); return; }
        if (state != State.UNINIT) { LOG.warn("Prebuild: stale state {} at start, resetting", state); reset(); }
        runBlocking(server, ow, true);
    }

    /** Neu vi ly do nao do dung chua xong khi nguoi choi vao: dung not ngay (dong bo) truoc khi ho choi. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (state != State.RUNNING || blocking || !(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) return;
        ServerLevel ow = p.server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        LOG.warn("Prebuild: player joined while build still running ({} of {}), finishing synchronously", idx, total);
        runBlocking(p.server, ow, false);
    }

    private static void runBlocking(MinecraftServer server, ServerLevel ow, boolean doInit) {
        long t0 = System.nanoTime();
        blocking = true;
        DrySites.heartbeat = () -> bumpWatchdog(server);
        try {
            if (doInit || state == State.UNINIT) init(server, ow);
            else if (state == State.RUNNING && phase == Phase.PRELOAD && preload != null) { /* tiep tuc */ }
            while (state == State.RUNNING) {
                bumpWatchdog(server);
                try {
                    step(server, ow);
                } catch (RuntimeException | StackOverflowError ex) {
                    LOG.error("Prebuild step failed for {}: {}", idx < targets.size() ? targets.get(idx).id() : "?", ex.toString(), ex);
                    failTarget(ow, server);
                }
                if (state == State.RUNNING && phase == Phase.PRELOAD && !preload.isDone()) {
                    try { Thread.sleep(1); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
            LOG.info("Prebuild (blocking, pre-join) finished in {} ms", (System.nanoTime() - t0) / 1_000_000L);
        } catch (RuntimeException ex) {
            LOG.error("Blocking prebuild failed, falling back to progressive build: {}", ex.toString(), ex);
            release(ow);
        } finally {
            blocking = false;
            DrySites.heartbeat = null;
            overlayText = null;
            bumpWatchdog(server);
        }
    }

    /** Muc tieu hien tai loi: thu lai 1 lan; van loi -> bo qua nhung KHONG danh dau the gioi la done (lan khoi dong sau dung tiep). */
    private static void failTarget(ServerLevel ow, MinecraftServer server) {
        release(ow);
        if (attempts < 1) {
            attempts++;
            LOG.warn("Prebuild: retrying target {}", targets.get(idx).id());
            beginTarget(server, ow);
        } else {
            failedTotal++;
            LOG.error("Prebuild: target {} FAILED twice, skipped (world not marked done; will resume next start)", targets.get(idx).id());
            next(ow, server);
        }
    }

    private static java.lang.reflect.Field tickField;
    private static boolean tickFieldTried;

    /** Giu watchdog (max-tick-time) va "can't keep up" khong kich hoat trong luc dung dong bo. */
    private static void bumpWatchdog(MinecraftServer server) {
        try {
            if (!tickFieldTried) {
                tickFieldTried = true;
                tickField = MinecraftServer.class.getDeclaredField("nextTickTimeNanos");
                tickField.setAccessible(true);
            }
            if (tickField != null) tickField.setLong(server, System.nanoTime());
        } catch (ReflectiveOperationException | RuntimeException ex) {
            if (tickField == null) LOG.warn("Cannot bump watchdog clock: {}", ex.toString());
        }
    }

    /** Con dang giu quyen dung (sinh lazy cua thanh/tong tam tat). */
    public static boolean owns() { return state != State.DONE; }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (state == State.DONE || blocking) return;
        ServerLevel ow = server.getLevel(Level.OVERWORLD);
        if (ow == null) return;
        if (state == State.UNINIT) {
            if (server.getTickCount() < 20) return;
            init(server, ow);
            if (state != State.RUNNING) return;
        }
        try {
            step(server, ow);
        } catch (RuntimeException ex) {
            LOG.error("Prebuild step failed for {}: {}", idx < targets.size() ? targets.get(idx).id() : "?", ex.toString(), ex);
            failTarget(ow, server);
        }
        if (state == State.RUNNING && server.getTickCount() % 20 == 0) {
            for (var p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
        }
    }

    private static void init(MinecraftServer server, ServerLevel ow) {
        WorldBuildData data = WorldBuildData.get(ow);
        if (data.done()) { LOG.info("Prebuild: world already complete (done flag set)"); state = State.DONE; return; }
        CityRegistryData reg = CityRegistryData.get(ow);
        SectSavedData sects = SectSavedData.get(ow);
        List<Target> list = new ArrayList<>();
        for (CityDef d : CityDef.ALL) if (CityService.builtCenter(ow, d) == null) list.add(new Target(d, null));
        for (SectDef d : SectDef.ALL) if (d.buildable() && !sects.isBuilt(d.id())) list.add(new Target(null, d));
        BlockPos sp = ow.getSharedSpawnPos();
        list.sort(Comparator.comparingDouble(t -> {
            BlockPos s = t.city != null ? CityService.site(ow, t.city) : SectEvents.site(ow, t.sect);
            return Math.hypot(s.getX() - sp.getX(), s.getZ() - sp.getZ());
        }));
        if (list.isEmpty()) { data.setDone(); state = State.DONE; LOG.info("Prebuild: all settlements already exist, marked done"); return; }
        targets = list;
        total = list.size();
        idx = 0;
        bar = new ServerBossEvent(Component.translatable("hukhong.prebuild.bar", "", 0, total), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("hukhong.prebuild.start", total), false);
        LOG.info("Prebuild: {} settlements to build", total);
        state = State.RUNNING;
        beginTarget(server, ow);
    }

    private static void beginTarget(MinecraftServer server, ServerLevel ow) {
        Target t = targets.get(idx);
        StructureLibrary.ensureUserDirs();
        long seed = ow.getSeed();
        preload = CompletableFuture.supplyAsync(() -> StructureLibrary.preload(server, t.category(), t.id(), seed));
        st = null; finder = null; site = null; center = null;
        phase = Phase.PRELOAD;
        phaseTicks = 0;
        progress(ow, 0.0f);
    }

    private static void progress(ServerLevel ow, float within) {
        Target t = targets.get(Math.min(idx, targets.size() - 1));
        bar.setName(Component.translatable("hukhong.prebuild.bar", Component.translatable(t.nameKey()), idx + 1, total));
        bar.setProgress(Math.min(1f, (idx + within) / total));
        if (blocking) {
            overlayText = Component.translatableWithFallback("hukhong.prebuild.loading", "Đang kiến tạo thế giới: %s (%s/%s)", Component.translatable(t.nameKey()), idx + 1, total).getString();
            overlayFraction = Math.min(1f, (idx + within) / total);
        }
    }

    private static BlockPos anchor(ServerLevel ow, Target t) {
        return t.city != null ? CityService.site(ow, t.city) : SectEvents.site(ow, t.sect);
    }

    private static void step(MinecraftServer server, ServerLevel ow) {
        Target t = targets.get(idx);
        WorldBuildData data = WorldBuildData.get(ow);
        phaseTicks++;
        switch (phase) {
            case PRELOAD -> {
                if (!preload.isDone()) return;
                try { st = preload.join(); } catch (RuntimeException ex) { LOG.warn("Preload failed for {}: {}", t.id(), ex.toString()); st = null; }
                int[] saved = t.city != null ? data.portSite(t.id()) : null;
                if (t.city != null && t.category().equals("port") && saved == null && st != null) {
                    finder = new PortSites(ow, st, anchor(ow, t));
                    phase = Phase.FIND;
                } else if (saved != null) {
                    site = new BlockPos(saved[0], 64, saved[1]);
                    enterLoad(ow);
                } else {
                    int half = st != null ? Math.max(st.sx, st.sz) / 2 : (t.city != null ? CityBuilder.HALF : SectBuilder.HALF);
                    String key = t.city != null ? t.id() : "sect:" + t.id();
                    site = CityBuilder.adjustSite(ow, key, anchor(ow, t), half + 4, t.category());
                    enterLoad(ow);
                }
            }
            case FIND -> {
                finder.step(blocking ? 1_000_000_000L : FIND_BUDGET_NS);
                if (!finder.finished()) return;
                site = finder.result();
                data.setPortSite(t.id(), site.getX(), site.getZ());
                LOG.info("Port {} placed on coast at {}", t.id(), site);
                enterLoad(ow);
            }
            case LOAD -> {
                if (!blocking && phaseTicks % 10 != 0) return;
                int ready = 0;
                for (ChunkPos c : held) if (ow.getChunkSource().getChunkNow(c.x, c.z) != null) ready++;
                progress(ow, 0.7f * ready / Math.max(1, held.size()));
                if (ready < held.size() && phaseTicks < LOAD_TIMEOUT) return;
                if (ready < held.size()) LOG.warn("Prebuild {}: chunk load timeout ({}/{}), placing anyway", t.id(), ready, held.size());
                center = t.city != null ? CityBuilder.build(ow, site, t.city) : SectBuilder.build(ow, site, t.sect);
                phase = Phase.DRAIN;
                phaseTicks = 0;
            }
            case DRAIN -> {
                if (blocking) StructurePlacer.drainFor(2_000_000_000L);
                if (StructurePlacer.pending() > 0) { if (phaseTicks % 20 == 0) progress(ow, 0.7f + 0.3f * (1f - Math.min(1f, StructurePlacer.pending() / 3f))); return; }
                if (t.city != null) {
                    if (t.city.index() == 0) CitySavedData.get(ow).markBuilt(center);
                    else CityRegistryData.get(ow).markBuilt(t.id(), center);
                } else SectSavedData.get(ow).markBuilt(t.id(), center);
                builtNow++;
                LOG.info("Prebuild: {} built at {} ({}/{})", t.id(), center, idx + 1, total);
                release(ow);
                next(ow, server);
            }
        }
    }

    private static void enterLoad(ServerLevel ow) {
        Target t = targets.get(idx);
        int half = st != null ? Math.max(st.sx, st.sz) / 2 + 16 : (t.city != null ? CityBuilder.HALF : SectBuilder.HALF) + 24;
        int cx0 = (site.getX() - half) >> 4, cx1 = (site.getX() + half) >> 4, cz0 = (site.getZ() - half) >> 4, cz1 = (site.getZ() + half) >> 4;
        held = new ArrayList<>();
        for (int x = cx0; x <= cx1; x++) for (int z = cz0; z <= cz1; z++) {
            ChunkPos c = new ChunkPos(x, z);
            ow.getChunkSource().addRegionTicket(TICKET, c, 1, c);
            held.add(c);
        }
        LOG.info("Prebuild {}: loading {} chunks around {}", t.id(), held.size(), site);
        if (blocking) for (ChunkPos c : held) {
            Runnable hb = DrySites.heartbeat;
            if (hb != null) hb.run();
            ow.getChunk(c.x, c.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
        }
        phase = Phase.LOAD;
        phaseTicks = 0;
    }

    private static void release(ServerLevel ow) {
        for (ChunkPos c : held) ow.getChunkSource().removeRegionTicket(TICKET, c, 1, c);
        held = new ArrayList<>();
    }

    private static void next(ServerLevel ow, MinecraftServer server) {
        idx++;
        attempts = 0;
        if (idx >= targets.size()) {
            if (failedTotal == 0) WorldBuildData.get(ow).setDone();
            state = State.DONE;
            bar.removeAllPlayers();
            server.getPlayerList().broadcastSystemMessage(Component.translatable("hukhong.prebuild.done", total), false);
            LOG.info("Prebuild SUMMARY: {}/{} settlements built this run, {} failed, world done flag={}", builtNow, total, failedTotal, failedTotal == 0);
            return;
        }
        beginTarget(server, ow);
    }
}
