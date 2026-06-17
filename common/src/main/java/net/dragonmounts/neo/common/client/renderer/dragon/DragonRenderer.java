package net.dragonmounts.neo.common.client.renderer.dragon;

import net.dragonmounts.neo.common.client.model.dragon.DragonGeoModel;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib 4 single-type-param renderer (correct for 1.21.1).
 * Replaces the whole vanilla TameableDragonRenderer / DragonRenderState stack.
 */
public class DragonRenderer extends GeoEntityRenderer<TameableDragonEntity> {
    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonGeoModel());
        this.shadowRadius = 1.5F;
        // Emissive glow: add a GeoRenderLayer here once the glow textures are
        // set up as *_glowmask.png (AutoGlowingGeoLayer) or a custom layer.
        // addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}