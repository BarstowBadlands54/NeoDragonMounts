package net.dragonmounts.neo.compat.registry;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

@SuppressWarnings("unused")
public class BlockEntityHolder<T extends BlockEntity> extends AbstractHolder<BlockEntityType<T>, BlockEntityType<?>> {
    public static <T extends BlockEntity> BlockEntityHolder<T> registerBlockEntity(String name, Factory<T> factory, BlockHolder<?>... blocks) {
        return Dummy.get();
    }

    public final Set<BlockHolder<?>> blocks;

    public BlockEntityHolder(ResourceKey<BlockEntityType<?>> key, Factory<? extends T> factory, BlockHolder<?>... blocks) {
        super(key);
        this.blocks = Set.of(blocks);
    }

    @FunctionalInterface
    public interface Factory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }
}
