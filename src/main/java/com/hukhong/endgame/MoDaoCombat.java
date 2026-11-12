package com.hukhong.endgame;

import com.hukhong.entity.HuMob;
import com.hukhong.story.npc.StoryNpc;
import com.hukhong.vfx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Mộ Dao chiến đấu (PLAN-v1-nttd-story-rework §2.1, §5, §6): model có kiếm + vỏ, anim riêng, máy trạng thái chiêu tự chứa
 * (không dùng BossBrain). Kế thừa {@link StoryNpc} để API {@code NttdAllies.findMo/spawnMo} (trả StoryNpc) giữ nguyên;
 * npcId để trống nên không lẫn với NPC Mộ Dao trong thành, không mở thoại.
 * <p>Hai chế độ: <b>scripted</b> (cutscene điều khiển vị trí + anim) và <b>đồng minh</b> (8 chiêu §5.2, chỉ gây sát thương lên
 * Nguyên Thủy Tịch Diệt / Hư mob, không bao giờ lên Player; hồi máu/khiên người chơi). Bất tử.
 */
public class MoDaoCombat extends StoryNpc {
    public static final String ANIM = "animation.npc_mo_dao_combat.";
    /** Anim kích hoạt qua controller "skill". */
    public static final String[] SKILLS = {"dash", "draw_sword", "sheathe_sword", "parry_high", "parry_low", "counter_slash",
            "dash_slash", "combo_1", "combo_2", "combo_3", "spin_leap", "backstep", "qi_cast", "sword_rain_cast",
            "sidestep_l", "sidestep_r", "backflip", "heal_cast", "talk"};
    public static final byte ST_RELAX = 0, ST_GUARD = 1, ST_STRAFE_L = 2, ST_STRAFE_R = 3;

    private static final EntityDataAccessor<Boolean> DRAWN = SynchedEntityData.defineId(MoDaoCombat.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> STANCE = SynchedEntityData.defineId(MoDaoCombat.class, EntityDataSerializers.BYTE);

    /** Sát thương lên NTTD: tỉ lệ máu tối đa mỗi đơn vị hệ số, trần mỗi đòn, trần tổng cả pha 4 (§5.1). */
    static final double ALLY_FRAC = 0.002, HIT_CAP = 0.008, TOTAL_CAP = 0.10;
    static final int COLOR = NttdAllies.MO_COLOR;

    // chiêu
    static final int NONE = 0, M1_DASH = 1, M2_COMBO = 2, M3_LINE = 3, M4_LEAP = 4, M5_QI = 5, M6_RAIN = 6, M7_EVADE = 7, M8_HEAL = 8,
            DRAW = 9, SHEATHE = 10;

    private boolean scripted;
    private int swapIn;
    private boolean swapTo;
    // trạng thái đồng minh (không lưu: khởi động lại thì bắt đầu từ đầu)
    private LivingEntity target;
    private int mv, mt, rest = 20, idle, strafeDir = 1, evadeKind;
    private boolean evadeReq;
    private final int[] cd = new int[11];
    private Vec3 a0 = Vec3.ZERO, a1 = Vec3.ZERO;
    private ServerPlayer healTo;
    private double dealt;

    public MoDaoCombat(EntityType<? extends MoDaoCombat> t, Level l) {
        super(t, l);
        setNoAi(true);
        setNoGravity(true);
    }

    /** Tạo + thêm vào thế giới tại at, quay mặt theo yaw. */
    public static MoDaoCombat create(ServerLevel sl, Vec3 at, float yaw, boolean scripted) {
        MoDaoCombat m = EndgameRegistry.MO_DAO_COMBAT.get().create(sl);
        if (m == null) return null;
        m.scripted = scripted;
        m.moveTo(at.x, at.y, at.z, yaw, 0f);
        m.setYBodyRot(yaw);
        m.setYHeadRot(yaw);
        m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        m.setCustomName(Component.translatable("hukhong.npc.mo_dao"));
        m.setCustomNameVisible(!scripted);
        m.setPersistenceRequired();
        return sl.addFreshEntity(m) ? m : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DRAWN, false);
        b.define(STANCE, ST_RELAX);
    }

