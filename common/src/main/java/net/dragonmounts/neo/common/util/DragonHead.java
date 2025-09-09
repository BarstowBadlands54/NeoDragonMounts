package net.dragonmounts.neo.common.util;

import net.dragonmounts.neo.common.block.DragonHeadStandingBlock;
import net.dragonmounts.neo.common.block.DragonHeadWallBlock;
import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.dragonmounts.neo.compat.registry.BlockHolder;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.dragonmounts.neo.compat.registry.ItemHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public final class DragonHead implements ItemLike {
    public final DragonVariant variant;
    public final BlockHolder<DragonHeadStandingBlock> standing;
    public final BlockHolder<DragonHeadWallBlock> wall;
    public final ItemHolder<DragonHeadItem> item;

    public DragonHead(
            DragonVariant variant,
            String standingName,
            String wallName,
            BiFunction<DragonHead, String, BlockHolder<DragonHeadStandingBlock>> standing,
            BiFunction<DragonHead, String, BlockHolder<DragonHeadWallBlock>> wall,
            BiFunction<DragonHead, String, ItemHolder<DragonHeadItem>> item
    ) {
        this.variant = variant;
        this.standing = standing.apply(this, standingName);
        this.wall = wall.apply(this, wallName);
        this.item = item.apply(this, standingName);
    }

    @Override
    public @NotNull Item asItem() {
        return this.item.get();
    }
}
