package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonmounts.neo.common.client.variant.DragonArmorSkin;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Draws the dragon's equipped body armour, using whichever {@link DragonArmorSkin} this dragon's
 * body category resolves for the equipped item.
 * <p>
 * Armour re-uses the dragon's own geometry -- a different texture over the same shape, like
 * vanilla horse armour -- so each pass is a plain full-model re-render, with none of the per-bone
 * isolation {@link DragonTackLayer} needs for the saddle and chest.
 * <p>
 * A dyeable skin takes two passes: the greyscale base multiplied by the stack's dye, then the
 * overlay on top with no tint. This is what {@code HorseArmorLayer} does for leather horse
 * armour, except vanilla has only the one texture and so tints the straps along with everything
 * else. Splitting base and overlay is what keeps the straps their own colour here.
 *
 * @see DragonTackLayer
 */
public class DragonArmorLayer extends GeoRenderLayer<TameableDragonEntity> {
    /// GeckoLib reads the colour argument as ARGB; -1 is opaque white, i.e. the texture as-is.
    private static final int NO_TINT = -1;

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

        var appearance = dragon.getVariant().appearance;
        if (appearance == null) return;

        DragonArmorSkin skin = appearance.getArmorSkin(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (skin == null) return;

        ResourceLocation overlay = skin.overlay();
        if (overlay == null) {
            this.pass(skin.base(), NO_TINT, bakedModel, poseStack, bufferSource, dragon, partialTick, packedLight);
            return;
        }
        this.pass(skin.base(), dyeOf(stack), bakedModel, poseStack, bufferSource, dragon, partialTick, packedLight);
        this.pass(overlay, NO_TINT, bakedModel, poseStack, bufferSource, dragon, partialTick, packedLight);
    }

    /**
     * The colour to multiply a dyeable base by.
     * <p>
     * {@code opaque()} is not optional. A dye component carries plain RGB, so its alpha byte is
     * zero, and GeckoLib hands the colour straight to {@code VertexConsumer#setColor}, which reads
     * it as ARGB -- an alpha of zero renders the whole pass invisible rather than untinted.
     */
    private static int dyeOf(ItemStack stack) {
        return FastColor.ARGB32.opaque(DyedItemColor.getOrDefault(stack, DMItems.UNDYED_LEATHER));
    }

    private void pass(ResourceLocation texture, int tint, BakedGeoModel bakedModel, PoseStack poseStack,
                      MultiBufferSource bufferSource, TameableDragonEntity dragon, float partialTick, int packedLight) {
        RenderType type = RenderType.entityCutoutNoCull(texture);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, dragon, type,
                bufferSource.getBuffer(type), partialTick, packedLight, OverlayTexture.NO_OVERLAY, tint);
    }
}
