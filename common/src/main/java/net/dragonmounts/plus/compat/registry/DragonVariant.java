package net.dragonmounts.plus.compat.registry;

import com.mojang.serialization.Codec;
import net.dragonmounts.plus.common.api.DragonTypified;
import net.dragonmounts.plus.common.client.variant.VariantAppearance;
import net.dragonmounts.plus.common.util.DragonHead;
import net.dragonmounts.plus.compat.Dummy;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static net.dragonmounts.plus.common.DragonMountsShared.DRAGON_VARIANT;
import static net.dragonmounts.plus.common.DragonMountsShared.makeId;
import static net.dragonmounts.plus.compat.registry.RegistryHandler.makeDefaultedRegistry;

public class DragonVariant implements DragonTypified {
    public static final String DATA_PARAMETER_KEY = "Variant";
    public static final ResourceLocation DEFAULT_KEY = makeId("ender_female");
    public static final DefaultedMappedRegistry<DragonVariant> REGISTRY = makeDefaultedRegistry(DRAGON_VARIANT, DEFAULT_KEY);
    public static final Codec<DragonVariant> CODEC = REGISTRY.byNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, DragonVariant> STREAM_CODEC = ByteBufCodecs.registry(DRAGON_VARIANT);
    public static final EntityDataSerializer<DragonVariant> SERIALIZER = EntityDataSerializer.forValueType(STREAM_CODEC);

    public static DragonVariant draw(DragonType type, RandomSource random) {
        return Dummy.get();
    }

    public static DragonVariant draw(DragonType type, RandomSource random, String current) {
        return Dummy.get();
    }

    int index = -1;// non-private to simplify nested class access
    public final DragonType type;
    public final ResourceLocation identifier;
    public final VariantAppearance appearance;
    public final DragonHead head;

    public DragonVariant(
            DragonType type,
            ResourceLocation identifier,
            VariantAppearance appearance,
            Function<DragonVariant, DragonHead> factory
    ) {
        this.type = type;
        this.identifier = identifier;
        this.appearance = appearance;
        this.head = factory.apply(this);
    }

    @Override
    public final DragonType getDragonType() {
        return this.type;
    }

    /// Simplified {@link it.unimi.dsi.fastutil.objects.ReferenceArrayList}
    @SuppressWarnings("ClassCanBeRecord")
    public static final class Manager implements DragonTypified {
        public static final int DEFAULT_INITIAL_CAPACITY = 8;
        public final DragonType type;

        public Manager(DragonType type) {
            this.type = type;
        }

        @SuppressWarnings("UnusedReturnValue")
        boolean add(final DragonVariant variant) {
            return Dummy.get();
        }

        @Contract("!null, !null, _ -> !null")
        public @Nullable DragonVariant draw(RandomSource random, @Nullable DragonVariant current, boolean acceptSelf) {
            return current;
        }

        public int size() {
            return Dummy.get();
        }

        @Override
        public DragonType getDragonType() {
            return this.type;
        }

        public void register(DragonVariant variant) {}
    }
}
