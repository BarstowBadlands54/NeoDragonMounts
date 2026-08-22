package net.dragonmounts.neo.common.entity.dragon;

import com.mojang.logging.LogUtils;
import net.dragonmounts.neo.common.api.AutoJumpRideable;
import net.dragonmounts.neo.common.api.ConditionalShearable;
import net.dragonmounts.neo.common.api.DragonTypified;
import net.dragonmounts.neo.common.api.DynamicAttributeEntity;
import net.dragonmounts.neo.common.client.ClientDragonEntity;
import net.dragonmounts.neo.common.component.DragonFood;
import net.dragonmounts.neo.common.entity.ai.control.DragonBodyControl;
import net.dragonmounts.neo.common.entity.ai.control.DragonLookControl;
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
import net.minecraft.core.GlobalPos;
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
    public static final String HOME_PARAMETER_KEY = "Home";
    /**
     * Where this dragon calls home, or null if it was never given one. Server-authoritative and
     * saved with the entity; the flute keeps its own cached copy purely so the button can be
     * labelled correctly while the dragon is too far away to be tracked by the client.
     */
    protected @Nullable GlobalPos home;
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
        this.lookControl = new DragonLookControl(this);
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
        builder.define(DATA_DRAGON_VARIANT, DragonVariants.ENDER_JEAN);
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

    @Override
    public void tick() {
        // DATA_DRAGON_VARIANT defaults to ENDER_JEAN, so a dragon that genuinely *is* ender_jean
        // never fires a change event for it -- and applyType(), the only thing that installs the
        // breath weapon and the type's attribute modifiers, hangs off that event.
        //
        // Server side: setVariant(ENDER_JEAN) equals the current value, so SynchedEntityData
        // discards it as a no-op and onSyncedDataUpdated is never called. setDragonType() skips
        // the call entirely anyway, since ENDER_JEAN's type already matches.
        // Client side: getNonDefaultValues() omits anything still at its default, so the variant
        // is left out of the spawn packet and the hook has nothing to fire on.
        //
        // Result on both sides: breath stays null, canBreathe() is false, and setBreathing() can
        // never latch. applyType() is idempotent -- it returns immediately once lastType matches
        // -- so initialising here costs one reference comparison per tick thereafter.
        if (this.lastType == null) {
            this.applyType(this.getDragonType());
        }
        super.tick();
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
        if (!this.isTame() || this.isSleeping()) return null;   // add the sleeping check
        // Steering also stops the moment the saddle comes off, so a rider cannot keep control of
        // a dragon that no longer meets the requirement.
        if (ServerConfig.INSTANCE.requireSaddleToRide.get() && !this.isSaddled()) return null;
        return !this.isNoAi() && isBreakInTrusted() && this.getFirstPassenger() instanceof Player player ? player : null;
    }

    /**
     * The player currently clinging to an untamed dragon during a break-in (may not be the controller).
     */
    @Nullable
    public Player getBreakInRider() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
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

        // locatePassenger returns an ADULT-sized offset. It was only being scaled by
        // MOJANG_MODEL_SCALE, so a juvenile -- whose model is drawn at getAdjustedSize() -- got a
        // seat placed for a full-grown dragon and the rider floated above its back. Scaling by the
        // same factor the model uses keeps the seat on the saddle at every life stage.
        Vec3 base = this.getDragonType().locatePassenger(seat, this.isInSittingPose())
                .scale(MathUtil.MOJANG_MODEL_SCALE * this.getAdjustedSize());

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
        // Vanilla AgeableMob uses a signed age: negative counts UP toward 0 (still a baby),
        // positive counts DOWN toward 0 (an adult on breeding cooldown). So the two pre-adult
        // stages start negative and JUVENILE starts positive.
        switch (this.stage.ordinal()) {
            case 0: // HATCHLING
            case 1: // FLEDGLING
                this.age = -this.stage.duration;
                return;
            case 2: // JUVENILE
                this.age = this.stage.duration;
                return;
            default: // ADULT
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
        // The owner is never a target. The Player case below does not cover this: canHarmPlayer()
        // returns true for a teamless player against themselves, so a stray hit from the owner
        // would otherwise fall through to `default -> true`.
        if (target == this || target == owner || target == this.getOwner()) return false;
        return switch (target) {
            case ArmorStand ignored -> false;
            // Any tamed animal is off-limits, not only this owner's. Breath is an area effect, so
            // retaliation between pets is self-amplifying: one stray blast across a pen sets the
            // whole pen fighting and every counter-attack sprays again. Untamed stays fair game,
            // so a wild or hostile dragon that starts the fight is still fought back.
            case TamableAnimal other -> !other.isTame();
            case AbstractHorse horse -> !horse.isTamed();
            case Player other when owner instanceof Player $owner && !$owner.canHarmPlayer(other) -> false;
            case null, default -> true;
        };
    }

    /**
     * Body pitch the breath may reach, in degrees. 90 is straight down, which is the most
     * atan2 can produce, so this removes the ceiling entirely rather than merely widening it.
     * The head does not follow this far -- DragonHeadLocator clamps what is drawn.
     */
    public static final int BREATH_MAX_PITCH = 90;

    @Override
    public int getMaxHeadXRot() {
        // A ceiling, not a turn rate. LookControl zeroes xRot every tick and then steps back
        // toward the look target by at most this many degrees, so at the vanilla 40 the dragon
        // physically cannot pitch past 40 degrees -- faceTarget's steeper aim was overwritten on
        // the same tick it was applied, which is why an airborne dragon fired over the top of
        // anything below it. Widened only while breathing so ordinary looking-about is unchanged.
        return this.isBreathing() ? BREATH_MAX_PITCH : super.getMaxHeadXRot();
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

    /**
     * Heading the rider is asking for, in degrees, derived from the camera yaw plus the
     * WASD input vector. Forward = camera yaw, left = -90, right = +90, back = 180.
     * <p>
     * In Minecraft yaw increases clockwise (viewed from above) and {@code xxa} is +1 for
     * strafe-LEFT, so the strafe component is negated to map left input to a left turn.
     *
     * @return the desired world heading, or the camera yaw when no key is held.
     */
    public static float getInputHeading(Player player) {
        if (player.xxa == 0.0F && player.zza == 0.0F) return player.getYRot();
        return player.getYRot() + (float) Math.toDegrees(Math.atan2(-player.xxa, player.zza));
    }

    /**
     * True while the dragon's body is pinned to the rider's crosshair instead of to its
     * direction of travel. Movement keys then strafe rather than steer, which is what lets
     * a rider circle a target while keeping the breath on it.
     */
    public boolean isAimLocked() {
        return this.isBreathing() && this.getControllingPassenger() != null;
    }

    /**
     * The direction the breath weapon (and any aimed projectile) should travel.
     * <p>
     * This deliberately does <em>not</em> go through {@link #getLookAngle()}: the dragon's own
     * rotation is smoothed and damped so the body turns believably, which would drop the beam
     * behind a fast-moving crosshair. Taking the rider's view vector directly means the stream
     * lands exactly where the player is pointing, and the body catches up over the next few
     * ticks. Falls back to the dragon's own look when nobody is steering (wild dragons,
     * bronco rides, AI-driven breath attacks).
     */
    public Vec3 getAimVector() {
        Player driver = this.getControllingPassenger();
        return driver == null ? this.getLookAngle() : driver.getViewVector(1.0F);
    }

    /** How fast the body swings onto the rider's crosshair while breathing. */
    private static final float AIM_TURN_RATE = 0.45F;
    /** How fast the body swings onto the travel heading while flying. */
    private static final float AIR_TURN_RATE = 0.20F;
    /** How fast the body swings onto the travel heading on the ground. */
    private static final float GROUND_TURN_RATE = 0.15F;

    /**
     * The body yaw {@link #tickRidden} is about to settle on this tick while aim-locked.
     * <p>
     * {@code travelRidden} calls {@link #getRiddenInput} <em>before</em> {@code tickRidden}, but
     * {@code travel} then rotates that vector by the already-updated yaw. Both sides go through
     * here so the strafe vector is resolved against the same heading the movement will use,
     * instead of lagging a tick behind whenever the rider swings the camera hard.
     */
    private float nextAimYaw(Player player) {
        float rotY = this.getYRot();
        return rotY + Mth.wrapDegrees(player.getYRot() - rotY) * AIM_TURN_RATE;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);

        // ---- AIM LOCK ----------------------------------------------------------------
        // While breathing, the body tracks the crosshair rather than the direction of
        // travel, so the dragon visibly points along its own beam. Pitch is taken at full
        // strength (not the damped ride pitch) so the head lines up with the stream.
        if (this.isAimLocked()) {
            float rotY = this.nextAimYaw(player);
            float rotX = this.getXRot();
            rotX += Mth.wrapDegrees(player.getXRot() - rotX) * AIM_TURN_RATE;
            this.setRot(rotY, rotX);
            this.yRotO = this.yBodyRot = this.yHeadRot = rotY;
            return;
        }

        // ---- STEERING ----------------------------------------------------------------
        // Desired heading = where the player is actually trying to GO, not just where the
        // camera points, so pressing A/D/S turns the dragon to face that way instead of
        // crab-walking sideways/backwards. Applies on the ground as well as in the air.
        var rot = EntityUtil.getRiddenRotation(player);
        float targetYaw = getInputHeading(player);
        float rotY = this.getYRot();
        rotY += Mth.wrapDegrees(targetYaw - rotY) * (this.isFlying() ? AIR_TURN_RATE : GROUND_TURN_RATE);
        this.setRot(rotY, rot.x * 1.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = rotY;
    }

    //----------Home----------

    public @Nullable GlobalPos getHomePos() {
        return this.home;
    }

    public void setHomePos(@Nullable GlobalPos home) {
        this.home = home;
    }

    public boolean hasHomePos() {
        return this.home != null;
    }

    /**
     * True when this dragon has a home it could actually be sent to right now — i.e. one that
     * exists and sits in the level the dragon is currently standing in. Cross-dimension recall
     * is deliberately not supported: dragging an entity between levels needs a full
     * {@code teleportTo(ServerLevel, ...)} and would let a flute pull a dragon out of the
     * Nether from the Overworld.
     */
    public boolean canReturnHome() {
        return this.home != null && this.level().dimension().equals(this.home.dimension());
    }

    public @Nullable DragonProjectileAbility getProjectile() {
        DragonProjectileAbility p = this.getVariant().projectile;   // variant override wins
        return p != null ? p : this.getVariant().getDragonType().getProjectile();
    }

    /** Speed multiplier applied while strafing under aim lock, relative to a normal run. */
    private static final float AIM_STRAFE_SCALE = 0.7F;

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 motion) {
        float strafe = player.xxa;
        float forward = player.zza;
        boolean moving = strafe != 0.0F || forward != 0.0F;
        boolean grounded = this.onGround();

        // ---- AIM LOCK ----------------------------------------------------------------
        // The body is pinned to the crosshair, so WASD can no longer be expressed as pure
        // forward thrust. Convert the requested world heading into the dragon's local frame
        // and hand back a real strafe vector: the dragon slides sideways while the mouth
        // stays on target.
        if (this.isAimLocked()) {
            double localX = 0.0;
            double localZ = 0.0;
            if (moving) {
                // moveRelative() rotates this vector by the dragon's yaw, so the local
                // direction for world heading H is (-sin d, cos d) with d = H - bodyYaw.
                float delta = Mth.wrapDegrees(getInputHeading(player) - this.nextAimYaw(player)) * MathUtil.TO_RAD_FACTOR;
                localX = -Mth.sin(delta) * AIM_STRAFE_SCALE;
                localZ = Mth.cos(delta) * AIM_STRAFE_SCALE;
            }
            // Climb/dive is deliberately cut loose from the look pitch here: aiming the
            // breath at the floor should not fly the dragon into it. Jump/descend still work.
            double upward = grounded ? 0.0 : player.jumping ? 0.5 : this.isDescending() ? -0.5 : 0.0;
            return new Vec3(localX, upward, localZ);
        }

        // ---- STEERING ----------------------------------------------------------------
        // Any movement key means "go": tickRidden has already turned the dragon onto that
        // heading, so all that is left is forward thrust. Holding only A or D still gives
        // full speed, which is why the magnitude is the larger of the two axes rather than
        // the forward axis alone.
        float thrust = moving ? Math.max(Math.abs(strafe), Math.abs(forward)) : 0.0F;
        if (grounded) {
            return new Vec3(0.0, 0.0, thrust);
        }
        float upward = 0.0F;
        if (moving) {
            // climb/dive component from where the player is looking (pitch)
            float facing = player.getXRot() * MathUtil.TO_RAD_FACTOR;
            upward = -Mth.sin(facing);          // look up -> climb
            thrust *= Mth.cos(facing);          // horizontal factor
        }
        return new Vec3(
                0.0,   // no lateral strafe — turning is handled by facing the movement direction
                player.jumping ? upward + 0.5F : this.isDescending() ? upward - 0.5F : upward,
                thrust
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
        return this.onGround();
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

    /** Minimum gap between wing-flap sounds, in ticks. */
    private static final int FLAP_SOUND_COOLDOWN = 10;
    private int lastFlapSoundTick = Integer.MIN_VALUE;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 1) LOCOMOTION state machine
        controllers.add(
                new AnimationController<>(this, "movement", 5, state -> {
                    if (this.isDeadOrDying()) {
                        // Hold the pose it died in. PlayState.STOP unbinds the bones and GeckoLib
                        // eases them back to the model's default over the controller's 5 transition
                        // ticks -- that easing is the neck and tail appearing to shrink and the head
                        // swinging round. Speed 0 keeps the current frame pinned while the dissolve
                        // decal in DragonRenderer fades the body out.
                        state.getController().setAnimationSpeed(0.0D);
                        return PlayState.CONTINUE;
                    }
                    state.getController().setAnimationSpeed(1.0D);
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
                            if (!"wings_flap".equals(event.getKeyframeData().getSound())) return;
                            // The predicate above runs once per render frame and flips between
                            // FLAP/HOVER/DIVE whenever velocity crosses a threshold. Each flip
                            // clears GeckoLib's executed-keyframe set and re-arms this handler, so
                            // a ridden dragon jittering around 0.08 horizontal fires it every
                            // frame -- which drains the 247-slot static sound channel pool in
                            // seconds and stops every other sound from being able to start.
                            if (this.tickCount - this.lastFlapSoundTick < FLAP_SOUND_COOLDOWN) return;
                            this.lastFlapSoundTick = this.tickCount;
                            // Read the scale now, not at registerControllers time: on the client
                            // the entity is built before its age data arrives, so a captured value
                            // was frozen at the spawn-time scale.
                            float flapVolume = Mth.clamp(1.5F + this.getAgeScale(), 1.5F, 2.5F);
                            this.level().playLocalSound(
                                    this.getX(),
                                    this.getY(),
                                    this.getZ(),
                                    DMSounds.DRAGON_FLAP,
                                    this.getSoundSource(),
                                    flapVolume,
                                    1.0F,
                                    false
                            );
                        })
        );

        // 2) FIRE BREATH — independent layer, plays on TOP of flap/walk/etc.
        controllers.add(new AnimationController<>(this, "breath", 3, state ->
                this.isBreathing() ? state.setAndContinue(BREATH) : PlayState.STOP
        ));

        // 3) BITE — triggered one-shot, layered over movement
        controllers.add(new AnimationController<>(this, "attack", 0, state -> PlayState.STOP)
                .triggerableAnim("bite", BITE));

        // No death controller: animation.dragonmounts2.dragon.death is an empty stub
        // ({"loop": true, "animation_length": 1.0833} with no bones), so playing it animated
        // nothing while stopping the movement controller reset the pose. Freezing movement above
        // reproduces the original behaviour -- hold the last pose, then dissolve.
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