package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Autonomous fire-breath attack for an UNRIDDEN dragon in combat. Bites trivial foes; breathes
 * fire on genuinely dangerous targets (heavily armoured players, high-health / hard-hitting NPCs).
 * Also breathes continuously when it has taken to the air to chase a flying target — an aerial
 * duel should be fought with fire, not by flapping in for a bite.
 */
public class DragonBreathAttack extends GoalBehavior<TameableDragonEntity> {
    private static final int BATTLE_LONG_TICKS = 200;      // 10s
    private static final float STRONG_ARMOR = 8.0F;
    private static final float STRONG_MAX_HEALTH = 40.0F;
    private static final float STRONG_ATTACK = 7.0F;
    private static final double MAX_BREATH_RANGE_SQR = 32.0 * 32.0;
    private static final int BREATH_DURATION = 40;         // 2s
    private static final int BREATH_COOLDOWN = 40;         // 2s between bursts

    private long targetAcquiredAt = -1;
    private LivingEntity trackedTarget;
    private long lastBreathEnd = Long.MIN_VALUE;
    private boolean hasBreathedOnce = false;
    private long burstStart = -1;

    public DragonBreathAttack() {
    }

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        if (dragon.isControlledByPlayer()) return false;
        if (!dragon.breathHelper.canBreathe()) return false;
        if (!dragon.getLifeStage().isOldEnough(DragonLifeStage.FLEDGLING)) return false;

        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()) {
            this.targetAcquiredAt = -1;
            this.trackedTarget = null;
            return false;
        }

        if (target != this.trackedTarget) {
            this.trackedTarget = target;
            this.targetAcquiredAt = dragon.level().getGameTime();
        }

        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > MAX_BREATH_RANGE_SQR) return false;

        // AERIAL DUEL: when WE have taken to the air to fight a flying target, breathe constantly.
        // No threat-gating and no cooldown here — an airborne chase against a flying foe should be
        // a continuous stream of fire. (We are unridden; the isRiddenByPlayer check above already
        // excludes a player-controlled dragon.)
        if (dragon.isFlying() && isAirborne(target)) {
            return true;
        }

        // Otherwise apply the normal sparing cooldown. Overflow-safe: only gate after the first
        // burst (before that lastBreathEnd is Long.MIN_VALUE and the subtraction would overflow).
        long now = dragon.level().getGameTime();
        if (this.hasBreathedOnce && now - this.lastBreathEnd < BREATH_COOLDOWN) {
            return false;
        }

        return this.shouldBreathe(dragon, target, now);
    }

    /** Is the target meaningfully off the ground (another flying dragon, phantom, etc.)? */
    private boolean isAirborne(LivingEntity target) {
        if (target instanceof TameableDragonEntity d && d.isFlying()) return true;
        return !target.onGround() && target.fallDistance == 0.0F && !target.onClimbable();
    }

    private boolean shouldBreathe(TameableDragonEntity dragon, LivingEntity target, long now) {
        boolean armoured = target.getArmorValue() >= STRONG_ARMOR;
        boolean tanky = target.getMaxHealth() >= STRONG_MAX_HEALTH;
        boolean hardHitting = false;
        var attackAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            hardHitting = attackAttr.getValue() >= STRONG_ATTACK;
        }
        if (armoured || tanky || hardHitting) return true;

        boolean trivial = target.getMaxHealth() <= 20.0F && target.getArmorValue() <= 0;
        boolean longFight = this.targetAcquiredAt >= 0
                && (now - this.targetAcquiredAt) >= BATTLE_LONG_TICKS;
        return longFight && !trivial;
    }

    /**
     * Rotate the dragon's BODY (and head) to face the target. The breath fires along
     * dragon.getLookAngle(), which is derived from the body yRot/xRot — NOT the head. We aim from
     * the dragon's eye to the target's centre of mass so the breath connects, including pitching
     * up/down when the target is above or below us in the air.
     */
    private void faceTarget(TameableDragonEntity dragon, LivingEntity target) {
        Vec3 from = dragon.getEyePosition();
        Vec3 to = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float wantYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float wantPitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));

        float newYaw = approachDegrees(dragon.getYRot(), wantYaw, 25.0F);
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;
        // pitch tracks directly so it aims up/down at airborne targets; wider clamp for steep dives
        dragon.setXRot(Mth.clamp(wantPitch, -85.0F, 85.0F));
    }

    /** Move `current` toward `target` (degrees) by at most maxStep, wrapping correctly. */
    private static float approachDegrees(float current, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        return current + delta;
    }

    @Override
    protected void doStart(ServerLevel level, TameableDragonEntity dragon) {
        super.doStart(level, dragon);
        this.burstStart = level.getGameTime();
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target != null) {
            dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        }
        dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        dragon.getNavigation().stop();
        if (target != null) this.faceTarget(dragon, target);
        dragon.setBreathing(true);
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);

        boolean expired = this.burstStart < 0 || (time - this.burstStart) >= BREATH_DURATION;
        boolean targetGone = target == null || !target.isAlive()
                || dragon.distanceToSqr(target) > MAX_BREATH_RANGE_SQR;
        boolean controlled = dragon.isControlledByPlayer();

        // In an aerial duel we keep the burst going as long as the target stays in range, so the
        // 2s burst limit doesn't cut the fire stream short mid-chase.
        boolean aerialDuel = dragon.isFlying() && target != null && isAirborne(target);
        if ((expired && !aerialDuel) || targetGone || controlled) {
            this.doStop(level, dragon, time);
            return;
        }

        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        dragon.getNavigation().stop();
        this.faceTarget(dragon, target);   // keep the BODY aimed (yaw + pitch) at the target
        dragon.setBreathing(true);
    }

    @Override
    public void doStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        super.doStop(level, dragon, time);
        dragon.setBreathing(false);
        this.lastBreathEnd = time;
        this.hasBreathedOnce = true;
        this.burstStart = -1;
    }
}