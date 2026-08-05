package net.dragonmounts.neo.common.init;

import net.dragonmounts.neo.common.api.ArmorEffectSource;
import net.dragonmounts.neo.common.component.DragonFood;
import net.dragonmounts.neo.common.component.FluteSound;
import net.dragonmounts.neo.common.component.ScoreboardInfo;
import net.dragonmounts.neo.compat.registry.DragonType;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import static net.dragonmounts.neo.compat.registry.RegistryHandler.registerComponent;

public class DMDataComponents {
    public static final DataComponentType<ArmorEffectSource> ARMOR_EFFECT_SOURCE = registerComponent(
            "armor_effect_source",
            builder -> builder.persistent(ArmorEffectSource.CODEC)
    );
    public static final DataComponentType<DragonFood> DRAGON_FOOD = registerComponent(
            "dragon_food",
            builder -> builder.persistent(DragonFood.CODEC).networkSynchronized(DragonFood.STREAM_CODEC)
    );
    public static final DataComponentType<DragonType> DRAGON_TYPE = registerComponent(
            "dragon_type",
            builder -> builder.persistent(DragonType.CODEC).networkSynchronized(DragonType.STREAM_CODEC)
    );
    public static final DataComponentType<Component> PLAYER_NAME = registerComponent(
            "player_name",
            builder -> builder.cacheEncoding()
                    .persistent(ComponentSerialization.FLAT_CODEC)
                    .networkSynchronized(ComponentSerialization.STREAM_CODEC)
    );
    public static final DataComponentType<ScoreboardInfo> SCORES = registerComponent(
            "scores",
            builder -> builder.persistent(ScoreboardInfo.CODEC)
    );
    /**
     * The flute's cached copy of its bound dragon's home. The dragon owns the real value; this
     * exists so {@code FluteScreen} can label its button without the dragon being loaded, which
     * is exactly the case that matters when the dragon has wandered off.
     */
    public static final DataComponentType<GlobalPos> DRAGON_HOME = registerComponent(
            "dragon_home",
            builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC)
    );
    public static final DataComponentType<FluteSound> FLUTE_SOUND = registerComponent(
            "flute_sound",
            builder -> builder.cacheEncoding()
                    .persistent(FluteSound.CODEC)
                    .networkSynchronized(FluteSound.STREAM_CODEC)
    );

    public static void init() {}
}
