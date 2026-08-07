package net.dragonmounts.neo.common.init;

import com.google.common.collect.ImmutableList;
import net.dragonmounts.neo.common.block.DragonHeadStandingBlock;
import net.dragonmounts.neo.common.block.DragonHeadWallBlock;
import net.dragonmounts.neo.common.client.variant.VariantAppearance;
import net.dragonmounts.neo.common.client.variant.VariantAppearances;
import net.dragonmounts.neo.common.entity.projectile.ability.DragonProjectileAbility;
import net.dragonmounts.neo.common.item.DragonHeadItem;
import net.dragonmounts.neo.common.util.DragonHead;
import net.dragonmounts.neo.compat.platform.PlatformCompat;
import net.dragonmounts.neo.compat.registry.BlockHolder;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.dragonmounts.neo.compat.registry.ItemHolder;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
import static net.dragonmounts.neo.common.init.DMBlocks.configureDragonHead;
import static net.dragonmounts.neo.compat.registry.BlockHolder.registerBlock;

public class DragonVariants {
    public static final ImmutableList<DragonVariant> BUILTIN_VALUES;
    public static final DragonVariant AETHRA;
    public static final DragonVariant WIND;
    public static final DragonVariant BREEZE;
    public static final DragonVariant BLOODMOON;
    public static final DragonVariant DEMON;
    public static final DragonVariant IMP;
    public static final DragonVariant UNDERWORLD;
    public static final DragonVariant FALLEN;
    public static final DragonVariant PRISM;
    public static final DragonVariant RADIANT;
    public static final DragonVariant LIGHT_SUNSET;
    public static final DragonVariant ENCHANTED_ENCHANTING;
    public static final DragonVariant ENCHANTED_SHIMMER;
    public static final DragonVariant ENCHANTED_SPARKLING;
    public static final DragonVariant ENDER_JEAN;
    public static final DragonVariant ENDER_JOHN;
    public static final DragonVariant ENDER_SHADOW;
    public static final DragonVariant BLAZE;
    public static final DragonVariant FLAME;
    public static final DragonVariant BLUE_FIRE;
    public static final DragonVariant NATURE;
    public static final DragonVariant COLD;
    public static final DragonVariant JUNGLE;
    public static final DragonVariant WARM;
    public static final DragonVariant ALPINE;
    public static final DragonVariant ICEBERG;
    public static final DragonVariant FROST;
    public static final DragonVariant STARLIGHT;
    public static final DragonVariant CONSTELLATION;
    public static final DragonVariant ECLIPSE;
    public static final DragonVariant MAGMA;
    public static final DragonVariant VOLCANIC;
    public static final DragonVariant SOUL_FIRE;
    public static final DragonVariant AMETHYST_SCULK;
    public static final DragonVariant BETA_SCULK;
    public static final DragonVariant WARDEN_SCULK;
    public static final DragonVariant SKELETON;
    public static final DragonVariant STRAY;
    public static final DragonVariant BOGGED;
    public static final DragonVariant LIGHTNING_STORM;
    public static final DragonVariant THUNDER_STORM;
    public static final DragonVariant BRONZED_STORM;
    public static final DragonVariant SUNLIGHT_SUNRISE;
    public static final DragonVariant SUNLIGHT_SUNSET;
    public static final DragonVariant SUNLIGHT_AURORA;
    public static final DragonVariant TERRA_VALLEY;
    public static final DragonVariant TERRA_MESA;
    public static final DragonVariant TERRA_CRYSTALLINE;
    public static final DragonVariant WATER_POND;
    public static final DragonVariant WATER_TIDAL;
    public static final DragonVariant WATER_BRINE;
    public static final DragonVariant WITHER;
    public static final DragonVariant ZOMBIE_DROWNED;
    public static final DragonVariant ZOMBIE_HUSK;
    public static final DragonVariant ZOMBIE_NORMAL;
    public static final DragonVariant LEGACY_AETHER;
    public static final DragonVariant LEGACY_ENDER;
    public static final DragonVariant LEGACY_FIRE;
    public static final DragonVariant LEGACY_FOREST;
    public static final DragonVariant LEGACY_ICE;
    public static final DragonVariant LEGACY_NETHER;
    public static final DragonVariant LEGACY_SKELETON;
    public static final DragonVariant LEGACY_WATER;

    static BlockHolder<DragonHeadStandingBlock> registerStandingHead(DragonHead head, String name) {
        return registerBlock(name, props ->
                new DragonHeadStandingBlock(head.variant, configureDragonHead(props))  // no .overrideDescription
        );
    }

