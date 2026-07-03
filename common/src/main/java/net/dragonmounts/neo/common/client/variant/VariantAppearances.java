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
    public static final VariantAppearance AETHER;
    public static final VariantAppearance BREEZE;
    public static final VariantAppearance WIND;
    public static final VariantAppearance DARK_FEMALE;
    public static final VariantAppearance DARK_MALE;
    public static final VariantAppearance ENCHANTED_FEMALE;
    public static final VariantAppearance ENCHANTED_MALE;
    public static final VariantAppearance ENDER_FEMALE;
    public static final VariantAppearance ENDER_MALE;
    public static final VariantAppearance ENDER_RARE;
    public static final VariantAppearance BLAZE;
    public static final VariantAppearance FIRE;
    public static final VariantAppearance BLUE_FIRE;
    public static final VariantAppearance COLD;
    public static final VariantAppearance DRY;
    public static final VariantAppearance JUNGLE;
    public static final VariantAppearance NATURE;
    public static final VariantAppearance WARM;
    public static final VariantAppearance ALPINE;
    public static final VariantAppearance SNOW;
    public static final VariantAppearance ICEBERG;
    public static final VariantAppearance STARLIGHT;
    public static final VariantAppearance CONSTELLATION;
    public static final VariantAppearance ECLIPSE;
    public static final VariantAppearance MAGMA;
    public static final VariantAppearance VOLCANIC;
    public static final VariantAppearance SOUL_FIRE;
    public static final VariantAppearance SKELETON;
    public static final VariantAppearance STRAY;
    public static final VariantAppearance BOGGED;
    public static final VariantAppearance STORM_FEMALE;
    public static final VariantAppearance STORM_MALE;
    public static final VariantAppearance BRONZED_STORM;
    public static final VariantAppearance SUNLIGHT_FEMALE;
    public static final VariantAppearance SUNLIGHT_MALE;
    public static final VariantAppearance AURORA;
    public static final VariantAppearance TERRA_FEMALE;
    public static final VariantAppearance TERRA_MALE;
    public static final VariantAppearance WATER_FEMALE;
    public static final VariantAppearance WATER_MALE;
    public static final VariantAppearance BRINE;
    public static final VariantAppearance WITHER;
    public static final VariantAppearance ZOMBIE;
    public static final VariantAppearance WILD_SCULK;
    public static final VariantAppearance MUTANT_SCULK;
    public static final VariantAppearance HOLLOWED;
    public static final VariantAppearance FALLEN;
    public static final VariantAppearance PRISM;
    public static final VariantAppearance RADIANT;
    public static final VariantAppearance SUNSET;

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.AIRFLOW_BREATH, AirflowBreathParticle.FACTORY);
        WIND = builder.build(makeId("aether/wind"));
        AETHER = builder.build(makeId("aether/aether"));
        BREEZE = builder()
                .withBreath(DMParticleSprites.AIRFLOW_BREATH, AirflowBreathParticle.FACTORY)
                .build(makeId("aether/breeze"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.DARK_BREATH);
        DARK_FEMALE = builder.build(makeId("dark/female"));
        DARK_MALE = builder.build(makeId("dark/male"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.AIRFLOW_BREATH);
        FALLEN = builder.build(makeId("light/fallen"));
        PRISM = builder.build(makeId("light/prism"));
        RADIANT = builder.build(makeId("light/radiant"));
        SUNSET = builder.build(makeId("light/sunset"));
    }

    static {
        var builder = builder();
        ENCHANTED_FEMALE = builder.build(makeId("enchanted/female"));
        ENCHANTED_MALE = builder.build(makeId("enchanted/male"));
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
        BLAZE = builder.build(makeId("fire/female"));
        FIRE = builder.build(makeId("fire/male"));
        BLUE_FIRE = builder.withBreath(DMParticleSprites.BLUE_FLAME_BREATH).build(makeId("fire/blue"));
    }

    static {
        var builder = builder().withBreath(DMParticleSprites.FOREST_BREATH, ForestGasBreathParticle.FACTORY);
        var glow = makeId(TEXTURES_ROOT + "forest/glow.png");
        COLD = builder.build(makeId(TEXTURES_ROOT + "forest/cold/body.png"), glow);
        DRY = builder.build(makeId(TEXTURES_ROOT + "forest/dry/body.png"), glow);
        JUNGLE = builder.build(makeId(TEXTURES_ROOT + "forest/jungle/body.png"), glow);
        NATURE = builder.build(makeId(TEXTURES_ROOT + "forest/nature/body.png"), glow);
        WARM = builder.build(makeId(TEXTURES_ROOT + "forest/warm/body.png"), glow);
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.ICE_BREATH, IceBreathParticle.FACTORY);
        ALPINE = builder.build(makeId("ice/alpine"));
        SNOW = builder.build(makeId("ice/snow"));
        ICEBERG = builder.build(makeId("ice/iceberg"));
    }

    static {
        var builder = builder().withBreath(DMParticleSprites.AIRFLOW_BREATH, AirflowBreathParticle.FACTORY);
        STARLIGHT = builder.build(makeId("moonlight/constellation"));
        CONSTELLATION = builder.build(makeId("moonlight/starlight"));
        ECLIPSE = builder.build(makeId("moonlight/eclipse"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.NETHER_BREATH, NetherBreathParticle.FACTORY);
        MAGMA = builder.build(makeId("nether/magma"));
        VOLCANIC = builder.build(makeId("nether/volcanic"));
        SOUL_FIRE = builder.withBreath(DMParticleSprites.SOUL_BREATH).build(makeId("nether/soul_fire"));
    }

    static {
        var builder = builder().setArmorCategory("skeleton");
        SKELETON = builder.build(makeId("skeleton/normal"));
        STRAY = builder.build(makeId("skeleton/stray"));
        BOGGED = builder.build(makeId("skeleton/bogged"));
    }

    static {
        var builder = builder();
        STORM_FEMALE = builder.build(makeId("storm/female"));
        STORM_MALE = builder.build(makeId("storm/male"));
        BRONZED_STORM = builder.build(makeId("storm/bronzed"));
    }

    static {
        var builder = builder();
        SUNLIGHT_FEMALE = builder.build(makeId("sunlight/female"));
        SUNLIGHT_MALE = builder.build(makeId("sunlight/male"));
        AURORA = builder.build(makeId("sunlight/aurora"));
    }

    static {
        var builder = builder();
        TERRA_FEMALE = builder.build(makeId("terra/female"));
        TERRA_MALE = builder.build(makeId("terra/male"));
    }

    static {
        var builder = builder()
                .withBreath(DMParticleSprites.WATER_BREATH, WaterBreathParticle.FACTORY);
        WATER_FEMALE = builder.build(makeId("water/female"));
        WATER_MALE = builder.build(makeId("water/male"));
        BRINE = builder.build(makeId("water/brine"));
    }

    static {
        WITHER = builder()
                .withBreath(DMParticleSprites.WITHER_BREATH)
                .build(makeId("wither"));
    }

    static {
        ZOMBIE = builder()
                .withBreath(DMParticleSprites.POISON_BREATH, PoisonBreathParticle.FACTORY)
                .build(makeId("zombie"));
    }

    static {
        var builder = builder().setArmorCategory("sculk");
        WILD_SCULK = builder.build(makeId("sculk/wild_type"));
        MUTANT_SCULK = builder.build(makeId("sculk/mutant"));
        HOLLOWED = builder.build(makeId("sculk/hollowed"));
    }

    static {
        registerArmorTextures(null, makeId("textures/entity/equipment/normal_dragon_body"));
        registerArmorTextures("sculk", makeId("textures/entity/equipment/sculk_dragon_body"));
        registerArmorTextures("skeleton", makeId("textures/entity/equipment/skeleton_dragon_body"));
    }

    public static Function<String, VariantAppearance> getBuiltinSupplier() {
        return key -> switch (key) {
            case "aether" -> AETHER;
            case "wind" -> WIND;
            case "breeze" -> BREEZE;
            case "dark_female" -> DARK_FEMALE;
            case "dark_male" -> DARK_MALE;
            case "enchanted_female" -> ENCHANTED_FEMALE;
            case "enchanted_male" -> ENCHANTED_MALE;
            case "ender_female" -> ENDER_FEMALE;
            case "ender_male" -> ENDER_MALE;
            case "ender_rare" -> ENDER_RARE;
            case "blaze" -> BLAZE;
            case "fire" -> FIRE;
            case "blue_fire" -> BLUE_FIRE;
            case "cold" -> COLD;
            case "dry" -> DRY;
            case "jungle" -> JUNGLE;
            case "nature" -> NATURE;
            case "warm" -> WARM;
            case "alpine" -> ALPINE;
            case "iceberg" -> ICEBERG;
            case "snow" -> SNOW;
            case "constellation" -> CONSTELLATION;
            case "starlight" -> STARLIGHT;
            case "eclipse" -> ECLIPSE;
            case "magma" -> MAGMA;
            case "volcanic" -> VOLCANIC;
            case "soul_fire" -> SOUL_FIRE;
            case "wild_sculk" -> WILD_SCULK;
            case "mutant_sculk" -> MUTANT_SCULK;
            case "hollowed" -> HOLLOWED;
            case "skeleton" -> SKELETON;
            case "stray" -> STRAY;
            case "bogged" -> BOGGED;
            case "storm_female" -> STORM_FEMALE;
            case "storm_male" -> STORM_MALE;
            case "bronzed_storm" -> BRONZED_STORM;
            case "sunlight_female" -> SUNLIGHT_FEMALE;
            case "sunlight_male" -> SUNLIGHT_MALE;
            case "aurora" -> AURORA;
            case "terra_female" -> TERRA_FEMALE;
            case "terra_male" -> TERRA_MALE;
            case "water_female" -> WATER_FEMALE;
            case "water_male" -> WATER_MALE;
            case "brine" -> BRINE;
            case "zombie" -> ZOMBIE;
            case "wither" -> WITHER;
            case "fallen" -> FALLEN;
            case "prism" -> PRISM;
            case "radiant" -> RADIANT;
            case "sunset" -> SUNSET;
            default -> throw new NoSuchElementException(
                    "There is no built-in variant appearance named \"" + key + "\". Please create a custom supplier."
            );
        };
    }
}