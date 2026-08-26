package net.dragonmounts.neo.common.client.variant;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The textures one armour material wears on one dragon body.
 * <p>
 * A skin with an overlay is dyeable: its base is a greyscale mask multiplied by the stack's
 * {@code DYED_COLOR} and the overlay is drawn untinted on top, the same two layers vanilla
 * leather armour uses. A skin without one is a finished texture and renders untinted.
 *
 * @param base    full-body texture, drawn first
 * @param overlay drawn over the base without a tint, or null when the material has none
 */
public record DragonArmorSkin(ResourceLocation base, @Nullable ResourceLocation overlay) {
    /// A finished texture with nothing to tint.
    public static DragonArmorSkin solid(ResourceLocation base) {
        return new DragonArmorSkin(base, null);
    }

    /// A greyscale mask plus the untinted detail that sits over it.
    public static DragonArmorSkin dyeable(ResourceLocation base, ResourceLocation overlay) {
        return new DragonArmorSkin(base, overlay);
    }

    public boolean isDyeable() {
        return this.overlay != null;
    }
}
