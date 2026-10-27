package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
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
    private int mountId = -1;
    private boolean mountSeated;


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
    public void clear() {
        super.clear();
        mount.clear();
        mountId = -1;
        mountSeated = false;
    }

    @Override
    public void tick() {
        super.tick();
        mount.tick();
        if (mountId == -1) return;
        LocalPlayer player = Minecraft.getInstance().player;
        Entity vehicle = player != null ? player.getVehicle() : null;
        if (vehicle != null && vehicle.getId() == mountId) {
            mountSeated = true;
        } else if (mountSeated || vehicle != null) {
            mount.clear();
            mountId = -1;
            mountSeated = false;
        }
    }

    public FeathersView mount() {
        return mountSeated && mount.hasFeathers() ? mount : FeathersView.NONE;
    }

    @Override
    public FeathersView local() {
        return this;
    }

    @Override
    public @Nullable FeathersView clientView(LivingEntity entity) {
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
    public SpendResult payPredicted(int cost, SpendOptions options) {
        if (!synced) return SpendResult.EXEMPT;
        SpendResult check = FeathersServiceImpl.simulateAgainst(this, cost, options.allowStrain(), options.ignoreExhaustion());
        if (check != SpendResult.OK) return check;

        int fromBonus = Math.min(bonus, cost);
        bonus -= fromBonus;
        int left = cost - fromBonus;
        int fromStamina = Math.min(Math.max(0, stamina - Stamina.ofFeathers(weight)), left);
        stamina -= fromStamina;
        strain += left - fromStamina;
        regenDelay = FeathersServiceImpl.regenDelayAfter(regenDelay, options);
        return SpendResult.OK;
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
