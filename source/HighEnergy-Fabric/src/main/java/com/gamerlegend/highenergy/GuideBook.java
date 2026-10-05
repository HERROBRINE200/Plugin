package com.gamerlegend.highenergy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * The in-game guide.
 *
 * <p>The original mod handed out a written book without a single page. This version
 * writes real pages (English and Hindi) listing every power, its cost and its cooldown.</p>
 */
public final class GuideBook {

    private GuideBook() {
    }

    public static boolean isHindi(String language) {
        return language != null
                && (language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi"));
    }

    /** Gives a fully written guide book to the player. */
    public static void give(ServerPlayer player, String language) {
        boolean hindi = isHindi(language);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : pages(hindi)) {
            pages.add(Filterable.passThrough(Component.literal(page)));
        }
        String title = hindi ? "\u26a1 High Energy Guide (Hindi)" : "\u26a1 High Energy Guide (English)";
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(title), "Gamerlegend", 0, pages, true));
        book.set(DataComponents.CUSTOM_NAME, Component.literal(hindi
                ? "\u00a7b\u26a1 High Energy Guide \u2014 Hindi"
                : "\u00a7b\u26a1 High Energy Guide \u2014 English"));

        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        sendInformation(player, hindi);
    }

    /** Guide text in chat, used by {@code /energy} and {@code /energy guide information}. */
    public static void sendInformation(ServerPlayer player, boolean hindi) {
        for (String line : information(hindi).split("\n")) {
            player.sendSystemMessage(Component.literal(line));
        }
    }

    public static String information(boolean hindi) {
        String rate = EnergyManager.REGEN_AMOUNT + " / "
                + (EnergyManager.REGEN_INTERVAL_TICKS / 20.0) + "s";
        if (hindi) {
            return "\u00a7b\u00a7l\u26a1 HIGH ENERGY GUIDE \u26a1\n"
                    + "\u00a7fHar player ke paas \u00a7b" + EnergyManager.MAX_DEFAULT + "\u00a7f Energy hoti hai.\n"
                    + "\u00a77Energy apne aap regenerate hoti hai (\u00a7f" + rate + "\u00a77).\n"
                    + "\u00a7fSneak + Use \u00a77se selected power activate hoti hai.\n"
                    + "\u00a7f/energy powers \u00a77se saari powers dekho.\n"
                    + "\u00a7f/energy select <power> \u00a77se power choose karo.\n"
                    + "\u00a7f/energy guide book hindi \u00a77se guide book lo.";
        }
        return "\u00a7b\u00a7l\u26a1 HIGH ENERGY GUIDE \u26a1\n"
                + "\u00a7fEvery player starts with \u00a7b" + EnergyManager.MAX_DEFAULT + "\u00a7f Energy.\n"
                + "\u00a77Energy regenerates automatically (\u00a7f" + rate + "\u00a77).\n"
                + "\u00a7fSneak + Use \u00a77activates the selected power.\n"
                + "\u00a7f/energy powers \u00a77lists powers.\n"
                + "\u00a7f/energy select <power> \u00a77selects a power.\n"
                + "\u00a7f/energy guide book english \u00a77gives you the guide book.";
    }

    /** Builds the actual book pages. */
    public static List<String> pages(boolean hindi) {
        List<String> pages = new ArrayList<>();
        int max = EnergyManager.MAX_DEFAULT;
        String rate = EnergyManager.REGEN_AMOUNT + " per "
                + (EnergyManager.REGEN_INTERVAL_TICKS / 20.0) + "s";

        if (hindi) {
            pages.add("\u00a70\u00a7l\u26a1 HIGH ENERGY\n\n"
                    + "\u00a78Ek Energy aur powers ka system.\n\n"
                    + "\u00a70Max Energy: \u00a78" + max + "\n"
                    + "\u00a70Regen: \u00a78" + rate + "\n\n"
                    + "\u00a78Agla page padho \u2192");
            pages.add("\u00a70\u00a7lKAISE KHELEN\n\n"
                    + "\u00a781. \u00a70/energy powers\n\u00a78   se powers dekho.\n"
                    + "\u00a782. \u00a70/energy select <power>\n\u00a78   se power choose karo.\n"
                    + "\u00a783. \u00a78Sneak (Shift) dabakar\n   use karo - power\n   activate ho jayegi.\n"
                    + "\u00a784. \u00a78Energy khatam ho to\n   thoda wait karo.");
        } else {
            pages.add("\u00a70\u00a7l\u26a1 HIGH ENERGY\n\n"
                    + "\u00a78An Energy & powers system.\n\n"
                    + "\u00a70Max Energy: \u00a78" + max + "\n"
                    + "\u00a70Regen: \u00a78" + rate + "\n\n"
                    + "\u00a78Turn the page \u2192");
            pages.add("\u00a70\u00a7lHOW TO PLAY\n\n"
                    + "\u00a781. \u00a70/energy powers\n\u00a78   lists all powers.\n"
                    + "\u00a782. \u00a70/energy select <power>\n\u00a78   picks your power.\n"
                    + "\u00a783. \u00a78Hold Sneak (Shift)\n   and use to fire it.\n"
                    + "\u00a784. \u00a78Out of Energy? Wait,\n   it refills over time.");
        }

        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (Power power : Power.values()) {
            if (index % 4 == 0) {
                if (sb.length() > 0) {
                    pages.add(sb.toString());
                }
                sb = new StringBuilder("\u00a70\u00a7lPOWERS\n\n");
            }
            sb.append("\u00a70").append(power.display).append('\n')
                    .append("\u00a78 ").append(hindi ? "Kharcha" : "Cost").append(": ").append(power.cost)
                    .append("  CD: ").append(power.cooldownTicks / 20.0).append("s\n\n");
            index++;
        }
        if (sb.length() > 0) {
            pages.add(sb.toString());
        }

        pages.add("\u00a70\u00a7lCOMMANDS\n\n"
                + "\u00a78/energy\n/energy powers\n/energy select <power>\n"
                + "/energy guide book\n   <english|hindi>\n"
                + "/energy guide information\n   <english|hindi>\n\n"
                + "\u00a70Admin:\n\u00a78/energy admin ...");
        return pages;
    }
}
