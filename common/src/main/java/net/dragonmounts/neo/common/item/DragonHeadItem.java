package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

public class DragonHeadItem extends StandingAndWallBlockItem implements DragonTypified, Equipable {
    public final DragonVariant variant;

    public DragonHeadItem(DragonVariant variant, Block standing, Block wall, Properties props) {
        // 1.21.1: ctor order is (Block, Block, Properties, Direction); no equippableUnswappable -- use Equipable
        super(standing, wall, props.component(DMDataComponents.DRAGON_TYPE, variant.type), Direction.DOWN);
        this.variant = variant;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public DragonType getDragonType() {
        return this.variant.type;
    }
}