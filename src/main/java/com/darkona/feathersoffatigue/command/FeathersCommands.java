package com.darkona.feathersoffatigue.command;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.darkona.jdlib.api.command.JDDebugCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;

@EventBusSubscriber(modid = FeathersIds.MOD_ID)
public final class FeathersCommands {

    private static final String TARGETS = "targets";
    private static final String AMOUNT = "amount";
    private static final double MAX_FEATHERS_ARG = Integer.MAX_VALUE / Stamina.PER_FEATHER;
    private static final Component NO_FEATHERS_TEXT = Component.literal("None of those have feathers");
    private static final SimpleCommandExceptionType NO_FEATHERS = new SimpleCommandExceptionType(NO_FEATHERS_TEXT);

    private FeathersCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("feathers")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info")
                        .then(Commands.argument(TARGETS, EntityArgument.entities()).executes(FeathersCommands::info)))
                .then(Commands.literal("set")
                        .then(Commands.argument(TARGETS, EntityArgument.entities())
                                .then(Commands.argument(AMOUNT, DoubleArgumentType.doubleArg(0, MAX_FEATHERS_ARG)).executes(FeathersCommands::set))))
                .then(Commands.literal("reset")
                        .then(Commands.argument(TARGETS, EntityArgument.entities()).executes(FeathersCommands::reset)))
                .then(Commands.literal("max")
                        .then(Commands.argument(TARGETS, EntityArgument.entities())
                                .then(Commands.argument(AMOUNT, IntegerArgumentType.integer(0, 1000)).executes(FeathersCommands::setMax))))
                .then(Commands.literal("regen")
                        .then(Commands.argument(TARGETS, EntityArgument.entities())
                                .then(Commands.argument(AMOUNT, DoubleArgumentType.doubleArg(-40, 40)).executes(FeathersCommands::setRegen))))
                .then(JDDebugCommand.recentSources("debug", entity -> FeathersServiceImpl.data(entity).spendLog(), FeathersAPI::hasFeathers, NO_FEATHERS_TEXT, amount -> "%.2f feathers".formatted(feathersOf(amount))))
                .then(Commands.literal("spend")
                        .then(Commands.argument(TARGETS, EntityArgument.entities())
                                .then(Commands.argument(AMOUNT, DoubleArgumentType.doubleArg(0, MAX_FEATHERS_ARG)).executes(FeathersCommands::spend)))));
    }

    private static List<LivingEntity> targets(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : EntityArgument.getEntities(ctx, TARGETS)) {
            if (entity instanceof LivingEntity living && FeathersAPI.hasFeathers(living)) targets.add(living);
        }
        if (targets.isEmpty()) throw NO_FEATHERS.create();
        return targets;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> entities = targets(ctx);
        for (LivingEntity entity : entities) {
            FeathersView f = FeathersAPI.get(entity);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "%s: %.2f/%d feathers (%.2f usable), strain %.2f/%.2f, bonus %.2f, weight %d, regen %.2f f/s%s%s, climate %s, rest %s x%.1f".formatted(
                            entity.getName().getString(), feathersOf(f.stamina()), f.maxFeathers(), feathersOf(f.availableStamina()),
                            feathersOf(f.strain()), feathersOf(f.maxStrain()), feathersOf(f.bonusStamina()), f.weight(),
                            FeathersAPI.getRegenPerSecond(entity), f.regenDelay() > 0 ? " (paused " + f.regenDelay() + "t)" : "",
                            f.exhausted() ? ", EXHAUSTED" : "", FeathersAPI.getClimate(entity), f.restState(), f.restMultiplier())), false);
        }
        return entities.size();
    }

    private static double feathersOf(int stamina) {
        return stamina / (double) Stamina.PER_FEATHER;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> entities = targets(ctx);
        double amount = DoubleArgumentType.getDouble(ctx, AMOUNT);
        for (LivingEntity entity : entities) FeathersAPI.setStamina(entity, Stamina.ofFeathers(amount));
        ctx.getSource().sendSuccess(() -> Component.literal("Set feathers to %.2f for %d target(s)".formatted(amount, entities.size())), true);
        return entities.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> entities = targets(ctx);
        entities.forEach(FeathersAPI::reset);
        ctx.getSource().sendSuccess(() -> Component.literal("Reset feathers for %d target(s)".formatted(entities.size())), true);
        return entities.size();
    }

    private static int setMax(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> entities = targets(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, AMOUNT);
        for (LivingEntity entity : entities) FeathersAPI.setMaxFeathers(entity, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("Set base max feathers to %d for %d target(s)".formatted(amount, entities.size())), true);
        return entities.size();
    }

    private static int setRegen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> entities = targets(ctx);
        double amount = DoubleArgumentType.getDouble(ctx, AMOUNT);
        for (LivingEntity entity : entities) FeathersAPI.setBaseRegenPerSecond(entity, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("Set base regeneration to %.2f feathers/s for %d target(s)".formatted(amount, entities.size())), true);
        return entities.size();
    }

    private static int spend(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> entities = targets(ctx);
        double amount = DoubleArgumentType.getDouble(ctx, AMOUNT);
        for (LivingEntity entity : entities) {
            var result = FeathersAPI.spend(entity, id("command"), Stamina.ofFeathers(amount));
            ctx.getSource().sendSuccess(() -> Component.literal("%s: %s".formatted(entity.getName().getString(), result)), false);
        }
        return entities.size();
    }
}
