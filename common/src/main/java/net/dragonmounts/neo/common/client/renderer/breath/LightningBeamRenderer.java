package net.dragonmounts.neo.common.client.renderer.breath;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.dragonmounts.neo.common.entity.breath.BreathNode;
import net.dragonmounts.neo.common.entity.breath.BreathState;
import net.dragonmounts.neo.common.entity.breath.LightningBreath;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a dragon's breath as lightning through {@link RenderType#lightning()}: the same render
 * type, shader and additive blend vanilla uses for a bolt out of the sky. Not a particle -- the
 * arc is a chain of untextured square tubes carrying position and colour only, and its brightness
 * comes from stacking passes in the additive buffer.
 * <p>
 * Three things are lifted from the vanilla bolt because they are what makes it read as lightning:
 * <ul>
 *   <li>the path is a random walk perpendicular to the beam, not a smooth curve;</li>
 *   <li>every pass redraws the <em>same</em> path wider, so a thin bright core sits in a halo;</li>
 *   <li>the shape is re-rolled once per tick and held. Per-frame re-rolls look like static.</li>
 * </ul>
 * Geometry comes from a seeded {@link RandomSource}, so there is nothing to tick, store or sync
 * and every client sees the same arc.
 */
public final class LightningBeamRenderer {
    /// Kinks along the beam. Vanilla uses 8 over a 128-block bolt.
    private static final int SEGMENTS = 8;
    /// Additive redraws of the same path. Vanilla uses 4; 3 is enough at this scale.
    private static final int PASSES = 3;
    /// Short branches thrown off the main arc.
    private static final int FORKS = 2;
    /// Segments a fork runs for before it dies out.
    private static final int FORK_LENGTH = 3;
    /// Half-width of the innermost pass, in blocks, before the size scale is applied.
    private static final float CORE_WIDTH = 0.05F;
    /// Each pass beyond the first widens by this multiple of the core.
    private static final float PASS_WIDTH_STEP = 1.8F;
    /// How far the arc may wander sideways, as a fraction of its length.
    private static final float SPREAD = 0.06F;
    /// Vanilla's bolt alpha. Low, because the passes stack.
    private static final float ALPHA = 0.3F;
    /// Colours are dimmed before the passes stack them back up towards white.
    private static final float COLOR_SCALE = 0.55F;
    /// Ticks are 50ms apart; this is how much the whole arc's brightness swings between them.
    private static final float FLICKER_RANGE = 0.4F;
    /// Beam length is derived from node speed, then held inside these bounds.
    private static final double MIN_RANGE = 6.0;
    private static final double MAX_RANGE = 28.0;

    private LightningBeamRenderer() {}

