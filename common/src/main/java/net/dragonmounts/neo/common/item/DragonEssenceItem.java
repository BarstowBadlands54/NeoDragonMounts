package net.dragonmounts.neo.common.item;

import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.ServerDragonEntity;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMDataComponents;
import net.dragonmounts.neo.common.inventory.DragonInventory;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import static net.dragonmounts.neo.common.DragonMountsShared.ITEM_TRANSLATION_KEY_PREFIX;
import static net.dragonmounts.neo.common.component.ScoreboardInfo.applyScores;
import static net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity.FLYING_DATA_PARAMETER_KEY;
import static net.dragonmounts.neo.common.util.EntityUtil.*;

public class DragonEssenceItem extends Item implements DragonTypified, EntityContainer<TameableDragonEntity> {
    public static final String TRANSLATION_KEY = ITEM_TRANSLATION_KEY_PREFIX + "dragon_essence";

    public final DragonType type;
    public final TranslatableContents name;

    public DragonEssenceItem(DragonType type, Properties props) {
        super(props.stacksTo(1).component(DMDataComponents.DRAGON_TYPE, type));
        this.type = type;
        this.name = new TranslatableContents(TRANSLATION_KEY + ".name", null, new Object[]{MutableComponent.create(type.name)});
    }

    @Override
    public Component getName(ItemStack stack) {
        return MutableComponent.create(this.name);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level) {
            var stack = context.getItemInHand();
            var pos = context.getClickedPos();
            var direction = context.getClickedFace();
            var spawnPos = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? pos : pos.relative(direction);
            var player = context.getPlayer();
            level.addFreshEntityWithPassengers(this.loadEntity(
                    level,
                    stack,
                    player,
                    spawnPos,
                    MobSpawnType.BUCKET,
                    true,
                    !Objects.equals(pos, spawnPos) && direction == Direction.UP
            ));
            level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
            consumeOne(player, context.getHand(), stack);
            // stat will be awarded at `ItemStack#useOn`
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != BlockHitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        if (!(level instanceof ServerLevel world)) return InteractionResultHolder.success(stack);
        var pos = hit.getBlockPos();
        if (!(world.getBlockState(pos).getBlock() instanceof LiquidBlock)) return InteractionResultHolder.pass(stack);
        if (world.mayInteract(player, pos) && player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            world.addFreshEntityWithPassengers(this.loadEntity(world, stack, player, pos, MobSpawnType.BUCKET, false, false));
            world.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            consumeOne(player, hand, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        tooltips.add(this.type.getName());
    }

    /** Shrinks the stack by 1, forcing the decrement even in creative mode. */
    private static void consumeOne(Player player, InteractionHand hand, ItemStack stack) {
        stack.shrink(1);
        // In creative, the held stack is normally restored — force the change into the real slot.
        if (player != null && player.getAbilities().instabuild) {
            player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    @Override
    public DragonType getDragonType() {
        return this.type;
    }

    @Override
    public ItemStack saveEntity(TameableDragonEntity entity, DataComponentPatch patch) {
        var stack = new ItemStack(this);
        var tag = saveWithId(entity, new CompoundTag());
        tag.remove(FLYING_DATA_PARAMETER_KEY);
        tag.remove(DragonInventory.DATA_PARAMETER_KEY);
        tag.remove("UUID");
        tag.remove("AbsorptionAmount");
        tag.remove("Age");
        tag.remove("AgeLocked");
        tag.remove("ArmorDropChances");
        tag.remove("ArmorItems");
        tag.remove("Attributes");
        tag.remove("Brain");
        tag.remove("ForcedAge");
        tag.remove("HandDropChances");
        tag.remove("HandItems");
        tag.remove("Health");
        tag.remove("LifeStage");
        tag.remove("LoveCause");
        tag.remove("ShearCooldown");
        tag.remove("Sitting");
        stack.set(DataComponents.ENTITY_DATA, EntityContainer.simplifyData(tag));
        stack.applyComponents(patch);
        return stack;
    }

    @Override
    public ServerDragonEntity loadEntity(
            ServerLevel world,
            ItemStack stack,
            @Nullable Player player,
            BlockPos pos,
            MobSpawnType reason,
            boolean yOffset,
            boolean extraOffset
    ) {
        return new ServerDragonEntity(world, (level, dragon) -> {
            finalizeSpawn(level, dragon, pos, reason, yOffset, extraOffset);
            CustomData data = stack.get(DataComponents.ENTITY_DATA);
            if (data != null) {
                mergeEntityData(dragon, level, player, data);
                dragon.setDragonType(this.type, false);
            } else {
                dragon.setDragonType(this.type, true);
            }
            applyScores(level.getScoreboard(), stack, dragon);
            dragon.setLifeStage(DragonLifeStage.HATCHLING, true, false);
        });
    }

    @Override
    public final Class<TameableDragonEntity> getContentType() {
        return TameableDragonEntity.class;
    }

    @Override
    public boolean isEmpty(ItemStack stack) {
        return false;
    }
}