package com.gamerlegend.highenergy.command;

import com.gamerlegend.highenergy.HighEnergyConfig;
import com.gamerlegend.highenergy.HighEnergyPlugin;
import com.gamerlegend.highenergy.Msg;
import com.gamerlegend.highenergy.energy.EnergyManager;
import com.gamerlegend.highenergy.guide.GuideBook;
import com.gamerlegend.highenergy.power.Power;
import com.gamerlegend.highenergy.power.PowerSystem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /energy} - the complete command tree of the mod, ported to Bukkit.
 *
 * <pre>
 * /energy
 * /energy powers
 * /energy select &lt;power&gt;
 * /energy guide book &lt;english|hindi&gt;
 * /energy guide information &lt;english|hindi&gt;
 * /energy admin give|take|set|setmax &lt;player&gt; &lt;amount&gt;
 * /energy admin refill &lt;player&gt;
 * /energy reload
 * </pre>
 */
public final class EnergyCommand implements CommandExecutor, TabCompleter {

    public static final String PERM_USE = "highenergy.use";
    public static final String PERM_ADMIN = "highenergy.admin";

    private final HighEnergyPlugin plugin;
    private final EnergyManager energy;
    private final PowerSystem powers;
    private final GuideBook guide;

    public EnergyCommand(HighEnergyPlugin plugin, EnergyManager energy, PowerSystem powers, GuideBook guide) {
        this.plugin = plugin;
        this.energy = energy;
        this.powers = powers;
        this.guide = guide;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return status(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "powers":
                return powers(sender);
            case "select":
                return select(sender, args);
            case "guide":
                return guide(sender, args);
            case "admin":
                return admin(sender, args);
            case "reload":
                return reload(sender);
            case "help":
                return help(sender);
            default:
                Msg.send(sender, "\u00a7cUnknown sub-command. Try \u00a7f/" + label + " help");
                return true;
        }
    }

    // ------------------------------------------------------------------ /energy

