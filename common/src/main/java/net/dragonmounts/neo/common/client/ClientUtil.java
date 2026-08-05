package net.dragonmounts.neo.common.client;

import net.dragonmounts.neo.common.client.gui.FluteScreen;
import net.dragonmounts.neo.common.entity.dragon.DragonModelContracts;
import net.dragonmounts.neo.common.util.Segment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.UUID;

public class ClientUtil {
    private static final int CAPACITY = DragonModelContracts.TAIL_SEGMENTS;
    private static final String[] STRING_OF_INT;

    static {
        var cache = new String[CAPACITY];
        for (int i = 0; i < cache.length; ++i) {
            cache[i] = Integer.toString(i);
        }
        STRING_OF_INT = cache;
    }

    public static Level getLevel() {
        return Minecraft.getInstance().level;
    }

    public static void openFluteScreen(UUID uuid, @Nullable GlobalPos home) {
        Minecraft.getInstance().setScreen(new FluteScreen(uuid, home));
    }

    /**
     * 1.21.1 {@link net.minecraft.client.model.geom.PartPose} cannot carry scale (the 9-arg
     * constructor is 1.21.4). Scale lives on the baked {@link ModelPart} instead, so apply it
     * after {@code bakeLayer(...)} rather than baking it into the LayerDefinition pose.
     */
    public static ModelPart applyScale(ModelPart part, float scaleX, float scaleY, float scaleZ) {
        part.xScale = scaleX;
        part.yScale = scaleY;
        part.zScale = scaleZ;
        return part;
    }

    public static ModelPart applyScale(ModelPart part, float scale) {
        return applyScale(part, scale, scale, scale);
    }

    public static String toString(@Range(from = 0, to = CAPACITY - 1) int i) {
        return STRING_OF_INT[i];
    }

    public static ModelPart[] getChildren(ModelPart parent, @Range(from = 0, to = CAPACITY) int length) {
        var children = new ModelPart[length];
        for (int i = 0; i < length; ++i) {
            children[i] = parent.getChild(STRING_OF_INT[i]);
        }
        return children;
    }

    public static float takeIfValid(float neo, float old) {
        return Float.isNaN(neo) ? old : neo;
    }

    public static void loadBasic(ModelPart part, Segment segment) {
        part.x = takeIfValid(segment.posX, part.x);
        part.y = takeIfValid(segment.posY, part.y);
        part.z = takeIfValid(segment.posZ, part.z);
        part.xRot = takeIfValid(segment.rotX, part.xRot);
        part.yRot = takeIfValid(segment.rotY, part.yRot);
        part.zRot = takeIfValid(segment.rotZ, part.zRot);
    }

    public static void loadScale(ModelPart part, Segment.Scalable segment) {
        part.xScale = takeIfValid(segment.scaleX, part.xScale);
        part.yScale = takeIfValid(segment.scaleY, part.yScale);
        part.zScale = takeIfValid(segment.scaleZ, part.zScale);
    }

    public static boolean isRemoteServer() {
        var minecraft = Minecraft.getInstance();
        return minecraft.getCurrentServer() != null && !minecraft.isSingleplayer();
    }
}