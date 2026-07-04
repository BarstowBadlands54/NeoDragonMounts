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
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders the dragon's equipped body armor (copper/iron/gold/emerald/diamond/netherite) as a
 * full-body texture overlay, using whichever texture
 * {@link net.dragonmounts.neo.common.client.variant.VariantAppearance#getArmorTexture} resolves
 * for this dragon's body category (falling back to the shared default texture set registered
 * under {@code category == null} in {@code VariantAppearances}).
 *
 * <p>Unlike {@link DragonTackLayer}'s chest/saddle (which are separate attached cubes and need
 * per-bone isolation to render), armor re-uses the dragon's existing body geometry — it's just a
 * different texture over the same shape, like vanilla horse armor. So this is a plain full-model
 * re-render with the armor texture, no bone hiding needed.
 *
 * <p>NOTE: does not yet handle leather dragon armor (dyeable base + overlay, tinted by the
 * item's dye color) — {@code leather_dyeable.png}/{@code leather_overlay.png} aren't registered
 * in {@code registerArmorTextures} yet, and rendering them needs a second tinted pass similar to
 * vanilla leather armor.
 *
 * @see DragonTackLayer
 */
public class DragonArmorLayer extends GeoRenderLayer<TameableDragonEntity> {
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

        RenderType rt = RenderType.entityCutoutNoCull(texture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, dragon, rt,
                bufferSource.getBuffer(rt), partialTick, packedLight, OverlayTexture.NO_OVERLAY, -1);
    }
}