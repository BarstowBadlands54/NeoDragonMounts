package net.dragonmounts.neo.mixin;

import net.dragonmounts.neo.common.client.renderer.block.DragonHeadRenderState;
import net.dragonmounts.neo.common.client.variant.VariantAppearance;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements DragonHeadRenderState {
    @Unique
    private VariantAppearance neodragonmounts$headAppearance;

    @Override
    public void neodragonmounts$setAppearance(VariantAppearance appearance) {
        this.neodragonmounts$headAppearance = appearance;
    }

    @Override
    public @Nullable VariantAppearance neodragonmounts$getAppearance() {
        return this.neodragonmounts$headAppearance;
    }

    private LivingEntityRenderStateMixin() {}
}
