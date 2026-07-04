package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the dragon's equipped body armor (copper/iron/gold/emerald/diamond/netherite), using
 * whichever texture {@link net.dragonmounts.neo.common.client.variant.VariantAppearance#getArmorTexture}
 * resolves for this dragon's body category (falling back to the shared default texture set
 * registered under {@code category == null} in {@code VariantAppearances}).
 *
 * <p>NOTE: does not yet handle leather dragon armor (dyeable base + overlay, tinted by the
 * item's dye color) — {@code leather_dyeable.png}/{@code leather_overlay.png} aren't registered
 * in {@code registerArmorTextures} yet, and rendering them needs a second tinted pass similar to
 * vanilla leather armor.
 *
 * @see DragonTackLayer
 */
public class DragonArmorLayer extends GeoRenderLayer<TameableDragonEntity> {
    // Assumed to match the "body.chest" / "body.saddle" naming convention used by DragonTackLayer.
    // Adjust if the modeler used a different bone name for the armor overlay.
    private static final String ARMOR_BONE = "body.armor";

    public DragonArmorLayer(GeoRenderer<TameableDragonEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, TameableDragonEntity dragon, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (dragon.isInvisible() || dragon.deathTime > 0) return;

        ItemStack stack = dragon.inventory.armor.get();
        if (stack.isEmpty()) return;

        var appearance = dragon.getVariant().appearance; // assumed field name — see class javadoc
        if (appearance == null) return;

        ResourceLocation assetId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation texture = appearance.getArmorTexture(assetId);
        if (texture == null) return; // no texture registered for this material on this dragon's body

        renderBoneWithTexture(bakedModel, ARMOR_BONE, texture, poseStack, dragon, bufferSource, partialTick, packedLight);
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