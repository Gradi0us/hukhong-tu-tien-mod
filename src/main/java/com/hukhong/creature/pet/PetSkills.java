package com.hukhong.creature.pet;

import com.hukhong.creature.LinhThu;
import com.hukhong.vfx.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ky nang chu dong cua tung nhanh (tu dung trong combat, co hoi chieu). Tang 2 manh hon va hoi chieu ngan hon.
 * Aura hat mau nhanh moi vai tick de nhan biet nhanh.
 */
public final class PetSkills {
    private PetSkills() {}

    private static final int FRENZY_TICKS = 120;

    /** Goi moi tick trong aiStep cua thu da thuan phuc (phia server). */
    public static void tick(LinhThu t) {
        if (!(t.level() instanceof ServerLevel sl) || t.isOrderedToSit()) return;
        if (t.getTarget() != null && t.tickCount % 60 == 0) PetForm.addBond(t, 1); // chien dau cung nhau
        if (t.frenzyTicks() > 0) t.setFrenzyTicks(t.frenzyTicks() - 1);
        if (t.formSkillCd() > 0) t.setFormSkillCd(t.formSkillCd() - 1);
        PetBranch b = t.petBranch();
        if (b == null || t.petStage() == 0) return;
        if (t.tickCount % 8 == 0) aura(sl, t, b);
        if (t.formSkillCd() > 0 || !(t.getOwner() instanceof ServerPlayer o)) return;
        if (cast(sl, t, o, b)) t.setFormSkillCd(t.petStage() >= 2 ? (int) (b.cooldown * 0.75) : b.cooldown);
    }

    /** Huyet Sat: trong dot cuong no, moi lan danh hoi mau cho thu. */
    public static void onHit(LinhThu t) {
        if (t.frenzyTicks() <= 0 || t.petBranch() != PetBranch.HUYET_SAT) return;
        float dmg = (float) t.getAttributeValue(Attributes.ATTACK_DAMAGE);
        t.heal(dmg * 0.35f * (t.petStage() >= 2 ? 1.4f : 1f));
    }

    private static boolean cast(ServerLevel sl, LinhThu t, ServerPlayer o, PetBranch b) {
        int rgb = b.rgb;
        boolean stage2 = t.petStage() >= 2;
        switch (b) {
            case HUYET_SAT -> {
                if (t.getTarget() == null) return false;
                t.setFrenzyTicks(FRENZY_TICKS);
                Fx.shell(sl, o, t, 1.2f, rgb, FRENZY_TICKS);
                sl.playSound(null, t.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6f, 1.4f);
                return true;
            }
            case HUYEN_GIAP -> { // khieu khich: quai gan do quay sang thu, thu co la chan
                List<Mob> foes = t.level().getEntitiesOfClass(Mob.class, t.getBoundingBox().inflate(8),
                        e -> e instanceof Enemy && e != t && !(e instanceof LinhThu) && e.isAlive());
                if (foes.isEmpty()) return false;
                for (Mob m : foes) m.setTarget(t);
                t.setAbsorptionAmount(Math.min(12f, t.getAbsorptionAmount() + 4f + 2f * t.petStage()));
                Fx.shell(sl, o, t, 1.6f, rgb, 40);
                Fx.groundRing(sl, o, t.position().add(0, 0.05, 0), 5f, rgb, 30);
                return true;
            }
            case LINH_PHONG -> { // hoi phuc chu + gio nhe
                boolean hurt = o.getHealth() < o.getMaxHealth();
                if (!hurt && t.getTarget() == null) return false;
                o.heal(6f * (stage2 ? 1.6f : 1f));
                o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0));
                Fx.ribbonHelix(sl, o, t.position().add(0, 1, 0), o.position().add(0, 1, 0), 0.6, 0, 20, 0, 0.3f, rgb);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.getX(), o.getY() + 1, o.getZ(), 12, 0.6, 0.8, 0.6, 0.02);
                return true;
            }
            case HU_KHONG -> { // bong chop: dich chuyen ra sau muc tieu, chem 2x sat thuong
                LivingEntity tg = t.getTarget();
                if (tg == null || !tg.isAlive() || t.distanceToSqr(tg) > 196) return false;
                Vec3 from = t.position();
                Vec3 look = tg.getLookAngle();
                double dx = tg.getX() - look.x * 1.5, dz = tg.getZ() - look.z * 1.5;
                t.getNavigation().stop();
                t.setPos(dx, tg.getY(), dz);
                tg.hurt(sl.damageSources().mobAttack(t), (float) (t.getAttributeValue(Attributes.ATTACK_DAMAGE) * (stage2 ? 2.6 : 2.0)));
                Fx.rift(sl, o, from.add(0, 1, 0), new Vec3(dx, tg.getY(), dz).subtract(from), 1.4f, rgb, 16);
                sl.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 20, 0.3, 0.6, 0.3, 0.2);
                sl.playSound(null, t.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8f, 1.2f);
                return true;
            }
        }
        return false;
    }

    private static void aura(ServerLevel sl, LinhThu t, PetBranch b) {
        DustParticleOptions d = new DustParticleOptions(new Vector3f(((b.rgb >> 16) & 0xFF) / 255f, ((b.rgb >> 8) & 0xFF) / 255f, (b.rgb & 0xFF) / 255f), 1.0f);
        sl.sendParticles(d, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 3, t.getBbWidth() * 0.5, t.getBbHeight() * 0.3, t.getBbWidth() * 0.5, 0);
    }
}
