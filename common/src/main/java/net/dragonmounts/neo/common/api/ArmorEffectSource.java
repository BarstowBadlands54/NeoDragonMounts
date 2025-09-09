package net.dragonmounts.neo.common.api;

import com.mojang.serialization.Codec;
import net.dragonmounts.neo.common.capability.ArmorEffectManager;
import net.dragonmounts.neo.common.util.DefaultedDispatchCodec;
import net.dragonmounts.neo.compat.registry.ArmorEffectSourceType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ArmorEffectSource {
    Codec<ArmorEffectSource> CODEC = new DefaultedDispatchCodec<ArmorEffectSourceType<?>, ArmorEffectSource>(
            ArmorEffectSourceType.REGISTRY.byNameCodec(),
            "type",
            ArmorEffectSourceType.COMPONENT,
            ArmorEffectSource::getType,
            ArmorEffectSourceType::codec
    ).codec();

    void affect(ArmorEffectManager manager, Player player, ItemStack stack);

    ArmorEffectSourceType<?> getType();
}
