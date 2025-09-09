package net.dragonmounts.neo.common.api;

import net.minecraft.world.level.chunk.BlockColumn;

public interface NoiseColumnExtension {
    int neodragonmounts$getMaxHeight();

    static int getMaxHeight(BlockColumn column) {
        assert column instanceof NoiseColumnExtension;
        return ((NoiseColumnExtension) column).neodragonmounts$getMaxHeight();
    }
}
