package net.dragonmounts.neo.common.client;

import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.common.init.DragonTypes;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Spawn-egg tint per dragon breed.
 * <p>
 * DragonSpawnEggItem passes 0xFFFFFF, 0xFFFFFF to SpawnEggItem, which is why every egg renders
 * white. Changing those constructor arguments alone is not enough: vanilla registers spawn-egg
 * colours by walking SpawnEggItem.eggs(), and that map is keyed by EntityType -- all 18 dragon
 * eggs share one entity type, so at most one of them would ever receive a colour handler. The
 * tint therefore has to come from the stack's DRAGON_TYPE component, which is what getColor does.
 * <p>
 * Colours were sampled from the breeds' own body textures: every variant of a breed pooled
 * together, the darkest 45% of pixels dropped so outlines and shading do not drag the result
 * toward black, then the dominant hue cluster of what remains. The highlight is that colour
 * lifted in value and eased off in saturation, matching how vanilla eggs read.
 */
public final class DMSpawnEggColors {
    private static final Map<DragonType, int[]> COLORS = new IdentityHashMap<>(24);

    private static void put(DragonType type, int background, int highlight) {
        COLORS.put(type, new int[]{background, highlight});
    }

    static {
        put(DragonTypes.AETHER, 0x94B7CB, 0xCBECFF);
        put(DragonTypes.DARK, 0x585352, 0x9E9796);
        put(DragonTypes.ENCHANTED, 0x946298, 0xF6B8FB);
        put(DragonTypes.ENDER, 0x393939, 0x717171);
        put(DragonTypes.FIRE, 0x865B32, 0xE1AB77);
        put(DragonTypes.FOREST, 0x6E6C43, 0xBEBC86);
        put(DragonTypes.ICE, 0xAFBAC5, 0xEAF4FF);
        put(DragonTypes.LIGHT, 0x9AE5EB, 0xBDFAFF);
        put(DragonTypes.MOONLIGHT, 0x17192A, 0x3C405B);
        put(DragonTypes.NETHER, 0x454245, 0x837E83);
        put(DragonTypes.SCULK, 0xBCB5B4, 0xFFF8F7);
        put(DragonTypes.SKELETON, 0xBEB5AE, 0xFFF6EF);
        put(DragonTypes.STORM, 0x663F2E, 0xB27F69);
        put(DragonTypes.SUNLIGHT, 0xD4A540, 0xFFD579);
        put(DragonTypes.TERRA, 0x7F624D, 0xD7B297);
        put(DragonTypes.WATER, 0x6A9D9F, 0xBFFDFF);
        put(DragonTypes.WITHER, 0x151518, 0x3B3B41);
        put(DragonTypes.ZOMBIE, 0x6A5C43, 0xB8A685);
    }

    /**
     * @param tintIndex 0 for the egg base, 1 for the spots -- the two layers of
     *                  minecraft:item/template_spawn_egg
     * @return packed RGB, or white when the stack carries no known dragon type
     */
    public static int getColor(ItemStack stack, int tintIndex) {
        var type = stack.get(DMDataComponents.DRAGON_TYPE);
        if (type == null) return 0xFFFFFF;
        int[] pair = COLORS.get(type);
        if (pair == null) return 0xFFFFFF;
        return pair[tintIndex == 0 ? 0 : 1];
    }

    private DMSpawnEggColors() {}
}
