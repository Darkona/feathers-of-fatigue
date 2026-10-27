package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.Climate;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.RestState;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.content.weight.WeightSplit;
import dev.darkona.jdlib.core.debug.RecentSourceLog;
import dev.darkona.jdlib.core.stat.FractionalCarry;
import dev.darkona.jdlib.core.stat.LayeredPayment;
import dev.darkona.jdlib.core.stat.ShownFields;
import dev.darkona.jdlib.core.stat.SourcedDrains;
import dev.darkona.jdlib.core.stat.SourcedPools;
import dev.darkona.jdlib.core.stat.TimedSources;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FeathersData implements FeathersView, INBTSerializable<CompoundTag> {

    private static final int SPEND_LOG_SIZE = 32;
    private static final int SPEND_FOLD_TICKS = 20;

    enum Shown { STAMINA, MAX_STAMINA, STRAIN, MAX_STRAIN, BONUS, WEIGHT, EXHAUSTED, DELAYED, REST }

    int stamina;
    int strain;
    int regenDelay;
    boolean exhausted;
    final SourcedPools<ResourceLocation> bonuses = new SourcedPools<>();
    final Object2DoubleOpenHashMap<String> counters = new Object2DoubleOpenHashMap<>();

    int maxStamina;
    int maxStrain;
    int weight;
    final WeightSplit weightSplit = new WeightSplit();
    double lastWeightMultiplier = Double.NaN;
    boolean initialized;
    boolean fresh = true;

    final SourcedDrains<ResourceLocation> drains = new SourcedDrains<>();
    final TimedSources<ResourceLocation> regenBlocks = new TimedSources<>();
    final TimedSources<ResourceLocation> restBonuses = new TimedSources<>();
    final FractionalCarry regenCarry = new FractionalCarry();
    private final Object2ObjectOpenHashMap<String, FractionalCarry> carries = new Object2ObjectOpenHashMap<>();
    boolean regenPaused;
    int lastDelta;
    long totalRegenerated;
    Climate climate = Climate.NEUTRAL;

    RecentSourceLog<ResourceLocation> spendLog;

    @Nullable AttributeInstance regenAttribute;
    @Nullable AttributeInstance usageAttribute;
    @Nullable AttributeInstance maxFeathersAttribute;
    @Nullable AttributeInstance maxStrainAttribute;
    @Nullable AttributeInstance weightMultiplierAttribute;

    RestState restState = RestState.NONE;
    double restMultiplier = 1.0;
    double lastX;
    double lastY;
    double lastZ;
    int stillTicks;

    final ShownFields<Shown> shown = new ShownFields<>(Shown.class);
    final WeightSplit syncedWeightSplit = new WeightSplit();

    public boolean mountSlowed;

    @Override
    public boolean hasFeathers() {
        return true;
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
        return regularAvailable() + bonusStamina();
    }

    @Override
    public int weight() {
        return weight;
    }

    public int weightPart(int part) {
        return weightSplit.part(part);
    }

    public WeightSplit weightSplit() {
        return weightSplit;
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
        return bonuses.total();
    }

    @Override
    public int regenDelay() {
        return regenDelay;
    }

    @Override
    public int lastDelta() {
        return lastDelta;
    }

    @Override
    public long totalRegenerated() {
        return totalRegenerated;
    }

    @Override
    public boolean exhausted() {
        return exhausted;
    }

    @Override
    public RestState restState() {
        return restState;
    }

    @Override
    public double restMultiplier() {
        return restMultiplier;
    }

    int regularAvailable() {
        return Math.max(0, stamina - Stamina.ofFeathers(weight));
    }

    int strainRoom(boolean allowStrain, boolean strainEnabled) {
        return LayeredPayment.overdrawRoom(strain, maxStrain, allowStrain && strainEnabled);
    }

    boolean canPay(int cost, boolean allowStrain, boolean strainEnabled) {
        return LayeredPayment.affordable(cost, bonuses.totalLong(), regularAvailable(), strainRoom(allowStrain, strainEnabled));
    }

    int pay(int cost) {
        int left = cost - bonuses.take(cost);
        int take = Math.min(regularAvailable(), left);
        stamina -= take;
        left -= take;
        strain += left;
        return left;
    }

    boolean isSpent(boolean strainEnabled) {
        return availableStamina() <= 0 && strainRoom(true, strainEnabled) <= 0;
    }

    public boolean isAtRest() {
        return initialized && stamina >= maxStamina && strain == 0 && regenDelay == 0 && !exhausted
                && bonuses.isEmpty() && drains.isEmpty() && regenBlocks.isEmpty();
    }

    public int bonusStamina(ResourceLocation source) {
        return bonuses.amount(source);
    }

    void expire(long now) {
        bonuses.expire(now);
        regenBlocks.expire(now);
        restBonuses.expire(now);
    }

    boolean isRegenBlocked() {
        return regenDelay > 0 || !regenBlocks.isEmpty() || drains.anyBlocksRegen();
    }

    void logSpend(ResourceLocation source, int amount, long gameTime) {
        if (spendLog == null) spendLog = new RecentSourceLog<>(SPEND_LOG_SIZE, SPEND_FOLD_TICKS);
        spendLog.record(source, amount, gameTime);
    }

    public @Nullable RecentSourceLog<ResourceLocation> spendLog() {
        return spendLog;
    }

    @Nullable AttributeInstance regenAttribute(LivingEntity entity) {
        if (regenAttribute == null) regenAttribute = entity.getAttribute(FeathersAttributes.FEATHERS_PER_SECOND);
        return regenAttribute;
    }

    @Nullable AttributeInstance usageAttribute(LivingEntity entity) {
        if (usageAttribute == null) usageAttribute = entity.getAttribute(FeathersAttributes.USAGE_MULTIPLIER);
        return usageAttribute;
    }

    @Nullable AttributeInstance maxFeathersAttribute(LivingEntity entity) {
        if (maxFeathersAttribute == null) maxFeathersAttribute = entity.getAttribute(FeathersAttributes.MAX_FEATHERS);
        return maxFeathersAttribute;
    }

    @Nullable AttributeInstance maxStrainAttribute(LivingEntity entity) {
        if (maxStrainAttribute == null) maxStrainAttribute = entity.getAttribute(FeathersAttributes.MAX_STRAIN);
        return maxStrainAttribute;
    }

    @Nullable AttributeInstance weightMultiplierAttribute(LivingEntity entity) {
        if (weightMultiplierAttribute == null) weightMultiplierAttribute = entity.getAttribute(FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER);
        return weightMultiplierAttribute;
    }

    public FractionalCarry carry(String name) {
        FractionalCarry carry = carries.get(name);
        if (carry == null) carries.put(name, carry = new FractionalCarry());
        return carry;
    }

    public double getCounter(String name) {
        return counters.getDouble(name);
    }

    public void setCounter(String name, double value) {
        counters.put(name, value);
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stamina", stamina);
        tag.putInt("strain", strain);
        tag.putInt("regen_delay", regenDelay);
        tag.putBoolean("exhausted", exhausted);

        ListTag bonusList = new ListTag();
        for (int i = 0; i < bonuses.size(); i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("source", bonuses.source(i).toString());
            entry.putInt("amount", bonuses.amountAt(i));
            entry.putLong("expires_at", bonuses.expiryAt(i));
            bonusList.add(entry);
        }
        tag.put("bonuses", bonusList);

        CompoundTag counterTag = new CompoundTag();
        for (Object2DoubleMap.Entry<String> e : counters.object2DoubleEntrySet()) {
            counterTag.putDouble(e.getKey(), e.getDoubleValue());
        }
        tag.put("counters", counterTag);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        stamina = tag.getInt("stamina");
        strain = tag.getInt("strain");
        regenDelay = tag.getInt("regen_delay");
        exhausted = tag.getBoolean("exhausted");

        bonuses.clear();
        for (Tag t : tag.getList("bonuses", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            ResourceLocation source = ResourceLocation.tryParse(entry.getString("source"));
            if (source != null) bonuses.set(source, entry.getInt("amount"), entry.getLong("expires_at"));
        }

        counters.clear();
        CompoundTag counterTag = tag.getCompound("counters");
        for (String key : counterTag.getAllKeys()) {
            counters.put(key, counterTag.getDouble(key));
        }
        initialized = false;
        fresh = false;
        shown.resendNext();
    }
}
