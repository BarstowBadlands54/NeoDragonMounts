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
import net.minecraft.client.renderer.texture.OverlayTexture;
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

    private static final DragonHeadItemRenderer neodragonmounts$WORN = new DragonHeadItemRenderer();

    // Vanilla skull scale is 1.1875. Geo head model origin differs, so these compensate.
    private static final float neodragonmounts$VANILLA_SKULL_SCALE = 1.1875F;
    private static final float neodragonmounts$WORN_Y_OFFSET = -0.5F;   // lower onto the head
    private static final float neodragonmounts$WORN_Z_OFFSET = 0.0F;// tune: centers front/back

    private CustomHeadLayerMixin(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Inject(
            at = @At("HEAD"),
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            cancellable = true
    )
    public void neodragonmounts$renderDragonHead(
            PoseStack matrices, MultiBufferSource buffers, int light, T livingEntity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch, CallbackInfo info) {
        ItemStack worn = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
        if (!(worn.getItem() instanceof DragonHeadItem)) return;

        matrices.pushPose();
        matrices.scale(this.scaleX, this.scaleY, this.scaleZ);
        this.getParentModel().getHead().translateAndRotate(matrices);

        // EXACT vanilla skull scale (1.1875, -1.1875, -1.1875) — matches vanilla head size
        matrices.scale(
                neodragonmounts$VANILLA_SKULL_SCALE,
                -neodragonmounts$VANILLA_SKULL_SCALE,
                neodragonmounts$VANILLA_SKULL_SCALE     // ← removed the minus (was -...)
        );
        matrices.translate(-0.5, 0.0, -0.5);
        matrices.translate(0.0, neodragonmounts$WORN_Y_OFFSET, neodragonmounts$WORN_Z_OFFSET);

        neodragonmounts$WORN.renderByItem(worn, ItemDisplayContext.HEAD, matrices, buffers,
                light, OverlayTexture.NO_OVERLAY);

        matrices.popPose();
        info.cancel();
    }
}