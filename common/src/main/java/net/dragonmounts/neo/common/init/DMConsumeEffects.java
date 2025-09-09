package net.dragonmounts.neo.common.init;

import net.dragonmounts.neo.common.component.impl.ContorlGrowthConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

import static net.dragonmounts.neo.compat.registry.RegistryHandler.registerConsumeEffect;

public class DMConsumeEffects {
    public static final ConsumeEffect.Type<ContorlGrowthConsumeEffect> CONTROL_GROWTH = registerConsumeEffect(
            "control_growth",
            ContorlGrowthConsumeEffect.CODEC,
            ContorlGrowthConsumeEffect.STREAM_CODEC
    );

    public void init() {}
}
