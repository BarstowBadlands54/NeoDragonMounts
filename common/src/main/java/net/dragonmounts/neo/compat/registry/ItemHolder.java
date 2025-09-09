package net.dragonmounts.neo.compat.registry;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;

@SuppressWarnings("unused")
public class ItemHolder<T extends Item> extends AbstractHolder<T, Item> implements ItemLike {
    public static <T extends Item> ItemHolder<T> registerItem(String name, Function<Item.Properties, T> factory) {
        return Dummy.get();
    }

    public ItemHolder(ResourceKey<Item> key, Function<Item.Properties, T> factory) {
        super(key);
    }

    public final boolean is(ItemStack stack) {
        return Dummy.get();
    }

    @Override
    public Item asItem() {
        return Dummy.get();
    }
}
