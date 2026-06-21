package net.dragonmounts.neo.common.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dragonmounts.neo.common.block.entity.DragonCoreBlockEntity;
import net.dragonmounts.neo.common.client.model.DragonCoreModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/**
 * @see net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer
 */
public class DragonCoreRenderer implements BlockEntityRenderer<DragonCoreBlockEntity> {
    private static final ResourceLocation TEXTURE_LOCATION = makeId("textures/block/dragon_core.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE_LOCATION);
    private final DragonCoreModel model;

    public DragonCoreRenderer(BlockEntityRendererProvider.Context context) {
        this(context.getModelSet());
    }

    public DragonCoreRenderer(EntityModelSet models) {
        this.model = new DragonCoreModel(models.bakeLayer(ModelLayers.SHULKER));
    }

    @Override
    public void render(DragonCoreBlockEntity core, float ticks, PoseStack matrices, MultiBufferSource buffers, int light, int overlay) {
        this.render(
                matrices, buffers, light, overlay,
                // 1.21.1: no getValueOrElse on StateHolder; use getOptionalValue(...).orElse(...)
                core.getBlockState().getOptionalValue(HORIZONTAL_FACING).orElse(Direction.SOUTH),
                core.getProgress(ticks)
        );
    }

    public void render(PoseStack matrices, MultiBufferSource buffers, int light, int overlay, Direction facing, float progress) {
        matrices.pushPose();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.scale(0.9995F, 0.9995F, 0.9995F);
        matrices.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        matrices.scale(1.0F, -1.0F, -1.0F);
        matrices.translate(0.0F, -1.0F, 0.0F);
        this.model.animate(progress);
        this.model.renderToBuffer(matrices, buffers.getBuffer(RENDER_TYPE), light, overlay, -1);
        matrices.popPose();
    }
}