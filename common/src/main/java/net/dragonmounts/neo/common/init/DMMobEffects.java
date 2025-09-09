package net.dragonmounts.neo.common.init;

import net.dragonmounts.neo.common.effect.DarkDragonsGraceEffect;
import net.dragonmounts.neo.compat.registry.EffectHolder;
import net.minecraft.world.effect.MobEffectCategory;

import static net.dragonmounts.neo.compat.registry.EffectHolder.registerMobEffect;

public class DMMobEffects {
    public static final EffectHolder<DarkDragonsGraceEffect> DARK_DRAGONS_GRACE = registerMobEffect(
            "dark_dragons_grace",
            new DarkDragonsGraceEffect(MobEffectCategory.BENEFICIAL, 0x6908265)
    );

    public static void init() {}
}
