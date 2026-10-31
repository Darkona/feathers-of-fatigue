package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.index.FeathersAttachmentRegistry;
import com.darkona.feathersoffatigue.network.SyncPayload;
import dev.darkona.jdlib.api.attribute.JDModifiers;
import dev.darkona.jdlib.api.stat.JDResources;
import dev.darkona.jdlib.core.registry.PriorityList;
import com.darkona.feathersoffatigue.Feathers;
import com.darkona.feathersoffatigue.api.*;
import com.darkona.feathersoffatigue.api.event.DrainEvent;
import com.darkona.feathersoffatigue.api.event.GainEvent;
import com.darkona.feathersoffatigue.api.event.SpendEvent;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersDataMaps;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.spi.FeathersService;
import com.darkona.feathersoffatigue.config.ServerConfig;
import com.darkona.feathersoffatigue.content.mount.MountTuning;
import com.darkona.feathersoffatigue.network.FeathersPackets;
import com.darkona.feathersoffatigue.content.weight.ArmorWeights;
import dev.darkona.jdlib.core.stat.Resource;
import dev.darkona.jdlib.core.stat.SourcedPools;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;


public final class FeathersServiceImpl implements FeathersService {

    public static final FeathersServiceImpl INSTANCE = new FeathersServiceImpl();

    public interface ClientBridge {
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        boolean isLocalPlayer(LivingEntity entity);

        @Nullable
        FeathersView clientView(LivingEntity entity);

        FeathersView localView();

        void accept(SyncPayload payload);
    }

    private static volatile ClientBridge clientBridge;

    private static final ResourceLocation ARMOR = FeathersIds.id("armor");

    private FeathersServiceImpl() {}

    public static void setClientBridge(ClientBridge bridge) {
        clientBridge = bridge;
    }

    public static void acceptOnClient(SyncPayload payload) {
        ClientBridge bridge = clientBridge;
        if (bridge != null) bridge.accept(payload);
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
        if (id < 0 || id >= verdicts.length) return computeMount(entity);
        byte verdict = verdicts[id];
        if (verdict == UNKNOWN_TYPE) {
            verdict = computeMount(entity) ? MOUNT_TYPE : NOT_MOUNT_TYPE;
            verdicts[id] = verdict;
        }
        return verdict == MOUNT_TYPE;
    }

    private static final byte UNKNOWN_TYPE = 0;
    private static final byte MOUNT_TYPE = 1;
    private static final byte NOT_MOUNT_TYPE = 2;
    private static volatile byte[] mountTypes = new byte[0];

