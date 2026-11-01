package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.event.DrainEvent;
import com.darkona.feathersoffatigue.api.event.ExhaustionEvent;
import com.darkona.feathersoffatigue.api.event.RegenEvent;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.content.feathers.ClimateEffects;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.mount.MountExertion;
import com.darkona.feathersoffatigue.content.mount.MountTraits;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@Mod.EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class FeathersTicker {
    private static final int ATTRIBUTE_INTERVAL = 10;
    private static final int REGEN_FACTOR_INTERVAL = 20;
    private static final double STILL_EPSILON_SQR = 1.0E-4;
    private static final ResourceLocation REGEN_FACTORS = id("regen_factors");
    private static final UUID REGEN_FACTORS_ID = modifierId(REGEN_FACTORS);

    public static UUID modifierId(ResourceLocation id) {
        return UUID.nameUUIDFromBytes(id.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void toggleModifier(LivingEntity entity, Attribute attribute, ResourceLocation name, UUID id, boolean on, double amount,
                                      AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier current = instance.getModifier(id);
        if (on && current != null && current.getAmount() == amount) return;
        if (current != null) instance.removeModifier(id);
        if (on) instance.addTransientModifier(new AttributeModifier(id, name.toString(), amount, operation));
    }

    private FeathersTicker() {}

    public static void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            if (event.has(type, FeathersAttributes.MAX_FEATHERS.get())) continue;
            event.add(type, FeathersAttributes.MAX_FEATHERS.get());
            event.add(type, FeathersAttributes.MAX_STRAIN.get());
            event.add(type, FeathersAttributes.FEATHERS_PER_SECOND.get());
            event.add(type, FeathersAttributes.USAGE_MULTIPLIER.get());
            event.add(type, FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER.get());
            event.add(type, FeathersAttributes.ARMOR_WEIGHT.get());
        }
    }

    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != ServerConfig.SPEC || event instanceof ModConfigEvent.Unloading) return;
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

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClone(PlayerEvent.Clone event) {
        FeathersServiceImpl.data(event.getEntity()).forceSync = true;
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        FeathersServiceImpl.data(event.getEntity()).forceSync = true;
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        FeathersServiceImpl.data(event.getEntity()).forceSync = true;
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !ServerConfig.SLEEPING_ALWAYS_RESTORES_FEATHERS.get()) return;

        if (!player.isSleepingLongEnough()) return;
        if (event.updateLevel() ? !player.level().isDay() : event.wakeImmediately()) return;
        FeathersServiceImpl.INSTANCE.reset(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() == EquipmentSlot.Type.ARMOR && !event.getEntity().level().isClientSide()
                && FeathersServiceImpl.INSTANCE.supports(event.getEntity())) {
            FeathersServiceImpl.INSTANCE.recalculateWeight(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        FeathersDataMapRegistry.onTagsChanged();
        ArmorWeights.invalidate();
        FeathersServiceImpl.invalidateMountTypes();
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().getPlayers().forEach(FeathersServiceImpl.INSTANCE::recalculateWeight);
    }

    static void refreshFromConfig(LivingEntity entity) {
        boolean player = entity instanceof Player;

        FeathersData data = FeathersServiceImpl.data(entity);
        if (player) configBase(entity, data, FeathersAttributes.MAX_FEATHERS, BASE_MAX, ServerConfig.MAX_FEATHERS.get());
        else MountTraits.ensureRolled(entity);
        configBase(entity, data, FeathersAttributes.MAX_STRAIN, BASE_STRAIN, ServerConfig.MAX_STRAIN.get());
        configBase(entity, data, FeathersAttributes.FEATHERS_PER_SECOND, BASE_REGEN, player ? ServerConfig.REGEN_FEATHERS_PER_SECOND.get()
                : FeathersServiceImpl.mountTuning(entity).regenPerSecond());
        FeathersServiceImpl.ensureInitialized(entity, data);
        FeathersServiceImpl.updateWeight(entity, data);
        data.forceSync = true;
    }

    public static void copyConfigBases(FeathersData from, FeathersData to) {
        copyCounter(from, to, BASE_MAX);
        copyCounter(from, to, BASE_STRAIN);
        copyCounter(from, to, BASE_REGEN);
    }

    private static void copyCounter(FeathersData from, FeathersData to, String key) {
        if (from.counters.containsKey(key)) to.setCounter(key, from.getCounter(key));
    }

    private static final String BASE_MAX = "config_base.max_feathers";
    private static final String BASE_STRAIN = "config_base.max_strain";
    private static final String BASE_REGEN = "config_base.feathers_per_second";

    private static void configBase(LivingEntity entity, FeathersData data, RegistryObject<Attribute> attribute, String key, double value) {
        AttributeInstance instance = entity.getAttribute(attribute.get());
        if (instance == null) return;
        if (data.counters.containsKey(key) && instance.getBaseValue() != data.getCounter(key)) return;
        if (instance.getBaseValue() != value) instance.setBaseValue(value);
        data.setCounter(key, value);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !player.isAlive()) return;
        tick(player);
    }

    public static void tick(LivingEntity entity) {
        FeathersData data = FeathersServiceImpl.data(entity);
        if (!data.initialized && !(entity instanceof Player)) refreshFromConfig(entity);
        FeathersServiceImpl.ensureInitialized(entity, data);

        if (!FeathersServiceImpl.isExempt(entity)) {
            int staminaBefore = data.stamina;

            if (entity.tickCount % ATTRIBUTE_INTERVAL == 0) tickAttributes(entity, data);
            if (entity.tickCount % REGEN_FACTOR_INTERVAL == 0) applyRegenFactors(entity, data);
            if (entity instanceof Player && entity.tickCount % ClimateEffects.INTERVAL == 0) {
                data.climate = ClimateEffects.evaluate(entity);
                ClimateEffects.apply(entity, data.climate);
            }

            data.expireRestBonuses(entity.level().getGameTime());
            updateRest(entity, data);
            FeathersServiceImpl.refreshMaximums(entity, data);
            tickStamina(entity, data, regenPerTick(entity, data));

            data.lastDelta = data.stamina - staminaBefore;
            updateIndicators(entity, data.strain > 0);
        }

        syncIfChanged(entity, data);
    }

    private static void tickAttributes(LivingEntity entity, FeathersData data) {
        AttributeInstance multiplier = data.weightMultiplierAttribute(entity);
        double value = multiplier != null ? multiplier.getValue() : 1.0;
        if (value != data.lastWeightMultiplier) {
            data.lastWeightMultiplier = value;
            FeathersServiceImpl.updateWeight(entity, data);
        }

        if (entity.hasEffect(FeathersMobEffects.ENDURANCE.get()) && data.bonus(EnduranceEffect.BONUS) == null) {
            entity.removeEffect(FeathersMobEffects.ENDURANCE.get());
        }
    }

    private static void applyRegenFactors(LivingEntity entity, FeathersData data) {
        double sum = 0;
        for (Extensions.RegenEntry factor : Extensions.regenFactors()) {
            sum += factor.factor().feathersPerSecond(entity, data);
        }
        toggleModifier(entity, FeathersAttributes.FEATHERS_PER_SECOND.get(), REGEN_FACTORS, REGEN_FACTORS_ID, sum != 0, sum, AttributeModifier.Operation.ADDITION);
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

        if (!ServerConfig.ENABLE_REST.get()) {
            data.restState = RestState.NONE;
            data.restMultiplier = 1.0;
            return;
        }

        boolean moved = dx * dx + dy * dy + dz * dz > STILL_EPSILON_SQR || entity.isSwimming() || entity.isFallFlying();
        data.stillTicks = moved ? 0 : data.stillTicks + 1;

        double multiplier;
        if (entity.isPassenger()) {
            data.restState = RestState.SITTING;
            multiplier = ServerConfig.REST_SITTING_MULTIPLIER.get();
        } else if (data.stillTicks >= ServerConfig.REST_STILL_TICKS.get()) {
            boolean crouching = entity.isCrouching();
            data.restState = crouching ? RestState.CROUCHING : RestState.STILL;
            multiplier = crouching ? ServerConfig.REST_CROUCHING_MULTIPLIER.get() : ServerConfig.REST_STILL_MULTIPLIER.get();
        } else {
            data.restState = RestState.NONE;
            multiplier = 1.0;
        }

        int size = data.restBonuses.size();
        for (int i = 0; i < size; i++) {
            multiplier = Math.max(multiplier, data.restBonuses.get(i).value);
        }
        data.restMultiplier = multiplier;
    }

    private static double regenPerTick(LivingEntity entity, FeathersData data) {
        AttributeInstance attribute = data.regenAttribute(entity);
        return attribute != null ? Stamina.perTick(attribute.getValue()) : 0.0;
    }

    static void tickStamina(LivingEntity entity, FeathersData data, double perTick) {
        boolean strainEnabled = ServerConfig.ENABLE_STRAIN.get();
        boolean strainedBefore = data.strain > 0;
        long now = entity.level().getGameTime();

        data.expire(now);
        tickDrains(entity, data, now, strainEnabled);
        regenerate(entity, data, perTick);

        if (!strainEnabled && data.strain > 0) data.strain = 0;
        if (strainedBefore && data.strain == 0) FeathersServiceImpl.post(data, new StrainEvent.Cleared(entity));

        FeathersServiceImpl.checkExhausted(entity, data, strainEnabled);
        checkRecovered(entity, data);
    }

    private static void tickDrains(LivingEntity entity, FeathersData data, long now, boolean strainEnabled) {
        for (int i = data.drains.size() - 1; i >= 0; i--) {
            FeathersData.Drain drain = data.drains.get(i);

            DrainEvent.Stopped.Reason stop = null;
            if (drain.timeoutTicks > 0 && now - drain.lastRefresh > drain.timeoutTicks) {
                stop = DrainEvent.Stopped.Reason.TIMED_OUT;
            } else if (data.exhausted) {
                stop = DrainEvent.Stopped.Reason.EXHAUSTED;
            } else {
                drain.carry += drain.perTick;
                int base = (int) drain.carry;
                if (base > 0) {
                    drain.carry -= base;
                    int cost = FeathersServiceImpl.effectiveCost(entity, data, drain.source, base);
                    if (data.canPay(cost, drain.allowStrain, strainEnabled)) {
                        FeathersServiceImpl.payAndSettle(entity, data, cost, strainEnabled);
                        if (!data.predicted()) data.logSpend(drain.source, cost, now);
                    } else {
                        stop = DrainEvent.Stopped.Reason.INSUFFICIENT;
                        FeathersServiceImpl.checkExhausted(entity, data, strainEnabled);
                    }
                }
            }

            if (stop != null) {
                data.drains.remove(i);
                FeathersServiceImpl.post(data, new DrainEvent.Stopped(entity, drain.source, stop));
            }
        }
    }

    private static void regenerate(LivingEntity entity, FeathersData data, double perTick) {
        boolean server = !data.predicted();
        boolean restBoosts = ServerConfig.REST_BOOSTS_REGEN.get();
        double factor = server && restBoosts ? data.restMultiplier : 1.0;
        boolean delayed = data.regenDelay > 0;
        if (delayed) data.regenDelay--;

        double applied = perTick;
        if (perTick > 0) {
            boolean held = data.isRegenHeld() || server && isTooHungry(entity);
            if (delayed || held) {
                data.rate = held ? 0.0 : perTick * factor;
                data.regenCarry = 0;
                data.regenPaused = true;
                return;
            }
            if (data.regenPaused && data.stamina < data.maxStamina) {
                if (server && MinecraftForge.EVENT_BUS.post(new RegenEvent(entity))) {
                    data.rate = 0.0;
                    return;
                }
                data.regenPaused = false;
            }
            applied = perTick * factor;
        }
        data.rate = applied;

        data.regenCarry += applied;
        int regen = (int) data.regenCarry;
        data.regenCarry -= regen;

        if (server) {
            for (Extensions.ModifierEntry modifier : Extensions.modifiers()) {
                regen = modifier.modifier().modifyRegen(entity, data, regen);
            }
        }

        if (regen < 0) {
            data.stamina = Math.max(0, data.stamina + regen);
            return;
        }
        if (regen == 0) return;

        int recovered = 0;
        if (data.strain > 0) {
            double rest = server && !restBoosts ? data.restMultiplier : 1.0;
            int recovery = (int) Math.round(regen * rest);
            recovered = Math.min(data.strain, recovery);
            data.strain -= recovered;
            regen = (int) ((recovery - recovered) / rest);
        }

        int gained = Math.min(regen, Math.max(0, data.maxStamina - data.stamina));
        data.stamina += gained;
        data.totalRegenerated += gained + recovered;

        if (server && entity instanceof Player player && ServerConfig.REGEN_USES_HUNGER.get() && gained + recovered > 0) {
            player.causeFoodExhaustion((float) ((gained + recovered) / (double) Stamina.PER_FEATHER * ServerConfig.HUNGER_PER_FEATHER.get()));
        }
    }

    private static boolean isTooHungry(LivingEntity entity) {
        return entity instanceof Player player && ServerConfig.REGEN_USES_HUNGER.get()
                && player.getFoodData().getFoodLevel() <= HungerRegen.HUNGRY_FOOD_LEVEL;
    }

    static void checkRecovered(LivingEntity entity, FeathersData data) {
        if (data.maxStamina <= 0) {
            data.exhausted = false;
            return;
        }
        if (!data.exhausted) return;
        boolean recovered = !ServerConfig.ENABLE_EXHAUSTION.get()
                || data.strain == 0 && data.availableStamina() >= usableMax(data) * ServerConfig.EXHAUSTION_RECOVERY.get();
        if (recovered) {
            data.exhausted = false;
            FeathersServiceImpl.post(data, new ExhaustionEvent.Recovered(entity));
        }
    }

    private static int usableMax(FeathersData data) {
        return Math.max(0, data.maxStamina - data.reserved);
    }

    private static void updateIndicators(LivingEntity entity, boolean strained) {
        if (strained == entity.hasEffect(FeathersMobEffects.STRAINED.get())) return;
        if (strained) {
            entity.addEffect(new MobEffectInstance(FeathersMobEffects.STRAINED.get(), MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        } else {
            entity.removeEffect(FeathersMobEffects.STRAINED.get());
        }
    }

    public static void releaseExMount(LivingEntity entity) {
        FeathersData data = FeathersServiceImpl.dataOrNull(entity);
        if (data == null) return;
        MobEffectInstance effect = entity.getEffect(FeathersMobEffects.STRAINED.get());

        boolean ownEffect = effect != null && effect.isInfiniteDuration() && !effect.isVisible();
        if (!ownEffect && !data.initialized && data.strain == 0 && !data.exhausted && !data.mountSlowed && data.syncedMax <= 0) return;

        if (ownEffect) entity.removeEffect(FeathersMobEffects.STRAINED.get());
        if (data.strain > 0) {
            data.strain = 0;
            MinecraftForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
        }
        if (data.exhausted) {
            data.exhausted = false;
            MinecraftForge.EVENT_BUS.post(new ExhaustionEvent.Recovered(entity));
        }
        data.mountSlowed = false;
        data.drains.clear();

        data.initialized = false;
        if (data.syncedMax > 0 && MountExertion.riderOf(entity) instanceof ServerPlayer rider) FeathersPackets.sendNoFeathers(rider, entity);
        data.syncedMax = -1;
        data.forceSync = true;
    }

    private static void syncIfChanged(LivingEntity entity, FeathersData data) {
        ServerPlayer player = entity instanceof ServerPlayer self ? self
                : MountExertion.riderOf(entity) instanceof ServerPlayer rider ? rider : null;
        if (player == null) return;

        int shownStamina = Stamina.toFeathers(data.stamina);
        int shownStrain = Stamina.toFeathersCeil(data.strain);
        int shownBonus = Stamina.toFeathersCeil(data.bonusStamina());
        boolean delayed = data.regenDelay > 0;

        if (!data.forceSync && shownStamina == data.syncedStamina && data.maxStamina == data.syncedMax
                && shownStrain == data.syncedStrain && data.maxStrain == data.syncedMaxStrain && shownBonus == data.syncedBonus
                && data.reserved == data.syncedReserved && data.exhausted == data.syncedExhausted && delayed == data.syncedDelayed
                && data.rate == data.syncedRate && data.restState == data.syncedRest && data.weightSplit.sameAs(data.syncedWeightSplit)) return;

        data.forceSync = false;
        data.syncedStamina = shownStamina;
        data.syncedMax = data.maxStamina;
        data.syncedStrain = shownStrain;
        data.syncedMaxStrain = data.maxStrain;
        data.syncedBonus = shownBonus;
        data.syncedReserved = data.reserved;
        data.syncedExhausted = data.exhausted;
        data.syncedDelayed = delayed;
        data.syncedRate = data.rate;
        data.syncedRest = data.restState;
        data.syncedWeightSplit.copyFrom(data.weightSplit);
        FeathersPackets.sendSync(player, entity, data);
    }
}
