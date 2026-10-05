package com.gamerlegend.highenergy.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.Map;

final class GuideBookService {
    private GuideBookService() {
    }

    static boolean isSupportedLanguage(String language) {
        return language.equalsIgnoreCase("english") || language.equalsIgnoreCase("en")
                || language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi");
    }

    static void give(Player player, String language) {
        boolean hindi = language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi");
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Component.text(hindi ? "High Energy Guide - Hindi" : "High Energy Guide",
                NamedTextColor.AQUA));
        meta.author(Component.text("Gamerlegend"));
        meta.pages(hindi ? hindiPages() : englishPages());
        meta.setGeneration(BookMeta.Generation.ORIGINAL);
        book.setItemMeta(meta);

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(book);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(Component.text("High Energy guide added to your inventory.", NamedTextColor.AQUA));
    }

    private static List<Component> englishPages() {
        return List.of(
                page("HIGH ENERGY\n\nEvery player starts with 200 Energy. Energy regenerates by 3 each second. Your Energy, maximum, and selected power persist after restarts."),
                page("ACTIVATION\n\nSneak + left-click air, a block, or an entity to activate. Sneak + right-click can be enabled as a fallback.\n\nSelect: /energy select <power>"),
                page("DASH - 15 E, 1.5s\nLaunch forward.\n\nSTRIKE - 20 E, 2s\nStrength II for 4s.\n\nSHOCKWAVE - 30 E, 3s\n5-block radius, 5 damage and knockback."),
                page("SHIELD - 25 E, 5s\nResistance III for 5s.\n\nLIGHTNING - 40 E, 5s\nStrike a target, or 8 blocks ahead.\n\nPULL - 25 E, 3s\nPull a targeted living entity."),
                page("BLAST - 35 E, 2.5s\n7-block radius, 7 damage and knockback.\n\nSPEED - 20 E, 4s\nSpeed IV for 6s.\n\nREGEN - 30 E, 5s\nRegeneration III for 5s."),
                page("OVERDRIVE - 80 E, 15s\nSpeed IV, Strength III and Resistance II for 8 seconds.\n\nUse /energy powers for the live power list."),
                page("PLAYER COMMANDS\n/energy\n/energy powers\n/energy select <power>\n/energy guide book <language>\n/energy guide information <language>"),
                page("ADMIN COMMANDS\n/energy admin give <player> <amount>\n... take ...\n... set ...\n... refill <player>\n... setmax <player> <amount>\n\nPermission: highenergy.admin")
        );
    }

    private static List<Component> hindiPages() {
        return List.of(
                page("HIGH ENERGY\n\nHar player 200 Energy se shuru karta hai. Har second 3 Energy wapas aati hai. Energy, maximum aur selected power restart ke baad bhi save rehte hain."),
                page("ACTIVATION\n\nSneak + left-click air, entity ya block par power chalata hai. Config mein right-click fallback bhi hai.\n\nSelect: /energy select <power>"),
                page("DASH - 15 E, 1.5s\nAage tez launch.\n\nSTRIKE - 20 E, 2s\n4s Strength II.\n\nSHOCKWAVE - 30 E, 3s\n5-block range, 5 damage aur knockback."),
                page("SHIELD - 25 E, 5s\n5s Resistance III.\n\nLIGHTNING - 40 E, 5s\nTarget ya 8 block aage bijli.\n\nPULL - 25 E, 3s\nTarget ko apni taraf kheenchta hai."),
                page("BLAST - 35 E, 2.5s\n7-block range, 7 damage.\n\nSPEED - 20 E, 4s\n6s Speed IV.\n\nREGEN - 30 E, 5s\n5s Regeneration III."),
                page("OVERDRIVE - 80 E, 15s\n8s ke liye Speed IV, Strength III aur Resistance II.\n\nPuri list: /energy powers"),
                page("PLAYER COMMANDS\n/energy\n/energy powers\n/energy select <power>\n/energy guide book english|hindi\n/energy guide information english|hindi"),
                page("ADMIN\ngive, take, set, refill aur setmax commands /energy admin ke andar hain.\n\nPermission: highenergy.admin")
        );
    }

    private static Component page(String text) {
        return Component.text(text, NamedTextColor.BLACK);
    }
}
