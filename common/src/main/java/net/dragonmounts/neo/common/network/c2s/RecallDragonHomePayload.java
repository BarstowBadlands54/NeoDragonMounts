package net.dragonmounts.neo.common.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

/**
 * Sent when the rider picks "Send Home" on the flute. Carries no position: the destination is
 * read from the dragon itself server-side, so a client cannot use this to teleport a dragon
 * to an arbitrary spot.
 *
 * @see net.dragonmounts.neo.common.item.FluteItem#recallHome
 */
public record RecallDragonHomePayload(UUID dragon) implements CustomPacketPayload {
    public static final Type<RecallDragonHomePayload> TYPE = new Type<>(makeId("recall_dragon_home"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecallDragonHomePayload> CODEC =
            CustomPacketPayload.codec(RecallDragonHomePayload::encode, RecallDragonHomePayload::decode);

    public static RecallDragonHomePayload decode(FriendlyByteBuf buffer) {
        return new RecallDragonHomePayload(buffer.readUUID());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.dragon);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
