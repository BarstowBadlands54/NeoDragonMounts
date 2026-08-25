package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.breath.BreathNode;
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
 * Autonomous breath attack for an unridden dragon. Trivial foes get bitten; dangerous ones get
 * breathed on. An aerial duel is fought with a continuous stream rather than by flapping in for
 * a bite.
 */
public class DragonBreathAttack extends GoalBehavior<TameableDragonEntity> {
    private static final int BATTLE_LONG_TICKS = 200;      // 10s
    private static final float STRONG_ARMOR = 8.0F;
    private static final float STRONG_MAX_HEALTH = 40.0F;
    private static final float STRONG_ATTACK = 7.0F;
    private static final double MAX_BREATH_RANGE_SQR = 32.0 * 32.0;
    /**
     * Cut a burst short if a ground target closes inside this while the dragon is airborne, so
     * DragonAerialCombat can re-establish its standoff. Holding position through a burst lets a
     * melee foe walk in underneath.
     */
    private static final double BREAK_OFF_RANGE_SQR = 10.0 * 10.0;
    /** Ceiling on how far ahead of a target the aim will lead, in ticks. */
    private static final double MAX_LEAD_TICKS = 12.0;
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
        if (dragon.isBeingRiddenByPlayer()) return false;
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

        // Aerial duel: no threat gate and no cooldown, an airborne chase is a continuous stream.
        if (dragon.isFlying() && isAirborne(target)) {
            return true;
        }

        // Otherwise the normal sparing cooldown. Only gated after the first burst: before that
        // lastBreathEnd is Long.MIN_VALUE and the subtraction would overflow.
        long now = dragon.level().getGameTime();
        if (this.hasBreathedOnce && now - this.lastBreathEnd < BREATH_COOLDOWN) {
            return false;
        }

        return this.shouldBreathe(dragon, target, now);
    }

    /** Is the target meaningfully off the ground? */
    private boolean isAirborne(LivingEntity target) {
        if (target instanceof TameableDragonEntity d && d.isFlying()) return true;
        return !target.onGround() && target.fallDistance == 0.0F && !target.onClimbable();
    }

    /**
     * Is this target worth spending breath on, and -- to {@link DragonAerialCombat} -- worth
     * refusing to melee at all? Shared so the two cannot drift apart on what counts as dangerous.
     */
    public static boolean isDangerous(LivingEntity target) {
        if (target.getArmorValue() >= STRONG_ARMOR) return true;
        if (target.getMaxHealth() >= STRONG_MAX_HEALTH) return true;
        var attackAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
        return attackAttr != null && attackAttr.getValue() >= STRONG_ATTACK;
    }

    private boolean shouldBreathe(TameableDragonEntity dragon, LivingEntity target, long now) {
        if (isDangerous(target)) return true;

        boolean trivial = target.getMaxHealth() <= 20.0F && target.getArmorValue() <= 0;
        boolean longFight = this.targetAcquiredAt >= 0
                && (now - this.targetAcquiredAt) >= BATTLE_LONG_TICKS;
        return longFight && !trivial;
    }

    /**
     * Rotate the dragon's body to face the target. The breath fires along
     * {@code getLookAngle()}, which reads body rotation rather than the head, so the aim runs
     * from the eye to the target's centre of mass and pitches with it.
     */
    private void faceTarget(TameableDragonEntity dragon, LivingEntity target) {
        Vec3 from = dragon.getEyePosition();
        Vec3 centre = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);

        // Lead the target: the stream is not instant, so aiming where it stands now puts the line
        // behind anything moving across the line of fire. The exact intercept is a quadratic; two
        // refinement passes converge close enough at these speeds.
        double nodeSpeed = BreathNode.getStartingSpeed(dragon.getLifeStage().power);
        Vec3 velocity = target.getDeltaMovement();
        Vec3 to = centre;
        if (nodeSpeed > 1.0E-4) {
            for (int i = 0; i < 2; ++i) {
                double lead = Math.min(from.distanceTo(to) / nodeSpeed, MAX_LEAD_TICKS);
                to = centre.add(velocity.scale(lead));
            }
        }

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
        // Pitch is unclamped: atan2 already bounds it to +/-90, and anything narrower cannot
        // reach a target directly below. DragonHeadLocator draws a clamped pose regardless.
        dragon.setXRot(wantPitch);
    }

    /** Move {@code current} toward {@code target}, in degrees, by at most maxStep. */
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
        boolean controlled = dragon.isBeingRiddenByPlayer();

        // an aerial duel runs as long as the target stays in range, ignoring the burst limit
        boolean aerialDuel = dragon.isFlying() && target != null && isAirborne(target);
        // A ground foe that has closed underneath cuts the burst short; the alternative is
        // hovering for the full duration while it beats on the dragon.
        boolean crowded = dragon.isFlying() && target != null && !isAirborne(target)
                && dragon.distanceToSqr(target) < BREAK_OFF_RANGE_SQR;
        if ((expired && !aerialDuel) || targetGone || controlled || crowded) {
            this.doStop(level, dragon, time);
            return;
        }

        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        dragon.getNavigation().stop();
        this.faceTarget(dragon, target);
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