package com.gamerlegend.highenergy.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class EnergyCommand implements CommandExecutor, TabCompleter {
    private final PlayerDataStore data;
    private final EnergyService energy;
    private final PowerService powers;

    EnergyCommand(PlayerDataStore data, EnergyService energy, PowerService powers) {
        this.data = data;
        this.energy = energy;
        this.powers = powers;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            return admin(sender, args);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Player subcommands must be run in game. Admin commands work in console.",
                    NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            information(player, "english");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "powers" -> listPowers(player);
            case "select" -> {
                if (args.length != 2) {
                    player.sendMessage(Component.text("Usage: /energy select <power>", NamedTextColor.YELLOW));
                } else {
                    Power power = Power.fromInput(args[1]).orElse(null);
                    if (power == null) {
                        player.sendMessage(Component.text("Unknown power. Use /energy powers.", NamedTextColor.RED));
                    } else {
                        powers.select(player, power);
                    }
                }
            }
            case "guide" -> guide(player, args);
            default -> player.sendMessage(Component.text(
                    "Usage: /energy [powers|select <power>|guide book|information <language>]",
                    NamedTextColor.YELLOW));
        }
        return true;
    }

    private void guide(Player player, String[] args) {
        if (args.length != 3 || (!args[1].equalsIgnoreCase("book")
                && !args[1].equalsIgnoreCase("information"))) {
            player.sendMessage(Component.text(
                    "Usage: /energy guide book|information english|hindi", NamedTextColor.YELLOW));
            return;
        }
        if (!GuideBookService.isSupportedLanguage(args[2])) {
            player.sendMessage(Component.text("Language must be english or hindi.", NamedTextColor.RED));
            return;
        }
        if (args[1].equalsIgnoreCase("book")) {
            GuideBookService.give(player, args[2]);
        } else {
            information(player, args[2]);
        }
    }

    private void information(Player player, String language) {
        boolean hindi = language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi");
        player.sendMessage(Component.text("HIGH ENERGY", NamedTextColor.AQUA)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        player.sendMessage(energy.bar(player));
        player.sendMessage(Component.text("Selected power: " + powers.selected(player).display, NamedTextColor.WHITE));
        player.sendMessage(Component.text(hindi
                ? "Energy har second 3 regenerate hoti hai. Sneak + left-click se power activate karein."
                : "Energy regenerates by 3 per second. Sneak + left-click to activate.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/energy powers | /energy select <power> | /energy guide book "
                + (hindi ? "hindi" : "english"), NamedTextColor.GRAY));
    }

    private void listPowers(Player player) {
        player.sendMessage(Component.text("HIGH ENERGY POWERS", NamedTextColor.AQUA)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        for (Power power : Power.values()) {
            player.sendMessage(Component.text(power.name().toLowerCase(Locale.ROOT) + " - " + power.display,
                            NamedTextColor.WHITE)
                    .append(Component.text(" | Cost " + power.cost + " | Cooldown "
                            + power.cooldownSeconds() + "s | " + power.description, NamedTextColor.GRAY)));
        }
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("highenergy.admin")) {
            sender.sendMessage(Component.text("You do not have highenergy.admin.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Admin: give, take, set, refill, setmax", NamedTextColor.AQUA));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        boolean refill = action.equals("refill");
        if ((!refill && args.length != 4) || (refill && args.length != 3)) {
            sender.sendMessage(Component.text("Usage: /energy admin " + action
                    + " <player>" + (refill ? "" : " <amount>"), NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return true;
        }

        if (refill) {
            energy.set(target, energy.getMaximum(target));
            saveAndConfirm(sender, target, "Refilled to " + energy.getMaximum(target) + " Energy.");
            return true;
        }

        Integer amount = parseAmount(sender, args[3]);
        if (amount == null) {
            return true;
        }
        switch (action) {
            case "give" -> {
                if (amount < 1) {
                    positiveRequired(sender);
                    return true;
                }
                energy.add(target, amount);
                saveAndConfirm(sender, target, "Gave " + amount + " Energy. New value: " + energy.get(target) + ".");
            }
            case "take" -> {
                if (amount < 1) {
                    positiveRequired(sender);
                    return true;
                }
                energy.add(target, -amount);
                saveAndConfirm(sender, target, "Took " + amount + " Energy. New value: " + energy.get(target) + ".");
            }
            case "set" -> {
                if (amount < 0) {
                    sender.sendMessage(Component.text("Amount must be zero or greater.", NamedTextColor.RED));
                    return true;
                }
                energy.set(target, amount);
                saveAndConfirm(sender, target, "Set Energy to " + energy.get(target) + ".");
            }
            case "setmax" -> {
                if (amount < 1) {
                    positiveRequired(sender);
                    return true;
                }
                energy.setMaximum(target, amount);
                saveAndConfirm(sender, target, "Set maximum Energy to " + energy.getMaximum(target) + ".");
            }
            default -> sender.sendMessage(Component.text("Unknown admin action. Use give, take, set, refill or setmax.",
                    NamedTextColor.RED));
        }
        return true;
    }

    private Integer parseAmount(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Component.text("Amount must be a whole number.", NamedTextColor.RED));
            return null;
        }
    }

    private void positiveRequired(CommandSender sender) {
        sender.sendMessage(Component.text("Amount must be at least 1.", NamedTextColor.RED));
    }

    private void saveAndConfirm(CommandSender sender, Player target, String detail) {
        data.saveIfDirty();
        sender.sendMessage(Component.text(target.getName() + ": " + detail, NamedTextColor.AQUA));
        target.sendMessage(Component.text("Your Energy is now " + energy.get(target) + "/"
                + energy.getMaximum(target) + ".", NamedTextColor.AQUA));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.addAll(List.of("powers", "select", "guide"));
            if (sender.hasPermission("highenergy.admin")) {
                choices.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            Arrays.stream(Power.values()).map(power -> power.name().toLowerCase(Locale.ROOT)).forEach(choices::add);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("guide")) {
            choices.addAll(List.of("book", "information"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("guide")) {
            choices.addAll(List.of("english", "hindi"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            choices.addAll(List.of("give", "take", "set", "refill", "setmax"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(choices::add);
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
