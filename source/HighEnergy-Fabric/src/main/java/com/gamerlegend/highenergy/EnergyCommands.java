package com.gamerlegend.highenergy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code /energy} command tree.
 *
 * <pre>
 * /energy
 * /energy powers
 * /energy select &lt;power&gt;
 * /energy guide book &lt;english|hindi&gt;
 * /energy guide information &lt;english|hindi&gt;
 * /energy admin give|take|set|setmax &lt;player&gt; &lt;amount&gt;
 * /energy admin refill &lt;player&gt;
 * </pre>
 */
public final class EnergyCommands {

    /** Vanilla permission level required for every {@code /energy admin ...} branch. */
    public static final int ADMIN_PERMISSION_LEVEL = 2;

    private EnergyCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(build()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("energy")
                .executes(ctx -> status(player(ctx)))
                .then(Commands.literal("powers")
                        .executes(ctx -> powers(ctx)))
                .then(Commands.literal("select")
                        .then(Commands.argument("power", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    List<String> ids = new ArrayList<>();
                                    for (Power power : Power.values()) {
                                        ids.add(power.id());
                                    }
                                    return SharedSuggestionProvider.suggest(ids, builder);
                                })
                                .executes(ctx -> select(ctx))))
                .then(Commands.literal("guide")
                        .then(Commands.literal("book")
                                .executes(ctx -> giveBook(ctx, "english"))
                                .then(Commands.argument("language", StringArgumentType.word())
                                        .suggests((ctx, builder) ->
                                                SharedSuggestionProvider.suggest(List.of("english", "hindi"), builder))
                                        .executes(ctx -> giveBook(ctx,
                                                StringArgumentType.getString(ctx, "language")))))
                        .then(Commands.literal("information")
                                .executes(ctx -> information(ctx, "english"))
                                .then(Commands.argument("language", StringArgumentType.word())
                                        .suggests((ctx, builder) ->
                                                SharedSuggestionProvider.suggest(List.of("english", "hindi"), builder))
                                        .executes(ctx -> information(ctx,
                                                StringArgumentType.getString(ctx, "language"))))))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION_LEVEL))
                        .then(Commands.literal("give")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    EnergyManager.add(target, amount);
                                                    EnergyStorage.save();
                                                    feedback(ctx, "\u00a7b\u26a1 Gave \u00a7f" + amount
                                                            + " \u00a7bEnergy to \u00a7f" + target.getGameProfile().getName());
                                                    target.sendSystemMessage(Component.literal(
                                                            "\u00a7b\u26a1 You received \u00a7f" + amount + " \u00a7bEnergy."));
                                                    return 1;
                                                }))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    EnergyManager.add(target, -amount);
                                                    EnergyStorage.save();
                                                    feedback(ctx, "\u00a7b\u26a1 Took \u00a7f" + amount
                                                            + " \u00a7bEnergy from \u00a7f" + target.getGameProfile().getName());
                                                    return 1;
                                                }))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    EnergyManager.set(target, IntegerArgumentType.getInteger(ctx, "amount"));
                                                    EnergyStorage.save();
                                                    feedback(ctx, "\u00a7b\u26a1 Set \u00a7f"
                                                            + target.getGameProfile().getName()
                                                            + "\u00a7b's Energy to \u00a7f" + EnergyManager.get(target));
                                                    return 1;
                                                }))))
                        .then(Commands.literal("refill")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                            EnergyManager.refill(target);
                                            EnergyStorage.save();
                                            feedback(ctx, "\u00a7b\u26a1 Refilled \u00a7f"
                                                    + target.getGameProfile().getName() + "\u00a7b to \u00a7f"
                                                    + EnergyManager.getMax(target) + " \u00a7bEnergy.");
                                            target.sendSystemMessage(Component.literal(
                                                    "\u00a7b\u26a1 Your Energy has been refilled."));
                                            return 1;
                                        })))
                        .then(Commands.literal("setmax")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    EnergyManager.setMax(target,
                                                            IntegerArgumentType.getInteger(ctx, "amount"));
                                                    EnergyStorage.save();
                                                    feedback(ctx, "\u00a7b\u26a1 Set \u00a7f"
                                                            + target.getGameProfile().getName()
                                                            + "\u00a7b's maximum Energy to \u00a7f"
                                                            + EnergyManager.getMax(target));
                                                    target.sendSystemMessage(Component.literal(
                                                            "\u00a7b\u26a1 Your maximum Energy is now \u00a7f"
                                                                    + EnergyManager.getMax(target)));
                                                    return 1;
                                                })))));
    }

    // ------------------------------------------------------------------ helpers

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
    }

    private static int status(ServerPlayer player) {
        GuideBook.sendInformation(player, false);
        player.sendSystemMessage(Component.literal(EnergyManager.bar(player)));
        Power selected = PowerSystem.selected(player);
        player.sendSystemMessage(Component.literal("\u00a7b\u26a1 Selected: \u00a7f" + selected.display
                + " \u00a77| Cost: \u00a7b" + selected.cost
                + " \u00a77| Cooldown: \u00a7f" + (selected.cooldownTicks / 20.0) + "s"));
        return 1;
    }

    private static int powers(CommandContext<CommandSourceStack> ctx) {
        feedback(ctx, "\u00a7b\u00a7l\u26a1 POWERS");
        for (Power power : Power.values()) {
            feedback(ctx, "\u00a7f" + power.display + " \u00a78(" + power.id() + ") \u00a77\u2014 Cost: \u00a7b"
                    + power.cost + " \u00a77Cooldown: \u00a7f" + (power.cooldownTicks / 20.0) + "s");
        }
        return 1;
    }

    private static int select(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = player(ctx);
        Power power = Power.byName(StringArgumentType.getString(ctx, "power"));
        if (power == null) {
            player.sendSystemMessage(Component.literal("\u00a7cUnknown power. \u00a77Use \u00a7f/energy powers"));
            return 0;
        }
        PowerSystem.select(player, power);
        return 1;
    }

    private static int giveBook(CommandContext<CommandSourceStack> ctx, String language)
            throws CommandSyntaxException {
        GuideBook.give(player(ctx), language);
        return 1;
    }

    private static int information(CommandContext<CommandSourceStack> ctx, String language)
            throws CommandSyntaxException {
        GuideBook.sendInformation(player(ctx), GuideBook.isHindi(language));
        return 1;
    }
}
