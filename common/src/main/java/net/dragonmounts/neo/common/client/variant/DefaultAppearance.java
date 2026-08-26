package net.dragonmounts.neo.common.client.variant;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.dragonmounts.neo.common.client.DMParticleSprites;
import net.dragonmounts.neo.common.client.breath.BreathParticleFactory;
import net.dragonmounts.neo.common.client.breath.impl.FlameBreathParticle;
import net.dragonmounts.neo.common.client.model.dragon.DragonGeoModel;
import net.dragonmounts.neo.common.client.renderer.RenderStateAccessor;
import net.dragonmounts.neo.common.client.renderer.dragon.DragonRenderer;
import net.dragonmounts.neo.common.entity.breath.BreathParticleOption;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DefaultAppearance implements VariantAppearance {
    private static final Object2ObjectOpenHashMap<ResourceLocation, DragonArmorSkin> DEFAULT_ARMOR_SKINS = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<String, Map<ResourceLocation, DragonArmorSkin>> ARMOR_SKINS = new Object2ObjectOpenHashMap<>();

    /// A category that registers no skin for an asset falls back to the one registered under
    /// {@code category == null}, so a body only needs its own art where it actually differs.
    public static void registerArmorSkin(@Nullable String category, ResourceLocation asset, DragonArmorSkin skin) {
        if (category == null) {
            if (DEFAULT_ARMOR_SKINS.putIfAbsent(asset, skin) == null) return;
        } else if (
                ARMOR_SKINS.computeIfAbsent(category, DefaultAppearance::makeMap).putIfAbsent(asset, skin) == null
        ) return;
        throw new IllegalStateException("Duplicate key: " + asset);
    }

    public final BreathParticleFactory factory;
    public final ResourceLocation breath;
    public final ResourceLocation body;
    public final RenderType base;
    public final RenderType decal;
    public final RenderType glow;
    public final RenderType glowDecal;
    public final RenderType chest;
    public final RenderType saddle;
    public final Map<ResourceLocation, DragonArmorSkin> armors;
    public final @Nullable ResourceLocation geoModel;
    public final @Nullable ResourceLocation headGeoModel;
    public final ResourceLocation saddleTexture;
    private DragonGeoModel model;

    public DefaultAppearance(
            ResourceLocation body,
            ResourceLocation glow,
            ResourceLocation breath,
            Map<ResourceLocation, DragonArmorSkin> armors,
            BreathParticleFactory factory
    ) {
        this(body, glow, breath, armors, factory, null, null, DEFAULT_SADDLE);
    }

    public DefaultAppearance(
            ResourceLocation body,
            ResourceLocation glow,
            ResourceLocation breath,
            Map<ResourceLocation, DragonArmorSkin> armors,
            BreathParticleFactory factory,
            @Nullable ResourceLocation geoModel,
            @Nullable ResourceLocation headGeoModel,
            ResourceLocation saddleTexture
    ) {
        this.factory = factory;
        this.breath = breath;
        this.armors = armors;
        this.body = body;
        this.geoModel = geoModel;
        this.headGeoModel = headGeoModel;
        this.saddleTexture = saddleTexture;
        this.base = RenderType.entityCutoutNoCull(body);
        this.decal = RenderStateAccessor.entityCutoutDecal(body, DEFAULT_DISSOLVE);
        this.glow = RenderType.entityTranslucentEmissive(glow);
        this.glowDecal = RenderStateAccessor.entityTranslucentEmissiveDecal(glow, DEFAULT_DISSOLVE);
        this.chest = RenderType.entityCutoutNoCull(DEFAULT_CHEST);
        this.saddle = RenderType.entityCutoutNoCull(saddleTexture);
    }

    @Override
    public @Nullable ResourceLocation getGeoModel() {
        return this.geoModel;
    }

    @Override
    public @Nullable ResourceLocation getHeadGeoModel() {
        return this.headGeoModel;
    }

    @Override
    public ResourceLocation getSaddleTexture() {
        return this.saddleTexture;
    }

    @Override
    public void onReload(EntityModelSet models) {
        this.model = new DragonGeoModel();
    }

    @Override
    public DragonGeoModel getModel() {
        return this.model;
    }

    @Override
    public ResourceLocation getBodyTexture(DragonRenderer state) {
        return this.body;
    }

    @Override
    public RenderType getBase(@Nullable DragonRenderer state) {
        return this.base;
    }

    @Override
    public RenderType getGlow(@Nullable DragonRenderer state) {
        return this.glow;
    }

    @Override
    public RenderType getDecal(DragonRenderer state) {
        return this.decal;
    }

    @Override
    public RenderType getGlowDecal(DragonRenderer state) {
        return this.glowDecal;
    }

    @Override
    public RenderType getChest(DragonRenderer state) {
        return this.chest;
    }

    @Override
    public RenderType getSaddle(DragonRenderer state) {
        return this.saddle;
    }

    @Override
    public @Nullable DragonArmorSkin getArmorSkin(@Nullable ResourceLocation asset) {
        var skin = this.armors.get(asset);
        return skin == null ? DEFAULT_ARMOR_SKINS.get(asset) : skin;
    }

    @Override
    public Particle createBreathParticle(BreathParticleOption option, TextureAtlas atlas, ClientLevel level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        return this.factory.createParticle(option, atlas.getSprite(this.breath), level, x, y, z, motionX, motionY, motionZ);
    }

    public static class Builder {
        public BreathParticleFactory factory = FlameBreathParticle.FACTORY;
        public ResourceLocation breath = DMParticleSprites.FLAME_BREATH;
        public Map<ResourceLocation, DragonArmorSkin> armors = Collections.emptyMap();
        public @Nullable ResourceLocation geoModel = null;
        public @Nullable ResourceLocation headGeoModel = null;
        public ResourceLocation saddleTexture = DEFAULT_SADDLE;

        public Builder() {
        }

        /// Mirrors {@link net.dragonmounts.neo.compat.registry.DragonTypeBuilder#model} so a
        /// variant can pin its own silhouette instead of inheriting the breed's.
        public Builder withModel(String bodyShape) {
            this.geoModel = makeId("geo/model/dragonmounts2.dragon." + bodyShape + ".geo.json");
            return this;
        }

        public Builder withModel(String bodyShape, String headShape) {
            this.headGeoModel = makeId("geo/head/dragonmounts2.head_block." + headShape + ".geo.json");
            return this.withModel(bodyShape);
        }

        public Builder withSaddle(ResourceLocation saddle) {
            this.saddleTexture = saddle;
            return this;
        }

        public Builder setArmorCategory(@Nullable String category) {
            this.armors = category == null ? Collections.emptyMap() : ARMOR_SKINS.computeIfAbsent(category, DefaultAppearance::makeMap);
            return this;
        }

        public Builder withBreath(ResourceLocation breath) {
            this.breath = breath;
            return this;
        }

        public Builder withBreath(ResourceLocation breath, BreathParticleFactory factory) {
            this.factory = factory;
            return this.withBreath(breath);
        }

        public DefaultAppearance build(ResourceLocation folder) {
            String path = folder.getPath();
            return this.build(
                    folder.withPath(TEXTURES_ROOT + path + "/body.png"),
                    folder.withPath(TEXTURES_ROOT + path + "/glow.png")
            );
        }

        /// Like {@link #build(ResourceLocation)} but also takes the saddle from the same folder,
        /// for variants that ship their own tack alongside body.png and glow.png.
        public DefaultAppearance buildWithOwnSaddle(ResourceLocation folder) {
            String path = folder.getPath();
            return this.withSaddle(folder.withPath(TEXTURES_ROOT + path + "/saddle.png")).build(folder);
        }

        public DefaultAppearance build(ResourceLocation body, ResourceLocation glow) {
            return new DefaultAppearance(
                    body, glow, this.breath, this.armors, this.factory,
                    this.geoModel, this.headGeoModel, this.saddleTexture
            );
        }
    }

    static Map<ResourceLocation, DragonArmorSkin> makeMap(Object ignored) {
        return new Object2ObjectOpenHashMap<>();
    }
}