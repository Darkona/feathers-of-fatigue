package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.client.ClientFeathersService;
import com.darkona.feathersoffatigue.network.SpendRequestPayload;
import com.darkona.feathersoffatigue.network.SyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class ClientFeathersData extends SyncedFeathers implements ClientFeathersService, FeathersServiceImpl.ClientBridge {

    public static final ClientFeathersData INSTANCE = new ClientFeathersData();

    private final SyncedFeathers mount = new SyncedFeathers();

    private ClientFeathersData() {}

    @Override
    public void accept(SyncPayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && payload.entityId() != player.getId()) {
            mount.apply(payload);
        } else {
            apply(payload);
        }
    }

    @Override
    public void clear() {
        super.clear();
        mount.clear();
    }

    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        follow(player);
        predict();
        Entity vehicle = player != null ? player.getVehicle() : null;
        mount.follow(vehicle instanceof LivingEntity living ? living : null);
        mount.predict();
    }

    public FeathersView mount() {
        return mount.hasFeathers() ? mount : FeathersView.NONE;
    }

    @Override
    public FeathersView local() {
        return this;
    }

    @Override
    public @Nullable FeathersView clientView(LivingEntity entity) {
        if (entity == Minecraft.getInstance().player) return this;
        return entity == mount.entity() && mount.hasFeathers() ? mount : null;
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
        PacketDistributor.sendToServer(new SpendRequestPayload(source, stamina, options.allowStrain(), options.regenDelayTicks()));
    }

    @Override
    public FeathersView localView() {
        return this;
    }
}
