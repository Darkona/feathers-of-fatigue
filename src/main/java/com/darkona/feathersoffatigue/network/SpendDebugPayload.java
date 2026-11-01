package com.darkona.feathersoffatigue.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SpendDebugPayload(ResourceLocation source, int cost) {
    void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(source);
        buf.writeVarInt(cost);
    }

    static SpendDebugPayload decode(FriendlyByteBuf buf) {
        return new SpendDebugPayload(buf.readResourceLocation(), buf.readVarInt());
    }
}
