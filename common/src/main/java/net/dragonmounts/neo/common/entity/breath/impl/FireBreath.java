package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMBlocks;
import net.dragonmounts.neo.common.init.DragonVariants;
import net.dragonmounts.neo.compat.platform.FlammableBlock;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LAYERS;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

public class FireBreath extends DragonBreath {
    public FireBreath(TameableDragonEntity dragon, float damage) {
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
                        || random.nextFloat() < 0.0625F
                        || density < this.calcIgnitionThreshold(level, pos, state, facing)
                ) continue;
                var sideToIgnite = pos.relative(facing);
                if (level.getBlockState(sideToIgnite).isAir()) {
                    consume = true;
                    this.burnBlock(level, sideToIgnite, random);
                    //    if (densityOfThisFace >= thresholdForDestruction && state.getBlockHardness(level, pos) != -1 && DragonMountsConfig.canFireBreathAffectBlocks) {
                    //   level.setBlockToAir(pos);
                }
            }
            if (enableSmelting && max > 0.5F) {
                consume = true;
                this.smeltBlock(level, pos, state);
            }
        }
        return consume ? new BreathAffectedBlock() : hit;
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        target.igniteForTicks(80);
        float damage = this.damage * hit.getHitDensity();
        if (target.isInPowderSnow || target.isFullyFrozen()) {
            damage *= 2.0F;
        } else if (target.isInWaterOrRain()) {
            damage *= 1.5F;
        }
        target.hurt(level.damageSources().mobAttack(this.dragon), damage);
    }

    protected boolean litBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.hasProperty(LIT) && !state.getValue(LIT)) {
            if (state.is(BlockTags.CAMPFIRES)) {
                level.levelEvent(null, 1009, pos, 0);
                level.setBlockAndUpdate(pos, state.setValue(LIT, true));
                return true;
            } else if (state.is(BlockTags.CANDLES) || state.is(BlockTags.CANDLE_CAKES)) {
                level.setBlock(pos, state.setValue(LIT, true), 11);
                return true;
            }
        }
        return false;
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

    protected void burnBlock(ServerLevel level, BlockPos sideToIgnite, RandomSource random) {
        BlockState fire = pickFireBlock(this.dragon.getVariant().identifier, random);
        level.setBlockAndUpdate(sideToIgnite, fire);
        level.playSound(null, sideToIgnite.getX() + 0.5, sideToIgnite.getY() + 0.5, sideToIgnite.getZ() + 0.5,
                SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.8F + random.nextFloat() * 0.4F);
    }

    protected BlockState pickFireBlock(ResourceLocation variantId, RandomSource random) {
        String path = variantId.getPath();
        if (this.dragon.getVariant() == DragonVariants.BLUE_FIRE) {
            // dual-flame variant: randomly red OR blue fire block
            return random.nextBoolean()
                    ? DMBlocks.BLUE_FIRE.get().defaultBlockState()
                    : Blocks.FIRE.defaultBlockState();   // your red fire block
        }
        // normal fire variants -> vanilla orange fire (or your yellow fire block)
        return Blocks.FIRE.defaultBlockState();
    }

    /**
     * Density of breath a face must take before it catches.
     * <p>
     * Vanilla flammability is the fast path: wood and wool light almost instantly. Everything
     * below it exists because dragon fire is not a tinderbox -- a sustained beam scorches stone,
     * dirt and sand too, it just takes longer than lighting a plank.
     */
    protected float calcIgnitionThreshold(Level level, BlockPos pos, BlockState state, Direction side) {
        int flammability = FlammableBlock.getFlammability(level, pos, state, side);
        if (flammability > 0) return 15.0F / flammability;
        // Vanilla assigns mushrooms, mycelium and nether fungi no flammability at all, so a
        // dragon torching a mushroom island did nothing. Treated as moderately flammable.
        if (isFungal(state)) return 15.0F / FUNGAL_FLAMMABILITY;
        // Any other sturdy face: stone, dirt, sand, grass. A passing sweep leaves them alone;
        // a held beam sets the surface alight. Air, fluids and non-solid faces stay unlit,
        // which is what stops fire appearing in mid-air along the beam.
        if (state.isFaceSturdy(level, pos, side)) return SOLID_IGNITION_THRESHOLD;
        return Float.MAX_VALUE;
    }

    /// Roughly as easy to light as vanilla wool. Raise to make fungal blocks catch faster.
    protected static final float FUNGAL_FLAMMABILITY = 20.0F;
    /**
     * Density needed to scorch an inert surface. For scale, the flammability path yields about
     * 0.25 for wool and 3.0 for planks, so this is deliberately several times slower than wood.
     */
    protected static final float SOLID_IGNITION_THRESHOLD = 6.0F;

    /// Mushrooms, mycelium and the nether's fungal growth, none of which vanilla treats as fuel.
    protected static boolean isFungal(BlockState state) {
        return state.is(Blocks.MYCELIUM)
                || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
                || state.is(Blocks.MUSHROOM_STEM)
                || state.is(Blocks.CRIMSON_FUNGUS) || state.is(Blocks.WARPED_FUNGUS)
                || state.is(Blocks.SHROOMLIGHT)
                || state.is(BlockTags.WART_BLOCKS);
    }
}
