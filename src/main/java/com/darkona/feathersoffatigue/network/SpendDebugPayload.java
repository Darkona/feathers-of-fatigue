package com.darkona.feathersoffatigue.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public record SpendDebugPayload(ResourceLocation source, int cost) implements CustomPacketPayload {

    public static final Type<SpendDebugPayload> TYPE = new Type<>(id("spend_debug"));

    public static final StreamCodec<ByteBuf, SpendDebugPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SpendDebugPayload::source,
            ByteBufCodecs.VAR_INT, SpendDebugPayload::cost,
            SpendDebugPayload::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
