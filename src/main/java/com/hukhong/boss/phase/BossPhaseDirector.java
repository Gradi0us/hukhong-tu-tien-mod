package com.hukhong.boss.phase;

import com.hukhong.HuKhong;
import com.hukhong.audio.BossMusic;
import com.hukhong.boss.phase.BossPhaseConfig.Config;
import com.hukhong.boss.phase.BossPhaseConfig.Move;
import com.hukhong.boss.phase.BossPhaseConfig.Phase;
import com.hukhong.events.BossBuff;
import com.hukhong.vfx.Fx;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Máy trạng thái pha boss: Đánh -> Chuyển pha (boss bất tử + khoá AI 3-6s, cutscene cho người trong 60 block) -> Đánh.
 * <ul>
 *   <li>Chuyển pha: tại tick "lộ diện" đổi pha (client đổi texture/xương), cỡ cơ thể tăng dần qua thuộc tính SCALE (hitbox theo),
 *       bật biến đổi đấu trường, đổi lớp nhạc.</li>
 *   <li>Trong trận: pha 2/3 chen chiêu phase ({@link PhaseMoves}) khi boss đang rảnh.</li>
 * </ul>
 * Mọi thứ chạy phía server; client chỉ nhận {@link CutscenePayload}/{@link BossSkyPayload} và đọc pha đồng bộ.
 */
public final class BossPhaseDirector {
    public static final double CUTSCENE_RANGE = 60;
    private static final ResourceLocation SCALE_MOD = ResourceLocation.fromNamespaceAndPath(HuKhong.MODID, "phase_scale");

    private final Mob boss;
    private final PhasedBoss pb;
    private int trans, transTotal, targetPhase;
    private boolean revealed;
    private float fromScale = 1f;
    private int moveCd = 100, lastMove = -1;
    private PhaseMoves.Run run;
    private boolean loaded;

    public BossPhaseDirector(PhasedBoss pb) {
        this.pb = pb;
        this.boss = (Mob) pb;
    }

    // ------------------------------------------------------------------ trạng thái
    public boolean transitioning() { return trans > 0; }
    /** Bất tử trong lúc chuyển pha (trừ sát thương BYPASSES_INVULNERABILITY: void, /kill). */
    public boolean invulnerable() { return trans > 0; }
    /** AI bị khoá (chuyển pha hoặc đang gồng chiêu phase) — dùng cho isImmobile(). */
    public boolean frozen() { return trans > 0 || run != null; }
    public int targetPhase() { return targetPhase; }
    public PhaseMoves.Run currentRun() { return run; }
    public int transitionTicksLeft() { return trans; }

    private Config cfg() { return pb.phaseConfig(); }

    // ------------------------------------------------------------------ tick (server)
    public void tick(ServerLevel sl) {
        if (!boss.isAlive()) return;
        if (!loaded) { loaded = true; if (pb.currentPhase() > 1) onLoad(sl); }
        if (trans > 0) { tickTransition(sl); return; }
        float frac = boss.getHealth() / boss.getMaxHealth();
        int cur = pb.currentPhase(), want = cfg().phaseFor(frac);
        if (want > cur) { begin(sl, cur + 1); return; }
        tickMoves(sl);
    }

