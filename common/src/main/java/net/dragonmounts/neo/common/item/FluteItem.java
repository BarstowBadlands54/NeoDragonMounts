package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.client.ClientUtil;
import net.dragonmounts.neo.common.entity.dragon.Relation;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.common.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FluteItem extends Item {
    public static @Nullable ServerDragonEntity getOrDeny(ServerPlayer player, UUID uuid) {
        if (player.serverLevel().getEntity(uuid) instanceof ServerDragonEntity dragon
                && Relation.checkRelation(dragon, player).isTrusted
        ) return dragon;
        player.sendSystemMessage(Component.translatable("message.neodragonmounts.flute.failed"), true);
        return null;
    }

    /**
     * Handles "Set Home". The dimension comes from the sending player rather than the packet, and
     * only the owner may re-home a dragon — a merely trusted rider can send it home but not move
     * where home is.
     */
    public static void setHome(ServerPlayer player, UUID uuid) {
        var dragon = getOrDeny(player, uuid);
        if (dragon == null) return;
        if (Relation.denyIfNotOwner(dragon, player)) return;
        // The block the player occupies, not the one they are looking at: you mark a nest by
        // standing in it. Read from the player, never from the packet, so a modified client
        // cannot home a dragon somewhere it has never been.
        var pos = player.blockPosition();
        dragon.setHomePos(new GlobalPos(player.serverLevel().dimension(), pos));
        cacheHomeOnFlute(player, uuid, dragon);
        player.displayClientMessage(Component.translatable(
                "message.neodragonmounts.flute.home_set", pos.getX(), pos.getY(), pos.getZ()
        ), true);
    }

    /**
     * Handles "Send Home". Reads the destination off the dragon, never off the packet, so this
     * cannot be used as an arbitrary teleport.
     */
    public static void recallHome(ServerPlayer player, UUID uuid) {
        var dragon = getOrDeny(player, uuid);
        if (dragon == null) return;
        var home = dragon.getHomePos();
        if (home == null) {
            // The flute's cached copy has gone stale (home cleared elsewhere, or the flute was
            // bound to a different dragon that shares the slot). Clear it so the button flips
            // back to "Set Home" instead of silently failing again.
            cacheHomeOnFlute(player, uuid, dragon);
            player.displayClientMessage(Component.translatable("message.neodragonmounts.flute.no_home"), true);
            return;
        }
        if (!dragon.canReturnHome()) {
            player.displayClientMessage(Component.translatable("message.neodragonmounts.flute.home_elsewhere"), true);
            return;
        }
        var pos = home.pos();
        // Sitting state is deliberately left alone: a dragon told to sit should still be sitting
        // when it arrives, which is usually what you want from something called "home".
        if (EntityUtil.teleportToAround(dragon, pos.getX(), pos.getY(), pos.getZ())) {
            player.displayClientMessage(Component.translatable(
                    "message.neodragonmounts.flute.home_recalled"
            ), true);
        } else {
            player.displayClientMessage(Component.translatable("message.neodragonmounts.flute.invalid_pos"), true);
        }
    }

    /**
     * Refreshes the held flute's cached home from the dragon's real value. Called after every
     * home interaction so the cache self-heals rather than drifting.
     */
    public static void cacheHomeOnFlute(ServerPlayer player, UUID uuid, ServerDragonEntity dragon) {
        var home = dragon.getHomePos();
        boolean changed = false;
        for (InteractionHand hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            var sound = stack.get(DMDataComponents.FLUTE_SOUND);
            if (sound == null || !uuid.equals(sound.dragon())) continue;
            if (home == null) {
                stack.remove(DMDataComponents.DRAGON_HOME);
            } else {
                stack.set(DMDataComponents.DRAGON_HOME, home);
            }
            changed = true;
        }
        // Component edits on a held stack only reach the client on the next container sync, and
        // the player is mid-use here, so nudge it rather than waiting.
        if (changed) player.containerMenu.broadcastChanges();
    }

    /// @see net.minecraft.world.item.ItemUtils#startUsingInstantly(Level, Player, InteractionHand)
    public static InteractionResultHolder<ItemStack> startPlaying(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var sound = stack.get(DMDataComponents.FLUTE_SOUND);
        if (sound == null) return InteractionResultHolder.pass(stack);
        if (player.isLocalPlayer()) {
            ClientUtil.openFluteScreen(sound.dragon(), stack.get(DMDataComponents.DRAGON_HOME));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public FluteItem(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1200;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        return player.isShiftKeyDown() ? InteractionResult.PASS : startPlaying(player, hand).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return startPlaying(player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int time) {
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return stack.has(DMDataComponents.FLUTE_SOUND) ? UseAnim.TOOT_HORN : UseAnim.NONE;
    }
}
