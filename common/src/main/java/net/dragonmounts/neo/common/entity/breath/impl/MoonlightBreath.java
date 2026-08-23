package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;

/// Blue arc. Behaviour lives in {@link LightBreath}; only the tint is breed-specific.
public class MoonlightBreath extends LightBreath {
    public MoonlightBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public int getLightningColor() {
        return MOONLIGHT_COLOR;
    }
}
