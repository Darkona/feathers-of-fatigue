package com.darkona.feathersoffatigue.content.feathers;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.SpendResult;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@Mod.EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class BasicExertion {
    public static final ResourceLocation SPRINT = id("sprint");
    public static final ResourceLocation JUMP = id("jump");

    private BasicExertion() {}

    public static boolean isActive() {
        return !FeathersAPI.arePlayerActionsTakenOver() && ServerConfig.ENABLE_BASIC_EXERTION.get();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || !isActive() || !player.isSprinting() || player.isPassenger()) return;

        double perSecond = ServerConfig.SPRINT_FEATHERS_PER_SECOND.get();
        if (perSecond <= 0) return;

        SpendResult result = FeathersAPI.startDrain(player, SPRINT, Stamina.perTick(perSecond));
        if (!result.allowed()) player.setSprinting(false);
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isActive()) return;
        double feathers = ServerConfig.JUMP_FEATHERS.get();
        if (feathers > 0) FeathersAPI.spend(player, JUMP, Stamina.ofFeathers(feathers));
    }
}
