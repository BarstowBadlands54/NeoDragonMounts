package net.dragonmounts.neo.common.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * One payload for both flute home actions, replacing the earlier SetDragonHomePayload and
 * RecallDragonHomePayload. Two payloads meant two registrations in each loader; this needs one.
 * <p>
 * Deliberately carries no position. "Set Home" now marks wherever the player is standing, and
 * the server reads that from the sending player — so a modified client can neither place a
 * dragon's home somewhere it has never been nor use "Send Home" as a free teleport.
 *
 * @param recall false to set home at the player's feet, true to send the dragon to it
 */
public record DragonHomePayload(UUID dragon, boolean recall) implements CustomPacketPayload {
    public static final Type<DragonHomePayload> TYPE = new Type<>(makeId("dragon_home"));
    public static final StreamCodec<FriendlyByteBuf, DragonHomePayload> CODEC =
            CustomPacketPayload.codec(DragonHomePayload::encode, DragonHomePayload::decode);

    public static DragonHomePayload decode(FriendlyByteBuf buffer) {
        return new DragonHomePayload(buffer.readUUID(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.dragon);
        buffer.writeBoolean(this.recall);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
