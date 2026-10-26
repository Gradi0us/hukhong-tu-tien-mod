package com.hukhong.endgame;

import com.hukhong.flight.FlightBans;
import com.hukhong.item.ModItems;
import com.hukhong.vfx.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Dạ Khuyết (Bước 25): xuất hiện ở pha 3 của Nguyên Thủy Tịch Diệt. "Đêm Khuyết" (mù + yếu diện rộng)
 * và "Dịch Lời" (phiên dịch ý chí Hư Không: hồi máu cho boss chính). Hạ hắn trước thì boss mất nguồn hồi.
 */
public class DaKhuyet extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.hukhong.da_khuyet"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private int skillCd = 80, windup, pending = -1;

    public DaKhuyet(EntityType<? extends Monster> t, Level l) {
        super(t, l);
        addTag(FlightBans.WORLD_BOSS_TAG);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6000).add(Attributes.ATTACK_DAMAGE, 34).add(Attributes.ARMOR, 18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canUsePortal(boolean allowPassengers) { return false; }
    @Override public boolean isImmobile() { return super.isImmobile() || windup > 0; }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason r) { super.remove(r); bar.removeAllPlayers(); }

    /** Xuất hiện: thoại + title cho người chơi quanh đó. */
    public void begin(ServerLevel sl, NguyenThuyTichDiet boss) {
        setHealth(getMaxHealth());
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) > 100 * 100) continue;
            p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.appear.1"), false);
            p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.appear.2"), false);
        }
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 40, 1, 1.5, 1, 0.05);
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel sl = (ServerLevel) level();
        bar.setProgress(getHealth() / getMaxHealth());
        if (pending >= 0) {
            getNavigation().stop();
            if (windup % 4 == 0) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 6, 3, 1.5, 3, 0.02);
            if (--windup <= 0) {
                fire(sl, pending);
                pending = -1;
                skillCd = 110;
            }
            return;
        }
        if (skillCd > 0) { skillCd--; return; }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || distanceToSqr(t) > 24 * 24) return;
        pending = random.nextInt(3) == 0 ? 1 : 0;
        windup = pending == 0 ? 35 : 45;
        triggerAnim("skill", pending == 0 ? "skill_a" : "skill_b");
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
    }

    private void fire(ServerLevel sl, int skill) {
        if (skill == 0) {  // Đêm Khuyết
            double dmg = getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8;
            for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10, 3, 10))) {
                if (p.isSpectator() || p.distanceToSqr(this) > 100) continue;
                p.hurt(damageSources().mobAttack(this), (float) dmg);
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            }
            sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 60, 4, 1.5, 4, 0.05);
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 30, 4, 1, 4, 0.05);
        } else {  // Dịch Lời: hồi 3% cho boss chính
            for (NguyenThuyTichDiet b : sl.getEntitiesOfClass(NguyenThuyTichDiet.class, getBoundingBox().inflate(60))) {
                b.heal(b.getMaxHealth() * 0.03f);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, b.getX(), b.getY() + 3, b.getZ(), 30, 1.5, 2, 1.5, 0.1);
            }
            sl.sendParticles(ModParticles.HU_KHONG.get(), getX(), getY() + 1.5, getZ(), 30, 1, 1.5, 1, 0.1);
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 1.4f);
    }

    @Override public void die(DamageSource src) {
        super.die(src);
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players())
            if (p.distanceToSqr(this) < 100 * 100) p.displayClientMessage(Component.translatable("hukhong.endgame.dakhuyet.fall"), false);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel sl, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(sl, src, recentlyHit);
        sl.addFreshEntity(new ItemEntity(sl, getX(), getY() + 0.5, getZ(), new ItemStack(ModItems.HON_TINH.get(), 20 + random.nextInt(8))));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        String n = "animation.da_khuyet.";
        c.add(new AnimationController<>(this, "move", 4, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop(n + "walk") : RawAnimation.begin().thenLoop(n + "idle"))));
        AnimationController<DaKhuyet> sk = new AnimationController<>(this, "skill", 0, s -> PlayState.STOP);
        for (String a : new String[]{"attack", "skill_a", "skill_b"})
            sk.triggerableAnim(a, RawAnimation.begin().then(n + a, Animation.LoopType.PLAY_ONCE));
        c.add(sk);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
