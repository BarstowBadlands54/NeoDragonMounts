package net.dragonmounts.neo.data;

import net.dragonmounts.neo.common.DragonMountsShared;
import net.dragonmounts.neo.common.init.DMEntities;
import net.dragonmounts.neo.common.tag.DMEntityTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;

import java.util.concurrent.CompletableFuture;

public class DMEntityTagProvider extends EntityTypeTagsProvider {
    public DMEntityTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider, DragonMountsShared.NAMESPACE);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        this.tag(DMEntityTags.DRAGONS)
                .add(DMEntities.TAMEABLE_DRAGON.key)
                .add(DMEntities.HATCHABLE_DRAGON_EGG.key);
        this.tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER)
                .addTag(DMEntityTags.DRAGONS);
        this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE)
                .addTag(DMEntityTags.DRAGONS);
    }
}
