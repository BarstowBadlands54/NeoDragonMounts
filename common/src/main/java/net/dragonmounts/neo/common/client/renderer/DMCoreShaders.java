package net.dragonmounts.neo.common.client.renderer;

import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

/// 1.21.1 port of the dragon decal core shaders.
///
/// 1.21.4 registered these as `ShaderProgram` constants appended to `CoreShaders.PROGRAMS`
/// via a mixin. That whole system is absent in 1.21.1, so instead the shaders are loaded
/// through Fabric's `CoreShaderRegistrationCallback` (registered in the fabric client init)
/// and stashed here as `ShaderInstance`s. The `RenderType` `ShaderStateShard`s read them back
/// through the getters (`new ShaderStateShard(DMCoreShaders::getEntityCutoutDecal)`), and the
/// callback writes them through the setters (`context.register(id, fmt, DMCoreShaders::setEntityCutoutDecal)`).
public final class DMCoreShaders {
    @Nullable
    private static ShaderInstance entityCutoutDecal;
    @Nullable
    private static ShaderInstance entityTranslucentEmissiveDecal;

    public static void setEntityCutoutDecal(@Nullable ShaderInstance shader) {
        entityCutoutDecal = shader;
    }

    @Nullable
    public static ShaderInstance getEntityCutoutDecal() {
        return entityCutoutDecal;
    }

    public static void setEntityTranslucentEmissiveDecal(@Nullable ShaderInstance shader) {
        entityTranslucentEmissiveDecal = shader;
    }

    @Nullable
    public static ShaderInstance getEntityTranslucentEmissiveDecal() {
        return entityTranslucentEmissiveDecal;
    }

    private DMCoreShaders() {}
}