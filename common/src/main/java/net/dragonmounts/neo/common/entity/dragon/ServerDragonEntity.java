package net.dragonmounts.neo.common.entity.dragon;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.doubles.DoubleIterators;
import net.dragonmounts.neo.common.api.BredDragonsTrigger;
import net.dragonmounts.neo.common.block.DragonCoreBlock;
import net.dragonmounts.neo.common.component.DragonFood;
import net.dragonmounts.neo.common.entity.ai.control.DragonHeadLocator;
import net.dragonmounts.neo.common.entity.ai.navigation.DragonPathNavigation;
import net.dragonmounts.neo.common.entity.breath.impl.ServerBreathHelper;
import net.dragonmounts.neo.common.init.*;
import net.dragonmounts.neo.common.inventory.DragonInventory;
import net.dragonmounts.neo.common.item.DragonEssenceItem;
import net.dragonmounts.neo.common.network.s2c.FeedDragonPayload;
import net.dragonmounts.neo.common.network.s2c.SyncDragonAgePayload;
import net.dragonmounts.neo.common.tag.DMItemTags;
import net.dragonmounts.neo.common.util.ArrayUtil;
import net.dragonmounts.neo.common.util.Segment;
import net.dragonmounts.neo.common.util.math.MathUtil;
import net.dragonmounts.neo.compat.platform.ServerNetworkHandler;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.List;
import java.util.function.BiConsumer;

