package com.darkona.feathersoffatigue.api;

import com.darkona.feathersoffatigue.api.client.ClientFeathers;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.api.spi.FeathersService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feathers of Fatigue: a stamina bar of feathers that any mod can spend.
 * <p>
 * <b>Sides.</b> The server changes stamina and synchronizes it with the owning player's client. On the client, read
 * the local player through {@link ClientFeathers}.
 * <p>
 * <b>Units.</b> Amounts are stamina, a thousandth of a feather (see {@link Stamina}). Costs therefore can be
 * fractions of a feather.
 * <p>
 * <b>Entities.</b> Methods accept any {@link LivingEntity}. Players and configured mounts can have feathers. Other
 * entities return {@code false} from {@link #hasFeathers}, and their views report zero.
 * <p>
 * <b>Sources.</b> Every spend, drain, bonus and block names its source with a {@link ResourceLocation} of your mod,
 * e.g. {@code mymod:dash}. Using the same source again replaces or refreshes your own entry and never touches
 * another mod's.
 * <p>
 * <b>Soft dependency.</b> Compile against {@code feathers-of-fatigue-api} only and guard calls with
 * {@code ModList.get().isLoaded("feathers_of_fatigue")}.
 */
public final class FeathersAPI {

    /** The current compatibility version. This value changes when a release introduces incompatible API changes. */
    public static final int API_VERSION = 2;

    private static FeathersService service;

    /** Mods that charge player actions themselves: see {@link #takeOverPlayerActions}. */
    private static final Set<String> playerActionOwners = ConcurrentHashMap.newKeySet();
    private static volatile boolean playerActionsTakenOver;

    private FeathersAPI() {}

    private static FeathersService service() {
        return Objects.requireNonNull(service, "Feathers of Fatigue is not loaded");
    }

    /**
     * Checks whether an entity uses the stamina system. Use this before showing controls or other integration UI.
     *
     * @param entity the entity to check
     * @return {@code true} for supported players and mounts
     */
    public static boolean hasFeathers(LivingEntity entity) {
        return service().supports(entity);
    }

    /**
     * Gets a live, read-only view of an entity's stamina. The method does not create a copy.
     *
     * @param entity the entity whose stamina to read
     * @return the live view, or {@link FeathersView#NONE} for an unsupported entity
     */
    public static FeathersView get(LivingEntity entity) {
        return service().view(entity);
    }

    /**
     * Spends stamina once, all or nothing. Use this for discrete actions such as jumps or attacks.
     * Bonus stamina goes first, then regular stamina, then strain if the
     * options and the server allow it. The usage multiplier attribute and stamina modifiers apply first.
     *
     * @param entity  the entity that performs the action
     * @param source  a stable identifier for the action
     * @param stamina the base cost in stamina units
     * @param options rules for this spend
     * @return the outcome after modifiers, events, and available stamina are considered
     */
    public static SpendResult spend(LivingEntity entity, ResourceLocation source, int stamina, SpendOptions options) {
        return service().spend(entity, source, stamina, options);
    }

    /**
     * Spends stamina once with {@link SpendOptions#DEFAULT default options}.
     *
     * @param entity  the entity that performs the action
     * @param source  a stable identifier for the action
     * @param stamina the base cost in stamina units
     * @return the outcome of the spend
     */
    public static SpendResult spend(LivingEntity entity, ResourceLocation source, int stamina) {
        return spend(entity, source, stamina, SpendOptions.DEFAULT);
    }

    /**
     * Spends a whole number of feathers with default options.
     *
     * @param entity   the entity that performs the action
     * @param source   a stable identifier for the action
     * @param feathers the base cost in feathers
     * @return the outcome of the spend
     */
    public static SpendResult spendFeathers(LivingEntity entity, ResourceLocation source, int feathers) {
        return spend(entity, source, Stamina.ofFeathers(feathers), SpendOptions.DEFAULT);
    }

    /**
     * Checks whether a spend can succeed without changing state or firing spend events.
     *
     * @param entity  the entity that would perform the action
     * @param source  a stable identifier for the action
     * @param stamina the base cost in stamina units
     * @param options rules for the simulated spend
     * @return {@code true} when the action is allowed
     */
    public static boolean canSpend(LivingEntity entity, ResourceLocation source, int stamina, SpendOptions options) {
        return spend(entity, source, stamina, options.simulated()).allowed();
    }

    /**
     * Checks a spend with default options without changing state or firing spend events.
     *
     * @param entity  the entity that would perform the action
     * @param source  a stable identifier for the action
     * @param stamina the base cost in stamina units
     * @return {@code true} when the action is allowed
     */
    public static boolean canSpend(LivingEntity entity, ResourceLocation source, int stamina) {
        return canSpend(entity, source, stamina, SpendOptions.DEFAULT);
    }

    /**
     * Starts or refreshes a continuous drain, e.g. while sprinting or gliding. Costs go through the same rules as
     * {@link #spend}. Fractions carry over between ticks. With a timeout, call this method each tick while the
     * activity lasts. Without a timeout, stop the drain with {@link #stopDrain}.
     *
     * @param entity         the entity whose stamina to drain
     * @param source         a stable identifier for the activity
     * @param staminaPerTick the base cost per tick in stamina units
     * @param options        rules for the drain
     * @return {@code OK} when the drain runs (started now or refreshed), {@code EXEMPT} when the entity pays nothing, or
     *         {@code EXHAUSTED}/{@code INSUFFICIENT} when a new drain can't afford its first tick, in which case it isn't
     *         started. On the client, a prediction for the local player. Each tick's cost is paid later, on the server tick
     */
    public static SpendResult startDrain(LivingEntity entity, ResourceLocation source, double staminaPerTick, DrainOptions options) {
        return service().startDrain(entity, source, staminaPerTick, options);
    }

    /**
     * Starts or refreshes a continuous drain with {@link DrainOptions#DEFAULT default options}.
     *
     * @param entity         the entity whose stamina to drain
     * @param source         a stable identifier for the activity
     * @param staminaPerTick the base cost per tick in stamina units
     * @return the result of the current tick's payment
     */
    public static SpendResult startDrain(LivingEntity entity, ResourceLocation source, double staminaPerTick) {
        return startDrain(entity, source, staminaPerTick, DrainOptions.DEFAULT);
    }

    /**
     * Stops the continuous drain for one source. Use this when an activity ends before its timeout.
     *
     * @param entity the entity whose drain to stop
     * @param source the identifier used to start the drain
     */
    public static void stopDrain(LivingEntity entity, ResourceLocation source) {
        service().stopDrain(entity, source);
    }

    /**
     * Checks whether one source currently drains an entity's stamina.
     *
     * @param entity the entity to check
     * @param source the drain identifier
     * @return {@code true} while that drain is active
     */
    public static boolean isDraining(LivingEntity entity, ResourceLocation source) {
        return service().isDraining(entity, source);
    }

    /**
     * Gives stamina: pays strain back first, as regeneration does, then fills the bar up to the maximum.
     *
     * @param entity  the entity that receives stamina
     * @param source  a stable identifier for the reason
     * @param stamina the amount to offer in stamina units
     * @return the stamina actually used: strain paid back plus stamina gained
     */
    public static int gain(LivingEntity entity, ResourceLocation source, int stamina) {
        return service().gain(entity, source, stamina);
    }

    /**
     * Temporary stamina spent before regular stamina, like the Endurance effect. Replaces any bonus of the same
     * source. Saved with the entity until it runs out or expires.
     *
     * @param entity  the entity that receives the bonus
     * @param source  a stable identifier for the bonus
     * @param stamina the bonus amount in stamina units
     * @param ticks the duration in ticks. A negative value lasts until spent or removed
     */
    public static void addBonusStamina(LivingEntity entity, ResourceLocation source, int stamina, int ticks) {
        service().addBonusStamina(entity, source, stamina, ticks);
    }

    /**
     * Removes temporary stamina from one source without affecting other bonuses.
     *
     * @param entity the entity whose bonus to remove
     * @param source the bonus identifier
     */
    public static void removeBonusStamina(LivingEntity entity, ResourceLocation source) {
        service().removeBonusStamina(entity, source);
    }

    /**
     * Sets stamina directly and clamps it to the maximum. Any stamina above zero clears the strain (and posts
     * {@code StrainEvent.Cleared}): stamina and strain never sit side by side. Use this for commands and scripted
     * events. Normal gameplay should use {@link #spend} or {@link #gain}.
     *
     * @param entity  the entity to update
     * @param stamina the new amount in stamina units
     */
    public static void setStamina(LivingEntity entity, int stamina) {
        service().setStamina(entity, stamina);
    }

    /**
     * Restores full stamina and clears strain and exhaustion. Use this for administrative or scripted resets.
     *
     * @param entity the entity to reset
     */
    public static void reset(LivingEntity entity) {
        service().reset(entity);
    }

    /**
     * Pauses regeneration for an activity. Source identifiers keep blocks from different mods independent.
     *
     * @param entity the entity whose regeneration to pause
     * @param source a stable identifier for the pause
     * @param ticks negative for until {@link #unblockRegen}
     */
    public static void blockRegen(LivingEntity entity, ResourceLocation source, int ticks) {
        service().blockRegen(entity, source, ticks);
    }

    /**
     * Removes the regeneration block from one source.
     *
     * @param entity the entity whose regeneration to resume
     * @param source the block identifier
     */
    public static void unblockRegen(LivingEntity entity, ResourceLocation source) {
        service().unblockRegen(entity, source);
    }

    /**
     * Adds a rest effect, such as a hot spring, that multiplies strain recovery. Only the highest applicable
     * multiplier applies. This transient bonus should be refreshed while its cause remains active.
     *
     * @param entity     the entity receiving the rest bonus
     * @param source     a stable identifier for the bonus
     * @param multiplier the strain recovery multiplier
     * @param ticks      the duration in ticks, or a negative value until removed
     */
    public static void setRestBonus(LivingEntity entity, ResourceLocation source, double multiplier, int ticks) {
        service().setRestBonus(entity, source, multiplier, ticks);
    }

    /**
     * Removes the rest bonus from one source.
     *
     * @param entity the entity whose bonus to remove
     * @param source the bonus identifier
     */
    public static void removeRestBonus(LivingEntity entity, ResourceLocation source) {
        service().removeRestBonus(entity, source);
    }

    /**
     * Gets the entity's climate from the latest provider evaluation.
     *
     * @param entity the entity to query
     * @return the latest climate, or {@link Climate#NEUTRAL} when no provider supplies one
     */
    public static Climate getClimate(LivingEntity entity) {
        return service().getClimate(entity);
    }

    /**
     * Gets the entity's current armor weight in feathers. Use this for displays or gameplay checks.
     *
     * @param entity the entity to query
     * @return the rounded total weight in feathers
     */
    public static int getArmorWeight(LivingEntity entity) {
        return service().getArmorWeight(entity);
    }

    /**
     * Calculates the weight of one worn item after enchantments. The fractional result is rounded only in totals.
     *
     * @param stack the item stack to evaluate
     * @return the item's effective weight in feathers
     */
    public static double getPieceWeight(ItemStack stack) {
        return service().getPieceWeight(stack);
    }

    /**
     * Recalculates the armor weight now, e.g. after your {@link WeightSource} changed its mind.
     *
     * @param entity the entity whose weight to recalculate
     */
    public static void recalculateWeight(LivingEntity entity) {
        service().recalculateWeight(entity);
    }

    /**
     * Registers or replaces a climate provider. Higher priorities run first.
     *
     * @param id       a stable identifier for the provider
     * @param priority its selection priority
     * @param provider the provider to register
     */
    public static void registerClimateProvider(ResourceLocation id, int priority, ClimateProvider provider) {
        service().registerClimateProvider(id, priority, provider);
    }

    /**
     * Registers or replaces a regeneration factor. Use this for needs such as thirst or nutrition.
     *
     * @param id     a stable identifier for the factor
     * @param factor the factor to register
     */
    public static void registerRegenFactor(ResourceLocation id, RegenFactor factor) {
        service().registerRegenFactor(id, factor);
    }

    /**
     * Registers or replaces an extra weight source. Use this for backpacks, accessories, or inventory weight.
     *
     * @param id     a stable identifier for the source
     * @param source the weight source to register
     */
    public static void registerWeightSource(ResourceLocation id, WeightSource source) {
        service().registerWeightSource(id, source);
    }

    /**
     * Registers or replaces a contextual stamina modifier. Lower ordinals run first.
     *
     * @param id       a stable identifier for the modifier
     * @param ordinal  its position in the modifier chain
     * @param modifier the modifier to register
     */
    public static void registerStaminaModifier(ResourceLocation id, int ordinal, StaminaModifier modifier) {
        service().registerStaminaModifier(id, ordinal, modifier);
    }

    /**
     * Declares that a mod charges player actions itself, such as sprinting and jumping. Feathers of Fatigue then turns
     * off its own costs for those actions (the basic exertion), so a player never pays twice. Call it during mod
     * construction or common setup, which run on both sides, before any player ticks. Thread safe. It cannot be undone.
     *
     * @param modId the id of the mod that takes over player actions
     */
    public static void takeOverPlayerActions(String modId) {
        playerActionOwners.add(Objects.requireNonNull(modId, "modId"));
        playerActionsTakenOver = true;
    }

    /**
     * Checks whether another mod took over player actions through {@link #takeOverPlayerActions}.
     *
     * @return {@code true} when at least one mod charges player actions itself
     */
    public static boolean arePlayerActionsTakenOver() {
        return playerActionsTakenOver;
    }

    /**
     * Gets the mods that took over player actions through {@link #takeOverPlayerActions}.
     *
     * @return a live, read-only view of their ids
     */
    public static Set<String> getPlayerActionOwners() {
        return Collections.unmodifiableSet(playerActionOwners);
    }

    /**
     * Sends the entity's feathers to its client now. Rarely needed: changes are synced automatically.
     *
     * @param entity the entity whose state to send
     */
    public static void sync(LivingEntity entity) {
        service().sync(entity);
    }

    /**
     * Changes the base maximum stamina attribute. Attribute modifiers still apply to the final value.
     *
     * @param entity   the entity to update
     * @param feathers the new base maximum in feathers
     */
    public static void setMaxFeathers(LivingEntity entity, double feathers) {
        AttributeInstance attr = entity.getAttribute(FeathersAttributes.MAX_FEATHERS);
        if (attr != null) attr.setBaseValue(feathers);
    }

    /**
     * Gets the final regeneration attribute after all attribute modifiers.
     *
     * @param entity the entity to query
     * @return the current rate in feathers per second
     */
    public static double getRegenPerSecond(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND);
        return attr != null ? attr.getValue() : 0.0;
    }

    /**
     * Changes the base regeneration attribute. Attribute modifiers still apply to the final value.
     *
     * @param entity            the entity to update
     * @param feathersPerSecond the new base rate
     */
    public static void setBaseRegenPerSecond(LivingEntity entity, double feathersPerSecond) {
        AttributeInstance attr = entity.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND);
        if (attr != null) attr.setBaseValue(feathersPerSecond);
    }

    /**
     * Gets the final multiplier applied to stamina costs.
     *
     * @param entity the entity to query
     * @return the current multiplier, or {@code 1.0} without the attribute
     */
    public static double getUsageMultiplier(LivingEntity entity) {
        AttributeInstance attr = entity.getAttribute(FeathersAttributes.USAGE_MULTIPLIER);
        return attr != null ? attr.getValue() : 1.0;
    }

    /**
     * Checks for the Feathers of Fatigue Cold effect.
     *
     * @param entity the entity to check
     * @return whether the effect is active
     */
    public static boolean isCold(LivingEntity entity) {
        return entity.hasEffect(FeathersMobEffects.COLD);
    }

    /**
     * Checks for the Feathers of Fatigue Hot effect.
     *
     * @param entity the entity to check
     * @return whether the effect is active
     */
    public static boolean isHot(LivingEntity entity) {
        return entity.hasEffect(FeathersMobEffects.HOT);
    }

    /**
     * Checks for the Feathers of Fatigue Fatigue effect.
     *
     * @param entity the entity to check
     * @return whether the effect is active
     */
    public static boolean isFatigued(LivingEntity entity) {
        return entity.hasEffect(FeathersMobEffects.FATIGUE);
    }

    /**
     * Checks for the Feathers of Fatigue Energized effect.
     *
     * @param entity the entity to check
     * @return whether the effect is active
     */
    public static boolean isEnergized(LivingEntity entity) {
        return entity.hasEffect(FeathersMobEffects.ENERGIZED);
    }

    /**
     * Checks for the Feathers of Fatigue Endurance effect.
     *
     * @param entity the entity to check
     * @return whether the effect is active
     */
    public static boolean isEnduring(LivingEntity entity) {
        return entity.hasEffect(FeathersMobEffects.ENDURANCE);
    }

    /**
     * Checks for the Feathers of Fatigue Momentum effect.
     *
     * @param entity the entity to check
     * @return whether the effect is active
     */
    public static boolean hasMomentum(LivingEntity entity) {
        return entity.hasEffect(FeathersMobEffects.MOMENTUM);
    }

    /**
     * Installs the internal service implementation. Feathers of Fatigue calls this once during startup.
     *
     * @param implementation the service implementation
     * @throws NullPointerException if {@code implementation} is {@code null}
     * @throws IllegalStateException if a service is already installed
     */
    @ApiStatus.Internal
    public static void setService(FeathersService implementation) {
        if (service != null) throw new IllegalStateException("The Feathers of Fatigue service is already set");
        service = Objects.requireNonNull(implementation, "implementation");
    }
}
