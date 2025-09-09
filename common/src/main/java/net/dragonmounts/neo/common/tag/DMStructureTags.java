package net.dragonmounts.neo.common.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public interface DMStructureTags {
    TagKey<Structure> DRAGON_NESTS = TagKey.create(Registries.STRUCTURE, makeId("dragon_nests"));
}
