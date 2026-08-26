package net.dragonmounts.neo.common.util;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.dragonmounts.neo.compat.platform.PlatformCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class EntityUtil extends /*to access protected methods*/ EntityType<Entity> {
    public static Vec2 getRiddenRotation(LivingEntity rider) {
        return new Vec2(rider.getXRot() * 0.5F, rider.getYRot());
    }

    /**
     * @see EntityType#create(ServerLevel, java.util.function.Consumer, BlockPos, MobSpawnType, boolean, boolean)
     */
    public static void finalizeSpawn(ServerLevel level, Entity entity, BlockPos pos, MobSpawnType reason, boolean yOffset, boolean extraOffset) {
        double offset, x = pos.getX() + 0.5D, y = pos.getY(), z = pos.getZ() + 0.5D;
        if (yOffset) {
            entity.setPos(x, y + 1.0D, z);
            offset = getYOffset(level, pos, extraOffset, entity.getBoundingBox());
        } else {
            offset = 0.0D;
        }
        entity.moveTo(x, y + offset, z, Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
        if (entity instanceof Mob mob) {
            mob.yHeadRot = mob.getYRot();
            mob.yBodyRot = mob.getYRot();
            PlatformCompat.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), reason, null);
            mob.playAmbientSound();
        }
    }

    /**
     * @see EntityType#updateCustomEntityTag(net.minecraft.world.level.Level, Player, Entity, CustomData)
     */
    public static void mergeEntityData(Entity entity, ServerLevel level, Player player, CustomData data) {
        MinecraftServer server = level.getServer();
        // 1.21.1: CustomData has no parseEntityType; resolve via EntityType.by(tag). onlyOpCanSetNbt is on Entity, not EntityType.
        EntityType<?> type = EntityType.by(data.copyTag()).orElse(null);
        if (entity.getType() == type && (!entity.onlyOpCanSetNbt() || player != null && server.getPlayerList().isOp(player.getGameProfile()))) {
            data.loadInto(entity);
        }
    }

    /**
     * Put a captured entity's own UUID back after {@link #mergeEntityData}.
     * <p>
     * {@code CustomData#loadInto} snapshots the fresh entity's UUID and reapplies it after the
     * merge, so that a stack of spawn eggs cannot spawn colliding entities. A container holding
     * one specific entity wants the opposite: anything that remembers that entity by UUID -- a
     * bound flute, above all -- is looking for the identity it went in with.
     * <p>
     * Skipped when something already answers to that UUID here, so releasing a copied container
     * yields a distinct entity rather than one the entity manager silently refuses to add.
     */
    public static void restoreIdentity(ServerLevel level, Entity entity, CustomData data) {
        var tag = data.copyTag();
        if (!tag.hasUUID("UUID")) return;
        UUID uuid = tag.getUUID("UUID");
        if (uuid.equals(entity.getUUID())) return;
        var existing = level.getEntity(uuid);
        // a discarded entity still answers until the manager sweeps at end of tick
        if (existing != null && !existing.isRemoved()) return;
        entity.setUUID(uuid);
    }

    public static boolean addOrMergeEffect(LivingEntity entity, Holder<MobEffect> holder, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        if (duration < 0) {
            return entity.addEffect(new MobEffectInstance(holder, -1, amplifier, ambient, visible, showIcon, null));
        }
        var effect = entity.getEffect(holder);
        if (effect == null) {
            return entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon, null));
        }
        if (effect.getAmplifier() < amplifier) {
            do {
                if (effect.isInfiniteDuration()) break;
                effect.duration += duration;
            } while ((effect = effect.hiddenEffect) != null);
            return entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon, null));
        }
        if (effect.isInfiniteDuration()) return false;
        return entity.addEffect(new MobEffectInstance(holder, duration + effect.getDuration(), amplifier, ambient, visible, showIcon, null));
    }

    public static boolean addOrResetEffect(LivingEntity entity, Holder<MobEffect> holder, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, int threshold) {
        var effect = entity.getEffect(holder);
        if (effect == null) {
            return entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon, null));
        }
        while (effect.getAmplifier() > amplifier) {
            if (effect.isInfiniteDuration()) return false;
            if (effect.hiddenEffect == null) {
                return effect.getDuration() < threshold &&
                        entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon, null));
            }
            effect = effect.hiddenEffect;
        }
        if (effect.getAmplifier() == amplifier) {
            return !effect.isInfiniteDuration() && effect.getDuration() < threshold &&
                    entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon, null));
        }
        return entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon, null));
    }

    public static ItemStack consumeStack(Player player, InteractionHand hand, ItemStack stack, ItemStack result) {
        // 1.21.1 has no USE_REMAINDER component / Player#handleExtraItemsCreatedOnUse (both added 1.21.2)
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, result);
            return result;
        }
        if (!result.isEmpty() && !player.getInventory().add(result)) {//Inventory.getFreeSlot() won't check the offhand slot
            player.drop(result, false);
        }
        return stack;
    }

    public static CompoundTag saveWithId(Entity entity, CompoundTag tag) {
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        entity.saveWithoutId(tag);
        return tag;
    }

    public static boolean isMoving(Entity entity) {
        double deltaX = entity.getX() - entity.xo, deltaZ = entity.getZ() - entity.zo;
        return deltaX * deltaX + deltaZ * deltaZ > 2.5E-7F;
    }

    /**
     * @return {@link EquipmentSlot#MAINHAND} by default.
     * @see LivingEntity#getSlotForHand
     */
    public static EquipmentSlot getSlotForHand(@Nullable InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    /// teleportToAroundBlockPos
    public static boolean teleportToAround(Mob entity, int centerX, int centerY, int centerZ) {
        var pos = new BlockPos.MutableBlockPos();
        var random = entity.getRandom();
        for (int i = 0; i < 10; i++) {
            int offsetX = random.nextIntBetweenInclusive(-3, 3), offsetZ = random.nextIntBetweenInclusive(-3, 3);
            if (Math.abs(offsetX) >= 2 || Math.abs(offsetZ) >= 2) {
                if (canTeleportTo(entity, pos.set(
                        centerX + offsetX,
                        centerY + random.nextIntBetweenInclusive(-1, 1),
                        centerZ + offsetZ
                ))) {
                    entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, entity.getYRot(), entity.getXRot());
                    entity.getNavigation().stop();
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canTeleportTo(Mob entity, BlockPos dest) {
        if (WalkNodeEvaluator.getPathTypeStatic(entity, dest) != PathType.WALKABLE) return false;
        var current = entity.blockPosition();
        return entity.level().noCollision(entity, entity.getBoundingBox().move(
                dest.getX() - current.getX(),
                dest.getY() - current.getY(),
                dest.getZ() - current.getZ()
        ));
    }

    public static void addOrUpdateTransientModifier(AttributeMap attributes, Holder<Attribute> attribute, AttributeModifier modifier) {
        var instance = attributes.getInstance(attribute);
        if (instance == null) return;
        instance.addOrUpdateTransientModifier(modifier);
    }

    public static Vec3 collectCollision(Entity entity, Vec3 motion, ObjectArrayList<Collision> collisions) {
        var moved = entity.getBoundingBox().expandTowards(motion);
        var candidates = entity.level().getEntityCollisions(entity, moved);
        var shapes = new ObjectArrayList<VoxelShape>(candidates.size() + 16);
        shapes.addAll(candidates);
        var border = entity.level().getWorldBorder();
        if (border.isInsideCloseToBorder(entity, moved)) {
            shapes.add(border.getCollisionShape());
        }
        entity.level().getBlockCollisions(entity, moved).forEach(shapes::add);
        if (shapes.isEmpty()) return motion;
        var box = entity.getBoundingBox();
        double motionX = motion.x, motionY = motion.y, motionZ = motion.z;
        if (motionY != 0.0) {
            motionY = Shapes.collide(Direction.Axis.Y, box, shapes, motionY);
            if (motionY != 0.0) {
                box = box.move(0.0, motionY, 0.0);
            }
            if (motionY != motion.y) {
                collisions.add(motion.y > 0
                        ? Collision.up(box, motion.y - motionY)
                        : Collision.down(box, motion.y - motionY)
                );
            }
        }

        boolean bl = Math.abs(motionX) < Math.abs(motionZ);
        if (bl && motionZ != 0.0) {
            motionZ = Shapes.collide(Direction.Axis.Z, box, shapes, motionZ);
            if (motionZ != 0.0) {
                box = box.move(0.0, 0.0, motionZ);
            }
            if (motionZ != motion.z) {
                collisions.add(motion.z > 0
                        ? Collision.south(box, motion.z - motionZ)
                        : Collision.north(box, motion.z - motionZ)
                );
            }
        }

        if (motionX != 0.0) {
            motionX = Shapes.collide(Direction.Axis.X, box, shapes, motionX);
            if (!bl && motionX != 0.0) {
                box = box.move(motionX, 0.0, 0.0);
            }
            if (motionX != motion.x) {
                collisions.add(motion.x > 0
                        ? Collision.east(box, motion.x - motionX)
                        : Collision.west(box, motion.x - motionX)
                );
            }
        }

        if (!bl && motionZ != 0.0) {
            motionZ = Shapes.collide(Direction.Axis.Z, box, shapes, motionZ);
            if (motionZ != motion.z) {
                collisions.add(motion.z > 0
                        ? Collision.south(box, motion.z - motionZ)
                        : Collision.north(box, motion.z - motionZ)
                );
            }
        }

        return new Vec3(motionX, motionY, motionZ);
    }

    public static void moveAndCollide(Entity entity, Vec3 motion, ObjectArrayList<Collision> collisions) {
        double motionLen = motion.lengthSqr();
        var movement = motionLen == 0.0 ? motion : collectCollision(entity, motion, collisions);
        double len = movement.lengthSqr();
        if (len > 1.0E-7 || motionLen - len < 1.0E-7) {
            var current = entity.position();
            entity.setPos(current.x + movement.x, current.y + movement.y, current.z + movement.z);
        }
        boolean bl = !Mth.equal(motion.x, movement.x);
        boolean bl2 = !Mth.equal(motion.z, movement.z);
        entity.horizontalCollision = bl || bl2;
        if (Math.abs(motion.y) > 0.0) {
            entity.verticalCollision = motion.y != movement.y;
            entity.verticalCollisionBelow = entity.verticalCollision && motion.y < 0.0;
            // 1.21.1: setOnGroundWithMovement(boolean onGround, Vec3 movement) -- no horizontalCollision arg
            entity.setOnGroundWithMovement(entity.verticalCollisionBelow, movement);
        }
        if (entity.horizontalCollision) {
            var delta = entity.getDeltaMovement();
            entity.setDeltaMovement(bl ? 0.0 : delta.x, delta.y, bl2 ? 0.0 : delta.z);
        }
        if (motion.y != movement.y) {
            var delta = entity.getDeltaMovement();
            entity.setDeltaMovement(delta.x, 0.0, delta.z);
        }
    }

    // 1.21.1 EntityType ctor: no String descriptionId, no Optional<ResourceKey<LootTable>> lootTable
    private EntityUtil(EntityFactory<Entity> a, MobCategory b, boolean c, boolean d, boolean e, boolean f, ImmutableSet<Block> g, EntityDimensions h, float i, int j, int k, FeatureFlagSet n) {
        super(a, b, c, d, e, f, g, h, i, j, k, n);
    }
}