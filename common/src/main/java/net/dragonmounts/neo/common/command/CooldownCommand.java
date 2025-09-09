package net.dragonmounts.neo.common.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.capability.ArmorEffectManager.Provider;
import net.dragonmounts.neo.compat.registry.CooldownCategory;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.Predicate;

public class CooldownCommand {
    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandBuildContext builder, Predicate<CommandSourceStack> permission) {
        return Commands.literal("cooldown")
                .requires(permission)
                .then(Commands.argument("players", EntityArgument.players())
                        .then(Commands.argument("category", ResourceArgument.resource(builder, DragonMountsShared.COOLDOWN_CATEGORY))
                                .executes(context -> get(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "players"),
                                        ResourceArgument.getResource(context, "category", DragonMountsShared.COOLDOWN_CATEGORY).value()
                                ))
                                .then(Commands.argument("cooldown", IntegerArgumentType.integer(0))
                                        .executes(context -> set(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "players"),
                                                ResourceArgument.getResource(context, "category", DragonMountsShared.COOLDOWN_CATEGORY).value(),
                                                IntegerArgumentType.getInteger(context, "cooldown")
                                        ))
                                )
                        )
                );
    }

    public static int get(CommandSourceStack source, Collection<ServerPlayer> players, CooldownCategory category) {
        if (players.isEmpty()) {
            source.sendFailure(Component.translatable("commands.neodragonmounts.cooldown.get.failure"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "commands.neodragonmounts.cooldown.get.success",
                category.identifier.toString(),
                ComponentUtils.formatList(players, player -> Component.translatable(
                        "commands.neodragonmounts.cooldown.get.entry",
                        player.getDisplayName(),
                        ((Provider) player).neodragonmounts$getManager().getCooldown(category)
                ))
        ), true);
        return players.size();
    }

    public static int set(CommandSourceStack source, Collection<ServerPlayer> players, CooldownCategory category, int value) {
        if (players.isEmpty()) {
            source.sendFailure(Component.translatable("commands.neodragonmounts.cooldown.set.failure"));
            return 0;
        }
        for (var player : players) {
            ((Provider) player).neodragonmounts$getManager().setCooldown(category, value);
        }
        int size = players.size();
        if (size == 1) {
            source.sendSuccess(() -> Component.translatable("commands.neodragonmounts.cooldown.set.single", players.iterator().next().getDisplayName(), category.identifier.toString(), value), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.neodragonmounts.cooldown.set.multiple", size, category.identifier.toString(), value), true);
        }
        return size;
    }
}
