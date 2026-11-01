package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.client.ClientFeathersService;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import com.darkona.feathersoffatigue.network.DataMapsPayload;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.network.SpendDebugPayload;
import com.darkona.feathersoffatigue.network.SpendRequestPayload;
import com.darkona.feathersoffatigue.network.SyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class ClientFeathersData extends SyncedFeathers implements ClientFeathersService, FeathersServiceImpl.ClientBridge {
    public static final ClientFeathersData INSTANCE = new ClientFeathersData();

    public static final int DEBUG_LINE_TICKS = 60;

    private final SyncedFeathers mount = new SyncedFeathers();
    private int mountId = -1;

    private boolean mountSeated;

    private ResourceLocation lastSpendSource;
    private int lastSpendCost;
    private int lastSpendTicks;

    private ClientFeathersData() {}

    @Override
    public void accept(SyncPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && payload.entityId() != player.getId()) {
            if (payload.entityId() != mountId) mountSeated = false;
            mountId = payload.entityId();
            mount.apply(payload);
        } else {
            apply(payload);
        }
    }

    @Override
    public void acceptDataMaps(DataMapsPayload payload) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) FeathersDataMapRegistry.accept(payload.raw());
    }

    @Override
    public void acceptDebug(SpendDebugPayload payload) {
        lastSpendSource = payload.source();
        lastSpendCost = payload.cost();
        lastSpendTicks = DEBUG_LINE_TICKS;
    }

    @Override
    public void clear() {
        super.clear();
        mount.clear();
        mountId = -1;
        mountSeated = false;
        lastSpendSource = null;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (lastSpendTicks > 0) lastSpendTicks--;
        Entity vehicle = player != null ? player.getVehicle() : null;
        if (mountId != -1) {
            if (vehicle != null && vehicle.getId() == mountId) {
                mountSeated = true;
            } else if (mountSeated || vehicle != null) {
                mount.clear();
                mountId = -1;
                mountSeated = false;
            }
        }
        if (minecraft.isPaused()) return;
        follow(player);
        predict();
        mount.follow(mountSeated && vehicle instanceof LivingEntity living ? living : null);
        mount.predict();
    }

    public FeathersView mount() {
        return mountSeated && mount.hasFeathers() ? mount : FeathersView.NONE;
    }

    public ResourceLocation lastSpendSource() {
        return lastSpendTicks > 0 ? lastSpendSource : null;
    }

    public int lastSpendCost() {
        return lastSpendCost;
    }

    @Override
    public FeathersView local() {
        return this;
    }

    @Override
    public @Nullable FeathersData clientView(LivingEntity entity) {
        if (entity == Minecraft.getInstance().player) return this;
        return entity.getId() == mountId && mount.hasFeathers() ? mount : null;
    }

    @Override
    public boolean isLocalPlayer(LivingEntity entity) {
        return entity == Minecraft.getInstance().player;
    }

    @Override
    public SpendResult predictSpend(ResourceLocation source, int stamina, SpendOptions options) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? FeathersServiceImpl.INSTANCE.spend(player, source, stamina, options) : SpendResult.EXEMPT;
    }

    @Override
    public void requestSpend(ResourceLocation source, int stamina, SpendOptions options) {
        FeathersPackets.CHANNEL.sendToServer(new SpendRequestPayload(source, stamina, options.allowStrain(), options.regenDelayTicks()));
    }
}
