package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.core.FeathersData;
import com.darkona.feathersoffatigue.weight.ArmorWeights;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public record SyncPayload(int entityId, int stamina, int maxStamina, int strain, int maxStrain, int bonus, int weight, int regenDelay,
                          boolean exhausted, RestState rest, int[] weightSplit) implements CustomPacketPayload {

    public static final Type<SyncPayload> TYPE = new Type<>(id("sync"));

    private static final RestState[] REST_STATES = RestState.values();
    private static final StreamCodec<ByteBuf, RestState> REST_CODEC =
            ByteBufCodecs.idMapper(i -> REST_STATES[i], RestState::ordinal);

    public static final StreamCodec<ByteBuf, SyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull SyncPayload decode(@NotNull ByteBuf buf) {
            return new SyncPayload(VarInt.read(buf), VarInt.read(buf), VarInt.read(buf), VarInt.read(buf),
                    VarInt.read(buf), VarInt.read(buf), VarInt.read(buf),
                    VarInt.read(buf), ByteBufCodecs.BOOL.decode(buf), REST_CODEC.decode(buf), readParts(buf));
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull SyncPayload p) {
            VarInt.write(buf, p.entityId);
            VarInt.write(buf, p.stamina);
            VarInt.write(buf, p.maxStamina);
            VarInt.write(buf, p.strain);
            VarInt.write(buf, p.maxStrain);
            VarInt.write(buf, p.bonus);
            VarInt.write(buf, p.weight);
            VarInt.write(buf, p.regenDelay);
            ByteBufCodecs.BOOL.encode(buf, p.exhausted);
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
        return new SyncPayload(entityId, data.stamina(), data.maxStamina(), data.strain(), data.maxStrain(), data.bonusStamina(),
                data.weight(), data.regenDelay(), data.exhausted(), data.restState(), weightParts(data));
    }

    public static SyncPayload none(int entityId) {
        return new SyncPayload(entityId, 0, 0, 0, 0, 0, 0, 0, false, RestState.NONE, new int[ArmorWeights.PARTS]);
    }

    private static int[] weightParts(FeathersData data) {
        return data.weightSplit().toArray();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
