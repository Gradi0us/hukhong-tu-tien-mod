package com.hukhong.endgame;

import com.hukhong.HuKhong;
import com.hukhong.boss.phase.BossPhaseConfig.SubLine;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.boss.phase.CutscenePayload;
import com.hukhong.chg.ChgKit;
import com.hukhong.endgame.SieuDoKit.Phase;
import com.hukhong.story.StoryService;
import com.hukhong.story.SuDo;
import com.hukhong.story.flow.RequiredNpcs;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.npc.StoryNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ket thuc thu ba "Sieu Do" (PLAN-v2 11.2): sau khi ha Nguyen Thuy Tich Diet (cho chon ket thuc) nguoi choi du dieu kien chon
 * "Sieu Do" -> cuoc gap Da Khuyet (cutscene phu de) -> Tam Chien (4 tan anh CHG, moi tan anh mot cau ky uc) -> giu 4 Manh 60s giua song Hu Khong
 * (Da Khuyet bat tu, khong the danh) -> man ket (Mo Dao ve / tan hon, Hang Chiêu tinh neu rival_lost) -> Tang The Gioi II "Linh Khi Phuc Hung".
 * Mot phien tai mot thoi diem, trong RAM (tat server giua tran -> nguoi choi con co F_PENDING, chon lai duoc). May trang thai: {@link SieuDoKit#next}.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class SieuDo {
    private SieuDo() {}

    public static final String TAG_SHADE = "hk_sieudo_shade", TAG_ACTOR = "hk_sieudo_actor", TAG_DAK = "hk_sieudo_dak";
    private static final int PART_RANGE = 120;

    private static final class Session {
        final ServerPlayer owner;
        final ServerLevel level;
        final BlockPos center;
        final ServerBossEvent bar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
        Phase phase = Phase.INTRO;
        int t, hold;
        DaKhuyet dak;
        boolean soul, rivalLost;

        Session(ServerPlayer owner, ServerLevel level, BlockPos center) { this.owner = owner; this.level = level; this.center = center; }
    }

    private static Session cur;

    public static boolean active() { return cur != null; }
    public static Phase phase() { return cur == null ? null : cur.phase; }
    public static int holdTicks() { return cur == null ? 0 : cur.hold; }
    public static DaKhuyet daKhuyet() { return cur == null ? null : cur.dak; }

    /** Tam san chinh trong Hu Khong Ban Nguyen. */
    public static BlockPos arenaCenter() { return new BlockPos(0, EndgameKit.CENTER_Y + 1, 0); }

    // ------------------------------------------------------------------ bat dau
    public static boolean begin(ServerPlayer p) { return begin(p, p.serverLevel(), arenaCenter()); }

    /** @return false neu da co phien khac dang chay hoac khong sinh duoc Da Khuyet. */
    public static boolean begin(ServerPlayer p, ServerLevel level, BlockPos center) {
        if (cur != null) {
            p.displayClientMessage(Component.translatable("hukhong.sieudo.busy"), false);
            return false;
        }
        Session s = new Session(p, level, center);
        DaKhuyet d = EndgameRegistry.DA_KHUYET.get().create(level);
        if (d == null) return false;
        BlockPos at = center.offset(0, 0, -6);
        d.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        d.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        d.setNoAi(true);
        d.setInvulnerable(true);   // phase 3 khong danh Da Khuyet: bat tu ca khi bi tan cong
        d.setHealth(d.getMaxHealth());
        d.addTag(TAG_DAK);
        level.addFreshEntity(d);
        s.dak = d;
        QuestData q = DialogueService.quest(p);
        s.soul = q.has("mo_dao_died");
        s.rivalLost = q.has("rival_lost");
        cur = s;
        for (ServerPlayer x : near(s)) {
            s.bar.addPlayer(x);
            x.displayClientMessage(Component.translatable("hukhong.sieudo.begin"), false);
        }
        s.bar.setName(Component.translatable("hukhong.sieudo.bar.intro"));
        s.bar.setProgress(0f);
        cutscene(s, SieuDoKit.INTRO_KEYS, SieuDoKit.INTRO_SUBS, SieuDoKit.INTRO_TICKS);
        return true;
    }

    // ------------------------------------------------------------------ tick
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) { tickSession(); }

    /** Mot tick cua phien dang chay (event goi moi tick; GameTest goi tay). */
    public static void tickSession() {
        Session s = cur;
        if (s == null) return;
        boolean ownerOk = s.owner.isAlive() && !s.owner.hasDisconnected() && s.owner.level() == s.level
                && Math.hypot(s.owner.getX() - s.center.getX(), s.owner.getZ() - s.center.getZ()) < 70;
        Phase before = s.phase;
        switch (s.phase) {
            case TAM_CHIEN -> tickTamChien(s);
            case GIU_MANH -> tickHold(s);
            default -> { }
        }
        int alive = s.phase == Phase.TAM_CHIEN ? shadesAlive(s) : 0;
        Phase nx = SieuDoKit.next(s.phase, s.t, alive, s.hold, ownerOk);
        s.t++;
        if (nx != before) enter(s, nx);
    }

    private static void enter(Session s, Phase to) {
        s.phase = to;
        s.t = 0;
        switch (to) {
            case TAM_CHIEN -> {
                spawnShades(s);
                s.bar.setName(Component.translatable("hukhong.sieudo.bar.tam_chien"));
                s.bar.setColor(BossEvent.BossBarColor.PURPLE);
                s.bar.setProgress(1f);
                msg(s, "hukhong.sieudo.tam_chien");
            }
            case GIU_MANH -> {
                s.bar.setName(Component.translatable("hukhong.sieudo.bar.hold"));
                s.bar.setColor(BossEvent.BossBarColor.WHITE);
                s.bar.setProgress(0f);
                msg(s, "hukhong.sieudo.hold");
            }
            case OUTRO -> {
                s.bar.setName(Component.translatable("hukhong.sieudo.bar.outro"));
                s.bar.setProgress(1f);
                actors(s);
                cutscene(s, SieuDoKit.OUTRO_KEYS, SieuDoKit.outroSubs(s.soul, s.rivalLost), SieuDoKit.OUTRO_TICKS);
            }
            case DONE -> finish(s);
            case FAILED -> fail(s);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Tam Chien
    private static void spawnShades(Session s) {
        ServerLevel sl = s.level;
        for (int i = 0; i < SieuDoKit.SHADES; i++) {
            double a = Math.PI / 2 * i + Math.PI / 4;
            BlockPos at = s.center.offset((int) Math.round(Math.cos(a) * 10), 0, (int) Math.round(Math.sin(a) * 10));
            int lv = SieuDoKit.shadeLevel(ChgKit.def(i + 1).level());
            SuDo sd = StoryService.spawn(sl, at, i % 3, lv);
            if (sd == null) continue;
            var hp = sd.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(hp.getBaseValue() * SieuDoKit.SHADE_HP_FACTOR);
            sd.setHealth(sd.getMaxHealth());
            sd.setCustomName(Component.translatable(SieuDoKit.shadeNameKey(), Component.translatable("entity.hukhong.chg_" + (i + 1))));
            sd.setCustomNameVisible(true);
            sd.setPersistenceRequired();
            sd.addTag(TAG_SHADE);
            sd.setTarget(s.owner);
        }
    }

    static int shadesAlive(Session s) {
        int n = 0;
        for (Entity e : s.level.getEntities((Entity) null, new AABB(s.center).inflate(40, 30, 40), x -> x.getTags().contains(TAG_SHADE) && x.isAlive())) n++;
        return n;
    }

    private static void tickTamChien(Session s) {
        for (int i = 0; i < SieuDoKit.SHADES; i++)
            if (s.t == SieuDoKit.shadeLineTick(i)) subtitle(s, SieuDoKit.shadeLineKey(i), "entity.hukhong.chg_" + (i + 1));
        if (s.t % 20 == 0) s.bar.setProgress(Math.max(0f, shadesAlive(s) / (float) SieuDoKit.SHADES));
    }

    // ------------------------------------------------------------------ giu 4 Manh
    private static void tickHold(Session s) {
        ServerLevel sl = s.level;
        boolean inside = Math.hypot(s.owner.getX() - s.center.getX(), s.owner.getZ() - s.center.getZ()) <= SieuDoKit.HOLD_RADIUS;
        s.hold = SieuDoKit.holdAfter(s.hold, inside);
        if (s.t % 20 == 0) {
            s.bar.setProgress(s.hold / (float) SieuDoKit.HOLD_TICKS);
            if (!inside) s.owner.displayClientMessage(Component.translatable("hukhong.sieudo.hold.outside"), true);
        }
        // 4 Manh: cot sang o 4 huong quanh vong giu
        if (s.t % 8 == 0)
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 2 * k + Math.PI / 4;
                double x = s.center.getX() + 0.5 + Math.cos(a) * 6, z = s.center.getZ() + 0.5 + Math.sin(a) * 6;
                sl.sendParticles(ParticleTypes.END_ROD, x, s.center.getY() + 1 + (s.t % 40) * 0.1, z, 2, 0.1, 0.4, 0.1, 0.0);
            }
        int wave = SieuDoKit.waveIndex(s.t), tc = SieuDoKit.waveTick(s.t);
        double r = SieuDoKit.waveRadius(wave);
        if (SieuDoKit.waveWarning(tc) && s.t % 4 == 0) ring(sl, s.center, r, ParticleTypes.SOUL_FIRE_FLAME, 36);
        if (SieuDoKit.waveBurst(tc)) {
            ring(sl, s.center, r, ParticleTypes.SONIC_BOOM, 8);
            ring(sl, s.center, r, ParticleTypes.SOUL_FIRE_FLAME, 48);
            sl.playSound(null, s.center, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2f, 0.7f);
            for (ServerPlayer p : near(s)) {
                if (p.isSpectator() || !p.isAlive()) continue;
                double d = Math.hypot(p.getX() - s.center.getX(), p.getZ() - s.center.getZ());
                if (SieuDoKit.waveHits(d, r, p.onGround())) p.hurt(sl.damageSources().magic(), SieuDoKit.waveDamage(p.getMaxHealth()));
            }
        }
    }

    private static void ring(ServerLevel sl, BlockPos c, double r, net.minecraft.core.particles.SimpleParticleType type, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            sl.sendParticles(type, c.getX() + 0.5 + Math.cos(a) * r, c.getY() + 0.2, c.getZ() + 0.5 + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ man ket
    private static void actors(Session s) {
        ServerLevel sl = s.level;
        BlockPos mo = s.center.offset(3, 0, 5), hc = s.center.offset(-3, 0, 5);
        StoryNpc m = RequiredNpcs.create(sl, "mo_dao", mo);
        if (m != null) {
            m.addTag(TAG_ACTOR);
            if (s.soul) m.setCustomName(Component.translatable("hukhong.npc.mo_dao_soul"));
        }
        if (s.rivalLost) {
            StoryNpc h = RequiredNpcs.create(sl, "hang_chieu", hc);
            if (h != null) h.addTag(TAG_ACTOR);
        }
    }

    private static void finish(Session s) {
        ServerPlayer owner = s.owner;
        cleanup(s);
        EndgameService.completeTruth(owner);
    }

    private static void fail(Session s) {
        ServerPlayer owner = s.owner;
        boolean online = !owner.hasDisconnected();
        cleanup(s);
        if (online) {
            owner.displayClientMessage(Component.translatable("hukhong.sieudo.failed"), false);
            if (owner.isAlive()) EndgameService.promptChoice(owner);
        }
    }

    private static void cleanup(Session s) {
        if (cur == s) cur = null;
        s.bar.removeAllPlayers();
        for (Entity e : s.level.getEntities((Entity) null, new AABB(s.center).inflate(60, 40, 60),
                x -> x.getTags().contains(TAG_SHADE) || x.getTags().contains(TAG_ACTOR) || x.getTags().contains(TAG_DAK))) e.discard();
    }

    /** Roi arena / tat server giua tran: huy phien (nguoi choi giu F_PENDING). */
    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        if (cur != null) cleanup(cur);
        cur = null;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (cur != null && cur.owner.getUUID().equals(e.getEntity().getUUID())) cleanup(cur);
    }

    /** Danh hieu "Sieu Do Gia" hien truoc ten nguoi choi (chat, bang ten). */
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat e) {
        if (e.getEntity() instanceof ServerPlayer sp && DialogueService.quest(sp).has(SieuDoKit.TITLE_FLAG))
            e.setDisplayname(Component.translatable("hukhong.title.sieu_do_gia").append(" ").append(e.getDisplayname()));
    }

    // ------------------------------------------------------------------ gui nguoi choi
    private static List<ServerPlayer> near(Session s) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : s.level.players())
            if (p.distanceToSqr(s.center.getX(), s.center.getY(), s.center.getZ()) < (double) PART_RANGE * PART_RANGE) l.add(p);
        return l;
    }

    private static void msg(Session s, String key) {
        for (ServerPlayer p : near(s)) p.displayClientMessage(Component.translatable(key), false);
    }

    /** Phu de ky uc (subtitle vanilla + chat) cho moi nguoi trong arena. */
    private static void subtitle(Session s, String key, String nameKey) {
        Component line = Component.translatable(key);
        Component who = Component.translatable(nameKey);
        for (ServerPlayer p : near(s)) {
            p.displayClientMessage(Component.translatable("hukhong.sieudo.say", who, line), false);
            if (p.connection == null) continue;
            try {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 70, 15));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
                p.connection.send(new ClientboundSetSubtitleTextPacket(line));
            } catch (RuntimeException ignored) { }
        }
    }

    private static void cutscene(Session s, List<CamPath.Key> keys, List<SubLine> subs, int ticks) {
        BlockPos at = s.center.offset(0, 0, -6);
        CutscenePayload pl = new CutscenePayload(-1, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, ticks, keys, subs);
        for (ServerPlayer p : near(s)) {
            try { PacketDistributor.sendToPlayer(p, pl); } catch (RuntimeException ignored) { }
        }
    }

    /** Huy phien dang chay (don thuc the). Cho GameTest / lenh. */
    public static void abort() { if (cur != null) cleanup(cur); }

    /** Cho GameTest: so tan anh con song. */
    public static int shadesAliveNow() { return cur == null ? 0 : shadesAlive(cur); }
}
