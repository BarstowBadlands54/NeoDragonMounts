package net.dragonmounts.neo.common.client.renderer.item;

import net.dragonmounts.neo.common.client.model.block.DragonHeadItemGeoModel;
import net.dragonmounts.neo.common.item.DragonHeadItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GeckoLib 4 item renderer (a BlockEntityWithoutLevelRenderer) for the dragon-head item in
 * GUI/hand. Replaces the 1.21.2+ SpecialModelRenderer Special/Unbaked records.
 * Wire per-loader via GeoRenderProvider.getGeoItemRenderer() in the NeoForge/Fabric modules.
 */
public class DragonHeadItemRenderer extends GeoItemRenderer<DragonHeadItem> {
    public DragonHeadItemRenderer() {
        super(new DragonHeadItemGeoModel());
    }
}