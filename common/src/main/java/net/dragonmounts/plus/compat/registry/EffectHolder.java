package net.dragonmounts.plus.compat.registry;

import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;

@SuppressWarnings("unused")
public class EffectHolder<T extends MobEffect> extends AbstractHolder<T, MobEffect> {
    public static <T extends MobEffect> EffectHolder<T> registerMobEffect(String name, T effect) {
        return Dummy.get();
    }

    public EffectHolder(ResourceKey<MobEffect> key, T effect) {
        super(key);
    }
}