    /** Bắt đầu chuyển sang pha {@code ph} ngay (GameTest, lệnh debug, hoặc HP chạm ngưỡng). */
    public boolean begin(ServerLevel sl, int ph) {
        if (trans > 0 || ph < 2 || ph > cfg().count()) return false;
        Phase def = cfg().phase(ph);
        trans = transTotal = def.transition();
        targetPhase = ph;
        revealed = false;
        fromScale = currentScale();
        run = null;
        pb.cancelSkill();
        boss.getNavigation().stop();
        boss.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.6f);
        CutscenePayload pl = new CutscenePayload(boss.getId(), boss.getX(), boss.getY(), boss.getZ(), boss.yBodyRot,
                transTotal, def.cam(), def.subs());
        for (ServerPlayer p : sl.players()) {
            double d2 = p.distanceToSqr(boss);
            if (d2 <= CUTSCENE_RANGE * CUTSCENE_RANGE) {
                try { PacketDistributor.sendToPlayer(p, pl); } catch (RuntimeException ignored) { }
            } else if (d2 < 100 * 100) {
                p.displayClientMessage(Component.translatable("hukhong.boss.phase", ph), true);
            }
        }
        return true;
    }

    private void tickTransition(ServerLevel sl) {
        int el = transTotal - trans;
        Phase def = cfg().phase(targetPhase);
        boss.getNavigation().stop();
        double y = boss.getY() + boss.getBbHeight() * 0.5;
        if (el % 3 == 0)
            sl.sendParticles(PhaseMoves.particle(cfg().aura()), boss.getX(), y, boss.getZ(), 6 + el / 8, boss.getBbWidth(), boss.getBbHeight() * 0.5, boss.getBbWidth(), 0.04);
        if (!revealed && el >= def.reveal()) reveal(sl, def);
        if (revealed) {
            float k = Mth.clamp((el - def.reveal()) / (float) Math.max(1, transTotal - def.reveal()), 0f, 1f);
            setScale(Mth.lerp(k * k * (3 - 2 * k), fromScale, def.scale()));
        }
        if (--trans <= 0) {
            setScale(def.scale());
            trans = 0;
            moveCd = 60;
            boss.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3f, 0.7f);
        }
    }

    /** Khoảnh khắc lộ diện: đổi pha + nổ hiệu ứng + bật đấu trường + đổi nhạc. */
    private void reveal(ServerLevel sl, Phase def) {
        revealed = true;
        pb.setCurrentPhase(targetPhase);
        pb.applyPhase(sl, targetPhase);
        Vec3 c = boss.position();
        int rgb = cfg().aura().rgb;
        Fx.elem(sl, null, Fx.E_HALOS, c.add(0, 1.0, 0), Vec3.ZERO, (float) (cfg().size() * 0.9), rgb, 0);
        Fx.groundRing(sl, null, c, (float) cfg().size() * 2.2f, rgb, 36);
        Fx.shake(sl, c, 3.5f, 24, 40f);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + boss.getBbHeight() * 0.5, c.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ModParticles.HU_KHONG.get(), c.x, c.y + boss.getBbHeight() * 0.5, c.z, 80, 2.5, 1.5, 2.5, 0.12);
        boss.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4f, 0.5f);
        PhaseArena.enable(boss, cfg().aura(), cfg().arenaFor(targetPhase), true);
        if (def.music() != null) BossMusic.retrack(boss, def.music());
    }

    // ------------------------------------------------------------------ chiêu phase
    private void tickMoves(ServerLevel sl) {
        if (run != null) {
            PhaseMoves.tick(sl, boss, run);
            if (--run.left <= 0) {
                PhaseMoves.fire(sl, boss, pb, run);
                run = null;
                moveCd = BossBuff.cd(pb.currentPhase() >= 3 ? 150 : 190);
            }
            return;
        }
        if (moveCd > 0) { moveCd--; return; }
        if (!pb.idleForMove()) return;
        List<Move> list = pb.movesForPhase(pb.currentPhase());
        if (list.isEmpty()) return;
        LivingEntity t = boss.getTarget();
        if (t == null || !t.isAlive() || boss.distanceToSqr(t) > 26 * 26) return;
        List<Move> normal = new ArrayList<>(), ult = new ArrayList<>();
        for (Move m : list) (m.ultimate() ? ult : normal).add(m);
        Move pick;
        boolean wantUlt = !ult.isEmpty() && boss.getRandom().nextFloat() < 0.4f;
        List<Move> pool = wantUlt || normal.isEmpty() ? ult : normal;
        int i = boss.getRandom().nextInt(pool.size());
        if (pool.size() > 1 && list.indexOf(pool.get(i)) == lastMove) i = (i + 1) % pool.size();
        pick = pool.get(i);
        lastMove = list.indexOf(pick);
        start(sl, pick, t);
    }

    /** Thi triển một chiêu phase ngay (cũng dùng cho GameTest). */
    public void start(ServerLevel sl, Move m, LivingEntity target) {
        if (trans > 0) return;
        run = PhaseMoves.begin(sl, boss, m, target);
        pb.playMoveAnim(m.ultimate());
    }

    public Move moveById(String id) {
        for (Move m : cfg().movesUpTo(cfg().count())) if (m.id().equals(id)) return m;
        return null;
    }

    // ------------------------------------------------------------------ cỡ cơ thể
    public float currentScale() {
        AttributeInstance a = boss.getAttribute(Attributes.SCALE);
        return a == null ? 1f : (float) a.getValue();
    }

    private void setScale(float s) {
        AttributeInstance a = boss.getAttribute(Attributes.SCALE);
        if (a == null || Math.abs(a.getValue() - s) < 1e-3) return;
        a.removeModifier(SCALE_MOD);
        if (Math.abs(s - 1f) > 1e-3) a.addPermanentModifier(new AttributeModifier(SCALE_MOD, s - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        boss.refreshDimensions();
    }

    /** Sau khi nạp từ save: áp lại phần phía server của pha hiện tại, bật lại hiệu ứng đấu trường không đổi khối. */
    public void onLoad(ServerLevel sl) {
        int ph = pb.currentPhase();
        pb.applyPhase(sl, ph);
        if (ph > 1) {
            setScale(cfg().phase(ph).scale());
            PhaseArena.enable(boss, cfg().aura(), cfg().arenaFor(ph), false);
        }
    }

    /** Boss chết/biến mất giữa chừng: dọn trạng thái. */
    public void reset() { trans = 0; run = null; }
}
