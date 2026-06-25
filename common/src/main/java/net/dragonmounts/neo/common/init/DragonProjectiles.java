package net.dragonmounts.neo.common.init;

import net.dragonmounts.neo.common.entity.projectile.ability.*;

public final class DragonProjectiles {
    // minChargeTicks = how long G must be held; cooldownTicks = anti-spam after firing
    public static final DragonProjectileAbility NETHER_FIREBALL =
            new FireballProjectileAbility(/*hold*/ 20, /*cooldown*/ 30, /*explosionPower*/ 5);
    public static final DragonProjectileAbility DRAGON_FIREBALL =
            new FireballProjectileAbility(/*hold*/ 20, /*cooldown*/ 30, /*explosionPower*/ 3);
    public static final DragonProjectileAbility ENDER_CHARGE =
            new EnderChargeProjectileAbility(/*hold*/ 15, /*cooldown*/ 30);

    private DragonProjectiles() {}
    public static void init() {}
}