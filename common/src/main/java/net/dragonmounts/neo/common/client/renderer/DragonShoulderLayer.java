package net.dragonmounts.neo.common.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class DragonShoulderLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
    // cache the reconstructed dragon so we don't rebuild it every frame
    private TameableDragonEntity cachedLeft, cachedRight;
    private UUID cachedLeftId, cachedRightId;

    public DragonShoulderLayer(RenderLayerParent<T, PlayerModel<T>> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T player,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        renderShoulder(poseStack, buffer, packedLight, player, partialTicks, true);
        renderShoulder(poseStack, buffer, packedLight, player, partialTicks, false);
    }

    private void renderShoulder(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                T player, float partialTicks, boolean left) {
        CompoundTag tag = left ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
        if (tag.isEmpty()) return;
        var typeOpt = EntityType.byString(tag.getString("id"));
        if (typeOpt.isEmpty() || typeOpt.get() != DMEntities.TAMEABLE_DRAGON.get()) return;

        TameableDragonEntity dragon = getOrCreate(tag, player, left);
        if (dragon == null) return;

        poseStack.pushPose();
        poseStack.translate(left ? 0.4F : -0.4F, player.isCrouching() ? -1.3F : -1.5F, 0.0F);

        Minecraft mc = Minecraft.getInstance();
        mc.getEntityRenderDispatcher().render(
                dragon, 0, 0, 0, 0.0F, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    private TameableDragonEntity getOrCreate(CompoundTag tag, T player, boolean left) {
        UUID id = tag.hasUUID("UUID") ? tag.getUUID("UUID") : null;
        TameableDragonEntity cached = left ? cachedLeft : cachedRight;
        UUID cachedId = left ? cachedLeftId : cachedRightId;
        if (cached != null && id != null && id.equals(cachedId)) return cached;

        var created = EntityType.loadEntityRecursive(tag, player.level(), e -> e);
        if (!(created instanceof TameableDragonEntity dragon)) return null;
        dragon.setBaby(true);   // ensure baby scale on shoulder
        if (left) { cachedLeft = dragon; cachedLeftId = id; }
        else      { cachedRight = dragon; cachedRightId = id; }
        return dragon;
    }
}