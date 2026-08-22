package net.dragonmounts.neo.common.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.dragonmounts.neo.common.init.DMMemories;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.Enemy;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

public class DragonTargetSensor extends NearestLivingEntitySensor<ServerDragonEntity> {
    public static boolean takeIfAttackable(
            ServerLevel level,
            Brain<?> brain,
            ServerDragonEntity dragon,
            @Nullable LivingEntity target,
            @Nullable LivingEntity owner
    ) {
        if (target != null && dragon.wantsToAttack(target, owner) && Sensor.isEntityAttackable(dragon, target)) {
            brain.setMemory(MemoryModuleType.NEAREST_ATTACKABLE, target);
            return true;
        }
        return false;
    }

    /**
     * Like {@link #takeIfAttackable}, but for REVENGE (the entity that just hurt the dragon).
     * Deliberately skips {@link Sensor#isEntityAttackable}, because that check requires the
     * target to already be in NEAREST_VISIBLE_LIVING_ENTITIES — which a player who just hit the
     * dragon usually isn't yet (players are tracked separately). Being hit is proof enough that
     * the attacker is a valid target, so the dragon retaliates against players too, not just
     * hostile mobs. Still gated by wantsToAttack so it never turns on its owner or their allies.
     */
    public static boolean takeIfHurtBy(
            Brain<?> brain,
            ServerDragonEntity dragon,
            @Nullable LivingEntity target,
            @Nullable LivingEntity owner
    ) {
        if (target != null && target.isAlive() && target != dragon && dragon.wantsToAttack(target, owner)) {
            brain.setMemory(MemoryModuleType.NEAREST_ATTACKABLE, target);
            return true;
        }
        return false;
    }

    private final Anger.HurtBy selfHurtBy = new Anger.HurtBy();
    private final Anger.HurtBy ownerHurtBy = new Anger.HurtBy();
    private final Anger.Attack ownerTarget = new Anger.Attack();

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.copyOf(Iterators.concat(super.requires().iterator(), Iterators.forArray(MemoryModuleType.NEAREST_ATTACKABLE)));
    }

    @Override
    protected void doTick(ServerLevel level, ServerDragonEntity dragon) {
        super.doTick(level, dragon);
        var brain = dragon.getBrain();
        var owner = brain.getMemory(DMMemories.FOLLOWABLE_OWNER).orElse(null);
        var current = brain.getMemory(MemoryModuleType.NEAREST_ATTACKABLE).orElse(null);

        // REVENGE first, and without the visibility gate, so the dragon fights back against
        // whoever (or whatever) just hit it — players included.
        var selfAttacker = this.selfHurtBy.updateTarget(dragon, current);
        if (takeIfHurtBy(brain, dragon, selfAttacker, owner)) return;
        if (takeIfHurtBy(brain, dragon, this.ownerHurtBy.updateTarget(owner, current), owner)) return;
        // Arguments were transposed here: takeIfAttackable takes (target, owner), so passing
        // `owner` fourth asked "should I attack my owner?" instead of "should I help my owner
        // fight what they are fighting?".
        if (takeIfAttackable(level, brain, dragon, this.ownerTarget.updateTarget(owner, current), owner)) return;

        // Otherwise look for a nearby hostile (Enemy) to attack proactively.
        var nearestEnemy = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
                .stream()
                .flatMap(Collection::stream)
                .filter(target -> target instanceof Enemy && Sensor.isEntityAttackable(dragon, target))
                .findFirst();
        if (nearestEnemy.isPresent()) {
            dragon.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, nearestEnemy.get());
            return;
        }

        // No hostile nearby. Only clear the target if the CURRENT one is no longer valid —
        // i.e. don't wipe a revenge target (e.g. a player) just because they aren't an Enemy.
        // Keep it while it's alive and the dragon still wants to attack it; otherwise erase.
        if (current == null || !current.isAlive() || !dragon.wantsToAttack(current, owner)) {
            dragon.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE);
        }
    }
}