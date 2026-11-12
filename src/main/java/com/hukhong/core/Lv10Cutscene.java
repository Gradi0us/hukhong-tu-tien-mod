package com.hukhong.core;

import com.hukhong.HuKhong;
import com.hukhong.arena.BossArena;
import com.hukhong.boss.phase.BossPhaseConfig;
import com.hukhong.boss.phase.BossPhaseDirector;
import com.hukhong.boss.phase.CamPath;
import com.hukhong.endgame.EndgameRegistry;
import com.hukhong.endgame.EndgameService;
import com.hukhong.endgame.MoDaoCombat;
import com.hukhong.endgame.NguyenThuyTichDiet;
import com.hukhong.endgame.NttdAllies;
import com.hukhong.story.nemesis.NemesisService;
import com.hukhong.story.npc.DialogueService;
import com.hukhong.story.npc.QuestData;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import java.util.function.DoubleUnaryOperator;

/**
 * Cutscene một lần khi đạt cấp 10 (PLAN-v1-nttd-story-rework §3): hoá thân Nguyên Thủy Tịch Diệt chui ra khỏi vết nứt, bay vào
 * theo đường cong rồi bổ búa; Mộ Dao ({@link MoDaoCombat}) lao tới, rút kiếm khỏi vỏ, đỡ 2 nhát, phản đòn, đối đầu, chém xuyên
 * bóng (Thanh Minh Nhất Tuyến), tra kiếm vào vỏ rồi biến mất. Người chơi bất tử + bị giữ tại chỗ suốt {@link #TICKS} tick.
 * Chạy phía server, không gây sát thương.
 * <p>Camera neo điểm (origin, hướng người chơi). CutsceneClient luôn nhìn vào origin+lookH và Key.yaw/pitch là độ LỆCH cộng
 * thêm vào hướng nhìn đó (không phải góc tuyệt đối), nên mỗi key ở đây khai báo điểm nhìn rồi quy đổi sang lệch ({@link #key}).
 */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class Lv10Cutscene {
    private Lv10Cutscene() {}

    /** Cờ quest: đã xem cutscene (chỉ đặt bởi đường lên cấp tự nhiên). */
    public static final String FLAG = "lv10_cutscene_seen";
    /** Tổng thời lượng (tick) ~ 17 giây. */
    public static final int TICKS = 340;
    private static final int DUR = TICKS + 40;
    private static final int RES_AMP = 4, SLOW_AMP = 9;
    static final String TAG_MO = "hk_lv10_mo";
    private static final int VOIDC = Fx.PURPLE, MO_C = NttdAllies.MO_COLOR, RED = Fx.RED;

    // vị trí cục bộ {dx phải, dy lên, dz trước mặt người chơi}
    private static final double[] RIFT = {0, 6, 28}, BEND = {4, 5, 14}, HOVER = {0, 2.5, 6}, LUNGE = {0, 1.0, 4.5}, BACK = {0, 2.5, 8.5};
    private static final double[] MO_IN = {8, 0, -4}, MO_GUARD = {0, 0, 2.5}, MO_SLID = {0, 0, 1.0}, MO_PAST = {0, 0, 14.5};

    private static final class Run {
        final ServerLevel level;
        final Vec3 origin;      // vị trí người chơi lúc bắt đầu (neo, giữ người chơi đứng yên)
        final float yaw, pitch;
        final Vec3 u, r;        // hướng ngang phía trước / bên phải người chơi
        final NguyenThuyTichDiet shade;
        MoDaoCombat mo;
        Vec3 last;
        int t;
        Run(ServerLevel level, Vec3 origin, float yaw, float pitch, Vec3 u, NguyenThuyTichDiet shade) {
            this.level = level; this.origin = origin; this.yaw = yaw; this.pitch = pitch; this.u = u; this.shade = shade;
            this.r = new Vec3(-u.z, 0, u.x);
        }
        Vec3 L(double dx, double dy, double dz) { return origin.add(r.scale(dx)).add(u.scale(dz)).add(0, dy, 0); }
        Vec3 L(double[] v) { return L(v[0], v[1], v[2]); }
        Vec3 player() { return origin.add(0, 1.0, 0); }
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
        Run run = new Run(sl, pos, p.getYRot(), p.getXRot(), u, shade);
        Vec3 spot = run.L(RIFT);                     // ẩn trong vết nứt, hiện ra ở tick 24
        shade.moveTo(spot.x, spot.y, spot.z, yawOf(u.scale(-1)), 0f);   // quay mặt về người chơi
        shade.finalizeSpawn(sl, sl.getCurrentDifficultyAt(shade.blockPosition()), MobSpawnType.EVENT, null);
        shade.addTag(NguyenThuyTichDiet.LV10_TAG);
        shade.setNoAi(true);
        shade.setNoGravity(true);
        shade.setInvisible(true);
        shade.setInvulnerable(true);
        shade.setHealth(shade.getMaxHealth());
        sl.addFreshEntity(shade);

        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DUR, RES_AMP, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DUR, SLOW_AMP, false, false));

        RUNS.put(p.getUUID(), run);
        BossPhaseDirector.sendCutscene(sl, null, pos, yawOf(u), TICKS, camPath(), subtitles());
        return true;
    }

    // ------------------------------------------------------------------ camera
    /**
     * Key §3.2 dạng (vị trí camera P, điểm nhìn F) cục bộ. Client nhìn vào (0, lookH, 0) rồi cộng lệch yaw/pitch, nên
     * lookH = F.y, lệch = góc(P→F) − góc(P→(0,F.y,0)). Yaw cục bộ = atan2(dx, dz), khớp khung fwd/right của CutsceneClient.
     */
    static CamPath.Key key(int t, double px, double py, double pz, double fx, double fy, double fz, double fov) {
        double yawF = Math.toDegrees(Math.atan2(fx - px, fz - pz)), yawL = Math.toDegrees(Math.atan2(-px, -pz));
        double hzF = Math.hypot(fx - px, fz - pz), hzL = Math.max(1e-3, Math.hypot(px, pz));
        double pitF = -Math.toDegrees(Math.atan2(fy - py, hzF)), pitL = -Math.toDegrees(Math.atan2(fy - py, hzL));
        return new CamPath.Key(t, (float) px, (float) py, (float) pz, Mth.wrapDegrees((float) (yawF - yawL)), (float) (pitF - pitL),
                (float) fov, (float) fy);
    }

    private static List<CamPath.Key> camPath() {
        return List.of(
                key(0, 0.8, 2.0, -2.5, 0, 6, 28, 60),       // sau lưng người chơi nhìn vết nứt
                key(40, 1.5, 1.6, -1.5, 2, 4, 12, 66),      // theo bóng bay tới
                key(58, 6.5, 1.6, 2.5, 4, 1.2, -0.5, 55),   // Mộ Dao lao vào từ bên phải
                key(66, 1.6, 1.1, 4.6, 0, 1.0, 2.5, 40),    // cận tay rút kiếm
                key(74, 4.5, 1.8, 4.0, 0, 2.0, 3.5, 50),    // va chạm 1, ngang
                key(92, -4.0, 0.6, 4.5, 0, 1.8, 3.0, 52),   // va chạm 2, góc thấp đối diện
                key(110, 0, 2.6, -3.5, 0, 2.0, 6, 70),      // đối đầu, rộng
                key(160, -3.5, 2.0, 1.0, 0, 2.0, 5, 62),    // xoay chậm sang trái
                key(210, 1.2, 1.6, 3.0, 0, 1.5, 1.0, 45),   // cận Mộ Dao trước cú lướt
                key(218, 0, 5.0, 4.0, 0, 1.0, 10, 70),      // theo cú lướt, góc cao
                key(236, 0, 2.2, 1.0, 0, 3.0, 8, 68),       // bóng tan
                key(260, 2.2, 1.4, 12.0, 0, 1.1, 14.5, 42), // cận tra kiếm
                key(300, 2.4, 1.8, 4.8, 0, 1.5, 1.4, 58),   // Mộ Dao + người chơi
                key(TICKS, 0, 1.7, -0.2, 0, 1.6, 3, 70));   // trả về sau lưng người chơi
    }

    private static List<BossPhaseConfig.SubLine> subtitles() {
        return List.of(
                new BossPhaseConfig.SubLine(28, 62, "hukhong.dialog.lv10.nttd.1"),
                new BossPhaseConfig.SubLine(66, 100, "hukhong.dialog.lv10.mo_dao.1"),
                new BossPhaseConfig.SubLine(104, 142, "hukhong.dialog.lv10.nttd.2"),
                new BossPhaseConfig.SubLine(142, 198, "hukhong.dialog.lv10.mo_dao.2"),
                new BossPhaseConfig.SubLine(222, 256, "hukhong.dialog.lv10.nttd.3"),
                new BossPhaseConfig.SubLine(266, 298, "hukhong.dialog.lv10.mo_dao.3"),
                new BossPhaseConfig.SubLine(298, 322, "hukhong.dialog.lv10.mo_dao.4"));
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

    private static double easeIn(double k) { return k * k; }
    private static double easeOut(double k) { return 1 - (1 - k) * (1 - k); }
    private static double lin(double k) { return k; }

    /** Nội suy a → b trong [t0, t1] theo ease. */
    private static Vec3 beat(Vec3 a, Vec3 b, int t, int t0, int t1, DoubleUnaryOperator ease) {
        double k = ease.applyAsDouble(Mth.clamp((t - t0) / (double) Math.max(1, t1 - t0), 0, 1));
        return a.add(b.subtract(a).scale(k));
    }

    private static Vec3 bezier(Vec3 p0, Vec3 p1, Vec3 p2, double k) {
        double m = 1 - k;
        return p0.scale(m * m).add(p1.scale(2 * m * k)).add(p2.scale(k * k));
    }

    private static void placeShade(Run r, Vec3 p) {
        float y = yawOf(r.player().subtract(p).multiply(1, 0, 1));
        r.shade.moveTo(p.x, p.y, p.z, y, 0f);
        r.shade.setYBodyRot(y);
        r.shade.setYHeadRot(y);
        r.shade.setDeltaMovement(Vec3.ZERO);
    }

    private static void moAt(Run r, Vec3 p, Vec3 face) {
        if (r.mo == null || !r.mo.isAlive()) return;
        r.mo.glide(new Vec3(p.x, r.mo.groundAt(p.x, r.origin.y + 1, p.z), p.z), face);
    }

    private static void step(Run r) {
        ServerLevel sl = r.level;
        int t = r.t;
        NguyenThuyTichDiet sh = r.shade;

        // ---------------- bóng NTTD: quỹ đạo
        if (sh.isAlive() && t >= 24 && t < 250) {
            Vec3 p;
            if (t <= 64) p = bezier(r.L(RIFT), r.L(BEND), r.L(HOVER), easeIn((t - 24) / 40.0));
            else if (t < 72) p = r.L(HOVER);
            else if (t < 74) p = beat(r.L(HOVER), r.L(LUNGE), t, 72, 74, Lv10Cutscene::lin);
            else if (t < 78) p = r.L(LUNGE);                                         // hit-stop
            else if (t < 96) p = r.L(LUNGE).add(r.u.scale(t >= 84 && t < 92 ? 0.4 : 0));
            else if (t <= 104) p = beat(r.L(LUNGE), r.L(BACK), t, 96, 104, Lv10Cutscene::easeOut);
            else p = r.L(BACK).add(0, Math.sin(t * 0.12) * 0.25, 0);
            placeShade(r, p);
            if (t <= 64 && t % 4 == 0 && r.last != null) Fx.ribbon(sl, null, r.last.add(0, 2, 0), p.add(0, 2, 0), 0, 6, 0, 0.4f, VOIDC);
            r.last = p;
            if (t % 4 == 0) sl.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 2.5, p.z, 6, 0.6, 1.2, 0.6, 0.05);
        }

        // ---------------- Mộ Dao: quỹ đạo
        if (r.mo != null && r.mo.isAlive()) {
            Vec3 shadeP = sh.isAlive() ? sh.position() : r.L(BACK);
            if (t <= 66) {
                moAt(r, beat(r.L(MO_IN), r.L(MO_GUARD), t, 50, 66, Lv10Cutscene::easeOut), t < 62 ? r.L(MO_GUARD).add(r.u) : shadeP);
                if (t % 2 == 0) sl.sendParticles(ParticleTypes.CLOUD, r.mo.getX(), r.mo.getY() + 0.1, r.mo.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
            } else if (t >= 78 && t <= 84) moAt(r, beat(r.L(MO_GUARD), r.L(MO_SLID), t, 78, 84, Lv10Cutscene::easeOut), shadeP);
            else if (t > 84 && t < 214) moAt(r, r.L(MO_SLID), shadeP);
            else if (t >= 214 && t <= 218) moAt(r, beat(r.L(MO_SLID), r.L(MO_PAST), t, 214, 218, Lv10Cutscene::lin), r.L(MO_PAST).add(r.u));
            else if (t >= 248 && t < 270) moAt(r, r.L(MO_PAST), r.player());
            else if (t >= 270 && t <= 296) moAt(r, beat(r.L(MO_PAST), r.L(MO_GUARD), t, 270, 296, Lv10Cutscene::lin), r.player());
            else if (t > 296) moAt(r, r.L(MO_GUARD), r.player());
            if (t >= 104 && t < 200 && t % 6 == 0) { // ánh kiếm dọc lưỡi
                Vec3 b = r.mo.position().add(r.u.scale(0.6)).add(0, 1.3, 0);
                sl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 2, 0.05, 0.3, 0.05, 0.005);
            }
        }

        switch (t) {
            case 1 -> {
                Fx.rune(sl, null, r.L(0, 12, 20), 5f, VOIDC, 80, 7);
                sound(sl, r.L(RIFT), SoundEvents.WARDEN_EMERGE, 2f, 0.6f);
            }
            case 10 -> {
                Fx.rift(sl, null, r.L(RIFT), new Vec3(0, 1, 0), 4f, VOIDC, 60);
                sound(sl, r.L(RIFT), SoundEvents.END_PORTAL_SPAWN, 1.2f, 0.6f);
            }
            case 24 -> {
                sh.setInvisible(false);
                sh.triggerAnim("skill", "f1_teleport"); // f1_fly_in có trong JSON nhưng chưa đăng ký trigger (NguyenThuyTichDiet, C2)
                Vec3 p = r.L(RIFT);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 2, p.z, 60, 1, 2, 1, 0.2);
                Fx.shake(sl, r.origin, 0.4f, 10, 40f);
            }
            case 50 -> spawnMo(r);
            case 64 -> {
                Fx.telegraphLine(sl, null, r.L(0, 0.1, 6), r.u.scale(-6), 2f, 16, RED);
                sound(sl, r.L(HOVER), SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.8f);
            }
            case 66 -> {
                sh.triggerAnim("skill", "f1_attack");
                if (r.mo != null && r.mo.isAlive()) {
                    r.mo.drawSword();
                    Vec3 hip = r.mo.position().add(0, 1.0, 0);
                    Fx.qi(sl, hip, r.u.scale(1.5).add(r.r.scale(-0.8)), 0.8f, 0xFFFFFF, 30, 6);
                    sound(sl, hip, SoundEvents.TRIDENT_RETURN, 1.5f, 1.2f);
                    sound(sl, hip, SoundEvents.ANVIL_LAND, 0.5f, 2.0f);
                }
            }
            case 72 -> moAnim(r, "parry_high");
            case 74 -> clash(r, 1.0f, true);
            case 84 -> {
                sh.triggerAnim("skill", "f1_combo");
                Fx.telegraphCone(sl, null, r.L(0, 0.1, 4.5), r.u.scale(-1), 4f, 50f, 8);
            }
            case 90 -> moAnim(r, "parry_low");
            case 92 -> clash(r, 0.8f, false);
            case 94 -> {
                moAnim(r, "counter_slash");
                Vec3 a = r.L(0, 1.4, 1.5), b = sh.position().add(0, 2.5, 0);
                Fx.qi(sl, a, b.subtract(a), 2.4f, MO_C, -35, 8);
                sl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 30, 0.8, 1.2, 0.8, 0.15);
                sound(sl, b, SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.9f);
                Fx.shake(sl, b, 0.8f, 10, 30f);
            }
            case 96 -> sh.triggerAnim("brain", "f1_backstep");
            case 200 -> {
                sh.triggerAnim("skill", "f1_ult");
                Fx.rune(sl, null, r.L(0, 0.1, 8.5), 3.5f, VOIDC, 30, 6);
                sound(sl, sh.position(), SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.6f);
            }
            case 206 -> Fx.telegraphLine(sl, null, r.L(0, 0.1, 1.0), r.u.scale(13.5), 1.4f, 8, MO_C);
            case 212 -> moAnim(r, "dash_slash");
            case 214 -> {
                for (int i = 0; i < 3; i++)
                    Fx.ribbon(sl, null, r.L(0, 1.0 + i * 0.25, 1.0), r.L(0, 1.0 + i * 0.25, 14.5), 0.3 * (i - 1), 4, i, 0.6f, MO_C);
                sound(sl, r.L(0, 1, 6), SoundEvents.TRIDENT_RIPTIDE_3.value(), 2f, 1.3f);
            }
            case 216 -> {
                Vec3 b = r.L(0, 2.5, 8.5);
                Fx.qi(sl, b.subtract(r.u.scale(2)), r.u.scale(4), 3f, MO_C, 0, 10);
                Fx.shake(sl, b, 1.2f, 14, 40f);
                sound(sl, b, SoundEvents.PLAYER_ATTACK_CRIT, 2f, 0.8f);
            }
            case 222 -> {
                sh.triggerAnim("brain", "f1_struggle");
                Vec3 b = sh.position().add(0, 2.5, 0);
                Fx.rift(sl, null, b, r.r, 1.6f, MO_C, 30);
                sl.sendParticles(ParticleTypes.SOUL, b.x, b.y, b.z, 20, 0.8, 1.2, 0.8, 0.03);
            }
            case 236 -> dissolveShade(r);
            case 250 -> { if (sh.isAlive()) sh.discard(); }
            case 256 -> {
                if (r.mo != null && r.mo.isAlive()) {
                    r.mo.sheatheSword();
                    sound(sl, r.mo.position(), SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5f, 1.2f);
                }
            }
            case 264 -> {
                if (r.mo != null && r.mo.isAlive()) {
                    Vec3 h = r.mo.position().add(0, 1.0, 0);
                    sl.sendParticles(ParticleTypes.END_ROD, h.x, h.y, h.z, 8, 0.1, 0.1, 0.1, 0.05);
                    sound(sl, h, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
                }
            }
            case 298, 304 -> moAnim(r, "talk");
            case 316 -> dissolveMo(r);
            case 320 -> { if (r.mo != null && r.mo.isAlive()) r.mo.discard(); }
            default -> { }
        }
    }

    private static void moAnim(Run r, String a) { if (r.mo != null && r.mo.isAlive()) r.mo.anim(a); }

    private static void sound(ServerLevel sl, Vec3 p, SoundEvent s, float vol, float pitch) {
        sl.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void spawnMo(Run r) {
        Vec3 spot = r.L(MO_IN);
        MoDaoCombat mo = MoDaoCombat.create(r.level, spot, yawOf(r.L(MO_GUARD).subtract(spot)), true);
        if (mo == null) return;
        mo.addTag(TAG_MO);
        mo.anim("dash");
        r.mo = mo;
        r.level.sendParticles(ParticleTypes.ENCHANT, spot.x, spot.y + 1.2, spot.z, 24, 0.6, 1.0, 0.6, 0.2);
        Fx.ribbon(r.level, null, spot.add(0, 0.8, 0), r.L(0, 0.8, 2.5), 0.8, 16, 0, 0.35f, MO_C);
        sound(r.level, spot, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.6f);
    }

    /** Va chạm kiếm - búa giữa Mộ Dao và bóng: VFX, tiếng nổ không sát thương, rung camera. */
    private static void clash(Run r, float shake, boolean crack) {
        ServerLevel sl = r.level;
        boolean mo = r.mo != null && r.mo.isAlive();
        Vec3 mid = mo ? r.mo.position().add(r.u.scale(0.8)).add(0, 1.8, 0) : r.L(0, 2, 3.2);
        Fx.elem(sl, null, Fx.E_ARC, mid, r.u, 1.2f, MO_C, 10);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, mid.x, mid.y, mid.z, 3, 0.4, 0.3, 0.4, 0);
        sl.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 40, 0.8, 0.6, 0.8, 0.3);
        sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 20, 0.6, 0.6, 0.6, 0.15);
        sl.sendParticles(ParticleTypes.EXPLOSION, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
        if (crack && mo) Fx.groundCrack(sl, null, r.mo.position().add(0, 0.05, 0), 2.5f, MO_C, 7, 40);
        sound(sl, mid, SoundEvents.ANVIL_LAND, 1.0f, 0.8f);
        sound(sl, mid, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f);
        Fx.shake(sl, mid, shake, 12, 20f);
    }

    private static void dissolveShade(Run r) {
        if (!r.shade.isAlive()) return;
        Vec3 p = r.shade.position();
        r.level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 2.5, p.z, 60, 0.8, 1.6, 0.8, 0.12);
        r.level.sendParticles(ParticleTypes.SOUL, p.x, p.y + 2.0, p.z, 15, 0.5, 0.8, 0.5, 0.02);
        Fx.elem(r.level, null, Fx.E_NUKE, p.add(0, 2.5, 0), Vec3.ZERO, 2f, VOIDC, 20);
        r.shade.setInvisible(true);
    }

    private static void dissolveMo(Run r) {
        if (r.mo == null || !r.mo.isAlive()) return;
        r.level.sendParticles(ParticleTypes.END_ROD, r.mo.getX(), r.mo.getY() + 1.2, r.mo.getZ(), 40, 0.5, 0.9, 0.5, 0.1);
        Fx.groundRing(r.level, null, r.mo.position().add(0, 0.05, 0), 2f, MO_C, 30);
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
