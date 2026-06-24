package net.dragonmounts.neo.common.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.dragonmounts.neo.common.block.entity.DragonHeadBlockEntity;
import net.dragonmounts.neo.common.client.model.block.DragonHeadBlockGeoModel;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DragonHeadRenderer extends GeoBlockRenderer<DragonHeadBlockEntity> {
    public DragonHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(new DragonHeadBlockGeoModel());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        DragonHeadBlockEntity be = this.animatable;
        if (be == null) {
            super.rotateBlock(facing, poseStack);
            return;
        }
        BlockState state = be.getBlockState();
        // Standing head: 16-step rotation from ROTATION_16 (+180 to flip geo model's facing)
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            float yRot = -state.getValue(BlockStateProperties.ROTATION_16) * 22.5F;
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            return;
        }
        // Wall head: face the mounted direction (+180 to flip geo model's facing)
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            float yRot = -state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot() + 180.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            return;
        }
        super.rotateBlock(facing, poseStack);
    }
}