package net.dragonmounts.neo.common.component.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dragonmounts.neo.common.entity.dragon.TameableDragonEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// 1.21.1 PORT: vanilla net.minecraft.world.item.consume_effects.ConsumeEffect does not exist
// in 1.21.1. Converted from a vanilla ConsumeEffect into a plain mod-owned record. apply() is
// now an ordinary method invoked directly by the dragon-feeding logic (it always was the mod's
// own behaviour, not vanilla's consume pipeline). CODEC/STREAM_CODEC are unchanged.
public record ContorlGrowthConsumeEffect(boolean isAllowed) {
    public static final MapCodec<ContorlGrowthConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("allowed").forGetter(ContorlGrowthConsumeEffect::isAllowed)
    ).apply(instance, ContorlGrowthConsumeEffect::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContorlGrowthConsumeEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ContorlGrowthConsumeEffect::isAllowed,
            ContorlGrowthConsumeEffect::new
    );

    public boolean apply(@NotNull Level level, @NotNull ItemStack stack, @NotNull LivingEntity entity) {
        if (entity instanceof TameableDragonEntity) {
            ((TameableDragonEntity) entity).setAgeLocked(!this.isAllowed);
            return true;
        }
        return false;
    }
}
