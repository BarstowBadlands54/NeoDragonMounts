package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;


public class MoonlightBreath extends LightBreath {
    public MoonlightBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public int getLightningColor() {
        return MOONLIGHT_COLOR;
    }
}
