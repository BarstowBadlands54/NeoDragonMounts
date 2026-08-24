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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LAYERS;

public class NetherBreath extends FireBreath {
    public NetherBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {

        var pos = BlockPos.of(location);
        var state = level.getBlockState(pos);
        if (this.litBlock(level, pos, state)) return new BreathAffectedBlock();
        if (this.meltBlock(level, pos, state, hit)) return new BreathAffectedBlock();
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
;
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
         "legacy_skeleton".equals(this.dragon.getVariant().identifier.getPath()) ||
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

    /// Hit density needed before ice or snow gives way. Roughly a second of sustained breath.
    protected static final float MELT_THRESHOLD = 0.4F;

    /**
     * Melt ice and snow into the same temporary flowing water the water breath lays down.
     * <p>
     * Snow layers thaw one layer at a time so a deep drift takes a moment; everything else goes
     * in one hit. The water placed is never a source, so the melt leaves puddles that drain
     * rather than permanently flooding the terrain.
     *
     * @return true if something melted, in which case the caller resets the hit density
     */
    protected boolean meltBlock(ServerLevel level, BlockPos pos, BlockState state, BreathAffectedBlock hit) {
        if (!isMeltable(state)) return false;
        // Reuses the existing fire-breath block switches rather than adding a config entry that
        // would need mirroring into both loader modules. A server that has turned off both
        // igniting and smelting has asked for fire breath to leave terrain alone.
        var config = ServerConfig.INSTANCE;
        if (!config.ignitingBreath.get() && !config.smeltingBreath.get()) return false;
        if (hit.getMaxHitDensity() < MELT_THRESHOLD) return false;

        if (state.is(Blocks.SNOW) && state.hasProperty(LAYERS) && state.getValue(LAYERS) > 1) {
            level.setBlock(pos, state.setValue(LAYERS, state.getValue(LAYERS) - 1), 3);
        } else {
            level.removeBlock(pos, false);
        }
        // 1501 is the lava-fizz event: hiss plus a puff of steam, which reads as melting
        level.levelEvent(null, 1501, pos, 0);
        this.spreadTemporaryWater(level, pos);
        return true;
    }

    /// Ice (incl. packed, blue, frosted), snow blocks, snow layers and powder snow.
    protected static boolean isMeltable(BlockState state) {
        return state.is(BlockTags.ICE) || state.is(BlockTags.SNOW) || state.is(Blocks.POWDER_SNOW);
    }

    protected void smeltBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return;
        var input = new SingleRecipeInput(state.getBlock().getCloneItemStack(level, pos, state));
        level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).ifPresent(holder -> {
            var stack = holder.value().assemble(input, level.registryAccess());
            if (stack.isEmpty()) return;
            if (stack.getItem() instanceof BlockItem item && item != Items.AIR) {
                level.setBlockAndUpdate(pos, item.getBlock().defaultBlockState());
            }
        });
    }
}
