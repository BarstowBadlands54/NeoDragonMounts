package net.dragonmounts.neo.common.entity.breath;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.dragonmounts.neo.common.init.DMParticles;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * @param source entity id of the dragon breathing, or {@link #NO_SOURCE}. The particle needs it
 *               to know which entity not to collide with: it spawns at the muzzle, so a stream
 *               that stopped on its own dragon would never leave the head.
 */
public record BreathParticleOption(DragonVariant variant, BreathPower power, int source) implements ParticleOptions {
    /// No known source, e.g. spawned by the `/particle` command rather than by a dragon.
    public static final int NO_SOURCE = -1;

    public static final MapCodec<BreathParticleOption> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DragonVariant.CODEC.fieldOf("variant").forGetter(BreathParticleOption::variant),
            BreathPower.CODEC.fieldOf("power").forGetter(BreathParticleOption::power),
            Codec.INT.optionalFieldOf("source", NO_SOURCE).forGetter(BreathParticleOption::source)
    ).apply(instance, BreathParticleOption::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BreathParticleOption> STREAM_CODEC = StreamCodec.composite(
            DragonVariant.STREAM_CODEC,
            BreathParticleOption::variant,
            BreathPower.STREAM_CODEC,
            BreathParticleOption::power,
            ByteBufCodecs.VAR_INT,
            BreathParticleOption::source,
            BreathParticleOption::new
    );

    @Override
    public ParticleType<?> getType() {
        return DMParticles.DRAGON_BREATH;
    }
}
