package com.hukhong.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Bang Nhan: con mat bang cua vu tru dong ket. Bo chieu RIENG kieu Astel + Rennala o BangNhanMoves (bn_*):
 * AI rieng qua begin() + director giao chieu phase qua startOwnMove. fire()/telegraph() cu chi con la duong du phong.
 */
public class BangNhan extends WorldBoss {
    private static final int VOLLEY = 0, NOVA = 1, RAIN = 2, BLIZZARD = 3;
    final List<Vec3> spots = new ArrayList<>();

    public BangNhan(EntityType<? extends Monster> t, Level l) { super(t, l, BossEvent.BossBarColor.WHITE); }

    // ================================================================ bo chieu rieng kieu Astel + Rennala (xem BangNhanMoves)
    String mv;
    int mt;
    LivingEntity mTarget;
    Vec3 aim = new Vec3(0, 0, 1), spot, from;
    final List<Vec3> points = new ArrayList<>();
    final java.util.Set<java.util.UUID> hitOnce = new java.util.HashSet<>();

    public String currentMove() { return mv; }
    public int moveTick() { return mt; }

    /** Bat dau chieu rieng; false neu dang ban hoac id la. */
    public boolean startMove(ServerLevel sl, String id, LivingEntity target) {
        if (mv != null || !isAlive() || !BangNhanMoves.handles(id)) return false;
        mv = id; mt = 0; mTarget = target; spot = null; from = null;
        hitOnce.clear(); points.clear(); spots.clear();
        getNavigation().stop();
        BangNhanMoves.begin(sl, this);
        return true;
    }

    void endMove() {
        if (mv == null) return;
        mv = null; mTarget = null;
        setNoGravity(false);
    }

    boolean strike(LivingEntity p, float dmg) { return p.hurt(damageSources().mobAttack(this), dmg); }

    /** Ky nang cu cua WorldBoss -> chieu rieng (AI rieng chon theo pha). */
    @Override public void begin(ServerLevel sl, int skill, LivingEntity t) {
        if (mv != null) return;
        String id = phase() <= 1 || t == null ? BangNhanMoves.bySkill(skill) : BangNhanMoves.pick(random, phase(), distanceTo(t));
        startMove(sl, id, t);
        skillCd = 9999;
    }

    @Override public boolean startOwnMove(ServerLevel sl, com.hukhong.boss.phase.BossPhaseConfig.Move m, LivingEntity target) {
        return BangNhanMoves.handles(m.id()) && startMove(sl, m.id(), target);
    }
    @Override public java.util.List<com.hukhong.boss.phase.CamPath.Key> introCamera(int T) { return BangNhanMoves.introCam(T); }
    @Override public java.util.List<com.hukhong.boss.phase.BossPhaseConfig.SubLine> introLines(int T) { return BangNhanMoves.introSubs(T); }
    @Override public boolean idleForMove() { return super.idleForMove() && mv == null; }
    @Override public void cancelSkill() { super.cancelSkill(); endMove(); }
    @Override public boolean isImmobile() { return super.isImmobile() || mv != null; }
    @Override public void applyPhase(ServerLevel sl, int ph) {
        super.applyPhase(sl, ph);
        if (director() != null && director().transitioning()) BangNhanMoves.transformFx(sl, this, ph);
    }

    /** isImmobile() khi dang thi trien => customServerAiStep bi bo qua, chieu chay o tick(). */
    @Override public void tick() {
        super.tick();
        if (level() instanceof ServerLevel s0 && isAlive()) { if (!mines.isEmpty()) tickMines(s0); if (mv == null && pending < 0 && !director().frozen()) brainTick(s0); }
        if (!(level() instanceof ServerLevel sl) || !isAlive() || mv == null) return;
        if (director().transitioning()) { endMove(); skillCd = 40; return; }
        if (!BangNhanMoves.step(sl, this, mt++)) {
            endMove();
            skillCd = (int) Math.round(com.hukhong.events.BossBuff.cd(cooldown(phase())) * cdMul());
        }
    }

    @Override public void die(net.minecraft.world.damagesource.DamageSource src) {
        super.die(src);
        endMove();
        if (level() instanceof ServerLevel sl) BangNhanMoves.deathScene(sl, this);
    }

    @Override protected String id() { return "bang_nhan"; }
    @Override protected String track() { return "su_do"; }
    @Override protected int skillCount(int phase) { return 4; }
    @Override protected int windupOf(int s) { return switch (s) { case VOLLEY -> 22; case NOVA -> 28; case RAIN -> 40; default -> 36; }; }
    @Override protected String animOf(int s) { return switch (s) { case VOLLEY -> "skill_a"; case NOVA, BLIZZARD -> "skill_b"; default -> "skill_c"; }; }
    @Override protected int cooldown(int phase) { return phase >= 3 ? 50 : phase >= 2 ? 60 : 80; }
    @Override protected double radiusOf(int s) { return switch (s) { case NOVA -> 9; case BLIZZARD -> 15; default -> 3; }; }

