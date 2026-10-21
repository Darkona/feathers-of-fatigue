package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.SpendOptions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public record SpendRequestPayload(ResourceLocation source, int stamina, boolean allowStrain, int regenDelayTicks) implements CustomPacketPayload {

    public static final Type<SpendRequestPayload> TYPE = new Type<>(id("spend_request"));

    public static final StreamCodec<ByteBuf, SpendRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SpendRequestPayload::source,
            ByteBufCodecs.VAR_INT, SpendRequestPayload::stamina,
            ByteBufCodecs.BOOL, SpendRequestPayload::allowStrain,
            ByteBufCodecs.VAR_INT, SpendRequestPayload::regenDelayTicks,
            SpendRequestPayload::new);

    static void handle(SpendRequestPayload request, IPayloadContext context) {
        if (request.stamina < 0) return;
        SpendOptions options = new SpendOptions(false, request.allowStrain, false,
                Math.clamp(request.regenDelayTicks, -1, 1200));
        FeathersAPI.spend(context.player(), request.source, request.stamina, options);
        FeathersAPI.sync(context.player());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
