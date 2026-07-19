package net.dragonmounts.neo.common.entity.dragon;

import com.mojang.logging.LogUtils;
import net.dragonmounts.neo.common.api.AutoJumpRideable;
import net.dragonmounts.neo.common.api.ConditionalShearable;
import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.api.DynamicAttributeEntity;
import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.common.component.DragonFood;
import net.dragonmounts.neo.common.entity.ai.control.DragonBodyControl;
import net.dragonmounts.neo.common.entity.ai.control.DragonMoveControl;
import net.dragonmounts.neo.common.entity.breath.DragonBreathHelper;
import net.dragonmounts.neo.common.entity.projectile.ability.DragonProjectileAbility;
import net.dragonmounts.neo.common.init.DMItems;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.common.init.DragonVariants;
import net.dragonmounts.neo.common.inventory.DragonInventory;
import net.dragonmounts.neo.common.inventory.DragonInventoryHandler;
import net.dragonmounts.neo.common.item.DragonScalesItem;
import net.dragonmounts.neo.common.item.DragonSpawnEggItem;
import net.dragonmounts.neo.common.tag.DMItemTags;
import net.dragonmounts.neo.common.util.EntityUtil;
import net.dragonmounts.neo.common.util.math.MathUtil;
import net.dragonmounts.neo.compat.platform.MenuProvider;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.dragonmounts.neo.compat.registry.DragonVariant;
import net.dragonmounts.neo.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * @see Mule
 * @see Horse
 */
