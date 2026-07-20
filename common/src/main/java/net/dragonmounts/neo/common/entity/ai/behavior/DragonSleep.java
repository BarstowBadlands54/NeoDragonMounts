package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMMemories;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class DragonSleep extends GoalBehavior<TameableDragonEntity> {

    public DragonSleep() {
    }

    @Override
    protected boolean canUse(ServerLevel level, TameableDragonEntity entity) {
        return true;
    }

    @Override
    public void tickOrStop(ServerLevel level, TameableDragonEntity dragon, long time) {
        if (!dragon.isSleeping()) {
            boolean canDoze = (dragon.isTame() ? dragon.isInSittingPose() : true)
                    && dragon.getPassengers().isEmpty(); // don't nod off mid-ride
            if (canDoze && dragon.getRandom().nextInt(1200) == 0) {
                dragon.setSleeping(true);
                dragon.getBrain().setMemory(DMMemories.IS_SLEEPING, Unit.INSTANCE);
            }
        } else {
            boolean gotHurt = dragon.getLastHurtByMob() != null;
            if (gotHurt || dragon.getRandom().nextInt(1200) == 0) {
                dragon.setSleeping(false);
                dragon.getBrain().eraseMemory(DMMemories.IS_SLEEPING);
                if (gotHurt) dragon.setInSittingPose(false);
            } else {
                // Freeze in place: CORE's MoveToTargetSink / LookAtTargetSink run every tick
                // regardless of which non-core activity is active, so any leftover WALK_TARGET
                // or LOOK_TARGET memory from before falling asleep would still be acted on.
                dragon.getNavigation().stop();
                dragon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                dragon.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
        }
    }
}