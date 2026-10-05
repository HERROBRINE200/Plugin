package com.gamerlegend.highenergy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

public final class EnergyCommands {
    private EnergyCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("energy");
            root.executes(context -> information(context.getSource().getPlayerOrException(), "english"));

            root.then(Commands.literal("powers")
                    .executes(context -> powers(context.getSource().getPlayerOrException())));

            root.then(Commands.literal("select")
                    .then(Commands.argument("power", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                    Arrays.stream(Power.values())
                                            .map(power -> power.name().toLowerCase(Locale.ROOT)).toList(),
                                    builder))
                            .executes(context -> select(
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "power")))));

            LiteralArgumentBuilder<CommandSourceStack> guide = Commands.literal("guide")
                    .executes(context -> {
                        context.getSource().sendSystemMessage(Component.literal(
                                "Usage: /energy guide book|information english|hindi")
                                .withStyle(ChatFormatting.YELLOW));
                        return 1;
                    });
            guide.then(Commands.literal("book")
                    .then(Commands.argument("language", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                    new String[]{"english", "hindi"}, builder))
                            .executes(context -> book(
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "language")))));
            guide.then(Commands.literal("information")
                    .then(Commands.argument("language", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                    new String[]{"english", "hindi"}, builder))
                            .executes(context -> information(
                                    context.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(context, "language")))));
            root.then(guide);

            LiteralArgumentBuilder<CommandSourceStack> admin = Commands.literal("admin")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        context.getSource().sendSystemMessage(Component.literal(
                                "Admin: give, take, set, refill, setmax")
                                .withStyle(ChatFormatting.AQUA));
                        return 1;
                    });
            admin.then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(context -> change(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, "player"),
                                            IntegerArgumentType.getInteger(context, "amount"),
                                            "gave")))));
            admin.then(Commands.literal("take")
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(context -> change(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, "player"),
                                            -IntegerArgumentType.getInteger(context, "amount"),
                                            "took")))));
            admin.then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                    .executes(context -> set(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, "player"),
                                            IntegerArgumentType.getInteger(context, "amount"))))));
            admin.then(Commands.literal("refill")
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> refill(
                                    context.getSource(), EntityArgument.getPlayer(context, "player")))));
            admin.then(Commands.literal("setmax")
                    .then(Commands.argument("player", EntityArgument.player())
                            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                    .executes(context -> setMaximum(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, "player"),
                                            IntegerArgumentType.getInteger(context, "amount"))))));
            root.then(admin);

            dispatcher.register(root);
        });
    }

    private static int select(ServerPlayer player, String input) {
        Power power = Power.fromInput(input).orElse(null);
        if (power == null) {
            player.sendSystemMessage(Component.literal("Unknown power. Use /energy powers.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        PowerSystem.select(player, power);
        return 1;
    }

    private static int book(ServerPlayer player, String language) {
        if (!GuideBook.isSupportedLanguage(language)) {
            player.sendSystemMessage(Component.literal("Language must be english or hindi.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        GuideBook.give(player, language);
        return 1;
    }

    private static int information(ServerPlayer player, String language) {
        if (!GuideBook.isSupportedLanguage(language)) {
            player.sendSystemMessage(Component.literal("Language must be english or hindi.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        boolean hindi = language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi");
        player.sendSystemMessage(Component.literal("HIGH ENERGY").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.sendSystemMessage(EnergyManager.bar(player));
        player.sendSystemMessage(Component.literal((hindi
                ? "Selected power: " + PowerSystem.selected(player).display
                    + "\nEnergy har second 3 regenerate hoti hai. Sneak + left-click se power activate karein."
                : "Selected power: " + PowerSystem.selected(player).display
                    + "\nEnergy regenerates by 3 per second. Sneak + left-click to activate.")));
        player.sendSystemMessage(Component.literal("/energy powers | /energy select <power> | /energy guide book "
                + (hindi ? "hindi" : "english")).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int powers(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("HIGH ENERGY POWERS").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        for (Power power : Power.values()) {
            player.sendSystemMessage(Component.literal(power.name().toLowerCase(Locale.ROOT) + " - " + power.display)
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(" | Cost " + power.cost + " | Cooldown "
                            + power.cooldownSeconds() + "s | " + power.description)
                            .withStyle(ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int change(CommandSourceStack source, ServerPlayer target, int amount, String verb) {
        EnergyManager.add(target, amount);
        HighEnergyData.saveIfDirty();
        int displayed = Math.abs(amount);
        source.sendSystemMessage(Component.literal("High Energy: " + verb + " " + displayed
                + " Energy " + (verb.equals("took") ? "from " : "to ") + target.getName().getString()
                + ". New value: " + EnergyManager.get(target) + ".").withStyle(ChatFormatting.AQUA));
        target.sendSystemMessage(Component.literal("Your Energy is now " + EnergyManager.get(target) + "/"
                + EnergyManager.getMax(target) + ".").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer target, int amount) {
        EnergyManager.set(target, amount);
        HighEnergyData.saveIfDirty();
        source.sendSystemMessage(Component.literal("Set " + target.getName().getString() + "'s Energy to "
                + EnergyManager.get(target) + ".").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int refill(CommandSourceStack source, ServerPlayer target) {
        EnergyManager.set(target, EnergyManager.getMax(target));
        HighEnergyData.saveIfDirty();
        source.sendSystemMessage(Component.literal("Refilled " + target.getName().getString() + " to "
                + EnergyManager.getMax(target) + " Energy.").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static int setMaximum(CommandSourceStack source, ServerPlayer target, int amount) {
        EnergyManager.setMax(target, amount);
        HighEnergyData.saveIfDirty();
        source.sendSystemMessage(Component.literal("Set " + target.getName().getString() + "'s maximum Energy to "
                + EnergyManager.getMax(target) + ".").withStyle(ChatFormatting.AQUA));
        return 1;
    }
}
