package net.dragonmounts.neo.common.client.variant;

import net.minecraft.resources.ResourceLocation;
import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * The distinct dragon body geometries, each backed by one Bedrock .geo.json
 * (formerly the BuiltinFactory cube models). A breed picks ONE shape; its
 * per-breed texture is carried separately on the VariantAppearance.
 */
public enum DragonShape {
    NORMAL("normal"),
    SKELETON("skeleton"),
    SCULK("sculk"),
    SCALE_SHARPENED("scale_sharpened"),
    TAIL_HORNED("tail_horned"),
    TAIL_SCALE_INCLINED("tail_scale_inclined"),
    SPIKED_HORNED("spiked_horned"),
    WINGED_HORNED("winged_horned"),
    BLAND("bland"),
    HORNED_ANTLERS("horned_antlers");

    public final ResourceLocation geo;

    DragonShape(String name) {
        // -> assets/neodragonmounts/geo/entity/dragon/<name>.geo.json
        this.geo = makeId("geo/entity/dragon/" + name + ".geo.json");
    }
}