package net.dragonmounts.neo.config;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

public abstract class ConfigEntry<T> {
    public final MutableComponent getDisplayName() {
        return Dummy.get();
    }

    public boolean isDefault() {
        return Dummy.get();
    }

    public abstract String getAsString();

    public abstract Tag dump();

    public abstract T load(@Nullable Tag data);

    public void set(T value) {}

    public abstract void override(T value);

    /// set to fallback
    public void reset() {}

    /// set to saved
    public abstract void revert();

    public abstract void setSaved();

    public abstract CustomPacketPayload wrap(int id);

    public abstract ArgumentType<T> getArgument();

    public abstract T parse(CommandContext<?> context, String name);
}
