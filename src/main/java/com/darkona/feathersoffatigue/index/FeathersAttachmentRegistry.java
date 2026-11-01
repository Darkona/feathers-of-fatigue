package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.content.feathers.FeathersTicker;
import com.darkona.feathersoffatigue.content.feathers.FeathersServiceImpl;
import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

public final class FeathersAttachmentRegistry {
    public static final Capability<FeathersData> FEATHERS = CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation KEY = id("feathers");

    private FeathersAttachmentRegistry() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> event.register(FeathersData.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, FeathersAttachmentRegistry::attach);
        MinecraftForge.EVENT_BUS.addListener(FeathersAttachmentRegistry::copyOnClone);
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity living && FeathersServiceImpl.canHaveFeathers(living)) {
            event.addCapability(KEY, new Provider());
        }
    }

    private static void copyOnClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        FeathersData from = original.getCapability(FEATHERS).orElse(null);
        FeathersData to = event.getEntity().getCapability(FEATHERS).orElse(null);
        if (from != null && to != null) {
            if (event.isWasDeath()) FeathersTicker.copyConfigBases(from, to);
            else to.deserializeNBT(from.serializeNBT());
        }
        original.invalidateCaps();

        for (RegistryObject<Attribute> attribute : ATTRIBUTES) {
            AttributeInstance old = original.getAttribute(attribute.get()), now = event.getEntity().getAttribute(attribute.get());
            if (old != null && now != null) now.setBaseValue(old.getBaseValue());
        }
    }

    private static final List<RegistryObject<Attribute>> ATTRIBUTES = List.of(FeathersAttributes.MAX_FEATHERS, FeathersAttributes.MAX_STRAIN,
            FeathersAttributes.FEATHERS_PER_SECOND, FeathersAttributes.USAGE_MULTIPLIER, FeathersAttributes.ARMOR_WEIGHT_MULTIPLIER, FeathersAttributes.ARMOR_WEIGHT);

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final FeathersData data = new FeathersData();
        private final LazyOptional<FeathersData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return FEATHERS.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
        }
    }
}