    static BlockHolder<DragonHeadWallBlock> registerWallHead(DragonHead head, String name) {
        return registerBlock(name, props -> {
            var standing = head.standing.get();
            return new DragonHeadWallBlock(head.variant, configureDragonHead(props)
                    .dropsLike(standing)            // was .overrideLootTable(standing.getLootTable())
            );                                            // .overrideDescription(...) removed
        });
    }

    static ItemHolder<DragonHeadItem> registerHeadItem(DragonHead head, String name) {
        return DMItemGroups.DRAGON_HEADS.register(name, props -> new DragonHeadItem(
                head.variant,
                head.standing.get(),
                head.wall.get(),
                props.rarity(Rarity.UNCOMMON)            // no .overrideDescription(...)
        ));
    }

    static DragonVariant make(Function<String, VariantAppearance> supplier, DragonType type, String name) {
        return new DragonVariant(type, makeId(name), supplier.apply(name), variant -> {
            var wall = variant.identifier.getPath() + "_dragon_head_wall";
            return new DragonHead(
                    variant,
                    wall.substring(0, wall.length() - 5),
                    wall,
                    DragonVariants::registerStandingHead,
                    DragonVariants::registerWallHead,
                    DragonVariants::registerHeadItem
            );
        });
    }

    static DragonVariant make(Function<String, VariantAppearance> supplier, DragonType type, String name,
                              DragonProjectileAbility projectile) {
        return new DragonVariant(type, makeId(name), supplier.apply(name), projectile, variant -> {
            var wall = variant.identifier.getPath() + "_dragon_head_wall";
            return new DragonHead(
                    variant,
                    wall.substring(0, wall.length() - 5),
                    wall,
                    DragonVariants::registerStandingHead,
                    DragonVariants::registerWallHead,
                    DragonVariants::registerHeadItem
            );
        });
    }

