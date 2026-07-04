package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMBlocks;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class NetherBreath extends FireBreath {
    public NetherBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        // Flammable blocks: set fire to them once they have been exposed enough.  After sufficient exposure, destroy the
        //   block (otherwise -if it's raining, the burning block will keep going out)
        // Non-flammable blocks:
        // 1) liquids (except lava) evaporate
        // 2) If the block can be smelted (eg sand), then convert the block to the smelted version
        // 3) If the block can't be smelted then convert to lava
        var pos = BlockPos.of(location);
        var state = level.getBlockState(pos);
        if (this.litBlock(level, pos, state)) return new BreathAffectedBlock();
        boolean consume = false;
        boolean disableIgniting = !ServerConfig.INSTANCE.ignitingBreath.get();
        boolean enableSmelting = ServerConfig.INSTANCE.smeltingBreath.get();
        if (enableSmelting || !disableIgniting) {
            var random = level.random;
            float max = 0.0F;
            for (var facing : Direction.values()) {
                float density = hit.getHitDensity(facing);
                if (density > max) {
                    max = density;
                }
                if (disableIgniting
                        || density < this.calcIgnitionThreshold(level, pos, state, facing)
                        || random.nextFloat() < 0.1875F
                ) continue;
                var sideToIgnite = pos.relative(facing);
                if (level.getBlockState(sideToIgnite).isAir()) {
                    consume = true;
                    this.burnBlock(level, sideToIgnite, random);
                    //    if (densityOfThisFace >= thresholdForDestruction && state.getBlockHardness(level, pos) != -1 && DragonMountsConfig.canFireBreathAffectBlocks) {
                    //   level.setBlockToAir(pos);
                }
            }
            if (enableSmelting && max > 0.25F) {
                consume = true;
                this.smeltBlock(level, pos, state);
            }
        }
        return consume ? new BreathAffectedBlock() : hit;
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        target.igniteForTicks(160);
        float damage = this.damage * hit.getHitDensity();
        if (target.isInPowderSnow || target.isFullyFrozen()) {
            damage *= 2.5F;
        } else if (target.isInWaterOrRain()) {
            damage *= 2.0F;
        }
        target.hurt(level.damageSources().mobAttack(this.dragon), damage);
    }

    protected void burnBlock(ServerLevel level, BlockPos sideToIgnite, RandomSource random) {
        BlockState fire = "soul_fire".equals(this.dragon.getVariant().identifier.getPath()) ||
         "soul_fire".equals(this.dragon.getVariant().identifier.getPath()) ||
         "skeleton".equals(this.dragon.getVariant().identifier.getPath()) ||
         "bogged".equals(this.dragon.getVariant().identifier.getPath()) ||
         "stray".equals(this.dragon.getVariant().identifier.getPath()) // had to manually put it here or now
                ? DMBlocks.BLUE_FIRE.get().defaultBlockState()
                : Blocks.FIRE.defaultBlockState();
        level.setBlockAndUpdate(sideToIgnite, fire);
        level.playSound(
                null,
                sideToIgnite.getX() + 0.5,
                sideToIgnite.getY() + 0.5,
                sideToIgnite.getZ() + 0.5,
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS,
                1.0F,
                0.8F + random.nextFloat() * 0.4F
        );
    }
}
