package net.dragonmounts.neo.common.entity.ai.sensing;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public abstract class Anger {
    public LivingEntity target;
    public int timestamp;

    public abstract @Nullable LivingEntity updateTarget(@Nullable LivingEntity source, @Nullable LivingEntity fallback);

    public static class HurtBy extends Anger {
        @Override
        public @Nullable LivingEntity updateTarget(@Nullable LivingEntity source, @Nullable LivingEntity fallback) {
            if (source == null) return null;
            // No isTame() check here. `source` is whoever's grievance is being read, and for the
            // self-defence call that is the dragon itself -- so gating on tameness switched off a
            // tamed dragon's own retaliation entirely. Whether the resulting target is legitimate
            // is wantsToAttack's job, not this class's.
            int timestamp = source.getLastHurtByMobTimestamp();
            if (this.timestamp == timestamp) return fallback;
            this.timestamp = timestamp;
            return this.target = source.getLastHurtByMob();
        }
    }

    public static class Attack extends Anger {
        @Override
        public @Nullable LivingEntity updateTarget(@Nullable LivingEntity source, @Nullable LivingEntity fallback) {
            if (source == null) return null;
            // Same reasoning as HurtBy: filtering belongs in wantsToAttack, not here.
            int timestamp = source.getLastHurtMobTimestamp();
            if (this.timestamp == timestamp) return fallback;
            this.timestamp = timestamp;
            return this.target = source.getLastHurtMob();
        }
    }
}
