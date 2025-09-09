package net.dragonmounts.neo.compat.platform;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class PlatformCompat {
    public static boolean isClientSide() {
        return Dummy.get();
    }

    public static boolean isModLoaded(String identifier) {
        return Dummy.get();
    }

    public static int sendSuccess(Object source, Supplier<Component> message) {
        return Dummy.get();
    }

    public static int sendFailure(Object source, Component message) {
        return Dummy.get();
    }

    public static SpawnGroupData finalizeMobSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        return Dummy.get();
    }
}