    static {
        Function<String, VariantAppearance> supplier = PlatformCompat.isClientSide()
                ? VariantAppearances.getBuiltinSupplier()
                : ignored -> null;
        var variants = ImmutableList.<DragonVariant>builderWithExpectedSize(63);
        variants.add(AETHRA = make(supplier, DragonTypes.AETHER, "aethra", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(WIND = make(supplier, DragonTypes.AETHER, "wind", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(BREEZE = make(supplier, DragonTypes.AETHER, "breeze", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(BLOODMOON = make(supplier, DragonTypes.DARK, "bloodmoon", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(DEMON = make(supplier, DragonTypes.DARK, "demon", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(IMP = make(supplier, DragonTypes.DARK, "imp", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(UNDERWORLD = make(supplier, DragonTypes.DARK, "underworld", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(FALLEN = make(supplier, DragonTypes.LIGHT, "fallen", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(PRISM = make(supplier, DragonTypes.LIGHT, "prism", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(RADIANT = make(supplier, DragonTypes.LIGHT, "radiant", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(LIGHT_SUNSET = make(supplier, DragonTypes.LIGHT, "light_sunset", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ENCHANTED_ENCHANTING = make(supplier, DragonTypes.ENCHANTED, "enchanting", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ENCHANTED_SHIMMER = make(supplier, DragonTypes.ENCHANTED, "shimmer", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ENCHANTED_SPARKLING = make(supplier, DragonTypes.ENCHANTED, "sparkling", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ENDER_JEAN = make(supplier, DragonTypes.ENDER, "ender_jean", DragonProjectiles.ENDER_CHARGE));
        variants.add(ENDER_JOHN = make(supplier, DragonTypes.ENDER, "ender_john", DragonProjectiles.ENDER_CHARGE));
        variants.add(ENDER_SHADOW = make(supplier, DragonTypes.ENDER, "ender_shadow", DragonProjectiles.ENDER_CHARGE));
        variants.add(BLAZE = make(supplier, DragonTypes.FIRE, "blaze", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(FLAME = make(supplier, DragonTypes.FIRE, "flame", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(BLUE_FIRE = make(supplier, DragonTypes.FIRE, "blue_fire", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(COLD = make(supplier, DragonTypes.FOREST, "cold", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(JUNGLE = make(supplier, DragonTypes.FOREST, "jungle", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(NATURE = make(supplier, DragonTypes.FOREST, "nature", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(WARM = make(supplier, DragonTypes.FOREST, "warm", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ALPINE = make(supplier, DragonTypes.ICE, "alpine", DragonProjectiles.ICE_BALL));
        variants.add(ICEBERG = make(supplier, DragonTypes.ICE, "iceberg", DragonProjectiles.ICE_BALL));
        variants.add(FROST = make(supplier, DragonTypes.ICE, "frost", DragonProjectiles.ICE_BALL));
        variants.add(STARLIGHT = make(supplier, DragonTypes.MOONLIGHT, "starlight", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ECLIPSE = make(supplier, DragonTypes.MOONLIGHT, "eclipse", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(CONSTELLATION = make(supplier, DragonTypes.MOONLIGHT, "constellation", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(MAGMA = make(supplier, DragonTypes.NETHER, "magma", DragonProjectiles.NETHER_FIREBALL));
        variants.add(VOLCANIC = make(supplier, DragonTypes.NETHER, "volcanic", DragonProjectiles.NETHER_FIREBALL));
        variants.add(SOUL_FIRE = make(supplier, DragonTypes.NETHER, "soul_fire", DragonProjectiles.NETHER_FIREBALL));
        variants.add(AMETHYST_SCULK = make(supplier, DragonTypes.SCULK, "sculk_amethyst", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(BETA_SCULK = make(supplier, DragonTypes.SCULK, "sculk_beta", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(WARDEN_SCULK = make(supplier, DragonTypes.SCULK, "sculk_warden", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(SKELETON = make(supplier, DragonTypes.SKELETON, "skeleton", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(STRAY = make(supplier, DragonTypes.SKELETON, "stray", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(BOGGED = make(supplier, DragonTypes.SKELETON, "bogged", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(LIGHTNING_STORM = make(supplier, DragonTypes.STORM, "lightning", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(THUNDER_STORM = make(supplier, DragonTypes.STORM, "thunder", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(BRONZED_STORM = make(supplier, DragonTypes.STORM, "bronzed_storm", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(SUNLIGHT_SUNRISE = make(supplier, DragonTypes.SUNLIGHT, "sunrise", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(SUNLIGHT_SUNSET = make(supplier, DragonTypes.SUNLIGHT, "sunset", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(SUNLIGHT_AURORA = make(supplier, DragonTypes.SUNLIGHT, "aurora", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(TERRA_VALLEY = make(supplier, DragonTypes.TERRA, "valley", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(TERRA_MESA = make(supplier, DragonTypes.TERRA, "mesa", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(TERRA_CRYSTALLINE = make(supplier, DragonTypes.TERRA, "crystalline", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(WATER_POND = make(supplier, DragonTypes.WATER, "pond", DragonProjectiles.WATER_BALL));
        variants.add(WATER_TIDAL = make(supplier, DragonTypes.WATER, "tidal", DragonProjectiles.WATER_BALL));
        variants.add(WATER_BRINE = make(supplier, DragonTypes.WATER, "brine", DragonProjectiles.WATER_BALL));
        variants.add(WITHER = make(supplier, DragonTypes.WITHER, "wither", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ZOMBIE_DROWNED = make(supplier, DragonTypes.ZOMBIE, "drowned", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ZOMBIE_HUSK = make(supplier, DragonTypes.ZOMBIE, "husk", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(ZOMBIE_NORMAL = make(supplier, DragonTypes.ZOMBIE, "zombie", DragonProjectiles.DRAGON_FIREBALL));

        // ---- Dragon Mounts 2 legacy breeds -------------------------------------------------
        // Same breed (type) as their modern counterparts, so they share breath, armour and
        // habitat; only the appearance differs, and it pins the plain body geo rather than the
        // breed's. Types with no legacy art (dark, enchanted, light, moonlight, sculk, storm,
        // sunlight, terra, wither, zombie) simply have no legacy variant.
        variants.add(LEGACY_AETHER = make(supplier, DragonTypes.AETHER, "legacy_aether", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(LEGACY_ENDER = make(supplier, DragonTypes.ENDER, "legacy_ender", DragonProjectiles.ENDER_CHARGE));
        variants.add(LEGACY_FIRE = make(supplier, DragonTypes.FIRE, "legacy_fire", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(LEGACY_FOREST = make(supplier, DragonTypes.FOREST, "legacy_forest", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(LEGACY_ICE = make(supplier, DragonTypes.ICE, "legacy_ice", DragonProjectiles.ICE_BALL));
        variants.add(LEGACY_NETHER = make(supplier, DragonTypes.NETHER, "legacy_nether", DragonProjectiles.NETHER_FIREBALL));
        variants.add(LEGACY_SKELETON = make(supplier, DragonTypes.SKELETON, "legacy_skeleton", DragonProjectiles.DRAGON_FIREBALL));
        variants.add(LEGACY_WATER = make(supplier, DragonTypes.WATER, "legacy_water", DragonProjectiles.WATER_BALL));
        BUILTIN_VALUES = variants.build();
    }

    /// Loading this class is what performs the registration; the body is intentionally empty.
    /// Called explicitly from each loader's entrypoint so the order is declared rather than
    /// inherited from whichever other init class happens to touch it first.
    public static void init() {}
}
