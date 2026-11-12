package com.hukhong.core;

import com.hukhong.HuKhong;
import com.hukhong.arena.BossArena;
import com.hukhong.boss.phase.BossPhaseConfig;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.endgame.EndgameService;
import com.hukhong.endgame.NguyenThuyTichDiet;
import com.hukhong.story.flow.RequiredNpcs;
import com.hukhong.story.nemesis.NemesisService;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.vfx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cutscene một lần khi đạt cấp 10: bóng Nguyên Thủy Tịch Diệt (dạng 1, bất tử, đứng yên) giơ búa 6 block trước mặt người chơi,
 * Mộ Dao chen giữa đỡ 2 nhát (VFX va chạm, rung camera), đối thoại qua phụ đề, bóng tan, Mộ Dao biến mất.
 * Người chơi bất tử + bị giữ tại chỗ trong suốt ({@link #TICKS} tick). Chạy phía server, không gây sát thương.
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class Lv10Cutscene {
    private Lv10Cutscene() {}

    /** Cờ quest: đã xem cutscene (chỉ đặt bởi đường lên cấp tự nhiên). */
    public static final String FLAG = "lv10_cutscene_seen";
    /** Tổng thời lượng (tick) ~ 14 giây. */
    public static final int TICKS = 280;
    private static final int DUR = TICKS + 40;
    private static final int RES_AMP = 4, SLOW_AMP = 9;
    static final String TAG_MO = "hk_lv10_mo";

    private static final class Run {
        final ServerLevel level;
        final Vec3 origin;      // vị trí người chơi lúc bắt đầu (neo, giữ người chơi đứng yên)
        final float yaw, pitch;
        final Vec3 u;           // hướng ngang từ người chơi tới bóng
        final NguyenThuyTichDiet shade;
        StoryNpc mo;
        int t;
        Run(ServerLevel level, Vec3 origin, float yaw, float pitch, Vec3 u, NguyenThuyTichDiet shade) {
            this.level = level; this.origin = origin; this.yaw = yaw; this.pitch = pitch; this.u = u; this.shade = shade;
        }
    }

    /** uuid người chơi -> lượt đang chạy. */
    private static final Map<UUID, Run> RUNS = new HashMap<>();

    /** Gọi từ CultivationService khi cấp vượt qua 10. Đặt cờ rồi thử chạy (bỏ qua im lặng nếu không phù hợp). */
    public static void onReach10(ServerPlayer p) {
        QuestData q = DialogueService.quest(p);
        if (q.has(FLAG)) return;
        DialogueService.setQuest(p, q.withFlag(FLAG));
        start(p);
    }

    /**
     * Bắt đầu cutscene cho người chơi. Không chạy khi: đang ở Hư Không Bản Nguyên, đang bị nhốt trong đấu trường boss,
     * đang trận Nemesis, hoặc đã có lượt đang chạy.
     *
     * @return true nếu đã bắt đầu
     */
    public static boolean start(ServerPlayer p) {
        if (RUNS.containsKey(p.getUUID())) return false;
        if (!(p.level() instanceof ServerLevel sl)) return false;
        if (sl.dimension() == EndgameRegistry.DIM || BossArena.isEngaged(p) || NemesisService.fighting(p)) return false;

        Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
        Vec3 u = look.normalize();
        Vec3 pos = p.position();

        NguyenThuyTichDiet shade = EndgameRegistry.BOSS.get().create(sl);
        if (shade == null) return false;
        Vec3 spot = pos.add(u.scale(6));
        shade.moveTo(spot.x, pos.y, spot.z, yawOf(u.scale(-1)), 0f);   // quay mặt về người chơi
        shade.finalizeSpawn(sl, sl.getCurrentDifficultyAt(shade.blockPosition()), MobSpawnType.EVENT, null);
        shade.addTag(NguyenThuyTichDiet.LV10_TAG);
        shade.setNoAi(true);
        shade.setInvulnerable(true);
        shade.setHealth(shade.getMaxHealth());
        sl.addFreshEntity(shade);

        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DUR, RES_AMP, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DUR, SLOW_AMP, false, false));

        Run r = new Run(sl, pos, p.getYRot(), p.getXRot(), u, shade);
        RUNS.put(p.getUUID(), r);
        BossPhaseDirector.sendCutscene(sl, shade, TICKS, camPath(), subtitles());
        return true;
    }

    /** Camera neo theo bóng; khung nhìn cả bóng lẫn Mộ Dao chen giữa (toạ độ tương đối với bóng). */
    private static List<CamPath.Key> camPath() {
        return List.of(
                new CamPath.Key(0, 0f, 0.8f, 7.0f, 0, 0, 70, 1.6f),
                new CamPath.Key(TICKS / 3, 2.5f, 1.0f, 5.0f, 0, 0, 65, 1.5f),
                new CamPath.Key(TICKS * 2 / 3, -2.0f, 0.9f, 5.5f, 0, 0, 62, 1.5f),
                new CamPath.Key(TICKS, 0f, 1.2f, 8.0f, 0, 0, 72, 1.6f));
    }

    private static List<BossPhaseConfig.SubLine> subtitles() {
        return List.of(
                new BossPhaseConfig.SubLine(8, 46, "hukhong.dialog.lv10.nttd.1"),
                new BossPhaseConfig.SubLine(46, 86, "hukhong.dialog.lv10.nttd.2"),
                new BossPhaseConfig.SubLine(92, 130, "hukhong.dialog.lv10.mo_dao.1"),
                new BossPhaseConfig.SubLine(130, 170, "hukhong.dialog.lv10.mo_dao.2"),
                new BossPhaseConfig.SubLine(176, 214, "hukhong.dialog.lv10.nttd.3"),
                new BossPhaseConfig.SubLine(214, 244, "hukhong.dialog.lv10.mo_dao.3"),
                new BossPhaseConfig.SubLine(246, 262, "hukhong.dialog.lv10.mo_dao.4"));
    }

    /** Yaw (độ) của hướng ngang d theo quy ước Minecraft. */
    private static float yawOf(Vec3 d) {
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    // ------------------------------------------------------------------ tick
    public static void tick(MinecraftServer s) {
        if (RUNS.isEmpty()) return;
        var it = RUNS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Run r = en.getValue();
            ServerPlayer p = s.getPlayerList().getPlayer(en.getKey());
            if (p == null || !p.isAlive() || p.serverLevel() != r.level) { // logout / chết / đổi chiều giữa cutscene
                it.remove();
                end(r, p);
                continue;
            }
            r.t++;
            if (p.position().distanceToSqr(r.origin) > 0.0025) p.connection.teleport(r.origin.x, r.origin.y, r.origin.z, r.yaw, r.pitch);
            step(r);
            if (r.t >= TICKS) { it.remove(); end(r, p); }
        }
    }

    private static void step(Run r) {
        ServerLevel sl = r.level;
        if (r.shade.isAlive() && r.t < 236 && r.t % 4 == 0) // sương Hư Không mờ quanh bóng
            sl.sendParticles(ParticleTypes.PORTAL, r.shade.getX(), r.shade.getY() + 1.4, r.shade.getZ(), 4, 0.4, 0.8, 0.4, 0.05);
        switch (r.t) {
            case 20 -> swing(r);                       // nhát 1 giơ búa
            case 26 -> spawnMo(r);                     // Mộ Dao lao vào giữa
            case 30 -> clash(r);                       // va chạm 1
            case 44 -> swing(r);                       // nhát 2
            case 50 -> clash(r);                       // va chạm 2
            case 236 -> dissolveShade(r);
            case 250 -> { if (r.shade.isAlive()) r.shade.discard(); }
            case 258 -> dissolveMo(r);
            case 262 -> { if (r.mo != null && r.mo.isAlive()) r.mo.discard(); }
            default -> { }
        }
    }

    private static void swing(Run r) {
        if (r.shade.isAlive()) {
            r.shade.triggerAnim("skill", "f1_attack");
            r.shade.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2f, 0.5f);
        }
    }

    private static void spawnMo(Run r) {
        Vec3 spot = r.origin.add(r.u.scale(3));
        StoryNpc mo = RequiredNpcs.create(r.level, "mo_dao", BlockPos.containing(spot));
        if (mo == null) return;
        mo.moveTo(spot.x, spot.y, spot.z, yawOf(r.u), 0f);   // đối mặt bóng
        mo.setYBodyRot(yawOf(r.u));
        mo.setYHeadRot(yawOf(r.u));
        mo.setNoAi(true);
        mo.addTag(TAG_MO);
        r.mo = mo;
        r.level.sendParticles(ParticleTypes.ENCHANT, spot.x, spot.y + 1.2, spot.z, 24, 0.6, 1.0, 0.6, 0.2);
    }

    /** Va chạm: VFX, tiếng nổ không sát thương, rung camera nhẹ. */
    private static void clash(Run r) {
        ServerLevel sl = r.level;
        Vec3 mid = r.origin.add(r.u.scale(3)).add(0, 1.2, 0);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 3, 0.4, 0.3, 0.4, 0);
        sl.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 30, 0.8, 0.6, 0.8, 0.3);
        sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 20, 0.6, 0.6, 0.6, 0.15);
        sl.sendParticles(ParticleTypes.EXPLOSION, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
        sl.playSound(null, mid.x, mid.y, mid.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
        Fx.shake(sl, mid, 0.6f, 12, 14f);
    }

    private static void dissolveShade(Run r) {
        if (!r.shade.isAlive()) return;
        r.level.sendParticles(ParticleTypes.REVERSE_PORTAL, r.shade.getX(), r.shade.getY() + 1.4, r.shade.getZ(), 40, 0.6, 1.0, 0.6, 0.1);
        r.level.sendParticles(ParticleTypes.SOUL, r.shade.getX(), r.shade.getY() + 1.0, r.shade.getZ(), 15, 0.5, 0.8, 0.5, 0.02);
    }

    private static void dissolveMo(Run r) {
        if (r.mo == null || !r.mo.isAlive()) return;
        r.level.sendParticles(ParticleTypes.END_ROD, r.mo.getX(), r.mo.getY() + 1.2, r.mo.getZ(), 40, 0.5, 0.9, 0.5, 0.1);
    }

    /** Dọn thực thể + gỡ hiệu ứng ta đã thêm (chỉ gỡ đúng bản không hiện, cùng cấp độ). */
    private static void end(Run r, ServerPlayer p) {
        if (r.shade.isAlive()) r.shade.discard();
        if (r.mo != null && r.mo.isAlive()) r.mo.discard();
        if (p != null) stripOurs(p);
    }

    private static void stripOurs(ServerPlayer p) {
        var res = p.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (res != null && res.getAmplifier() == RES_AMP && !res.isVisible()) p.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        var slow = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slow != null && slow.getAmplifier() == SLOW_AMP && !slow.isVisible()) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    /** Lệnh debug: chạy lại (bỏ qua cờ). Vẫn tôn trọng điều kiện không chạy trong endgame/trận. */
    public static boolean replay(ServerPlayer p) { return start(p); }

    // ------------------------------------------------------------------ sự kiện
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) { tick(e.getServer()); }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        // dọn bóng/Mộ Dao sót lại sau khi server tắt giữa cutscene (không thuộc lượt nào đang chạy)
        Set<UUID> live = new HashSet<>();
        for (Run r : RUNS.values()) { live.add(r.shade.getUUID()); if (r.mo != null) live.add(r.mo.getUUID()); }
        ServerLevel sl = p.serverLevel();
        List<Entity> orphans = new ArrayList<>(sl.getEntities((Entity) null, new AABB(p.blockPosition()).inflate(120),
                x -> (x.getTags().contains(NguyenThuyTichDiet.LV10_TAG) || x.getTags().contains(TAG_MO)) && !live.contains(x.getUUID())));
        for (Entity x : orphans) x.discard();
        if (!EndgameService.hasEnding(DialogueService.quest(p))) stripOurs(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Run r = RUNS.remove(p.getUUID());
        if (r != null) end(r, p);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent e) {
        for (var en : RUNS.entrySet()) {
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            end(en.getValue(), p);
        }
        RUNS.clear();
    }
}
