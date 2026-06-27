package net.dragonmounts.neo.common.client;

import net.dragonmounts.neo.common.client.breath.impl.ClientBreathHelper;
import net.dragonmounts.neo.common.component.DragonFood;
import net.dragonmounts.neo.common.entity.ai.control.DragonHeadLocator;
import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.dragonmounts.neo.common.entity.dragon.DragonModelContracts;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.dragonmounts.neo.common.init.DMKeyMappings;
import net.dragonmounts.neo.common.init.DMSounds;
import net.dragonmounts.neo.common.inventory.DragonInventory;
import net.dragonmounts.neo.common.network.c2s.FireProjectilePayload;
import net.dragonmounts.neo.common.tag.DMItemTags;
import net.dragonmounts.neo.common.util.ArrayUtil;
import net.dragonmounts.neo.common.util.Segment;
import net.dragonmounts.neo.common.util.math.MathUtil;
import net.dragonmounts.neo.compat.platform.ClientNetworkHandler;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientDragonEntity extends TameableDragonEntity {

    // --- Wing-flap sound (replaces the old DragonAnimator-driven flap) ---
    // The wing cycle advances FAST when hovering and SLOW when moving forward, exactly
    // like the original animator: anim += flying ? 0.070 - clamp(speed/0.05)*0.035 : 0.035.
    // A flap sound plays each time the wings cross from up to down.
    private float flapAnim = 0.0F;
    private float flapAnimPrev = 0.0F;
    private boolean flapWingsDown = false;
    public final DragonHeadLocator<ClientDragonEntity> headLocator = new DragonHeadLocator<>(this);
    public int controlFlags;
    private float pendingJumpPower;
    private boolean wasOnGround;
    private final Segment[] neckSegments =
            ArrayUtil.fillArray(new Segment[DragonModelContracts.NECK_SEGMENTS], Segment::new);

    public ClientDragonEntity(EntityType<? extends TameableDragonEntity> type, Level world) {
        super(type, world);
        this.stage = DragonLifeStage.ADULT;
    }

    @Override
    protected @NotNull ClientBreathHelper createBreathHelper() {
        return new ClientBreathHelper(this);
    }

    public final @NotNull Vec3 getHeadRelativeOffset(float x, float y, float z) {
        return headLocator.getHeadRelativeOffset(x,y,z);
    }

    /**
     * @see #onFlap()
     */
    @Deprecated
    public void onWingsDown(float speed) {
        // play wing sounds
        this.level().playLocalSound(
                this,
                SoundEvents.ENDER_DRAGON_FLAP,
                this.getSoundSource(),
                0.8f + (this.getAgeScale() - speed),
                1.0F
        );
    }

    /**
     * Drives the wing-flap sound without the old animator. The flap cycle speeds up when
     * hovering and slows when moving forward (matching the original NeoDragonMounts feel),
     * and a sound fires on each down-stroke.
     */
    private void tickWingFlapSound() {
        this.flapAnimPrev = this.flapAnim;
        if (!this.isFlying() || this.isInWater()) {
            this.flapWingsDown = false;
            return;
        }
        // Cycle speed: fast in hover (0.070), slowing toward 0.035 as horizontal speed rises.
        final float speedMax = 0.05F;
        var motion = this.getDeltaMovement();
        float speedEnt = (float) (motion.x * motion.x + motion.z * motion.z);
        this.flapAnim += 0.070F - MathUtil.clamp(speedEnt / speedMax) * 0.035F;

        // Down-stroke detection on the sine cycle (same test the animator used).
        float base = this.flapAnim * (MathUtil.PI * 2.0F);
        boolean wingsDown = net.minecraft.util.Mth.sin(base - 1.0F) > 0.0F;
        if (wingsDown && !this.flapWingsDown) {
            float horizontal = (float) Math.sqrt(speedEnt);
            this.level().playLocalSound(
                    this,
                    SoundEvents.ENDER_DRAGON_FLAP,
                    this.getSoundSource(),
                    0.8F + (this.getAgeScale() - horizontal),
                    1.0F
            );
        }
        this.flapWingsDown = wingsDown;
    }

    @Override
    public void aiStep() {
        this.wasOnGround = this.onGround();
        if (this.isDeadOrDying()) {
            this.nearestCrystal = null;
        } else {
            this.checkCrystals();
        }
        super.aiStep();
        this.headLocator.tick();
        this.headLocator.calculateHeadAndNeck(this.neckSegments, this.getXRot(), this.yHeadRot - this.yBodyRot);
        this.updateRenderPitchAndRoll();
        this.tickWingFlapSound();

        this.breathHelper.tick();
        if (!this.isAgeLocked()) {
            if (this.age < 0) {
                ++this.age;
            } else if (this.age > 0) {
                --this.age;
            }
        }
        var sneeze = this.getVariant().type.sneezeParticle;
        if (sneeze != null && !this.isBaby() && !this.isBreathing() && this.random.nextInt(700) == 0 && !isInWater()) {
            var level = this.level();
            var pos = this.getHeadRelativeOffset(0.0F, 4.0F, 22.0F);
            double x = pos.x, y = pos.y, z = pos.z;
            for (int i = -1; i < 1; ++i) {
                level.addParticle(sneeze, x, y + 0.5 * i, z, 0, 0.3, 0);
            }
            level.playSound(null, x, y, z, DMSounds.DRAGON_SNEEZE, SoundSource.NEUTRAL, 0.8F, 1);
        }
    }

    // client-side field somewhere persistent (e.g. a ClientProjectileHelper or the client mod class)
    private int projectileChargeTicks = 0;

    public void clientTickProjectile(net.minecraft.client.Minecraft mc) {
        var player = mc.player;
        if (player == null) return;
        // only while riding a dragon you control
        if (!(player.getVehicle() instanceof TameableDragonEntity dragon) || dragon.getControllingPassenger() != player) {
            projectileChargeTicks = 0;
            return;
        }
        var ability = dragon.getProjectile();
        if (ability == null) { projectileChargeTicks = 0; return; }

        if (DMKeyMappings.PROJECTILE.isDown()) {
            projectileChargeTicks++;
        } else if (projectileChargeTicks > 0) {
            int held = projectileChargeTicks;
            projectileChargeTicks = 0;
            System.out.println("[DM] G released, held=" + held + " min=" + ability.minChargeTicks);
            if (held >= ability.minChargeTicks) {
                float power = Math.min(1.0F, (held - ability.minChargeTicks) / 20.0F);
                System.out.println("[DM] sending FireProjectilePayload power=" + power);
                ClientNetworkHandler.send(new FireProjectilePayload(dragon.getId(), power));
            }
        }
    }

    @Override
    protected void checkCrystals() {
        if (this.nearestCrystal != null && this.nearestCrystal.isAlive()) {
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
        if (this.lastType != null) {
            this.getAttributes().removeAttributeModifiers(this.lastType.attributes);
        }
        this.getAttributes().addTransientAttributeModifiers(type.attributes);
        this.breathHelper.onTypeChange(type);
        this.lastType = type;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        boolean notOwner = !this.isOwnedBy(player);
        if (!this.isBreathing()) {
            var food = DragonFood.getInstance(stack);
            if (food != null) {
                return (food.requiresOwner() && notOwner) || (
                        !food.canAlwaysFeed() && this.getHealth() >= this.getMaxHealth() && this.isTame()
                ) ? InteractionResult.FAIL : InteractionResult.CONSUME;
            }
        }
        if (notOwner) return InteractionResult.PASS;
        if (DragonInventory.isDragonArmor(stack)
                || DragonInventory.isDragonSaddle(stack)
                || DragonInventory.isChest(stack)
                || stack.is(DMItemTags.BATONS)
        ) return InteractionResult.CONSUME;
        var result = stack.interactLivingEntity(player, this, hand);
        return result.consumesAction() ? result : InteractionResult.CONSUME;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case ON_ATTACK -> {
                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.7F);
                this.triggerAnim("attack", "bite");
            }
            case ON_ROAR -> {
                SoundEvent sound = this.getVariant().type.getRoarSound(this);
                if (sound == null) break;
                this.playSound(sound, Mth.clamp(this.getAgeScale(), 0.3F, 0.6F), 1.0F);
                this.triggerAnim("attack", "bite");
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void setLifeStage(DragonLifeStage stage, boolean reset, boolean sync) {
        if (this.stage == stage) return;
        this.stage = stage;
        if (reset) {
            this.refreshAge();
        }
        this.reapplyPosition();
        this.refreshDimensions();
    }

    @Override
    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        int data = packet.getData();
        this.setLifeStage(DragonLifeStage.byId(data & 0b111), false, false);
        this.setAge(data >>> 3);
    }

    public void refreshForcedAgeTimer() {
        if (this.forcedAgeTimer <= 0) {
            this.forcedAgeTimer = 40;
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.level().isClientSide && this.deathTime % 4 == 0) {
            var pos = this.position();
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        pos.x + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        pos.y + this.random.nextDouble() * this.getBbHeight(),
                        pos.z + (this.random.nextDouble() - 0.5) * this.getBbWidth(),
                        0, 0.05, 0);
            }
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {}

    public @Nullable Vec3 locateCrystal() {
        return this.nearestCrystal == null ? null : this.nearestCrystal.position();
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        if (this.onGround() && this.isControlledByLocalInstance()) {
            // handle jump
            float power = this.pendingJumpPower;
            this.pendingJumpPower = 0.0F;
            if (power > 0.0F) {
                var motion = this.getDeltaMovement();
                this.hasImpulse = true;
                if (input.z > 0.0) {
                    float facing = this.getYRot() * MathUtil.TO_RAD_FACTOR;
                    this.setDeltaMovement(
                            motion.x - 0.4F * Mth.sin(facing) * power,
                            this.getJumpPower(power) * 2.5,
                            motion.z + 0.4F * Mth.cos(facing) * power
                    );
                } else {
                    this.setDeltaMovement(motion.x, this.getJumpPower(power) * 2.5, motion.z);
                }
            }
        }
    }

    @Override
    public boolean canJump() {
        return this.wasOnGround && super.canJump();
    }

    @Override
    public void onPlayerJump(int power) {
        this.pendingJumpPower = power >= 90 ? 1.0F : 0.4F + 0.4F * power / 90.0F;
    }
}