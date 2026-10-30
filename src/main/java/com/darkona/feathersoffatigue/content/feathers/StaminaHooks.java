package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.StaminaModifier;
import com.darkona.feathersoffatigue.api.event.DrainEvent;
import com.darkona.feathersoffatigue.api.event.ExhaustionEvent;
import com.darkona.feathersoffatigue.api.event.RegenEvent;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.config.ServerConfig;
import dev.darkona.jdlib.core.registry.PriorityList;
import dev.darkona.jdlib.core.stat.Resource;
import dev.darkona.jdlib.core.stat.ResourceRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public enum StaminaHooks implements Resource.Hooks<ResourceLocation, LivingEntity> {
    INSTANCE;

    private static volatile ResourceRules rules = new ResourceRules(true, true, 0.0, false);

    public static ResourceRules rules() {
        ResourceRules current = rules;
        boolean strain = ServerConfig.ENABLE_STRAIN.get();
        boolean exhaustion = ServerConfig.ENABLE_EXHAUSTION.get();
        double recoverAt = ServerConfig.EXHAUSTION_RECOVERY.get();
        if (current.overdraw() != strain || current.exhaustion() != exhaustion || current.recoverAt() != recoverAt) {
            current = new ResourceRules(strain, exhaustion, recoverAt, false);
            rules = current;
        }
        return current;
    }

    @Override
    public int cost(LivingEntity entity, ResourceLocation source, int base) {
        return FeathersServiceImpl.effectiveCost(entity, FeathersServiceImpl.data(entity), source, base);
    }

    @Override
    public boolean mayRegen(LivingEntity entity) {
        return !(entity instanceof Player player && ServerConfig.REGEN_USES_HUNGER.get()
                && player.getFoodData().getFoodLevel() <= HungerRegen.HUNGRY_FOOD_LEVEL);
    }

    @Override
    public boolean resumeRegen(LivingEntity entity) {
        return !NeoForge.EVENT_BUS.post(new RegenEvent(entity)).isCanceled();
    }

    @Override
    public double regenFactor(LivingEntity entity) {
        return ServerConfig.REST_BOOSTS_REGEN.get() ? FeathersServiceImpl.data(entity).restMultiplier : 1.0;
    }

    @Override
    public int regen(LivingEntity entity, int units) {
        FeathersData data = FeathersServiceImpl.data(entity);
        PriorityList.Snapshot<ResourceLocation, StaminaModifier> modifiers = Extensions.modifiers();
        int regen = units;
        for (int i = 0; i < modifiers.size(); i++) {
            regen = modifiers.value(i).modifyRegen(entity, data, regen);
        }
        return regen;
    }

    @Override
    public double overdrawRecovery(LivingEntity entity) {
        return ServerConfig.REST_BOOSTS_REGEN.get() ? 1.0 : FeathersServiceImpl.data(entity).restMultiplier;
    }

    @Override
    public void regenerated(LivingEntity entity, int units) {
        FeathersServiceImpl.data(entity).totalRegenerated += units;
        if (entity instanceof Player player && ServerConfig.REGEN_USES_HUNGER.get()) {
            player.causeFoodExhaustion((float) (units / (double) Stamina.PER_FEATHER * ServerConfig.HUNGER_PER_FEATHER.get()));
        }
    }

    @Override
    public void drained(LivingEntity entity, ResourceLocation source, int cost) {
        FeathersServiceImpl.data(entity).logSpend(source, cost, entity.level().getGameTime());
    }

    @Override
    public void drainStopped(LivingEntity entity, ResourceLocation source, Resource.Stop reason) {
        NeoForge.EVENT_BUS.post(new DrainEvent.Stopped(entity, source, switch (reason) {
            case TIMED_OUT -> DrainEvent.Stopped.Reason.TIMED_OUT;
            case EXHAUSTED -> DrainEvent.Stopped.Reason.EXHAUSTED;
            case INSUFFICIENT -> DrainEvent.Stopped.Reason.INSUFFICIENT;
        }));
    }

    @Override
    public void overdrawStarted(LivingEntity entity) {
        NeoForge.EVENT_BUS.post(new StrainEvent.Started(entity));
    }

    @Override
    public void overdrawCleared(LivingEntity entity) {
        NeoForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
    }

    @Override
    public void exhausted(LivingEntity entity) {
        NeoForge.EVENT_BUS.post(new ExhaustionEvent.Exhausted(entity));
    }

    @Override
    public void recovered(LivingEntity entity) {
        NeoForge.EVENT_BUS.post(new ExhaustionEvent.Recovered(entity));
    }
}
