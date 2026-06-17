package net.dragonmounts.neo.common.client.model.dragon;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * GeckoLib model resolver. Per-breed it returns:
 *  - which .geo.json (the breed's DragonShape)
 *  - which body texture (the breed's VariantAppearance texture)
 *  - the single shared animation file
 * GeckoLib bakes/caches each geo by ResourceLocation, so every shape loads once.
 */
public class DragonGeoModel extends GeoModel<TameableDragonEntity> {
    private static final ResourceLocation ANIMATIONS =
            makeId("animations/entity/dragon/dragon.animation.json");

    @Override
    public ResourceLocation getModelResource(TameableDragonEntity dragon) {
        return dragon.getVariant().getDragonType().geoModel();   // DragonShape.geo of this breed
    }

    @Override
    public ResourceLocation getTextureResource(TameableDragonEntity dragon) {
        return dragon.getVariant().getDragonType().texture();     // per-breed body png
    }

    @Override
    public ResourceLocation getAnimationResource(TameableDragonEntity dragon) {
        return ANIMATIONS;
    }
}