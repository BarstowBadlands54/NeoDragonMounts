package net.dragonmounts.neo.common.client.model.block;

import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DragonHeadItemGeoModel extends GeoModel<DragonHeadItem> {
    private static final ResourceLocation ANIMATIONS = makeId("animations/entity/dragon/dragonmounts2.dragon.animation.json");

    @Override
    public ResourceLocation getModelResource(DragonHeadItem item) {
        var appearance = item.variant.appearance;
        if (appearance != null) {
            var override = appearance.getHeadGeoModel();
            if (override != null) return override;
        }
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