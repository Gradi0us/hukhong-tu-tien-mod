package com.hukhong.voidblock;

import com.hukhong.effect.ModEffects;
import com.hukhong.entity.HuMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Vết nứt hư không: đứng lên bị Hư Hóa + sát thương nhẹ. */
public class VoidRiftBlock extends Block {
    public VoidRiftBlock(Properties p) { super(p); }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
        if (!level.isClientSide && e instanceof LivingEntity le && !(le instanceof HuMob) && e.tickCount % 20 == 0) {
            le.hurt(level.damageSources().magic(), 1.0f);
            le.addEffect(new MobEffectInstance(ModEffects.HU_HOA, 200, 0));
        }
        super.stepOn(level, pos, state, e);
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        if (r.nextInt(3) == 0)
            l.addParticle(ParticleTypes.REVERSE_PORTAL, p.getX() + r.nextDouble(), p.getY() + 1.0, p.getZ() + r.nextDouble(), 0, 0.05, 0);
    }
}
