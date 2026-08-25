package net.dragonmounts.neo.common.entity.breath;

/**
 * Everything that distinguishes one lightning breed from another. Breeds differ only in these
 * numbers, so they hold a constant rather than overriding a getter apiece.
 *
 * @param color               arc tint, 0xRRGGBB
 * @param bolts               arcs drawn at once
 * @param igniteTicks         how long a struck target burns for
 * @param wetDamageMultiplier a wet target takes this much more damage, but is extinguished on its
 *                            next tick -- soaking trades the burn for a bigger hit rather than
 *                            escaping both
 */
public record LightningProfile(int color, int bolts, int igniteTicks, float wetDamageMultiplier) {
    public static final LightningProfile STORM = new LightningProfile(0xE8F2FF, 3, 100, 1.5F);
    public static final LightningProfile MOONLIGHT = new LightningProfile(0x4C7BFF, 2, 100, 1.5F);
    public static final LightningProfile SUNLIGHT = new LightningProfile(0xFF8A1E, 2, 100, 1.5F);

    public float red() {
        return ((this.color >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((this.color >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (this.color & 0xFF) / 255.0F;
    }
}
