package net.dragonmounts.neo.common.entity.ai.behavior;

import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

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
            boolean canDoze = dragon.isTame() ? dragon.isInSittingPose() : true;
            if (canDoze && dragon.getRandom().nextInt(1200) == 0) {
                dragon.setSleeping(true);
            }
        } else {
            boolean gotHurt = dragon.getLastHurtByMob() != null;
            if (gotHurt || dragon.getRandom().nextInt(1200) == 0) {
                dragon.setSleeping(false);
                if (gotHurt) dragon.setInSittingPose(false); // stand it up if it was hit while asleep
            }
        }
    }
}
