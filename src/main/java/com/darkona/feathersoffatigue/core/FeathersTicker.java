package com.darkona.feathersoffatigue.core;

import com.darkona.feathersoffatigue.api.StaminaModifier;
import com.darkona.feathersoffatigue.api.RegenFactor;
import dev.darkona.jdlib.core.registry.PriorityList;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.event.DrainEvent;
import com.darkona.feathersoffatigue.api.event.ExhaustionEvent;
import com.darkona.feathersoffatigue.api.event.RegenEvent;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.registry.ModEffects;
import com.darkona.feathersoffatigue.mount.MountExertion;
import com.darkona.feathersoffatigue.mount.MountTraits;
import com.darkona.feathersoffatigue.network.FeathersNetwork;
import com.darkona.feathersoffatigue.weight.ArmorWeights;
import dev.darkona.jdlib.core.stat.ShownFields;
import dev.darkona.jdlib.core.stat.SourcedDrains;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class FeathersTicker {

    private static final int ATTRIBUTE_INTERVAL = 10;
    private static final int REGEN_FACTOR_INTERVAL = 20;
    private static final double STILL_EPSILON_SQR = 1.0E-4;
    private static final ResourceLocation REGEN_FACTORS = id("regen_factors");

    private FeathersTicker() {}

    public static void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (event.has(type, FeathersAttributes.MAX_FEATHERS)) continue;
            event.add(type, FeathersAttributes.MAX_FEATHERS);
            event.add(type, FeathersAttributes.MAX_STRAIN);
            event.add(type, FeathersAttributes.FEATHERS_PER_SECOND);
            event.add(type, FeathersAttributes.USAGE_MULTIPLIER);
            event.add(type, FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER);
        }
    }

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != FeathersServerConfig.SPEC || event instanceof ModConfigEvent.Unloading) return;
        ArmorWeights.invalidate();
        FeathersServiceImpl.invalidateMountTypes();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(() -> server.getPlayerList().getPlayers().forEach(FeathersTicker::refreshFromConfig));
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)
                || !FeathersServiceImpl.INSTANCE.supports(entity)) return;
        refreshFromConfig(entity);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        FeathersServiceImpl.INSTANCE.reset(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        FeathersData data = FeathersServiceImpl.data(event.getEntity());
        if (event.isWasDeath() && event.getOriginal().hasData(FeathersAttachments.FEATHERS)) {
            FeathersData original = FeathersServiceImpl.data(event.getOriginal());
            copyCounter(original, data, BASE_MAX);
            copyCounter(original, data, BASE_STRAIN);
            copyCounter(original, data, BASE_REGEN);
        }
        data.shown.resendNext();
    }

    private static void copyCounter(FeathersData from, FeathersData to, String key) {
        if (from.counters.containsKey(key)) to.setCounter(key, from.getCounter(key));
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        FeathersServiceImpl.data(event.getEntity()).shown.resendNext();
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        FeathersServiceImpl.data(event.getEntity()).shown.resendNext();
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !FeathersServerConfig.SLEEPING_ALWAYS_RESTORES_FEATHERS.get()) return;
        if (!player.isSleepingLongEnough()) return;
        if (event.updateLevel() ? !player.level().isDay() : event.wakeImmediately()) return;
        FeathersServiceImpl.INSTANCE.reset(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().isArmor() && !event.getEntity().level().isClientSide()) {
            FeathersServiceImpl.INSTANCE.recalculateWeight(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        ArmorWeights.invalidate();
        FeathersServiceImpl.invalidateMountTypes();
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().getPlayers().forEach(FeathersServiceImpl.INSTANCE::recalculateWeight);
    }

    @SubscribeEvent
    public static void onDataMapsUpdated(DataMapsUpdatedEvent event) {
        FeathersServiceImpl.invalidateMountTypes();
    }

    static void refreshFromConfig(LivingEntity entity) {
        boolean player = entity instanceof Player;
        FeathersData data = FeathersServiceImpl.data(entity);
        if (player) configBase(entity, data, FeathersAttributes.MAX_FEATHERS, BASE_MAX, FeathersServerConfig.MAX_FEATHERS.get());
        else MountTraits.ensureRolled(entity);
        configBase(entity, data, FeathersAttributes.MAX_STRAIN, BASE_STRAIN, FeathersServerConfig.MAX_STRAIN.get());
        configBase(entity, data, FeathersAttributes.FEATHERS_PER_SECOND, BASE_REGEN, player ? FeathersServerConfig.REGEN_FEATHERS_PER_SECOND.get()
                : FeathersServiceImpl.mountTuning(entity).regenPerSecond());
        FeathersServiceImpl.ensureInitialized(entity, data);
        FeathersServiceImpl.refreshMaximums(entity, data);
        data.weight = ArmorWeights.totalWeight(entity, data.weightSplit);
        data.shown.resendNext();
    }

    private static final String BASE_MAX = "config_base.max_feathers";
    private static final String BASE_STRAIN = "config_base.max_strain";
    private static final String BASE_REGEN = "config_base.feathers_per_second";

    private static void configBase(LivingEntity entity, FeathersData data, Holder<Attribute> attribute, String key, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (data.counters.containsKey(key) && instance.getBaseValue() != data.getCounter(key)) return;
        if (instance.getBaseValue() != value) instance.setBaseValue(value);
        data.setCounter(key, value);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !player.isAlive()) return;
        tick(player);
    }

    public static void tick(LivingEntity entity) {
        FeathersData data = FeathersServiceImpl.data(entity);
        if (!data.initialized && !(entity instanceof Player)) refreshFromConfig(entity);
        FeathersServiceImpl.ensureInitialized(entity, data);

        if (!FeathersServiceImpl.isExempt(entity)) {
            long now = entity.level().getGameTime();
            boolean strainEnabled = FeathersServerConfig.ENABLE_STRAIN.get();
            int staminaBefore = data.stamina;
            boolean strainedBefore = data.strain > 0;

            if (entity.tickCount % ATTRIBUTE_INTERVAL == 0) tickAttributes(entity, data);
            if (entity.tickCount % REGEN_FACTOR_INTERVAL == 0) applyRegenFactors(entity, data);
            if (entity instanceof Player && entity.tickCount % ClimateEffects.INTERVAL == 0) {
                data.climate = ClimateEffects.evaluate(entity);
                ClimateEffects.apply(entity, data.climate);
            }

            data.expire(now);
            updateRest(entity, data);
            tickDrains(entity, data, now, strainEnabled);
            regenerate(entity, data);

            data.lastDelta = data.stamina - staminaBefore;
            if (!strainEnabled && data.strain > 0) data.strain = 0;
            boolean strained = data.strain > 0;
            if (strainedBefore && !strained) NeoForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));

            FeathersServiceImpl.checkExhausted(entity, data, strainEnabled);
            checkRecovered(entity, data);
            updateIndicators(entity, data, strained);
        }

        syncIfChanged(entity, data);
    }

    private static void tickAttributes(LivingEntity entity, FeathersData data) {
        FeathersServiceImpl.refreshMaximums(entity, data);

        AttributeInstance multiplier = data.weightMultiplierAttribute(entity);
        double value = multiplier != null ? multiplier.getValue() : 1.0;
        if (value != data.lastWeightMultiplier) {
            data.lastWeightMultiplier = value;
            data.weight = ArmorWeights.totalWeight(entity, data.weightSplit);
        }

        if (entity.hasEffect(FeathersMobEffects.ENDURANCE) && !data.bonuses.has(ModEffects.ENDURANCE_BONUS)) {
            entity.removeEffect(FeathersMobEffects.ENDURANCE);
        }
    }

    private static void applyRegenFactors(LivingEntity entity, FeathersData data) {
        PriorityList.Snapshot<ResourceLocation, RegenFactor> factors = Extensions.regenFactors();
        AttributeInstance regen = data.regenAttribute(entity);
        if (regen == null) return;

        double sum = 0;
        for (int i = 0; i < factors.size(); i++) {
            sum += factors.value(i).feathersPerSecond(entity, data);
        }
        AttributeModifier current = regen.getModifier(REGEN_FACTORS);
        if (sum == 0) {
            if (current != null) regen.removeModifier(REGEN_FACTORS);
        } else if (current == null || current.amount() != sum) {
            regen.addOrUpdateTransientModifier(new AttributeModifier(REGEN_FACTORS, sum, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void updateRest(LivingEntity entity, FeathersData data) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        double dx = x - data.lastX;
        double dy = y - data.lastY;
        double dz = z - data.lastZ;
        data.lastX = x;
        data.lastY = y;
        data.lastZ = z;

        if (!FeathersServerConfig.ENABLE_REST.get()) {
            data.restState = RestState.NONE;
            data.restMultiplier = 1.0;
            return;
        }

        boolean moved = dx * dx + dy * dy + dz * dz > STILL_EPSILON_SQR || entity.isSwimming() || entity.isFallFlying();
        data.stillTicks = moved ? 0 : data.stillTicks + 1;

        double multiplier;
        if (entity.isPassenger()) {
            data.restState = RestState.SITTING;
            multiplier = FeathersServerConfig.REST_SITTING_MULTIPLIER.get();
        } else if (data.stillTicks >= FeathersServerConfig.REST_STILL_TICKS.get()) {
            boolean crouching = entity.isCrouching();
            data.restState = crouching ? RestState.CROUCHING : RestState.STILL;
            multiplier = crouching ? FeathersServerConfig.REST_CROUCHING_MULTIPLIER.get() : FeathersServerConfig.REST_STILL_MULTIPLIER.get();
        } else {
            data.restState = RestState.NONE;
            multiplier = 1.0;
        }

        data.restMultiplier = data.restBonuses.max(multiplier);
    }

    private static void tickDrains(LivingEntity entity, FeathersData data, long now, boolean strainEnabled) {
        SourcedDrains<ResourceLocation> drains = data.drains;
        for (int i = drains.size() - 1; i >= 0; i--) {
            ResourceLocation source = drains.source(i);

            DrainEvent.Stopped.Reason stop = null;
            if (drains.timedOut(i, now)) {
                stop = DrainEvent.Stopped.Reason.TIMED_OUT;
            } else if (data.exhausted) {
                stop = DrainEvent.Stopped.Reason.EXHAUSTED;
            } else {
                int base = drains.advance(i);
                if (base > 0) {
                    int cost = FeathersServiceImpl.effectiveCost(entity, data, source, base);
                    if (data.canPay(cost, drains.overdraws(i), strainEnabled)) {
                        FeathersServiceImpl.payAndSettle(entity, data, cost, strainEnabled);
                        data.logSpend(source, cost, now);
                    } else {
                        stop = DrainEvent.Stopped.Reason.INSUFFICIENT;
                        FeathersServiceImpl.checkExhausted(entity, data, strainEnabled);
                    }
                }
            }

            if (stop != null) {
                drains.removeAt(i);
                NeoForge.EVENT_BUS.post(new DrainEvent.Stopped(entity, source, stop));
            }
        }
    }

    private static void regenerate(LivingEntity entity, FeathersData data) {
        boolean blocked = data.isRegenBlocked();
        if (data.regenDelay > 0) data.regenDelay--;

        AttributeInstance attribute = data.regenAttribute(entity);
        double perTick = attribute != null ? Stamina.perTick(attribute.getValue()) : 0.0;

        if (perTick > 0) {
            boolean hungry = entity instanceof Player player && FeathersServerConfig.REGEN_USES_HUNGER.get()
                    && player.getFoodData().getFoodLevel() <= HungerRegen.HUNGRY_FOOD_LEVEL;
            if (blocked || hungry) {
                data.regenCarry.reset();
                data.regenPaused = true;
                return;
            }
            if (data.regenPaused && data.stamina < data.maxStamina) {
                if (NeoForge.EVENT_BUS.post(new RegenEvent(entity)).isCanceled()) return;
                data.regenPaused = false;
            }
            if (FeathersServerConfig.REST_BOOSTS_REGEN.get()) perTick *= data.restMultiplier;
        }

        int regen = data.regenCarry.add(perTick);

        PriorityList.Snapshot<ResourceLocation, StaminaModifier> modifiers = Extensions.modifiers();
        for (int i = 0; i < modifiers.size(); i++) {
            regen = modifiers.value(i).modifyRegen(entity, data, regen);
        }

        if (regen < 0) {
            data.stamina = Math.max(0, data.stamina + regen);
            return;
        }
        if (regen == 0) return;

        int recovered = 0;
        if (data.strain > 0) {
            double rest = FeathersServerConfig.REST_BOOSTS_REGEN.get() ? 1.0 : data.restMultiplier;
            int recovery = (int) Math.round(regen * rest);
            recovered = Math.min(data.strain, recovery);
            data.strain -= recovered;
            regen = (int) ((recovery - recovered) / rest);
        }

        int gained = Math.min(regen, Math.max(0, data.maxStamina - data.stamina));
        data.stamina += gained;
        data.totalRegenerated += gained + recovered;

        if (entity instanceof Player player && FeathersServerConfig.REGEN_USES_HUNGER.get() && gained + recovered > 0) {
            player.causeFoodExhaustion((float) ((gained + recovered) / (double) Stamina.PER_FEATHER * FeathersServerConfig.HUNGER_PER_FEATHER.get()));
        }
    }

    static void checkRecovered(LivingEntity entity, FeathersData data) {
        if (data.maxStamina <= 0) {
            data.exhausted = false;
            return;
        }
        if (!data.exhausted) return;
        boolean recovered = !FeathersServerConfig.ENABLE_EXHAUSTION.get()
                || data.strain == 0 && data.availableStamina() >= usableMax(data) * FeathersServerConfig.EXHAUSTION_RECOVERY.get();
        if (recovered) {
            data.exhausted = false;
            NeoForge.EVENT_BUS.post(new ExhaustionEvent.Recovered(entity));
        }
    }

    private static int usableMax(FeathersData data) {
        return Math.max(0, data.maxStamina - Stamina.ofFeathers(data.weight));
    }

    private static void updateIndicators(LivingEntity entity, FeathersData data, boolean strained) {
        if (strained == entity.hasEffect(FeathersMobEffects.STRAINED)) return;
        if (strained) {
            entity.addEffect(new MobEffectInstance(FeathersMobEffects.STRAINED, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        } else {
            entity.removeEffect(FeathersMobEffects.STRAINED);
        }
    }

    public static void releaseExMount(LivingEntity entity) {
        if (!entity.hasData(FeathersAttachments.FEATHERS)) return;
        FeathersData data = FeathersServiceImpl.data(entity);
        MobEffectInstance effect = entity.getEffect(FeathersMobEffects.STRAINED);
        boolean ownEffect = effect != null && effect.isInfiniteDuration() && !effect.isVisible();
        if (!ownEffect && !data.initialized && data.strain == 0 && !data.exhausted && !data.mountSlowed && data.shown.sent(FeathersData.Shown.MAX_STAMINA) <= 0) return;

        if (ownEffect) entity.removeEffect(FeathersMobEffects.STRAINED);
        if (data.strain > 0) {
            data.strain = 0;
            NeoForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
        }
        if (data.exhausted) {
            data.exhausted = false;
            NeoForge.EVENT_BUS.post(new ExhaustionEvent.Recovered(entity));
        }
        data.mountSlowed = false;
        data.drains.clear();
        data.initialized = false;
        if (data.shown.sent(FeathersData.Shown.MAX_STAMINA) > 0 && MountExertion.riderOf(entity) instanceof ServerPlayer rider) {
            FeathersNetwork.sendNoFeathers(rider, entity);
        }
        data.shown.forget();
    }

    private static void syncIfChanged(LivingEntity entity, FeathersData data) {
        ServerPlayer player = entity instanceof ServerPlayer self ? self
                : MountExertion.riderOf(entity) instanceof ServerPlayer rider ? rider : null;
        if (player == null) return;

        int shownStamina = FeathersServerConfig.DEBUG_MODE.get() ? data.stamina : Stamina.toFeathers(data.stamina);
        int shownStrain = Stamina.toFeathersCeil(data.strain);
        int shownBonus = Stamina.toFeathersCeil(data.bonusStamina());
        boolean delayed = data.regenDelay > 0;

        ShownFields<FeathersData.Shown> shown = data.shown;
        shown.show(FeathersData.Shown.STAMINA, shownStamina);
        shown.show(FeathersData.Shown.MAX_STAMINA, data.maxStamina);
        shown.show(FeathersData.Shown.STRAIN, shownStrain);
        shown.show(FeathersData.Shown.MAX_STRAIN, data.maxStrain);
        shown.show(FeathersData.Shown.BONUS, shownBonus);
        shown.show(FeathersData.Shown.WEIGHT, data.weight);
        shown.show(FeathersData.Shown.EXHAUSTED, data.exhausted);
        shown.show(FeathersData.Shown.DELAYED, delayed);
        shown.show(FeathersData.Shown.REST, data.restState.ordinal());
        boolean splitChanged = !data.weightSplit.sameAs(data.syncedWeightSplit);
        if (!shown.shouldSend() && !splitChanged) return;
        data.syncedWeightSplit.copyFrom(data.weightSplit);
        FeathersNetwork.sendSync(player, entity, data);
    }
}
