package net.dragonmounts.neo.common.client.model.armor;

import net.dragonmounts.neo.common.item.DragonScaleArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/// One instance serves every slot and every dragon type: both the geometry and the texture
/// resolve off the item being rendered, and GeckoLib caches baked models per ResourceLocation,
/// so splitting this per slot would buy nothing.
public class DragonScaleArmorGeoModel extends GeoModel<DragonScaleArmorItem> {
    /// DragonScaleArmorItem registers no controllers, so AnimationProcessor#tickAnimation never
    /// resolves an animation and this file is never actually read. Pointing it at the existing
    /// dragon animations, as DragonHeadItemGeoModel does, keeps it from being a dangling path.
    private static final ResourceLocation ANIMATIONS =
            makeId("animations/entity/dragon/dragonmounts2.dragon.animation.json");

    @Override
    public ResourceLocation getModelResource(DragonScaleArmorItem item) {
        return item.type.armorGeo(item.getType());
    }

    /// Never null in practice -- createGeoRenderer returns null for types with no armorTexture,
    /// so this model is only ever reached for a type that has one.
    @Override
    public ResourceLocation getTextureResource(DragonScaleArmorItem item) {
        return item.type.armorTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(DragonScaleArmorItem item) {
        return ANIMATIONS;
    }

    /// Runs last in GeoModel#handleAnimations, after tickAnimation, so values set here win.
    /// Chestplate geo files carry an optional `cape` bone parented to armorBody; it inherits the
    /// torso transform for free, but nothing gives it motion of its own -- that is this method.
    /// Absent bone means a set without a cape, so this no-ops for those.
    @Override
    public void setCustomAnimations(DragonScaleArmorItem item, long instanceId, AnimationState<DragonScaleArmorItem> state) {
        GeoBone cape = getBone("cape").orElse(null);
        if (cape == null) return;

        // GeoArmorRenderer#preRender puts the wearer on the state under DataTickets.ENTITY.
        if (!(state.getData(DataTickets.ENTITY) instanceof LivingEntity wearer)) return;

        float partialTick = state.getPartialTick();
        float gait = Math.min(wearer.walkAnimation.speed(partialTick), 1.0F);
        float stride = wearer.walkAnimation.position(partialTick);

        // Lift trails the gait; the stride term keeps it breathing rather than pinned at an angle.
        float lift = gait * 0.62F + Mth.sin(stride * 0.55F) * 0.10F * gait;
        // Falling billows it out, rising presses it back down.
        float vertical = (float) Mth.clamp(-wearer.getDeltaMovement().y * 1.4D, -0.25D, 0.55D);
        // Sideways sway, a half-period behind the lift so it reads as fabric rather than a board.
        float sway = Mth.cos(stride * 0.55F + 1.57F) * 0.09F * gait;
        // Crouching pitches armorBody forward and the cape inherits that, so add trail to compensate.
        float crouch = wearer.isCrouching() ? 0.38F : 0.0F;

        // Clamped non-negative before the sign flip below: `vertical` bottoms out at -0.25, which
        // would otherwise swing the cape ~9 degrees into the torso on a standing jump.
        float swing = Mth.clamp(0.09F + lift + vertical + crouch, 0.0F, 1.35F);

        // NEGATIVE rotX trails the cape behind the wearer. The bone pivots at [0, 24, 3] with its
        // cube hanging 16 below, so a positive angle carries the hem toward -Z -- through the
        // stomach. Flip this sign and it clips straight through the body again.
        var rest = cape.getInitialSnapshot();
        cape.setRotX(rest.getRotX() - swing);
        cape.setRotZ(rest.getRotZ() + sway);
    }
}
