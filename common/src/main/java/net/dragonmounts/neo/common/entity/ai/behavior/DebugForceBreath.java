package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * DEBUG behavior: forces the dragon to breathe fire at its attack target with NO conditions
 * and NO cooldown — used to verify the breath system actually fires in combat. Faces the
 * target and holds setBreathing(true) while there's a valid ATTACK_TARGET in range.
 *
 * Typed on TameableDragonEntity (the parent) to match the proven GoalBehavior pattern used by
 * DragonFollowPlayerFlying, so it conforms cleanly as BehaviorControl in the activity list.
 * Everything it needs (breathHelper, getLifeStage, setBreathing, isRiddenByPlayer, getBrain)
 * lives on TameableDragonEntity / LivingEntity.
 *
 * Testing only — once breathing is confirmed, swap back to DragonBreathAttack + re-enable bite.
 */
public class DebugForceBreath extends GoalBehavior<TameableDragonEntity> {
    private static final double MAX_RANGE_SQR = 40.0 * 40.0;

    public DebugForceBreath() {
    }

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        if (dragon.isRiddenByPlayer()) return false;
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()) return false;
        boolean canBreathe = dragon.breathHelper.canBreathe();
        boolean oldEnough = dragon.getLifeStage().isOldEnough(DragonLifeStage.FLEDGLING);
        boolean inRange = dragon.distanceToSqr(target) <= MAX_RANGE_SQR;
        System.out.println("[DebugForceBreath] target=" + target.getName().getString()
                + " canBreathe=" + canBreathe + " oldEnough=" + oldEnough
                + " inRange=" + inRange + " dist=" + Math.sqrt(dragon.distanceToSqr(target)));
        return canBreathe && oldEnough && inRange;
    }

    @Override
    protected void doStart(ServerLevel level, TameableDragonEntity dragon) {
        super.doStart(level, dragon);
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target != null) {
            dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        }
        dragon.setBreathing(true);
        System.out.println("[DebugForceBreath] doStart -> setBreathing(true), isBreathing=" + dragon.isBreathing());
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        LivingEntity target = dragon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (target == null || !target.isAlive()
                || dragon.distanceToSqr(target) > MAX_RANGE_SQR
                || dragon.isRiddenByPlayer()) {
            this.doStop(level, dragon, time);
            return;
        }
        dragon.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        dragon.setBreathing(true);
    }

    @Override
    public void doStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        super.doStop(level, dragon, time);
        dragon.setBreathing(false);
        System.out.println("[DebugForceBreath] doStop -> setBreathing(false)");
    }
}