package com.hukhong.treasure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Bảo Rương: 4 cấp (TIER 0 Gỗ Đồng, 1 Bích Ngọc, 2 Hoàng Kim, 3 Hư Không). LOOTED = đã mở lần đầu (tắt bớt ánh sáng/hạt). */
public class BaoRuongBlock extends BaseEntityBlock {
    public static final MapCodec<BaoRuongBlock> CODEC = simpleCodec(BaoRuongBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, 3);
    public static final BooleanProperty LOOTED = BooleanProperty.create("looted");
    static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);
    public static final int[] COLOR = {0xFFC45A, 0x6EFFBE, 0xFFE282, 0xC460FF};

    public BaoRuongBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, 0).setValue(LOOTED, false));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, TIER, LOOTED); }

    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }

    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new BaoRuongBE(pos, s); }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level lv, BlockPos pos, Player p, BlockHitResult hit) {
        if (lv.isClientSide) return InteractionResult.SUCCESS;
        if (lv.getBlockEntity(pos) instanceof BaoRuongBE be && p instanceof ServerPlayer sp) {
            if (!s.getValue(LOOTED)) {
                be.unpackLootTable(sp);
                TreasureFx.firstOpen((ServerLevel) lv, pos, s.getValue(TIER), sp);
                lv.setBlock(pos, s.setValue(LOOTED, true), 3);
            }
            sp.openMenu(be);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState s, Level lv, BlockPos pos, BlockState n, boolean moved) {
        if (!s.is(n.getBlock()) && lv.getBlockEntity(pos) instanceof BaoRuongBE be) Containers.dropContents(lv, pos, be);
        super.onRemove(s, lv, pos, n, moved);
    }

    /** Hạt lấp lánh khi chưa mở: bụi màu theo cấp bay lên, tia sáng trắng; cấp cao thêm hạt đặc trưng. */
    @Override
    public void animateTick(BlockState s, Level lv, BlockPos pos, RandomSource r) {
        int t = s.getValue(TIER), rgb = COLOR[t];
        boolean looted = s.getValue(LOOTED);
        var dust = new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 1.2f);
        int n = looted ? 1 : 2 + t;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.5 + r.nextDouble() * 0.6;
            lv.addParticle(dust, pos.getX() + 0.5 + Math.cos(a) * d, pos.getY() + 0.2 + r.nextDouble() * 0.9, pos.getZ() + 0.5 + Math.sin(a) * d, 0, 0.02, 0);
        }
        if (looted) return;
        if (r.nextInt(3) == 0) lv.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5 + (r.nextDouble() - .5) * .6, pos.getY() + 1.0, pos.getZ() + 0.5 + (r.nextDouble() - .5) * .6, 0, 0.03, 0);
        if (t == 2 && r.nextInt(2) == 0) lv.addParticle(ParticleTypes.WAX_ON, pos.getX() + r.nextDouble(), pos.getY() + 0.9 + r.nextDouble() * 0.5, pos.getZ() + r.nextDouble(), 0, 0.02, 0);
        if (t == 3) lv.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, (r.nextDouble() - .5) * .3, 0.05, (r.nextDouble() - .5) * .3);
        if (t >= 1 && r.nextInt(4) == 0) lv.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, (r.nextDouble() - .5) * 2, -1.2, (r.nextDouble() - .5) * 2);
    }
}
