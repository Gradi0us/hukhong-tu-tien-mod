package com.hukhong.breakthrough;

import com.hukhong.HuKhong;
import com.hukhong.core.CultivationService;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.FxType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thiên Kiếp trước mốc đột phá: chuột phải Ấn Hồn Đài khi chưa độ kiếp -> bắt đầu tại đây.
 * Sống hết thời gian = qua kiếp (lưu cờ "hk_kiep_passed" = mốc cao nhất đã qua). Chết / đi quá 40 khối /
 * thoát game / đổi chiều = thất bại, được thử lại. Phiên chỉ nằm trong RAM; tâm ma bị gắn tag và dọn khi kết thúc.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ThienKiep {
    private ThienKiep() {}

    /** Khóa dữ liệu trên persistent data của người chơi: mốc cao nhất đã qua Thiên Kiếp. */
    public static final String PASS_KEY = "hk_kiep_passed";
    static final String MOB_TAG = "hk_thien_kiep_mob";
    /** Thời gian: 30s + 1s/mốc, tối đa 45s. Tâm ma xuất hiện giữa kiếp. */
    static final int BASE_SECONDS = 30, MAX_SECONDS = 45;
    /** INTRO: cảnh mở đầu (máy quay ngước lên lôi vân), chưa có sét. FINAL: lôi kiếp cuối trước khi kết thúc. */
    static final int INTRO = 100, STRIKE_EVERY = 60, TELEGRAPH = 20, FIRST_STRIKE = INTRO + 30, FINAL_WARN = 50, CLOUD_H = 18;
    static final double MAX_DIST = 40.0, STRIKE_RADIUS = 2.5;
    static final int WEAKNESS_TICKS = 600;

    private static final class Strike {
        final Vec3 pos; final long fireAt; final float radius; final boolean last;
        Strike(Vec3 pos, long fireAt) { this(pos, fireAt, (float) STRIKE_RADIUS, false); }
        Strike(Vec3 pos, long fireAt, float radius, boolean last) { this.pos = pos; this.fireAt = fireAt; this.radius = radius; this.last = last; }
    }

    private static final class Session {
        final UUID player; final int milestone; final ServerLevel level; final Vec3 start;
        final int duration; final ServerBossEvent bar;
        final List<Strike> strikes = new ArrayList<>();
        final List<UUID> mobs = new ArrayList<>();
        int ticks; boolean spawnedMobs, finalWave; int nextStrikeAt = FIRST_STRIKE;
        Session(ServerPlayer p, int milestone, int duration) {
            this.player = p.getUUID();
            this.milestone = milestone;
            this.level = (ServerLevel) p.level();
            this.start = p.position();
            this.duration = duration;
            this.bar = new ServerBossEvent(Component.translatable("hukhong.kiep.bar", milestone),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
            this.bar.setProgress(0f);
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    // ---------------------------------------------------------------- API

    public static int passedMilestone(Player p) { return p.getPersistentData().getInt(PASS_KEY); }

    /** Đã qua Thiên Kiếp cho mốc này (hoặc mốc cao hơn). */
    public static boolean isPassed(Player p, int milestone) { return passedMilestone(p) >= milestone; }

    public static boolean active(ServerPlayer p) { return SESSIONS.containsKey(p.getUUID()); }

    /** Bắt đầu kiếp cho mốc. false nếu đang có kiếp hoặc đã qua mốc này. */
    public static boolean begin(ServerPlayer p, int milestone) {
        if (active(p) || isPassed(p, milestone)) return false;
        int seconds = Math.min(MAX_SECONDS, BASE_SECONDS + milestone);
        Session s = new Session(p, milestone, INTRO + seconds * 20);
        SESSIONS.put(p.getUUID(), s);
        s.bar.addPlayer(p);
        p.sendSystemMessage(Component.translatable("hukhong.kiep.start", seconds));
        s.level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.6f, 0.5f);
        s.level.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.WEATHER, 0.5f, 0.5f);
        if (s.level.dimensionType().hasSkyLight()) s.level.setWeatherParameters(0, s.duration + 40, true, true); // trời đất biến sắc
        ThienKiepFx.intro(p, milestone);
        return true;
    }

    /** Qua kiếp: ghi cờ, hồi đầy máu, hiệu ứng. Chỉ gọi nội bộ / test. */
    static void succeed(ServerPlayer p) {
        Session s = SESSIONS.remove(p.getUUID());
        if (s == null) return;
        cleanup(s, p.server);
        int m = Math.max(s.milestone, passedMilestone(p));
        p.getPersistentData().putInt(PASS_KEY, m);
        p.setHealth(p.getMaxHealth());
        BreakthroughEffects.play(p, CultivationService.get(p));
        ThienKiepFx.success(p, s.start);
        p.sendSystemMessage(Component.translatable("hukhong.kiep.pass"));
    }

    /** Thất bại: dọn, debuff nhẹ (trừ khi đã chết / thoát), báo lý do. */
    static void fail(ServerPlayer p, String reasonKey, boolean debuff) {
        Session s = SESSIONS.remove(p.getUUID());
        if (s == null) return;
        cleanup(s, p.server);
        if (debuff && p.isAlive()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 1, false, true));
        ThienKiepFx.failure(p, s.level, s.start);
        p.sendSystemMessage(Component.translatable("hukhong.kiep.fail", Component.translatable(reasonKey)));
    }

    private static void cleanup(Session s, MinecraftServer srv) {
        s.bar.removeAllPlayers();
        if (s.level.dimensionType().hasSkyLight()) s.level.setWeatherParameters(12000, 0, false, false); // mây tan
        for (UUID id : s.mobs) {
            var e = s.level.getEntity(id);
            if (e != null) {
                s.level.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + 0.8, e.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
                e.discard();
            }
        }
        s.mobs.clear();
    }

    // ---------------------------------------------------------------- tick

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (SESSIONS.isEmpty()) return;
        MinecraftServer srv = e.getServer();
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            ServerPlayer p = srv.getPlayerList().getPlayer(s.player);
            if (p == null) continue; // đăng xuất: PlayerLoggedOutEvent xử lý
            if (!p.isAlive()) { fail(p, "hukhong.kiep.reason.death", false); continue; }
            if (p.level() != s.level || p.position().distanceToSqr(s.start) > MAX_DIST * MAX_DIST) {
                fail(p, "hukhong.kiep.reason.far", true);
                continue;
            }
            tick(p, s);
        }
    }

    private static void tick(ServerPlayer p, Session s) {
        s.ticks++;
        s.bar.setProgress(Math.min(1f, s.ticks / (float) s.duration));
        ServerLevel lv = s.level;

        if (s.ticks % 20 == 0) Fx.shake(lv, p.position(), s.ticks < INTRO ? 0.25f : 0.4f, 20, 32f);
        if (s.ticks % 80 == 1) {
            Fx.play(lv, FxType.STORM_CLOUD, s.start.add(0, CLOUD_H - 6, 0), Vec3.ZERO, 22f, 0x1C1C2C, 100);
            lv.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.6f, 1.2f);
        }
        ThienKiepFx.vortex(lv, s.start, s.ticks, Math.min(1f, s.ticks / (float) INTRO));
        if (s.ticks % 30 == 15) ThienKiepFx.distantFlash(lv, s.start);
        for (UUID id : s.mobs) { var m = lv.getEntity(id); if (m != null && s.ticks % 3 == 0) ThienKiepFx.demonAura(lv, m); }
        for (Strike k : s.strikes) ThienKiepFx.charge(lv, k.pos, 1f - (k.fireAt - s.ticks) / (float) (k.last ? FINAL_WARN : TELEGRAPH), k.last);
        if (s.ticks < INTRO) return; // cảnh mở đầu: chỉ có mây tụ
        if (!s.finalWave && s.ticks >= s.duration - FINAL_WARN - 10) { // lôi kiếp cuối: một cột sét lớn nhắm vào chỗ người chơi đứng
            s.finalWave = true;
            s.strikes.add(new Strike(p.position(), s.duration - 10, 4.5f, true));
            Fx.groundRing(lv, p, p.position(), 4.5f, 0xFFF2B0, FINAL_WARN + 2);
            Fx.rune(lv, p, p.position(), 5f, 0xFFE070, FINAL_WARN + 2, 8);
            ThienKiepFx.title(p, "hukhong.kiep.final_title", "hukhong.kiep.final_sub", 0xFFE070);
            lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.WEATHER, 2f, 0.5f);
        }

        if (!s.spawnedMobs && s.ticks >= s.duration / 2) {
            s.spawnedMobs = true;
            spawnHeartDemons(p, s);
        }

        if (!s.finalWave && s.ticks >= FIRST_STRIKE && s.ticks >= s.nextStrikeAt) {
            s.nextStrikeAt += STRIKE_EVERY;
            scheduleWave(p, s);
        }

        for (var it = s.strikes.iterator(); it.hasNext(); ) {
            Strike k = it.next();
            if (s.ticks < k.fireAt) continue;
            it.remove();
            strike(p, s, k);
        }

        if (s.ticks >= s.duration) succeed(p);
    }

    /** Mỗi đợt: 1..4 cột sét (tăng theo mốc). Mỗi cột báo trước 1 giây bằng vòng sáng trên đất. */
    private static void scheduleWave(ServerPlayer p, Session s) {
        int n = Math.min(4, 1 + s.milestone / 3);
        long fire = s.ticks + TELEGRAPH;
        for (int i = 0; i < n; i++) {
            Vec3 pos = i == 0 ? p.position()
                    : p.position().add((p.getRandom().nextDouble() - 0.5) * 8, 0, (p.getRandom().nextDouble() - 0.5) * 8);
            s.strikes.add(new Strike(pos, fire));
            Fx.groundRing(s.level, p, pos, (float) STRIKE_RADIUS, 0xB8D8FF, TELEGRAPH + 2);
        }
    }

    /** Sát thương = % máu tối đa, 8% ở mốc 1, +0.5%/mốc, tối đa 15%. */
    static float strikeFraction(int milestone) { return (float) Math.min(0.15, 0.08 + 0.005 * milestone); }

    private static void strike(ServerPlayer p, Session s, Strike k) {
        ServerLevel lv = s.level;
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(lv);
        if (b != null) { b.moveTo(k.pos); b.setVisualOnly(true); lv.addFreshEntity(b); }
        Fx.play(lv, FxType.LIGHTNING_BOLT, k.pos, Vec3.ZERO, k.last ? 3f : 1.2f, 0xB8D8FF);
        ThienKiepFx.impact(p, lv, k.pos, k.radius, k.last);
        double dx = p.getX() - k.pos.x, dz = p.getZ() - k.pos.z;
        if (dx * dx + dz * dz <= k.radius * k.radius)
            p.hurt(p.damageSources().lightningBolt(), p.getMaxHealth() * strikeFraction(s.milestone) * (k.last ? 1.6f : 1f));
    }

    /** Tâm Ma: 1..3 Vex thù địch quanh người chơi, gắn tag để dọn khi kết thúc. */
    private static void spawnHeartDemons(ServerPlayer p, Session s) {
        int n = Math.min(3, 1 + s.milestone / 4);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            Mob m = EntityType.VEX.create(s.level);
            if (m == null) continue;
            m.moveTo(p.getX() + Math.cos(a) * 6, p.getY(), p.getZ() + Math.sin(a) * 6, 0f, 0f);
            m.setCustomName(Component.translatable("hukhong.kiep.tam_ma"));
            m.setCustomNameVisible(true);
            m.setPersistenceRequired();
            m.addTag(MOB_TAG);
            s.level.addFreshEntity(m);
            m.setTarget(p);
            s.mobs.add(m.getUUID());
            ThienKiepFx.demonRise(s.level, m);
        }
        s.level.playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 1f, 0.7f);
        s.level.playSound(null, p.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.6f);
        ThienKiepFx.title(p, "", "hukhong.kiep.tam_ma_sub", 0xC060FF);
    }

    // ---------------------------------------------------------------- sự kiện thoát / chết / dừng

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && active(p)) fail(p, "hukhong.kiep.reason.death", false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && active(p)) fail(p, "hukhong.kiep.reason.logout", false);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (Session s : new ArrayList<>(SESSIONS.values())) {
            cleanup(s, e.getServer());
        }
        SESSIONS.clear();
    }

    /** Tâm ma lưu trong chunk (server tắt giữa kiếp) -> bỏ khi nạp lại. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() && e.getEntity().getTags().contains(MOB_TAG)) e.setCanceled(true);
    }
}
