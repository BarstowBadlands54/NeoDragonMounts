package net.dragonmounts.neo.common.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static net.dragonmounts.neo.common.command.DMCommands.createClassCastException;

/**
 * Directly sets a dragon's break-in trust flag ({@code isBreakInTrusted}) — the boolean
 * that gates saddle/armor/chest equipping and full player-controlled riding. Shaped like
 * TameCommand: a bare no-selector form auto-resolves a nearby dragon for the running
 * player and grants trust, while explicit forms take a selector and an optional
 * true/false value so you can test either direction.
 */
public class TrustCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register(Predicate<CommandSourceStack> permission) {
        return Commands.literal("trust").requires(permission)
                // No-selector form: `/dragonmounts trust` grants full-ride trust to the
                // dragon you're riding, looking at, or nearest to. Handy for testing.
                .executes(TrustCommand::trustResolved)
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(context -> trust(context, EntityArgument.getEntities(context, "targets"), true))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> trust(
                                        context,
                                        EntityArgument.getEntities(context, "targets"),
                                        BoolArgumentType.getBool(context, "value")
                                ))
                        )
                );
    }

    /** Grant trust to the auto-resolved nearby dragon (ridden / looked-at / nearest). */
    private static int trustResolved(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var dragon = DMCommands.resolveNearbyDragon(context.getSource());
        return trust(context, List.of(dragon), true);
    }

    private static int trust(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets, boolean value) {
        var source = context.getSource();
        Entity cache = null;
        boolean flag = true;
        int count = 0;
        for (var target : targets) {
            if (target instanceof TameableDragonEntity dragon) {
                dragon.setBreakInTrusted(value);
                ++count;
                flag = false;
                cache = dragon;
            }
        }
        if (flag) {
            source.sendFailure(createClassCastException(targets.iterator().next(), TameableDragonEntity.class));
        } else if (count == 1) {
            final var temp = cache;
            source.sendSuccess(() -> Component.translatable(
                    value ? "commands.neodragonmounts.trust.single.on" : "commands.neodragonmounts.trust.single.off",
                    temp.getDisplayName()
            ), true);
        } else {
            final var temp = count;
            source.sendSuccess(() -> Component.translatable(
                    value ? "commands.neodragonmounts.trust.multiple.on" : "commands.neodragonmounts.trust.multiple.off",
                    temp
            ), true);
        }
        return count;
    }
}