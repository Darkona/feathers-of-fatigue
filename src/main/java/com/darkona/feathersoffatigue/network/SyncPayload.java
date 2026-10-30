package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public record SyncPayload(int entityId, RestState rest, int[] weightSplit) implements CustomPacketPayload {

    public static final Type<SyncPayload> TYPE = new Type<>(id("sync"));

    private static final RestState[] REST_STATES = RestState.values();
    private static final StreamCodec<ByteBuf, RestState> REST_CODEC =
            ByteBufCodecs.idMapper(i -> REST_STATES[i], RestState::ordinal);

    public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SyncPayload decode(@NotNull ByteBuf buf) {
            return new SyncPayload(VarInt.read(buf), REST_CODEC.decode(buf), readParts(buf));
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull SyncPayload p) {
            VarInt.write(buf, p.entityId);
            REST_CODEC.encode(buf, p.rest);
            VarInt.write(buf, p.weightSplit.length);
            for (int value : p.weightSplit) VarInt.write(buf, value);
        }
    };

    private static int[] readParts(ByteBuf buf) {
        int length = VarInt.read(buf);
        if (length < ArmorWeights.PARTS || length > ArmorWeights.PARTS + 2 * MAX_SOURCES || (length - ArmorWeights.PARTS) % 2 != 0) {
            throw new DecoderException("Bad weight split length " + length);
        }
        int[] parts = new int[length];
        for (int i = 0; i < length; i++) parts[i] = VarInt.read(buf);
        return parts;
    }

    private static final int MAX_SOURCES = 64;

    public static SyncPayload of(int entityId, FeathersData data) {
        return new SyncPayload(entityId, data.restState(), data.weightSplit().toArray());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