    @Override protected void onBegin(ServerLevel sl, int skill, LivingEntity t) {
        super.onBegin(sl, skill, t);
        spots.clear();
        if (skill != RAIN) return;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(26), q -> !q.isSpectator())) {
            spots.add(p.position());
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = 2 + random.nextDouble() * 4;
                spots.add(p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        }
        if (spots.isEmpty() && t != null) spots.add(t.position());
    }

    @Override protected void telegraph(ServerLevel sl, int skill) {
        if (skill != RAIN) { super.telegraph(sl, skill); return; }
        for (Vec3 s : spots) ringFx(sl, s, 2.5, ParticleTypes.SNOWFLAKE);
    }

    @Override protected void fire(ServerLevel sl, int skill, LivingEntity t) {
        switch (skill) {
            case VOLLEY -> {
                spitFan(sl, t, 5, 12, 1.0, 2.5, true);
                playSound(SoundEvents.SNOW_GOLEM_SHOOT, 2f, 0.6f);
            }
            case NOVA -> {
                hitPlayers(sl, position(), 9, 1.3, p -> {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                    p.setTicksFrozen(p.getTicksRequiredToFreeze() + 100);
                });
                ringFx(sl, position(), 9, ParticleTypes.SNOWFLAKE);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 80, 4, 1.5, 4, 0.1);
                playSound(SoundEvents.GLASS_BREAK, 2f, 0.5f);
            }
            case RAIN -> {
                for (Vec3 s : new ArrayList<>(spots)) {
                    hitPlayers(sl, s, 2.5, 1.5, p -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)));
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, s.x, s.y + 1, s.z, 20, 0.8, 1.2, 0.8, 0.05);
                    sl.sendParticles(ParticleTypes.CLOUD, s.x, s.y + 0.3, s.z, 8, 0.6, 0.2, 0.6, 0.02);
                }
                spots.clear();
            }
            default -> {
                hitPlayers(sl, position(), 15, 0.9, p -> {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    p.setTicksFrozen(p.getTicksRequiredToFreeze() + 60);
                });
                ringFx(sl, position(), 15, ParticleTypes.SNOWFLAKE);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2, getZ(), 120, 7, 2, 7, 0.15);
            }
        }
    }

    // ================================================================ BossBrain (PLAN-v1 §4: không đi bộ — trôi + dịch chuyển)
    /** Trôi chậm giữ 8-14 ô, dịch chuyển mỗi 3-4 giây; bị áp sát -> dịch chuyển ngay; áp sát = dịch chuyển ra sau lưng rồi
     *  gậy băng 2 nhát + nổ băng tại chỗ; người chơi đuổi theo -> để lại mìn băng ở chỗ cũ. Không né (dịch chuyển thay né). */
    public static final com.hukhong.boss.ai.BossProfile BRAIN = com.hukhong.boss.ai.BossProfile.builder()
            .phase(1, new com.hukhong.boss.ai.BossProfile.PhaseVals(8, 14, 0.06, 12f, 0, 999, 50, 80))
            .phase(2, new com.hukhong.boss.ai.BossProfile.PhaseVals(8, 14, 0.07, 14f, 0, 999, 40, 65))
            .phase(3, new com.hukhong.boss.ai.BossProfile.PhaseVals(8, 14, 0.08, 16f, 0, 999, 30, 50))
            .weight(0.8).accel(8, 6).moveSpeed(0.1)
            .gapClose(new BlinkBehind(), 7)
            .combo(new com.hukhong.boss.ai.BossProfile.Combo("gay", java.util.List.of(
                    com.hukhong.boss.ai.BossProfile.hit(10, 3, 6, 3, 120, 0.8, 0.5),
                    com.hukhong.boss.ai.BossProfile.hit(8, 3, 6, 3, 120, 0.8, 0.5),
                    new com.hukhong.boss.ai.BossProfile.Hit(16, 4, 18, 3.5, 360, 1.2, 0, 0, false, true, false)),
                    0, 4.5, 3, 1, java.util.List.of(), false))
            .bigMoveEvery(99)
            .build();

    @Override public com.hukhong.boss.ai.BossProfile brainProfile() { return BRAIN; }
    @Override protected java.util.Set<String> brainAnims() { return java.util.Set.of("strafe_l", "strafe_r", "backstep", "dash", "combo_gay_1", "combo_gay_2", "combo_gay_3"); }

    record Mine(Vec3 pos, int[] age) {}
    final List<Mine> mines = new ArrayList<>();
    int blinkCd = 60;
    double lastBlinkDist = 11;
    public int blinks;
    public int mineCount() { return mines.size(); }

    private void brainTick(ServerLevel sl) {
        com.hukhong.boss.ai.BossBrain b = com.hukhong.boss.ai.BossBrain.get(this);
        LivingEntity t = getTarget();
        if (blinkCd > 0) blinkCd--;
        if (b == null || t == null || !t.isAlive()) return;
        var st = b.state();
        if (st != com.hukhong.boss.ai.BossBrain.State.STRAFE && st != com.hukhong.boss.ai.BossBrain.State.OBSERVE) return;
        double d = distanceTo(t);
        boolean crowded = d < 4.5 && blinkCd < 50;
        if (!crowded && blinkCd > 0) return;
        boolean chased = crowded || lastBlinkDist - d > 2.5;
        Vec3 old = position();
        if (!blinkAround(sl, t, 9, 13)) { blinkCd = 10; return; }
        if (chased && mines.size() < 5) { mines.add(new Mine(old, new int[1])); sl.sendParticles(ParticleTypes.SNOWFLAKE, old.x, old.y + 0.3, old.z, 20, 0.5, 0.1, 0.5, 0.02); }
        blinkCd = 60 + random.nextInt(21);
        lastBlinkDist = distanceTo(t);
    }

    /** Dịch chuyển tới điểm cách mục tiêu r0..r1 ô, trong sân, không kẹt khối. */
    boolean blinkAround(ServerLevel sl, LivingEntity t, double r0, double r1) {
        double[] a = com.hukhong.arena.BossArena.bounds(this);
        for (int k = 0; k < 14; k++) {
            double ang = random.nextDouble() * Math.PI * 2, r = r0 + random.nextDouble() * (r1 - r0);
            Vec3 at = t.position().add(Math.cos(ang) * r, 0, Math.sin(ang) * r);
            if (a != null && Math.hypot(at.x - a[0], at.z - a[1]) > a[2] - 3) continue;
            if (!sl.noCollision(this, getBoundingBox().move(at.subtract(position())))) continue;
            blinkTo(sl, at);
            return true;
        }
        return false;
    }

    void blinkTo(ServerLevel sl, Vec3 at) {
        sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1, getZ(), 30, 0.5, 1, 0.5, 0.05);
        teleportTo(at.x, at.y, at.z);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1, at.z, 30, 0.5, 1, 0.5, 0.05);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.5f);
        playBrainAnim("dash");
        blinks++;
    }

    private void tickMines(ServerLevel sl) {
        mines.removeIf(m -> {
            int age = ++m.age()[0];
            if (age > 300) return true;
            if (age % 10 == 0) sl.sendParticles(ParticleTypes.SNOWFLAKE, m.pos().x, m.pos().y + 0.2, m.pos().z, 3, 0.4, 0.05, 0.4, 0);
            if (age < 10 || sl.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(m.pos(), m.pos()).inflate(1.6, 1.5, 1.6), p -> !p.isSpectator()).isEmpty()) return false;
            hitPlayers(sl, m.pos(), 2.5, 0.8, p -> { p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)); p.setTicksFrozen(p.getTicksRequiredToFreeze() + 60); });
            ringFx(sl, m.pos(), 2.5, ParticleTypes.SNOWFLAKE);
            sl.playSound(null, m.pos().x, m.pos().y, m.pos().z, SoundEvents.GLASS_BREAK, getSoundSource(), 1.5f, 0.7f);
            return true;
        });
    }

    /** Dịch chuyển ra sau lưng mục tiêu (~2.5 ô) rồi brain vung gậy. */
    static final class BlinkBehind implements com.hukhong.boss.ai.GapClose {
        public String anim() { return "dash"; }
        public boolean step(ServerLevel sl, net.minecraft.world.entity.Mob b, com.hukhong.boss.ai.BossMovement mv, LivingEntity t, int i) {
            mv.freeze();
            if (i < 6) { sl.sendParticles(ParticleTypes.SNOWFLAKE, b.getX(), b.getY() + 1, b.getZ(), 6, 0.3, 0.6, 0.3, 0.05); return true; }
            Vec3 look = t.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1e-3) look = b.position().subtract(t.position()).multiply(1, 0, 1).scale(-1);
            Vec3 at = t.position().subtract(look.normalize().scale(2.5));
            BangNhan bn = (BangNhan) b;
            if (sl.noCollision(b, b.getBoundingBox().move(at.subtract(b.position())))) bn.blinkTo(sl, at);
            mv.face(t.position(), 360f);
            bn.blinkCd = Math.max(bn.blinkCd, 60);
            return false;
        }
    }
}
