package net.dragonmounts.neo.common.client.model.block;

import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/** GeoModel for the dragon-head item (GUI/hand). Resolves geo/texture from the item's own variant. */
public class DragonHeadItemGeoModel extends GeoModel<DragonHeadItem> {
    private static final ResourceLocation ANIMATIONS = makeId("animations/entity/dragon/dragon.animation.json");

    @Override
    public ResourceLocation getModelResource(DragonHeadItem item) {
        return item.variant.type.headGeoModel();
    }

    @Override
    public ResourceLocation getTextureResource(DragonHeadItem item) {
        return item.variant.appearance.getBodyTexture(null);
    }

    @Override
    public ResourceLocation getAnimationResource(DragonHeadItem item) {
        return ANIMATIONS;
    }
}