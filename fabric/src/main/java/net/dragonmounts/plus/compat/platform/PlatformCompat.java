package net.dragonmounts.plus.compat.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PlatformCompat {
    public static boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static int sendSuccess(Object source, Supplier<Component> message) {
        if (source instanceof CommandSourceStack) {
            ((CommandSourceStack) source).sendSuccess(message, true);
        } else if (isClientSide() && source instanceof FabricClientCommandSource) {
            ((FabricClientCommandSource) source).sendFeedback(message.get());
        }
        return 1;
    }

    public static int sendFailure(Object source, Component message) {
        if (source instanceof CommandSourceStack) {
            ((CommandSourceStack) source).sendFailure(message);
        } else if (isClientSide() && source instanceof FabricClientCommandSource) {
            ((FabricClientCommandSource) source).sendError(message);
        }
        return 0;
    }

    public static SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        return mob.finalizeSpawn(level, difficulty, reason, data);
    }
}
