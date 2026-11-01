package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.ClimateProvider;
import com.darkona.feathersoffatigue.api.DrainOptions;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.MountStats;
import com.darkona.feathersoffatigue.api.RegenFactor;
import com.darkona.feathersoffatigue.api.SpendOptions;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.StaminaModifier;
import com.darkona.feathersoffatigue.api.WeightSource;
import com.darkona.feathersoffatigue.api.event.DrainEvent;
import com.darkona.feathersoffatigue.api.event.ExhaustionEvent;
import com.darkona.feathersoffatigue.api.event.GainEvent;
import com.darkona.feathersoffatigue.api.event.SpendEvent;
import com.darkona.feathersoffatigue.api.event.StrainEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.spi.FeathersService;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.index.FeathersDataMapRegistry;
import com.darkona.feathersoffatigue.content.mount.MountTuning;
import com.darkona.feathersoffatigue.network.DataMapsPayload;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.network.SpendDebugPayload;
import com.darkona.feathersoffatigue.network.SyncPayload;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

public final class FeathersServiceImpl implements FeathersService {
    public static final FeathersServiceImpl INSTANCE = new FeathersServiceImpl();

    public interface ClientBridge {
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean isLocalPlayer(LivingEntity entity);

        @Nullable
        FeathersData clientView(LivingEntity entity);

        void accept(SyncPayload payload);

        void acceptDebug(SpendDebugPayload payload);

        void acceptDataMaps(DataMapsPayload payload);
    }

    private static volatile ClientBridge clientBridge;

    private static final ResourceLocation ARMOR = FeathersIds.id("armor");
    private static final UUID ARMOR_ID = FeathersTicker.modifierId(ARMOR);

    private FeathersServiceImpl() {}

    public static void setClientBridge(ClientBridge bridge) {
        clientBridge = bridge;
    }

    public static void acceptOnClient(SyncPayload payload) {
        ClientBridge bridge = clientBridge;
        if (bridge != null) bridge.accept(payload);
    }

    public static void acceptDebugOnClient(SpendDebugPayload payload) {
        ClientBridge bridge = clientBridge;
        if (bridge != null) bridge.acceptDebug(payload);
    }

    public static void acceptDataMapsOnClient(DataMapsPayload payload) {
        ClientBridge bridge = clientBridge;
        if (bridge != null) bridge.acceptDataMaps(payload);
    }

    @Override
    public boolean supports(LivingEntity entity) {
        return entity instanceof Player || isMount(entity);
    }

    public static boolean isMount(LivingEntity entity) {
        if (entity instanceof Player || !ServerConfig.ENABLE_MOUNTS.get()) return false;

        int id = BuiltInRegistries.ENTITY_TYPE.getId(entity.getType());
        byte[] verdicts = mountTypes;
        if (verdicts.length == 0) {
            invalidateMountTypes();
            verdicts = mountTypes;
        }
        if (id < 0 || id >= verdicts.length) return computeMount(entity) && dataOrNull(entity) != null;
        byte verdict = verdicts[id];
        if (verdict == UNKNOWN_TYPE) {
            verdict = computeMount(entity) ? MOUNT_TYPE : NOT_MOUNT_TYPE;
            verdicts[id] = verdict;
        }

        return verdict == MOUNT_TYPE && dataOrNull(entity) != null;
    }

    public static boolean canHaveFeathers(LivingEntity entity) {
        if (entity instanceof Player) return true;
        EntityType<?> type = entity.getType();
        if (type.is(FeathersIds.NO_FEATHERS)) return false;
        return entity instanceof AbstractHorse || type.is(FeathersIds.MOUNTS) || FeathersDataMapRegistry.mountStats(type) != null;
    }

    private static final byte UNKNOWN_TYPE = 0;
    private static final byte MOUNT_TYPE = 1;
    private static final byte NOT_MOUNT_TYPE = 2;
    private static volatile byte[] mountTypes = new byte[0];

    private static boolean computeMount(LivingEntity entity) {
        if (entity.getType().is(FeathersIds.NO_FEATHERS)) return false;
        boolean mount = entity instanceof AbstractHorse || entity.getType().is(FeathersIds.MOUNTS)
                || FeathersDataMapRegistry.mountStats(entity.getType()) != null;
        return mount && entity.getAttribute(FeathersAttributes.MAX_FEATHERS.get()) != null;
    }

    public static void invalidateMountTypes() {
        mountTypes = new byte[BuiltInRegistries.ENTITY_TYPE.size()];
        mountTunings = new MountTuning[0];
    }

    public static MountStats mountStats(LivingEntity entity) {
        MountStats stats = FeathersDataMapRegistry.mountStats(entity.getType());
        return stats != null ? stats : MountStats.DEFAULT;
    }

