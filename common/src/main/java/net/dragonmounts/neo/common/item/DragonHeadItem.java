package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;

import java.util.function.Consumer;

public class DragonHeadItem extends StandingAndWallBlockItem implements DragonTypified, Equipable, GeoItem {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_head";
    public final DragonVariant variant;
    public final TranslatableContents name;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DragonHeadItem(DragonVariant variant, Block standing, Block wall, Properties props) {
        // 1.21.1: ctor order is (Block, Block, Properties, Direction); no equippableUnswappable -- use Equipable
        super(standing, wall, props.component(DMDataComponents.DRAGON_TYPE, variant.type), Direction.DOWN);
        this.variant = variant;
        // build "<Type> Dragon Head" from the .name key with the dragon type as %s, mirroring
        // DragonScalesItem / the gear items so the head shows its type instead of a bare "Dragon Head".
        this.name = new TranslatableContents(TRANSLATION_KEY + ".name", null, new Object[]{MutableComponent.create(variant.type.name)});
    }

    @Override
    public Component getName(ItemStack stack) {
        return MutableComponent.create(this.name);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public DragonType getDragonType() {
        return this.variant.type;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Item GUI/hand render uses the same head geo model; no animation controllers needed
        // for a static display. Add controllers here if the held item should animate.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private BlockEntityWithoutLevelRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new net.dragonmounts.neo.common.client.renderer.item.DragonHeadItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}