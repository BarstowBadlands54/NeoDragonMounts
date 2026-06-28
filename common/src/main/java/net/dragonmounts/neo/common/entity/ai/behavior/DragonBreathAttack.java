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
 *
 * DIAGNOSTIC BUILD: prints to the server console on every canUse evaluation so we can see which
 * gate is blocking the breath. Remove the System.out lines once it's confirmed working.
 */
public class DragonBreathAttack extends GoalBehavior<TameableDragonEntity> {
    private static final int BATTLE_LONG_TICKS = 200;      // 10s
    private static final float STRONG_ARMOR = 8.0F;
    private static final float STRONG_MAX_HEALTH = 40.0F;
    private static final float STRONG_ATTACK = 7.0F;
    private static final double MAX_BREATH_RANGE_SQR = 32.0 * 32.0;
    private static final int BREATH_DURATION = 40;         // 2s
    private static final int BREATH_COOLDOWN = 160;        // 8s

    private long targetAcquiredAt = -1;
    private LivingEntity trackedTarget;
    private long lastBreathEnd = Long.MIN_VALUE;
    private boolean hasBreathedOnce = false;
    private long burstStart = -1;

    public DragonBreathAttack() {
    }

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        if (dragon.isRiddenByPlayer()) { return false; }
        if (!dragon.breathHelper.canBreathe()) {
            System.out.println("[BreathAI] blocked: canBreathe=false (breath not initialized for this type)");
            return false;
        }
        if (!dragon.getLifeStage().isOldEnough(DragonLifeStage.FLEDGLING)) {
            System.out.println("[BreathAI] blocked: too young, stage=" + dragon.getLifeStage());
            return false;
        }

        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()) {
            System.out.println("[BreathAI] blocked: no ATTACK_TARGET (dragon isn't in FIGHT against anyone)");
            this.targetAcquiredAt = -1;
            this.trackedTarget = null;
            return false;
        }

        if (target != this.trackedTarget) {
            this.trackedTarget = target;
            this.targetAcquiredAt = dragon.level().getGameTime();
        }

        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > MAX_BREATH_RANGE_SQR) {
            System.out.println("[BreathAI] blocked: out of range, dist=" + Math.sqrt(distSqr));
            return false;
        }

        long now = dragon.level().getGameTime();
        // Overflow-safe cooldown: only gate AFTER the first burst. Before that, lastBreathEnd is
        // unset and (now - Long.MIN_VALUE) would overflow into a bogus huge value.
        if (this.hasBreathedOnce && now - this.lastBreathEnd < BREATH_COOLDOWN) {
            System.out.println("[BreathAI] blocked: on cooldown, " + (BREATH_COOLDOWN - (now - this.lastBreathEnd)) + " ticks left");
            return false;
        }

        boolean decision = this.shouldBreathe(dragon, target, now);
        System.out.println("[BreathAI] target=" + target.getName().getString()
                + " armor=" + target.getArmorValue()
                + " maxHp=" + target.getMaxHealth()
                + " atk=" + (target.getAttribute(Attributes.ATTACK_DAMAGE) != null ? target.getAttribute(Attributes.ATTACK_DAMAGE).getValue() : "n/a")
                + " dist=" + Math.sqrt(distSqr)
                + " -> shouldBreathe=" + decision);
        return decision;
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
     * dragon.getLookAngle(), which is derived from the body yRot/xRot — NOT the head. The
     * LookAtTargetSink only turns the head, so without this the breath would shoot wherever the
     * body happens to point. We aim from the dragon's eye to the target's centre of mass.
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

        // smooth the yaw turn a little so it doesn't snap; pitch can track directly
        float newYaw = approachDegrees(dragon.getYRot(), wantYaw, 25.0F);
        dragon.setYRot(newYaw);
        dragon.yBodyRot = newYaw;
        dragon.yHeadRot = newYaw;
        dragon.setXRot(Mth.clamp(wantPitch, -75.0F, 75.0F));
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
        System.out.println("[BreathAI] FIRING breath -> isBreathing=" + dragon.isBreathing());
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);

        boolean expired = this.burstStart < 0 || (time - this.burstStart) >= BREATH_DURATION;
        boolean targetGone = target == null || !target.isAlive()
                || dragon.distanceToSqr(target) > MAX_BREATH_RANGE_SQR;
        boolean controlled = dragon.isRiddenByPlayer();

        if (expired || targetGone || controlled) {
            this.doStop(level, dragon, time);
            return;
        }

        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        dragon.getNavigation().stop();
        this.faceTarget(dragon, target);   // keep the BODY aimed so the breath tracks the target
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