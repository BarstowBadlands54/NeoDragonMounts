package net.dragonmounts.neo.common.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.compat.platform.PlatformCompat;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Comparator;
import java.util.List;

import java.util.function.Predicate;

import static net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY;


public class DMCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection ignored) {
        Predicate<CommandSourceStack> hasPermissionLevel2 = source -> source.hasPermission(2);
        var root = dispatcher.register(Commands.literal(DragonMountsShared.NAMESPACE)
                .then(CooldownCommand.register(context, hasPermissionLevel2))
                .then(FreeCommand.register(hasPermissionLevel2))
                .then(SaveCommand.register(context, hasPermissionLevel2))
                .then(StageCommand.register(hasPermissionLevel2))
                .then(TameCommand.register(hasPermissionLevel2))
                .then(TrustCommand.register(hasPermissionLevel2))
                .then(TypeCommand.register(context, hasPermissionLevel2))
                .then(VariantCommand.register(hasPermissionLevel2))
                .then(ServerConfig.INSTANCE.appendCommands(
                        Commands.literal("config").requires(source -> source.hasPermission(3))
                ))
        );
        if (PlatformCompat.isModLoaded("dragonmounts")) return;
        dispatcher.register(Commands.literal("dragonmounts").redirect(root));
    }

    public static Component createClassCastException(Class<?> from, Class<?> to) {
        return Component.literal("java.lang.ClassCastException: " + from.getName() + " cannot be cast to " + to.getName());
    }

    public static Component createClassCastException(Entity entity, Class<?> clazz) {
        return Component.literal("java.lang.ClassCastException: ").append(Component.literal(entity.getClass().getName())
                .setStyle(Style.EMPTY.withInsertion(entity.getStringUUID()).withHoverEvent(
                        new HoverEvent(SHOW_ENTITY, new HoverEvent.EntityTooltipInfo(entity.getType(), entity.getUUID(), entity.getName()))
                ))
        ).append(" cannot be cast to " + clazz.getName());
    }

    public static GameProfile getSingleProfileOrException(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        var profiles = GameProfileArgument.getGameProfiles(context, name);
        if (profiles.isEmpty()) throw EntityArgument.NO_PLAYERS_FOUND.create();
        if (profiles.size() > 1) throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create();
        return profiles.iterator().next();
    }

    /** Thrown when we can't auto-resolve a dragon for a player who gave no explicit target. */
    public static final SimpleCommandExceptionType NO_DRAGON_FOUND = new SimpleCommandExceptionType(
            Component.translatable("commands.neodragonmounts.no_dragon_found")
    );

    /**
     * Smart "which dragon did you mean?" resolver for commands run by a player without an explicit
     * entity selector. Priority:
     *   1. the dragon the player is currently RIDING (so you can target your own mount),
     *   2. the dragon the player is LOOKING AT (within reach),
     *   3. the NEAREST dragon around the player (within a generous radius).
     * Returns the dragon, or throws NO_DRAGON_FOUND if none qualifies / source isn't a player.
     */
    public static TameableDragonEntity resolveNearbyDragon(CommandSourceStack source) throws CommandSyntaxException {
        if (!(source.getEntity() instanceof ServerPlayer player)) throw NO_DRAGON_FOUND.create();

        // 1) the dragon the player is riding
        if (player.getVehicle() instanceof TameableDragonEntity ridden) return ridden;

        if (!(player.level() instanceof ServerLevel level)) throw NO_DRAGON_FOUND.create();

        // 2) the dragon the player is looking at (ray within 16 blocks)
        TameableDragonEntity looked = dragonInCrosshair(level, player, 16.0);
        if (looked != null) return looked;

        // 3) the nearest dragon within 24 blocks of the player
        AABB box = player.getBoundingBox().inflate(24.0);
        List<TameableDragonEntity> nearby = level.getEntitiesOfClass(
                TameableDragonEntity.class, box, d -> d.isAlive());
        if (!nearby.isEmpty()) {
            nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
            return nearby.get(0);
        }
        throw NO_DRAGON_FOUND.create();
    }

    /** Ray-trace from the player's eyes for a dragon within `reach` blocks; null if none. */
    private static TameableDragonEntity dragonInCrosshair(ServerLevel level, ServerPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);

        TameableDragonEntity best = null;
        double bestDistSqr = reach * reach;
        for (var entity : level.getEntities(player, searchBox, e -> e instanceof TameableDragonEntity && e.isAlive())) {
            AABB hitbox = entity.getBoundingBox().inflate(0.3);
            var clip = hitbox.clip(eye, end);
            if (clip.isPresent()) {
                double d = eye.distanceToSqr(clip.get());
                if (d < bestDistSqr) {
                    bestDistSqr = d;
                    best = (TameableDragonEntity) entity;
                }
            }
        }
        return best;
    }

}