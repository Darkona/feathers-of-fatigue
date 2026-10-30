package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import dev.darkona.jdlib.api.debug.JDDebugFeed;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class FeathersPackets {

    private static final String PROTOCOL_VERSION = "6";

    private FeathersPackets() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(SpendRequestPayload.TYPE, SpendRequestPayload.STREAM_CODEC, SpendRequestPayload::handle);

        registrar.playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC,
                (payload, context) -> FeathersServiceImpl.acceptOnClient(payload));
    }

    public static void sendSync(ServerPlayer player, LivingEntity entity, FeathersData data) {
        send(player, SyncPayload.of(entity.getId(), data));
    }

    public static void sendSpendDebug(LivingEntity entity, ResourceLocation source, int cost) {
        if (entity instanceof ServerPlayer player) JDDebugFeed.send(player, source, cost);
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player instanceof FakePlayer || player.connection == null || !player.connection.hasChannel(payload)) return;
        PacketDistributor.sendToPlayer(player, payload);
    }
}
