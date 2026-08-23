package net.dragonmounts.neo.common.client.renderer.armor;

import net.dragonmounts.neo.common.client.model.armor.DragonScaleArmorGeoModel;
import net.dragonmounts.neo.common.item.DragonScaleArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/// Bone lookups are left at GeoArmorRenderer's defaults -- armorHead, armorBody, armorRightArm,
/// armorLeftArm, armorRightLeg, armorLeftLeg, armorRightBoot, armorLeftBoot -- which is what the
/// geo files in assets/neodragonmounts/geo/armor/ are named for.
///
/// Nothing needs overriding for capes, horns or wings: extra bones parented under one of those
/// eight inherit the vanilla ModelPart transform through the bone hierarchy, and GeoBone#setHidden
/// propagates to children, so they hide with their parent too.
public class DragonScaleArmorRenderer extends GeoArmorRenderer<DragonScaleArmorItem> {
    public DragonScaleArmorRenderer() {
        super(new DragonScaleArmorGeoModel());
    }
}
