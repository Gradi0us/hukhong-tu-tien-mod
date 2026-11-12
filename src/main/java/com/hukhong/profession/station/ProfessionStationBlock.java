package com.hukhong.profession.station;

import com.hukhong.profession.ProfessionRecipes;
import com.hukhong.profession.ProfessionRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Tram che nghe phu: Phu An (Phu Van Su) va Doan Tao Dai (Doan Tao Su). Chuot phai mo menu 3 o nguyen lieu + 1 o ket qua. */
public class ProfessionStationBlock extends BaseEntityBlock {
    public enum Station {
        PHU_AN("block.hukhong.phu_an"),
        DOAN_TAO_DAI("block.hukhong.doan_tao_dai");

        public final String key;

        Station(String key) { this.key = key; }

        public List<ProfessionRecipes.Recipe> recipes() {
            return this == PHU_AN ? ProfessionRecipes.PHU : ProfessionRecipes.FORGE;
        }
    }

    /** Codec chi dung cho dinh nghia block; ban Phu An la mac dinh. */
    public static final MapCodec<ProfessionStationBlock> CODEC =
            simpleCodec(p -> new ProfessionStationBlock(Station.PHU_AN, p));

    public final Station station;

    public ProfessionStationBlock(Station station, Properties props) {
        super(props);
        this.station = station;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ProfessionStationBlockEntity(pos, state); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ProfessionStationBlockEntity be) sp.openMenu(be, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProfessionStationBlockEntity be)
            Containers.dropContents(level, pos, be);
        super.onRemove(state, level, pos, newState, moving);
    }
}
