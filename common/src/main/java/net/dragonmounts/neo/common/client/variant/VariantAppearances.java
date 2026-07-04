package net.dragonmounts.neo.common.client.variant;

import net.dragonmounts.neo.common.client.DMParticleSprites;
import net.dragonmounts.neo.common.client.breath.impl.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Function;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;
import static net.dragonmounts.neo.common.client.variant.DefaultAppearance.registerArmorTexture;
import static net.dragonmounts.neo.common.client.variant.VariantAppearance.TEXTURES_ROOT;

public class VariantAppearances {
    // Body shape used to come from a BuiltinFactory ModelLayerLocation; with GeckoLib the body
    // geo is resolved per-breed via DragonType.geoModel(), so the builder no longer takes a model.
    public static DefaultAppearance.Builder builder() {
        return new DefaultAppearance.Builder();
    }

    public static void registerArmorTextures(@Nullable String category, ResourceLocation folder) {
        registerArmorTexture(category, makeId("copper_dragon_armor"),    folder.withSuffix("/copper.png"));
        registerArmorTexture(category, makeId("iron_dragon_armor"),      folder.withSuffix("/iron.png"));
        registerArmorTexture(category, makeId("golden_dragon_armor"),    folder.withSuffix("/gold.png"));
        registerArmorTexture(category, makeId("emerald_dragon_armor"),   folder.withSuffix("/emerald.png"));
        registerArmorTexture(category, makeId("diamond_dragon_armor"),   folder.withSuffix("/diamond.png"));
        registerArmorTexture(category, makeId("netherite_dragon_armor"), folder.withSuffix("/netherite.png"));
    }

