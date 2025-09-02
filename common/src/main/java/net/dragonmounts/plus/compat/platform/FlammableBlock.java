package net.dragonmounts.plus.compat.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.NotImplementedException;

@SuppressWarnings("unused")
public class FlammableBlock extends Block {
    public static int getFlammability(Level level, BlockPos pos, BlockState state, Direction side) {
        throw new NotImplementedException();
    }

    public FlammableBlock(int flammability, int spreadSpeed, Properties props) {
        super(props);
    }
}
