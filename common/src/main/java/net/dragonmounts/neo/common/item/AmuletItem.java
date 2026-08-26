package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.ScoreboardAccessor;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMAmuletArmor;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.common.init.DMItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import static net.dragonmounts.neo.common.component.ScoreboardInfo.applyScores;
import static net.dragonmounts.neo.common.util.EntityUtil.*;

/**
 * @see net.minecraft.world.item.SpawnEggItem
 */
public class AmuletItem<T extends Entity> extends ArmorItem implements EntityContainer<T> {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_amulet";
    public final Class<T> contentType;

    /**
     * @param armorTexture texture stem under textures/models/armor/, e.g. "aether_dragon_amulet".
     *                     It is passed explicitly rather than derived from the DragonType id
     *                     because ENDER registers under DragonType.DEFAULT_KEY ("legacu_ender"),
     *                     which would resolve to a texture that does not exist.
     */
    public AmuletItem(Class<T> contentType, String armorTexture, Properties props) {
        super(Holder.direct(DMAmuletArmor.material(armorTexture)),
                Type.CHESTPLATE,
                DMAmuletArmor.withDefaultTint(props).stacksTo(1));
        this.contentType = contentType;
    }

    @Override
    public @Nullable Entity loadEntity(
            ServerLevel level,
            ItemStack stack,
            @Nullable Player player,
            BlockPos pos,
            MobSpawnType reason,
            boolean yOffset,
            boolean extraOffset
    ) {
        var data = stack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        if (data.isEmpty()) return null;
        var type = EntityType.by(data.copyTag()).orElse(null);
        if (type == null) return null;
        var entity = type.create(level, null, pos, reason, yOffset, extraOffset);
        if (entity == null) return null;
        mergeEntityData(entity, level, player, data);
        restoreIdentity(level, entity, data);
        applyScores(level.getScoreboard(), stack, entity);
        return entity;
    }

    @Override
    public final Class<T> getContentType() {
        return this.contentType;
    }

    /**
     * Carries the player's own decorations from one amulet stack to the next.
     * <p>
     * Capturing and releasing both build a brand-new ItemStack, so anything not copied across is
     * silently dropped -- which is why a dyed amulet reverted to its default colour the moment a
     * dragon went in or came out. Only player-applied components are copied; the entity payload
     * deliberately is not.
     */
    public static ItemStack carryOverDecoration(ItemStack from, ItemStack to) {
        var dye = from.get(DataComponents.DYED_COLOR);
        if (dye != null) to.set(DataComponents.DYED_COLOR, dye);
        var name = from.get(DataComponents.CUSTOM_NAME);
        if (name != null) to.set(DataComponents.CUSTOM_NAME, name);
        return to;
    }

    @Override
    public ItemStack saveEntity(T entity, DataComponentPatch patch) {
        var type = entity.getType();
        if (type.canSerialize()) {
            var stack = new ItemStack(this);
            stack.set(DataComponents.ENTITY_DATA, EntityContainer.simplifyData(saveWithId(entity, new CompoundTag())));
            stack.set(DMDataComponents.SCORES, ((ScoreboardAccessor) entity.level().getScoreboard()).neodragonmounts$getInfo(entity));
            stack.applyComponents(patch);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof TameableDragonEntity dragon)) return InteractionResult.PASS;
        if (dragon.isOwnedBy(player)) {
            var amulet = dragon.getDragonType().getInstance(DragonAmuletItem.class, null);
            if (amulet == null) return InteractionResult.FAIL;
            if (!(dragon.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
            if (!this.isEmpty(stack)) {
                var pos = dragon.blockPosition();
                var entity = this.loadEntity(level, stack, player, pos, MobSpawnType.BUCKET, false, false);
                if (entity != null) {
                    level.addFreshEntityWithPassengers(entity);
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
                }
            }
            dragon.inventory.dropContents(true, 0);
            dragon.ejectPassengers();
            // Capture sound. The existing playSound sits inside the `!isEmpty(stack)` branch
            // above, so it only fired when a previous occupant was being swapped out -- a plain
            // capture into an empty amulet was silent.
            level.playSound(null, dragon.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            consumeStack(player, hand, stack,
                    carryOverDecoration(stack, amulet.saveEntity(dragon, DataComponentPatch.EMPTY)));
            player.awardStat(Stats.ITEM_USED.get(this));
            dragon.discard();
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("message.neodragonmounts.not_owner"), true);
        return InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var stack = context.getItemInHand();
        if (this.isEmpty(stack)) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel level) {
            var pos = context.getClickedPos();
            var direction = context.getClickedFace();
            var spawnPos = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(direction);
            var player = context.getPlayer();
            var entity = this.loadEntity(
                    level,
                    stack,
                    player,
                    spawnPos,
                    MobSpawnType.BUCKET,
                    true,
                    !Objects.equals(pos, spawnPos) && direction == Direction.UP
            );
            if (entity != null) {
                level.addFreshEntityWithPassengers(entity);
                // Release-on-ground had no sound: gameEvent only feeds sculk sensors, it is not
                // audible. Matches the liquid path in use().
                level.playSound(null, spawnPos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
                if (player != null) {
                    consumeStack(player, context.getHand(), stack, carryOverDecoration(stack, new ItemStack(DMItems.AMULET)));
                }
                // stat will be awarded at `ItemStack#useOn`
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var result = this.releaseIntoLiquid(level, player, hand);
        // Nothing was released -- the amulet is empty, or the player is not pointing at a fluid
        // source -- so fall through to the vanilla armour swap and wear it, the way right-clicking
        // a chestplate does. Pointing at a solid block is handled by useOn above, which consumes
        // the interaction before use() is ever reached, so this cannot steal a release.
        return result.getResult() == InteractionResult.PASS
                ? this.swapWithEquipmentSlot(this, level, player, hand)
                : result;
    }

    private InteractionResultHolder<ItemStack> releaseIntoLiquid(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (this.isEmpty(stack)) return InteractionResultHolder.pass(stack);
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != BlockHitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        var pos = hit.getBlockPos();
        // The liquid test has to run before the ServerLevel guard. use() now equips on PASS, so
        // both sides must agree on whether this is a release; bailing out client-side first would
        // have the server equip while the client only swung its arm.
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock)) return InteractionResultHolder.pass(stack);
        if (!(level instanceof ServerLevel world)) return InteractionResultHolder.success(stack);
        if (world.mayInteract(player, pos) && player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            var entity = this.loadEntity(world, stack, player, pos, MobSpawnType.BUCKET, false, false);
            if (entity == null) return InteractionResultHolder.pass(stack);
            world.addFreshEntityWithPassengers(entity);
            // useOn plays ENDER_EYE_DEATH on release; this liquid path had no sound at all.
            world.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
            consumeStack(player, hand, stack, carryOverDecoration(stack, new ItemStack(DMItems.AMULET)));
            world.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void onDestroyed(ItemEntity item) {
        var stack = item.getItem();
        if (this.isEmpty(stack)) return;
        var level = (ServerLevel) item.level();
        var entity = this.loadEntity(level, stack, null, item.getOnPos(), MobSpawnType.BUCKET, true, false);
        if (entity != null) {
            level.addFreshEntityWithPassengers(entity);
        }
    }
}