    private boolean status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "\u00a7cOnly players have an Energy pool. Use \u00a7f/energy admin ...");
            return true;
        }
        if (!player.hasPermission(PERM_USE)) {
            return denied(sender);
        }
        Msg.sendLines(player, guide.information(false));
        Msg.send(player, energy.bar(player));
        Power selected = powers.selected(player);
        Msg.send(player, "\u00a7b\u26a1 Selected: \u00a7f" + selected.display()
                + " \u00a77| Cost: \u00a7b" + plugin.config().cost(selected)
                + " \u00a77| Cooldown: \u00a7f" + (plugin.config().cooldownTicks(selected) / 20.0) + "s");
        return true;
    }

    private boolean help(CommandSender sender) {
        Msg.sendLines(sender, "\u00a7b\u00a7l\u26a1 HIGH ENERGY COMMANDS\n"
                + "\u00a7f/energy \u00a77- your Energy and selected power\n"
                + "\u00a7f/energy powers \u00a77- list all powers\n"
                + "\u00a7f/energy select <power> \u00a77- choose a power\n"
                + "\u00a7f/energy guide book <english|hindi> \u00a77- get the guide book\n"
                + "\u00a7f/energy guide information <english|hindi> \u00a77- guide in chat");
        if (sender.hasPermission(PERM_ADMIN)) {
            Msg.sendLines(sender, "\u00a7f/energy admin give <player> <amount>\n"
                    + "\u00a7f/energy admin take <player> <amount>\n"
                    + "\u00a7f/energy admin set <player> <amount>\n"
                    + "\u00a7f/energy admin refill <player>\n"
                    + "\u00a7f/energy admin setmax <player> <amount>\n"
                    + "\u00a7f/energy reload \u00a77- reload config.yml");
        }
        return true;
    }

    // ------------------------------------------------------------------ /energy powers

    private boolean powers(CommandSender sender) {
        if (!sender.hasPermission(PERM_USE)) {
            return denied(sender);
        }
        HighEnergyConfig cfg = plugin.config();
        Msg.send(sender, "\u00a7b\u00a7l\u26a1 POWERS");
        for (Power power : Power.values()) {
            Msg.send(sender, "\u00a7f" + power.display() + " \u00a78(" + power.id() + ") "
                    + "\u00a77\u2014 Cost: \u00a7b" + cfg.cost(power)
                    + " \u00a77Cooldown: \u00a7f" + (cfg.cooldownTicks(power) / 20.0) + "s");
        }
        return true;
    }

    // ------------------------------------------------------------------ /energy select

    private boolean select(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "\u00a7cOnly players can select a power.");
            return true;
        }
        if (!player.hasPermission(PERM_USE)) {
            return denied(sender);
        }
        if (args.length < 2) {
            Msg.send(sender, "\u00a7cUsage: \u00a7f/energy select <power>");
            return true;
        }
        Power power = Power.byName(args[1]);
        if (power == null) {
            Msg.send(sender, "\u00a7cUnknown power. \u00a77Use \u00a7f/energy powers");
            return true;
        }
        powers.select(player, power);
        return true;
    }

    // ------------------------------------------------------------------ /energy guide

    private boolean guide(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_USE)) {
            return denied(sender);
        }
        if (args.length < 2) {
            Msg.send(sender, "\u00a7cUsage: \u00a7f/energy guide <book|information> <english|hindi>");
            return true;
        }
        String language = args.length >= 3 ? args[2] : "english";
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "book" -> {
                if (!(sender instanceof Player player)) {
                    Msg.send(sender, "\u00a7cOnly players can receive the guide book.");
                    return true;
                }
                guide.give(player, language);
            }
            case "information", "info" -> Msg.sendLines(sender, guide.information(GuideBook.isHindi(language)));
            default -> Msg.send(sender, "\u00a7cUsage: \u00a7f/energy guide <book|information> <english|hindi>");
        }
        return true;
    }

    // ------------------------------------------------------------------ /energy admin

    private boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            return denied(sender);
        }
        if (args.length < 3) {
            Msg.send(sender, "\u00a7cUsage: \u00a7f/energy admin <give|take|set|refill|setmax> <player> [amount]");
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Msg.send(sender, "\u00a7cPlayer \u00a7f" + args[2] + " \u00a7cis not online.");
            return true;
        }
        energy.ensure(target);

        if (action.equals("refill")) {
            energy.refill(target);
            Msg.send(sender, "\u00a7b\u26a1 Refilled \u00a7f" + target.getName()
                    + "\u00a7b to \u00a7f" + energy.getMax(target) + " \u00a7bEnergy.");
            Msg.send(target, "\u00a7b\u26a1 Your Energy has been refilled.");
            return true;
        }

        if (args.length < 4) {
            Msg.send(sender, "\u00a7cUsage: \u00a7f/energy admin " + action + " <player> <amount>");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            Msg.send(sender, "\u00a7cAmount must be a whole number.");
            return true;
        }

        switch (action) {
            case "give" -> {
                if (amount < 1) {
                    Msg.send(sender, "\u00a7cAmount must be at least 1.");
                    return true;
                }
                energy.add(target, amount);
                Msg.send(sender, "\u00a7b\u26a1 Gave \u00a7f" + amount + " \u00a7bEnergy to \u00a7f" + target.getName());
                Msg.send(target, "\u00a7b\u26a1 You received \u00a7f" + amount + " \u00a7bEnergy.");
            }
            case "take" -> {
                if (amount < 1) {
                    Msg.send(sender, "\u00a7cAmount must be at least 1.");
                    return true;
                }
                energy.add(target, -amount);
                Msg.send(sender, "\u00a7b\u26a1 Took \u00a7f" + amount + " \u00a7bEnergy from \u00a7f" + target.getName());
                Msg.send(target, "\u00a7b\u26a1 \u00a7f" + amount + " \u00a7bEnergy was taken from you.");
            }
            case "set" -> {
                if (amount < 0) {
                    Msg.send(sender, "\u00a7cAmount cannot be negative.");
                    return true;
                }
                energy.set(target, amount);
                Msg.send(sender, "\u00a7b\u26a1 Set \u00a7f" + target.getName() + "\u00a7b's Energy to \u00a7f"
                        + energy.get(target));
            }
            case "setmax" -> {
                if (amount < 1) {
                    Msg.send(sender, "\u00a7cMaximum Energy must be at least 1.");
                    return true;
                }
                energy.setMax(target, amount);
                Msg.send(sender, "\u00a7b\u26a1 Set \u00a7f" + target.getName() + "\u00a7b's maximum Energy to \u00a7f"
                        + energy.getMax(target));
                Msg.send(target, "\u00a7b\u26a1 Your maximum Energy is now \u00a7f" + energy.getMax(target));
            }
            default -> Msg.send(sender, "\u00a7cUnknown admin action: \u00a7f" + action);
        }
        plugin.store().save();
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            return denied(sender);
        }
        plugin.reloadEverything();
        Msg.send(sender, "\u00a7b\u26a1 High Energy configuration reloaded.");
        return true;
    }

    private boolean denied(CommandSender sender) {
        Msg.send(sender, "\u00a7cYou do not have permission to do that.");
        return true;
    }

    // ------------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addMatches(out, args[0], "powers", "select", "guide", "help");
            if (sender.hasPermission(PERM_ADMIN)) {
                addMatches(out, args[0], "admin", "reload");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "select" -> {
                    for (Power power : Power.values()) {
                        addMatches(out, args[1], power.id());
                    }
                }
                case "guide" -> addMatches(out, args[1], "book", "information");
                case "admin" -> addMatches(out, args[1], "give", "take", "set", "refill", "setmax");
                default -> {
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("guide")) {
                addMatches(out, args[2], "english", "hindi");
            } else if (args[0].equalsIgnoreCase("admin")) {
                for (OfflinePlayer player : Bukkit.getOnlinePlayers()) {
                    if (player.getName() != null) {
                        addMatches(out, args[2], player.getName());
                    }
                }
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            addMatches(out, args[3], "10", "50", "100", "200");
        }
        return out;
    }

    private void addMatches(List<String> out, String prefix, String... options) {
        String needle = prefix.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(option);
            }
        }
    }
}
