package com.hukhong.dongphu;

import com.hukhong.vfx.ModParticles;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Động Phủ Trận Bàn: khối lõi (model GeckoLib). Đặt = lập trận; chuột phải = nâng tầng; Shift + chuột phải = trạng thái. */
public class DongPhuTranBanBlock extends BaseEntityBlock {
    public static final MapCodec<DongPhuTranBanBlock> CODEC = simpleCodec(DongPhuTranBanBlock::new);
    /** Bản sao tầng để hiển thị (nguồn chân lý là DongPhuData). */
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, DongPhuService.MAX_TIER);
    static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public DongPhuTranBanBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(TIER, 1));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(TIER); }

    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new DongPhuTranBanBE(pos, s); }

    @Override
    public void setPlacedBy(Level lv, BlockPos pos, BlockState s, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(lv, pos, s, placer, stack);
        if (lv instanceof ServerLevel sl && placer instanceof ServerPlayer sp) DongPhuService.claim(sp, sl, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level lv, BlockPos pos, Player p, BlockHitResult hit) {
        if (lv.isClientSide) return InteractionResult.SUCCESS;
        if (!(p instanceof ServerPlayer sp) || !(lv instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (p.isShiftKeyDown()) {
            DongPhuService.status(sp, sl, pos);
        } else {
            Component err = DongPhuService.upgrade(sp, sl, pos);
            if (err != null) sp.displayClientMessage(err, true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState s, Level lv, BlockPos pos, BlockState n, boolean moved) {
        if (!s.is(n.getBlock()) && lv instanceof ServerLevel sl) DongPhuService.forget(sl, pos);
        super.onRemove(s, lv, pos, n, moved);
    }

    /** Linh khí bốc lên quanh lõi; tầng cao nhiều hạt hơn. Chỉ phía client. */
    @Override
    public void animateTick(BlockState s, Level lv, BlockPos pos, RandomSource r) {
        int t = s.getValue(TIER);
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.9, cz = pos.getZ() + 0.5;
        int n = 1 + t / 2;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.6 + r.nextDouble() * 1.4;
            lv.addParticle(ModParticles.LINH_KHI.get(), cx + Math.cos(a) * d, cy + r.nextDouble() * 0.4, cz + Math.sin(a) * d,
                    0, 0.04 + r.nextDouble() * 0.04, 0);
        }
        if (r.nextInt(3) == 0) lv.addParticle(DongPhuFx.JADE_DUST, cx + (r.nextDouble() - .5) * 1.6, cy + 1.2 + r.nextDouble() * 0.6, cz + (r.nextDouble() - .5) * 1.6, 0, 0.03, 0);
        if (r.nextInt(6) == 0) lv.addParticle(ParticleTypes.ENCHANT, cx, cy + 1.6, cz, (r.nextDouble() - .5) * 1.6, -1.1, (r.nextDouble() - .5) * 1.6);
        if (t >= 3 && r.nextInt(8) == 0) lv.addParticle(ParticleTypes.END_ROD, cx + (r.nextDouble() - .5) * 2.4, cy + 2.2, cz + (r.nextDouble() - .5) * 2.4, 0, 0.02, 0);
    }
}