public abstract class TameableDragonEntity extends TamableAnimal implements
        MenuProvider<TameableDragonEntity>,
        HasCustomInventoryScreen,
        ConditionalShearable,
        AutoJumpRideable,
        FlyingAnimal,
        Saddleable,
        DynamicAttributeEntity,
        DragonTypified.Mutable,
        GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float renderPitch;
    public float renderPitchO;
    public float renderRoll;
    public float renderRollO;
    private float lastYRotForRoll;

    public static TameableDragonEntity construct(EntityType<? extends TameableDragonEntity> type, Level level) {
        return level instanceof ServerLevel server ? new ServerDragonEntity(type, server) : new ClientDragonEntity(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        var config = ServerConfig.INSTANCE;
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ARMOR, config.baseArmor.get())
                .add(Attributes.ARMOR_TOUGHNESS, config.baseArmorToughness.get())
                .add(Attributes.ATTACK_DAMAGE, config.baseDamage.get())
                .add(Attributes.MAX_HEALTH, config.baseHealth.get())
                .add(Attributes.ATTACK_KNOCKBACK, config.baseKnockback.get())
                .add(Attributes.KNOCKBACK_RESISTANCE, config.baseKnockbackResistance.get())
                .add(Attributes.FOLLOW_RANGE, config.baseFollowRange.get())
                .add(Attributes.MOVEMENT_SPEED, config.baseMovementSpeed.get())
                .add(Attributes.FLYING_SPEED, config.baseFlyingSpeed.get())
                .add(Attributes.SCALE, config.baseBodySize.get())
                .add(Attributes.JUMP_STRENGTH, config.baseJumpStrength.get())
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, config.baseWaterMovementEfficiency.get());
    }

    public static final double LIFTOFF_THRESHOLD = 10;
    protected static final Logger LOGGER = LogUtils.getLogger();
    // flags
    public static final byte ON_ATTACK = 4;
    public static final byte ON_ROAR = 67;
    public static final byte ON_TAMING_SUCCEED = 7;
    public static final byte ON_TAMING_FAIL = 6;
    // data value IDs
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_AGE_LOCKED = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOVER_DISABLED = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BREATHING = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHEARED = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TRUST_OTHER = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Bronco taming: set once the wild dragon has been fed enough to allow break-in rides. Distinct from DATA_TRUST_OTHER.
     */
    private static final EntityDataAccessor<Boolean> DATA_BREAK_IN_TRUST = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.BOOLEAN);
    /**
     * Player-set V-formation flight rank (0 = auto/unset; 1 = innermost wing, 2 = next, ...).
     */
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_RANK = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_CHEST_ITEM = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_SADDLE_ITEM = SynchedEntityData.defineId(TameableDragonEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<DragonVariant> DATA_DRAGON_VARIANT = SynchedEntityData.defineId(TameableDragonEntity.class, DragonVariant.SERIALIZER);
    public static final String AGE_LOCKED_DATA_PARAMETER_KEY = "AgeLocked";
    public static final String FLYING_DATA_PARAMETER_KEY = "Flying";
    public static final String SADDLE_DATA_PARAMETER_KEY = "Saddle";
    public static final String SHEARED_DATA_PARAMETER_KEY = "ShearCooldown";
    public static final String SLEEPING_DATA_PARAMETER_KEY = "Sleeping";
    public static final String BREAK_IN_TRUSTED_PARAMETER_KEY = "BreakInTrust";
    public static final String FLIGHT_RANK_PARAMETER_KEY = "FlightRank";
    protected DragonType lastType;
    protected EndCrystal nearestCrystal;
    protected DragonLifeStage stage;
    protected boolean hasChest;
    protected boolean isSaddled;
    protected int flightTicks;
    protected int crystalTicks;
    protected int shearCooldown;
    protected int projectileCooldown;
    public final DragonInventory inventory = new DragonInventory(
            this,
            DATA_CHEST_ITEM,
            this::setChested,
            DATA_SADDLE_ITEM,
            this::setSaddled
    );
    public final DragonBreathHelper<?> breathHelper = this.createBreathHelper();

    public TameableDragonEntity(EntityType<? extends TameableDragonEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.moveControl = new DragonMoveControl(this);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new DragonBodyControl(this);
    }

    protected abstract DragonBreathHelper<?> createBreathHelper();

    public abstract Vec3 getHeadRelativeOffset(float x, float y, float z);

    public final float getAdjustedSize() {
        return this.getAgeScale() * this.getScale();
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return true;
    }

    public final DragonVariant getVariant() {
        return this.entityData.get(DATA_DRAGON_VARIANT);
    }

    public final void setVariant(DragonVariant variant) {
        this.entityData.set(DATA_DRAGON_VARIANT, variant);
    }

    public boolean isNearGround(double factor) {
        var box = this.getBoundingBox();
        double offset = (box.maxY - box.minY) * factor;
        for (var shape : this.level().getBlockCollisions(this, new AABB(
                box.minX,
                box.minY - offset,
                box.minZ,
                box.maxX,
                box.maxY - offset,
                box.maxZ
        ))) {
            if (!shape.isEmpty()) return true;
        }
        return false;
    }

    public final int getMaxDeathTime() {
        return 120;
    }

    public final void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
        this.setNoGravity(flying);
    }

    @Override
    public final boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    public final float adjustSpeed(float speed) {
        return this.isSprinting() ? speed * 1.5F + 0.25F : speed;
    }

    public final void setHoverDisabled(boolean disabled) {
        this.entityData.set(DATA_HOVER_DISABLED, disabled);
    }

    public final boolean isHoverDisabled() {
        return this.entityData.get(DATA_HOVER_DISABLED);
    }

    public final boolean isBreathing() {
        return this.entityData.get(DATA_BREATHING);
    }

    public final void setBreathing(boolean breathing) {
        this.entityData.set(DATA_BREATHING, breathing && this.getLifeStage().isOldEnough(DragonLifeStage.FLEDGLING) && this.breathHelper.canBreathe());
    }

    public final boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    public final void setSleeping(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }

    public boolean isMovementDisabled() {
        return super.isInSittingPose() || isSleeping();
    }

    protected abstract void checkCrystals();

    protected @Nullable EndCrystal findCrystal() {
        EndCrystal result = null;
        double min = Double.MAX_VALUE;
        for (var crystal : this.level().getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0))) {
            double distance = crystal.distanceToSqr(this);
            if (distance < min) {
                min = distance;
                result = crystal;
            }
        }
        return result;
    }

    protected abstract void applyType(DragonType type);

    //----------Entity----------
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLYING, false);
        builder.define(DATA_SHEARED, false);
        builder.define(DATA_AGE_LOCKED, false);
        builder.define(DATA_HOVER_DISABLED, false);
        builder.define(DATA_BREATHING, false);
        builder.define(DATA_SLEEPING, false);
        builder.define(DATA_TRUST_OTHER, false);
        builder.define(DATA_BREAK_IN_TRUST, false);
        builder.define(DATA_FLIGHT_RANK, 0);
        builder.define(DATA_SADDLE_ITEM, ItemStack.EMPTY);
        builder.define(DATA_CHEST_ITEM, ItemStack.EMPTY);
        builder.define(DATA_DRAGON_VARIANT, DragonVariants.ENDER_FEMALE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (accessor.equals(DATA_SADDLE_ITEM)) {
            this.inventory.saddle.setLocal(this.entityData.get(DATA_SADDLE_ITEM), false);
        } else if (accessor.equals(DATA_CHEST_ITEM)) {
            this.inventory.chest.setLocal(this.entityData.get(DATA_CHEST_ITEM), false);
        } else if (accessor.equals(DATA_DRAGON_VARIANT)) {
            this.applyType(this.entityData.get(DATA_DRAGON_VARIANT).type);
        } else {
            super.onSyncedDataUpdated(accessor);
        }
    }

    @Deprecated
    @Override
    public @Nullable TameableDragonEntity getBreedOffspring(ServerLevel level, AgeableMob parent) {
        return null;
    }

    public boolean hasChest() {
        return this.hasChest;
    }

    @Override
    public final boolean isSaddled() {
        return this.isSaddled;
    }

    @Override
    public void refreshDimensions() {
        Vec3 pos = this.position();
        super.refreshDimensions();
        this.setPos(pos.x, pos.y, pos.z);
    }

    protected int getMaxPassengers() {
        return 3;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 3;
    }

    @Override
    public float getAgeScale() {
        return DragonLifeStage.getSize(this.stage, this.age);
    }

    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.getType().getDimensions().scale(DragonLifeStage.getSizeAverage(this.stage));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && DragonFood.isDragonFood(stack);
    }

    @Override
    protected void dropEquipment() {
        this.inventory.dropContents(false, 0);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        var entity = source.getEntity();
        return (entity != null && (entity == this || this.hasPassenger(entity))) || super.isInvulnerableTo(source) || this.getDragonType().isInvulnerableTo(source);
    }

    @Override
    protected int calculateFallDamage(float distance, float damageMultiplier) {
        return 0;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    public abstract void setLifeStage(DragonLifeStage stage, boolean reset, boolean sync);

    public final DragonLifeStage getLifeStage() {
        return this.stage;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(this.getDragonType().getInstance(DragonSpawnEggItem.class, DMItems.ENDER_DRAGON_SPAWN_EGG.get()));
    }

    public boolean isBeingRiddenByPlayer() {
        return this.getFirstPassenger() instanceof Player;
    }

    @Override
    public @Nullable Player getControllingPassenger() {
        // Untamed dragons are never controlled by their rider: during a break-in
        // attempt the dragon flies ITSELF (via its move control) while the player
        // merely clings on. Only a tamed dragon yields control to the rider.
        if (!this.isTame()) return null;
        return !this.isNoAi() && isBreakInTrusted() && this.getFirstPassenger() instanceof Player player ? player : null;
    }

    /**
     * The player currently clinging to an untamed dragon during a break-in (may not be the controller).
     */
    @Nullable
    public Player getBreakInRider() {
        return this.getControllingPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
        int seat;
        Player driver = this.getControllingPassenger();
        if (entity == driver) {
            seat = 0;
        } else {
            int s = (driver == null) ? 0 : 1;
            for (Entity p : this.getPassengers()) {
                if (p == driver) continue;
                if (p == entity) break;
                s++;
            }
            seat = s;
        }

        Vec3 base = this.getDragonType().locatePassenger(seat, this.isInSittingPose())
                .scale(MathUtil.MOJANG_MODEL_SCALE);

        if (this.getPassengers().size() == 1 && this.isFlying()) {
            float pitch = Mth.lerp(partialTick, this.renderPitchO, this.renderPitch);
            float roll = Mth.lerp(partialTick, this.renderRollO, this.renderRoll);
            base = base.xRot(-pitch * MathUtil.TO_RAD_FACTOR);   // pitch shifts seat fwd/back+up/down
            base = base.zRot(-roll * MathUtil.TO_RAD_FACTOR);     // roll shifts seat left/right (the part you want)
        }
        return base.yRot(-MathUtil.TO_RAD_FACTOR * this.yBodyRot);
    }

    //----------MobEntity----------
    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return !effectInstance.is(MobEffects.WEAKNESS) && !this.getVariant().type.isImmuneTo(effectInstance.getEffect()) && super.canBeAffected(effectInstance);
    }

    @Override
    protected Component getTypeName() {
        return this.getDragonType().getFormattedName("entity.neodragonmounts.dragon.name");
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
    }

    @Override
    public SlotAccess getSlot(int slot) {
        return switch (slot) {
            case 400 -> this.inventory.saddle;
            case 499 -> this.inventory.chest;
            default -> {
                var access = this.inventory.access(slot);
                yield access == SlotAccess.NULL ? super.getSlot(slot) : access;
            }
        };
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (this.isInWater() || this.isFlying() || this.isMovementDisabled()) return;
        if (this.isBaby()) {
            super.playStepSound(this.getPrimaryStepSoundBlockPos(pos), state);
        } else {
            this.playSound(DMSounds.DRAGON_STEP, 0.2F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getDragonType().getAmbientSound(this);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getDragonType().getDeathSound(this);
    }


    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return this.getDragonType().getLootTable();
    }


    //----------AgeableEntity----------

    protected void refreshAge() {
        switch (this.stage.ordinal()) {
            case 0:// NEWBORN
            case 1:// INFANT
                this.age = -this.stage.duration;
                return;
            case 2:// JUVENILE
            case 3:// PREJUVENILE
                this.age = this.stage.duration;
                return;
            default:
                this.age = 0;
        }
    }

    public void setAgeLocked(boolean locked) {
        this.entityData.set(DATA_AGE_LOCKED, locked);
    }

    public final boolean isAgeLocked() {
        return this.entityData.get(DATA_AGE_LOCKED);
    }

    @Override
    protected void ageBoundaryReached() {
        this.setLifeStage(DragonLifeStage.byId(this.stage.ordinal() + 1), true, false);
    }

    @Override
    public void ageUp(int amount, boolean forced) {
        int old = this.age;
        //Notice:                           ↓↓                      ↓↓              ↓↓           ↓↓
        if (!this.isAgeLocked() && (old < 0 && (this.age += amount) >= 0 || old > 0 && (this.age -= amount) <= 0)) {
            this.ageBoundaryReached();
            if (forced) {
                this.forcedAge += old < 0 ? -old : old;
            }
        }
    }

    @Override
    public final int getAge() {
        return this.age;
    }

    @Override
    public final void setBaby(boolean value) {
        this.setLifeStage(value ? DragonLifeStage.HATCHLING : DragonLifeStage.ADULT, true, true);
    }

    @Override
    protected int getBaseExperienceReward() {
        return 0;
    }

    public boolean isPlayerTrusted(Player player) {
        return this.isTrustingAnyPlayer();
    }

    public boolean isTrustingAnyPlayer() {
        return this.entityData.get(DATA_TRUST_OTHER);
    }

    public void setTrustingAnyPlayer(boolean state) {
        this.entityData.set(DATA_TRUST_OTHER, state);
    }

    /**
     * Bronco taming: whether this wild dragon has been fed enough to be mounted for break-in.
     */
    public boolean isBreakInTrusted() {
        return this.entityData.get(DATA_BREAK_IN_TRUST);
    }

    public void setBreakInTrusted(boolean state) {
        this.entityData.set(DATA_BREAK_IN_TRUST, state);
    }

    /**
     * Player-set V-formation flight rank. 0 means "auto" (fall back to age-based ordering).
     */
    public int getFlightRank() {
        return this.entityData.get(DATA_FLIGHT_RANK);
    }

    public void setFlightRank(int rank) {
        this.entityData.set(DATA_FLIGHT_RANK, Math.max(0, rank));
    }

    /**
     * Assigns the lowest rank not already used by another of this owner's nearby dragons.
     * Useful to avoid duplicate numbers when auto-ranking at spawn or from the GUI. Returns
     * the rank chosen. If no owner/level context is available, leaves the dragon on Auto (0).
     */
    public int assignNextFreeRank() {
        var owner = this.getOwner();
        if (owner == null || this.level().isClientSide) return this.getFlightRank();
        var used = new java.util.HashSet<Integer>();
        var area = this.getBoundingBox().inflate(96.0);
        for (var other : this.level().getEntitiesOfClass(TameableDragonEntity.class, area,
                d -> d != this && owner.equals(d.getOwner()))) {
            int r = other.getFlightRank();
            if (r > 0) used.add(r);
        }
        int rank = 1;
        while (used.contains(rank)) ++rank;
        this.setFlightRank(rank);
        return rank;
    }

    //----------IDragonTypified.Mutable----------

    @Override
    public final void setDragonType(DragonType type, boolean reset) {
        var previous = this.getVariant();
        if (previous.type != type || reset) {
            DragonVariant drawn = type.variants.draw(this.random, previous, true);
            this.setVariant(drawn);
        }
        if (reset) this.setHealth(this.getMaxHealth());
    }

    @Override
    public final DragonType getDragonType() {
        return this.getVariant().type;
    }

    //----------ConditionalShearable----------

    public final boolean isSheared() {
        return this.entityData.get(DATA_SHEARED);
    }

    public final void setSheared(int cooldown) {
        this.shearCooldown = cooldown;
        this.entityData.set(DATA_SHEARED, cooldown > 0);
    }

    public int getProjectileCooldown() {
        return this.projectileCooldown;
    }

    public void setProjectileCooldown(int ticks) {
        this.projectileCooldown = ticks;
    }

    @Override
    public boolean readyForShearing(ServerLevel level, ItemStack stack) {
        return this.isAlive() && this.stage.ordinal() >= 2 && !this.isSheared() && stack.is(DMItemTags.HARD_SHEARS);
    }

    @Override
    public boolean shear(ServerLevel level, @Nullable Player player, ItemStack stack, BlockPos pos, SoundSource source) {
        var scale = this.getDragonType().getInstance(DragonScalesItem.class, null);
        if (scale == null) return false;
        level.playSound(player, this, DMSounds.DRAGON_PURR, source, 1.0F, 1.0F);
        var random = this.random;
        var item = this.spawnAtLocation(new ItemStack(scale), 1.0F);
        if (item != null) {
            item.setDeltaMovement(item.getDeltaMovement().add(
                    (random.nextFloat() - random.nextFloat()) * 0.1F,
                    random.nextFloat() * 0.05F,
                    (random.nextFloat() - random.nextFloat()) * 0.1F
            ));
        }
        this.setSheared(2500 + random.nextInt(1000));
        return true;
    }

    @Override
    public boolean isSaddleable() {
        return !this.isBaby() && this.isTame();
    }

    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        this.inventory.saddle.set(stack);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        if (data instanceof DragonSpawnData $data) {
            this.setLifeStage($data.stage, true, false);
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public boolean wantsToAttack(@Nullable LivingEntity target, @Nullable LivingEntity owner) {
        return switch (target) {
            case ArmorStand ignored -> false;
            case TamableAnimal other -> !other.isTame() || other.getOwner() != owner;
            case AbstractHorse horse -> !horse.isTamed();
            case Player other when owner instanceof Player $owner && !$owner.canHarmPlayer(other) -> false;
            case null, default -> true;
        };
    }

    @Override
    public final boolean shouldTryTeleportToOwner() {
        var owner = this.getOwner();
        return owner != null && this.distanceToSqr(owner) >= 400.0;
    }

    //----------Player Control----------

    @Override
    protected float getFlyingSpeed() {
        return this.adjustSpeed((float) this.getAttributeValue(Attributes.FLYING_SPEED));
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (this.isInWater()) {
            this.moveRelative(this.getSpeed(), motion);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.35F));   // water drag
            return;
        }
        if (this.isFlying()) {
            if (this.isDeadOrDying()) return;
            this.moveRelative(this.getFlyingSpeed(), motion);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
        } else {
            super.travel(motion);
        }
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);

        var rot = EntityUtil.getRiddenRotation(player);
        float lookYaw = rot.y;

        // Desired heading = where the player is actually trying to GO, not just where the
        // camera points. Combine strafe (xxa) + forward/back (zza) into a direction relative
        // to the look yaw, so pressing A/D/S banks and turns the dragon to face that way
        // instead of crab-walking sideways/backwards.
        float targetYaw = lookYaw;
        if (this.isFlying() && (player.xxa != 0.0F || player.zza != 0.0F)) {
            // Angle of the input vector relative to "forward". In Minecraft yaw increases
            // clockwise (viewed from above), and xxa is +1 for strafe-LEFT, so we negate xxa
            // to map left input -> left (negative) turn. forward=0, left=-90, right=+90, back=180.
            float inputAngle = (float) Math.toDegrees(Math.atan2(-player.xxa, player.zza));
            targetYaw = lookYaw + inputAngle;
        }

        float rotY = this.getYRot();
        rotY += Mth.wrapDegrees(targetYaw - rotY) * 0.20F;   // smooth turn toward the heading
        this.setRot(rotY, rot.x * 1.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = rotY;
    }

    public @Nullable DragonProjectileAbility getProjectile() {
        DragonProjectileAbility p = this.getVariant().projectile;   // variant override wins
        return p != null ? p : this.getVariant().getDragonType().getProjectile();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 motion) {
        if (this.onGround()) {
            float forward = player.zza;
            return new Vec3(
                    player.xxa * 0.5F,
                    0.0,
                    forward < 0.0F ? forward * 0.25F : forward
            );
        }
        // Flying: any movement key (W/A/S/D) means "go" — the dragon has already been turned
        // to face that direction in tickRidden, so we just push FORWARD. No sideways strafe.
        float upward = 0.0F;
        float forward = 0.0F;
        boolean moving = player.zza != 0.0F || player.xxa != 0.0F;
        if (moving) {
            // climb/dive component from where the player is looking (pitch)
            float facing = player.getXRot() * MathUtil.TO_RAD_FACTOR;
            float i = Mth.cos(facing);   // horizontal factor
            float j = -Mth.sin(facing);  // vertical factor (look up -> climb)
            // Only apply the pitch-dive when actively moving forward-ish; full magnitude.
            upward = j;
            forward = i;
        }
        return new Vec3(
                0.0,   // no lateral strafe — turning is handled by facing the movement direction
                player.jumping ? upward + 0.5F : this.isDescending() ? upward - 0.5F : upward,
                forward
        );
    }

    private void setChested(boolean chested) {
        if (!this.firstTick) {
            if (chested) {
                this.playSound(DMSounds.DRAGON_CHEST, 0.5F, 1.0F);
            } else if (this.hasChest) {
                this.inventory.dropContents(true, 1.25);
            }
        }
        this.hasChest = chested;
    }

    private void setSaddled(boolean saddled) {
        if (!this.firstTick && saddled) {
            this.playSound(SoundEvents.HORSE_SADDLE, 0.5F, 1.0F);
        }
        this.isSaddled = saddled;
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return this.adjustSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
    }

    @Override
    public boolean canJump() {
        return this.onGround() && this.isSaddled();
    }

    @Override
    public void handleStartJump(int power) {
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public TameableDragonEntity getScreenOpeningData(ServerPlayer player) {
        return this;
    }

    @Override
    public DragonInventoryHandler createMenu(int id, Inventory inventory, Player player) {
        return new DragonInventoryHandler(id, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.getId());
    }

    @Override
    public AttributeSupplier getDynamicAttributes() {
        return ServerConfig.INSTANCE.getDragonAttributes();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.walking");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.swimming");
    private static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.sit");
    private static final RawAnimation REST = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.rest");
    private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.hover");
    private static final RawAnimation HOVER_CARRY = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.hover_carry");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.flying");
    private static final RawAnimation DIVE = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.dive");
    private static final RawAnimation BREATH = RawAnimation.begin().thenLoop("animation.dragonmounts2.dragon.breath");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.dragonmounts2.dragon.bite");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.dragonmounts2.dragon.death");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1) LOCOMOTION state machine
        float volume = Mth.clamp(1.5F + this.getAgeScale(), 1.5F, 2.5F);
        controllers.add(
                new AnimationController<>(this, "movement", 5, state -> {
                    if (this.isDeadOrDying()) return PlayState.STOP;
                    if (this.isFlying() && !isInWater()) {
                        Vec3 v = this.getDeltaMovement();
                        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);

                        if (v.y < -0.35) return state.setAndContinue(DIVE);
                        if (horizontal > 0.08)
                            return state.setAndContinue(FLAP);

                        return getPassengers().size() > 1 ? state.setAndContinue(HOVER_CARRY) : state.setAndContinue(HOVER);
                    }

                    if (this.isUnderWater())
                        return state.setAndContinue(SWIM);
                    if (this.isSleeping())
                        return state.setAndContinue(REST);
                    if (this.isInSittingPose())
                        return state.setAndContinue(SIT);

                    return state.setAndContinue(state.isMoving() ? WALK : IDLE);
                })
                        .setSoundKeyframeHandler(event -> {
                            String sound = event.getKeyframeData().getSound();

                            SoundEvent soundEvent = switch (sound) {
                                case "wings_flap" -> DMSounds.DRAGON_FLAP;
                                default -> null;
                            };

                            if (soundEvent != null) {
                                this.level().playLocalSound(
                                        this.getX(),
                                        this.getY(),
                                        this.getZ(),
                                        soundEvent,
                                        this.getSoundSource(),
                                        volume,
                                        1.0F,
                                        false
                                );
                            }
                        })
        );

        // 2) FIRE BREATH — independent layer, plays on TOP of flap/walk/etc.
        controllers.add(new AnimationController<>(this, "breath", 3, state ->
                this.isBreathing() ? state.setAndContinue(BREATH) : PlayState.STOP
        ));

        // 3) BITE — triggered one-shot, layered over movement
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("bite", BITE));

        controllers.add(new AnimationController<>(this, "death", 0, state ->
                this.isDeadOrDying() ? state.setAndContinue(DEATH) : PlayState.STOP
        ));
    }

    public void updateRenderPitchAndRoll() {
        this.renderPitchO = this.renderPitch;
        this.renderRollO = this.renderRoll;

        float targetPitch = 0.0F;
        float targetRoll = 0.0F;
        // Bank whenever the dragon is flying — under a rider OR flying itself (e.g. a
        // bronco break-in ride, or following its owner). Pitch comes from vertical
        // velocity, roll from how fast the heading is turning.
        if (this.isFlying() && getPassengers().size() == 1) {
            Vec3 v = this.getDeltaMovement();
            double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);

            // only pitch when actually flying forward — not when hovering or going straight up
            if (horizontal > 0.08) {
                targetPitch = (float) Mth.clamp(-v.y * 45.0, -25.0, 25.0);
            } else {
                targetPitch = 0.0F;   // hovering / vertical ascent -> level out
            }

            // Roll from heading turn-rate. Use the driver's intended yaw when a player
            // is steering (snappier), otherwise the dragon's own yaw (autonomous flight).
            Player driver = this.getControllingPassenger();
            float yaw = (driver != null) ? driver.getYRot() : this.getYRot();
            float turn = Mth.wrapDegrees(yaw - this.lastYRotForRoll);
            targetRoll = Mth.clamp(turn * 8.0F, -25.0F, 25.0F);
            this.lastYRotForRoll = yaw;
        } else {
            this.lastYRotForRoll = this.getYRot();
        }

        this.renderPitch += (targetPitch - this.renderPitch) * 0.2F;
        this.renderRoll += (targetRoll - this.renderRoll) * 0.25F;
    }
}