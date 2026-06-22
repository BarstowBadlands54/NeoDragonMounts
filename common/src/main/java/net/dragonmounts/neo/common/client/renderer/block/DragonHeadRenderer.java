package net.dragonmounts.neo.common.client.renderer.block;

import net.dragonmounts.neo.common.block.entity.DragonHeadBlockEntity;
import net.dragonmounts.neo.common.client.model.block.DragonHeadBlockGeoModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * GeckoLib 4 block-entity renderer for the dragon head. GeoBlockRenderer already implements
 * BlockEntityRenderer<T> and orients the model from the block's HORIZONTAL_FACING via
 * getFacing()/rotateBlock(), so the wall-mount facing the old renderer did by hand is handled.
 * Register with: BlockEntityRenderers.register(DMBlockEntities.DRAGON_HEAD.get(), DragonHeadRenderer::new);
 */
public class DragonHeadRenderer extends GeoBlockRenderer<DragonHeadBlockEntity> {
    public DragonHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(new DragonHeadBlockGeoModel());
        // If the head renders too large/small or the standing form needs the 16-step rotation,
        // tune here: withScale(0.75F) for the old 0.75 scale, and/or override getFacing(...).
    }
}