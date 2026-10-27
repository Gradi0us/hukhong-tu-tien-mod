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
 * Bang Nhan: cu nhan bang cua nui tuyet. Bo ky nang: Loat Bang (5 tia bang), Bang Bao Nova (dong bang dien rong),
 * Mua Bang Truy (diem don khoanh vung quanh nguoi choi); pha 2: Bao Tuyet (cham + yeu dien rong).
 */
public class BangNhan extends WorldBoss {
    private static final int VOLLEY = 0, NOVA = 1, RAIN = 2, BLIZZARD = 3;
    private final List<Vec3> spots = new ArrayList<>();

    public BangNhan(EntityType<? extends Monster> t, Level l) { super(t, l, BossEvent.BossBarColor.WHITE); }

    @Override protected String id() { return "bang_nhan"; }
    @Override protected String track() { return "su_do"; }
    @Override protected int skillCount(int phase) { return phase >= 2 ? 4 : 3; }
    @Override protected int windupOf(int s) { return switch (s) { case VOLLEY -> 22; case NOVA -> 28; case RAIN -> 40; default -> 36; }; }
    @Override protected String animOf(int s) { return switch (s) { case VOLLEY -> "skill_a"; case NOVA, BLIZZARD -> "skill_b"; default -> "skill_c"; }; }
    @Override protected int cooldown(int phase) { return phase >= 2 ? 72 : 100; }
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
}
