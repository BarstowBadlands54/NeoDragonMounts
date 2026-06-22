//package net.dragonmounts.neo.mixin;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.dragonmounts.neo.common.item.DragonHeadItem;
//import net.minecraft.client.model.EntityModel;
//import net.minecraft.client.model.HeadedModel;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.entity.RenderLayerParent;
//import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
//import net.minecraft.client.renderer.entity.layers.RenderLayer;
//import net.minecraft.client.renderer.texture.OverlayTexture;
//import net.minecraft.world.entity.EquipmentSlot;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.item.ItemStack;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import static net.dragonmounts.neo.common.client.renderer.block.DragonHeadRenderer.renderHead;
//
///// 1.21.1 port: there is no LivingEntityRenderState, so this single mixin replaces the old
///// trio (LivingEntityRenderStateMixin + LivingEntityRendererMixin + CustomHeadLayerMixin).
///// The worn DragonHeadItem is read live from the entity's HEAD slot, and the head walk
///// animation position is taken from the entity's walkAnimation (the vanilla skull path does
///// the same).
//@Mixin(CustomHeadLayer.class)
//public abstract class CustomHeadLayerMixin<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
//    @Shadow
//    @Final
//    private float scaleX;
//    @Shadow
//    @Final
//    private float scaleY;
//    @Shadow
//    @Final
//    private float scaleZ;
//
//    @Inject(
//            at = @At("HEAD"),
//            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
//            cancellable = true
//    )
//    public void renderDragonHead(
//            PoseStack matrices,
//            MultiBufferSource buffers,
//            int light,
//            T livingEntity,
//            float limbSwing,
//            float limbSwingAmount,
//            float partialTicks,
//            float ageInTicks,
//            float netHeadYaw,
//            float headPitch,
//            CallbackInfo info
//    ) {
//        ItemStack worn = livingEntity.getItemBySlot(EquipmentSlot.HEAD);
//        if (!(worn.getItem() instanceof DragonHeadItem head)) return;
//        var appearance = head.variant.appearance;
//        if (appearance == null) return;
//
//        // mirror the vanilla skull-block placement path (CustomHeadLayer#render in 1.21.1)
//        matrices.pushPose();
//        matrices.scale(this.scaleX, this.scaleY, this.scaleZ);
//        this.getParentModel().getHead().translateAndRotate(matrices);
//        matrices.scale(1.1875F, -1.1875F, -1.1875F);
//        matrices.translate(-0.5, 0.0, -0.5);
//
//        // 1.21.4 read this from the render state (wornHeadAnimationPos); 1.21.1 reads it live.
//        float walkPos = livingEntity.walkAnimation.position(partialTicks);
//        var model = appearance.getModel();
//        model.setupBlock(walkPos, 180.0F, 0.75F);
//        renderHead(model.head, appearance, matrices, buffers, 0.5, 0.0, 0.5, light, OverlayTexture.NO_OVERLAY);
//
//        matrices.popPose();
//        info.cancel();
//    }
//
//    private CustomHeadLayerMixin(RenderLayerParent<T, M> renderer) {
//        super(renderer);
//    }
//}