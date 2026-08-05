package net.dragonmounts.neo.common.network.c2s;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * Sent when the rider picks "Set Home" on the flute. The position is the block the player is
 * looking at, falling back to the player's own feet, and the dimension is taken from the
 * sending player server-side rather than trusted from the packet.
 *
 * @see net.dragonmounts.neo.common.item.FluteItem#setHome
 */
public record SetDragonHomePayload(UUID dragon, BlockPos pos) implements CustomPacketPayload {
    public static final Type<SetDragonHomePayload> TYPE = new Type<>(makeId("set_dragon_home"));
    public static final StreamCodec<FriendlyByteBuf, SetDragonHomePayload> CODEC =
            CustomPacketPayload.codec(SetDragonHomePayload::encode, SetDragonHomePayload::decode);

    public static SetDragonHomePayload decode(FriendlyByteBuf buffer) {
        return new SetDragonHomePayload(buffer.readUUID(), buffer.readBlockPos());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.dragon).writeBlockPos(this.pos);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
