package com.darkona.feathersoffatigue.index;

import com.darkona.feathersoffatigue.content.feathers.FeathersData;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FeathersAttachmentTypes {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FeathersIds.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FeathersData>> FEATHERS =
            ATTACHMENT_TYPES.register("feathers", () -> AttachmentType.serializable(FeathersData::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private FeathersAttachmentTypes() {}
}
