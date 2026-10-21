package com.darkona.feathersoffatigue.client;

import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.network.SyncPayload;
import com.darkona.feathersoffatigue.weight.WeightSplit;

public class SyncedFeathers implements FeathersView {

    protected boolean synced;
    protected int stamina, maxStamina, strain, maxStrain, bonus, weight, regenDelay;
    protected boolean exhausted;
    protected RestState rest = RestState.NONE;
    protected final WeightSplit weightSplit = new WeightSplit();

    void apply(SyncPayload p) {
        synced = true;
        stamina = p.stamina();
        maxStamina = p.maxStamina();
        strain = p.strain();
        maxStrain = p.maxStrain();
        bonus = p.bonus();
        weight = p.weight();
        regenDelay = p.regenDelay();
        exhausted = p.exhausted();
        rest = p.rest();
        weightSplit.readFrom(p.weightSplit());
    }

    @SuppressWarnings("unused")
    public int weightPart(int part) {
        return weightSplit.part(part);
    }

    public WeightSplit weightSplit() {
        return weightSplit;
    }

    void clear() {
        synced = false;
        stamina = maxStamina = strain = maxStrain = bonus = weight = regenDelay = 0;
        exhausted = false;
        rest = RestState.NONE;
        weightSplit.clear();
    }

    void tick() {
        if (regenDelay > 0) regenDelay--;
    }

    @Override
    public boolean hasFeathers() {
        return synced;
    }

    @Override
    public int stamina() {
        return stamina;
    }

    @Override
    public int maxStamina() {
        return maxStamina;
    }

    @Override
    public int availableStamina() {
        return Math.max(0, stamina - Stamina.ofFeathers(weight)) + bonus;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public int strain() {
        return strain;
    }

    @Override
    public int maxStrain() {
        return maxStrain;
    }

    @Override
    public int bonusStamina() {
        return bonus;
    }

    @Override
    public int regenDelay() {
        return regenDelay;
    }

    @Override
    public boolean exhausted() {
        return exhausted;
    }

    @Override
    public RestState restState() {
        return rest;
    }
}
