package net.dragonmounts.neo.common.client;

import net.dragonmounts.neo.common.item.DragonSpawnEggItem;
import net.dragonmounts.neo.common.init.DragonTypes;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Spawn-egg tint per dragon breed.
 * <p>
 * Vanilla registers spawn-egg colours by walking {@code SpawnEggItem.eggs()}, keyed by
 * EntityType. Every dragon egg shares one entity type, so at most one of them could ever get a
 * colour handler that way; the tint has to come from the stack's DRAGON_TYPE component instead.
 */
public final class DMSpawnEggColors {
    /**
     * ARGB, not RGB: ItemRenderer pulls the alpha byte out with FastColor.ARGB32.alpha(), so a
     * plain 0xRRGGBB has alpha 0 and renders fully transparent. Hence the 0xFF top byte on every
     * value here.
     */
    private static final Map<DragonType, int[]> COLORS = new IdentityHashMap<>(24);

    private static void put(DragonType type, int background, int highlight) {
        COLORS.put(type, new int[]{background, highlight});
    }

    static {
        put(DragonTypes.AETHER,    0xFF4A90D9, 0xFFF5D742);   // blue / yellow
        put(DragonTypes.DARK,      0xFF141414, 0xFFC1272D);   // black / red
        put(DragonTypes.ENCHANTED, 0xFF8E44AD, 0xFFFFFFFF);   // purple / white
        put(DragonTypes.ENDER,     0xFF0F0F14, 0xFFA050C8);   // black / purple
        put(DragonTypes.FIRE,      0xFFD32F2F, 0xFFFFC107);   // red / yellow
        put(DragonTypes.FOREST,    0xFF1F6B2E, 0xFF6ECB63);   // dark green / light green
        put(DragonTypes.ICE,       0xFF5BC8F5, 0xFFFFFFFF);   // blue / white
        put(DragonTypes.LIGHT,     0xFFFFE45C, 0xFFFFFFFF);   // yellow / white
        put(DragonTypes.MOONLIGHT, 0xFF7B5CB8, 0xFFFFFFFF);   // purple / white
        put(DragonTypes.NETHER,    0xFF8B1A1A, 0xFFE85D2A);   // crimson / ember  (not in your list -- adjust if wrong)
        put(DragonTypes.SCULK,     0xFF0A4A5A, 0xFF8A9A9E);   // warden blue / grey
        put(DragonTypes.SKELETON,  0xFFE8E8E8, 0xFF9A9A9A);   // white / grey
        put(DragonTypes.STORM,     0xFF3A3F45, 0xFF4A90D9);   // dark grey / blue
        put(DragonTypes.SUNLIGHT,  0xFFF57C00, 0xFFFFD54F);   // orange / yellow
        put(DragonTypes.TERRA,     0xFF7B5230, 0xFF8E5BA8);   // brown / purple
        put(DragonTypes.WATER,     0xFF2E7FD4, 0xFFFFFFFF);   // blue / white
        put(DragonTypes.WITHER,    0xFF0D0D0D, 0xFF4A3520);   // black / brown
        put(DragonTypes.ZOMBIE,    0xFF2E5D34, 0xFF3F7CC4);   // dark green / blue
    }

    /**
     * @param tintIndex 0 for the egg base, 1 for the spots -- the two layers of
     *                  minecraft:item/template_spawn_egg
     * @return packed RGB, or white when the stack carries no known dragon type
     */
    public static int getColor(ItemStack stack, int tintIndex) {
        // Read the breed off the ITEM, not the stack. Unlike the egg block item -- which gets a
        // DRAGON_TYPE component from makeDragonEggBlock -- makeDragonSpawnEgg attaches no
        // component at all; DragonSpawnEggItem keeps the breed in a final field. Looking at the
        // stack's components therefore always came back null and every egg rendered white.
        if (!(stack.getItem() instanceof DragonSpawnEggItem egg)) return 0xFFFFFFFF;
        int[] pair = COLORS.get(egg.type);
        if (pair == null) return 0xFFFFFFFF;
        return pair[tintIndex == 0 ? 0 : 1];
    }

    private DMSpawnEggColors() {}
}
