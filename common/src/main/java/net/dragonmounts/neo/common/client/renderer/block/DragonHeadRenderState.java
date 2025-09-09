package net.dragonmounts.neo.common.client.renderer.block;

import net.dragonmounts.neo.common.client.variant.VariantAppearance;
import org.jetbrains.annotations.Nullable;

public interface DragonHeadRenderState {
    default void neodragonmounts$setAppearance(@Nullable VariantAppearance appearance) {}

    default @Nullable VariantAppearance neodragonmounts$getAppearance() {
        return null;
    }
}
