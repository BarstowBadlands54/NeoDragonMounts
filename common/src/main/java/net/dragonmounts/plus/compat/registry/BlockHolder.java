package net.dragonmounts.plus.compat.registry;

import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

@SuppressWarnings("unused")
public class BlockHolder<T extends Block> extends AbstractHolder<T, Block> implements ItemLike {
    public static <T extends Block> BlockHolder<T> registerBlock(String name, Function<Properties, T> factory) {
        return Dummy.get();
    }

    public BlockHolder(ResourceKey<Block> key, Function<Properties, T> factory) {
        super(key);
    }

    @Override
    public final Item asItem() {
        return Dummy.get();
    }

    public final BlockState defaultBlockState() {
        return Dummy.get();
    }
}