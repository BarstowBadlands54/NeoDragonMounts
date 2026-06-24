package net.dragonmounts.neo.common.client.model.dragon;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DragonGeoModel extends GeoModel<TameableDragonEntity> {
    private static final ResourceLocation ANIMATIONS =
            makeId("animations/entity/dragon/dragonmounts2.dragon.animation.json");

    @Override
    public ResourceLocation getModelResource(TameableDragonEntity dragon) {
        return dragon.getVariant().getDragonType().geoModel();   // DragonShape.geo of this breed
    }

    @Override
    public ResourceLocation getTextureResource(TameableDragonEntity dragon) {
        return dragon.getVariant().appearance.getBodyTexture(null);
    }

    @Override
    public ResourceLocation getAnimationResource(TameableDragonEntity dragon) {
        return ANIMATIONS;
    }
}