package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

public record SyncPayload(int entityId, int stamina, int maxStamina, int strain, int maxStrain, int bonus, int reserved, int regenDelay,
                          boolean exhausted, double rate, RestState rest, int[] weightSplit) {
    private static final RestState[] REST_STATES = RestState.values();
    private static final int MAX_SOURCES = 64;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(stamina);
        buf.writeVarInt(maxStamina);
        buf.writeVarInt(strain);
        buf.writeVarInt(maxStrain);
        buf.writeVarInt(bonus);
        buf.writeVarInt(reserved);
        buf.writeVarInt(regenDelay);
        buf.writeBoolean(exhausted);
        buf.writeDouble(rate);
        buf.writeVarInt(rest.ordinal());
        buf.writeVarInt(weightSplit.length);
        for (int value : weightSplit) buf.writeVarInt(value);
    }

    public static SyncPayload decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int stamina = buf.readVarInt();
        int maxStamina = buf.readVarInt();
        int strain = buf.readVarInt();
        int maxStrain = buf.readVarInt();
        int bonus = buf.readVarInt();
        int reserved = buf.readVarInt();
        int regenDelay = buf.readVarInt();
        boolean exhausted = buf.readBoolean();
        double rate = buf.readDouble();
        int rest = buf.readVarInt();
        if (rest < 0 || rest >= REST_STATES.length) throw new DecoderException("Bad rest state " + rest);
        return new SyncPayload(entityId, stamina, maxStamina, strain, maxStrain, bonus, reserved, regenDelay, exhausted, rate, REST_STATES[rest], readParts(buf));
    }

    private static int[] readParts(FriendlyByteBuf buf) {
        int length = buf.readVarInt();
        if (length < ArmorWeights.PARTS || length > ArmorWeights.PARTS + 2 * MAX_SOURCES || (length - ArmorWeights.PARTS) % 2 != 0) {
            throw new DecoderException("Bad weight split length " + length);
        }
        int[] parts = new int[length];
        for (int i = 0; i < length; i++) parts[i] = buf.readVarInt();
        return parts;
    }

    public static SyncPayload none(int entityId) {
        return new SyncPayload(entityId, 0, 0, 0, 0, 0, 0, 0, false, 0.0, RestState.NONE, new int[ArmorWeights.PARTS]);
    }

    public static SyncPayload of(int entityId, FeathersData data) {
        return new SyncPayload(entityId, data.stamina(), data.maxStamina(), data.strain(), data.maxStrain(), data.bonusStamina(),
                data.reserved(), data.regenDelay(), data.exhausted(), data.rate(), data.restState(), data.weightSplit().toArray());
    }
}
