package net.dragonmounts.neo.compat.registry;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class AbstractHolder<V extends T, T> implements Supplier<V> {
    public final ResourceKey<T> key;

    public AbstractHolder(ResourceKey<T> key) {
        this.key = key;
    }

    public final Holder<T> wrap() {
        return Dummy.get();
    }

    @Override
    public final V get() {
        return Dummy.get();
    }

    @SuppressWarnings("unused")
    public final boolean is(@Nullable T other) {
        return Dummy.get();
    }
}
