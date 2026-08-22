package net.dragonmounts.neo.common.entity.breath.impl;

import net.dragonmounts.neo.common.entity.breath.BreathAffectedBlock;
import net.dragonmounts.neo.common.entity.breath.BreathAffectedEntity;
import net.dragonmounts.neo.common.entity.breath.DragonBreath;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Sculk breath. Overtakes whatever it washes over the way a catalyst blooms after a kill, leaving
 * sculk, veins and the occasional catalyst behind, and blinding anything caught in the stream.
 * <p>
 * Spread deliberately reuses vanilla's own {@code minecraft:sculk_replaceable} tag rather than a
 * hand-written block list, so it claims exactly the ground a catalyst would -- dirt, stone,
 * deepslate, sand and so on -- and leaves everything else alone. Sculk itself is not in that tag,
 * so the stream never churns ground it has already taken.
 * <p>
 * No shriekers are placed. A shrieker summons a Warden onto whoever is standing nearby, which is
 * usually the dragon's own owner.
 */
public class SculkBreath extends DragonBreath {
    /** Chance a converted block becomes a catalyst instead of plain sculk, so growth continues. */
    private static final float CATALYST_CHANCE = 0.004F;
    /** Chance a converted block sprouts veins into the open space above it. */
    private static final float VEIN_CHANCE = 0.22F;
    /** Darkness applied to anything caught in the stream, in ticks. */
    private static final int DARKNESS_TICKS = 80;
    /** Hit density a target must take before the darkness lands, so a graze does not blind. */
    private static final float DARKNESS_THRESHOLD = 0.2F;

    public SculkBreath(TameableDragonEntity dragon, float damage) {
        super(dragon, damage);
    }

    @Override
    public BreathAffectedBlock affectBlock(ServerLevel level, long location, BreathAffectedBlock hit) {
        // Terrain conversion rides destructiveBreath. There is no dedicated sculk toggle in the
        // config yet, and rewriting somebody's build into sculk is the kind of thing that switch
        // exists to gate -- add a `sculkBreath` entry alongside `frostyBreath` if it deserves one.
        if (!ServerConfig.INSTANCE.destructiveBreath.get()) return new BreathAffectedBlock();

        var pos = BlockPos.of(location);
        var state = level.getBlockState(pos);
        if (!state.is(BlockTags.SCULK_REPLACEABLE)) return new BreathAffectedBlock();

        var random = level.random;
        boolean catalyst = random.nextFloat() < CATALYST_CHANCE;
        level.setBlockAndUpdate(pos, (catalyst ? Blocks.SCULK_CATALYST : Blocks.SCULK).defaultBlockState());
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                catalyst ? 6 : 2, 0.25, 0.1, 0.25, 0.0);

        // Veins creep into the open air above, which is what sells the spread visually -- a flat
        // slab of sculk on its own reads as a texture swap rather than something growing.
        if (!catalyst && random.nextFloat() < VEIN_CHANCE) {
            var above = pos.above();
            if (level.getBlockState(above).isAir()) {
                // On a MultifaceBlock the true face is the one it clings to, so a vein resting on
                // this block sets DOWN.
                level.setBlockAndUpdate(above, Blocks.SCULK_VEIN.defaultBlockState()
                        .setValue(BlockStateProperties.DOWN, true));
            }
        }
        return new BreathAffectedBlock(); // reset to zero
    }

    @Override
    public void affectEntity(ServerLevel level, LivingEntity target, BreathAffectedEntity hit) {
        float density = hit.getHitDensity();
        target.hurt(level.damageSources().mobAttack(this.dragon), this.damage * density);
        if (density > DARKNESS_THRESHOLD) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DARKNESS_TICKS), this.dragon);
        }
        var direction = hit.getHitDirection();
        target.knockback(0.05F * density, -direction.x, -direction.z);
    }

    @Override
    public SoundEvent getStartSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_START_SCULK;
    }

    @Override
    public SoundEvent getLoopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_LOOP_SCULK;
    }

    @Override
    public SoundEvent getStopSound(DragonLifeStage stage) {
        return DMSounds.DRAGON_BREATH_STOP_SCULK;
    }
}
