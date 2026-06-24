package net.dragonmounts.neo.common.client.renderer.dragon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DragonTackLayer extends GeoRenderLayer<TameableDragonEntity> {
    private static final ResourceLocation SADDLE = makeId("textures/entity/dragon/saddle.png");
    private static final ResourceLocation CHEST  = makeId("textures/entity/dragon/chest.png");

    public DragonTackLayer(GeoRenderer<TameableDragonEntity> renderer) {
        super(renderer);
    }

    private void overlay(ResourceLocation texture, RenderType rt, BakedGeoModel model, PoseStack poseStack,
                         MultiBufferSource bufferSource, TameableDragonEntity dragon,
                         float partialTick, int packedLight) {
        getRenderer().reRender(model, poseStack, bufferSource, dragon, rt,
                bufferSource.getBuffer(rt), partialTick, packedLight, OverlayTexture.NO_OVERLAY, -1);
    }

    @Override
    public void render(PoseStack poseStack, TameableDragonEntity dragon, BakedGeoModel bakedModel,
                       @Nullable RenderType renderType, MultiBufferSource bufferSource,
                       @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (dragon.isInvisible() || dragon.deathTime > 0) return;

        if (dragon.isSaddled()) {
            overlay(SADDLE, RenderType.entityCutoutNoCull(SADDLE), bakedModel, poseStack, bufferSource, dragon, partialTick, packedLight);
        }
        if (dragon.hasChest()) {
            overlay(CHEST, RenderType.entityCutoutNoCull(CHEST), bakedModel, poseStack, bufferSource, dragon, partialTick, packedLight);
        }

        ItemStack armor = dragon.inventory.armor.get();
        if (armor != null && !armor.isEmpty()) {
            ResourceLocation assetId = BuiltInRegistries.ITEM.getKey(armor.getItem());   // e.g. neodragonmounts:copper_dragon_armor
            ResourceLocation tex = dragon.getVariant().appearance.getArmorTexture(assetId);
            if (tex != null) {
                RenderType rt = RenderType.armorCutoutNoCull(tex);
                VertexConsumer vc = ItemRenderer.getArmorFoilBuffer(bufferSource, rt, armor.hasFoil());
                getRenderer().reRender(bakedModel, poseStack, bufferSource, dragon, rt, vc,
                        partialTick, packedLight, OverlayTexture.NO_OVERLAY, -1);
            }
        }
    }
}