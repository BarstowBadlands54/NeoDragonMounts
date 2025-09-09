package net.dragonmounts.neo.compat.registry;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.function.BiFunction;

@SuppressWarnings("unused")
public class BlockItemHolder<B extends Block, I extends Item> extends AbstractHolder<I, Item> implements ItemLike {
    public static <B extends Block, I extends Item> BlockItemHolder<B, I> registerItem(BlockHolder<B> block, BiFunction<B, Item.Properties, I> factory) {
        return Dummy.get();
    }

    public final BlockHolder<B> block;

    public BlockItemHolder(BlockHolder<B> block, ResourceKey<Item> key, I item) {
        super(key);
        this.block = block;

    }

    @Override
    public Item asItem() {
        return Dummy.get();
    }
}