package net.dragonmounts.neo.common.client.renderer.egg;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity;
import net.dragonmounts.neo.common.init.DMBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import org.joml.Quaternionf;

import static net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity.EGG_CRACK_THRESHOLD;
import static net.dragonmounts.neo.common.entity.dragon.HatchableDragonEggEntity.MIN_HATCHING_TIME;
import static net.dragonmounts.neo.common.util.math.MathUtil.HALF_RAD_FACTOR;
import static net.minecraft.client.renderer.ItemBlockRenderTypes.getMovingBlockRenderType;

/**
 * @see net.minecraft.client.renderer.entity.FallingBlockRenderer
 */
public class DragonEggRenderer extends EntityRenderer<HatchableDragonEggEntity> {
    protected final BlockRenderDispatcher dispatcher;

    public DragonEggRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(HatchableDragonEggEntity egg, float entityYaw, float partialTicks, PoseStack matrices, MultiBufferSource buffers, int light) {
        var block = egg.asBlock(DMBlocks.ENDER_DRAGON_EGG.get()).defaultBlockState();
        if (block.getRenderShape() != RenderShape.MODEL) return;
        var pos = BlockPos.containing(egg.getX(), egg.getBoundingBox().maxY, egg.getZ());
        var level = egg.level();
        var random = level.random;
        int age = egg.getAge();
        float amplitude = egg.getAmplitude(partialTicks);
        var renderer = this.dispatcher.getModelRenderer();
        var model = this.dispatcher.getBlockModel(block);
        long seed = block.getSeed(pos);
        matrices.pushPose();
        matrices.translate(-0.5, 0.0, -0.5);
        if (amplitude != 0) {
            float axis = egg.getRotationAxis();
            amplitude *= HALF_RAD_FACTOR;
            float sin = Mth.sin(amplitude);
            matrices.mulPose(new Quaternionf(
                    Mth.cos(axis) * sin,
                    0.0F,
                    Mth.sin(axis) * sin,
                    Mth.cos(amplitude)
            ));
        }
        renderer.tesselateBlock(level, model, block, pos, matrices, buffers.getBuffer(getMovingBlockRenderType(block)), false, random, seed, OverlayTexture.NO_OVERLAY);
        if (age >= EGG_CRACK_THRESHOLD) {
            renderer.tesselateBlock(level, model, block, pos, matrices, new SheetedDecalTextureGenerator(buffers.getBuffer(
                    ModelBakery.DESTROY_TYPES.get(Math.min((age - EGG_CRACK_THRESHOLD) * 90 / MIN_HATCHING_TIME, 9))
            ), matrices.last(), 1.0F), false, random, seed, OverlayTexture.NO_OVERLAY);
        }
        super.render(egg, entityYaw, partialTicks, matrices, buffers, light);
        matrices.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(HatchableDragonEggEntity egg) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}