package net.dragonmounts.neo.common.client.model.block;

import net.dragonmounts.neo.common.block.DragonHeadBlock;
import net.dragonmounts.neo.common.block.entity.DragonHeadBlockEntity;
import net.dragonmounts.neo.common.init.DragonVariants;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/** GeoModel for the placed dragon-head block entity. Resolves geo/texture from the block's variant. */
public class DragonHeadBlockGeoModel extends GeoModel<DragonHeadBlockEntity> {
    private static final ResourceLocation ANIMATIONS = makeId("animations/entity/dragon/dragonmounts2.dragon.animation.json");

    private static DragonVariant variantOf(DragonHeadBlockEntity be) {
        return be.getBlockState().getBlock() instanceof DragonHeadBlock head ? head.variant : DragonVariants.ENDER_JEAN;
    }

    @Override
    public ResourceLocation getModelResource(DragonHeadBlockEntity be) {
        var variant = variantOf(be);
        var appearance = variant.appearance;
        if (appearance != null) {
            var override = appearance.getHeadGeoModel();
            if (override != null) return override;
        }
        return variant.type.headGeoModel();
    }

    @Override
    public ResourceLocation getTextureResource(DragonHeadBlockEntity be) {
        return variantOf(be).appearance.getBodyTexture(null);
    }

    @Override
    public ResourceLocation getAnimationResource(DragonHeadBlockEntity be) {
        return ANIMATIONS;
    }
}