package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Autonomous fire-breath attack used by an UNRIDDEN dragon in combat. The dragon mostly
 * relies on its bite (vanilla MeleeAttack); it only resorts to breathing fire SPARINGLY,
 * when the fight justifies it:
 *
 *   - the battle has dragged on too long (BATTLE_LONG_TICKS since this dragon acquired the
 *     target), OR
 *   - the dragon is still healthy (>= HEALTHY_FRACTION of max health) — i.e. it can afford
 *     to spend a breath rather than conserving, OR
 *   - the target is "considerably strong or armoured" (high armour, high max health, or a
 *     strong attack-damage attribute).
 *
 * Even when a trigger is met, the breath is rate-limited by a cooldown so it stays an
 * occasional finisher, not a constant beam. While breathing, the dragon faces the target
 * (LOOK_TARGET) so the breath — which fires along the dragon's look direction — actually
 * hits. A player riding the dragon suppresses this entirely (they control the breath).
 *
 * Add to Activity.FIGHT alongside MeleeAttack.
 */
public class DragonBreathAttack extends GoalBehavior<ServerDragonEntity> {
    /** Fight considered "too long" once this many ticks pass since the target was acquired. */
    private static final int BATTLE_LONG_TICKS = 200;      // 10s
    /** Dragon is "still healthy" at or above this fraction of max health. */
    private static final float HEALTHY_FRACTION = 0.80F;
    /** A target is "armoured" at or above this armour value. */
    private static final float STRONG_ARMOR = 10.0F;
    /** A target is "tanky" at or above this max health. */
    private static final float STRONG_MAX_HEALTH = 40.0F;
    /** A target is "hard-hitting" at or above this attack-damage attribute. */
    private static final float STRONG_ATTACK = 6.0F;

    /** Only breathe within this range (breath is short-ranged; closer than melee disengage). */
    private static final double MAX_BREATH_RANGE_SQR = 18.0 * 18.0;
    private static final double MIN_BREATH_RANGE_SQR = 3.0 * 3.0;

    /** How long a single breath burst lasts. */
    private static final int BREATH_DURATION = 40;         // 2s of sustained breath
    /** Minimum ticks between breath bursts — keeps it sparing. */
    private static final int BREATH_COOLDOWN = 160;        // 8s between bursts

    /** Tick (gameTime) at which the current target was first seen, to measure battle length. */
    private long targetAcquiredAt = -1;
    private LivingEntity trackedTarget;
    /** gameTime when the last breath burst ended; gates the cooldown. */
    private long lastBreathEnd = Long.MIN_VALUE;
    /** gameTime when the current burst started; null/-1 when not breathing. */
    private long burstStart = -1;

    public DragonBreathAttack() {
    }

    @Override
    protected boolean canUse(ServerLevel level, ServerDragonEntity dragon) {
        // never auto-breathe while a player is in control
        if (dragon.isRiddenByPlayer()) return false;
        // must be physically able to breathe (life stage / type) — mirrors setBreathing's gate
        if (!dragon.breathHelper.canBreathe()) return false;

        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()) {
            // no target -> reset battle timer so a fresh fight starts its clock cleanly
            this.targetAcquiredAt = -1;
            this.trackedTarget = null;
            return false;
        }

        // (re)start the battle timer when the target changes
        if (target != this.trackedTarget) {
            this.trackedTarget = target;
            this.targetAcquiredAt = dragon.level().getGameTime();
        }

        double distSqr = dragon.distanceToSqr(target);
        if (distSqr > MAX_BREATH_RANGE_SQR || distSqr < MIN_BREATH_RANGE_SQR) return false;

        // cooldown gate — keeps breath sparing
        long now = dragon.level().getGameTime();
        if (now - this.lastBreathEnd < BREATH_COOLDOWN) return false;

        // at least one justification must hold
        return this.shouldBreathe(dragon, target, now);
    }

    /** The "is it worth a breath?" decision. */
    private boolean shouldBreathe(ServerDragonEntity dragon, LivingEntity target, long now) {
        // 1) battle has been taking too long
        boolean longFight = this.targetAcquiredAt >= 0 && (now - this.targetAcquiredAt) >= BATTLE_LONG_TICKS;

        // 2) dragon is still healthy (can afford to spend a breath)
        boolean healthy = dragon.getHealth() >= dragon.getMaxHealth() * HEALTHY_FRACTION;

        // 3) target is considerably strong or armoured
        boolean armoured = target.getArmorValue() >= STRONG_ARMOR;
        boolean tanky = target.getMaxHealth() >= STRONG_MAX_HEALTH;
        boolean hardHitting = false;
        var attackAttr = target.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            hardHitting = attackAttr.getValue() >= STRONG_ATTACK;
        }
        boolean strongTarget = armoured || tanky || hardHitting;

        return longFight || healthy || strongTarget;
    }

    @Override
    protected void doStart(ServerLevel level, ServerDragonEntity dragon) {
        super.doStart(level, dragon);
        this.burstStart = level.getGameTime();
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target != null) {
            // face the target so the breath (fired along look direction) connects
            dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        }
        dragon.setBreathing(true);
    }

    @Override
    public void tickOrStop(ServerLevel level, ServerDragonEntity dragon, long time) {
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);

        boolean expired = this.burstStart < 0 || (time - this.burstStart) >= BREATH_DURATION;
        boolean targetGone = target == null || !target.isAlive()
                || dragon.distanceToSqr(target) > MAX_BREATH_RANGE_SQR;
        boolean controlled = dragon.isRiddenByPlayer();

        if (expired || targetGone || controlled) {
            this.doStop(level, dragon, time);
            return;
        }

        // keep facing the target and keep the breath sustained
        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        dragon.setBreathing(true);
    }

    @Override
    public void doStop(ServerLevel level, ServerDragonEntity dragon, long time) {
        super.doStop(level, dragon, time);
        dragon.setBreathing(false);
        this.lastBreathEnd = time;     // start the cooldown
        this.burstStart = -1;
    }
}