package net.dragonmounts.neo.compat.registry;

import com.mojang.serialization.Codec;
import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.client.variant.VariantAppearance;
import net.dragonmounts.neo.common.entity.projectile.ability.DragonProjectileAbility;
import net.dragonmounts.neo.common.util.DragonHead;
import net.dragonmounts.neo.compat.Dummy;
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

import static net.dragonmounts.neo.common.DragonMountsShared.DRAGON_VARIANT;
import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public class DragonVariant implements DragonTypified {
    public static final String DATA_PARAMETER_KEY = "Variant";
    /**
     * Must name a variant that is actually registered: a DefaultedMappedRegistry only captures its
     * default when that exact key is registered, so a dangling key leaves the fallback null and
     * REGISTRY.get() returns null for unknown ids instead of degrading gracefully. "ender_female"
     * was renamed to "ender_jean" and no longer exists, which also left this out of step with the
     * DATA_DRAGON_VARIANT default in TameableDragonEntity.
     */
    public static final ResourceLocation DEFAULT_KEY = makeId("ender_jean");
    public static final DefaultedMappedRegistry<DragonVariant> REGISTRY = Dummy.get();
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
    public final @Nullable DragonProjectileAbility projectile;

    public DragonVariant(
            DragonType type,
            ResourceLocation identifier,
            VariantAppearance appearance,
            @Nullable DragonProjectileAbility projectile,
            Function<DragonVariant, DragonHead> factory
    ) {
        this.type = type;
        this.identifier = identifier;
        this.appearance = appearance;
        this.head = factory.apply(this);
        this.projectile = projectile;
        // Registry.register, not just Manager.add: adding only filed the variant in the type's
        // local array, so DragonVariant.REGISTRY stayed empty even though the registry itself was
        // created and handed to the root registry. BuiltInRegistries.bootStrap() then walks every
        // DefaultedRegistry and resolves its default key, found nothing behind ender_jean, and
        // dereferenced a null defaultValue. Manager.register does both halves, and mirrors how
        // DragonType's constructor has always registered itself.
        type.variants.register(this);
    }

    public DragonVariant(DragonType type, ResourceLocation identifier, VariantAppearance appearance,
                         Function<DragonVariant, DragonHead> factory) {
        this(type, identifier, appearance, null, factory);
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
