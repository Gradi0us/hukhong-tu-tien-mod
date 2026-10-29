package com.hukhong.story.chain;

import com.hukhong.HuKhong;
import com.hukhong.bicanh.BiCanhEvents;
import com.hukhong.bicanh.BiCanhManager;
import com.hukhong.bicanh.BiCanhSaved;
import com.hukhong.core.CultivationService;
import com.hukhong.entity.ModEntities;
import com.hukhong.guild.EscortMover;
import com.hukhong.story.StoryService;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.NpcPlacement;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.npc.StoryNpcRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chuoi nhiem vu phu noi voi tuyen chinh (docs/DESIGN-v2-quest-chains.md). Trang thai nam trong QuestData.flags:
 * "qc.ID.N" = dang o buoc N (chi mot co buoc moi luc), "qc.ID.done", lua chon "qc.ID.c.TEN" (vinh vien, thoai sau tham chieu), "qc.ID.at:x:z" = dich.
 * Giao nhiem vu bang NPC xuat hien ngau nhien canh nguoi choi (offer), buoc chuyen bang action "qc" trong thoai hoac theo su kien (tick).
 * Cac moc chi giu trong RAM (escort dang di, hen gio tra thu): mat khi tat server -> chuoi lui ve buoc truoc, khong ket.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class QuestChains {
    private QuestChains() {}

    public static final String MERCHANT = "merchant", DISCIPLE = "disciple", NOI_GIAN = "noi_gian", DYING = "dying";
    public static final String TAG_OWNER = "hk_qc:", TAG_OFFER = "hk_qc_offer", TAG_AMB = "hk_qc_amb", TAG_WAVE = "hk_qc_wave", TAG_TMP = "hk_qc_tmp";
    static final long OFFER_COOLDOWN = 6000, OFFER_TTL = 6000, ESCORT_TIMEOUT = 24000;

    /** needStage -1 = chuoi chua bat dau; n&gt;0 = phai dang o buoc n. */
    public record Offer(String chain, String npc, int needStage, int minChapter, int minLevel) {}

    public static final List<Offer> OFFERS = List.of(
            new Offer(MERCHANT, "thuong_nhan", -1, 2, 10),
            new Offer(DISCIPLE, "lam_thu", -1, 3, 20),
            new Offer(NOI_GIAN, "de_tu_mat", -1, 4, 30),
            new Offer(NOI_GIAN, "truong_lao_gian", 1, 5, 40),
            new Offer(DYING, "tu_si_hap_hoi", -1, 1, 8));

    public static final List<String> CHAINS = List.of(MERCHANT, DISCIPLE, NOI_GIAN, DYING);

    // ------------------------------------------------------------------ co (thuan, kiem thu duoc)
    public static String stageFlag(String chain, int n) { return "qc." + chain + "." + n; }
    public static String doneFlag(String chain) { return "qc." + chain + ".done"; }
    public static String choiceFlag(String chain, String c) { return "qc." + chain + ".c." + c; }

    /** Buoc hien tai (1..8) cua chuoi, 0 = chua bat dau / da xong. */
    public static int stageOf(List<String> flags, String chain) {
        for (int n = 1; n <= 8; n++) if (flags.contains(stageFlag(chain, n))) return n;
        return 0;
    }

    public static boolean offerApplies(Offer o, List<String> flags, int chapter, int level) {
        if (chapter < o.minChapter() || level < o.minLevel() || flags.contains(doneFlag(o.chain()))) return false;
        int st = stageOf(flags, o.chain());
        return o.needStage() < 0 ? st == 0 : st == o.needStage();
    }

    /** 0..7 = B, DB, D, DN, N, TN, T, TB (dz am = huong bac). */
    public static int compass(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = bac, 90 = dong
        return (int) Math.floorMod(Math.round(a / 45.0), 8L);
    }

    static QuestData quest(ServerPlayer p) { return DialogueService.quest(p); }

    // ------------------------------------------------------------------ chuyen buoc
    public static void setStage(ServerPlayer p, String chain, int n) {
        QuestData q = quest(p);
        for (int i = 1; i <= 9; i++) q = q.withoutFlag(stageFlag(chain, i));
        if (n >= 9) q = q.withFlag(doneFlag(chain));
        else if (n > 0) q = q.withFlag(stageFlag(chain, n));
        DialogueService.setQuest(p, q);
        if (n >= 9) { p.sendSystemMessage(Component.translatable("hukhong.qc." + chain + ".done")); cleanup(p, chain); }
        else if (n > 0) onStage(p, chain, n);
        else p.sendSystemMessage(Component.translatable("hukhong.qc." + chain + ".reset"));
    }

    private static void onStage(ServerPlayer p, String chain, int n) {
        switch (chain + ":" + n) {
            case MERCHANT + ":2" -> startEscort(p);
            case MERCHANT + ":5" -> { Escort e = ESCORTS.get(p.getUUID()); if (e != null) e.walker.halted = false; }
            case DISCIPLE + ":1" -> pickTarget(p, DISCIPLE, 130, 170);
            case DISCIPLE + ":2" -> { BlockPos at = target(p, DISCIPLE); if (at != null) spawnDisciple(p, at); }
            case DISCIPLE + ":4" -> spawnWave(p, p.blockPosition(), 5, TAG_WAVE, 7, 12);
            case DYING + ":1" -> pickTarget(p, DYING, 170, 230);
            case NOI_GIAN + ":8" -> REVENGE.put(p.getUUID(), p.serverLevel().getGameTime() + 1200);
            default -> { }
        }
        if (!(chain.equals(MERCHANT) && n == 2)) p.sendSystemMessage(Component.translatable("hukhong.qc." + chain + ".s" + n));
    }

    private static void cleanup(ServerPlayer p, String chain) {
        if (chain.equals(MERCHANT)) { Escort e = ESCORTS.remove(p.getUUID()); if (e != null) tmp(p, e.npc); }
        if (chain.equals(DISCIPLE)) for (Entity x : owned(p, 128)) if (x instanceof StoryNpc n && n.npcId().equals("de_tu_lac")) n.discard();
        if (chain.equals(NOI_GIAN)) for (Entity x : owned(p, 128)) if (x instanceof StoryNpc n && n.npcId().equals("truong_lao_gian")) n.discard();
        QuestData q = quest(p);
        String at = q.valueOf("qc." + chain + ".at:");
        if (!at.isEmpty()) DialogueService.setQuest(p, q.withoutFlag("qc." + chain + ".at:" + at));
    }

    // ------------------------------------------------------------------ dich den (luu trong co)
    static void pickTarget(ServerPlayer p, String chain, int dmin, int dmax) {
        int x = 0, z = 0;
        // Chi chon dat lien (khong bien/song) - tranh hang/cong bi chim duoi nuoc. Het luot thu thi lay diem cuoi.
        for (int tries = 0; tries < 32; tries++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2, r = dmin + p.getRandom().nextInt(Math.max(1, dmax - dmin));
            x = (int) Math.round(p.getX() + Math.cos(a) * r);
            z = (int) Math.round(p.getZ() + Math.sin(a) * r);
            if (dryLand(p.serverLevel(), x, z)) break;
        }
        QuestData q = quest(p);
        String old = q.valueOf("qc." + chain + ".at:");
        if (!old.isEmpty()) q = q.withoutFlag("qc." + chain + ".at:" + old);
        DialogueService.setQuest(p, q.withFlag("qc." + chain + ".at:" + x + ":" + z));
    }

    /** Dat lien quanh (x,z): khong bien/song, it nuoc trong ban kinh 14, do doc vua phai (khong nap chunk). */
    static boolean dryLand(ServerLevel sl, int x, int z) {
        return com.hukhong.city.DrySites.suitable(sl, x, z, 14, 14);
    }

    public static BlockPos target(ServerPlayer p, String chain) {
        String v = quest(p).valueOf("qc." + chain + ".at:");
        if (v.isEmpty()) return null;
        try {
            String[] s = v.split(":");
            int x = Integer.parseInt(s[0]), z = Integer.parseInt(s[1]);
            return new BlockPos(x, p.serverLevel().getSeaLevel(), z);
        } catch (RuntimeException e) { return null; }
    }

    static double dist2(ServerPlayer p, BlockPos t) {
        double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------------ thuc the
    static List<Entity> owned(ServerPlayer p, double r) {
        String own = TAG_OWNER + p.getUUID();
        return p.serverLevel().getEntities((Entity) null, p.getBoundingBox().inflate(r, 64, r), e -> e.getTags().contains(own));
    }

    static int alive(ServerPlayer p, String tag, double r) {
        int c = 0;
        for (Entity e : owned(p, r)) if (e.getTags().contains(tag) && e.isAlive()) c++;
        return c;
    }

    static StoryNpc spawnNpc(ServerPlayer p, String id, BlockPos stand, String... tags) {
        ServerLevel sl = p.serverLevel();
        StoryNpc n = StoryNpcRegistry.STORY_NPC.get().create(sl);
        if (n == null) return null;
        n.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        n.finalizeSpawn(sl, sl.getCurrentDifficultyAt(stand), MobSpawnType.EVENT, null);
        n.setNpcId(id);
        n.addTag(TAG_OWNER + p.getUUID());
        for (String t : tags) n.addTag(t);
        sl.addFreshEntity(n);
        return n;
    }

    static BlockPos standAround(ServerLevel sl, ServerPlayer p, BlockPos c, int rmin, int rmax) {
        int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ());
        BlockPos s = NpcPlacement.findStand(sl, new BlockPos(c.getX(), gy - 1, c.getZ()), rmin, rmax, p.getRandom().nextInt(1 << 16));
        return s != null ? s : new BlockPos(c.getX(), gy, c.getZ());
    }

    static void spawnWave(ServerPlayer p, BlockPos c, int n, String tag, int rmin, int rmax) {
        ServerLevel sl = p.serverLevel();
        for (int i = 0; i < n; i++) {
            BlockPos s = standAround(sl, p, c, rmin, rmax);
            Mob m = ModEntities.HU_THI.get().create(sl);
            if (m == null) continue;
            m.moveTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(s), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            m.addTag(tag);
            m.addTag(TAG_OWNER + p.getUUID());
            sl.addFreshEntity(m);
            m.setTarget(p);
        }
    }

    static void spawnAmbush(ServerPlayer p, BlockPos c, int n, Component name, String tag) {
        ServerLevel sl = p.serverLevel();
        int lv = Math.max(15, Math.min(StoryService.MAX_LEVEL, CultivationService.get(p).level()));
        for (int i = 0; i < n; i++) {
            var r = StoryService.spawn(sl, standAround(sl, p, c, 5, 9), 0, lv);
            if (r == null) continue;
            if (name != null) { r.setCustomName(name); r.setCustomNameVisible(true); }
            r.setPersistenceRequired();
            r.addTag(tag);
            r.addTag(TAG_OWNER + p.getUUID());
            r.setTarget(p);
        }
    }

    static void spawnDisciple(ServerPlayer p, BlockPos at) {
        ServerLevel sl = p.serverLevel();
        BlockPos s = standAround(sl, p, at, 0, 4);
        spawnNpc(p, "de_tu_lac", s);
        spawnWave(p, at, 6, TAG_WAVE, 6, 11);
    }

    /** Danh dau NPC tam: bi don sau ~10 phut. */
    static void tmp(ServerPlayer p, UUID npc) {
        Entity e = p.serverLevel().getEntity(npc);
        if (e != null) e.addTag(TAG_TMP);
    }

    // ------------------------------------------------------------------ escort thuong nhan
    private static final class Escort {
        final UUID npc; final EscortMover.Walker walker; final long deadline; boolean ambushed;
        Escort(UUID npc, EscortMover.Walker w, long deadline) { this.npc = npc; this.walker = w; this.deadline = deadline; }
    }

    private static final Map<UUID, Escort> ESCORTS = new HashMap<>();
    private static final Map<UUID, Long> REVENGE = new HashMap<>(), LAST_OFFER = new HashMap<>();

    private static void startEscort(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Escort old = ESCORTS.remove(p.getUUID());
        if (old != null) tmp(p, old.npc);
        for (Entity e : owned(p, 96)) if (e instanceof StoryNpc n && n.npcId().equals("thuong_nhan")) n.discard(); // NPC giao viec -> doi sang NPC di cung
        BlockPos stand = NpcPlacement.findStand(sl, p.blockPosition().below(), 2, 5, p.getRandom().nextInt(1 << 16));
        StoryNpc n = spawnNpc(p, "thuong_nhan_e", stand != null ? stand : p.blockPosition());
        if (n == null) { setStage(p, MERCHANT, 0); return; }
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        BlockPos dest = new BlockPos((int) Math.round(p.getX() + Math.cos(a) * 110), 64, (int) Math.round(p.getZ() + Math.sin(a) * 110));
        DialogueService.setQuest(p, quest(p).withFlag("qc.merchant.at:" + dest.getX() + ":" + dest.getZ()));
        ESCORTS.put(p.getUUID(), new Escort(n.getUUID(), new EscortMover.Walker(sl, n.position(), dest), sl.getGameTime() + ESCORT_TIMEOUT));
        p.sendSystemMessage(Component.translatable("hukhong.qc.merchant.s2"));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (ESCORTS.isEmpty()) return;
        for (var it = ESCORTS.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) { it.remove(); continue; }
            Escort es = en.getValue();
            Entity ent = p.serverLevel().getEntity(es.npc);
            long now = p.serverLevel().getGameTime();
            if (!(ent instanceof StoryNpc n) || !n.isAlive() || now > es.deadline || p.level().dimension() != Level.OVERWORLD) {
                if (ent != null) ent.discard();
                it.remove();
                p.sendSystemMessage(Component.translatable("hukhong.qc.merchant.lost"));
                setStage(p, MERCHANT, 0);
                continue;
            }
            int st = stageOf(quest(p).flags(), MERCHANT);
            if (st == 4) { es.walker.halted = true; continue; } // cho nguoi choi tra hoi
            if (!es.ambushed && es.walker.index() >= es.walker.size() / 2) {
                es.ambushed = true;
                es.walker.halted = true;
                setStage(p, MERCHANT, 3);
                spawnAmbush(p, n.blockPosition(), 3, Component.translatable("hukhong.qc.merchant.guard"), TAG_AMB);
                continue;
            }
            if (st == 3) continue;
            if (es.walker.tick(n, p) && st == 5) {
                it.remove();
                n.addTag(TAG_TMP);
                setStage(p, MERCHANT, 6);
            }
        }
    }

    // ------------------------------------------------------------------ tick moi giay
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 9 || !DialogueService.storyMode(p)) return;
        if (!(p.level() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        QuestData q = quest(p);
        long now = sl.getGameTime();
        // buoc theo su kien
        int st = stageOf(q.flags(), DISCIPLE);
        BlockPos dt = target(p, DISCIPLE);
        if (st == 1 && dt != null && dist2(p, dt) < 20.0 * 20.0) setStage(p, DISCIPLE, 2);
        else if (st == 2 && alive(p, TAG_WAVE, 96) == 0) setStage(p, DISCIPLE, 3);
        else if (st == 4 && alive(p, TAG_WAVE, 96) == 0) setStage(p, DISCIPLE, 5);

        if (stageOf(q.flags(), MERCHANT) == 3 && alive(p, TAG_AMB, 96) == 0) setStage(p, MERCHANT, 4);

        st = stageOf(q.flags(), DYING);
        BlockPos yt = target(p, DYING);
        if (st == 1 && yt != null && dist2(p, yt) < 24.0 * 24.0) {
            var portal = BiCanhEvents.spawnPortalAround(sl, p, yt.getX(), yt.getZ(), 3, 12, StoryFlow.storyTier(CultivationService.get(p).level()), 24);
            if (portal != null) {
                setStage(p, DYING, 2);
                if (q.has(choiceFlag(DYING, "keep"))) spawnAmbush(p, portal, 2, Component.translatable("hukhong.qc.dying.owner"), TAG_AMB);
            } else if (!dryLand(sl, yt.getX(), yt.getZ())) {
                // Diem cu (luu tu ban truoc) nam tren nuoc -> chon lai diem dat lien gan do
                pickTarget(p, DYING, 40, 90);
                p.displayClientMessage(Component.translatable("hukhong.qc.dying.moved"), true);
            }
        } else if (st == 2) {
            if (BiCanhManager.inRun(p)) { reward(p, DYING); setStage(p, DYING, 9); }
            else if (yt != null && !portalNear(sl, yt)) setStage(p, DYING, 1); // cong het han -> mo lai
        }

        Long rv = REVENGE.get(p.getUUID());
        if (rv != null && now >= rv) {
            REVENGE.remove(p.getUUID());
            spawnAmbush(p, p.blockPosition(), 2, Component.translatable("hukhong.qc.noi_gian.assassin"), TAG_AMB);
            setStage(p, NOI_GIAN, 9);
        } else if (stageOf(q.flags(), NOI_GIAN) == 8 && rv == null) REVENGE.put(p.getUUID(), now + 600);

        if (p.tickCount % 100 == 9) hint(p);
        if (p.tickCount % 400 == 209) { offer(p, sl, now); sweep(p); }
    }

    private static boolean portalNear(ServerLevel sl, BlockPos t) {
        long now = sl.getGameTime();
        for (var pt : BiCanhSaved.get(sl).portals) if (pt.expire() > now && pt.pos().distSqr(t) < 40.0 * 40.0) return true;
        return false;
    }

    /** Thuong rieng cua chuoi (phan con lai do thoai tra truc tiep bang exp / give_item / doc). */
    private static void reward(ServerPlayer p, String chain) {
        if (chain.equals(DYING)) {
            CultivationService.addExp(p, 500);
            var d = com.hukhong.story.StoryDocs.roll(p.getRandom(), Math.max(26, CultivationService.get(p).level()));
            if (d != null) StoryService.collect(p, d.id());
        }
    }

    // ------------------------------------------------------------------ goi y
    private static void hint(ServerPlayer p) {
        QuestData q = quest(p);
        for (String c : CHAINS) {
            int st = stageOf(q.flags(), c);
            if (st == 0) continue;
            BlockPos t = target(p, c);
            boolean pointed = t != null && ((c.equals(DISCIPLE) && st <= 2) || (c.equals(DYING) && st <= 2) || (c.equals(MERCHANT) && st == 5));
            if (pointed) {
                int d = (int) Math.sqrt(dist2(p, t));
                p.displayClientMessage(Component.translatable("hukhong.qc." + c + ".hint." + st, d, Component.translatable("hukhong.qc.dir." + compass(t.getX() - p.getX(), t.getZ() - p.getZ()))), true);
            } else p.displayClientMessage(Component.translatable("hukhong.qc." + c + ".hint." + st), true);
            return; // moi lan chi nhac mot chuoi
        }
    }

    // ------------------------------------------------------------------ NPC giao viec ngau nhien
    private static void offer(ServerPlayer p, ServerLevel sl, long now) {
        if (BiCanhManager.inRun(p) || com.hukhong.world.SafeZones.isSafe(sl, p.blockPosition())) return;
        Long last = LAST_OFFER.get(p.getUUID());
        if (last != null && now - last < OFFER_COOLDOWN) return;
        for (Entity x : owned(p, 160)) if (x.getTags().contains(TAG_OFFER) && x.isAlive()) return; // dang co NPC cho
        if (p.getRandom().nextInt(100) >= 50) return;
        QuestData q = quest(p);
        int lv = CultivationService.get(p).level();
        int start = p.getRandom().nextInt(OFFERS.size());
        for (int i = 0; i < OFFERS.size(); i++) {
            Offer o = OFFERS.get((start + i) % OFFERS.size());
            if (!offerApplies(o, q.flags(), q.current(), lv)) continue;
            BlockPos s = NpcPlacement.findStand(sl, p.blockPosition().below(), 9, 14, p.getRandom().nextInt(1 << 16));
            if (s == null) return;
            StoryNpc n = spawnNpc(p, o.npc(), s, TAG_OFFER);
            if (n == null) return;
            LAST_OFFER.put(p.getUUID(), now);
            p.sendSystemMessage(Component.translatable("hukhong.qc.offer." + o.npc(), n.getName()));
            return;
        }
    }

    /** Don NPC giao viec het han / NPC tam sau khi xong chuoi. */
    private static void sweep(ServerPlayer p) {
        for (Entity x : owned(p, 200)) {
            if (x.getTags().contains(TAG_OFFER) && x.tickCount > OFFER_TTL) x.discard();
            else if (x.getTags().contains(TAG_TMP) && x.tickCount > 12000) x.discard();
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Escort es = ESCORTS.remove(p.getUUID());
        if (es != null) {
            Entity ent = p.serverLevel().getEntity(es.npc);
            if (ent != null) ent.discard();
            QuestData q = quest(p);
            if (stageOf(q.flags(), MERCHANT) > 0) {
                for (int i = 1; i <= 8; i++) q = q.withoutFlag(stageFlag(MERCHANT, i));
                DialogueService.setQuest(p, q);
            }
        }
        LAST_OFFER.remove(p.getUUID());
        for (Entity x : owned(p, 200)) if (x.getTags().contains(TAG_OFFER) || x.getTags().contains(TAG_AMB) || x.getTags().contains(TAG_WAVE)) x.discard();
    }
}
