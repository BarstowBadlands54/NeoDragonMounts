package net.dragonmounts.neo.common.client.renderer.dragon;

import net.dragonmounts.neo.common.client.model.dragon.DragonGeoModel;
import net.dragonmounts.neo.common.client.renderer.RenderStateAccessor;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.dragonmounts.neo.common.client.renderer.RenderStateAccessor;
import software.bernie.geckolib.util.Color;

import javax.annotation.Nullable;

import static net.dragonmounts.neo.common.client.variant.VariantAppearance.DEFAULT_DISSOLVE;

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
         addRenderLayer(new DragonGlowLayer(this));
    }

    @Override
    public RenderType getRenderType(TameableDragonEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        if (animatable.deathTime > 0) {
            // body dissolves through the custom decal shader during death
            return RenderStateAccessor.entityCutoutDecal(texture, DEFAULT_DISSOLVE);
        }
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(TameableDragonEntity animatable, float partialTick, int packedLight) {
        if (animatable.deathTime > 0) {
            int max = animatable.getMaxDeathTime();
            int alpha = Math.min(Mth.floor((animatable.deathTime + partialTick) * 255.0F / max), 255);
            return Color.ofARGB(alpha, 255, 255, 255);
        }
        return Color.WHITE;
    }

}