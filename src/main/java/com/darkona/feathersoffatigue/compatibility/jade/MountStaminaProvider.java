package com.darkona.feathersoffatigue.compatibility.jade;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

enum MountStaminaProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = id("mount_stamina");
    private static final String FEATHERS = "feathers";
    private static final String MAX = "max_feathers";

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (accessor.getEntity() instanceof LivingEntity entity && FeathersServiceImpl.isMount(entity)) {
            FeathersView feathers = FeathersAPI.get(entity);
            data.putInt(FEATHERS, feathers.feathers());
            data.putInt(MAX, feathers.maxFeathers());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(MAX)) {
            tooltip.add(Component.translatable("jade.feathers_of_fatigue.mount_stamina", data.getInt(FEATHERS), data.getInt(MAX)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
