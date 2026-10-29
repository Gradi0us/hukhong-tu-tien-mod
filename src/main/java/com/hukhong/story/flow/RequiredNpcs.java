package com.hukhong.story.flow;

import com.hukhong.HuKhong;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.story.npc.Dialogue;
import com.hukhong.story.npc.DialogueManager;
import com.hukhong.story.npc.NpcPlacement;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.npc.StoryNpcRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registry cac NPC bat buoc cua tuyen chinh theo buoc truyen (TALK / DUEL): id NPC + thanh nha (lay tu bang thoai; rong = di theo nguoi choi).
 * Kiem tra dinh ky (nguoi choi trong 150 block quanh nha): thieu / chet / mat -> sinh lai, khong trung (dedupe theo vung + so cai UUID).
 * Trieu hoi thu cong: {@link #resummon} (60s hoi chieu), dung cho nut nhat ky + lenh /hukhong quest npc.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class RequiredNpcs {
    private RequiredNpcs() {}

    public static final int CHECK_RADIUS = 150, RESUMMON_COOLDOWN = 1200;
    private static final String NAME = "hukhong_story_npc_ledger";

    /** city rong = di theo nguoi choi (khong co nha). */
    public record Req(String stepId, String npc, String city, boolean duel) {
        public boolean follower() { return city.isEmpty(); }
    }

    public enum Result { PRESENT, SPAWNED, MOVED, SKIPPED }

    /** So cai UUID NPC nha (npcId -> UUID) de tim lai / chong trung khi NPC bi day di xa. */
    public static final class Ledger extends SavedData {
        private final Map<String, UUID> ids = new LinkedHashMap<>();

        public static Ledger get(ServerLevel l) {
            return l.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Ledger::new, Ledger::load, null), NAME);
        }

        static Ledger load(CompoundTag t, HolderLookup.Provider reg) {
            Ledger d = new Ledger();
            CompoundTag c = t.getCompound("ids");
            for (String k : c.getAllKeys()) if (c.hasUUID(k)) d.ids.put(k, c.getUUID(k));
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
            CompoundTag c = new CompoundTag();
            ids.forEach(c::putUUID);
            t.put("ids", c);
            return t;
        }

        public UUID get(String npc) { return ids.get(npc); }
        public void put(String npc, UUID id) { if (!id.equals(ids.put(npc, id))) setDirty(); }
    }

    // ------------------------------------------------------------------ registry
    /** NPC bat buoc cua buoc (null neu buoc khong can NPC). */
    public static Req forStep(FlowSteps.Step s) {
        if (s == null || (s.kind() != FlowSteps.Kind.TALK && s.kind() != FlowSteps.Kind.DUEL) || s.arg().isEmpty()) return null;
        Dialogue d = DialogueManager.get(s.arg());
        return new Req(s.id(), s.arg(), d == null ? "" : d.city(), s.kind() == FlowSteps.Kind.DUEL);
    }

    /** Toan bo bang: buoc -> NPC bat buoc. */
    public static Map<String, Req> all() {
        Map<String, Req> m = new LinkedHashMap<>();
        for (FlowSteps.Step s : FlowSteps.ALL) { Req r = forStep(s); if (r != null) m.put(s.id(), r); }
        return m;
    }

    // ------------------------------------------------------------------ sinh / dedupe
    private static BlockPos homePos(ServerLevel sl, BlockPos center) {
        return NpcPlacement.plazaStand(sl, center, 2, 9, 0);
    }

    public static StoryNpc create(ServerLevel sl, String npcId, BlockPos pos) {
        StoryNpc n = StoryNpcRegistry.STORY_NPC.get().create(sl);
        if (n == null) return null;
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        n.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        n.setNpcId(npcId);
        n.setPersistenceRequired();
        if (!sl.addFreshEntity(n)) { com.mojang.logging.LogUtils.getLogger().warn("StoryNpc {} add failed at {}", npcId, pos); return null; }
        return n;
    }

    /** NPC nha tai thanh (tam center): co roi -> giu 1 (xoa ban sao); mat -> keo ve neu ton tai noi khac, khong thi sinh moi. */
    public static Result ensureAt(ServerLevel sl, String npcId, BlockPos center, boolean requireTicking) {
        if (!sl.hasChunkAt(center) || (requireTicking && !sl.isPositionEntityTicking(center))) return Result.SKIPPED;
        Ledger led = Ledger.get(sl);
        AABB box = new AABB(center).inflate(64, 40, 64);
        List<StoryNpc> found = new ArrayList<>(sl.getEntitiesOfClass(StoryNpc.class, box, n -> n.isAlive() && n.npcId().equals(npcId)));
        if (!found.isEmpty()) {
            StoryNpc keep = found.get(0);
            for (StoryNpc n : found) if (n.getUUID().equals(led.get(npcId))) { keep = n; break; }
            for (StoryNpc n : found) if (n != keep) n.discard();
            led.put(npcId, keep.getUUID());
            return Result.PRESENT;
        }
        BlockPos pos = homePos(sl, center);
        UUID known = led.get(npcId);
        Entity far = known == null ? null : sl.getEntity(known);
        if (far instanceof StoryNpc n && n.isAlive()) {
            n.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            n.setHome(center);
            return Result.MOVED;
        }
        StoryNpc n = create(sl, npcId, pos);
        if (n == null) return Result.SKIPPED;
        n.setHome(center);
        led.put(npcId, n.getUUID());
        return Result.SPAWNED;
    }

    // ------------------------------------------------------------------ kiem tra dinh ky
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 47 || !StoryFlow.active(p)) return;
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        Req r = forStep(StoryFlow.current(p));
        if (r == null || r.duel()) return; // DUEL: StoryFlow.maintain lo
        if (r.follower()) {
            boolean has = StoryFlow.flowEntities(p, 128).stream().anyMatch(x -> x instanceof StoryNpc n && n.npcId().equals(r.npc()));
            if (!has) StoryFlow.spawnNpc(p, r.npc(), 4);
            return;
        }
        CityDef city = CityDef.byId(r.city());
        BlockPos c = city == null ? null : CityService.builtCenter(sl, city);
        if (c == null) return;
        double dx = p.getX() - c.getX(), dz = p.getZ() - c.getZ();
        if (dx * dx + dz * dz > (double) CHECK_RADIUS * CHECK_RADIUS) return;
        ensureAt(sl, r.npc(), c, true);
    }

    // ------------------------------------------------------------------ trieu hoi thu cong
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { COOLDOWN.remove(e.getEntity().getUUID()); }

    /** Trieu hoi lai NPC cua buoc hien tai canh nguoi choi neu khong thay gan nha. Tra true neu da sinh / keo NPC. */
    public static boolean resummon(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        if (!StoryFlow.active(p)) { say(p, "hukhong.quest.npc.not_story"); return false; }
        Req r = forStep(StoryFlow.current(p));
        if (r == null) { say(p, "hukhong.quest.npc.none"); return false; }
        long now = sl.getGameTime();
        Long ready = COOLDOWN.get(p.getUUID());
        if (ready != null && ready > now) {
            p.displayClientMessage(Component.translatable("hukhong.quest.npc.cooldown", (ready - now + 19) / 20), true);
            return false;
        }
        Ledger led = Ledger.get(sl);
        if (r.follower() || r.duel()) {
            boolean has = StoryFlow.flowEntities(p, 128).stream().anyMatch(x -> x instanceof StoryNpc n && n.npcId().equals(r.npc()) && n.isAlive());
            if (has) { say(p, "hukhong.quest.npc.found_near"); return false; }
            if (StoryFlow.spawnNpc(p, r.npc(), 3) == null) return false;
        } else {
            CityDef city = CityDef.byId(r.city());
            BlockPos c = city == null ? null : CityService.builtCenter(sl, city);
            if (c != null && sl.hasChunkAt(c)) {
                AABB box = new AABB(c).inflate(64, 40, 64);
                boolean near = !sl.getEntitiesOfClass(StoryNpc.class, box, n -> n.isAlive() && n.npcId().equals(r.npc())).isEmpty();
                if (near) { p.displayClientMessage(Component.translatable("hukhong.quest.npc.found_home", c.getX(), c.getZ()), false); return false; }
            }
            UUID known = led.get(r.npc());
            Entity far = known == null ? null : sl.getEntity(known);
            if (far instanceof StoryNpc n && n.isAlive()) {
                n.teleportTo(p.getX() + 1.5, p.getY(), p.getZ());
            } else {
                BlockPos pos = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.blockPosition().offset(2, 0, 0));
                StoryNpc n = create(sl, r.npc(), pos);
                if (n == null) return false;
                led.put(r.npc(), n.getUUID());
            }
        }
        COOLDOWN.put(p.getUUID(), now + RESUMMON_COOLDOWN);
        say(p, "hukhong.quest.npc.summoned");
        return true;
    }

    private static void say(ServerPlayer p, String key) { p.displayClientMessage(Component.translatable(key), true); }
}
