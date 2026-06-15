package net.dragonmounts.neo.common.client.renderer.dragon;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DragonRenderer extends GeoEntityRenderer<TameableDragonEntity> {

    public DragonRenderer(EntityRendererProvider.Context context, EntityType<? extends TameableDragonEntity> entityType) {
        super(context, entityType);
    }
}
