package net.dragonmounts.neo.common.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * Client -> server request to set a tamed dragon's V-formation flight rank.
 * {@code rank} of 0 means "auto" (age-based ordering); 1+ is an explicit slot.
 */
public record SetFlightRankPayload(int dragon, int rank) implements CustomPacketPayload {
    public static final Type<SetFlightRankPayload> TYPE = new Type<>(makeId("set_flight_rank"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFlightRankPayload> CODEC =
            CustomPacketPayload.codec(SetFlightRankPayload::encode, SetFlightRankPayload::decode);

    public static SetFlightRankPayload decode(FriendlyByteBuf buffer) {
        return new SetFlightRankPayload(buffer.readVarInt(), buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.dragon);
        buffer.writeVarInt(this.rank);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}