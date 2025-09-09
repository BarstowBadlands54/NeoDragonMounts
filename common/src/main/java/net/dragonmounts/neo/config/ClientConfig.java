package net.dragonmounts.neo.config;

import com.mojang.brigadier.builder.ArgumentBuilder;
import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.commands.CommandSourceStack;

import java.util.Collection;

public class ClientConfig extends ConfigHolder<CommandSourceStack> {
    public static final ClientConfig INSTANCE = new ClientConfig();
    public final BooleanEntry debug;
    public final DoubleEntry cameraDistance;
    public final DoubleEntry cameraOffset;
    public final BooleanEntry convergePitchAngle;
    public final BooleanEntry convergeYawAngle;
    public final BooleanEntry hoverState;
    public final BooleanEntry toggleDescending;
    public final BooleanEntry toggleBreathing;
    public final BooleanEntry pauseOnFluting;

    private ClientConfig() {
        this.debug = Dummy.get();
        this.cameraDistance = Dummy.get();
        this.cameraOffset = Dummy.get();
        this.convergePitchAngle = Dummy.get();
        this.convergeYawAngle = Dummy.get();
        this.hoverState = Dummy.get();
        this.toggleDescending = Dummy.get();
        this.toggleBreathing = Dummy.get();
        this.pauseOnFluting = Dummy.get();
    }

    @Override
    public Collection<ConfigEntry<?>> getEntries() {
        return Dummy.get();
    }

    @Override
    protected <T> ArgumentBuilder<CommandSourceStack, ?> buildCommand(ConfigEntry<T> entry) {
        return Dummy.get();
    }
}
