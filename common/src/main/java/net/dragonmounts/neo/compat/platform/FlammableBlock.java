package net.dragonmounts.neo.compat.platform;

import net.dragonmounts.neo.compat.Dummy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
public class FlammableBlock extends Block {
    public static int getFlammability(Level level, BlockPos pos, BlockState state, Direction side) {
        return Dummy.get();
    }

    public FlammableBlock(int flammability, int spreadSpeed, Properties props) {
        super(props);
    }
}
