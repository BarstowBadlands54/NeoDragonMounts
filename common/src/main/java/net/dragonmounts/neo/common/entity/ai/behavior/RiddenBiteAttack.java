package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;

/**
 * Lets a ridden dragon bite whatever gets close enough to bite it.
 * <p>
 * Everything that made a dragon fight -- DragonAerialCombat, DragonBreathAttack, and the FIGHT
 * activity's own MeleeAttack -- stands down the moment a player mounts, and DragonAi.tickBrain
 * forces the CONTROLLED activity, which runs nothing but ControlledByPlayer. A ridden dragon was
 * therefore completely passive: a pack of zombies or spiders could surround it and chew on it
 * while it did nothing at all, because the rider is expected to do the fighting and the rider has
 * one crosshair.
 * <p>
 * This lives in CORE so it survives that activity switch, and it is deliberately the smallest
 * thing that fixes the problem. It never steers, never sets a walk or attack target, and never
 * takes a memory -- the rider keeps full control of where the dragon goes and what it aims at.
 * All it does is snap at something already within reach, on a cooldown.
 */
public class RiddenBiteAttack extends GoalBehavior<TameableDragonEntity> {
    /** How far past its own bounding box the dragon will snap. Its box already scales with age. */
    private static final double BITE_REACH = 2.0;
    /** Ticks between bites. Deliberately shorter than the FIGHT activity's MeleeAttack cooldown,
     *  since this is self-defence against a crowd rather than a duel, but still one target a bite. */
    private static final int BITE_COOLDOWN = 15;

    private long lastBite = Long.MIN_VALUE;

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity dragon) {
        // Unridden combat is already handled by the FIGHT activity; running here as well would
        // just give the dragon two independent bite timers.
        if (!dragon.isBeingRiddenByPlayer()) return false;
        long now = level.getGameTime();
        // Overflow-safe: lastBite starts at Long.MIN_VALUE, so subtract in this direction only.
        if (this.lastBite != Long.MIN_VALUE && now - this.lastBite < BITE_COOLDOWN) return false;
        return findVictim(level, dragon) != null;
    }

    /**
     * Closest valid thing in reach, or null.
     * <p>
     * Hostiles are fair game on sight; anything else has to have drawn blood first. Without that
     * second rule a ridden dragon would eat every cow and villager it flew past.
     */
    private static LivingEntity findVictim(ServerLevel level, TameableDragonEntity dragon) {
        var owner = dragon.getOwner();
        var attacker = dragon.getLastHurtByMob();
        return level.getEntitiesOfClass(
                LivingEntity.class,
                dragon.getBoundingBox().inflate(BITE_REACH),
                target -> target != dragon
                        && target.isAlive()
                        && !dragon.hasPassenger(target)          // never the rider
                        && (target instanceof Enemy || target == attacker)
                        && dragon.wantsToAttack(target, owner)   // never the owner or another pet
        ).stream().min(Comparator.comparingDouble(dragon::distanceToSqr)).orElse(null);
    }

    @Override
    protected void doStart(ServerLevel level, TameableDragonEntity dragon) {
        super.doStart(level, dragon);
        var victim = findVictim(level, dragon);
        if (victim == null) return;
        this.lastBite = level.getGameTime();
        // doHurtTarget broadcasts ON_ATTACK, so the bite animation and sound come along for free.
        dragon.doHurtTarget(victim);
    }

    /**
     * One bite per activation. Brain#tick starts non-running behaviours before ticking running
     * ones, so returning false here stops this again on the same tick it fired and the cooldown
     * in canUse is the only thing gating the next one.
     */
    @Override
    protected boolean canContinueToUse(ServerLevel level, TameableDragonEntity dragon) {
        return false;
    }
}
