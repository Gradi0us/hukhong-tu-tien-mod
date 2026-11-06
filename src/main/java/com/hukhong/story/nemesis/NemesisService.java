package com.hukhong.story.nemesis;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.story.StoryService;
import com.hukhong.story.flow.FlowSteps;
import com.hukhong.story.flow.StoryFlow;
import com.hukhong.story.objective.Objectives;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Chủ thể Phản Thần Dạ Khuyết (tuyến nemesis): mỗi bước NEMESIS là một "buổi" (session) theo từng người chơi.
 * Buổi chạy theo dòng thời gian tick (cảnh VISION/TAUNT/REVELATION, cảnh mở đầu + rút lui của FIGHT); trận FIGHT
 * là trạng thái lặp tới khi Ảnh Dạ Khuyết rút (≤35% máu) — rồi hoàn thành bước. Bước chỉ hoàn thành qua
 * {@link StoryFlow#complete}, nên không bao giờ nhảy bước.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class NemesisService {
    private NemesisService() {}

    /** Tag thực thể Ảnh (để hủy drop/xp, chặn sát thương ngoài trận, nhận diện khi dọn dẹp). */
    static final String TAG_AVATAR = "hk_nemesis_avatar", TAG_OWNER = "hk_nemesis_o:";
    /** Ảnh rút khi còn 35% máu (không bao giờ chết dưới ngưỡng này). */
    static final float RETREAT_FRAC = 0.35f;
    /** Người chơi đi xa hơn 48 block thì trận dừng; quay lại trong 24 block quanh điểm bắt đầu thì trận khởi lại từ đầu. */
    static final double LEASH = 48, RESUME = 24;
    static final int INTRO_TICKS = 80, RETREAT_TICKS = 60, VISION_TICKS = 200, TAUNT_TICKS = 200, REVEAL_TICKS = 230;

    static final Map<UUID, Session> SESSIONS = new HashMap<>();
    static final Map<UUID, Session> PAUSED = new HashMap<>();

    // ------------------------------------------------------------------ mô hình buổi
    /** Dòng thời gian: hành động theo tick (0..len), chạy xong thì gọi {@code after}. */
    static final class Timeline {
        final int len;
        final Consumer<Session> after;
        final Map<Integer, List<Consumer<Session>>> acts = new HashMap<>();

        Timeline(int len, Consumer<Session> after) { this.len = len; this.after = after; }

        Timeline at(int t, Consumer<Session> a) {
            acts.computeIfAbsent(t, k -> new ArrayList<>()).add(a);
            return this;
        }

        void run(int t, Session s) {
            List<Consumer<Session>> l = acts.get(t);
            if (l != null) for (Consumer<Session> a : l) a.accept(s);
        }
    }

    /** Một buổi của một người chơi ở một bước NEMESIS. */
    static final class Session {
        final ServerPlayer p;
        final String stepId, arg;
        Vec3 anchor = Vec3.ZERO;
        ResourceKey<Level> dim;
        int t;
        Timeline tl;
        boolean fight, retreating;
        Mob avatar;
        int lineCd, taunt;

        Session(ServerPlayer p, FlowSteps.Step s) { this.p = p; this.stepId = s.id(); this.arg = s.arg(); }

        void play(Timeline tl) { this.tl = tl; this.t = 0; }
    }

    // ------------------------------------------------------------------ vào / khởi lại
    /** Bắt đầu bước NEMESIS (gọi từ {@link StoryFlow}). Luôn thay buổi cũ của người chơi. */
    public static void start(ServerPlayer p, FlowSteps.Step s) {
        SESSIONS.remove(p.getUUID());
        PAUSED.remove(p.getUUID());
        Session se = new Session(p, s);
        switch (s.arg()) {
            case "vision" -> se.play(NemesisScenes.vision(se));
            case "taunt" -> se.play(NemesisScenes.taunt(se));
            case "revelation" -> se.play(NemesisScenes.revelation(se));
            case "fight" -> { clearStale(p); se.play(NemesisScenes.fightIntro(se)); }
            default -> { StoryFlow.complete(p, s.id()); return; } // loại lạ: không bao giờ kẹt luồng
        }
        SESSIONS.put(p.getUUID(), se);
        objective(p, s);
    }

    /** Đăng nhập/hồi sinh khi đang ở bước NEMESIS: chạy lại buổi (trận chết thì bắt đầu lại từ đầu). */
    public static void resume(ServerPlayer p, FlowSteps.Step s) {
        if (SESSIONS.containsKey(p.getUUID())) return;
        start(p, s);
    }

    /** Mục tiêu của bước NEMESIS (gọi từ StoryFlow.setObjective). */
    public static void objective(ServerPlayer p, FlowSteps.Step s) {
        Component text = switch (s.arg()) {
            case "vision" -> txt("obj.vision", "Tìm ánh trăng khuyết đang rọi xuống", "Find the waning moonlight");
            case "taunt" -> txt("obj.taunt", "Lần theo cái bóng sau lưng ngươi", "Follow the shadow behind you");
            case "fight" -> txt("obj.fight", "Đánh lui Ảnh Dạ Khuyết (nó rút khi còn 35% máu)", "Drive back the Shadow (it retreats at 35% HP)");
            default -> txt("obj.revelation", "Đến trước cánh cửa Hư Không", "Reach the Void door");
        };
        Objectives.set(p, s.id(), text, null, null);
    }

    static Component txt(String k, String vi, String en) {
        return com.hukhong.auction.Tx.t("hukhong.nemesis." + k, vi, en);
    }

    // ------------------------------------------------------------------ vòng tick
    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!SESSIONS.isEmpty()) {
            for (Session s : List.copyOf(SESSIONS.values())) {
                if (SESSIONS.get(s.p.getUUID()) == s) tickSession(s);
            }
        }
        if (!PAUSED.isEmpty()) {
            for (Session s : List.copyOf(PAUSED.values())) if (s.p.tickCount % 20 == 7) checkResume(s);
        }
    }

    static void tickSession(Session s) {
        if (s.fight) { tickFight(s); return; }
        if (s.tl == null) return;
        Timeline tl = s.tl;
        tl.run(s.t, s);
        if (s.tl != tl) return; // cảnh đã đổi giữa chừng
        if (s.t >= tl.len) {
            s.tl = null;
            tl.after.accept(s);
            return;
        }
        s.t++;
    }

    /** Hoàn thành bước: gỡ buổi trước (để bước kế tiếp có thể bắt đầu), rồi báo luồng truyện. */
    static void finish(Session s) {
        discardAvatar(s);
        SESSIONS.remove(s.p.getUUID(), s);
        StoryFlow.complete(s.p, s.stepId);
    }

    // ------------------------------------------------------------------ trận đánh
    /** Mở đầu xong: Ảnh bắt đầu đánh (gọi từ cuối cảnh mở đầu). */
    static void startFight(Session s) {
        Mob a = s.avatar;
        if (a == null || !a.isAlive()) { pause(s); return; }
        a.setNoAi(false);
        a.setTarget(s.p);
        s.fight = true;
        s.lineCd = 200;
        objective(s.p, FlowSteps.get(s.stepId));
    }

    static void tickFight(Session s) {
        ServerPlayer p = s.p;
        Mob a = s.avatar;
        if (p.isDeadOrDying()) return; // cái chết do sự kiện LivingDeath xử lý
        if (a == null || !a.isAlive()) { pause(s); return; }
        if (p.level() != a.level() || p.distanceTo(a) > LEASH) { pause(s); return; }
        if (a.getHealth() <= a.getMaxHealth() * RETREAT_FRAC + 0.01f) { retreat(s); return; }
        if (a.getTarget() != p) a.setTarget(p);
        NemesisFx.aura((ServerLevel) a.level(), a.position(), p.tickCount);
        if (--s.lineCd <= 0) {
            s.lineCd = 260;
            NemesisFx.say(p, txt(FIGHT_TAUNT_KEYS[s.taunt % FIGHT_TAUNT_KEYS.length], FIGHT_TAUNT_VI[s.taunt % FIGHT_TAUNT_VI.length], FIGHT_TAUNT_EN[s.taunt % FIGHT_TAUNT_EN.length]));
            s.taunt++;
        }
    }

    static final String[] FIGHT_TAUNT_KEYS = {"fight.t1", "fight.t2", "fight.t3"};
    static final String[] FIGHT_TAUNT_VI = {"Ánh sáng của ngươi đang nhạt dần.", "Ngươi không cứu được ai đâu.", "Đánh đi, từng nhát một, rồi xem bóng tối có tan không."};
    static final String[] FIGHT_TAUNT_EN = {"Your light is fading.", "You cannot save anyone.", "Strike on, one blow at a time, and see if the dark breaks."};

    /** Ảnh rút: chuyển sang cảnh rút lui (bất tử), rồi tan và hoàn thành bước. */
    static void retreat(Session s) {
        s.fight = false;
        s.retreating = true;
        s.play(NemesisScenes.retreat(s));
    }

    /** Người chơi đi xa / Ảnh biến mất: gỡ Ảnh, lưu điểm bắt đầu, khởi lại khi người chơi quay về gần. */
    static void pause(Session s) {
        discardAvatar(s);
        SESSIONS.remove(s.p.getUUID(), s);
        PAUSED.put(s.p.getUUID(), s);
    }

    static void checkResume(Session s) {
        ServerPlayer p = s.p;
        FlowSteps.Step cur = StoryFlow.current(p);
        if (cur == null || !cur.id().equals(s.stepId) || !StoryFlow.active(p)) { PAUSED.remove(p.getUUID(), s); return; }
        if (!p.level().dimension().equals(s.dim)) return;
        if (p.position().distanceTo(s.anchor) > RESUME) return;
        PAUSED.remove(p.getUUID(), s);
        start(p, cur);
    }

    static void discardAvatar(Session s) {
        if (s.avatar != null) { if (s.avatar.isAlive()) s.avatar.discard(); s.avatar = null; }
    }

    /** Tạo Ảnh: Wither Skeleton (không drop, không tự biến mất), chỉ số theo cấp người chơi, tên và tag riêng. */
    static Mob spawnAvatar(ServerPlayer p, Vec3 at) {
        ServerLevel sl = p.serverLevel();
        com.hukhong.endgame.DaKhuyet w = com.hukhong.endgame.EndgameRegistry.DA_KHUYET.get().create(sl);
        if (w != null) { w.markAvatar(); w.removeTag(com.hukhong.flight.FlightBans.WORLD_BOSS_TAG); }
        if (w == null) return null;
        int lv = Math.max(15, Math.min(StoryService.MAX_LEVEL, CultivationService.get(p).level()));
        w.moveTo(at.x, at.y, at.z, NemesisFx.yawFromTo(at, p.position()), 0f);
        w.setCustomName(txt("avatar_name", "Ảnh Dạ Khuyết", "Shadow of Dạ Khuyết"));
        w.setCustomNameVisible(true);
        setAttr(w, Attributes.MAX_HEALTH, (40 + lv * 6.0) * 4.0);
        w.setHealth(w.getMaxHealth());
        setAttr(w, Attributes.ATTACK_DAMAGE, (4 + lv * 0.25) * 1.6);
        setAttr(w, Attributes.ARMOR, 2 + lv * 0.2);
        setAttr(w, Attributes.KNOCKBACK_RESISTANCE, 1.0);
        w.setPersistenceRequired();
        w.setNoAi(true);
        w.addTag(TAG_AVATAR);
        w.addTag(TAG_OWNER + p.getUUID());
        sl.addFreshEntity(w);
        return w;
    }

    /** Hình bóng Dạ Khuyết trong cảnh (không đánh, bất tử) — dùng model thật thay cho khung dây. */
    static Mob spawnPhantom(ServerPlayer p, Vec3 at) {
        Mob m = spawnAvatar(p, at);
        if (m != null) { m.setInvulnerable(true); m.setSilent(true); m.setCustomNameVisible(false); }
        return m;
    }

    private static void setAttr(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double v) {
        AttributeInstance i = e.getAttribute(attr);
        if (i != null) i.setBaseValue(v);
    }

    /** Dọn Ảnh còn sót (do server tắt giữa chừng) quanh người chơi trước khi mở trận mới. */
    static void clearStale(ServerPlayer p) {
        String own = TAG_OWNER + p.getUUID();
        for (Entity e : p.serverLevel().getEntities((Entity) null, p.getBoundingBox().inflate(128),
                x -> x.getTags().contains(TAG_AVATAR) && x.getTags().contains(own))) e.discard();
    }

    static Session sessionOf(LivingEntity avatar) {
        for (String t : avatar.getTags()) if (t.startsWith(TAG_OWNER)) {
            Session s = SESSIONS.get(UUID.fromString(t.substring(TAG_OWNER.length())));
            return s != null && s.avatar == avatar ? s : null;
        }
        return null;
    }

    // ------------------------------------------------------------------ sự kiện
    /** Ảnh: ngoài trận (intro/rút lui) bất tử; trong trận không bao giờ hạ dưới 35% máu. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !v.getTags().contains(TAG_AVATAR)) return;
        Session s = sessionOf(v);
        if (s == null || !s.fight) { e.setCanceled(true); return; }
        float floor = v.getMaxHealth() * RETREAT_FRAC;
        if (v.getHealth() - e.getAmount() < floor) e.setAmount(Math.max(0f, v.getHealth() - floor));
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity().getTags().contains(TAG_AVATAR)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onXp(LivingExperienceDropEvent e) {
        if (e.getEntity().getTags().contains(TAG_AVATAR)) e.setDroppedExperience(0);
    }

    /** Người chơi chết: trận dừng ngay, Ảnh tan; hồi sinh sẽ khởi lại bước từ đầu (qua StoryFlow.restore). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Session s = SESSIONS.remove(p.getUUID());
        if (s != null) discardAvatar(s);
        PAUSED.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Session s = SESSIONS.remove(p.getUUID());
        if (s != null) discardAvatar(s);
        PAUSED.remove(p.getUUID());
    }

    /** Dùng cho GameTest: buổi đang chạy của người chơi (hoặc null). */
    public static boolean fighting(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s != null && s.fight;
    }

    /** Dùng cho GameTest: Ảnh của buổi hiện tại (hoặc null). */
    public static LivingEntity avatar(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null ? null : s.avatar;
    }

}
