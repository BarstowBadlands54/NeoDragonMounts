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

    /**
     * Variant first, breed second. The breed's geo stays the default so every existing variant
     * renders exactly as before; only a variant that explicitly pins a model diverges.
     */
    @Override
    public ResourceLocation getModelResource(TameableDragonEntity dragon) {
        var variant = dragon.getVariant();
        var appearance = variant.appearance;
        if (appearance != null) {
            var override = appearance.getGeoModel();
            if (override != null) return override;
        }
        return variant.getDragonType().geoModel();
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

        var processor = this.getAnimationProcessor();
        var saddle = processor.getBone("body.saddle");
        if (saddle != null) saddle.setHidden(!dragon.isSaddled());
        var chest = processor.getBone("body.chest");
        if (chest != null) chest.setHidden(!dragon.hasChest());

        // hide the back spikes/scales when a saddle is on
        var back = processor.getBone("body.back");
        if (back != null) back.setHidden(dragon.isSaddled());

        var head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        // use the smoothed roll (already turn-based + clamped) so the tail follows turns smoothly
        float turnSway = Mth.lerp(state.getPartialTick(), dragon.renderRollO, dragon.renderRoll);
        // convert to a small per-segment yaw; sign so the tail trails the turn
        float perSeg = (turnSway / 25.0F) * 0.06F;   // normalize by the roll clamp (±25), tiny amplitude
        for (int i = 0; i <= 11; i++) {
            var seg = processor.getBone("tail." + i);
            if (seg == null) continue;
            // deeper segments sway slightly more (tip trails most) for a smooth curve
            float factor = 1.0F + i * 0.15F;
            seg.setRotY(seg.getRotY() + perSeg * factor);
        }

        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data == null) return;

        // distribute pitch across the head + neck for a natural arc
        float pitchRad = data.headPitch() * Mth.DEG_TO_RAD;
        float yawRad = data.netHeadYaw() * Mth.DEG_TO_RAD;

        head.setRotX(head.getRotX() + pitchRad);
        head.setRotY(head.getRotY() + yawRad);
    }
}