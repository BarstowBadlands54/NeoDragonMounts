package net.dragonmounts.neo.common.entity.breath;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static net.dragonmounts.neo.common.entity.breath.impl.ServerBreathHelper.*;

/**
 * Created by TGG on 5/08/2015.
 */
public abstract class DragonBreath {
    /**
     * @param pos the block the breath struck. The cloud is placed on TOP of it: an AreaEffectCloud
     *            is anchored at its feet, so spawning at pos.getY() buries it inside the block it
     *            hit and the visible gas ends up under the surface. Callers pass the hit block, not
     *            the space above it, so the offset belongs here rather than at every call site.
     */
    public static AreaEffectCloud createEffectCloud(ServerLevel level, BlockPos pos, float radius, int duration) {
        var cloud = new AreaEffectCloud(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick((1.0F - radius) / duration);
        return cloud;
    }

    protected final TameableDragonEntity dragon;
    public final float damage;

    public DragonBreath(TameableDragonEntity dragon, float damage) {
        this.dragon = dragon;
        this.damage = damage;
    }

    /** @see TameableDragonEntity#getMuzzlePosition() */
    public Vec3 getSpawnPosition() {
        return this.dragon.getMuzzlePosition();
    }

    /**
     * Direction the stream travels. Defaults to the dragon's aim, which is the rider's
     * crosshair while ridden and the dragon's own look angle otherwise. Override to give a
     * breed a spread, an arc, or a fixed muzzle direction.
     */
    public Vec3 getAimVector() {
        return this.dragon.getAimVector();
    }

    public void collide(
            ServerDragonEntity dragon,
            boolean breathing,
            List<BreathNodeEntity> nodes,
            Long2ObjectMap<BreathAffectedBlock> affectedBlocks,
            Map<LivingEntity, BreathAffectedEntity> affectedEntities
    ) {
        if (breathing) {
            nodes.add(new BreathNodeEntity(dragon, this.getSpawnPosition(), this.getAimVector()));
        }
        if (nodes.isEmpty()) return;
        var level = (ServerLevel) dragon.level();
        updateBlockAndEntityHitDensities(level, this, nodes, affectedBlocks, affectedEntities);
        implementEffectsOnBlocksTick(this, level, affectedBlocks);
        implementEffectsOnEntitiesTick(this, level, affectedEntities);
        // decay the hit densities of the affected blocks and entities (eg for flame weapon - cools down)
        Predicate<BreathEffectHandler> predicate = BreathEffectHandler::decayEffectTick;
        affectedBlocks.values().removeIf(predicate);
        affectedEntities.values().removeIf(predicate);
    }

    /**
     * if the hitDensity is high enough, manipulate the block (eg set fire to it)
     *
     * @return the updated block hit density
     */
    public abstract BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit);

    /**
     * Lay down short-lived FLOWING water (level 1) on an empty spot at or near {@code hitPos},
     * then schedule a fluid tick so vanilla's flowing-fluid logic spreads it and lets it drain.
     * <p>
     * Only ever flowing water, never a source: with nothing feeding it the puddle recedes on its
     * own, so it can't leave a permanent pool behind. This lived in WaterBreath; it is here so
     * fire breath can melt ice and snow into exactly the same temporary layers.
     *
     * @return true if water was placed
     */
    protected boolean spreadTemporaryWater(ServerLevel level, BlockPos hitPos) {
        BlockPos place = findWaterSpot(level, hitPos);
        if (place == null) return false;
        level.setBlock(place, Blocks.WATER.defaultBlockState().setValue(LEVEL, 1), 3);
        // tick it so it starts spreading and draining instead of sitting static
        level.scheduleTick(place, Fluids.WATER, 5);
        return true;
    }

    /// A nearby empty, water-holding position (the hit, the block above, or below), or null.
    protected @Nullable BlockPos findWaterSpot(ServerLevel level, BlockPos hitPos) {
        for (BlockPos candidate : new BlockPos[]{hitPos.above(), hitPos, hitPos.below()}) {
            var state = level.getBlockState(candidate);
            if (!level.getFluidState(candidate).isEmpty()) continue;
            if (!state.isAir() && !state.canBeReplaced()) continue;
            // needs something to rest on, or the puddle just falls forever
            var below = level.getBlockState(candidate.below());
            if (below.isFaceSturdy(level, candidate.below(), Direction.UP)
                    || !below.getFluidState().isEmpty()) return candidate;
        }
        return null;
    }

    public boolean canAffect(LivingEntity entity) {
        return !this.dragon.isPassengerOfSameVehicle(entity);
    }

    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        target.hurt(level.damageSources().mobAttack(this.dragon), this.damage * hit.getHitDensity());
    }

    public SoundEvent getStartSound(DragonLifeStage stage) {
        return switch (stage) {
            case ADULT -> DMSounds.DRAGON_BREATH_START_ADULT;
            case JUVENILE -> DMSounds.DRAGON_BREATH_START_JUVENILE;
            default -> DMSounds.DRAGON_BREATH_START_HATCHLING;
        };
    }

    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return switch (stage) {
            case ADULT -> DMSounds.DRAGON_BREATH_LOOP_ADULT;
            case JUVENILE -> DMSounds.DRAGON_BREATH_LOOP_JUVENILE;
            default -> DMSounds.DRAGON_BREATH_LOOP_HATCHLING;
        };
    }

    public SoundEvent getStopSound(DragonLifeStage stage) {
        return switch (stage) {
            case ADULT -> DMSounds.DRAGON_BREATH_STOP_ADULT;
            case JUVENILE -> DMSounds.DRAGON_BREATH_STOP_JUVENILE;
            default -> DMSounds.DRAGON_BREATH_STOP_HATCHLING;
        };
    }
}
