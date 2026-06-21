package net.dragonmounts.neo.common.client.renderer;

import net.minecraft.client.renderer.ShaderInstance;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 has no {@code ShaderProgram}/{@code ShaderDefines} handles. Core shaders are
 * {@link ShaderInstance} objects that must be built during shader registration (they need a
 * {@code ResourceProvider} and can throw {@code IOException}), so they cannot be {@code static final}.
 * <p>
 * These fields are populated by the loader-specific registration hooks (NeoForge
 * {@code RegisterShadersEvent}, Fabric {@code CoreShaderRegistrationCallback}) and consumed by
 * RenderTypes via {@code new RenderStateShard.ShaderStateShard(DMCoreShaders::getEntityCutoutDecal)}.
 */
public class DMCoreShaders {
    @Nullable
    public static ShaderInstance rendertypeEntityCutoutDecal;
    @Nullable
    public static ShaderInstance rendertypeEntityTranslucentEmissiveDecal;

    @Nullable
    public static ShaderInstance getEntityCutoutDecal() {
        return rendertypeEntityCutoutDecal;
    }

    @Nullable
    public static ShaderInstance getEntityTranslucentEmissiveDecal() {
        return rendertypeEntityTranslucentEmissiveDecal;
    }
}