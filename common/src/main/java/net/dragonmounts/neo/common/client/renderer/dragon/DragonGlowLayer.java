package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class DragonGlowLayer extends GeoRenderLayer<TameableDragonEntity> {
    public DragonGlowLayer(GeoRenderer<TameableDragonEntity> renderer) {
        super(renderer);
    }

    private ResourceLocation glowTexture(TameableDragonEntity animatable) {
        ResourceLocation body = getRenderer().getTextureLocation(animatable);
        return body.withPath(body.getPath().replace("/body.png", "/glow.png"));
    }

    @Override
    public void render(PoseStack poseStack, TameableDragonEntity animatable, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable.isInvisible()) return;
        RenderType emissive = RenderType.entityTranslucentEmissive(glowTexture(animatable));
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissive,
                bufferSource.getBuffer(emissive), partialTick, LightTexture.FULL_SKY, packedOverlay,
                -1);   // white, no tint
    }
}