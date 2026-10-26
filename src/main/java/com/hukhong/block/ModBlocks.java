package com.hukhong.block;

import com.hukhong.HuKhong;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HuKhong.MODID);

    public static final DeferredBlock<AnHonDaiBlock> AN_HON_DAI = BLOCKS.register("an_hon_dai",
            () -> new AnHonDaiBlock(BlockBehaviour.Properties.of().strength(3.5f, 1200f).sound(SoundType.AMETHYST).lightLevel(s -> 7)));
}
