package com.gamerlegend.highenergy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Small helper that keeps the original mod's legacy colour codes ({@code §b}, {@code &b})
 * working while still talking to Paper through Adventure components.
 */
public final class Msg {

    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private Msg() {
    }

    public static Component of(String legacy) {
        if (legacy == null) {
            return Component.empty();
        }
        return legacy.indexOf('&') >= 0 && legacy.indexOf('\u00a7') < 0
                ? AMPERSAND.deserialize(legacy)
                : SECTION.deserialize(legacy);
    }

    public static void send(CommandSender sender, String legacy) {
        sender.sendMessage(of(legacy));
    }

    /** Sends a multi-line message; the mod used {@code \n} inside a single chat line. */
    public static void sendLines(CommandSender sender, String legacyWithNewLines) {
        for (String line : legacyWithNewLines.split("\n")) {
            sender.sendMessage(of(line));
        }
    }
}