    /**
     * Draw the arc, if this dragon is currently breathing lightning. Call from
     * {@code DragonRenderer.render} <em>before</em> {@code super.render}: the arc is
     * world-aligned, so the pose must still be at the entity's interpolated position and
     * un-rotated by the model.
     */
    public static void render(
            TameableDragonEntity dragon,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float partialTick
    ) {
        if (!dragon.isBreathing()) return;
        if (dragon.breathHelper.getCurrentBreathState() != BreathState.SUSTAIN) return;
        if (!(dragon.breathHelper.getBreath() instanceof LightningBreath breath)) return;

        var muzzle = dragon.getMuzzlePosition();
        var aim = dragon.getAimVector();
        if (aim.lengthSqr() < 1.0E-6) return;
        aim = aim.normalize();

        // stop the arc on whatever it is pointing at, so it lands on a surface
        double range = Mth.clamp(
                BreathNode.getStartingSpeed(dragon.getLifeStage().power) * 6.0,
                MIN_RANGE,
                MAX_RANGE
        );
        var clip = dragon.level().clip(new ClipContext(
                muzzle,
                muzzle.add(aim.scale(range)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                dragon
        ));
        double length = clip.getType() == HitResult.Type.MISS
                ? range
                : Math.max(clip.getLocation().distanceTo(muzzle), 1.0);

        // the pose is already at the entity's render position, so work relative to it
        double originX = muzzle.x - Mth.lerp(partialTick, dragon.xOld, dragon.getX());
        double originY = muzzle.y - Mth.lerp(partialTick, dragon.yOld, dragon.getY());
        double originZ = muzzle.z - Mth.lerp(partialTick, dragon.zOld, dragon.getZ());

        // A frame perpendicular to the beam. The reference is swapped near vertical so the cross
        // product cannot degenerate when the dragon looks straight up or down.
        var reference = Math.abs(aim.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        var right = aim.cross(reference).normalize();
        var up = right.cross(aim).normalize();

        var profile = breath.getLightningProfile();
        float red = profile.red() * COLOR_SCALE;
        float green = profile.green() * COLOR_SCALE;
        float blue = profile.blue() * COLOR_SCALE;

        int tick = dragon.tickCount;
        // interpolating against the previous tick keeps the flicker smooth at high frame rates
        // while the shape itself still snaps once per tick
        float flicker = Mth.lerp(partialTick, brightness(dragon, tick - 1), brightness(dragon, tick));
        float scale = Mth.clamp(dragon.getAdjustedSize(), 0.4F, 1.6F);

        var matrix = poseStack.last().pose();
        var buffer = bufferSource.getBuffer(RenderType.lightning());
        int bolts = profile.bolts();
        for (int bolt = 0; bolt < bolts; ++bolt) {
            arc(
                    matrix, buffer,
                    RandomSource.create(seed(dragon, tick) + bolt * 0x9E3779B9L),
                    originX, originY, originZ,
                    aim, right, up,
                    length, scale,
                    red, green, blue, ALPHA * flicker
            );
        }
    }

    /// Per-tick, per-dragon seed. Changing every tick is what makes the arc crackle.
    private static long seed(TameableDragonEntity dragon, int tick) {
        return dragon.getId() * 0x5DEECE66DL + tick;
    }

    private static float brightness(TameableDragonEntity dragon, int tick) {
        return 1.0F - FLICKER_RANGE + RandomSource.create(seed(dragon, tick)).nextFloat() * FLICKER_RANGE;
    }

    /**
     * One arc: a random walk from the muzzle to the impact point, redrawn {@link #PASSES} times
     * at increasing width, plus a couple of short branches.
     */
    private static void arc(
            Matrix4f matrix,
            VertexConsumer buffer,
            RandomSource random,
            double originX,
            double originY,
            double originZ,
            Vec3 axis,
            Vec3 right,
            Vec3 up,
            double length,
            float scale,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float spread = (float) length * SPREAD;
        var offsetR = new float[SEGMENTS + 1];
        var offsetU = new float[SEGMENTS + 1];
        float walkR = 0.0F;
        float walkU = 0.0F;
        for (int i = 0; i <= SEGMENTS; ++i) {
            // the envelope pins both ends, so the wandering happens in between
            float envelope = Mth.sin(i / (float) SEGMENTS * Mth.PI);
            offsetR[i] = walkR * envelope;
            offsetU[i] = walkU * envelope;
            walkR += (random.nextFloat() - 0.5F) * spread;
            walkU += (random.nextFloat() - 0.5F) * spread;
        }

        double step = length / SEGMENTS;
        for (int pass = 0; pass < PASSES; ++pass) {
            float width = CORE_WIDTH * scale * (1.0F + pass * PASS_WIDTH_STEP);
            for (int i = 0; i < SEGMENTS; ++i) {
                segment(
                        matrix, buffer,
                        originX, originY, originZ, axis, right, up,
                        step * i, offsetR[i], offsetU[i],
                        step * (i + 1), offsetR[i + 1], offsetU[i + 1],
                        width, red, green, blue, alpha
                );
            }
        }

        // Branches, thin pass only, so they read as sparks rather than a second beam.
        for (int fork = 0; fork < FORKS; ++fork) {
            int start = 1 + random.nextInt(SEGMENTS - FORK_LENGTH);
            float branchR = offsetR[start];
            float branchU = offsetU[start];
            for (int i = 0; i < FORK_LENGTH; ++i) {
                // three times the main jitter, and it never rejoins: a fork that came back would
                // just look like a thicker beam
                float nextR = branchR + (random.nextFloat() - 0.5F) * spread * 3.0F;
                float nextU = branchU + (random.nextFloat() - 0.5F) * spread * 3.0F;
                segment(
                        matrix, buffer,
                        originX, originY, originZ, axis, right, up,
                        step * (start + i), branchR, branchU,
                        step * (start + i + 1), nextR, nextU,
                        CORE_WIDTH * scale, red, green, blue,
                        alpha * (1.0F - i / (float) FORK_LENGTH)
                );
                branchR = nextR;
                branchU = nextU;
            }
        }
    }

    /// One square tube between two points on the arc: four quads, the same box vanilla draws.
    private static void segment(
            Matrix4f matrix,
            VertexConsumer buffer,
            double originX,
            double originY,
            double originZ,
            Vec3 axis,
            Vec3 right,
            Vec3 up,
            double fromDistance,
            float fromR,
            float fromU,
            double toDistance,
            float toR,
            float toU,
            float width,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float x0 = (float) (originX + axis.x * fromDistance + right.x * fromR + up.x * fromU);
        float y0 = (float) (originY + axis.y * fromDistance + right.y * fromR + up.y * fromU);
        float z0 = (float) (originZ + axis.z * fromDistance + right.z * fromR + up.z * fromU);
        float x1 = (float) (originX + axis.x * toDistance + right.x * toR + up.x * toU);
        float y1 = (float) (originY + axis.y * toDistance + right.y * toR + up.y * toU);
        float z1 = (float) (originZ + axis.z * toDistance + right.z * toR + up.z * toU);

        float rx = (float) right.x * width, ry = (float) right.y * width, rz = (float) right.z * width;
        float ux = (float) up.x * width, uy = (float) up.y * width, uz = (float) up.z * width;

        // Same corner order at both ends, so quads wind consistently and the tube culls like a
        // solid box instead of flickering inside out.
        for (int side = 0; side < 4; ++side) {
            float a1 = CORNER_R[side], b1 = CORNER_U[side];
            float a2 = CORNER_R[(side + 1) & 3], b2 = CORNER_U[(side + 1) & 3];
            vertex(matrix, buffer, x0 + rx * a1 + ux * b1, y0 + ry * a1 + uy * b1, z0 + rz * a1 + uz * b1, red, green, blue, alpha);
            vertex(matrix, buffer, x1 + rx * a1 + ux * b1, y1 + ry * a1 + uy * b1, z1 + rz * a1 + uz * b1, red, green, blue, alpha);
            vertex(matrix, buffer, x1 + rx * a2 + ux * b2, y1 + ry * a2 + uy * b2, z1 + rz * a2 + uz * b2, red, green, blue, alpha);
            vertex(matrix, buffer, x0 + rx * a2 + ux * b2, y0 + ry * a2 + uy * b2, z0 + rz * a2 + uz * b2, red, green, blue, alpha);
        }
    }

    private static final float[] CORNER_R = {1.0F, -1.0F, -1.0F, 1.0F};
    private static final float[] CORNER_U = {1.0F, 1.0F, -1.0F, -1.0F};

    /// POSITION_COLOR only: no uv, no light. The lightning shader is fullbright by nature.
    private static void vertex(
            Matrix4f matrix,
            VertexConsumer buffer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }
}
