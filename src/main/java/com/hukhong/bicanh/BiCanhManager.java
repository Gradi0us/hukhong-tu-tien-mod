package com.hukhong.bicanh;

import com.hukhong.bicanh.BiCanhLayout.Kind;
import com.hukhong.bicanh.BiCanhLayout.Room;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.HuHoa;
import com.hukhong.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Vòng đời instance Bí Cảnh: vào, sóng quái, mini-boss, rương, thoát / hết giờ. Chỉ chạy trên server thread. */
public final class BiCanhManager {
    private BiCanhManager() {}

    private static final String RET_TAG = "hukhong_bicanh_ret";
    public static final int VICTORY_GRACE_TICKS = 1200;

    record Ret(ResourceKey<Level> dim, double x, double y, double z, float yRot) {}

    static final class Run {
        final int slot;
        final BiCanhTier tier;
        final long seed;
        final BlockPos origin;
        final List<Room> rooms;
        final int[] state; // 0 chưa, 1 đang đánh, 2 đã dọn
        final Set<UUID> players = new LinkedHashSet<>();
        final Map<UUID, Ret> ret = new HashMap<>();
        final Set<UUID> mobs = new HashSet<>();
        final ServerBossEvent bar;
        final List<long[]> doorAnims = new ArrayList<>(); // {roomIdx, startTick}
        long endTick;
        int active = -1;
        boolean won;
        int partyAtStart;

        Run(int slot, BiCanhTier tier, long seed, BlockPos origin, long endTick) {
            this.slot = slot; this.tier = tier; this.seed = seed; this.origin = origin; this.endTick = endTick;
            this.rooms = BiCanhLayout.rooms(tier);
            this.state = new int[rooms.size()];
            this.bar = new ServerBossEvent(Component.translatable("hukhong.bicanh.bar", Component.translatable(tier.nameKey()), "--:--"),
                    BossEvent.BossBarColor.values()[Math.min(tier.ordinal() + 1, 6)], BossEvent.BossBarOverlay.PROGRESS);
        }
    }

    static final Map<Integer, Run> RUNS = new HashMap<>();

    public static void reset() { RUNS.clear(); }

    public static boolean inRun(ServerPlayer p) { return runOf(p) != null; }

    /** Cấp Bí Cảnh người chơi đang ở (ordinal của BiCanhTier), -1 nếu không trong run (dùng cho ambient client). */
    public static int tierOrdinalOf(ServerPlayer p) {
        Run r = runOf(p);
        return r == null ? -1 : r.tier.ordinal();
    }

    static Run runOf(ServerPlayer p) {
        for (Run r : RUNS.values()) if (r.players.contains(p.getUUID())) return r;
        return null;
    }

    /** Gỡ người chơi khỏi bossbar khi thoát game (tránh giữ tham chiếu ServerPlayer cũ). */
    public static void dropBar(ServerPlayer p) {
        Run r = runOf(p);
        if (r != null) r.bar.removePlayer(p);
    }

    static int maxRuns(MinecraftServer s) { return s.isSingleplayer() ? 1 : 3; }

    // ---------------------------------------------------------------- vào

    public static void enterFromPortal(ServerLevel ow, BlockPos clicked, BiCanhTier tier, ServerPlayer starter) {
        BlockPos pos = clicked;
        for (BiCanhSaved.Portal sp : BiCanhSaved.get(ow).portals) if (sp.pos().closerThan(clicked, 3.5)) { pos = sp.pos(); break; }
        BiCanhSaved saved = BiCanhSaved.get(ow);
        List<ServerPlayer> party = new ArrayList<>();
        party.add(starter);
        for (ServerPlayer p : ow.players())
            if (p != starter && p.blockPosition().closerThan(pos, 8) && party.size() < tier.maxParty) party.add(p);
        String err = enter(ow.getServer(), starter, tier, party, false);
        if (err != null) starter.sendSystemMessage(Component.translatable(err, tier.minLevel, tier.maxLevel));
        else {
            removePortal(ow, pos);
            saved.remove(pos);
        }
    }


