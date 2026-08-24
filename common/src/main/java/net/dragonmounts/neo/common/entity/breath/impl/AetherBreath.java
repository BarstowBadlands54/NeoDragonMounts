package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.common.tag.DMBlockTags;
import net.dragonmounts.neo.compat.platform.FlammableBlock;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_15;

/**
 * Created by TGG on 7/12/2015.
 * <p>
 * Fire handling is governed by {@code aetherExtinguishesFire}. On (the default) the airflow snuffs
 * flames out. Off, it behaves like blowing on a campfire: the flame is fed rather than smothered,
 * and a sustained blast pushes it into whatever is downwind.
 */
public class AetherBreath extends DragonBreath {
    public static float getDestroyDensity(ServerLevel level, BlockPos pos, BlockState state) {
        var time = state.getDestroySpeed(level, pos);
        return time * time * 400;
    }

    protected static final float FAN_THRESHOLD = 0.3F;
    protected static final float FLARE_THRESHOLD = FAN_THRESHOLD * 3.0F;
    protected static final int FANNED_BURN_TICKS = 100;

    public AetherBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        var pos = BlockPos.of(location);
        var state = level.getBlockState(pos);
        boolean extinguishes = ServerConfig.INSTANCE.aetherExtinguishesFire.get();
        // Fire is settled here in both modes and returns early. It has to be: #minecraft:fire is a
        // member of AIRFLOW_DESTRUCTIBLE and fire's destroy speed is 0, so getDestroyDensity
        // returns 0 and the destructive branch below blew flames out at any hit density at all --
        // which silently defeated this config whenever it was switched off.
        if (isFire(state)) {
            return extinguishes
                    ? this.extinguishFire(level, pos)
                    : this.fanFlames(level, pos, state, hit);
        }
        if (extinguishes) {
            if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
                return new BreathAffectedBlock();
            }
            if (CampfireBlock.isLitCampfire(state)) {
                level.levelEvent(null, 1009, pos, 0);
                level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
                return new BreathAffectedBlock();
            }
            // In spreading mode lit candles and campfires are left burning -- air feeds them. They
            // are not lit from cold either, since airflow carries no ignition source of its own.
        }
        if (ServerConfig.INSTANCE.destructiveBreath.get() && state.is(DMBlockTags.AIRFLOW_DESTRUCTIBLE)) {
            if (hit.getMaxHitDensity() > getDestroyDensity(level, pos, state)) {
                level.destroyBlock(pos, true, this.dragon);
                return new BreathAffectedBlock();
            }
        }
        return hit;
    }

    /**
     * Soul fire and the mod's own blue fire are BaseFireBlock but are not in {@code #minecraft:fire},
     * so the tag alone missed them; the tag is still checked so modded fires that are not
     * BaseFireBlock are caught too.
     */
    protected static boolean isFire(BlockState state) {
        return state.getBlock() instanceof BaseFireBlock || state.is(BlockTags.FIRE);
    }

    protected BreathAffectedBlock extinguishFire(ServerLevel level, BlockPos pos) {
        level.destroyBlock(pos, true, this.dragon);
        return new BreathAffectedBlock();
    }

    /**
     * Feeds a flame instead of smothering it.
     * <p>
     * Two things happen. The flame is reset to a fresh burn, because vanilla fire ages out and
     * dies even when standing on fuel and a fanned fire should not; and the fire is pushed into
     * open ground on the far side from the blast. The face carrying the most breath is the one the
     * air lands on, so the spread runs out of the opposite side the way a draught carries a flame.
     *
     * @return a reset hit if anything caught, otherwise the accumulating hit
     */
    protected BreathAffectedBlock fanFlames(ServerLevel level, BlockPos pos, BlockState state, BreathAffectedBlock hit) {
        float max = hit.getMaxHitDensity();
        if (max < FAN_THRESHOLD) return hit;
        boolean changed = false;
        if (state.hasProperty(AGE_15) && state.getValue(AGE_15) > 0) {
            level.setBlock(pos, state.setValue(AGE_15, 0), 3);
            changed = true;
        }
        var spread = state.getBlock().defaultBlockState();
        var random = level.random;
        for (var facing : Direction.values()) {
            if (hit.getHitDensity(facing) < FAN_THRESHOLD) continue;
            if (this.igniteIfPossible(level, pos.relative(facing.getOpposite()), spread, random)) {
                changed = true;
            }
        }
        if (max >= FLARE_THRESHOLD) {
            for (var facing : Direction.values()) {
                if (random.nextBoolean()) continue;
                if (this.igniteIfPossible(level, pos.relative(facing), spread, random)) {
                    changed = true;
                }
            }
        }
        return changed ? new BreathAffectedBlock() : hit;
    }

    protected boolean igniteIfPossible(ServerLevel level, BlockPos pos, BlockState fire, RandomSource random) {
        if (!canHoldFire(level, pos)) return false;
        level.setBlockAndUpdate(pos, fire);
        if (random.nextInt(4) == 0) {   // one in four, or a wide blast is a wall of noise
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.8F + random.nextFloat() * 0.4F);
        }
        return true;
    }

    /**
     * Mirrors FireBlock#canSurvive -- a sturdy floor underneath, or fuel on any side -- without
     * reaching for vanilla's protected block methods. This is what keeps the airflow from leaving
     * flames hanging in mid-air along the beam.
     */
    protected static boolean canHoldFire(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        var floorPos = pos.below();
        var floor = level.getBlockState(floorPos);
        if (floor.isFaceSturdy(level, floorPos, Direction.UP)) return true;
        for (var facing : Direction.values()) {
            var neighbourPos = pos.relative(facing);
            var neighbour = level.getBlockState(neighbourPos);
            if (FlammableBlock.getFlammability(level, neighbourPos, neighbour, facing.getOpposite()) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        if (target.isOnFire()) {
            if (ServerConfig.INSTANCE.aetherExtinguishesFire.get()) {
                target.clearFire();
            } else {
                // Fanned, not ignited. Air carries no ignition source, so a target that is not
                // already alight is untouched -- the blast only keeps an existing burn going.
                target.igniteForTicks(FANNED_BURN_TICKS);
            }
        }
        TameableDragonEntity dragon = this.dragon;

        //    System.out.format("Old entity motion:[%.2f, %.2f, %.2f]\n", entity.motionX, entity.motionY, entity.motionZ);
        // push in the direction of the wind, but add a vertical upthrust as well
        final double FORCE_MULTIPLIER = 0.05;
        final double VERTICAL_FORCE_MULTIPLIER = 0.05;
        float density = hit.getHitDensity();
        var direction = hit.getHitDirection();
//        Vec3d airForceDirection = hit.getHitDensityDirection();
//        Vec3d airMotion = MathX.multiply(airForceDirection, FORCE_MULTIPLIER);
        final double WT_ENTITY = 0.05;
        final double WT_AIR = 1 - WT_ENTITY;
        target.hurt(level.damageSources().mobAttack(dragon), this.damage * density);
        target.knockback(0.1F * density, -direction.x, -direction.z);
        /*
        if (density > 1.0) {
            final double GRAVITY_OFFSET = -0.08;
            target.motionY = WT_ENTITY * (target.motionY - GRAVITY_OFFSET) + WT_AIR * VERTICAL_FORCE_MULTIPLIER * density;
        }*/
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_AIRFLOW;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_AIRFLOW;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_AIRFLOW;
    }
}
