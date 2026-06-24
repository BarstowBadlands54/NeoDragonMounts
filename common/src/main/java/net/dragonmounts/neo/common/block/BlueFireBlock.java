package net.dragonmounts.neo.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlueFireBlock extends BaseFireBlock {
    public static final MapCodec<BlueFireBlock> CODEC = simpleCodec(BlueFireBlock::new);

    public BlueFireBlock(Properties properties) {
        super(properties, 2.0F);   // 2.0F damage, same as soul fire
    }

    @Override
    protected MapCodec<? extends BaseFireBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;   // survives on ANY block, unlike vanilla soul fire
    }
}