    /** Gỡ cổng + sập hang (nếu cổng còn). */
    public static void removePortal(ServerLevel ow, BlockPos pos) {
        if (!ow.hasChunkAt(pos)) return;
        var st = ow.getBlockState(pos);
        if (st.is(BiCanhRegistry.PORTAL.get())) BiCanhCave.collapse(ow, pos, st.getValue(BiCanhPortalBlock.FACING));
    }
    /** Trả null nếu thành công, ngược lại là lang key lỗi. Thành viên không đủ điều kiện bị bỏ qua. */
    public static String enter(MinecraftServer server, ServerPlayer starter, BiCanhTier tier, Collection<ServerPlayer> party, boolean ignoreGate) {
        ServerLevel dim = server.getLevel(BiCanhRegistry.DIM);
        if (dim == null) return "hukhong.bicanh.err.nodim";
        if (starter.level().dimension() == BiCanhRegistry.DIM || runOf(starter) != null) return "hukhong.bicanh.err.inside";
        if (!ignoreGate && !tier.allows(CultivationService.get(starter).level())) return "hukhong.bicanh.err.level";
        int slot = -1;
        for (int i = 0; i < maxRuns(server); i++) if (!RUNS.containsKey(i)) { slot = i; break; }
        if (slot < 0) return "hukhong.bicanh.err.busy";

        BlockPos origin = new BlockPos(8 + slot * BiCanhLayout.SLOT_SPACING, 64, 0);
        Run run = new Run(slot, tier, dim.random.nextLong(), origin, server.getTickCount() + tier.ticks());
        BiCanhArena.build(dim, origin, tier, run.seed, run.rooms);
        RUNS.put(slot, run);

        List<ServerPlayer> members = new ArrayList<>();
        members.add(starter);
        for (ServerPlayer p : party) {
            if (p == starter || members.size() >= tier.maxParty || runOf(p) != null || p.level().dimension() == BiCanhRegistry.DIM) continue;
            if (!ignoreGate && !tier.allows(CultivationService.get(p).level())) continue;
            members.add(p);
        }
        run.partyAtStart = members.size();
        for (ServerPlayer p : members) {
            Ret r = new Ret(p.level().dimension(), p.getX(), p.getY(), p.getZ(), p.getYRot());
            run.ret.put(p.getUUID(), r);
            CompoundTag t = new CompoundTag();
            t.putString("dim", r.dim().location().toString());
            t.putDouble("x", r.x()); t.putDouble("y", r.y()); t.putDouble("z", r.z());
            p.getPersistentData().put(RET_TAG, t);
            run.players.add(p.getUUID());
            run.bar.addPlayer(p);
            p.teleportTo(dim, origin.getX() + 3.5, origin.getY(), origin.getZ() + 0.5, -90f, 0f);
            p.sendSystemMessage(Component.translatable("hukhong.bicanh.enter", Component.translatable(tier.nameKey()), tier.minutes));
        }
        return null;
    }

    // ---------------------------------------------------------------- thoát

