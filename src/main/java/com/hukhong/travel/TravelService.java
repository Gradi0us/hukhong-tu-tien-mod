package com.hukhong.travel;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.currency.TuTinhText;
import com.hukhong.currency.Wallet;
import com.hukhong.guild.GuildHalls;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectSavedData;
import com.hukhong.story.objective.ObjectiveAttachments;
import com.hukhong.story.objective.ObjectiveData;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Dich chuyen nhanh: diem den = muc tieu hien tai ("obj") hoac noi da kham pha ("c:<thanh/cang>", "s:<tong>", "g:<thanh>").
 * Kenh 3s (huy khi di chuyen / bi sat thuong), phi Tu Tinh theo khoang cach, hoi chieu 60s, ha canh o cho dung duoc.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class TravelService {
    private TravelService() {}

    public static final String OBJ = "obj";

    private record Target(ServerLevel level, BlockPos pos, Integer yHint, double dist) {}

    private record Chan(String dest, Target target, long cost, long endTick, Vec3 start) {}

    private static final Map<UUID, Chan> CHANNELS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();


    private static String normalize(String id) {
        if (id.equals(OBJ) || id.contains(":")) return id;
        if (CityDef.byId(id) != null) return "c:" + id;
        if (SectDef.byId(id) != null) return "s:" + id;
        return id;
    }

    // ------------------------------------------------------------------ bat dau
    /** null = da bat dau kenh; con lai = key loi. force (lenh thu nghiem): bo qua kham pha + phi. Van ton trong ngu canh + hoi chieu. */
    public static String start(ServerPlayer p, String rawId, boolean force) {
        String id = normalize(rawId);
        String err = TravelRules.check(p);
        if (err != null) return err;
        if (CHANNELS.containsKey(p.getUUID())) return "hukhong.travel.err_busy";
        long now = p.level().getGameTime();
        Long ready = COOLDOWN.get(p.getUUID());
        if (ready != null && ready > now) return "hukhong.travel.err_cooldown";
        if (!force && !id.equals(OBJ) && !p.getData(TravelRegistry.TRAVEL.get()).has(id)) return "hukhong.travel.err_undiscovered";
        Object r = resolve(p, id);
        if (r instanceof String s) return s;
        Target t = (Target) r;
        long cost = force ? 0 : TravelMath.cost(t.dist());
        if (!force && Wallet.count(p) < cost) return "hukhong.travel.err_funds";
        CHANNELS.put(p.getUUID(), new Chan(id, t, cost, now + TravelMath.CHANNEL_TICKS, p.position()));
        Fx.play(p.serverLevel(), FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 1.0f, Fx.CYAN, TravelMath.CHANNEL_TICKS);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
        p.displayClientMessage(Component.translatable("hukhong.travel.start", Component.translatable(TravelMath.nameKey(id)), TuTinhText.of(cost)), false);
        return null;
    }

    public static boolean channeling(ServerPlayer p) { return CHANNELS.containsKey(p.getUUID()); }

    public static void cancel(ServerPlayer p, String reasonKey) {
        if (CHANNELS.remove(p.getUUID()) != null) {
            p.displayClientMessage(Component.translatable(reasonKey), true);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    public static boolean onCooldown(ServerPlayer p) {
        Long ready = COOLDOWN.get(p.getUUID());
        return ready != null && ready > p.level().getGameTime();
    }

    /** Target hoac key loi (String). */
    private static Object resolve(ServerPlayer p, String id) {
        MinecraftServer srv = p.server;
        ServerLevel ow = srv.getLevel(Level.OVERWORLD);
        if (ow == null) return "hukhong.travel.err_dim";
        BlockPos pos;
        Integer yHint = null;
        if (id.equals(OBJ)) {
            ObjectiveData od = p.getData(ObjectiveAttachments.OBJECTIVE.get());
            if (od.isEmpty() || od.target().isEmpty()) return "hukhong.travel.err_noobj";
            if (od.dim().isPresent() && od.dim().get() != Level.OVERWORLD) return "hukhong.travel.err_dim";
            pos = od.target().get();
        } else if (id.startsWith("c:")) {
            CityDef d = CityDef.byId(id.substring(2));
            pos = d == null ? null : CityService.builtCenter(ow, d);
            if (pos != null) yHint = pos.getY();
        } else if (id.startsWith("s:")) {
            pos = SectSavedData.get(ow).center(id.substring(2));
            if (pos != null) yHint = pos.getY();
        } else if (id.startsWith("g:")) {
            pos = GuildHalls.get(ow).get(id.substring(2));
            if (pos != null) yHint = pos.getY();
        } else if (id.startsWith("m:")) { // Khu Cho: tam = san khu cho
            pos = com.hukhong.city.MarketDistricts.get(ow).built(id.substring(2));
            if (pos != null) yHint = pos.getY();
        } else if (id.startsWith("p:")) { // dia diem (Khach Diem): diem dung truoc cua
            com.hukhong.poi.PoiInstance pi = com.hukhong.poi.PoiSavedData.get(ow).get(id.substring(2));
            pos = pi == null ? null : com.hukhong.poi.PoiService.travelPos(pi);
            if (pos != null) yHint = pos.getY();
        } else return "hukhong.travel.err_unknown";
        if (pos == null) return "hukhong.travel.err_unbuilt";
        double dx = p.getX() - pos.getX(), dz = p.getZ() - pos.getZ();
        return new Target(ow, pos, yHint, Math.sqrt(dx * dx + dz * dz));
    }

    // ------------------------------------------------------------------ kenh
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (CHANNELS.isEmpty()) return;
        MinecraftServer srv = e.getServer();
        for (Iterator<Map.Entry<UUID, Chan>> it = CHANNELS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Chan> en = it.next();
            Chan c = en.getValue();
            ServerPlayer p = srv.getPlayerList().getPlayer(en.getKey());
            if (p == null || !p.isAlive()) { it.remove(); continue; }
            long now = p.level().getGameTime();
            double dx = p.getX() - c.start().x, dz = p.getZ() - c.start().z;
            if (dx * dx + dz * dz > 0.36) { it.remove(); notifyCancel(p, "hukhong.travel.err_moved"); continue; }
            String err = TravelRules.check(p);
            if (err != null) { it.remove(); notifyCancel(p, err); continue; }
            long left = c.endTick() - now;
            if (left > 0) {
                if (left % 20 == 0) p.displayClientMessage(Component.translatable("hukhong.travel.channel", left / 20), true);
                continue;
            }
            it.remove();
            finish(p, c);
        }
    }

    private static void notifyCancel(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void finish(ServerPlayer p, Chan c) {
        if (c.cost() > 0 && !Wallet.pay(p, c.cost())) { notifyCancel(p, "hukhong.travel.err_funds"); return; }
        ServerLevel to = c.target().level();
        BlockPos land = findLanding(to, c.target().pos().getX(), c.target().pos().getZ(), c.target().yHint());
        Fx.play(p.serverLevel(), FxType.LIGHT_PILLAR, p.position(), Vec3.ZERO, 1.2f, Fx.CYAN, 20);
        p.teleportTo(to, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        Fx.play(to, FxType.LIGHT_PILLAR, Vec3.atBottomCenterOf(land), Vec3.ZERO, 1.2f, Fx.CYAN, 30);
        to.playSound(null, land, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.0f);
        COOLDOWN.put(p.getUUID(), to.getGameTime() + TravelMath.COOLDOWN_TICKS);
        p.displayClientMessage(Component.translatable("hukhong.travel.arrived", Component.translatable(TravelMath.nameKey(c.dest()))), true);
    }

    // ------------------------------------------------------------------ ha canh an toan
    private static boolean hazard(BlockState s) {
        return s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.FIRE)
                || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.COBWEB)
                || s.is(Blocks.WITHER_ROSE) || s.is(BlockTags.FIRE) || s.is(BlockTags.CAULDRONS) || s.is(Blocks.LAVA);
    }

    private static boolean standable(ServerLevel l, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockState below = l.getBlockState(feet.below());
        if (!below.blocksMotion() || hazard(below) || !l.getFluidState(feet.below()).isEmpty()) return false;
        for (int h = 0; h < 2; h++) {
            BlockPos q = feet.above(h);
            BlockState s = l.getBlockState(q);
            if (s.blocksMotion() || hazard(s) || !l.getFluidState(q).isEmpty() || !s.getCollisionShape(l, q).isEmpty()) return false;
        }
        return true;
    }

    /** Vi tri dung duoc gan (x,z): xoay vong ban kinh 0..12; co yHint thi chi quet y +-4 quanh do (san dien dan / sanh), khong thi theo heightmap. */
    public static BlockPos findLanding(ServerLevel l, int x, int z, Integer yHint) {
        l.getChunk(x >> 4, z >> 4);
        for (int r = 0; r <= 12; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int px = x + dx, pz = z + dz;
                if (!l.hasChunkAt(new BlockPos(px, 64, pz))) continue;
                if (yHint != null) {
                    for (int d = 0; d <= 4; d++) for (int sgn = -1; sgn <= 1; sgn += 2) {
                        if (d == 0 && sgn == 1) continue;
                        int py = yHint + 1 + sgn * d;
                        if (standable(l, px, py, pz)) return new BlockPos(px, py, pz);
                    }
                } else {
                    int py = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
                    if (standable(l, px, py, pz)) return new BlockPos(px, py, pz);
                }
            }
        }
        int py = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Math.max(py, l.getMinBuildHeight() + 1), z);
    }

    // ------------------------------------------------------------------ kham pha
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 11) return;
        if (p.level().dimension() != Level.OVERWORLD) return;
        ServerLevel ow = p.serverLevel();
        TravelData d = p.getData(TravelRegistry.TRAVEL.get());
        TravelData nd = d;
        int r2 = TravelMath.DISCOVER_RADIUS * TravelMath.DISCOVER_RADIUS;
        for (CityDef c : CityDef.ALL) {
            BlockPos pos = CityService.builtCenter(ow, c);
            if (pos != null && near(p, pos, r2)) nd = discover(p, nd, "c:" + c.id());
        }
        SectSavedData sects = SectSavedData.get(ow);
        for (SectDef s : SectDef.ALL) {
            BlockPos pos = s.hidden() ? null : sects.center(s.id());
            if (pos != null && near(p, pos, r2)) nd = discover(p, nd, "s:" + s.id());
        }
        for (Map.Entry<String, BlockPos> g : GuildHalls.get(ow).all().entrySet())
            if (near(p, g.getValue(), r2)) nd = discover(p, nd, "g:" + g.getKey());
        for (Map.Entry<String, BlockPos> m : com.hukhong.city.MarketDistricts.get(ow).allBuilt().entrySet())
            if (near(p, m.getValue(), r2)) nd = discover(p, nd, "m:" + m.getKey());
        if (nd != d) p.setData(TravelRegistry.TRAVEL.get(), nd);
    }

    private static boolean near(ServerPlayer p, BlockPos pos, int r2) {
        double dx = p.getX() - pos.getX(), dz = p.getZ() - pos.getZ();
        return dx * dx + dz * dz <= r2;
    }

    private static TravelData discover(ServerPlayer p, TravelData d, String id) {
        if (d.has(id)) return d;
        p.displayClientMessage(Component.translatable("hukhong.travel.discovered", Component.translatable(TravelMath.nameKey(id))), true);
        return d.with(id);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        CHANNELS.remove(e.getEntity().getUUID());
        COOLDOWN.remove(e.getEntity().getUUID());
    }
}
