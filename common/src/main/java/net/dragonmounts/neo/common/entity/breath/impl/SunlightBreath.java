package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.LightningProfile;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;

/// Orange arc, the mirror of {@link MoonlightBreath}.
public class SunlightBreath extends LightBreath {
    public SunlightBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public LightningProfile getLightningProfile() {
        return LightningProfile.SUNLIGHT;
    }
}
