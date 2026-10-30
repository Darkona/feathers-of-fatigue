package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import com.darkona.feathersoffatigue.api.RegenFactor;
import dev.darkona.jdlib.core.registry.PriorityList;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.event.ExhaustionEvent;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.mount.MountExertion;
import com.darkona.feathersoffatigue.content.mount.MountTraits;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import dev.darkona.jdlib.api.attribute.JDModifiers;
import dev.darkona.jdlib.api.stat.JDResources;
import dev.darkona.jdlib.core.stat.Resource;
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
            event.add(type, FeathersAttributes.ARMOR_WEIGHT);
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

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        FeathersData data = FeathersServiceImpl.data(event.getEntity());
        if (event.isWasDeath() && event.getOriginal().hasData(FeathersAttachmentRegistry.FEATHERS)) {
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
        if (player.level().isClientSide() || !ServerConfig.SLEEPING_ALWAYS_RESTORES_FEATHERS.get()) return;
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
        if (player) configBase(entity, data, FeathersAttributes.MAX_FEATHERS, BASE_MAX, ServerConfig.MAX_FEATHERS.get());
        else MountTraits.ensureRolled(entity);
        configBase(entity, data, FeathersAttributes.MAX_STRAIN, BASE_STRAIN, ServerConfig.MAX_STRAIN.get());
        configBase(entity, data, FeathersAttributes.FEATHERS_PER_SECOND, BASE_REGEN, player ? ServerConfig.REGEN_FEATHERS_PER_SECOND.get()
                : FeathersServiceImpl.mountTuning(entity).regenPerSecond());
        FeathersServiceImpl.ensureInitialized(entity, data);
        FeathersServiceImpl.updateWeight(entity, data);
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
        Resource<ResourceLocation> r = data.resource;

        if (r != null && !FeathersServiceImpl.isExempt(entity)) {
            int staminaBefore = r.stored();

            if (entity.tickCount % ATTRIBUTE_INTERVAL == 0) tickAttributes(entity, data, r);
            if (entity.tickCount % REGEN_FACTOR_INTERVAL == 0) applyRegenFactors(entity, data);
            if (entity instanceof Player && entity.tickCount % ClimateEffects.INTERVAL == 0) {
                data.climate = ClimateEffects.evaluate(entity);
                ClimateEffects.apply(entity, data.climate);
            }

            data.restBonuses.expire(entity.level().getGameTime());
            updateRest(entity, data);
            JDResources.tick(entity, FeathersAttachmentRegistry.STAMINA, entity.level());

            data.lastDelta = r.stored() - staminaBefore;
            updateIndicators(entity, r.overdraw() > 0);
        } else {
            JDResources.syncIfChanged(entity, FeathersAttachmentRegistry.STAMINA);
        }

        syncIfChanged(entity, data);
    }

    private static void tickAttributes(LivingEntity entity, FeathersData data, Resource<ResourceLocation> r) {
        AttributeInstance multiplier = data.weightMultiplierAttribute(entity);
        double value = multiplier != null ? multiplier.getValue() : 1.0;
        if (value != data.lastWeightMultiplier) {
            data.lastWeightMultiplier = value;
            FeathersServiceImpl.updateWeight(entity, data);
        }

        if (entity.hasEffect(FeathersMobEffects.ENDURANCE) && !r.bonuses().has(EnduranceEffect.BONUS)) {
            entity.removeEffect(FeathersMobEffects.ENDURANCE);
        }
    }

    private static void applyRegenFactors(LivingEntity entity, FeathersData data) {
        PriorityList.Snapshot<ResourceLocation, RegenFactor> factors = Extensions.regenFactors();
        double sum = 0;
        for (int i = 0; i < factors.size(); i++) {
            sum += factors.value(i).feathersPerSecond(entity, data);
        }
        JDModifiers.toggle(entity, FeathersAttributes.FEATHERS_PER_SECOND, REGEN_FACTORS, sum != 0, sum, AttributeModifier.Operation.ADD_VALUE);
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

        data.restMultiplier = data.restBonuses.max(multiplier);
    }

    private static void updateIndicators(LivingEntity entity, boolean strained) {
        if (strained == entity.hasEffect(FeathersMobEffects.STRAINED)) return;
        if (strained) {
            entity.addEffect(new MobEffectInstance(FeathersMobEffects.STRAINED, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        } else {
            entity.removeEffect(FeathersMobEffects.STRAINED);
        }
    }

    public static void releaseExMount(LivingEntity entity) {
        if (!entity.hasData(FeathersAttachmentRegistry.FEATHERS)) return;
        FeathersData data = FeathersServiceImpl.data(entity);
        boolean held = JDResources.holds(entity, FeathersAttachmentRegistry.STAMINA);
        MobEffectInstance effect = entity.getEffect(FeathersMobEffects.STRAINED);
        boolean ownEffect = effect != null && effect.isInfiniteDuration() && !effect.isVisible();
        if (!ownEffect && !data.initialized && !held && !data.mountSlowed) return;

        if (ownEffect) entity.removeEffect(FeathersMobEffects.STRAINED);
        if (held) {
            Resource<ResourceLocation> r = JDResources.of(entity, FeathersAttachmentRegistry.STAMINA);
            if (r.overdraw() > 0) NeoForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
            if (r.exhausted()) NeoForge.EVENT_BUS.post(new ExhaustionEvent.Recovered(entity));
            entity.removeData(JDResources.attachment(FeathersAttachmentRegistry.STAMINA));
        }
        data.mountSlowed = false;
        data.resource = null;
        data.initialized = false;
        data.shown.forget();
    }

    private static void syncIfChanged(LivingEntity entity, FeathersData data) {
        ServerPlayer player = entity instanceof ServerPlayer self ? self
                : MountExertion.riderOf(entity) instanceof ServerPlayer rider ? rider : null;
        if (player == null) return;

        data.shown.show(FeathersData.Shown.REST, data.restState.ordinal());
        boolean splitChanged = !data.weightSplit.sameAs(data.syncedWeightSplit);
        if (!data.shown.shouldSend() && !splitChanged) return;
        data.syncedWeightSplit.copyFrom(data.weightSplit);
        FeathersPackets.sendSync(player, entity, data);
    }
}
