package com.darkona.feathersoffatigue.api.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Common Feathers of Fatigue identifiers and entity type tags. */
public final class FeathersIds {

    /** The namespace used by Feathers of Fatigue resources and registrations. */
    public static final String MOD_ID = "feathers_of_fatigue";

    private FeathersIds() {}

    /**
     * Entity types that count as mounts besides every {@code AbstractHorse}: add your rideable creatures here
     * ({@code data/feathers_of_fatigue/tags/entity_types/mounts.json}) and they get feathers while mounts are enabled. An
     * entry in the {@code feathers_of_fatigue:mount_stats} data map does the same and tunes them too.
     */
    public static final TagKey<EntityType<?>> MOUNTS = TagKey.create(Registries.ENTITY_TYPE, id("mounts"));

    /**
     * Entity types in this opt-out tag never get feathers, including horses. Use it for mounts that should not tire.
     */
    public static final TagKey<EntityType<?>> NO_FEATHERS = TagKey.create(Registries.ENTITY_TYPE, id("no_feathers"));

    /**
     * Creates a resource location in the Feathers of Fatigue namespace.
     *
     * @param path the resource path
     * @return {@code feathers_of_fatigue:path}
     */
    @SuppressWarnings("removal")
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