    // ================================================================ API cho cutscene / NttdAllies
    public boolean drawn() { return entityData.get(DRAWN); }
    public byte stance() { return entityData.get(STANCE); }
    public void setStance(byte s) { if (stance() != s) entityData.set(STANCE, s); }
    public boolean scripted() { return scripted; }
    public void setScripted(boolean s) { scripted = s; if (s) { mv = NONE; setStance(drawn() ? ST_GUARD : ST_RELAX); } }
    public void anim(String name) { triggerAnim("skill", name); }
    /** Đặt trạng thái kiếm ngay (không anim). */
    public void setDrawnNow(boolean d) { entityData.set(DRAWN, d); swapIn = 0; }
    /** Rút kiếm: anim 10 tick, tay chạm chuôi ở tick 4 thì kiếm chuyển từ vỏ sang tay. */
    public void drawSword() { anim("draw_sword"); swapTo = true; swapIn = 4; setStance(ST_GUARD); }
    /** Tra kiếm: anim 14 tick, kiếm vào vỏ ở tick 8. */
    public void sheatheSword() { anim("sheathe_sword"); swapTo = false; swapIn = 8; setStance(ST_RELAX); }
    /** Gợi ý mục tiêu (NTTD) từ bên ngoài. */
    public void setTargetHint(LivingEntity t) { if (t != null && t.isAlive()) target = t; }

    /** Đặt vị trí + hướng mặt về điểm face (null = giữ hướng). */
    public void glide(Vec3 p, Vec3 face) {
        float yaw = face == null ? getYRot() : yawTo(p, face);
        moveTo(p.x, p.y, p.z, yaw, 0f);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        setDeltaMovement(Vec3.ZERO);
    }

    public void face(Vec3 at) { glide(position(), at); }

    static float yawTo(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        if (d.x * d.x + d.z * d.z < 1e-6) return 0f;
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    /** Mặt đất đứng được gần (x, yRef, z): quét từ yRef+3 xuống yRef-8; không thấy thì trả yRef. */
    public double groundAt(double x, double yRef, double z) {
        Level l = level();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int bx = Mth.floor(x), bz = Mth.floor(z), base = Mth.floor(yRef);
        for (int dy = 3; dy >= -8; dy--) {
            m.set(bx, base + dy - 1, bz);
            VoxelShape s = l.getBlockState(m).getCollisionShape(l, m);
            if (s.isEmpty()) continue;
            BlockPos up = m.above();
            if (l.getBlockState(up).getCollisionShape(l, up).isEmpty() && l.getBlockState(up.above()).getCollisionShape(l, up.above()).isEmpty())
                return m.getY() + s.max(Direction.Axis.Y);
        }
        return yRef;
    }

    private Vec3 onGround(Vec3 p) { return new Vec3(p.x, groundAt(p.x, p.y, p.z), p.z); }

    // ================================================================ vòng đời
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        setNoGravity(true);           // StoryNpc.tick có thể tắt; vị trí luôn do ta đặt
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0;
        if (swapIn > 0 && --swapIn == 0) entityData.set(DRAWN, swapTo);
        if (!scripted && level() instanceof ServerLevel sl) allyTick(sl);
    }

