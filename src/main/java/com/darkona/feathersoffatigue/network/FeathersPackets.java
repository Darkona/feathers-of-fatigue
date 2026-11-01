package com.darkona.feathersoffatigue.network;

import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class FeathersPackets {
    private static final String PROTOCOL_VERSION = "6";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(id("main"), () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private FeathersPackets() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SpendRequestPayload.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpendRequestPayload::encode).decoder(SpendRequestPayload::decode)
                .consumerMainThread(SpendRequestPayload::handle).add();

        CHANNEL.messageBuilder(SyncPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncPayload::encode).decoder(SyncPayload::decode)
                .consumerMainThread((payload, context) -> FeathersServiceImpl.acceptOnClient(payload))
                .add();
        CHANNEL.messageBuilder(SpendDebugPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpendDebugPayload::encode).decoder(SpendDebugPayload::decode)
                .consumerMainThread((payload, context) -> FeathersServiceImpl.acceptDebugOnClient(payload))
                .add();
        CHANNEL.messageBuilder(DataMapsPayload.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DataMapsPayload::encode).decoder(DataMapsPayload::decode)
                .consumerMainThread((payload, context) -> FeathersServiceImpl.acceptDataMapsOnClient(payload))
                .add();
    }

    public static void sendSync(ServerPlayer player, LivingEntity entity, FeathersData data) {
        send(player, SyncPayload.of(entity.getId(), data));
    }

    public static void sendNoFeathers(ServerPlayer player, LivingEntity entity) {
        send(player, SyncPayload.none(entity.getId()));
    }

    public static void sendSpendDebug(LivingEntity entity, ResourceLocation source, int cost) {
        if (entity instanceof ServerPlayer player) send(player, new SpendDebugPayload(source, cost));
    }

    public static void sendDataMaps(ServerPlayer player) {
        send(player, new DataMapsPayload(FeathersDataMapRegistry.raw()));
    }

    private static void send(ServerPlayer player, Object payload) {
        if (player instanceof FakePlayer || player.connection == null || !CHANNEL.isRemotePresent(player.connection.connection)) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
}
