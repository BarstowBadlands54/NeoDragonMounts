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

public class SleepCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register(Predicate<CommandSourceStack> permission) {
        return Commands.literal("sleep").requires(permission)
                // No-selector form: `/dragonmounts sleep` grants full-ride sleep to the
                // dragon you're riding, looking at, or nearest to. Handy for testing.
                .executes(SleepCommand::sleepResolved)
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(context -> sleep(context, EntityArgument.getEntities(context, "targets"), true))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> sleep(
                                        context,
                                        EntityArgument.getEntities(context, "targets"),
                                        BoolArgumentType.getBool(context, "value")
                                ))
                        )
                );
    }

    /** Grant sleep to the auto-resolved nearby dragon (ridden / looked-at / nearest). */
    private static int sleepResolved(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var dragon = DMCommands.resolveNearbyDragon(context.getSource());
        return sleep(context, List.of(dragon), true);
    }

    private static int sleep(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets, boolean value) {
        var source = context.getSource();
        Entity cache = null;
        boolean flag = true;
        int count = 0;
        for (var target : targets) {
            if (target instanceof TameableDragonEntity dragon) {
                dragon.setSleeping(value);
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
                    value ? "commands.neodragonmounts.sleep.single.on" : "commands.neodragonmounts.sleep.single.off",
                    temp.getDisplayName()
            ), true);
        } else {
            final var temp = count;
            source.sendSuccess(() -> Component.translatable(
                    value ? "commands.neodragonmounts.sleep.multiple.on" : "commands.neodragonmounts.sleep.multiple.off",
                    temp
            ), true);
        }
        return count;
    }
}
