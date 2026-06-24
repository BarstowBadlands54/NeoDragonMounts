package net.dragonmounts.neo.common.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Predicate;

public final class VariantCommand {
    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> register(
            Predicate<CommandSourceStack> permission) {
        return Commands.literal("variant").requires(permission)
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("variant", ResourceLocationArgument.id())
                                .suggests((ctx, b) -> {
                                    for (DragonVariant v : DragonVariant.REGISTRY) b.suggest(v.identifier.toString());
                                    return b.buildFuture();
                                })
                                .executes(VariantCommand::setVariant)));
    }

    private static int setVariant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(ctx, "target");
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "variant");
        if (!(entity instanceof TameableDragonEntity dragon)) {
            ctx.getSource().sendFailure(Component.literal("Target is not a dragon"));
            return 0;
        }
        DragonVariant variant = DragonVariant.REGISTRY.get(id);
        if (variant == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown variant: " + id));
            return 0;
        }
        dragon.setVariant(variant);
        ctx.getSource().sendSuccess(() ->
                Component.literal("Set variant to " + id), true);
        return 1;
    }
}