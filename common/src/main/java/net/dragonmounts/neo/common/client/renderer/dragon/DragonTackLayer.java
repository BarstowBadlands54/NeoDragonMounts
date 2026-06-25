package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DragonTackLayer extends GeoRenderLayer<TameableDragonEntity> {
    private static final ResourceLocation SADDLE_TEX = makeId("textures/entity/dragon/saddle.png");
    private static final ResourceLocation CHEST_TEX  = makeId("textures/entity/dragon/chest.png");

    public DragonTackLayer(GeoRenderer<TameableDragonEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, TameableDragonEntity dragon, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (dragon.isInvisible() || dragon.deathTime > 0) return;

        if (dragon.isSaddled()) {
            renderBoneWithTexture(bakedModel, "body.saddle", SADDLE_TEX,
                    poseStack, dragon, bufferSource, partialTick, packedLight);
        }
        if (dragon.hasChest()) {
            renderBoneWithTexture(bakedModel, "body.chest", CHEST_TEX,
                    poseStack, dragon, bufferSource, partialTick, packedLight);
        }
    }

    private void renderBoneWithTexture(BakedGeoModel model, String boneName, ResourceLocation texture,
                                       PoseStack poseStack, TameableDragonEntity dragon,
                                       MultiBufferSource bufferSource, float partialTick, int packedLight) {
        // hide ALL top-level bones, then unhide only the chain leading to our target bone
        for (GeoBone top : model.topLevelBones()) {
            top.setHidden(true);
        }
        GeoBone target = model.getBone(boneName).orElse(null);
        if (target == null) {
            for (GeoBone top : model.topLevelBones()) top.setHidden(false);
            return;
        }
        // unhide the bone and all its ancestors (so the chain renders), but keep siblings' cubes hidden
        unhideChain(target);

        RenderType rt = RenderType.entityCutoutNoCull(texture);
        getRenderer().reRender(model, poseStack, bufferSource, dragon, rt,
                bufferSource.getBuffer(rt), partialTick, packedLight, OverlayTexture.NO_OVERLAY, -1);

        // restore: unhide everything
        for (GeoBone top : model.topLevelBones()) top.setHidden(false);
        resetChildrenHidden(model);
    }

    private void unhideChain(GeoBone bone) {
        while (bone != null) {
            bone.setHidden(false);
            bone.setChildrenHidden(false);
            bone = bone.getParent();
        }
    }

    private void resetChildrenHidden(BakedGeoModel model) {
        for (GeoBone top : model.topLevelBones()) {
            top.setChildrenHidden(false);
            resetRecursive(top);
        }
    }
    private void resetRecursive(GeoBone bone) {
        bone.setHidden(false);
        bone.setChildrenHidden(false);
        for (GeoBone c : bone.getChildBones()) resetRecursive(c);
    }
}