    private static volatile MountTuning[] mountTunings = new MountTuning[0];

    public static MountTuning mountTuning(LivingEntity entity) {
        int id = BuiltInRegistries.ENTITY_TYPE.getId(entity.getType());
        MountTuning[] tunings = mountTunings;
        if (tunings.length == 0) {
            tunings = new MountTuning[BuiltInRegistries.ENTITY_TYPE.size()];
            mountTunings = tunings;
        }
        if (id < 0 || id >= tunings.length) return MountTuning.of(mountStats(entity));
        MountTuning tuning = tunings[id];
        if (tuning == null) {
            tuning = MountTuning.of(mountStats(entity));
            tunings[id] = tuning;
        }
        return tuning;
    }

    public static boolean isExempt(LivingEntity entity) {
        return entity instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    public static FeathersData data(LivingEntity entity) {
        FeathersData data = dataOrNull(entity);
        if (data == null) throw new IllegalStateException(entity + " has no feathers");
        return data;
    }

    public static @Nullable FeathersData dataOrNull(LivingEntity entity) {
        return entity.getCapability(FeathersAttachmentRegistry.FEATHERS).orElse(null);
    }

    private static boolean onClient(LivingEntity entity) {
        return entity.level().isClientSide();
    }

    @Override
    public FeathersView view(LivingEntity entity) {
        if (!supports(entity)) return FeathersView.NONE;
        if (onClient(entity)) {
            ClientBridge bridge = clientBridge;
            FeathersView known = bridge != null ? bridge.clientView(entity) : null;
            return known != null ? known : FeathersView.NONE;
        }
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        return data;
    }

    static void ensureInitialized(LivingEntity entity, FeathersData data) {
        if (data.initialized) return;
        data.initialized = true;
        updateWeight(entity, data);
        if (data.fresh) {
            data.stamina = data.maxStamina;
            data.fresh = false;
        }
        data.forceSync = true;
    }

    static void updateWeight(LivingEntity entity, FeathersData data) {
        int weight = ArmorWeights.totalWeight(entity, data.weightSplit);
        FeathersTicker.toggleModifier(entity, FeathersAttributes.ARMOR_WEIGHT.get(), ARMOR, ARMOR_ID, weight != 0, weight, AttributeModifier.Operation.ADDITION);
        refreshMaximums(entity, data);
    }

    static void refreshMaximums(LivingEntity entity, FeathersData data) {
        AttributeInstance maxFeathers = data.maxFeathersAttribute(entity);
        AttributeInstance maxStrain = data.maxStrainAttribute(entity);
        AttributeInstance armorWeight = data.armorWeightAttribute(entity);
        data.maxStamina = maxFeathers != null ? Stamina.ofFeathers(maxFeathers.getValue()) : 0;
        data.maxStrain = maxStrain != null ? Stamina.ofFeathers(maxStrain.getValue()) : 0;
        data.reserved = armorWeight != null ? Math.min(Stamina.ofFeathers(armorWeight.getValue()), data.maxStamina) : 0;
        if (data.stamina > data.maxStamina) data.stamina = data.maxStamina;
    }

    public static @Nullable FeathersData stamina(LivingEntity entity) {
        if (onClient(entity)) {
            ClientBridge bridge = clientBridge;
            FeathersData known = bridge != null ? bridge.clientView(entity) : null;
            return known != null && known.hasFeathers() ? known : null;
        }
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        return data;
    }

    static void post(FeathersData data, Event event) {
        if (!data.predicted()) MinecraftForge.EVENT_BUS.post(event);
    }

    private static boolean predictable(LivingEntity entity) {
        if (!onClient(entity)) return true;
        ClientBridge bridge = clientBridge;
        return bridge != null && bridge.isLocalPlayer(entity);
    }

    static int effectiveCost(LivingEntity entity, FeathersData data, ResourceLocation source, double baseCost) {
        return effectiveCost(entity, data, data.usageAttribute(entity), source, baseCost);
    }

    public static int effectiveCost(LivingEntity entity, FeathersView feathers, @Nullable AttributeInstance usage,
                                    ResourceLocation source, double baseCost) {
        int cost = (int) Math.min(Integer.MAX_VALUE, Math.round(baseCost * (usage != null ? usage.getValue() : 1.0)));
        for (Extensions.ModifierEntry modifier : Extensions.modifiers()) {
            cost = modifier.modifier().modifyCost(entity, feathers, source, cost);
        }
        return Math.max(0, cost);
    }

    @Override
    public SpendResult spend(LivingEntity entity, ResourceLocation source, int stamina, SpendOptions options) {
        if (!supports(entity) || isExempt(entity) || !predictable(entity)) return SpendResult.EXEMPT;
        FeathersData data = stamina(entity);
        if (data == null) return SpendResult.EXEMPT;
        boolean client = onClient(entity);

        if (data.exhausted && !options.ignoreExhaustion()) return SpendResult.EXHAUSTED;

        if (stamina <= 0) {
            if (!options.simulate()) applyRegenDelay(data, options);
            return SpendResult.OK;
        }

        int cost = effectiveCost(entity, data, source, stamina);
        if (options.simulate()) return data.check(cost, options.allowStrain(), true);

        if (!client) {
            SpendEvent.Pre pre = new SpendEvent.Pre(entity, source, cost, options);
            if (MinecraftForge.EVENT_BUS.post(pre)) return SpendResult.CANCELLED;
            cost = pre.getCost();
        }

        SpendResult spent = data.check(cost, options.allowStrain(), true);
        if (spent != SpendResult.OK) return spent;
        payAndSettle(entity, data, cost, ServerConfig.ENABLE_STRAIN.get());
        applyRegenDelay(data, options);
        if (client) return SpendResult.OK;

        data.logSpend(source, cost, entity.level().getGameTime());
        MinecraftForge.EVENT_BUS.post(new SpendEvent.Post(entity, source, cost, SpendResult.OK));
        if (ServerConfig.DEBUG_MODE.get()) {
            Feathers.LOGGER.info("{} spent {} stamina on {}", entity.getName().getString(), cost, source);
            FeathersPackets.sendSpendDebug(entity, source, cost);
        }
        return SpendResult.OK;
    }

    private static void applyRegenDelay(FeathersData data, SpendOptions options) {
        data.regenDelay = regenDelayAfter(data.regenDelay, options);
    }

    public static int regenDelayAfter(int current, SpendOptions options) {
        int delay = options.regenDelayTicks() < 0 ? ServerConfig.DEFAULT_USAGE_COOLDOWN.get() : options.regenDelayTicks();
        return (int) Math.min((long) current + delay, ServerConfig.MAX_COOLDOWN.get() * 20L);
    }

    static void payAndSettle(LivingEntity entity, FeathersData data, int cost, boolean strainEnabled) {
        boolean wasStrained = data.strain > 0;
        data.pay(cost);
        if (!wasStrained && data.strain > 0) post(data, new StrainEvent.Started(entity));
        checkExhausted(entity, data, strainEnabled);
    }

    static void checkExhausted(LivingEntity entity, FeathersData data, boolean strainEnabled) {
        if (!data.exhausted && data.maxStamina > 0 && ServerConfig.ENABLE_EXHAUSTION.get() && data.isSpent(strainEnabled)) {
            data.exhausted = true;
            post(data, new ExhaustionEvent.Exhausted(entity));
        }
    }

    @Override
    public SpendResult startDrain(LivingEntity entity, ResourceLocation source, double staminaPerTick, DrainOptions options) {
        if (!supports(entity) || isExempt(entity) || !predictable(entity)) return SpendResult.EXEMPT;
        FeathersData data = stamina(entity);
        if (data == null) return SpendResult.EXEMPT;
        boolean client = onClient(entity);

        FeathersData.Drain drain = data.drain(source);
        boolean started = client || drain == null;
        if (started) {
            SpendResult first = data.check(effectiveCost(entity, data, source, Math.ceil(staminaPerTick)), options.allowStrain(), false);
            if (first != SpendResult.OK) return first;
        }
        if (drain == null) {
            drain = new FeathersData.Drain(source);
            data.drains.add(drain);
            if (!client) MinecraftForge.EVENT_BUS.post(new DrainEvent.Started(entity, source, staminaPerTick));
        }
        drain.perTick = Math.max(0, staminaPerTick);
        drain.allowStrain = options.allowStrain();
        drain.blocksRegen = options.blocksRegen();
        drain.timeoutTicks = options.timeoutTicks();
        drain.lastRefresh = entity.level().getGameTime();
        return SpendResult.OK;
    }

    @Override
    public void stopDrain(LivingEntity entity, ResourceLocation source) {
        if (!supports(entity) || !predictable(entity)) return;
        FeathersData data = stamina(entity);
        if (data == null) return;
        int i = FeathersData.indexOf(data.drains, source);
        if (i < 0) return;
        data.drains.remove(i);
        post(data, new DrainEvent.Stopped(entity, source, DrainEvent.Stopped.Reason.STOPPED));
    }

    @Override
    public boolean isDraining(LivingEntity entity, ResourceLocation source) {
        return supports(entity) && !onClient(entity) && data(entity).drain(source) != null;
    }

    @Override
    public int gain(LivingEntity entity, ResourceLocation source, int stamina) {
        if (!supports(entity) || onClient(entity) || stamina <= 0) return 0;
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        GainEvent event = new GainEvent(entity, source, stamina);
        if (MinecraftForge.EVENT_BUS.post(event)) return 0;

        int recovered = Math.min(data.strain, event.getAmount());
        data.strain -= recovered;
        int gained = Math.max(0, Math.min(data.maxStamina - data.stamina, event.getAmount() - recovered));
        data.stamina += gained;
        if (recovered > 0 && data.strain == 0) MinecraftForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
        FeathersTicker.checkRecovered(entity, data);
        return recovered + gained;
    }

    @Override
    public void setStamina(LivingEntity entity, int stamina) {
        if (!supports(entity) || onClient(entity)) return;
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        data.stamina = Mth.clamp(stamina, 0, data.maxStamina);

        if (data.stamina > 0 && data.strain > 0) {
            data.strain = 0;
            MinecraftForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
        }
        FeathersTicker.checkRecovered(entity, data);
    }

    @Override
    public void reset(LivingEntity entity) {
        if (!supports(entity) || onClient(entity)) return;
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        boolean wasStrained = data.strain > 0;
        data.stamina = data.maxStamina;
        data.strain = 0;
        data.regenDelay = 0;
        if (wasStrained) MinecraftForge.EVENT_BUS.post(new StrainEvent.Cleared(entity));
        if (data.exhausted) {
            data.exhausted = false;
            MinecraftForge.EVENT_BUS.post(new ExhaustionEvent.Recovered(entity));
        }
    }

    private static long until(LivingEntity entity, int ticks) {
        return ticks < 0 ? FeathersData.FOREVER : entity.level().getGameTime() + ticks;
    }

    @Override
    public void addBonusStamina(LivingEntity entity, ResourceLocation source, int stamina, int ticks) {
        if (!supports(entity) || onClient(entity)) return;
        FeathersData data = data(entity);
        if (stamina <= 0) data.removeBonus(source);
        else data.setBonus(source, stamina, until(entity, ticks));
        FeathersTicker.checkRecovered(entity, data);
    }

    @Override
    public void removeBonusStamina(LivingEntity entity, ResourceLocation source) {
        if (supports(entity) && !onClient(entity)) data(entity).removeBonus(source);
    }

    @Override
    public void blockRegen(LivingEntity entity, ResourceLocation source, int ticks) {
        if (supports(entity) && !onClient(entity)) FeathersData.setTimed(data(entity).regenBlocks, source, 0, until(entity, ticks));
    }

    @Override
    public void unblockRegen(LivingEntity entity, ResourceLocation source) {
        if (supports(entity) && !onClient(entity)) FeathersData.removeTimed(data(entity).regenBlocks, source);
    }

    @Override
    public void setRestBonus(LivingEntity entity, ResourceLocation source, double multiplier, int ticks) {
        if (supports(entity) && !onClient(entity)) FeathersData.setTimed(data(entity).restBonuses, source, multiplier, until(entity, ticks));
    }

    @Override
    public void removeRestBonus(LivingEntity entity, ResourceLocation source) {
        if (supports(entity) && !onClient(entity)) FeathersData.removeTimed(data(entity).restBonuses, source);
    }

    @Override
    public Climate getClimate(LivingEntity entity) {
        return supports(entity) && !onClient(entity) ? data(entity).climate : Climate.NEUTRAL;
    }

    @Override
    public int getArmorWeight(LivingEntity entity) {
        return view(entity).weight();
    }

    @Override
    public double getPieceWeight(ItemStack stack) {
        return ArmorWeights.pieceWeight(stack);
    }

    @Override
    public void recalculateWeight(LivingEntity entity) {
        if (!supports(entity) || onClient(entity)) return;
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        updateWeight(entity, data);
    }

    @Override
    public void registerClimateProvider(ResourceLocation id, int priority, ClimateProvider provider) {
        Extensions.addClimate(id, priority, provider);
    }

    @Override
    public void registerRegenFactor(ResourceLocation id, RegenFactor factor) {
        Extensions.addRegenFactor(id, factor);
    }

    @Override
    public void registerWeightSource(ResourceLocation id, WeightSource source) {
        Extensions.addWeightSource(id, source);
    }

    @Override
    public void registerStaminaModifier(ResourceLocation id, int ordinal, StaminaModifier modifier) {
        Extensions.addModifier(id, ordinal, modifier);
    }

    @Override
    public @Nullable Integer dataMapArmorWeight(Item item) {
        int weight = FeathersDataMapRegistry.armorWeight(item);
        return weight >= 0 ? weight : null;
    }

    @Override
    public @Nullable MountStats dataMapMountStats(EntityType<?> type) {
        return FeathersDataMapRegistry.mountStats(type);
    }

    @Override
    public void sync(LivingEntity entity) {
        if (supports(entity) && !onClient(entity)) data(entity).forceSync = true;
    }
}
