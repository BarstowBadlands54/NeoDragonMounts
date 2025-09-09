package net.dragonmounts.neo.common.api;

import net.dragonmounts.neo.common.component.ScoreboardInfo;
import net.minecraft.world.scores.ScoreHolder;

import java.util.List;

public interface ScoreboardAccessor {
    ScoreboardInfo neodragonmounts$getInfo(ScoreHolder holder);

    void neodragonmounts$preventRemoval(ScoreHolder holder);

    void neodragonmounts$addPlayerToTeam(String name, String team);

    void neodragonmounts$loadEntries(ScoreHolder holder, List<ScoreboardInfo.Entry> entries);
}
