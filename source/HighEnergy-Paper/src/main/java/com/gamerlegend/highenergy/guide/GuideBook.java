package com.gamerlegend.highenergy.guide;

import com.gamerlegend.highenergy.HighEnergyConfig;
import com.gamerlegend.highenergy.Msg;
import com.gamerlegend.highenergy.power.Power;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The in-game guide.
 *
 * <p>The mod handed out an empty written book; this version writes real, readable pages
 * (English and Hindi) that document every power, its cost and its cooldown.</p>
 */
public final class GuideBook {

    private final HighEnergyConfig config;

    public GuideBook(HighEnergyConfig config) {
        this.config = config;
    }

    public static boolean isHindi(String language) {
        return language != null
                && (language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi"));
    }

    /** Gives a fully written guide book to the player. */
    public void give(Player player, String language) {
        boolean hindi = isHindi(language);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.title(Component.text(hindi
                    ? "\u26a1 High Energy Guide (Hindi)"
                    : "\u26a1 High Energy Guide (English)"));
            meta.author(Component.text("Gamerlegend"));
            meta.displayName(Msg.of(hindi
                    ? "\u00a7b\u26a1 High Energy Guide \u2014 Hindi"
                    : "\u00a7b\u26a1 High Energy Guide \u2014 English"));
            List<Component> pages = new ArrayList<>();
            for (String page : pages(hindi)) {
                pages.add(Msg.of(page));
            }
            meta.pages(pages);
            book.setItemMeta(meta);
        }
        player.getInventory().addItem(book);
        Msg.sendLines(player, information(hindi));
    }

    /** Short chat version used by {@code /energy} and {@code /energy guide information}. */
    public String information(boolean hindi) {
        int max = config.defaultMaxEnergy();
        String rate = config.regenAmount() + " / " + (config.regenIntervalTicks() / 20.0) + "s";
        if (hindi) {
            return "\u00a7b\u00a7l\u26a1 HIGH ENERGY GUIDE \u26a1\n"
                    + "\u00a7fHar player ke paas \u00a7b" + max + "\u00a7f Energy hoti hai.\n"
                    + "\u00a77Energy apne aap regenerate hoti hai (\u00a7f" + rate + "\u00a77).\n"
                    + "\u00a7fSneak + Click \u00a77se selected power activate hoti hai.\n"
                    + "\u00a7f/energy powers \u00a77se saari powers dekho.\n"
                    + "\u00a7f/energy select <power> \u00a77se power choose karo.\n"
                    + "\u00a7f/energy guide book hindi \u00a77se guide book lo.";
        }
        return "\u00a7b\u00a7l\u26a1 HIGH ENERGY GUIDE \u26a1\n"
                + "\u00a7fEvery player starts with \u00a7b" + max + "\u00a7f Energy.\n"
                + "\u00a77Energy regenerates automatically (\u00a7f" + rate + "\u00a77).\n"
                + "\u00a7fSneak + Click \u00a77activates the selected power.\n"
                + "\u00a7f/energy powers \u00a77lists powers.\n"
                + "\u00a7f/energy select <power> \u00a77selects a power.\n"
                + "\u00a7f/energy guide book english \u00a77gives you the guide book.";
    }

    /** Builds the actual book pages. */
    public List<String> pages(boolean hindi) {
        List<String> pages = new ArrayList<>();
        int max = config.defaultMaxEnergy();
        String rate = config.regenAmount() + " per " + (config.regenIntervalTicks() / 20.0) + "s";

        if (hindi) {
            pages.add("\u00a70\u00a7l\u26a1 HIGH ENERGY\n\n"
                    + "\u00a78Ek Energy aur powers ka system.\n\n"
                    + "\u00a70Max Energy: \u00a78" + max + "\n"
                    + "\u00a70Regen: \u00a78" + rate + "\n\n"
                    + "\u00a78Agla page padho \u2192");
            pages.add("\u00a70\u00a7lKAISE KHELEN\n\n"
                    + "\u00a781. \u00a70/energy powers\n\u00a78   se powers dekho.\n"
                    + "\u00a782. \u00a70/energy select <power>\n\u00a78   se power choose karo.\n"
                    + "\u00a783. \u00a78Sneak (Shift) dabakar\n   click karo \u2014 power\n   activate ho jayegi.\n"
                    + "\u00a784. \u00a78Energy khatam ho to\n   ruk kar wait karo.");
        } else {
            pages.add("\u00a70\u00a7l\u26a1 HIGH ENERGY\n\n"
                    + "\u00a78An Energy & powers system.\n\n"
                    + "\u00a70Max Energy: \u00a78" + max + "\n"
                    + "\u00a70Regen: \u00a78" + rate + "\n\n"
                    + "\u00a78Turn the page \u2192");
            pages.add("\u00a70\u00a7lHOW TO PLAY\n\n"
                    + "\u00a781. \u00a70/energy powers\n\u00a78   lists all powers.\n"
                    + "\u00a782. \u00a70/energy select <power>\n\u00a78   picks your power.\n"
                    + "\u00a783. \u00a78Hold Sneak (Shift)\n   and click to fire it.\n"
                    + "\u00a784. \u00a78Out of Energy? Wait,\n   it refills over time.");
        }

        // Power pages: four powers per page.
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Power power : Power.values()) {
            if (index % 4 == 0) {
                if (sb.length() > 0) {
                    pages.add(sb.toString());
                }
                sb = new StringBuilder("\u00a70\u00a7l" + (hindi ? "POWERS" : "POWERS") + "\n\n");
            }
            sb.append("\u00a70").append(power.display()).append('\n')
                    .append("\u00a78 ").append(hindi ? "Kharcha" : "Cost").append(": ")
                    .append(config.cost(power))
                    .append("  CD: ")
                    .append(config.cooldownTicks(power) / 20.0).append("s\n\n");
            index++;
        }
        if (sb.length() > 0) {
            pages.add(sb.toString());
        }

        pages.add("\u00a70\u00a7l" + (hindi ? "COMMANDS" : "COMMANDS") + "\n\n"
                + "\u00a78/energy\n/energy powers\n/energy select <power>\n"
                + "/energy guide book\n   <english|hindi>\n"
                + "/energy guide information\n   <english|hindi>\n\n"
                + "\u00a70Admin:\n\u00a78/energy admin ...");
        return pages;
    }

    public static String normalizeLanguage(String raw) {
        return raw == null ? "english" : raw.toLowerCase(Locale.ROOT);
    }
}
