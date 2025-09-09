package net.dragonmounts.neo.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@SuppressWarnings("UnusedMixin")
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("lootTable")
    Optional<ResourceKey<LootTable>> getForcedLootTable();
}