    @Override
    public boolean hurt(DamageSource s, float amount) {
        if (s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(s, amount);
        if (!level().isClientSide && !scripted && s.getEntity() instanceof NguyenThuyTichDiet) evadeReq = true;
        return false;
    }

    @Override protected InteractionResult mobInteract(Player p, InteractionHand h) { return InteractionResult.PASS; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("MoScripted", scripted);
        t.putDouble("MoDealt", dealt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        scripted = t.getBoolean("MoScripted");
        dealt = t.getDouble("MoDealt");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "move", 4, st -> {
            MoDaoCombat m = st.getAnimatable();
            byte s = m.stance();
            String a = s == ST_STRAFE_L ? "strafe_l" : s == ST_STRAFE_R ? "strafe_r"
                    : st.isMoving() ? (m.drawn() ? "walk_drawn" : "walk") : (m.drawn() ? "guard_idle" : "idle");
            return st.setAndContinue(RawAnimation.begin().thenLoop(ANIM + a));
        }));
        AnimationController<MoDaoCombat> sk = new AnimationController<>(this, "skill", 2, st -> PlayState.STOP);
        for (String a : SKILLS) sk.triggerableAnim(a, RawAnimation.begin().then(ANIM + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    // ================================================================ đồng minh: máy trạng thái chiêu
    private void allyTick(ServerLevel sl) {
        for (int i = 0; i < cd.length; i++) if (cd[i] > 0) cd[i]--;
        if (target == null || !target.isAlive() || target.distanceToSqr(this) > 64 * 64 || tickCount % 20 == 0) target = findTarget(sl);
        if (mv != NONE) { runMove(sl); return; }
        if (target == null) { relax(sl); return; }
        idle = 0;
        if (!drawn() && swapIn == 0) { begin(DRAW); return; }
        double edge = edge(target);
        if (edge < 2.2 && random.nextInt(40) == 0) evadeReq = true;
        if (--rest > 0) {
            if (evadeReq && cd[M7_EVADE] == 0) { begin(M7_EVADE); return; }
            strafe(sl, edge);
            return;
        }
        begin(pick(sl, edge));
    }

    private LivingEntity findTarget(ServerLevel sl) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (NguyenThuyTichDiet n : sl.getEntitiesOfClass(NguyenThuyTichDiet.class, getBoundingBox().inflate(64), e -> e.isAlive() && !e.lv10Shade())) {
            double d = n.distanceToSqr(this);
            if (d < bd) { bd = d; best = n; }
        }
        return best;
    }

    /** Khoảng cách từ mép mục tiêu (trừ nửa bề rộng hộp). */
    private double edge(LivingEntity t) {
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz) - t.getBbWidth() * 0.5;
    }

    private Vec3 flatDir(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).multiply(1, 0, 1);
        return d.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : d.normalize();
    }

