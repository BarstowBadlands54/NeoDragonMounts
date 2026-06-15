package net.dragonmounts.neo.common.client.model.dragon;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class DragonModel extends GeoModel<TameableDragonEntity> {

    @Override
    public ResourceLocation getModelResource(TameableDragonEntity animatable) {
        return null;
    }

    @Override
    public ResourceLocation getTextureResource(TameableDragonEntity animatable) {
        return null;
    }

    @Override
    public ResourceLocation getAnimationResource(TameableDragonEntity animatable) {
        return null;
    }
}