    private static boolean computeMount(LivingEntity entity) {
        if (entity.getType().is(FeathersIds.NO_FEATHERS)) return false;
        boolean mount = entity instanceof AbstractHorse || entity.getType().is(FeathersIds.MOUNTS)
                || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType())
                .getData(FeathersDataMaps.MOUNT_STATS) != null;
        return mount && entity.getAttribute(FeathersAttributes.MAX_FEATHERS) != null;
    }

    public static void invalidateMountTypes() {
        mountTypes = new byte[BuiltInRegistries.ENTITY_TYPE.size()];
        mountTunings = new MountTuning[0];
    }

    public static MountStats mountStats(LivingEntity entity) {
        MountStats stats = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType())
                .getData(FeathersDataMaps.MOUNT_STATS);
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
        return entity.getData(FeathersAttachmentRegistry.FEATHERS);
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
        Resource<ResourceLocation> r = JDResources.of(entity, FeathersAttachmentRegistry.STAMINA);
        data.resource = r;
        updateWeight(entity, data);
        if (data.fresh) {
            r.refill(entity, Resource.Hooks.none());
            data.fresh = false;
        }
        data.shown.resendNext();
    }

    static void updateWeight(LivingEntity entity, FeathersData data) {
        int weight = ArmorWeights.totalWeight(entity, data.weightSplit);
        JDModifiers.toggle(entity, FeathersAttributes.ARMOR_WEIGHT, ARMOR, weight != 0, weight, AttributeModifier.Operation.ADD_VALUE);
        reshape(entity, data);
    }

    static void reshape(LivingEntity entity, FeathersData data) {
        Resource<ResourceLocation> r = data.resource;
        if (r != null) FeathersAttachmentRegistry.STAMINA.shape(entity, r);
    }

    public static @Nullable Resource<ResourceLocation> stamina(LivingEntity entity) {
        if (onClient(entity)) {
            return JDResources.holds(entity, FeathersAttachmentRegistry.STAMINA) ? JDResources.of(entity, FeathersAttachmentRegistry.STAMINA) : null;
        }
        FeathersData data = data(entity);
        ensureInitialized(entity, data);
        return data.resource;
    }

    private static Resource.Hooks<ResourceLocation, LivingEntity> hooks(LivingEntity entity) {
        return onClient(entity) ? Resource.Hooks.none() : StaminaHooks.INSTANCE;
    }

    public static SpendResult result(Resource.Spent spent) {
        return switch (spent) {
            case OK -> SpendResult.OK;
            case EXHAUSTED -> SpendResult.EXHAUSTED;
            case INSUFFICIENT -> SpendResult.INSUFFICIENT;
        };
    }

    static int effectiveCost(LivingEntity entity, FeathersData data, ResourceLocation source, double baseCost) {
        return effectiveCost(entity, data, data.usageAttribute(entity), source, baseCost);
    }

    public static int effectiveCost(LivingEntity entity, FeathersView feathers, @Nullable AttributeInstance usage,
                                    ResourceLocation source, double baseCost) {
        int cost = (int) Math.min(Integer.MAX_VALUE, Math.round(baseCost * (usage != null ? usage.getValue() : 1.0)));
        PriorityList.Snapshot<ResourceLocation, StaminaModifier> modifiers = Extensions.modifiers();
        for (int i = 0; i < modifiers.size(); i++) {
            cost = modifiers.value(i).modifyCost(entity, feathers, source, cost);
        }
        return Math.max(0, cost);
    }

    private static int cost(LivingEntity entity, ResourceLocation source, double baseCost) {
        if (onClient(entity)) {
            ClientBridge bridge = clientBridge;
            FeathersView view = bridge != null ? bridge.localView() : FeathersView.NONE;
            return effectiveCost(entity, view, entity.getAttribute(FeathersAttributes.USAGE_MULTIPLIER), source, baseCost);
        }
        return effectiveCost(entity, data(entity), source, baseCost);
    }

    private static boolean predictable(LivingEntity entity) {
        if (!onClient(entity)) return true;
        ClientBridge bridge = clientBridge;
        return bridge != null && bridge.isLocalPlayer(entity);
    }

    @Override
    public SpendResult spend(LivingEntity entity, ResourceLocation source, int stamina, SpendOptions options) {
        if (!supports(entity) || isExempt(entity) || !predictable(entity)) return SpendResult.EXEMPT;
        Resource<ResourceLocation> r = stamina(entity);
        if (r == null) return SpendResult.EXEMPT;
        boolean client = onClient(entity);

        if (r.exhausted() && !options.ignoreExhaustion()) return SpendResult.EXHAUSTED;

        if (stamina <= 0) {
            if (!options.simulate()) applyRegenDelay(r, options);
            return SpendResult.OK;
        }

        int cost = cost(entity, source, stamina);
        if (options.simulate()) return result(r.check(cost, options.allowStrain(), true));

        if (!client) {
            SpendEvent.Pre pre = NeoForge.EVENT_BUS.post(new SpendEvent.Pre(entity, source, cost, options));
            if (pre.isCanceled()) return SpendResult.CANCELLED;
            cost = pre.getCost();
        }

        Resource.Spent spent = r.spend(entity, cost, options.allowStrain(), true, hooks(entity));
        if (spent != Resource.Spent.OK) return result(spent);
        applyRegenDelay(r, options);
        if (client) return SpendResult.OK;

        data(entity).logSpend(source, cost, entity.level().getGameTime());
        NeoForge.EVENT_BUS.post(new SpendEvent.Post(entity, source, cost, SpendResult.OK));
        if (ServerConfig.DEBUG_MODE.get()) {
            Feathers.LOGGER.info("{} spent {} stamina on {}", entity.getName().getString(), cost, source);
            FeathersPackets.sendSpendDebug(entity, source, cost);
        }
        return SpendResult.OK;
    }

    private static void applyRegenDelay(Resource<ResourceLocation> r, SpendOptions options) {
        r.regenDelay(regenDelayAfter(r.regenDelay(), options));
    }

    public static int regenDelayAfter(int current, SpendOptions options) {
        int delay = options.regenDelayTicks() < 0 ? ServerConfig.DEFAULT_USAGE_COOLDOWN.get() : options.regenDelayTicks();
        return (int) Math.min((long) current + delay, ServerConfig.MAX_COOLDOWN.get() * 20L);
    }

    @Override
    public SpendResult startDrain(LivingEntity entity, ResourceLocation source, double staminaPerTick, DrainOptions options) {
        if (!supports(entity) || isExempt(entity) || !predictable(entity)) return SpendResult.EXEMPT;
        Resource<ResourceLocation> r = stamina(entity);
        if (r == null) return SpendResult.EXEMPT;
        boolean client = onClient(entity);

        boolean started = client || !r.drains().has(source);
        if (started) {
            Resource.Spent first = r.check(cost(entity, source, Math.ceil(staminaPerTick)), options.allowStrain(), false);
            if (first != Resource.Spent.OK) return result(first);
        }
        r.drain(source, Resource.perTick(staminaPerTick), options.timeoutTicks(), options.allowStrain(), options.blocksRegen(),
                entity.level().getGameTime());
        if (!client && started) NeoForge.EVENT_BUS.post(new DrainEvent.Started(entity, source, staminaPerTick));
        return SpendResult.OK;
    }

    @Override
    public void stopDrain(LivingEntity entity, ResourceLocation source) {
        if (!supports(entity) || !predictable(entity)) return;
        Resource<ResourceLocation> r = stamina(entity);
        if (r == null || !r.stopDrain(source) || onClient(entity)) return;
        NeoForge.EVENT_BUS.post(new DrainEvent.Stopped(entity, source, DrainEvent.Stopped.Reason.STOPPED));
    }

    @Override
    public boolean isDraining(LivingEntity entity, ResourceLocation source) {
        if (!supports(entity) || onClient(entity)) return false;
        Resource<ResourceLocation> r = stamina(entity);
        return r != null && r.drains().has(source);
    }

    private @Nullable Resource<ResourceLocation> serverStamina(LivingEntity entity) {
        return supports(entity) && !onClient(entity) ? stamina(entity) : null;
    }

    @Override
    public int gain(LivingEntity entity, ResourceLocation source, int stamina) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r == null || stamina <= 0) return 0;
        GainEvent event = NeoForge.EVENT_BUS.post(new GainEvent(entity, source, stamina));
        if (event.isCanceled()) return 0;
        return r.receive(entity, event.getAmount(), false, StaminaHooks.INSTANCE);
    }

    @Override
    public void setStamina(LivingEntity entity, int stamina) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r != null) r.set(entity, stamina, StaminaHooks.INSTANCE);
    }

    @Override
    public void reset(LivingEntity entity) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r != null) r.refill(entity, StaminaHooks.INSTANCE);
    }

    private static long until(LivingEntity entity, int ticks) {
        return ticks < 0 ? SourcedPools.FOREVER : entity.level().getGameTime() + ticks;
    }

    @Override
    public void addBonusStamina(LivingEntity entity, ResourceLocation source, int stamina, int ticks) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r == null) return;
        if (stamina <= 0) r.bonuses().remove(source);
        else r.bonus(source, stamina, until(entity, ticks));
        r.checkRecovered(entity, StaminaHooks.INSTANCE);
    }

    @Override
    public void removeBonusStamina(LivingEntity entity, ResourceLocation source) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r != null) r.bonuses().remove(source);
    }

    @Override
    public void blockRegen(LivingEntity entity, ResourceLocation source, int ticks) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r != null) r.blockRegen(source, until(entity, ticks));
    }

    @Override
    public void unblockRegen(LivingEntity entity, ResourceLocation source) {
        Resource<ResourceLocation> r = serverStamina(entity);
        if (r != null) r.regenBlocks().remove(source);
    }

    @Override
    public void setRestBonus(LivingEntity entity, ResourceLocation source, double multiplier, int ticks) {
        if (supports(entity) && !onClient(entity)) data(entity).restBonuses.set(source, multiplier, until(entity, ticks));
    }

    @Override
    public void removeRestBonus(LivingEntity entity, ResourceLocation source) {
        if (supports(entity) && !onClient(entity)) data(entity).restBonuses.remove(source);
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
    public void sync(LivingEntity entity) {
        if (!supports(entity) || onClient(entity)) return;
        data(entity).shown.resendNext();
        if (JDResources.holds(entity, FeathersAttachmentRegistry.STAMINA)) entity.syncData(JDResources.attachment(FeathersAttachmentRegistry.STAMINA));
    }
}
