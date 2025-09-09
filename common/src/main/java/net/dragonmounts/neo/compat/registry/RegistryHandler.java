package net.dragonmounts.neo.compat.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class RegistryHandler {
    public static Activity registerActivity(String name) {
        return Dummy.get();
    }

    public static <T extends ArmorEffect> T registerArmorEffect(ResourceLocation identifier, T effect) {
        return Dummy.get();
    }

    public static <T> MemoryModuleType<T> registerSensoryMemory(String name) {
        return Dummy.get();
    }

    public static MemoryModuleType<Unit> registerMemory(String name) {
        return Dummy.get();
    }

    public static <T> MemoryModuleType<T> registerMemory(String name, Codec<T> codec) {
        return Dummy.get();
    }

    public static <T extends ParticleOptions> ParticleType<T> registerParticle(
            String name,
            boolean overrideLimiter,
            MapCodec<T> codec,
            StreamCodec<? super RegistryFriendlyByteBuf, T> packetCodec
    ) {
        return Dummy.get();
    }

    public static <S extends RecipeSerializer<T>, T extends Recipe<?>> S registerRecipe(ResourceLocation identifier, S serializer) {
        return Dummy.get();
    }

    public static <T extends Sensor<?>> SensorType<T> registerSensor(String name, Supplier<T> factory) {
        return Dummy.get();
    }

    public static <T extends SoundEvent> T registerSound(ResourceLocation identifier, T sound) {
        return Dummy.get();
    }

    public static <S extends Structure> StructureType<S> registerStructure(String name, StructureType<S> structure) {
        return Dummy.get();
    }

    public static StructurePieceType registerStructure(String name, StructurePieceType.StructureTemplateType piece) {
        return Dummy.get();
    }

    public static <T extends ArmorEffect> T registerArmorEffect(String name, Function<ResourceLocation, T> factory) {
        return Dummy.get();
    }

    public static <T> DataComponentType<T> registerComponent(String name, UnaryOperator<DataComponentType.Builder<T>> operator) {
        return Dummy.get();
    }

    public static <T extends ConsumeEffect> ConsumeEffect.Type<T> registerConsumeEffect(String name, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> network) {
        return Dummy.get();
    }
}
