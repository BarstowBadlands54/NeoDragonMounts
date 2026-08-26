package net.dragonmounts.neo.common.client.variant;

import net.dragonmounts.neo.common.client.model.dragon.DragonGeoModel;
import net.dragonmounts.neo.common.client.renderer.dragon.DragonRenderer;
import net.dragonmounts.neo.common.entity.breath.BreathParticleOption;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public interface VariantAppearance {
    String TEXTURES_ROOT = "textures/entity/dragon/";
    ResourceLocation DEFAULT_CHEST = makeId(TEXTURES_ROOT + "chest.png");
    ResourceLocation DEFAULT_SADDLE = makeId(TEXTURES_ROOT + "saddle.png");
    ResourceLocation DEFAULT_DISSOLVE = makeId(TEXTURES_ROOT + "dissolve.png");

    /**
     * Body geo this specific variant renders with, or null to inherit the breed's.
     * <p>
     * Geo used to be resolved purely from {@link net.dragonmounts.neo.compat.registry.DragonType},
     * which forces every variant of a breed onto the same silhouette. That holds for the modern
     * variants but not the legacy ones: legacy forest, for instance, shares the forest breed's
     * type and breath while using the plain body rather than the antlered one.
     */
    default @Nullable ResourceLocation getGeoModel() {
        return null;
    }

    /** Head-block geo for this variant, or null to inherit the breed's. */
    default @Nullable ResourceLocation getHeadGeoModel() {
        return null;
    }

    /** Saddle overlay texture, so a variant can ship tack that matches its own palette. */
    default ResourceLocation getSaddleTexture() {
        return DEFAULT_SADDLE;
    }

    void onReload(EntityModelSet models);

    DragonGeoModel getModel();

    RenderType getBase(@Nullable DragonRenderer state);

    RenderType getGlow(@Nullable DragonRenderer state);

    RenderType getDecal(DragonRenderer state);

    RenderType getGlowDecal(DragonRenderer state);

    RenderType getChest(DragonRenderer state);

    RenderType getSaddle(DragonRenderer state);

    /// Textures for the armour item with this registry id, or null if it has none on this body.
    @Nullable DragonArmorSkin getArmorSkin(@Nullable ResourceLocation asset);

    ResourceLocation getBodyTexture(DragonRenderer state);

    Particle createBreathParticle(
            BreathParticleOption option,
            TextureAtlas atlas,
            ClientLevel level,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ
    );
}
