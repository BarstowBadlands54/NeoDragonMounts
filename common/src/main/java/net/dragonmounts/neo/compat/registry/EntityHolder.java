package net.dragonmounts.neo.compat.registry;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class EntityHolder<T extends Entity> extends AbstractHolder<EntityType<T>, EntityType<?>> {
    public static <T extends Entity> EntityHolder<T> registerEntity(
            String name,
            MobCategory category,
            EntityType.EntityFactory<T> factory,
            Consumer<EntityType.Builder<T>> init
    ) {
        return Dummy.get();
    }

    public static <T extends LivingEntity> EntityHolder<T> registerLivingEntity(
            String name,
            MobCategory category,
            EntityType.EntityFactory<T> factory,
            Supplier<AttributeSupplier.Builder> supplier,
            Consumer<EntityType.Builder<T>> init
    ) {
        return Dummy.get();
    }

    public EntityHolder(ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder) {
        super(key);
    }

    public final <R extends T> EntityType<R> cast() {
        return Dummy.get();
    }
}
