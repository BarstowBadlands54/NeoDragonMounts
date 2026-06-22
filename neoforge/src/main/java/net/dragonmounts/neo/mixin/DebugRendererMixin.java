package net.dragonmounts.neo.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.client.debug.DebugInfoRenderer;
import net.dragonmounts.neo.config.ClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.BrainDebugRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Shadow
    @Final
    public BrainDebugRenderer brainDebugRenderer;

    // 1.21.1: DebugRenderer.render(PoseStack, MultiBufferSource.BufferSource, double, double, double)
    // -- the Frustum parameter was only added in 1.21.4.
    @Inject(method = "render", at = @At("HEAD"))
    public void renderExtraLayers(
            PoseStack matrices,
            MultiBufferSource.BufferSource buffers,
            double camX,
            double camY,
            double camZ,
            CallbackInfo info
    ) {
        if (ClientConfig.INSTANCE.debug.get()) {
            this.brainDebugRenderer.render(matrices, buffers, camX, camY, camZ);
            DebugInfoRenderer.INSTANCE.render(matrices, buffers, camX, camY, camZ);
        }
    }
}