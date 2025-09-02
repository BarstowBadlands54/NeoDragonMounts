package net.dragonmounts.plus.compat.platform;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PlatformCompat {
    public static boolean isClientSide() {
        return FMLLoader.getDist().isClient();
    }

    public static int sendSuccess(Object source, Supplier<Component> message) {
        if (source instanceof CommandSourceStack) {
            ((CommandSourceStack) source).sendSuccess(message, true);
        }
        return 1;
    }

    public static int sendFailure(Object source, Component message) {
        if (source instanceof CommandSourceStack) {
            ((CommandSourceStack) source).sendFailure(message);
        }
        return 0;
    }

    public static SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        return EventHooks.finalizeMobSpawn(mob, level, difficulty, reason, data);
    }
}
