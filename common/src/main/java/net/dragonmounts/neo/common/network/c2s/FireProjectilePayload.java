package net.dragonmounts.neo.common.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public record FireProjectilePayload(int dragon, float power) implements CustomPacketPayload {
    public static final Type<FireProjectilePayload> TYPE = new Type<>(makeId("fire_projectile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireProjectilePayload> CODEC =
            CustomPacketPayload.codec(FireProjectilePayload::encode, FireProjectilePayload::decode);

    public static FireProjectilePayload decode(FriendlyByteBuf buffer) {
        return new FireProjectilePayload(buffer.readVarInt(), buffer.readFloat());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.dragon);
        buffer.writeFloat(this.power);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}