    private static Ret retOf(ServerPlayer p, Run run) {
        Ret r = run == null ? null : run.ret.get(p.getUUID());
        if (r != null) return r;
        CompoundTag t = p.getPersistentData().getCompound(RET_TAG);
        if (t.contains("dim")) {
            ResourceLocation id = ResourceLocation.tryParse(t.getString("dim"));
            if (id != null) return new Ret(ResourceKey.create(Registries.DIMENSION, id), t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), 0f);
        }
        return null;
    }

    /** Đưa người chơi về điểm vào (hoặc spawn Overworld nếu mất dữ liệu). */
    static void evacuate(ServerPlayer p, Run run, String msgKey) {
        Ret r = retOf(p, run);
        MinecraftServer s = p.server;
        ServerLevel to = r == null ? null : s.getLevel(r.dim());
        if (to == null || r.dim() == BiCanhRegistry.DIM) {
            to = s.overworld();
            BlockPos sp = to.getSharedSpawnPos();
            p.teleportTo(to, sp.getX() + 0.5, to.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.getX(), sp.getZ()), sp.getZ() + 0.5, 0f, 0f);
        } else {
            p.teleportTo(to, r.x(), r.y(), r.z(), r.yRot(), 0f);
        }
        com.hukhong.audio.BossMusic.stop(p);
        p.getPersistentData().remove(RET_TAG);
        p.fallDistance = 0;
        p.setDeltaMovement(Vec3.ZERO);
        if (run != null) run.bar.removePlayer(p);
        if (msgKey != null) p.sendSystemMessage(Component.translatable(msgKey));
    }

    public static void leave(ServerPlayer p, String msgKey) {
        Run run = runOf(p);
        if (run == null) {
            if (p.level().dimension() == BiCanhRegistry.DIM) evacuate(p, null, msgKey);
            return;
        }
        run.players.remove(p.getUUID());
        evacuate(p, run, msgKey);
    }

    /** Chặn chết trong Bí Cảnh: hồi nửa máu rồi đưa ra ngoài. */
    public static boolean rescue(ServerPlayer p) {
        if (p.level().dimension() != BiCanhRegistry.DIM) return false;
        p.setHealth(Math.max(1f, p.getMaxHealth() * 0.5f));
        p.removeAllEffects();
        leave(p, "hukhong.bicanh.rescued");
        return true;
    }

    private static void end(MinecraftServer s, Run run) {
        ServerLevel dim = s.getLevel(BiCanhRegistry.DIM);
        for (UUID id : new ArrayList<>(run.players)) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) evacuate(p, run, run.won ? "hukhong.bicanh.done" : "hukhong.bicanh.timeout");
        }
        run.players.clear();
        run.bar.removeAllPlayers();
        if (dim != null) {
            for (UUID id : run.mobs) { Entity e = dim.getEntity(id); if (e != null) e.discard(); }
            BiCanhArena.clear(dim, run.origin, run.rooms);
        }
        RUNS.remove(run.slot);
    }

    // ---------------------------------------------------------------- tick

    public static void tick(MinecraftServer s) {
        if (RUNS.isEmpty()) return;
        ServerLevel dim = s.getLevel(BiCanhRegistry.DIM);
        for (Run run : new ArrayList<>(RUNS.values())) {
            if (dim == null) { end(s, run); continue; }
            tickRun(s, dim, run);
        }
    }

    private static void tickRun(MinecraftServer s, ServerLevel dim, Run run) {
        long now = s.getTickCount();
        List<ServerPlayer> online = new ArrayList<>();
        for (UUID id : new ArrayList<>(run.players)) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null || p.level() != dim) {
                run.players.remove(id);
                if (p != null) run.bar.removePlayer(p);
            } else online.add(p);
        }
        if (online.isEmpty() || now >= run.endTick) { end(s, run); return; }

        long left = run.endTick - now;
        run.bar.setProgress(Math.max(0f, Math.min(1f, (float) left / run.tier.ticks())));
        if (now % 20 == 0) {
            long sec = left / 20;
            run.bar.setName(Component.translatable("hukhong.bicanh.bar", Component.translatable(run.tier.nameKey()),
                    String.format("%02d:%02d", sec / 60, sec % 60)));
        }

        tickDoors(dim, run, now);
        for (ServerPlayer p : online) confine(dim, run, p, now);

        if (run.active < 0) {
            for (ServerPlayer p : online) {
                int idx = BiCanhLayout.roomAt(run.rooms, p.getBlockX() - run.origin.getX());
                if (idx > 0 && run.state[idx] == 0 && Math.abs(p.getBlockZ() - run.origin.getZ()) <= BiCanhLayout.HALF_Z) {
                    activate(dim, run, idx, online);
                    break;
                }
            }
        } else {
            run.mobs.removeIf(id -> { Entity e = dim.getEntity(id); return e == null || !e.isAlive(); });
            if (run.mobs.isEmpty()) cleared(dim, run, run.active, online);
        }
    }

    private static void activate(ServerLevel dim, Run run, int idx, List<ServerPlayer> online) {
        Room r = run.rooms.get(idx);
        run.state[idx] = 1;
        run.active = idx;
        double scale = BiCanhTier.partyScale(online.size());
        if (r.kind() == Kind.BOSS) {
            spawn(dim, run, ModEntities.HU_COT.get(), r, BiCanhLayout.ROOM_LEN / 2, 0, 5.0 * scale, 1.8, true);
            for (ServerPlayer p : online) com.hukhong.audio.BossMusic.start(p, "bicanh_boss");
            for (int i = 0; i < 2 + run.tier.ordinal() / 2; i++) spawnRandom(dim, run, r);
            for (ServerPlayer p : online) p.sendSystemMessage(Component.translatable("hukhong.bicanh.boss_start"));
            floorTitle(dim, online, Component.translatable("hukhong.bicanh.floor.boss"), Component.translatable("hukhong.bicanh.boss"), true);
        } else {
            int n = (int) Math.round(run.tier.mobsForWave(idx - 1) * scale);
            for (int i = 0; i < n; i++) spawnRandom(dim, run, r);
            for (ServerPlayer p : online)
                p.sendSystemMessage(Component.translatable("hukhong.bicanh.wave_start", idx, run.tier.waves, n));
            floorTitle(dim, online, Component.translatable("hukhong.bicanh.floor", idx),
                    Component.translatable("hukhong.bicanh.floor.sub", idx, run.tier.waves, n), false);
        }
    }

    private static void spawnRandom(ServerLevel dim, Run run, Room r) {
        var rnd = dim.random;
        double w = rnd.nextDouble();
        EntityType<? extends Mob> type = w < 0.60 ? ModEntities.HU_THI.get() : w < 0.85 ? ModEntities.HU_COT.get() : ModEntities.HU_CHU.get();
        int L = BiCanhLayout.ROOM_LEN, hz = BiCanhLayout.HALF_Z;
        int lx = L / 2, lz = hz - 3; // du phong: gan tuong
        for (int i = 0; i < 24; i++) {
            int tx = 3 + rnd.nextInt(L - 7), tz = -(hz - 2) + rnd.nextInt(2 * (hz - 2) + 1);
            BlockPos at = new BlockPos(run.origin.getX() + r.x0() + tx, run.origin.getY(), run.origin.getZ() + tz);
            if (dim.getBlockState(at).isAir() && dim.getBlockState(at.above()).isAir()
                    && dim.getBlockState(at.below()).blocksMotion() && dim.getFluidState(at.below()).isEmpty()) { lx = tx; lz = tz; break; }
        }
        spawn(dim, run, type, r, lx, lz, 1.0, 1.0, false);
    }

    private static void spawn(ServerLevel dim, Run run, EntityType<? extends Mob> type, Room r, int lx, int lz,
                              double hpMul, double dmgMul, boolean boss) {
        Mob m = type.create(dim);
        if (m == null) return;
        BlockPos o = run.origin;
        m.moveTo(o.getX() + r.x0() + lx + 0.5, o.getY() + (boss ? 1 : 0), o.getZ() + lz + 0.5, dim.random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(dim, dim.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
        HuHoa.applyStats(m, run.tier.mobLevel, hpMul, dmgMul);
        m.setPersistenceRequired();
        if (boss) {
            com.hukhong.audio.BossMusic.watch(m, "bicanh_boss");
            m.setCustomName(com.hukhong.entity.BossLevel.tag(Component.translatable("hukhong.bicanh.boss"), run.tier.mobLevel));
            m.setCustomNameVisible(true);
            m.setGlowingTag(true);
        }
        dim.addFreshEntity(m);
        run.mobs.add(m.getUUID());
    }

    private static void cleared(ServerLevel dim, Run run, int idx, List<ServerPlayer> online) {
        Room r = run.rooms.get(idx);
        run.state[idx] = 2;
        run.active = -1;
        if (r.kind() == Kind.WAVE) {
            run.doorAnims.add(new long[]{idx, dim.getServer().getTickCount()});
            burst(dim, run, r);
            for (ServerPlayer p : online) {
                p.sendSystemMessage(Component.translatable("hukhong.bicanh.wave_clear", idx));
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("hukhong.bicanh.floor_clear.sub")));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("hukhong.bicanh.floor_clear", idx)));
            }
            return;
        }
        burst(dim, run, r);
        for (ServerPlayer p : online) com.hukhong.audio.BossMusic.stop(p);
        run.won = true;
        run.endTick = Math.min(run.endTick, dim.getServer().getTickCount() + VICTORY_GRACE_TICKS);
        BlockPos o = run.origin;
        int cx = o.getX() + r.x0() + BiCanhLayout.ROOM_LEN - 3;
        dim.setBlock(new BlockPos(o.getX() + r.x0() + 2, o.getY(), o.getZ()), BiCanhRegistry.EXIT.get().defaultBlockState(), 3);
        Random rnd = new Random(dim.random.nextLong());
        int n = Math.min(online.size(), 10);
        for (int i = 0; i < n; i++) {
            BlockPos cp = new BlockPos(cx, o.getY(), o.getZ() - n / 2 + i);
            dim.setBlock(cp, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), 3);
            if (dim.getBlockEntity(cp) instanceof ChestBlockEntity ce) {
                List<ItemStack> loot = BiCanhLoot.stacks(run.tier, rnd);
                loot = new java.util.ArrayList<>(loot);
                loot.addAll(com.hukhong.currency.Wallet.stacks(com.hukhong.currency.TuTinh.bicanhChest(run.tier.ordinal(), rnd)));
                for (int k = 0; k < loot.size() && k < ce.getContainerSize(); k++) ce.setItem(k, loot.get(k));
            }
        }
        for (ServerPlayer p : online) com.hukhong.guild.GuildService.onBiCanhClear(p, run.tier.ordinal());
        for (ServerPlayer p : online) p.sendSystemMessage(Component.translatable("hukhong.bicanh.victory", VICTORY_GRACE_TICKS / 20));
    }

    // ---------------------------------------------------------------- hieu ung chuyen tang / ranh gioi

    private static void floorTitle(ServerLevel dim, List<ServerPlayer> online, Component title, Component sub, boolean boss) {
        for (ServerPlayer p : online) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(8, 40, 14));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
            dim.playSound(null, p.blockPosition(), boss ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.EVOKER_PREPARE_SUMMON,
                    SoundSource.HOSTILE, boss ? 0.6f : 0.8f, boss ? 0.8f : 1.0f);
        }
    }

    /** Chum sang + xung kich tai tam phong vua don. */
    private static void burst(ServerLevel dim, Run run, Room r) {
        BlockPos c = BiCanhArena.roomCenter(run.origin, r);
        double y = c.getY() + 1.5;
        dim.sendParticles(ParticleTypes.FLASH, c.getX() + 0.5, y, c.getZ() + 0.5, 1, 0, 0, 0, 0);
        dim.sendParticles(ParticleTypes.END_ROD, c.getX() + 0.5, y, c.getZ() + 0.5, 160, 0.3, 0.6, 0.3, 0.45);
        dim.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.getX() + 0.5, y, c.getZ() + 0.5, 60, 4.0, 1.0, 4.0, 0.2);
        dim.playSound(null, c, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        dim.playSound(null, c, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    /** Hoat anh mo cua: nhac tung hang song sat tu duoi len, moi 4 tick. */
    private static void tickDoors(ServerLevel dim, Run run, long now) {
        if (run.doorAnims.isEmpty()) return;
        for (java.util.Iterator<long[]> it = run.doorAnims.iterator(); it.hasNext(); ) {
            long[] a = it.next();
            long dt = now - a[1];
            if (dt < 0 || dt % 4 != 0) continue;
            int row = (int) (dt / 4);
            Room r = run.rooms.get((int) a[0]);
            if (row >= BiCanhArena.DOOR_ROWS) { it.remove(); continue; }
            BiCanhArena.setDoorRow(dim, run.origin, r, row, false);
            BlockPos d = new BlockPos(run.origin.getX() + r.x0() + BiCanhLayout.ROOM_LEN, run.origin.getY() + row, run.origin.getZ());
            dim.playSound(null, d, row == 0 ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 0.6f + 0.1f * row);
            dim.sendParticles(ParticleTypes.CLOUD, d.getX() + 0.5, d.getY() + 0.5, d.getZ() + 0.5, 6, 0.2, 0.2, 1.6, 0.02);
        }
    }

    /** Ranh gioi vo hinh: khong ra khoi the tich Bi Canh; khi dang danh khong qua duoc cua dong. Tuong hat mo gan ranh gioi. */
    private static void confine(ServerLevel dim, Run run, ServerPlayer p, long now) {
        BlockPos o = run.origin;
        int hz = BiCanhLayout.HALF_Z, L = BiCanhLayout.ROOM_LEN;
        double minX = o.getX() + 0.3;
        double maxX = o.getX() + BiCanhLayout.endX(run.rooms) - 0.3;
        if (run.active >= 0 && run.rooms.get(run.active).kind() != Kind.BOSS)
            maxX = o.getX() + run.rooms.get(run.active).x0() + L - 0.3;
        double zr = hz + 3.7;
        double minZ = o.getZ() - zr, maxZ = o.getZ() + 1 + zr;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double nx = Math.max(minX, Math.min(maxX, x)), nz = Math.max(minZ, Math.min(maxZ, z));
        double ny = y;
        boolean vert = y < o.getY() - 4 || y > o.getY() + BiCanhLayout.HEIGHT + 3;
        if (vert) { ny = o.getY() + 1; nx = o.getX() + 3.5; nz = o.getZ() + 0.5; }
        if (nx != x || nz != z || ny != y) {
            p.teleportTo(dim, nx, ny, nz, p.getYRot(), p.getXRot());
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0;
            p.hurtMarked = true;
            dim.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 0.6f);
            return;
        }
        if (now % 6 != 0 || run.active < 0) return;
        if (maxX - x < 3.0) // tuong hat truoc cua dong
            dim.sendParticles(p, ParticleTypes.ELECTRIC_SPARK, false, maxX + 0.2, y + 1.2, z, 3, 0.05, 0.9, 1.4, 0.01);
    }
}
