package com.hukhong.story.chain;

import com.hukhong.HuKhong;
import com.hukhong.chg.ChgService;
import com.hukhong.city.CityDef;
import com.hukhong.city.CityService;
import com.hukhong.core.CultivationService;
import com.hukhong.endgame.EndgameService;
import com.hukhong.endgame.WorldTierData;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.CityRetake;
import com.hukhong.events.CityStates;
import com.hukhong.pvp.ArenaBuilder;
import com.hukhong.story.StoryService;
import com.hukhong.story.SuDo;
import com.hukhong.story.chain.CityArcKit.Arc;
import com.hukhong.story.flow.RequiredNpcs;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.NpcPlacement;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.world.Faction;
import com.hukhong.story.world.Lore;
import com.hukhong.story.world.StoryHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Engine 7 cung truyen thanh (docs/DESIGN-v2-quest-chains.md, "Cung truyen thanh"). Moi cung la mot chuoi QuestChains 3 buoc;
 * NPC giao viec dung o thanh (Dialogue JSON, DialogueService sinh qua NpcPlacement). Quy tac chuyen buoc nam o {@link CityArcKit#advance};
 * lop nay dien ngu canh tu the gioi (moi 1s), sinh cuoc cham tran / canh dien (chi trong RAM: relog thi sinh lai khi nguoi choi den gan), va phat thuong.
 * Cung la tuy chon: khong co buoc nao cua tuyen chinh phu thuoc vao day (StoryFlow chi dung {@link #duelSpot}, {@link #arenaTarget}, {@link #modaoSpot} de dinh vi).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class CityArcs {
    private CityArcs() {}

    public static final String TAG_ARC = "hk_arc", TAG_SCENE = "hk_arc_scene", TAG_PEARL = "hk_pearl", TAG_ACTOR = "hk_arc_actor";
    public static final int SCENE_LINES = 8, SCENE_LINE_TICKS = 60, SPAWN_NEAR = 110, DUEL_NEAR = 200, MODAO_NEAR = 150;

    /** uuid -> "arc:stage" da sinh cuoc cham tran trong phien nay. */
    private static final Map<UUID, String> ENC = new HashMap<>();
    /** uuid -> {tick bat dau, so dong da phat}. */
    private static final Map<UUID, long[]> SCENE = new HashMap<>();
    private static final Map<UUID, Long> PEARL_AT = new HashMap<>();

    // ------------------------------------------------------------------ tien ich
    private static QuestData quest(ServerPlayer p) { return DialogueService.quest(p); }

    static BlockPos center(ServerLevel ow, Arc a) {
        CityDef d = CityDef.byId(a.cityId());
        return d == null ? null : CityService.builtCenter(ow, d);
    }

    private static double d2(ServerPlayer p, BlockPos t) {
        double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
        return dx * dx + dz * dz;
    }

    private static int level(ServerPlayer p) { return Math.max(15, Math.min(StoryService.MAX_LEVEL, CultivationService.get(p).level())); }

    private static void setAt(ServerPlayer p, String chain, int x, int z) {
        QuestData q = quest(p);
        String old = q.valueOf("qc." + chain + ".at:");
        if (!old.isEmpty()) q = q.withoutFlag("qc." + chain + ".at:" + old);
        DialogueService.setQuest(p, q.withFlag("qc." + chain + ".at:" + x + ":" + z));
    }

    /** Dich quanh tam thanh, dat lien (khong bien/song). */
    private static void pickFrom(ServerPlayer p, String chain, BlockPos c, int dmin, int dmax) {
        int x = c.getX(), z = c.getZ();
        for (int tries = 0; tries < 32; tries++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2, r = dmin + p.getRandom().nextInt(Math.max(1, dmax - dmin));
            x = (int) Math.round(c.getX() + Math.cos(a) * r);
            z = (int) Math.round(c.getZ() + Math.sin(a) * r);
            if (QuestChains.dryLand(p.serverLevel(), x, z)) break;
        }
        setAt(p, chain, x, z);
    }

    // ------------------------------------------------------------------ hook tu QuestChains
    /** Vao buoc n cua mot cung: dat dich / sinh canh. Goi tu QuestChains.onStage. */
    static void onStage(ServerPlayer p, String chain, int n) {
        Arc a = CityArcKit.byId(chain);
        if (a == null) return;
        reset(p);
        if (!(p.level() instanceof ServerLevel sl)) return;
        BlockPos c = center(sl, a);
        switch (chain + ":" + n) {
            case CityArcKit.CHO_MAU + ":2" -> { if (c != null) pickFrom(p, chain, c, 110, 150); }
            case CityArcKit.DAU_GIA + ":2" -> { if (c != null) pickFrom(p, chain, c, 90, 130); }
            case CityArcKit.KIEM_TRUNG + ":1" -> { if (c != null) pickFrom(p, chain, c, 90, 140); }
            case CityArcKit.LOI_VU + ":1" -> { int[] xz = ChgService.lairXZ(sl.getServer().overworld(), 1); setAt(p, chain, xz[0], xz[1]); }
            case CityArcKit.HOANG_DO + ":1" -> { if (c != null) setAt(p, chain, c.getX(), c.getZ()); }
            default -> { }
        }
    }

    /** Hoan thanh cung: thuong + co + Van Kien + loi Mo Dao. Goi tu QuestChains.setStage(n>=9). */
    static void onDone(ServerPlayer p, String chain) {
        Arc a = CityArcKit.byId(chain);
        if (a == null) return;
        reset(p);
        CultivationService.addExp(p, a.exp());
        Lore lore = new Lore(factionOf(chain), 0, 99, List.of(), a.setsFlags(), a.docs().get(0), null, a.modaoKey());
        StoryHooks.complete(p, lore);
        for (int i = 1; i < a.docs().size(); i++) StoryService.collect(p, a.docs().get(i));
        if (p.level() instanceof ServerLevel sl) {
            ServerLevel ow = sl.getServer().overworld();
            switch (chain) {
                case CityArcKit.LOI_VU -> ChgService.revealLair(p, 1);
                case CityArcKit.KIEM_TRUNG -> ChgService.revealLair(p, 3);
                case CityArcKit.HOANG_DO -> restoreGateway(ow, p, a);
                default -> { }
            }
        }
    }

    static void cleanup(ServerPlayer p, String chain) {
        if (CityArcKit.isArc(chain)) reset(p);
    }

    static Faction factionOf(String chain) {
        return switch (chain) {
            case CityArcKit.CHO_MAU, CityArcKit.DAU_GIA -> Faction.DA_KHUYET;
            case CityArcKit.LOI_VU -> Faction.CHG;
            case CityArcKit.DAI_HOI -> Faction.TONG_MON;
            case CityArcKit.KIEM_TRUNG -> Faction.TIEN_NHAN;
            default -> Faction.THANH;
        };
    }

    /** Hoang Do tai chiem xong: dung lai cong Hu Khong canh thanh (c10_gate) neu chua co. */
    private static void restoreGateway(ServerLevel ow, ServerPlayer p, Arc a) {
        WorldTierData wd = WorldTierData.get(ow);
        BlockPos c = center(ow, a);
        if (wd.gateway() != null || c == null) return;
        BlockPos ground = ow.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.offset(16, 0, 0));
        EndgameService.openGateway(ow, ground);
        p.sendSystemMessage(Component.translatable("hukhong.qc.hoang_do.gateway", ground.getX(), ground.getY(), ground.getZ()));
    }

    /** Song sot Loi Vu gan hang CHG {@code lair}: tien cung loi_vu (goi tu LoiVuHandler.end). */
    public static void onLoiVuSurvived(ServerPlayer p, int lair) {
        if (lair != 1 || !DialogueService.storyMode(p)) return;
        int st = QuestChains.stageOf(quest(p).flags(), CityArcKit.LOI_VU);
        if (st != 1 && st != 2) return;
        DialogueService.setQuest(p, quest(p).withFlag(QuestChains.choiceFlag(CityArcKit.LOI_VU, "storm")));
        p.sendSystemMessage(Component.translatable("hukhong.qc.loi_vu.survived"));
    }

    // ------------------------------------------------------------------ vi tri cho StoryFlow (huong dan, khong chan)
    /** San Dai Hoi (ArenaBuilder) cua Thanh Van Thanh: dung neu chua co, null neu thanh chua dung / nguoi choi xa (&gt; DUEL_NEAR). */
    public static BlockPos duelSpot(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return null;
        BlockPos c = center(sl, CityArcKit.byId(CityArcKit.DAI_HOI));
        if (c == null || d2(p, c) > (double) DUEL_NEAR * DUEL_NEAR) return null;
        BlockPos arena = ensureArena(sl, c);
        return arena == null ? null : ArenaBuilder.spotB(arena);
    }

    /** Muc tieu chi duong cua c8_rival: san Dai Hoi (uoc luong, khong nap chunk) hoac tam thanh; null neu thanh chua dung. */
    public static BlockPos arenaTarget(ServerLevel ow) {
        BlockPos c = center(ow, CityArcKit.byId(CityArcKit.DAI_HOI));
        if (c == null) return null;
        int[] xz = ArenaBuilder.siteXZ(c);
        return new BlockPos(xz[0], c.getY(), xz[1]);
    }

    /** Noi Mo Dao roi di / hy sinh (c9_modao): Bac Han Thanh khi da dung, nguoc lai null (StoryFlow roi ve Luu Ly Cang). */
    public static BlockPos modaoSpot(ServerLevel ow) {
        return center(ow, CityArcKit.byId(CityArcKit.KIEM_TRUNG));
    }

    private static BlockPos ensureArena(ServerLevel ow, BlockPos cityCenter) {
        int[] xz = ArenaBuilder.siteXZ(cityCenter);
        if (!ow.hasChunk(xz[0] >> 4, xz[1] >> 4)) return null;
        int y0 = Math.max(ow.getSeaLevel() + 1, ow.getHeight(Heightmap.Types.WORLD_SURFACE_WG, xz[0], xz[1]));
        BlockPos mid = new BlockPos(xz[0], y0 - 1, xz[1]);
        if (!ow.getBlockState(mid).is(Blocks.CRYING_OBSIDIAN)) return ArenaBuilder.build(ow, cityCenter);
        return new BlockPos(xz[0], y0, xz[1]);
    }

    // ------------------------------------------------------------------ tick moi giay
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 1 || !DialogueService.storyMode(p)) return;
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        for (Arc a : CityArcKit.ARCS) {
            int st = QuestChains.stageOf(quest(p).flags(), a.id());
            if (st > 0) step(p, sl, a, st);
        }
        if (p.tickCount % 100 == 61) hint(p);
        modaoScene(p, sl);
    }

    /** Mot nhip cua mot cung: tinh ngu canh, chuyen buoc neu du dieu kien, nguoc lai duy tri canh. */
    static void step(ServerPlayer p, ServerLevel sl, Arc a, int st) {
        int next = CityArcKit.advance(a.id(), st, context(p, sl, a, st));
        if (next != st) {
            if (a.id().equals(CityArcKit.HAC_TRIEU) && st == 2) takePearls(p, CityArcKit.PEARLS_NEEDED);
            QuestChains.setStage(p, a.id(), next);
            return;
        }
        maintain(p, sl, a, st);
    }

    static CityArcKit.Ctx context(ServerPlayer p, ServerLevel sl, Arc a, int st) {
        QuestData q = quest(p);
        BlockPos c = center(sl, a);
        boolean near = c != null && d2(p, c) < (double) CityArcKit.NEAR_CITY * CityArcKit.NEAR_CITY;
        BlockPos t = QuestChains.target(p, a.id());
        double reach = a.id().equals(CityArcKit.LOI_VU) ? 60 : CityArcKit.REACH;
        boolean reached = t != null && QuestChains.dist2(p, t) < reach * reach;
        boolean cleared = ENC.get(p.getUUID()) != null && ENC.get(p.getUUID()).equals(a.id() + ":" + st) && QuestChains.alive(p, TAG_ARC, 160) == 0;
        boolean retaken = false;
        if (a.id().equals(CityArcKit.HOANG_DO)) {
            CityDef d = CityDef.byId(a.cityId());
            retaken = d != null && c != null && !CityStates.isRuin(sl.getServer().overworld(), d);
        }
        CityArcKit.Ctx ctx = CityArcKit.Ctx.NONE
                .withNight(CityArcKit.isNight(sl.getDayTime())).withNearCity(a.id().equals(CityArcKit.HOANG_DO) ? c != null && d2(p, c) < 100.0 * 100.0 : near)
                .withReached(reached).withCleared(cleared).withPearls(a.id().equals(CityArcKit.HAC_TRIEU) ? countPearls(p) : 0)
                .withScene(q.has(QuestChains.choiceFlag(a.id(), "scene"))).withStorm(q.has(QuestChains.choiceFlag(a.id(), "storm")))
                .withRival(q.has("d:c8_rival")).withModao(q.has("d:c9_modao")).withRetaken(retaken);
        return ctx;
    }

    /** Cho GameTest/lenh: ap mot ngu canh gia lap len cung cua nguoi choi (khong sinh quai). Tra buoc moi. */
    public static int simulate(ServerPlayer p, String chain, CityArcKit.Ctx ctx) {
        int st = QuestChains.stageOf(quest(p).flags(), chain);
        if (st == 0) return 0;
        int next = CityArcKit.advance(chain, st, ctx);
        if (next != st) {
            if (chain.equals(CityArcKit.HAC_TRIEU) && st == 2) takePearls(p, CityArcKit.PEARLS_NEEDED);
            QuestChains.setStage(p, chain, next);
        }
        return next;
    }

    private static void maintain(ServerPlayer p, ServerLevel sl, Arc a, int st) {
        String key = a.id() + ":" + st;
        BlockPos c = center(sl, a);
        switch (key) {
            case CityArcKit.CHO_MAU + ":3", CityArcKit.DAU_GIA + ":3", CityArcKit.KIEM_TRUNG + ":2" -> {
                BlockPos t = QuestChains.target(p, a.id());
                if (t != null) ensureEncounter(p, sl, key, t, a);
            }
            case CityArcKit.HAC_TRIEU + ":1", CityArcKit.DAI_HOI + ":1", CityArcKit.DAI_HOI + ":2" -> {
                if (c != null) {
                    BlockPos anchor = a.id().equals(CityArcKit.DAI_HOI) ? arenaOrCenter(sl, c, p) : c;
                    ensureEncounter(p, sl, key, anchor, a);
                }
            }
            case CityArcKit.HAC_TRIEU + ":2" -> spawnPearls(p, sl, c);
            case CityArcKit.DAU_GIA + ":1" -> scene(p, sl, a, c);
            case CityArcKit.HOANG_DO + ":2" -> { if (c != null) CityRetake.ensure(sl.getServer().overworld(), a.city()); }
            default -> { }
        }
    }

    private static BlockPos arenaOrCenter(ServerLevel sl, BlockPos c, ServerPlayer p) {
        if (d2(p, c) > (double) DUEL_NEAR * DUEL_NEAR) return c;
        BlockPos arena = ensureArena(sl, c);
        return arena != null ? arena : c;
    }

    // ------------------------------------------------------------------ cuoc cham tran
    private static void ensureEncounter(ServerPlayer p, ServerLevel sl, String key, BlockPos anchor, Arc a) {
        if (key.equals(ENC.get(p.getUUID()))) return;
        if (d2(p, anchor) > (double) SPAWN_NEAR * SPAWN_NEAR) return;
        if (spawnEncounter(p, sl, key, anchor)) ENC.put(p.getUUID(), key);
    }

    static boolean spawnEncounter(ServerPlayer p, ServerLevel sl, String key, BlockPos anchor) {
        int lv = level(p);
        int n = 0;
        switch (key) {
            case CityArcKit.CHO_MAU + ":3" -> {
                n += boss(p, sl, anchor, 2, lv, null);
                n += guards(p, sl, anchor, 2, lv, "hukhong.qc.cho_mau.guard");
            }
            case CityArcKit.DAU_GIA + ":3" -> {
                n += boss(p, sl, anchor, 0, lv, "hukhong.qc.dau_gia.buyer");
                n += guards(p, sl, anchor, 1, lv, "hukhong.qc.dau_gia.guard");
            }
            case CityArcKit.KIEM_TRUNG + ":2" -> n += guards(p, sl, anchor, 3, lv, "hukhong.qc.kiem_trung.shade");
            case CityArcKit.DAI_HOI + ":1" -> n += guards(p, sl, anchor, 1, Math.max(15, lv - 4), "hukhong.qc.dai_hoi.disciple");
            case CityArcKit.DAI_HOI + ":2" -> n += guards(p, sl, anchor, 2, Math.max(15, lv - 1), "hukhong.qc.dai_hoi.elite");
            case CityArcKit.HAC_TRIEU + ":1" -> n += raid(p, sl, anchor, 8);
            default -> { }
        }
        return n > 0;
    }

    private static int boss(ServerPlayer p, ServerLevel sl, BlockPos c, int variant, int lv, String nameKey) {
        SuDo s = StoryService.spawn(sl, QuestChains.standAround(sl, p, c, 3, 7), variant, lv);
        if (s == null) return 0;
        tag(p, s, nameKey);
        return 1;
    }

    private static int guards(ServerPlayer p, ServerLevel sl, BlockPos c, int count, int lv, String nameKey) {
        int made = 0;
        for (int i = 0; i < count; i++) {
            SuDo s = StoryService.spawn(sl, QuestChains.standAround(sl, p, c, 4, 9), i % 2, lv);
            if (s == null) continue;
            tag(p, s, nameKey);
            made++;
        }
        return made;
    }

    private static void tag(ServerPlayer p, Mob m, String nameKey) {
        if (nameKey != null) { m.setCustomName(Component.translatable(nameKey)); m.setCustomNameVisible(true); }
        m.setPersistenceRequired();
        m.addTag(TAG_ARC);
        m.addTag(QuestChains.TAG_OWNER + p.getUUID());
        m.setTarget(p);
    }

    /** Quai bien do bo ("Hac Trieu"): Hu mob quanh bo nuoc cua thanh cang. */
    private static int raid(ServerPlayer p, ServerLevel sl, BlockPos c, int count) {
        int made = 0;
        for (int i = 0; i < count; i++) {
            BlockPos s = shore(sl, p, c);
            Mob m = switch (i % 3) {
                case 0 -> ModEntities.HU_THI.get().create(sl);
                case 1 -> ModEntities.HU_COT.get().create(sl);
                default -> ModEntities.HU_CHU.get().create(sl);
            };
            if (m == null) continue;
            m.moveTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(s), MobSpawnType.EVENT, null);
            tag(p, m, null);
            sl.addFreshEntity(m);
            made++;
        }
        return made;
    }

    /** Cho dung gan nuoc (uu tien); khong co thi cho dung bat ky quanh tam. */
    static BlockPos shore(ServerLevel sl, ServerPlayer p, BlockPos c) {
        BlockPos fallback = null;
        for (int tries = 0; tries < 40; tries++) {
            BlockPos s = QuestChains.standAround(sl, p, c, 14, 48);
            if (fallback == null) fallback = s;
            if (nearWater(sl, s)) return s;
        }
        return fallback != null ? fallback : c;
    }

    static boolean nearWater(ServerLevel sl, BlockPos s) {
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            BlockPos q = s.offset((int) Math.round(Math.cos(a) * 5), -1, (int) Math.round(Math.sin(a) * 5));
            if (sl.hasChunkAt(q) && sl.getFluidState(q).is(Fluids.WATER)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ ngoc trai (Hac Trieu buoc 2)
    static ItemStack pearl() {
        ItemStack st = new ItemStack(Items.PRISMARINE_SHARD);
        CompoundTag t = new CompoundTag();
        t.putBoolean(TAG_PEARL, true);
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        st.set(DataComponents.CUSTOM_NAME, Component.translatable("hukhong.qc.hac_trieu.pearl"));
        return st;
    }

    static boolean isPearl(ItemStack st) {
        if (st.isEmpty() || !st.is(Items.PRISMARINE_SHARD)) return false;
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        return cd != null && cd.copyTag().getBoolean(TAG_PEARL);
    }

    static int countPearls(ServerPlayer p) {
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (isPearl(st)) n += st.getCount();
        return n;
    }

    static void takePearls(ServerPlayer p, int n) {
        for (ItemStack st : p.getInventory().items) {
            if (n <= 0) return;
            if (!isPearl(st)) continue;
            int t = Math.min(n, st.getCount());
            st.shrink(t);
            n -= t;
        }
    }

    private static void spawnPearls(ServerPlayer p, ServerLevel sl, BlockPos c) {
        if (c == null || p.tickCount % 200 != 1) return;
        long now = sl.getGameTime();
        Long last = PEARL_AT.get(p.getUUID());
        if (last != null && now - last < 1200) return;
        int have = countPearls(p);
        for (ItemEntity ie : sl.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(80), x -> isPearl(x.getItem()))) have += ie.getItem().getCount();
        if (have >= CityArcKit.PEARLS_NEEDED) return;
        PEARL_AT.put(p.getUUID(), now);
        for (int i = have; i < CityArcKit.PEARLS_NEEDED; i++) {
            BlockPos s = shore(sl, p, p.blockPosition());
            ItemEntity ie = new ItemEntity(sl, s.getX() + 0.5, s.getY() + 0.5, s.getZ() + 0.5, pearl());
            ie.setGlowingTag(true);
            ie.setUnlimitedLifetime();
            sl.addFreshEntity(ie);
        }
        p.displayClientMessage(Component.translatable("hukhong.qc.hac_trieu.pearls_drop"), true);
    }

    // ------------------------------------------------------------------ canh dau gia (Dau Gia buoc 1)
    private static void scene(ServerPlayer p, ServerLevel sl, Arc a, BlockPos c) {
        if (c == null || d2(p, c) > 60.0 * 60.0) return;
        long now = sl.getGameTime();
        long[] sc = SCENE.get(p.getUUID());
        if (sc == null) {
            sc = new long[]{now, 0};
            SCENE.put(p.getUUID(), sc);
            DialogueService.setQuest(p, quest(p).withFlag(QuestChains.choiceFlag(a.id(), "seen")));
            actor(p, sl, "hang_chieu", c, 3);
            actor(p, sl, "nguoi_mua_bi_an", c, 5);
        }
        int idx = (int) ((now - sc[0]) / SCENE_LINE_TICKS) + 1;
        while (sc[1] < Math.min(idx, SCENE_LINES + 1)) {
            sc[1]++;
            if (sc[1] <= SCENE_LINES) p.sendSystemMessage(Component.translatable("hukhong.qc.dau_gia.scene." + sc[1]));
        }
        if (sc[1] > SCENE_LINES) {
            SCENE.remove(p.getUUID());
            for (Entity x : QuestChains.owned(p, 160)) if (x.getTags().contains(TAG_ACTOR)) x.discard();
            DialogueService.setQuest(p, quest(p).withFlag(QuestChains.choiceFlag(a.id(), "scene")));
        }
    }

    private static void actor(ServerPlayer p, ServerLevel sl, String npc, BlockPos c, int r) {
        BlockPos s = NpcPlacement.plazaStand(sl, c, r, r + 4, npc.hashCode());
        StoryNpc n = QuestChains.spawnNpc(p, npc, s, TAG_ACTOR);
        if (n != null) n.setHome(c);
    }

    // ------------------------------------------------------------------ Mo Dao tai Bac Han (c9_modao)
    private static void modaoScene(ServerPlayer p, ServerLevel sl) {
        if (p.tickCount % 100 != 21 || !StoryFlow.active(p)) return;
        var cur = StoryFlow.current(p);
        BlockPos c = modaoSpot(sl);
        if (cur == null || !cur.id().equals("c9_modao") || c == null || d2(p, c) > (double) MODAO_NEAR * MODAO_NEAR) return;
        if (!sl.hasChunkAt(c) || !sl.isPositionEntityTicking(c)) return;
        boolean present = !sl.getEntitiesOfClass(StoryNpc.class, new net.minecraft.world.phys.AABB(c).inflate(64, 40, 64),
                n -> n.isAlive() && n.npcId().equals("mo_dao")).isEmpty();
        if (present) return;
        StoryNpc n = RequiredNpcs.create(sl, "mo_dao", NpcPlacement.plazaStand(sl, c, 3, 9, 7));
        if (n != null) { n.setHome(c); n.addTag(TAG_SCENE); }
    }

    // ------------------------------------------------------------------ goi y
    private static void hint(ServerPlayer p) {
        QuestData q = quest(p);
        for (Arc a : CityArcKit.ARCS) {
            int st = QuestChains.stageOf(q.flags(), a.id());
            if (st == 0) continue;
            BlockPos t = QuestChains.target(p, a.id());
            boolean pointed = t != null && (a.id().equals(CityArcKit.CHO_MAU) && st == 2 || a.id().equals(CityArcKit.DAU_GIA) && st == 2
                    || a.id().equals(CityArcKit.LOI_VU) && st <= 2 || a.id().equals(CityArcKit.KIEM_TRUNG) && st == 1
                    || a.id().equals(CityArcKit.HOANG_DO) && st == 1);
            if (pointed) {
                int d = (int) Math.sqrt(QuestChains.dist2(p, t));
                p.displayClientMessage(Component.translatable(a.hintKey(st), d, Component.translatable("hukhong.qc.dir." + QuestChains.compass(t.getX() - p.getX(), t.getZ() - p.getZ()))), true);
            } else p.displayClientMessage(Component.translatable(a.hintKey(st)), true);
            return;
        }
    }

    // ------------------------------------------------------------------ don dep
    /** Don mot cuoc cham tran / canh cua nguoi choi (khong dong vao co tien do). */
    static void reset(ServerPlayer p) {
        ENC.remove(p.getUUID());
        SCENE.remove(p.getUUID());
        if (!(p.level() instanceof ServerLevel)) return;
        for (Entity x : QuestChains.owned(p, 200)) if (x.getTags().contains(TAG_ARC) || x.getTags().contains(TAG_ACTOR)) x.discard();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        reset(p);
        PEARL_AT.remove(p.getUUID());
    }

    /** Cho GameTest: co cuoc cham tran dang chay cho nguoi choi khong. */
    public static String encounterKey(ServerPlayer p) { return ENC.get(p.getUUID()); }
}
