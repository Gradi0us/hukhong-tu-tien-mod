package com.hukhong.story.flow;

import com.hukhong.HuKhong;
import com.hukhong.block.AnHonDaiBlock;
import com.hukhong.chg.ChgKit;
import com.hukhong.chg.ChgService;
import com.hukhong.city.CityDef;
import com.hukhong.city.CitySavedData;
import com.hukhong.city.CityService;
import com.hukhong.core.CultivationData;
import com.hukhong.core.CultivationService;
import com.hukhong.endgame.EndgameService;
import com.hukhong.endgame.WorldTierData;
import com.hukhong.entity.ModEntities;
import com.hukhong.events.InvasionManager;
import com.hukhong.society.LinhKhiDaiBlock;
import com.hukhong.society.SectDef;
import com.hukhong.society.SectEvents;
import com.hukhong.society.SectService;
import com.hukhong.story.StoryData;
import com.hukhong.story.StoryService;
import com.hukhong.story.SuDo;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.story.npc.StoryNpcRegistry;
import com.hukhong.story.objective.Objectives;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.UUID;

/**
 * Luồng truyện tuyến chính (docs/DESIGN-v1-story-flow.md): chương 0 (mở đầu theo xuất thân) + chương 1-10 gắn mốc cảnh giới.
 * Mỗi bước luôn có mục tiêu (Objectives). Chỉ chạy ở Tuyến truyện. Bước hiện tại lưu trong QuestData ("step:ID").
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class StoryFlow {
    private StoryFlow() {}

    public static final String F_V2 = "flow2", F_DONE = "flow_done", TAG_FLOW = "hk_flow", TAG_RIVAL = "hk_rival", TAG_OWNER = "hk_o:";
    private static final String HUB = "luu_ly_cang";

    // ------------------------------------------------------------------ trạng thái
    static QuestData quest(ServerPlayer p) { return DialogueService.quest(p); }

    static void save(ServerPlayer p, QuestData q) { DialogueService.setQuest(p, q); }

    public static boolean active(ServerPlayer p) {
        return DialogueService.storyMode(p) && quest(p).has(F_V2) && !quest(p).has(F_DONE);
    }

    public static FlowSteps.Step current(ServerPlayer p) { return FlowSteps.get(quest(p).step()); }

    static String group(ServerPlayer p) { return FlowSteps.groupOf(CultivationService.get(p).origin()); }

    static ServerLevel ow(ServerPlayer p) { return p.server.getLevel(Level.OVERWORLD); }

    // ------------------------------------------------------------------ vào luồng
    /** Gọi khi người chơi vừa chọn Tuyến truyện: bắt đầu chương 0 theo xuất thân. */
    public static void startPrologue(ServerPlayer p) {
        if (!DialogueService.storyMode(p) || quest(p).has(F_V2)) return;
        save(p, quest(p).withFlag(F_V2));
        FlowSteps.Step first = FlowSteps.firstOf(0, group(p));
        beat(p, "hukhong.flow.pro." + group(p), 3);
        enter(p, first);
        settle(p);
    }

    /** Đăng nhập/hồi sinh: di trú save cũ (chương 1-6 cũ) hoặc đặt lại mục tiêu hiện tại. */
    public static void restore(ServerPlayer p) {
        if (!DialogueService.storyMode(p)) return;
        QuestData q = quest(p);
        if (!q.has(F_V2)) { migrate(p); return; }
        if (q.has(F_DONE)) return;
        FlowSteps.Step s = current(p);
        if (s != null) setObjective(p, s);
    }

    /** Save cũ (chưa có luồng): chương theo cảnh giới; cờ chN_done cũ đánh dấu các bước thoại đã xong. */
    private static void migrate(ServerPlayer p) {
        QuestData q = quest(p).withFlag(F_V2);
        String[][] map = {{"ch1_done", "c1_talk"}, {"ch2_done", "c2_rival"}, {"ch3_done", "c4_hua_co"}, {"ch4_done", "c6_modao"}};
        for (String[] m : map) if (q.has(m[0])) q = q.withFlag("d:" + m[1]);
        CultivationData d = CultivationService.get(p);
        int ch = FlowSteps.chapterFor(d.level(), d.milestoneLocked());
        q = q.withChapter(ch);
        save(p, q);
        FlowSteps.Step first = FlowSteps.firstOf(ch, group(p));
        if (first != null) { enter(p, first); settle(p); }
    }

    // ------------------------------------------------------------------ chuyển bước
    /** Hoàn thành bước nếu đúng là bước hiện tại (dùng cho hành động thoại, sự kiện). */
    public static void complete(ServerPlayer p, String id) {
        if (!DialogueService.storyMode(p) || !id.equals(quest(p).step())) return;
        moveNext(p, FlowSteps.get(id));
        settle(p);
    }

    private static void moveNext(ServerPlayer p, FlowSteps.Step cur) {
        save(p, quest(p).withFlag("d:" + cur.id()));
        FlowSteps.Step nx = FlowSteps.next(cur, group(p));
        if (nx == null) finish(p); else enter(p, nx);
    }

    /** Tự hoàn thành các bước đã thỏa (mốc, cảnh giới, vị trí...). */
    private static void settle(ServerPlayer p) {
        for (int i = 0; i < 24; i++) {
            FlowSteps.Step s = current(p);
            if (s == null || quest(p).has(F_DONE) || !satisfied(p, s)) return;
            moveNext(p, s);
        }
    }

    private static void enter(ServerPlayer p, FlowSteps.Step s) {
        QuestData q = quest(p);
        boolean newChapter = s.chapter() > q.raw();
        q = q.withChapter(s.chapter()).withStep(s.id());
        if (s.kind() == FlowSteps.Kind.INVASION) {
            ServerLevel ow = ow(p);
            q = q.withProg(ow == null ? 1 : CitySavedData.get(ow).invasionsWon() + 1);
        }
        save(p, q);
        if (newChapter) beat(p, "hukhong.flow.ch." + s.chapter(), 3);
        switch (s.kind()) {
            case KILL -> spawnHuThi(p, s.n() + 2);
            case TALK -> { if (s.arg().equals("su_phu")) spawnNpc(p, "su_phu", 4); }
            case DUEL -> spawnNpc(p, s.arg(), 7);
            default -> { }
        }
        setObjective(p, s);
    }

    private static void finish(ServerPlayer p) {
        save(p, quest(p).withStep("").withFlag(F_DONE));
        Objectives.clear(p);
        beat(p, "hukhong.flow.end", 3);
    }

    /** Thỏa điều kiện bước (thuần đọc trạng thái, không tác dụng phụ). */
    static boolean satisfied(ServerPlayer p, FlowSteps.Step s) {
        QuestData q = quest(p);
        if (q.has("d:" + s.id())) return true;
        CultivationData d = CultivationService.get(p);
        StoryData sd = StoryService.get(p);
        return switch (s.kind()) {
            case TALK, DUEL, ALTAR, LINH_KHI -> false;
            case KILL -> q.prog() >= s.n();
            case REACH -> {
                BlockPos t = cityPos(p, s.arg());
                yield t != null && p.level().dimension() == Level.OVERWORLD && dist2(p, t) < (double) s.n() * s.n();
            }
            case LEVEL -> d.level() >= s.n();
            case MILESTONE -> d.level() >= s.n() && !d.milestoneLocked();
            case SECT -> SectService.get(p).member() || guildStarted(p);
            case DOCS -> sd.docs().size() >= s.n();
            case LAIR_SEEN -> (sd.lair() & 3) != 0;
            case CHG -> ChgKit.defeated(sd.lair(), s.n());
            case BICANH -> com.hukhong.bicanh.BiCanhManager.inRun(p);
            case INVASION -> {
                ServerLevel ow = ow(p);
                yield ow != null && CitySavedData.get(ow).invasionsWon() >= q.prog();
            }
            case GATE -> {
                ServerLevel ow = ow(p);
                yield ow != null && WorldTierData.get(ow).gateway() != null && EndgameService.eligible(p);
            }
            case ENDING -> EndgameService.hasEnding(q);
        };
    }

    /** Cảnh giới vượt chương hiện tại (đột phá mốc, lệnh, ...): nhảy sang chương mới + beat. */
    public static void onProgress(ServerPlayer p) {
        if (!active(p)) return;
        FlowSteps.Step s = current(p);
        if (s == null) return;
        CultivationData d = CultivationService.get(p);
        int target = FlowSteps.chapterFor(d.level(), d.milestoneLocked());
        if (target > s.chapter() && !(s.chapter() == 0 && target < 2)) {
            FlowSteps.Step first = FlowSteps.firstOf(target, group(p));
            if (first != null) enter(p, first);
        }
        settle(p);
    }

    // ------------------------------------------------------------------ mục tiêu
    static void setObjective(ServerPlayer p, FlowSteps.Step s) {
        int prog = quest(p).prog();
        Object a1 = s.kind() == FlowSteps.Kind.KILL ? Math.min(prog, s.n()) : s.n();
        Component text = Component.translatable(s.objKey(), a1, s.n());
        BlockPos t = target(p, s);
        Objectives.set(p, s.id(), text, t, t == null ? null : Level.OVERWORLD);
    }

    static BlockPos target(ServerPlayer p, FlowSteps.Step s) {
        ServerLevel ow = ow(p);
        if (ow == null) return null;
        return switch (s.kind()) {
            case REACH, INVASION -> cityPos(p, s.arg());
            case ALTAR, MILESTONE -> altarPos(ow);
            case TALK -> switch (s.arg()) {
                case "mo_dao" -> cityPos(p, HUB);
                case "hang_chieu" -> cityPos(p, "hac_thach_quan");
                case "hua_co" -> cityPos(p, "co_tien_thanh");
                default -> null; // NPC đi theo người chơi (su_phu)
            };
            case BICANH -> { BlockPos c = cityPos(p, s.arg()); BlockPos pt = c == null || ow == null ? null : storyPortal(ow, c); yield pt != null ? pt : c; }
            case LINH_KHI, SECT -> sectPos(p, ow);
            case CHG -> lair(ow, s.n());
            case GATE -> WorldTierData.get(ow).gateway();
            default -> null;
        };
    }

    static BlockPos cityPos(ServerPlayer p, String id) {
        ServerLevel ow = ow(p);
        CityDef d = CityDef.byId(id);
        return ow == null || d == null ? null : CityService.site(ow, d);
    }

    static BlockPos altarPos(ServerLevel ow) {
        CitySavedData cd = CitySavedData.get(ow);
        if (cd.isBuilt()) return cd.altarPos();
        return CityService.site(ow, CityDef.ALL.get(0));
    }

    static BlockPos lair(ServerLevel ow, int idx) {
        int[] xz = ChgService.lairXZ(ow, idx);
        return new BlockPos(xz[0], ow.getSeaLevel(), xz[1]);
    }

    static BlockPos sectPos(ServerPlayer p, ServerLevel ow) {
        var sd = SectService.get(p);
        SectDef best = sd.member() ? SectDef.byId(sd.sect()) : null;
        double bd = Double.MAX_VALUE;
        if (best == null) for (SectDef d : SectDef.ALL) {
            if (d.hidden() || !d.buildable()) continue;
            // chỉ chỉ đường tới tông môn mình đủ điều kiện gia nhập (tránh kẹt ở tông từ chối)
            if (SectService.checkApply(p, d, false) != com.hukhong.society.SectMath.OK) continue;
            double dd = SectEvents.site(ow, d).distSqr(p.blockPosition());
            if (dd < bd) { bd = dd; best = d; }
        }
        if (best == null && !sd.member()) return cityPos(p, "luu_ly_cang"); // không hợp tông nào → đi Hiệp Hội (trong thành)
        return best == null ? null : SectEvents.site(ow, best);
    }

    /** Lối thay thế cho người không vào được tông môn: đã làm ít nhất 1 nhiệm vụ Hiệp Hội. */
    static boolean guildStarted(ServerPlayer p) {
        var g = com.hukhong.guild.GuildService.get(p);
        return g.points() > 0 || g.done() > 0;
    }

    private static double dist2(ServerPlayer p, BlockPos t) {
        double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
        return dx * dx + dz * dz;
    }

    // ------------------------------------------------------------------ beat (banner + vài dòng)
    /** Title + subtitle + {@code lines} dòng chat: keys prefix.title / prefix.sub / prefix.l1... */
    static void beat(ServerPlayer p, String prefix, int lines) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(prefix + ".sub")));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(prefix + ".title")));
        for (int i = 1; i <= lines; i++) p.sendSystemMessage(Component.translatable(prefix + ".l" + i));
    }

    // ------------------------------------------------------------------ thực thể
    static BlockPos groundNear(ServerPlayer p, double minR, double maxR) {
        ServerLevel sl = p.serverLevel();
        double a = p.getRandom().nextDouble() * Math.PI * 2, r = minR + p.getRandom().nextDouble() * (maxR - minR);
        BlockPos xz = BlockPos.containing(p.getX() + Math.cos(a) * r, 0, p.getZ() + Math.sin(a) * r);
        return sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, xz);
    }

    static void spawnHuThi(ServerPlayer p, int n) {
        ServerLevel sl = p.serverLevel();
        for (int i = 0; i < n; i++) {
            BlockPos pos = groundNear(p, 9, 16);
            Mob m = ModEntities.HU_THI.get().create(sl);
            if (m == null) continue;
            m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            m.setPersistenceRequired();
            m.addTag(TAG_FLOW);
            m.addTag(TAG_OWNER + p.getUUID());
            sl.addFreshEntity(m);
            m.setTarget(p);
        }
    }

    public static StoryNpc spawnNpc(ServerPlayer p, String id, double dist) {
        ServerLevel sl = p.serverLevel();
        BlockPos pos = groundNear(p, dist, dist + 1);
        StoryNpc n = StoryNpcRegistry.STORY_NPC.get().create(sl);
        if (n == null) return null;
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sl.random.nextFloat() * 360f, 0f);
        n.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        n.setNpcId(id);
        n.addTag(TAG_FLOW);
        n.addTag(TAG_OWNER + p.getUUID());
        sl.addFreshEntity(n);
        return n;
    }

    static List<Entity> flowEntities(ServerPlayer p, double r) {
        String own = TAG_OWNER + p.getUUID();
        return p.serverLevel().getEntities((Entity) null, p.getBoundingBox().inflate(r),
                e -> e.getTags().contains(TAG_FLOW) && e.getTags().contains(own));
    }

    // ------------------------------------------------------------------ đấu Hạng Chiêu
    /** Hành động thoại "duel": thay NPC bằng đối thủ chiến đấu thật; còn 20% máu thì tự rút lui, hoàn thành bước. */
    public static void startDuel(ServerPlayer p) {
        FlowSteps.Step s = current(p);
        if (s == null || s.kind() != FlowSteps.Kind.DUEL) return;
        BlockPos at = p.blockPosition().offset(3, 0, 0);
        for (Entity e : flowEntities(p, 48)) {
            if (e instanceof StoryNpc n && n.npcId().equals(s.arg())) { at = n.blockPosition(); n.discard(); }
        }
        int lv = Math.max(15, Math.min(StoryService.MAX_LEVEL, CultivationService.get(p).level()));
        SuDo r = StoryService.spawn(p.serverLevel(), at, 0, lv);
        if (r == null) return;
        r.setCustomName(Component.translatable("hukhong.npc.hang_chieu"));
        r.setCustomNameVisible(true);
        r.setPersistenceRequired();
        r.addTag(TAG_FLOW);
        r.addTag(TAG_RIVAL);
        r.addTag(TAG_OWNER + p.getUUID());
        r.setTarget(p);
    }

    @SubscribeEvent
    public static void onRivalHurt(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !v.getTags().contains(TAG_RIVAL)) return;
        if (v.getHealth() - e.getAmount() > v.getMaxHealth() * 0.2f) return;
        e.setCanceled(true);
        ServerPlayer p = ownerOf(v);
        v.discard();
        if (p == null) return;
        FlowSteps.Step s = current(p);
        if (s == null || s.kind() != FlowSteps.Kind.DUEL) return;
        p.sendSystemMessage(Component.translatable("hukhong.flow.rival.yield." + s.id()));
        CultivationService.addExp(p, 200L * s.chapter());
        complete(p, s.id());
    }

    private static ServerPlayer ownerOf(Entity v) {
        if (v.getServer() == null) return null;
        for (String t : v.getTags()) if (t.startsWith(TAG_OWNER)) {
            return v.getServer().getPlayerList().getPlayer(UUID.fromString(t.substring(TAG_OWNER.length())));
        }
        return null;
    }

    // ------------------------------------------------------------------ sự kiện
    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        boolean tagged = v.getTags().contains(TAG_FLOW);
        if (!tagged && v.getType() != ModEntities.HU_THI.get()) return;
        ServerPlayer p = null;
        if (e.getSource().getEntity() instanceof ServerPlayer sp) p = sp;
        else if (tagged) {
            ServerPlayer o = ownerOf(v);
            if (o != null && o.distanceTo(v) < 48) p = o;
        }
        if (p != null) onKillCounted(p);
    }

    private static void onKillCounted(ServerPlayer p) {
        if (!active(p)) return;
        FlowSteps.Step s = current(p);
        if (s == null || s.kind() != FlowSteps.Kind.KILL) return;
        QuestData q = quest(p).withProg(quest(p).prog() + 1);
        save(p, q);
        if (q.prog() >= s.n()) complete(p, s.id()); else setObjective(p, s);
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getLevel().isClientSide() || !active(p)) return;
        var b = e.getLevel().getBlockState(e.getPos()).getBlock();
        FlowSteps.Step s = current(p);
        if (s == null) return;
        if (b instanceof AnHonDaiBlock && s.kind() == FlowSteps.Kind.ALTAR) complete(p, s.id());
        else if (b instanceof LinhKhiDaiBlock && s.kind() == FlowSteps.Kind.LINH_KHI) complete(p, s.id());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) restore(p);
    }

    /** Mỗi 1s: đồng bộ chương theo cảnh giới, tự hoàn thành bước, duy trì thực thể/sự kiện của bước. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7 || !active(p)) return;
        onProgress(p);
        FlowSteps.Step s = current(p);
        if (s == null) return;
        maintain(p, s);
        s = current(p);
        if (s != null && !s.id().equals(Objectives.currentId(p))) setObjective(p, s);
    }

    private static void maintain(ServerPlayer p, FlowSteps.Step s) {
        switch (s.kind()) {
            case KILL -> {
                if (p.tickCount % 100 != 7) return;
                int alive = (int) flowEntities(p, 96).stream().filter(x -> x instanceof Mob m && m.isAlive() && !(x instanceof StoryNpc)).count();
                int need = s.n() - quest(p).prog();
                if (alive < need) spawnHuThi(p, need - alive);
            }
            case DUEL -> {
                if (p.tickCount % 100 != 7) return;
                if (flowEntities(p, 128).isEmpty()) spawnNpc(p, s.arg(), 7);
            }
            case INVASION -> {
                ServerLevel ow = ow(p);
                if (ow == null || p.level().dimension() != Level.OVERWORLD) return;
                CitySavedData cd = CitySavedData.get(ow);
                if (!cd.isBuilt()) return;
                if (cd.state() == CitySavedData.State.RUIN) {
                    p.sendSystemMessage(Component.translatable("hukhong.flow.invasion.ruin"));
                    complete(p, s.id());
                    return;
                }
                if (!InvasionManager.active() && !InvasionManager.warning() && dist2(p, cd.center()) < 80.0 * 80.0) InvasionManager.warn(ow, 600);
            }
            case BICANH -> {
                if (p.tickCount % 100 != 7 || p.level().dimension() != Level.OVERWORLD) return;
                ServerLevel ow = ow(p);
                BlockPos city = cityPos(p, s.arg());
                if (ow == null || city == null || dist2(p, city) > 300.0 * 300.0) return;
                if (storyPortal(ow, city) == null) {
                    com.hukhong.bicanh.BiCanhEvents.spawnPortalAround(ow, p, city.getX(), city.getZ(), 90, 150,
                            com.hukhong.bicanh.BiCanhTier.BACH, 16);
                }
                setObjective(p, s); // cập nhật đích chỉ tới cổng thật
            }
            default -> { }
        }
    }

    /** Cổng Bí Cảnh đang mở gần thành (≤220 block), hoặc null. */
    static BlockPos storyPortal(ServerLevel ow, BlockPos city) {
        BlockPos best = null; double bd = 220.0 * 220.0;
        for (var pt : com.hukhong.bicanh.BiCanhSaved.get(ow).portals) {
            double d = pt.pos().distSqr(city);
            if (d < bd) { bd = d; best = pt.pos(); }
        }
        return best;
    }
}