    private ServerPlayer weakestPlayer(ServerLevel sl) {
        ServerPlayer best = null;
        float bf = 0.5f;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(40), q -> q.isAlive() && !q.isSpectator())) {
            float f = p.getHealth() / p.getMaxHealth();
            if (f < bf) { bf = f; best = p; }
        }
        return best;
    }

    /** Không có NTTD: đứng yên nhìn người chơi gần nhất, lâu thì tra kiếm. */
    private void relax(ServerLevel sl) {
        setStance(drawn() ? ST_GUARD : ST_RELAX);
        Player p = sl.getNearestPlayer(this, 24);
        if (p != null && tickCount % 5 == 0) face(p.position());
        if (drawn() && swapIn == 0 && ++idle > 100) begin(SHEATHE);
    }

    /** Lúc nghỉ: đi vòng quanh mục tiêu ở bán kính 6-9 tính từ mép, luôn nhìn vào nó. */
    private void strafe(ServerLevel sl, double edge) {
        if (edge > 13) { rest = 0; return; }
        Vec3 c = target.position();
        Vec3 radial = flatDir(c, position());
        Vec3 tan = new Vec3(-radial.z, 0, radial.x).scale(strafeDir);
        double want = 7.5 - edge;
        Vec3 step = tan.scale(0.17).add(radial.scale(Mth.clamp(want * 0.08, -0.12, 0.12)));
        glide(onGround(position().add(step)), c);
        setStance(strafeDir > 0 ? ST_STRAFE_L : ST_STRAFE_R);
    }

    private int pick(ServerLevel sl, double edge) {
        if (evadeReq && cd[M7_EVADE] == 0) return M7_EVADE;
        if (cd[M8_HEAL] == 0 && (healTo = weakestPlayer(sl)) != null) return M8_HEAL;
        if (edge > 7) {
            if (cd[M3_LINE] == 0 && edge < 12 && random.nextInt(3) == 0) return M3_LINE;
            if (cd[M5_QI] == 0 && edge < 16 && random.nextInt(3) == 0) return M5_QI;
            return M1_DASH;
        }
        if (cd[M6_RAIN] == 0 && random.nextInt(4) == 0) return M6_RAIN;
        int r = random.nextInt(100);
        if (r < 20 && cd[M4_LEAP] == 0) return M4_LEAP;
        if (r < 35 && cd[M3_LINE] == 0) return M3_LINE;
        if (r < 48 && cd[M5_QI] == 0) return M5_QI;
        return edge > 3 ? M1_DASH : M2_COMBO;
    }

    private void begin(int m) {
        mv = m;
        mt = 0;
        evadeReq = false;
        setStance(drawn() || m == DRAW ? ST_GUARD : ST_RELAX);
    }

    private void end() {
        mv = NONE;
        rest = 20 + random.nextInt(16);
        if (random.nextInt(3) == 0) strafeDir = -strafeDir;
    }

    private void runMove(ServerLevel sl) {
        int t = mt++;
        LivingEntity tg = target;
        if (tg == null && mv != DRAW && mv != SHEATHE && mv != M8_HEAL && mv != M7_EVADE) { end(); return; }
        switch (mv) {
            case DRAW -> { if (t == 0) { drawSword(); sound(SoundEvents.TRIDENT_RETURN, 1.2f, 1.4f); } if (t >= 12) { mv = NONE; rest = 6; } }
            case SHEATHE -> { if (t == 0) sheatheSword(); if (t == 8) sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5f, 1.2f); if (t >= 16) mv = NONE; }
            case M1_DASH -> moveDash(sl, tg, t);
            case M2_COMBO -> moveCombo(sl, tg, t);
            case M3_LINE -> moveLine(sl, tg, t);
            case M4_LEAP -> moveLeap(sl, tg, t);
            case M5_QI -> moveQi(sl, tg, t);
            case M6_RAIN -> moveRain(sl, tg, t);
            case M7_EVADE -> moveEvade(sl, tg, t);
            case M8_HEAL -> moveHeal(sl, tg, t);
            default -> end();
        }
    }

    private static double easeOut(double k) { return 1 - (1 - k) * (1 - k); }

    private Vec3 lerpGround(double k) { return onGround(a0.add(a1.subtract(a0).scale(k))); }

    /** M1 Lưu Vân Bộ: lướt thẳng, dừng cách mép mục tiêu 2 block. */
    private void moveDash(ServerLevel sl, LivingEntity tg, int t) {
        if (t == 0) {
            Vec3 d = flatDir(position(), tg.position());
            double len = Math.min(12, Math.max(0, edge(tg) - 2));
            a0 = position();
            a1 = a0.add(d.scale(len));
            Fx.ribbon(sl, null, a0.add(0, 0.6, 0), a1.add(0, 0.6, 0), 0, 3, 0, 0.25f, COLOR);
        }
        if (t == 3) { anim("dash"); sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.6f); }
        if (t >= 3 && t <= 8) {
            glide(lerpGround(easeOut((t - 3) / 5.0)), tg.position());
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 2, 0.2, 0.05, 0.2, 0.01);
        }
        if (t >= 12) {
            end();
            if (edge(tg) < 4) { begin(M2_COMBO); }
        }
    }

    /** M2 Tam Kiếm Liên Hoàn: 3 nhát, mỗi nhát tiến 1 block, cung 120° tầm 3.5. */
    private void moveCombo(ServerLevel sl, LivingEntity tg, int t) {
        int[] at = {0, 9, 17};
        String[] an = {"combo_1", "combo_2", "combo_3"};
        double[] mult = {0.6, 0.6, 1.0};
        for (int i = 0; i < 3; i++) {
            if (t == at[i]) {
                anim(an[i]);
                a0 = position();
                a1 = a0.add(flatDir(a0, tg.position()).scale(edge(tg) > 1.5 ? 1 : 0));
            }
            if (t > at[i] && t <= at[i] + 3) glide(lerpGround((t - at[i]) / 3.0), tg.position());
            if (t == at[i] + 4) {
                Vec3 fwd = flatDir(position(), tg.position());
                Vec3 o = position().add(0, 1.2, 0);
                Fx.qi(sl, o, fwd.scale(2.5), 1.6f, COLOR, i == 0 ? -30 : i == 1 ? 30 : 85, 6);
                sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 1.1f + i * 0.15f);
                for (LivingEntity v : victims(sl, position(), 3.5)) if (inArc(v, fwd, 60)) strike(sl, v, mult[i]);
            }
        }
        if (t >= 30) {
            end();
            if (random.nextInt(3) == 0) { evadeKind = 2; begin(M7_EVADE); }
        }
    }

    /** M3 Thanh Minh Nhất Tuyến: lướt xuyên qua mục tiêu ra sau 6 block. */
    private void moveLine(ServerLevel sl, LivingEntity tg, int t) {
        if (t == 0) {
            cd[M3_LINE] = 120;
            a0 = position();
            Vec3 d = flatDir(a0, tg.position());
            double through = edge(tg) + tg.getBbWidth() + 6;
            a1 = a0.add(d.scale(through));
            Fx.telegraphLine(sl, null, a0.add(0, 0.1, 0), a1.subtract(a0), 1.6f, 8, COLOR);
            announce(sl, 3);
        }
        if (t < 8) face(tg.position());
        if (t == 6) anim("dash_slash");
        if (t >= 8 && t <= 12) {
            glide(lerpGround((t - 8) / 4.0), a1);
            if (t == 8) {
                Fx.ribbon(sl, null, a0.add(0, 1.1, 0), a1.add(0, 1.1, 0), 0, 4, 0, 0.6f, COLOR);
                sound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.6f, 1.4f);
            }
        }
        if (t == 10) {
            Vec3 mid = tg.position().add(0, tg.getBbHeight() * 0.5, 0);
            for (LivingEntity v : victims(sl, mid, 2.5 + tg.getBbWidth() * 0.5)) strike(sl, v, 1.6);
            sl.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 30, 0.6, 1.0, 0.6, 0.2);
            Fx.shake(sl, mid, 0.6f, 8, 20f);
        }
        if (t >= 20) { end(); face(tg.position()); }
    }

    /** M4 Phi Yến Hồi Toàn: nhảy 3 block, xoay, đáp xuống AoE r=3.5. */
    private void moveLeap(ServerLevel sl, LivingEntity tg, int t) {
        if (t == 0) {
            cd[M4_LEAP] = 100;
            a0 = position();
            Vec3 d = flatDir(tg.position(), a0);
            a1 = onGround(tg.position().add(d.scale(tg.getBbWidth() * 0.5 + 1.5)));
            Fx.groundRing(sl, null, a1.add(0, 0.05, 0), 3.5f, COLOR, 14);
            anim("spin_leap");
            announce(sl, 4);
        }
        if (t >= 2 && t <= 14) {
            double k = (t - 2) / 12.0;
            Vec3 p = a0.add(a1.subtract(a0).scale(k));
            glide(new Vec3(p.x, p.y + 3 * Math.sin(Math.PI * k), p.z), tg.position());
            if (t % 2 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 3, 0.4, 0.4, 0.4, 0.02);
        }
        if (t == 14) {
            Fx.groundCrack(sl, null, a1, 3.5f, COLOR, 8, 30);
            Fx.qi(sl, a1.add(0, 0.5, 0), Vec3.ZERO, 3.5f, COLOR, 0, 8);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, a1.x, a1.y + 0.6, a1.z, 6, 1.5, 0.2, 1.5, 0);
            sound(SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 1.5f);
            Fx.shake(sl, a1, 0.7f, 8, 20f);
            for (LivingEntity v : victims(sl, a1, 3.5)) strike(sl, v, 1.3);
        }
        if (t >= 24) end();
    }

    /** M5 Kiếm Khí Tam Điệp: lùi 3 block rồi phóng 3 dải kiếm khí. */
    private void moveQi(ServerLevel sl, LivingEntity tg, int t) {
        if (t == 0) {
            cd[M5_QI] = 80;
            anim("backstep");
            a0 = position();
            a1 = a0.add(flatDir(tg.position(), a0).scale(3));
            announce(sl, 5);
        }
        if (t >= 1 && t <= 5) glide(lerpGround(easeOut(t / 5.0)), tg.position());
        if (t == 6) Fx.telegraphCone(sl, null, position().add(0, 0.1, 0), flatDir(position(), tg.position()), 6f, 20f, 6);
        if (t == 10) anim("qi_cast");
        for (int i = 0; i < 3; i++) {
            if (t == 12 + i * 3) {
                Vec3 o = position().add(0, 1.3, 0), b = tg.position().add(0, tg.getBbHeight() * 0.5, 0);
                Fx.qi(sl, o, b.subtract(o), 1.4f, COLOR, -40 + i * 40, 10);
                sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 1.5f);
            }
            if (t == 16 + i * 3) for (LivingEntity v : victims(sl, tg.position(), tg.getBbWidth() * 0.5 + 1.5)) strike(sl, v, 0.5);
        }
        if (t >= 26) end();
    }

    /** M6 Thiên Ảnh Bách Kiếm: lơ lửng 1 block, rune, 12 kiếm rơi xuống mục tiêu. */
    private void moveRain(ServerLevel sl, LivingEntity tg, int t) {
        if (t == 0) {
            cd[M6_RAIN] = 500;
            anim("sword_rain_cast");
            a0 = position();
            Fx.rune(sl, null, a0.add(0, 0.1, 0), 3f, COLOR, 24, 8);
            sound(SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
            announce(sl, 6);
        }
        if (t <= 8) glide(a0.add(0, t / 8.0, 0), tg.position());
        else if (t < 44) face(tg.position());
        if (t == 6) for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            Vec3 o = position().add(Math.cos(a) * 1.6, 1.2, Math.sin(a) * 1.6);
            Fx.swordFly(sl, o, o.add(0, 3, 0), 10, i, 0.8f);
        }
        if (t >= 20 && t < 44 && (t - 20) % 2 == 0) {
            Vec3 c = tg.position();
            double r = tg.getBbWidth() * 0.5 + random.nextDouble() * 1.2, a = random.nextDouble() * Math.PI * 2;
            Vec3 hit = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
            Fx.swordStab(sl, hit.add(0, 10, 0), hit, 6, 0, 1.0f);
        }
        if (t >= 26 && t < 50 && (t - 26) % 2 == 0) {
            for (LivingEntity v : victims(sl, tg.position(), tg.getBbWidth() * 0.5 + 2)) strike(sl, v, 0.25);
            if (t % 6 == 0) sound(SoundEvents.TRIDENT_HIT, 1.5f, 1.3f);
        }
        if (t >= 44 && t <= 50) glide(a0.add(0, 1 - (t - 44) / 6.0, 0), tg.position());
        if (t >= 52) end();
    }

    /** M7 né bên / lộn ngược: 3 block trong 6 tick. */
    private void moveEvade(ServerLevel sl, LivingEntity tg, int t) {
        if (t == 0) {
            cd[M7_EVADE] = 50;
            Vec3 away = tg == null ? getLookAngle().scale(-1).multiply(1, 0, 1) : flatDir(tg.position(), position());
            if (away.lengthSqr() < 1e-6) away = new Vec3(0, 0, 1);
            away = away.normalize();
            int kind = evadeKind != 0 ? evadeKind : random.nextInt(3);
            evadeKind = 0;
            Vec3 side = new Vec3(-away.z, 0, away.x);
            Vec3 d = kind == 0 ? side : kind == 1 ? side.scale(-1) : away;
            anim(kind == 0 ? "sidestep_r" : kind == 1 ? "sidestep_l" : "backflip");
            a0 = position();
            a1 = a0.add(d.scale(3));
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 6, 0.3, 0.05, 0.3, 0.02);
        }
        if (t >= 1 && t <= 6) {
            Vec3 p = lerpGround(easeOut(t / 6.0));
            glide(p, tg == null ? null : tg.position());
        }
        if (t >= 10) end();
    }

    /** M8 Thanh Tâm Hộ Thể: tra kiếm, lướt tới người chơi yếu nhất, hồi 12% + Hấp Thụ II + khiên, rút kiếm lại. */
    private void moveHeal(ServerLevel sl, LivingEntity tg, int t) {
        ServerPlayer p = healTo;
        if (p == null || !p.isAlive()) { mv = NONE; rest = 10; return; }
        if (t == 0) {
            cd[M8_HEAL] = 300;
            sheatheSword();
            a0 = position();
            Vec3 d = flatDir(p.position(), a0);
            a1 = p.position().add(d.scale(1.6));
            announce(sl, 8);
        }
        if (t == 2) { anim("dash"); Fx.ribbon(sl, null, a0.add(0, 0.8, 0), a1.add(0, 0.8, 0), 0.6, 6, 0, 0.3f, COLOR); }
        if (t >= 2 && t <= 8) glide(lerpGround(easeOut((t - 2) / 6.0)), p.position());
        if (t == 10) Fx.rune(sl, null, position().add(0, 0.1, 0), 3f, COLOR, 14, 6);
        if (t == 14) anim("heal_cast");
        if (t == 20) {
            p.heal(p.getMaxHealth() * 0.12f);
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
            Fx.shell(sl, null, p, 1.2f, COLOR, 40);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 10, 0.5, 0.8, 0.5, 0.05);
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 3f, 1.3f);
            if (random.nextInt(2) == 0) NttdAllies.say(sl, this, "hukhong.endgame.modao.heal");
        }
        if (t == 30 && tg != null) drawSword();
        if (t >= 40) end();
    }

    // ================================================================ sát thương: chỉ NTTD / Hư mob, không bao giờ Player
    private java.util.List<LivingEntity> victims(ServerLevel sl, Vec3 c, double r) {
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r + 3), LivingEntity::isAlive)) {
            if (e instanceof Player || e == this || DaKhuyet.isStoryAlly(e)) continue;
            if (!(e instanceof NguyenThuyTichDiet n && !n.lv10Shade()) && !(e instanceof HuMob)) continue;
            double dx = e.getX() - c.x, dz = e.getZ() - c.z;
            if (Math.sqrt(dx * dx + dz * dz) - e.getBbWidth() * 0.5 <= r && Math.abs(e.getY() - c.y) < r + e.getBbHeight()) out.add(e);
        }
        return out;
    }

    private boolean inArc(LivingEntity v, Vec3 fwd, double halfDeg) {
        Vec3 d = v.position().subtract(position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 4) return true;
        return d.normalize().dot(fwd) >= Math.cos(Math.toRadians(halfDeg));
    }

    private void strike(ServerLevel sl, LivingEntity v, double mult) {
        if (v instanceof Player) return;
        float dmg;
        if (v instanceof NguyenThuyTichDiet) {
            double frac = Math.min(HIT_CAP, ALLY_FRAC * mult);
            frac = Math.max(0, Math.min(frac, TOTAL_CAP - dealt));
            dealt += frac;
            dmg = (float) (v.getMaxHealth() * frac);
        } else dmg = (float) (v.getMaxHealth() * 0.08 * mult);
        Vec3 h = v.position().add(0, v.getBbHeight() * 0.5, 0);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, h.x, h.y, h.z, 1, 0.4, 0.4, 0.4, 0);
        sl.sendParticles(ParticleTypes.CRIT, h.x, h.y, h.z, 8, 0.5, 0.6, 0.5, 0.2);
        if (dmg <= 0) return;
        v.invulnerableTime = 0;
        v.hurt(damageSources().mobAttack(this), dmg);
    }

    private void sound(SoundEvent s, float vol, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), s, SoundSource.NEUTRAL, vol, pitch);
    }

    private void announce(ServerLevel sl, int move) {
        Component c = Component.translatable("hukhong.endgame.modao.move." + move);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 48 * 48) p.displayClientMessage(c, true);
    }
}