import static net.dragonmounts.neo.common.entity.dragon.DragonModelContracts.NECK_SEGMENTS;
import static net.dragonmounts.neo.common.util.EntityUtil.addOrResetEffect;
import static net.dragonmounts.neo.common.util.EntityUtil.addOrUpdateTransientModifier;
import static net.minecraft.resources.ResourceLocation.tryParse;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class ServerDragonEntity extends TameableDragonEntity {
    private final Segment[] neckSegments = ArrayUtil.fillArray(new Segment[NECK_SEGMENTS], Segment::new);
    public final DragonHeadLocator<ServerDragonEntity> headLocator = new DragonHeadLocator<>(this);

    // --- Bronco-style taming (server-only state) ---
    /**
     * Counts up while an untamed dragon is being ridden; when it exceeds buckThreshold the dragon bucks the rider.
     */
    private int rideTicks = 0;
    /**
     * Randomized number of ticks the player must stay mounted before the dragon attempts to buck.
     */
    private int buckThreshold = 0;
    /**
     * Successful rides accumulated toward taming.
     */
    private int breakProgress = 0;
    /**
     * How many successful rides are needed to tame.
     */
    public static final int RIDES_TO_TAME = 5;
    /**
     * Player must hang on at least this many ticks for the ride to "count" as successful.
     */
    public static final int SUCCESS_RIDE_TICKS = 60; // 3 seconds
    /**
     * Min/max ticks before a buck attempt.
     */
    public static final int BUCK_MIN_TICKS = 70;   // ~3.5s
    public static final int BUCK_MAX_TICKS = 160;  // ~8s
    /**
     * How high (blocks above the start) the dragon climbs while bucking.
     */
    public static final double BRONCO_CLIMB_HEIGHT = 40.0;
    /**
     * Speed multiplier for the bronco climb (DragonFollowPlayerFlying uses ~1.5-2.0).
     */
    public static final double BRONCO_FLY_SPEED = 2.0;
    /**
     * Horizontal radius (blocks) of the wild sweep while bucking — bigger = wider flight.
     */
    public static final double BRONCO_SWEEP_RADIUS = 24.0;
    /**
     * Position where the current break-in ride began (anchors the wide sweep + climb target).
     */
    private double breakInStartX = 0.0;
    private double breakInStartY = 0.0;
    private double breakInStartZ = 0.0;
    private int ticksClimbY = 0;
    /**
     * The player currently attempting to break in this dragon (server-side).
     */
    @Nullable
    private java.util.UUID breakingInPlayer = null;

    // --- Saddle-less flight grace (a tamed dragon will fly briefly without a saddle, then insist on one) ---
    /**
     * Ticks spent flying-while-ridden with no saddle.
     */
    private int noSaddleFlightTicks = 0;
    /**
     * Warn the rider once they've flown this long without a saddle.
     */
    public static final int NO_SADDLE_WARN_TICKS = 100;   // ~5s
    /**
     * After this long with no saddle, the dragon stops cooperating and sets the rider down.
     */
    public static final int NO_SADDLE_LAND_TICKS = 300;   // ~15s
    private boolean noSaddleWarned = false;

    public ServerDragonEntity(EntityType<? extends TameableDragonEntity> type, ServerLevel level) {
        super(type, level);
        this.setLifeStage(DragonLifeStage.ADULT, true, false);
    }

    public ServerDragonEntity(ServerLevel level, BiConsumer<ServerLevel, ServerDragonEntity> init) {
        super(DMEntities.TAMEABLE_DRAGON.get(), level);
        init.accept(level, this);
        if (this.stage == null) {
            this.setLifeStage(DragonLifeStage.ADULT, true, false);
        }
    }

    @Override
    protected ServerBreathHelper createBreathHelper() {
        return new ServerBreathHelper(this);
    }

    @Override
    public final Vec3 getHeadRelativeOffset(float x, float y, float z) {
        return this.headLocator.getHeadRelativeOffset(x, y, z);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new DragonPathNavigation(this, level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(DragonVariant.DATA_PARAMETER_KEY, this.getVariant().identifier.toString());
        if (this.stage != null) {
            tag.putString(DragonLifeStage.DATA_PARAMETER_KEY, this.stage.getSerializedName());
        }
        tag.putBoolean(AGE_LOCKED_DATA_PARAMETER_KEY, this.isAgeLocked());
        tag.putBoolean(BREAK_IN_TRUSTED_PARAMETER_KEY, this.isBreakInTrusted());
        tag.putInt(FLIGHT_RANK_PARAMETER_KEY, this.getFlightRank());
        tag.putInt(SHEARED_DATA_PARAMETER_KEY, this.isSheared() ? this.shearCooldown : 0);
        var items = this.inventory.saveItems(this.registryAccess());
        if (!items.isEmpty()) {
            tag.put(DragonInventory.DATA_PARAMETER_KEY, items);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        int age = this.age;
        var stage = this.stage;
        if (tag.contains(DragonLifeStage.DATA_PARAMETER_KEY)) {
            this.setLifeStage(DragonLifeStage.byName(tag.getString(DragonLifeStage.DATA_PARAMETER_KEY)), false, false);
        }
        if (tag.contains(DragonVariant.DATA_PARAMETER_KEY)) {
            this.setVariant(DragonVariant.REGISTRY.get(tryParse(tag.getString(DragonVariant.DATA_PARAMETER_KEY))));
        } else if (tag.contains(DragonType.DATA_PARAMETER_KEY)) {
            this.setVariant(DragonType.REGISTRY.get(tryParse(tag.getString(DragonType.DATA_PARAMETER_KEY))).variants.draw(this.random, DragonVariants.ENDER_FEMALE, true));
        } else {
            this.applyType(this.getDragonType());
        }
        super.readAdditionalSaveData(tag);
        this.setInSittingPose(this.isOrderedToSit() && this.onGround());
        if (tag.contains(BREAK_IN_TRUSTED_PARAMETER_KEY)) {
            this.setBreakInTrusted(tag.getBoolean(BREAK_IN_TRUSTED_PARAMETER_KEY));
        }
        if (tag.contains(FLIGHT_RANK_PARAMETER_KEY)) {
            this.setFlightRank(tag.getInt(FLIGHT_RANK_PARAMETER_KEY));
        }
        if (!this.firstTick && (this.age != age || stage != this.stage)) {
            ServerNetworkHandler.sendTracking(this, new SyncDragonAgePayload(this.getId(), this.age, this.stage));
        }
        if (tag.contains(SADDLE_DATA_PARAMETER_KEY)) {
            this.inventory.saddle.setLocal(ItemStack.parseOptional(this.registryAccess(), tag.getCompound(SADDLE_DATA_PARAMETER_KEY)), true);
        }
        if (tag.contains(SHEARED_DATA_PARAMETER_KEY)) {
            this.setSheared(tag.getInt(SHEARED_DATA_PARAMETER_KEY));
        }
        if (tag.contains(SLEEPING_DATA_PARAMETER_KEY)) {
            this.setSleeping(tag.getBoolean(SLEEPING_DATA_PARAMETER_KEY));
        }
        if (tag.contains(AGE_LOCKED_DATA_PARAMETER_KEY)) {
            this.setAgeLocked(tag.getBoolean(AGE_LOCKED_DATA_PARAMETER_KEY));
        }
        if (tag.contains(DragonInventory.DATA_PARAMETER_KEY)) {
            this.inventory.loadItems(tag.getList(DragonInventory.DATA_PARAMETER_KEY, 10), this.registryAccess());
        }
    }

    public void spawnEssence(ItemStack stack) {
        if (!isBaby()) {
            var pos = this.blockPosition();
            var level = this.level();
            var state = DMBlocks.DRAGON_CORE.defaultBlockState().setValue(HORIZONTAL_FACING, this.getDirection());
            if (!DragonCoreBlock.tryPlaceAt(level, pos, state, stack)) {
                int y = pos.getY(), max = Math.min(y + 5, level.getMaxBuildHeight());
                var mutable = pos.mutable();
                while (++y < max) {
                    if (DragonCoreBlock.tryPlaceAt(level, mutable.setY(y), state, stack)) return;
                }
            } else return;
            level.addFreshEntity(new ItemEntity(level, this.getX(), this.getY(), this.getZ(), stack));
        }
    }

    @Override
    protected void checkCrystals() {
        if (this.nearestCrystal != null && this.nearestCrystal.isAlive()) {
            if (++this.crystalTicks > 0 && this.getHealth() < this.getMaxHealth()) {
                this.crystalTicks = -10;
                this.heal(1.0F);
                addOrResetEffect(this, MobEffects.DAMAGE_BOOST, 300, 0, false, true, true, 101);//15s
            }
            if (this.random.nextInt(20) == 0) {
                this.nearestCrystal = this.findCrystal();
            }
        } else {
            this.nearestCrystal = this.random.nextInt(10) == 0 ? this.findCrystal() : null;
        }
    }

    @Override
    protected void applyType(DragonType type) {
        if (this.lastType == type) return;
        float health = this.getHealth() / this.getMaxHealth();
        if (this.lastType != null) {
            this.getAttributes().removeAttributeModifiers(this.lastType.attributes);
        }
        this.getAttributes().addTransientAttributeModifiers(type.attributes);
        this.setHealth(health * this.getMaxHealth());
        this.breathHelper.onTypeChange(type);
        this.lastType = type;
    }

    @Override
    protected Brain.Provider<ServerDragonEntity> brainProvider() {
        return DragonAi.brainProvider();
    }

    @Override
    protected Brain<ServerDragonEntity> makeBrain(Dynamic<?> dynamic) {
        return DragonAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<ServerDragonEntity> getBrain() {
        return (Brain<ServerDragonEntity>) super.getBrain();
    }

    @Override
    protected void customServerAiStep() {
        DragonAi.tickBrain((ServerLevel) level(), this);
    }

    private void tickAnimalPassengers() {
        if (this.level().isClientSide) return;
        if (!this.isTrustingAnyPlayer()) return;

        this.checkInsideBlocks();
        List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F), EntitySelector.pushableBy(this));
        if (!list.isEmpty()) {

            for (Entity entity : list) {
                if (!entity.hasPassenger(this)) {
                    if (this.getPassengers().size() < this.getMaxPassengers()
                            && !entity.isPassenger()
                            && this.hasEnoughSpaceFor(entity)
                            && (entity instanceof LivingEntity || entity instanceof Villager)
                            && !(entity instanceof WaterAnimal)
                            && !(entity instanceof Player)
                            && !(entity instanceof Enemy)) {
                        entity.startRiding(this);
                    } else {
                        this.push(entity);
                    }
                }
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (passenger instanceof Player player && player == this.getControllingPassenger()
                && this.isFlying() && this.getPassengers().size() == 1) {
            float dragonYaw = this.getYRot();
            // lock the MODEL orientation to the dragon (NOT setYRot — that moves the camera)
            player.setYBodyRot(dragonYaw);
            player.setYHeadRot(dragonYaw);
            player.yBodyRotO = this.yRotO;
            player.yHeadRotO = this.yRotO;
        }

        if (!(passenger instanceof Player)) {
            float dragonYaw = this.getYRot();
            passenger.setYRot(dragonYaw);
            passenger.yRotO = dragonYaw;            // prev-tick yaw, prevents render interpolation swing
            if (passenger instanceof LivingEntity living) {
                living.setYBodyRot(dragonYaw);
                living.setYHeadRot(dragonYaw);
                living.yBodyRotO = this.yRotO;
                living.yHeadRotO = this.yRotO;
            }
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        boolean wasDriver = (passenger instanceof Player);
        super.removePassenger(passenger);
        if (wasDriver) {
            this.ejectPassengers();   // driver left -> drop all animal passengers too
        }
    }

    public boolean hasEnoughSpaceFor(Entity entity) {
        return entity.getBbWidth() < this.getBbWidth();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float scale) {
        int seat;
        Player driver = this.getControllingPassenger();   // the player driver, if any
        if (entity == driver) {
            seat = 0;   // driver -> front-center
        } else {
            // non-driver passengers fill seats 1, 2 by their order, skipping the driver
            int idx = 0;
            seat = 1;
            for (Entity p : this.getPassengers()) {
                if (p == driver) continue;     // don't count the driver
                if (p == entity) break;
                idx++;
                seat++;
            }
        }
        return this.getDragonType().locatePassenger(seat, this.isInSittingPose())
                .scale(scale * MathUtil.MOJANG_MODEL_SCALE)
                .yRot(-MathUtil.TO_RAD_FACTOR * this.yBodyRot);
    }


    /**
     * Friendly = not a hostile mob.
     */
    private static boolean isFriendly(Entity entity) {
        return !(entity instanceof Enemy);   // Enemy is the hostile-mob marker (zombies, skeletons, etc.)
    }

    /**
     * Bronco-style taming tick. While an unbroken dragon carries a rider:
     * - it forces itself airborne and flies erratically,
     * - after a randomized delay it BUCKS the rider off (mid-air = a real fall),
     * - if the rider hung on long enough, the ride counts toward taming,
     * - enough successful rides -> tamed.
     */
    private void tickBronco() {
        if (this.level().isClientSide) return;

        // The player clinging on during a break-in (untamed dragon).
        Player rider = this.getBreakInRider();
        if (rider == null || (this.isBreakInTrusted() && isTame())) {
            this.rideTicks = 0;
            return;
        }

        // Safety: baby dragons can never be broken in.
        if (this.isBaby()) {
            this.ejectPassengers();
            this.rideTicks = 0;
            return;
        }

        // try to limit how the dragon flies too high
        if (ticksClimbY < 120) {
            ticksClimbY++;
        }

        // reset it to 0 so it starts flying high again when on ground;
        if (onGround() && ticksClimbY > 0) {
            ticksClimbY = 0;
        }

        // Drive the dragon's own flight controller to climb HIGH and weave around,
        // exactly like DragonFollowPlayerFlying does — but toward a wild point far
        // above the start, so the player is carried dangerously high.
        this.setFlying(true);
        this.setOrderedToSit(false);

        // Anchor the climb at where the ride began (first tick records it).
        if (this.rideTicks == 0) {
            this.breakInStartX = this.getX();
            this.breakInStartY = this.getY();
            this.breakInStartZ = this.getZ();
        }

        // Target: high above, sweeping in WIDE arcs anchored to where the ride began
        // (anchoring to the start — not the live position — makes it cover a large area
        // instead of chasing its own tail in tight circles).
        double climbHeight = ticksClimbY < 120 ? BRONCO_CLIMB_HEIGHT : 0;
        double climbTarget = this.breakInStartY + climbHeight;
        double t = this.tickCount * 0.12;                 // slower phase = broader, sweeping arcs
        double sweepX = Math.sin(t) * BRONCO_SWEEP_RADIUS;
        double sweepZ = Math.cos(t * 0.6) * BRONCO_SWEEP_RADIUS;  // different freq -> figure-8 / wandering path

        // Stop any pathfinding/brain walk target from competing with our climb.
        this.getNavigation().stop();
        this.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);

        this.getMoveControl().setWantedPosition(
                this.breakInStartX + sweepX,
                climbTarget,
                this.breakInStartZ + sweepZ,
                BRONCO_FLY_SPEED
        );

        ++this.rideTicks;

        // Time to resolve the ride?
        if (this.rideTicks >= this.buckThreshold) {
            boolean success = this.rideTicks >= SUCCESS_RIDE_TICKS;
            // Would this successful ride be the one that finally tames it?
            boolean willTame = success
                    && (this.breakProgress + 1) >= RIDES_TO_TAME
                    && this.breakingInPlayer != null
                    && rider.getUUID().equals(this.breakingInPlayer);

            if (willTame) {
                // BROKEN IN — accept the rider. Do NOT buck. Keep flying a victory lap.
                ++this.breakProgress;
                this.tame(rider);
                this.setBreakInTrusted(true);
                this.level().broadcastEntityEvent(this, ON_TAMING_SUCCEED); // hearts
                this.rideTicks = 0;
                // Dragon is tamed now; the rider keeps flying. The saddle-less flight
                // timer (in TameableDragonEntity) will warn and eventually set them down.
            } else {
                this.buckOffRider(rider, success);
            }
        }
    }

    /**
     * Throw the current rider off (a non-final outcome). If {@code success} the ride
     * counted toward taming but didn't finish it; either way the dragon bucks and smokes.
     */
    private void buckOffRider(Player rider, boolean success) {
        Level level = this.level();

        // Fling the player off with some momentum so they actually tumble.
        Vec3 fling = this.getLookAngle().scale(-0.4).add(0.0, 0.3, 0.0);
        this.ejectPassengers();
        rider.setDeltaMovement(rider.getDeltaMovement().add(fling));
        rider.hasImpulse = true;
        rider.hurtMarked = true;

        if (success) {
            ++this.breakProgress;   // progress, but not the final ride
        }
        level.broadcastEntityEvent(this, ON_TAMING_FAIL); // smoke either way — it threw you

        this.rideTicks = 0;
        this.buckThreshold = BUCK_MIN_TICKS + this.random.nextInt(BUCK_MAX_TICKS - BUCK_MIN_TICKS);
    }

    @Override
    public void aiStep() {
        this.tickBronco();
        if (this.isDeadOrDying()) {
            this.nearestCrystal = null;
        } else {
            this.checkCrystals();
        }
        if (this.shearCooldown > 0) {
            this.setSheared(this.shearCooldown - 1);
        }
        if (this.projectileCooldown > 0) this.projectileCooldown--;
        this.headLocator.tick();
        this.headLocator.calculateHeadAndNeck(
                this.neckSegments,
                this.getXRot(),
                this.yHeadRot - this.yBodyRot
        );
        tickAnimalPassengers();
        this.breathHelper.tick();
        if (this.isAgeLocked()) {
            int age = this.age;
            this.age = 0;
            super.aiStep();
            this.age = age;
        } else {
            super.aiStep();
        }
        if (this.isNearGround(0.25)) {
            this.flightTicks = 0;
        } else {
            ++this.flightTicks;
        }
        this.setFlying(++this.flightTicks > LIFTOFF_THRESHOLD && !this.isBaby() && !this.isInWater() && (
                this.fluidHeight.isEmpty() || DoubleIterators.all(
                        this.fluidHeight.values().doubleIterator(),
                        value -> value == 0.0
                ) || this.isBeingRiddenByPlayer()
        ));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean isOwner = this.isOwnedBy(player);
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isBreathing()) {
            DragonFood food = DragonFood.getInstance(stack);

            if (food != null) {

                if ((food.requiresOwner() && !isOwner) ||
                        (!food.canAlwaysFeed() && this.getHealth() >= this.getMaxHealth() && this.isTame())) {
                    return InteractionResult.FAIL;
                }

                Level level = this.level();
                boolean locked = this.isAgeLocked();

//                for (var effect : food.effects()) {
//                    // effect MUST be your custom wrapper in 1.21.1
//                    effect.apply(level, this);
//                }

                if (!locked) {
                    this.ageUp(food.age(), false);
                }

                this.heal(food.health());

                if (isOwner) {
                    if (this.getLifeStage() == DragonLifeStage.ADULT && this.canFallInLove()) {
                        this.setInLove(player);
                    }
                } else if (!this.isTame()) {
                    if (this.random.nextFloat() < food.tamingProbability()) {
                        this.tame(player);
                        level.broadcastEntityEvent(this, ON_TAMING_SUCCEED); // reuse heart particles for "now trusts"
                    } else {
                        level.broadcastEntityEvent(this, ON_TAMING_FAIL);
                    }
                    // already trusting: feeding still heals/ages but no further effect here
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);   // ← fixed

                ServerNetworkHandler.sendTracking(
                        this,
                        new FeedDragonPayload(this.getId(), this.age, this.stage, stack)
                );

                return InteractionResult.SUCCESS;
            }
        }

        // --- Bronco taming: mount an untamed-but-trusting dragon to break it in ---
        if (!this.isTame() && this.isBreakInTrusted() && stack.isEmpty()
                && !this.isBaby() && this.getPassengers().isEmpty()) {
            if (this.level().isClientSide) return InteractionResult.SUCCESS;
            this.setOrderedToSit(false);
            this.breakingInPlayer = player.getUUID();
            this.rideTicks = 0;
            this.buckThreshold = BUCK_MIN_TICKS + this.random.nextInt(BUCK_MAX_TICKS - BUCK_MIN_TICKS);
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this, true);
            return InteractionResult.SUCCESS;
        }

        if (isTrustingAnyPlayer()) {
            return InteractionResult.SUCCESS;
        } else if (!isOwner) {
            return InteractionResult.PASS;
        }

        if (this.inventory.onInteract(stack)) return InteractionResult.SUCCESS;

        if (stack.is(DMItemTags.BATONS)) {
            this.setOrderedToSit(!this.isOrderedToSit());
            return InteractionResult.SUCCESS;
        }

        InteractionResult result = stack.interactLivingEntity(player, this, hand);
        if (result.consumesAction()) return result;

        if (player.isSecondaryUseActive()) {
            this.openCustomInventoryScreen(player);

        } else if (this.isTame() && this.isBreakInTrusted() && stack.isEmpty()
                && !this.isBaby()) {
            this.setOrderedToSit(false);
            player.setYRot(this.getYRot());
            player.setXRot(this.getXRot());
            player.startRiding(this);
        } else {
            this.openCustomInventoryScreen(player);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        super.thunderHit(level, bolt);
        this.getDragonType().onThunderHit(this, bolt);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this, ON_ATTACK);
        return super.doHurtTarget(target);
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (super.hurt(source, amount)) {
            if (!this.isBreathing() && this.random.nextFloat() < 0.25F) {
                level().broadcastEntityEvent(this, ON_ROAR);
            }
            if (!source.is(DamageTypes.IN_WALL)) {
                // don't just sit there!
                this.setOrderedToSit(false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.ejectPassengers();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.isTame()) {
            this.spawnEssence(this.getDragonType().getInstance(DragonEssenceItem.class, DMItems.ENDER_DRAGON_ESSENCE.get())
                    .saveEntity(this, DataComponentPatch.EMPTY)
            );
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity, (this.age << 3) | this.stage.ordinal());
    }

    @Override
    public void setLifeStage(DragonLifeStage stage, boolean reset, boolean sync) {
        var modifier = stage.makeModifier(1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        var attributes = this.getAttributes();
        float health = this.getHealth() / this.getMaxHealth();
        addOrUpdateTransientModifier(attributes, Attributes.MAX_HEALTH, modifier);
        addOrUpdateTransientModifier(attributes, Attributes.ATTACK_DAMAGE, modifier);
        addOrUpdateTransientModifier(attributes, Attributes.ARMOR, stage.makeModifier(ServerConfig.INSTANCE.baseArmor.get(), AttributeModifier.Operation.ADD_VALUE));
        addOrUpdateTransientModifier(attributes, Attributes.STEP_HEIGHT, stage.makeModifier(ServerConfig.INSTANCE.baseStepHeight.get(), AttributeModifier.Operation.ADD_VALUE));
        this.setHealth(health * this.getMaxHealth());
        if (this.stage == stage) return;
        this.stage = stage;
        if (reset) {
            this.refreshAge();
        }
        this.reapplyPosition();
        this.refreshDimensions();
        if (sync) {
            ServerNetworkHandler.sendTracking(this, new SyncDragonAgePayload(this.getId(), this.age, stage));
        }
    }

    @Override
    public void setAge(int age) {
        if (this.age == age) return;
        if (this.age < 0 && age >= 0 || this.age > 0 && age <= 0) {
            this.ageBoundaryReached();
        } else {
            this.age = age;
        }
        ServerNetworkHandler.sendTracking(this, new SyncDragonAgePayload(this.getId(), age, this.stage));
    }

    @Override
    protected void tickDeath() {
        if (++this.deathTime >= this.getMaxDeathTime()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        boolean flag = this.isBeingRiddenByPlayer();
        super.addPassenger(passenger);
        if (!flag && this.isBeingRiddenByPlayer()) {
            this.getBrain().setMemory(DMMemories.IS_CONTROLLED, Unit.INSTANCE);
        }
    }

    /// @see #finalizeSpawnChildFromBreeding(ServerLevel, Animal, AgeableMob)
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal other) {
        if (!(other instanceof ServerDragonEntity mate)) return;
        var egg = new HatchableDragonEggEntity(level);
        egg.setDragonType(this.getDragonType(), true);
        var pos = this.position();
        egg.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        var cause = this.getLoveCause();
        if (cause == null) {
            cause = mate.getLoveCause();
        }
        if (cause != null) {
            cause.awardStat(Stats.ANIMALS_BRED);
            ((BredDragonsTrigger) CriteriaTriggers.BRED_ANIMALS).neodragonmounts$trigger(cause, this, mate, egg);
        }
        this.resetLove();
        mate.resetLove();
        level.broadcastEntityEvent(this, (byte) 18);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            level.addFreshEntity(new ExperienceOrb(level, pos.x, pos.y, pos.z, this.getRandom().nextInt(12) + 4));
        }
        level.addFreshEntityWithPassengers(egg);
    }

    @Override
    public void checkDespawn() {
        this.noActionTime = 0;
    }

    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;// double insurance
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        // Inventory is only accessible on a TAMED dragon, and only by its owner.
        // An untamed dragon — even one that trusts the player and is being ridden
        // during a break-in attempt — never exposes its inventory.
        if (!this.isTame() || !this.isOwnedBy(player) || !this.isBreakInTrusted()) return;
        player.openMenu(this);
    }

    /// Never called from server side
    @Override
    public void onPlayerJump(int power) {
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return null;
    }
}