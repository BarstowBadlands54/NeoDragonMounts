package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dragonmounts.neo.common.client.model.dragon.DragonGeoModel;
import net.dragonmounts.neo.common.client.renderer.RenderStateAccessor;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;


import static net.dragonmounts.neo.common.client.variant.VariantAppearance.DEFAULT_DISSOLVE;

/**
 * GeckoLib 4 single-type-param renderer (correct for 1.21.1).
 * Replaces the whole vanilla TameableDragonRenderer / DragonRenderState stack.
 */
public class DragonRenderer extends GeoEntityRenderer<TameableDragonEntity> {
    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonGeoModel());
        addRenderLayer(new DragonGlowLayer(this));
        addRenderLayer(new DragonTackLayer(this));
        addRenderLayer(new DragonArmorLayer(this));
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

    @Override
    protected void applyRotations(TameableDragonEntity animatable, PoseStack poseStack,
                                  float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);

        if (animatable.isFlying()) {   // bank under a rider OR during autonomous flight (bronco / follow)
            float pitch = Mth.lerp(partialTick, animatable.renderPitchO, animatable.renderPitch);
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

            float roll = Mth.lerp(partialTick, animatable.renderRollO, animatable.renderRoll);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-roll));   // bank into turns
        }
    }

    @Override
    protected float getDeathMaxRotation(TameableDragonEntity animatable) {
        return 0.0F;
    }
}