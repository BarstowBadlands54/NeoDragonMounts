package net.dragonmounts.neo.compat.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegistryBuilder;

import static net.dragonmounts.neo.common.DragonMountsShared.COOLDOWN_CATEGORY;

@SuppressWarnings("ClassCanBeRecord")
public class CooldownCategory {
    public static final Registry<CooldownCategory> REGISTRY = new RegistryBuilder<>(COOLDOWN_CATEGORY).sync(true).create();

    public final ResourceLocation identifier;

    public CooldownCategory(ResourceLocation identifier) {
        this.identifier = identifier;
    }

    public final int getId() {
        return REGISTRY.getId(this);
    }
}