    public static final VariantAppearance AETHER_AETHER;
    public static final VariantAppearance AETHER_BREEZE;
    public static final VariantAppearance AETHER_WIND;
    public static final VariantAppearance DARK_BLOODMOON;
    public static final VariantAppearance DARK_DEMON;
    public static final VariantAppearance DARK_IMP;
    public static final VariantAppearance DARK_UNDERWORLD;
    public static final VariantAppearance ENCHANTED_ENCHANTING;
    public static final VariantAppearance ENCHANTED_SHIMMER;
    public static final VariantAppearance ENCHANTED_SPARKLING;
    public static final VariantAppearance ENDER_FEMALE;
    public static final VariantAppearance ENDER_MALE;
    public static final VariantAppearance ENDER_RARE;
    public static final VariantAppearance FIRE_BLAZE;
    public static final VariantAppearance FIRE_FIRE;
    public static final VariantAppearance FIRE_BLUE_FIRE;
    public static final VariantAppearance FOREST_COLD;
    public static final VariantAppearance FOREST_JUNGLE;
    public static final VariantAppearance FOREST_NATURE;
    public static final VariantAppearance FOREST_WARM;
    public static final VariantAppearance ICE_ALPINE;
    public static final VariantAppearance ICE_SNOW;
    public static final VariantAppearance ICE_ICEBERG;
    public static final VariantAppearance MOONLIGHT_STARLIGHT;
    public static final VariantAppearance MOONLIGHT_CONSTELLATION;
    public static final VariantAppearance MOONLIGHT_ECLIPSE;
    public static final VariantAppearance NETHER_MAGMA;
    public static final VariantAppearance NETHER_VOLCANIC;
    public static final VariantAppearance NETHER_SOUL_FIRE;
    public static final VariantAppearance SKELETON_SKELETON;
    public static final VariantAppearance SKELETON_STRAY;
    public static final VariantAppearance SKELETON_BOGGED;
    public static final VariantAppearance STORM_LIGHTNING;
    public static final VariantAppearance STORM_THUNDER;
    public static final VariantAppearance STORM_BRONZED;
    public static final VariantAppearance SUNLIGHT_AURORA;
    public static final VariantAppearance SUNLIGHT_SUNRISE;
    public static final VariantAppearance SUNLIGHT_SUNSET;
    public static final VariantAppearance TERRA_VALLEY;
    public static final VariantAppearance TERRA_MESA;
    public static final VariantAppearance TERRA_CRYSTALLINE;
    public static final VariantAppearance WATER_BRINE;
    public static final VariantAppearance WATER_POND;
    public static final VariantAppearance WATER_TIDAL;
    public static final VariantAppearance WITHER;
    public static final VariantAppearance ZOMBIE_NORMAL;
    public static final VariantAppearance ZOMBIE_DROWNED;
    public static final VariantAppearance ZOMBIE_HUSK;
    public static final VariantAppearance SCULK_WILD;
    public static final VariantAppearance SCULK_WMUTANT;
    public static final VariantAppearance SCULK_HOLLOWED;
    public static final VariantAppearance LIGHT_FALLEN;
    public static final VariantAppearance LIGHT_PRISM;
    public static final VariantAppearance LIGHT_RADIANT;
    public static final VariantAppearance LIGHT_SUNSET;

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.AIRFLOW_BREATH, AirflowBreathParticle.FACTORY);
        AETHER_WIND = builder.build(makeId("aether/wind"));
        AETHER_AETHER = builder.build(makeId("aether/aether"));
        AETHER_BREEZE = builder()
                .withBreath(DMParticleSprites.AIRFLOW_BREATH, AirflowBreathParticle.FACTORY)
                .build(makeId("aether/breeze"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.DARK_BREATH);
        DARK_BLOODMOON = builder.build(makeId("dark/bloodmoon"));
        DARK_DEMON = builder.build(makeId("dark/demon"));
        DARK_IMP = builder.build(makeId("dark/imp"));
        DARK_UNDERWORLD = builder.build(makeId("dark/underworld"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.AIRFLOW_BREATH);
        LIGHT_FALLEN = builder.build(makeId("light/fallen"));
        LIGHT_PRISM = builder.build(makeId("light/prism"));
        LIGHT_RADIANT = builder.build(makeId("light/radiant"));
        LIGHT_SUNSET = builder.build(makeId("light/sunset"));
    }

    static {
        var builder = builder();
        ENCHANTED_ENCHANTING = builder.build(makeId("enchanted/enchanting"));
        ENCHANTED_SHIMMER = builder.build(makeId("enchanted/shimmer"));
        ENCHANTED_SPARKLING = builder.build(makeId("enchanted/sparkling"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.ENDER_BREATH, EnderBreathParticle.FACTORY);
        ENDER_FEMALE = builder.build(makeId("ender/female"));
        ENDER_MALE = builder.build(makeId("ender/male"));
        ENDER_RARE = builder.build(makeId("ender/rare"));
    }

    static {
        var builder = builder();
        FIRE_BLAZE = builder.build(makeId("fire/blaze"));
        FIRE_FIRE = builder.build(makeId("fire/fire"));
        FIRE_BLUE_FIRE = builder.withBreath(DMParticleSprites.BLUE_FLAME_BREATH).build(makeId("fire/blue_fire"));
    }

    static {
        var builder = builder().withBreath(DMParticleSprites.FOREST_BREATH, ForestGasBreathParticle.FACTORY);
        var glow = makeId(TEXTURES_ROOT + "forest/glow.png");
        FOREST_COLD = builder.build(makeId(TEXTURES_ROOT + "forest/cold/body.png"), glow);
        FOREST_JUNGLE = builder.build(makeId(TEXTURES_ROOT + "forest/jungle/body.png"), glow);
        FOREST_NATURE = builder.build(makeId(TEXTURES_ROOT + "forest/nature/body.png"), glow);
        FOREST_WARM = builder.build(makeId(TEXTURES_ROOT + "forest/warm/body.png"), glow);
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.ICE_BREATH, IceBreathParticle.FACTORY);
        ICE_ALPINE = builder.build(makeId("ice/alpine"));
        ICE_SNOW = builder.build(makeId("ice/snow"));
        ICE_ICEBERG = builder.build(makeId("ice/iceberg"));
    }

    static {
        var builder = builder().withBreath(DMParticleSprites.AIRFLOW_BREATH, AirflowBreathParticle.FACTORY);
        MOONLIGHT_STARLIGHT = builder.build(makeId("moonlight/starlight"));
        MOONLIGHT_CONSTELLATION = builder.build(makeId("moonlight/constellation"));
        MOONLIGHT_ECLIPSE = builder.build(makeId("moonlight/eclipse"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.NETHER_BREATH, NetherBreathParticle.FACTORY);
        NETHER_MAGMA = builder.build(makeId("nether/magma"));
        NETHER_VOLCANIC = builder.build(makeId("nether/volcanic"));
        NETHER_SOUL_FIRE = builder.withBreath(DMParticleSprites.SOUL_BREATH).build(makeId("nether/soul_fire"));
    }

    static {
        var builder = builder().setArmorCategory("skeleton").withBreath(DMParticleSprites.NETHER_BREATH, NetherBreathParticle.FACTORY);
        SKELETON_SKELETON = builder.withBreath(DMParticleSprites.SOUL_BREATH).build(makeId("skeleton/normal"));
        SKELETON_STRAY = builder.withBreath(DMParticleSprites.SOUL_BREATH).build(makeId("skeleton/stray"));
        SKELETON_BOGGED = builder.withBreath(DMParticleSprites.SOUL_BREATH).build(makeId("skeleton/bogged"));
    }

    static {
        var builder = builder();
        STORM_LIGHTNING = builder.build(makeId("storm/lightning"));
        STORM_THUNDER = builder.build(makeId("storm/thunder"));
        STORM_BRONZED = builder.build(makeId("storm/bronzed"));
    }

    static {
        var builder = builder();
        SUNLIGHT_SUNRISE = builder.build(makeId("sunlight/sunrise"));
        SUNLIGHT_SUNSET = builder.build(makeId("sunlight/sunset"));
        SUNLIGHT_AURORA = builder.build(makeId("sunlight/aurora"));
    }

    static {
        var builder = builder();
        TERRA_CRYSTALLINE = builder.build(makeId("terra/crystalline"));
        TERRA_MESA = builder.build(makeId("terra/mesa"));
        TERRA_VALLEY = builder.build(makeId("terra/valley"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.WATER_BREATH, WaterBreathParticle.FACTORY);
        WATER_POND = builder.build(makeId("water/pond"));
        WATER_TIDAL = builder.build(makeId("water/tidal"));
        WATER_BRINE = builder.build(makeId("water/brine"));
    }

    static {
        WITHER = builder()
                .withBreath(DMParticleSprites.WITHER_BREATH)
                .build(makeId("wither"));
    }

    static {
        ZOMBIE_NORMAL = builder()
                .withBreath(DMParticleSprites.POISON_BREATH, PoisonBreathParticle.FACTORY)
                .build(makeId("zombie/zombie"));
        ZOMBIE_DROWNED = builder()
                .withBreath(DMParticleSprites.POISON_BREATH, PoisonBreathParticle.FACTORY)
                .build(makeId("zombie/drowned"));
        ZOMBIE_HUSK = builder()
                .withBreath(DMParticleSprites.POISON_BREATH, PoisonBreathParticle.FACTORY)
                .build(makeId("zombie/husk"));
    }

    static {
        var builder = builder().setArmorCategory("sculk");
        SCULK_WILD = builder.build(makeId("sculk/wild_type"));
        SCULK_WMUTANT = builder.build(makeId("sculk/mutant"));
        SCULK_HOLLOWED = builder.build(makeId("sculk/hollowed"));
    }

    static {
        registerArmorTextures(null, makeId("textures/entity/equipment/normal_dragon_body"));
        registerArmorTextures("sculk", makeId("textures/entity/equipment/sculk_dragon_body"));
        registerArmorTextures("skeleton", makeId("textures/entity/equipment/skeleton_dragon_body"));
    }

    public static Function<String, VariantAppearance> getBuiltinSupplier() {
        return key -> switch (key) {
            case "aether" -> AETHER_AETHER;
            case "wind" -> AETHER_WIND;
            case "breeze" -> AETHER_BREEZE;
            case "bloodmoon" -> DARK_BLOODMOON;
            case "demon" -> DARK_DEMON;
            case "imp" -> DARK_IMP;
            case "underworld" -> DARK_UNDERWORLD;
            case "enchanting" -> ENCHANTED_ENCHANTING;
            case "shimmer" -> ENCHANTED_SHIMMER;
            case "sparkling" -> ENCHANTED_SPARKLING;
            case "ender_female" -> ENDER_FEMALE;
            case "ender_male" -> ENDER_MALE;
            case "ender_rare" -> ENDER_RARE;
            case "blaze" -> FIRE_BLAZE;
            case "fire" -> FIRE_FIRE;
            case "blue_fire" -> FIRE_BLUE_FIRE;
            case "cold" -> FOREST_COLD;
            case "jungle" -> FOREST_JUNGLE;
            case "nature" -> FOREST_NATURE;
            case "warm" -> FOREST_WARM;
            case "alpine" -> ICE_ALPINE;
            case "iceberg" -> ICE_ICEBERG;
            case "snow" -> ICE_SNOW;
            case "starlight" -> MOONLIGHT_STARLIGHT;
            case "constellation" -> MOONLIGHT_CONSTELLATION;
            case "eclipse" -> MOONLIGHT_ECLIPSE;
            case "magma" -> NETHER_MAGMA;
            case "volcanic" -> NETHER_VOLCANIC;
            case "soul_fire" -> NETHER_SOUL_FIRE;
            case "wild_sculk" -> SCULK_WILD;
            case "mutant_sculk" -> SCULK_WMUTANT;
            case "hollowed" -> SCULK_HOLLOWED;
            case "skeleton" -> SKELETON_SKELETON;
            case "stray" -> SKELETON_STRAY;
            case "bogged" -> SKELETON_BOGGED;
            case "lightning" -> STORM_LIGHTNING;
            case "thunder" -> STORM_THUNDER;
            case "bronzed_storm" -> STORM_BRONZED;
            case "sunrise" -> SUNLIGHT_SUNRISE;
            case "sunset" -> SUNLIGHT_SUNSET;
            case "aurora" -> SUNLIGHT_AURORA;
            case "valley" -> TERRA_VALLEY;
            case "crystalline" -> TERRA_CRYSTALLINE;
            case "mesa" -> TERRA_MESA;
            case "tidal" -> WATER_TIDAL;
            case "pond" -> WATER_POND;
            case "brine" -> WATER_BRINE;
            case "zombie" -> ZOMBIE_NORMAL;
            case "drowned" -> ZOMBIE_DROWNED;
            case "husk" -> ZOMBIE_HUSK;
            case "wither" -> WITHER;
            case "fallen" -> LIGHT_FALLEN;
            case "prism" -> LIGHT_PRISM;
            case "radiant" -> LIGHT_RADIANT;
            case "light_sunset" -> LIGHT_SUNSET;
            default -> throw new NoSuchElementException(
                    "There is no built-in variant appearance named \"" + key + "\". Please create a custom supplier."
            );
        };
    }
}