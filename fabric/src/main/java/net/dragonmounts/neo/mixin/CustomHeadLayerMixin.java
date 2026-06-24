package net.dragonmounts.neo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.client.renderer.item.DragonHeadItemRenderer;
import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
    @Shadow @Final private float scaleX;
    @Shadow @Final private float scaleY;
    @Shadow @Final private float scaleZ;

    // shared renderer instance for worn heads
    private static final DragonHeadItemRenderer neodragonmounts$WORN = new DragonHeadItemRenderer();

    private CustomHeadLayerMixin(RenderLayerParent<T, M> renderer) { super(renderer); }

    @Inject(
            at = @At("HEAD"),
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            cancellable = true
    )
    public void neodragonmounts$renderDragonHead(
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo info) {
        ItemStack worn = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(worn.getItem() instanceof DragonHeadItem)) return;

        poseStack.pushPose();
        poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
        // position at the head bone (same as vanilla skull path)
        this.getParentModel().getHead().translateAndRotate(poseStack);
        // skull-standard orientation/scale, then center
        poseStack.scale(1.1875F, -1.1875F, -1.1875F);
        poseStack.translate(-0.5, 0.0, -0.5);
        // tweak to cover/lower the head:
        poseStack.translate(0.0, neodragonmounts$WORN_Y_OFFSET, 0.0);
        poseStack.scale(neodragonmounts$WORN_SCALE, neodragonmounts$WORN_SCALE, neodragonmounts$WORN_SCALE);

        neodragonmounts$WORN.renderByItem(worn, ItemDisplayContext.HEAD, poseStack, buffer,
                packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        info.cancel();
    }

    // ---- tune these two for "lower" + "larger to cover the head" ----
    private static final float neodragonmounts$WORN_SCALE = 1.4F;     // >1 = larger
    private static final float neodragonmounts$WORN_Y_OFFSET = 0.0F;  // negative = lower onto head
}