package net.dragonmounts.neo.common.client.model.dragon;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DragonGeoModel extends GeoModel<TameableDragonEntity> {
    private static final ResourceLocation ANIMATIONS =
            makeId("animations/entity/dragon/dragonmounts2.dragon.animation.json");

    @Override
    public ResourceLocation getModelResource(TameableDragonEntity dragon) {
        return dragon.getVariant().getDragonType().geoModel();
    }

    @Override
    public ResourceLocation getTextureResource(TameableDragonEntity dragon) {
        return dragon.getVariant().appearance.getBodyTexture(null);
    }

    @Override
    public ResourceLocation getAnimationResource(TameableDragonEntity dragon) {
        return ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(TameableDragonEntity dragon, long instanceId, AnimationState<TameableDragonEntity> state) {
        super.setCustomAnimations(dragon, instanceId, state);

        var head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;

        // distribute pitch across the head + neck for a natural arc
        float pitchRad = data.headPitch() * Mth.DEG_TO_RAD;
        float yawRad   = data.netHeadYaw() * Mth.DEG_TO_RAD;

        head.setRotX(head.getRotX() + pitchRad);
        head.setRotY(head.getRotY() + yawRad);
    }
}