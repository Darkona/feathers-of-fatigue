package com.darkona.feathersoffatigue.command;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.core.FeathersServiceImpl;
import com.darkona.feathersoffatigue.core.SpendLog;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
    private static final String SECONDS = "seconds";
    private static final int DEFAULT_DEBUG_SECONDS = 30;
    private static final double MAX_FEATHERS_ARG = Integer.MAX_VALUE / Stamina.PER_FEATHER;
    private static final SimpleCommandExceptionType NO_FEATHERS =
            new SimpleCommandExceptionType(Component.literal("None of those have feathers"));

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
                .then(Commands.literal("debug")
                        .then(Commands.argument(TARGETS, EntityArgument.entities())
                                .executes(FeathersCommands::debugDefault)
                                .then(Commands.argument(SECONDS, IntegerArgumentType.integer(1, 600)).executes(FeathersCommands::debug))))
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

    private static int debug(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return debugFor(ctx, IntegerArgumentType.getInteger(ctx, SECONDS));
    }

    private static int debugFor(CommandContext<CommandSourceStack> ctx, int seconds) throws CommandSyntaxException {
        List<LivingEntity> targets = targets(ctx);
        for (LivingEntity entity : targets) {
            SpendLog log = FeathersServiceImpl.data(entity).spendLog();
            var totals = log != null ? log.totalsSince(entity.level().getGameTime() - seconds * 20L) : null;
            if (totals == null || totals.isEmpty()) {
                ctx.getSource().sendSuccess(() -> Component.literal("%s spent nothing in the last %ds".formatted(entity.getName().getString(), seconds)), false);
                continue;
            }
            StringBuilder lines = new StringBuilder("%s, last %ds:".formatted(entity.getName().getString(), seconds));
            for (var entry : totals.object2IntEntrySet()) {
                lines.append("\n  ").append(entry.getKey()).append(": %.2f feathers".formatted(feathersOf(entry.getIntValue())));
            }
            ctx.getSource().sendSuccess(() -> Component.literal(lines.toString()), false);
        }
        return targets.size();
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

    private static int debugDefault(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return debugFor(ctx, DEFAULT_DEBUG_SECONDS);
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
