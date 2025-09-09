package net.dragonmounts.neo.common.network.s2c;

import net.dragonmounts.neo.common.entity.dragon.DragonLifeStage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static net.dragonmounts.neo.common.DragonMountsShared.makeId;

public record SyncDragonAgePayload(int id, int age, DragonLifeStage stage) implements CustomPacketPayload {
    public static final Type<SyncDragonAgePayload> TYPE = new Type<>(makeId("sync_dragon"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDragonAgePayload> CODEC =
            CustomPacketPayload.codec(SyncDragonAgePayload::encode, SyncDragonAgePayload::decode);

    public static SyncDragonAgePayload decode(FriendlyByteBuf buffer) {
        return new SyncDragonAgePayload(buffer.readVarInt(), buffer.readVarInt(), DragonLifeStage.byId(buffer.readVarInt()));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.id).writeVarInt(this.age).writeVarInt(this.stage.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
