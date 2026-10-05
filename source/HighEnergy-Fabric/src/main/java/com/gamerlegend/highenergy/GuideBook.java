package com.gamerlegend.highenergy;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.List;

public final class GuideBook {
    private GuideBook() {
    }

    public static boolean isSupportedLanguage(String language) {
        return language.equalsIgnoreCase("english") || language.equalsIgnoreCase("en")
                || language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi");
    }

    public static void give(ServerPlayer player, String language) {
        boolean hindi = language.equalsIgnoreCase("hindi") || language.equalsIgnoreCase("hi");
        String title = hindi ? "High Energy Guide - Hindi" : "High Energy Guide";
        List<Component> pages = hindi ? hindiPages() : englishPages();

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title),
                "Gamerlegend",
                0,
                pages.stream().map(Filterable::passThrough).toList(),
                true
        ));

        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        player.sendSystemMessage(Component.literal("High Energy guide added to your inventory.")
                .withStyle(ChatFormatting.AQUA));
    }

    private static List<Component> englishPages() {
        return List.of(
                page("HIGH ENERGY\n\nEvery player starts with 200 Energy. Energy regenerates by 3 each second. Your Energy, maximum, and selected power persist after restarts."),
                page("ACTIVATION\n\nSneak + left-click an entity or block to activate. Sneak + right-click is also supported as an air-use fallback.\n\nSelect: /energy select <power>"),
                page("DASH - 15 E, 1.5s\nLaunch forward.\n\nSTRIKE - 20 E, 2s\nStrength II for 4s.\n\nSHOCKWAVE - 30 E, 3s\n5-block radius, 5 damage and knockback."),
                page("SHIELD - 25 E, 5s\nResistance III for 5s.\n\nLIGHTNING - 40 E, 5s\nStrike a target, or 8 blocks ahead.\n\nPULL - 25 E, 3s\nPull a targeted living entity."),
                page("BLAST - 35 E, 2.5s\n7-block radius, 7 damage and knockback.\n\nSPEED - 20 E, 4s\nSpeed IV for 6s.\n\nREGEN - 30 E, 5s\nRegeneration III for 5s."),
                page("OVERDRIVE - 80 E, 15s\nSpeed IV, Strength III and Resistance II for 8 seconds.\n\nUse /energy powers for the live power list."),
                page("PLAYER COMMANDS\n/energy\n/energy powers\n/energy select <power>\n/energy guide book <language>\n/energy guide information <language>"),
                page("ADMIN COMMANDS (OP 2)\n/energy admin give <player> <amount>\n... take ...\n... set ...\n... refill <player>\n... setmax <player> <amount>")
        );
    }

    private static List<Component> hindiPages() {
        return List.of(
                page("HIGH ENERGY\n\nHar player 200 Energy se shuru karta hai. Har second 3 Energy wapas aati hai. Energy, maximum aur selected power restart ke baad bhi save rehte hain."),
                page("ACTIVATION\n\nSneak + left-click entity ya block par power chalata hai. Air fallback ke liye sneak + right-click bhi chalega.\n\nSelect: /energy select <power>"),
                page("DASH - 15 E, 1.5s\nAage tez launch.\n\nSTRIKE - 20 E, 2s\n4s Strength II.\n\nSHOCKWAVE - 30 E, 3s\n5-block range, 5 damage aur knockback."),
                page("SHIELD - 25 E, 5s\n5s Resistance III.\n\nLIGHTNING - 40 E, 5s\nTarget ya 8 block aage bijli.\n\nPULL - 25 E, 3s\nTarget ko apni taraf kheenchta hai."),
                page("BLAST - 35 E, 2.5s\n7-block range, 7 damage.\n\nSPEED - 20 E, 4s\n6s Speed IV.\n\nREGEN - 30 E, 5s\n5s Regeneration III."),
                page("OVERDRIVE - 80 E, 15s\n8s ke liye Speed IV, Strength III aur Resistance II.\n\nPuri list: /energy powers"),
                page("PLAYER COMMANDS\n/energy\n/energy powers\n/energy select <power>\n/energy guide book english|hindi\n/energy guide information english|hindi"),
                page("ADMIN (OP 2)\ngive, take, set, refill aur setmax commands /energy admin ke andar milte hain. Admin madad ke liye /energy admin use karein.")
        );
    }

    private static Component page(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BLACK);
    }
}
