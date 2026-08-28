package net.dragonmounts.neo.common.client.model.armor;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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

    /// Fraction of the remaining gap the cape closes each tick. Lower trails more.
    private static final float CAPE_FOLLOW = 0.28F;
    /// Beyond this many missed ticks the wearer was off-screen or teleported; snap rather than slide.
    private static final int CAPE_MAX_CATCHUP = 8;
    private static final int CAPE_EVICT_AFTER = 200;

    /// Per-wearer, because one model instance serves every wearer of this item. Keyed on the
    /// instanceId GeoArmorRenderer derives from entity + slot, so two players in the same
    /// chestplate don't fight over one damper.
    private final Long2ObjectOpenHashMap<CapeMotion> capeMotion = new Long2ObjectOpenHashMap<>();
    private long frames;

    private static final class CapeMotion {
        float swing;
        float swingPrev;
        int lastTick = -1;
        long touched;
    }

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

        // Observed displacement, not getDeltaMovement(). On remote players delta movement is
        // rebuilt from position packets that land every few ticks, so it arrives as steps; the
        // previous-tick delta is the motion that actually happened.
        float rise = (float) (wearer.getY() - wearer.yOld);
        float vertical = Mth.clamp(-rise * 1.4F, -0.25F, 0.55F);
        // Crouching pitches armorBody forward and the cape inherits that, so add trail to
        // compensate. It is a boolean, so it steps -- the damper below is what stops it popping.
        float crouch = wearer.isCrouching() ? 0.38F : 0.0F;

        // Clamped non-negative before the sign flip below: `vertical` bottoms out at -0.25, which
        // would otherwise swing the cape ~9 degrees into the torso on a standing jump.
        float target = Mth.clamp(0.09F + gait * 0.62F + vertical + crouch, 0.0F, 1.35F);

        float swing = followCape(instanceId, wearer.tickCount, target, partialTick);

        // Oscillation rides on top of the damped value rather than through it: stride and gait are
        // already partial-tick interpolated, so these are smooth on their own, and damping them
        // would only flatten the amplitude.
        float bob = Mth.sin(stride * 0.55F) * 0.10F * gait;
        float sway = Mth.cos(stride * 0.55F + 1.57F) * 0.09F * gait;

        // NEGATIVE rotX trails the cape behind the wearer. The bone pivots at [0, 24, 3] with its
        // cube hanging 16 below, so a positive angle carries the hem toward -Z -- through the
        // stomach. Flip this sign and it clips straight through the body again.
        var rest = cape.getInitialSnapshot();
        cape.setRotX(rest.getRotX() - Math.max(swing + bob, 0.0F));
        cape.setRotZ(rest.getRotZ() + sway);
    }

    /// Damped follower on a tick clock, sampled per frame.
    ///
    /// The damping has to advance per tick or its speed would scale with framerate; the lerp has
    /// to happen per frame or the cape would visibly step at 20Hz. Doing both is what vanilla does
    /// for the player cloak, and is the reason this reads as fabric rather than as a stutter.
    private float followCape(long instanceId, int tickCount, float target, float partialTick) {
        CapeMotion motion = this.capeMotion.get(instanceId);

        if (motion == null) {
            motion = new CapeMotion();
            this.capeMotion.put(instanceId, motion);
            evictStaleCapeMotion();
        }
        motion.touched = this.frames++;

        // `!=` rather than a subtraction guard: several frames land inside one tick and must not
        // each advance the damper. Comparing for equality also cannot overflow.
        if (motion.lastTick != tickCount) {
            int steps = motion.lastTick < 0 ? -1 : tickCount - motion.lastTick;

            if (steps < 0 || steps > CAPE_MAX_CATCHUP) {
                motion.swing = motion.swingPrev = target;
            } else {
                for (int i = 0; i < steps; ++i) {
                    motion.swingPrev = motion.swing;
                    motion.swing += (target - motion.swing) * CAPE_FOLLOW;
                }
            }
            motion.lastTick = tickCount;
        }

        return Mth.lerp(partialTick, motion.swingPrev, motion.swing);
    }

    /// Wearers stop being sampled the moment they leave view, so entries would otherwise
    /// accumulate one per (entity, slot) seen for the whole session.
    private void evictStaleCapeMotion() {
        if (this.capeMotion.size() < 64) return;

        long cutoff = this.frames - CAPE_EVICT_AFTER;
        this.capeMotion.values().removeIf(motion -> motion.touched < cutoff);
    }
}
