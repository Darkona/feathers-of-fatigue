package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.SpendOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SpendRequestPayload(ResourceLocation source, int stamina, boolean allowStrain, int regenDelayTicks) {
    void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(source);
        buf.writeVarInt(stamina);
        buf.writeBoolean(allowStrain);
        buf.writeVarInt(regenDelayTicks);
    }

    static SpendRequestPayload decode(FriendlyByteBuf buf) {
        return new SpendRequestPayload(buf.readResourceLocation(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
    }

    static void handle(SpendRequestPayload request, Supplier<NetworkEvent.Context> context) {
        ServerPlayer player = context.get().getSender();
        if (player == null || request.stamina < 0) return;

        SpendOptions options = new SpendOptions(false, request.allowStrain, false, Math.max(-1, Math.min(request.regenDelayTicks, 1200)));
        FeathersAPI.spend(player, request.source, request.stamina, options);

        FeathersAPI.sync(player);
    }
}
