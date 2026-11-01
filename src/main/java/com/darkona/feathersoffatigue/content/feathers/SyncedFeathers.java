package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.network.SyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public class SyncedFeathers extends FeathersData {

    private static final ResourceLocation SYNCED_BONUS = id("synced_bonus");

    protected boolean synced;
    private double predictedRate;
    private @Nullable LivingEntity entity;

    public void apply(SyncPayload p) {
        synced = true;
        stamina = p.stamina();
        maxStamina = p.maxStamina();
        strain = p.strain();
        maxStrain = p.maxStrain();
        reserved = p.reserved();
        regenDelay = p.regenDelay();
        exhausted = p.exhausted();
        predictedRate = p.rate();
        restState = p.rest();
        bonuses.clear();
        if (p.bonus() > 0) setBonus(SYNCED_BONUS, p.bonus(), FOREVER);
        weightSplit.readFrom(p.weightSplit());
    }

    public void follow(@Nullable LivingEntity followed) {
        if (followed == entity) return;
        entity = followed;
        forgetAttributes();
    }

    public void predict() {
        LivingEntity e = entity;
        if (e != null && synced && !FeathersServiceImpl.isExempt(e)) FeathersTicker.tickStamina(e, this, predictedRate);
    }

    void clear() {
        synced = false;
        stamina = 0;
        maxStamina = 0;
        strain = 0;
        maxStrain = 0;
        reserved = 0;
        regenDelay = 0;
        exhausted = false;
        predictedRate = 0.0;
        restState = RestState.NONE;
        bonuses.clear();
        drains.clear();
        weightSplit.clear();
        follow(null);
    }

    @Override
    public boolean hasFeathers() {
        return synced;
    }

    @Override
    boolean predicted() {
        return true;
    }
}
