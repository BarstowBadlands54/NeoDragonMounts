package net.dragonmounts.neo.common.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;

public class DragonCoreModel extends Model {
    public final ModelPart lid;
    public final ModelPart base;

    public DragonCoreModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.lid = root.getChild("lid");
        this.base = root.getChild("base");
    }

    public void animate(float progress) {
        this.lid.setPos(0.0F, 24.0F - progress * 0.5F * 16.0F, 0.0F);
        this.lid.yRot = 270.0F * progress * (float) (Math.PI / 180.0);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.base.render(poseStack, buffer, packedLight, packedOverlay, color);   // ← actually draw
        this.lid.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}