package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.client.model.dragon.DragonGeoModel;
import net.dragonmounts.neo.common.client.renderer.RenderStateAccessor;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
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
        addRenderLayer(new DragonGlowLayer(this));
    }

    /** Scale the model by life stage so hatchlings render small and grow to adult size. */
    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    TameableDragonEntity animatable, BakedGeoModel model,
                                    boolean isReRender, float partialTick, int packedLight, int packedOverlay) {
        float age = animatable.getAgeScale();
        super.scaleModelForRender(widthScale * age, heightScale * age, poseStack, animatable,
                model, isReRender, partialTick, packedLight, packedOverlay);
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

    @Override
    public int getPackedOverlay(TameableDragonEntity animatable, float u, float partialTick) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(animatable.hurtTime > 0));
    }

    @Override
    protected float getShadowRadius(TameableDragonEntity animatable) {
        return this.shadowRadius * animatable.getAgeScale();
    }
}