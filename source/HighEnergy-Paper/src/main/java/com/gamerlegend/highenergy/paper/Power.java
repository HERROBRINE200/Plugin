package com.gamerlegend.highenergy.paper;

import java.util.Locale;
import java.util.Optional;

/** The original ten High Energy powers and their exact costs/cooldowns. */
public enum Power {
    DASH("Energy Dash", 15, 30, "Launches you forward."),
    STRIKE("Energy Strike", 20, 40, "Strength II for 4 seconds."),
    SHOCKWAVE("Shockwave", 30, 60, "5-block wave: 5 damage and strong knockback."),
    SHIELD("Energy Shield", 25, 100, "Resistance III for 5 seconds."),
    LIGHTNING("Lightning Strike", 40, 100, "Calls lightning on a target or 8 blocks ahead."),
    PULL("Energy Pull", 25, 60, "Pulls the targeted living entity toward you."),
    BLAST("Energy Blast", 35, 50, "7-block blast: 7 damage and knockback."),
    SPEED("Speed Surge", 20, 80, "Speed IV for 6 seconds."),
    REGEN("Regeneration Burst", 30, 100, "Regeneration III for 5 seconds."),
    OVERDRIVE("Energy Overdrive", 80, 300, "Speed IV, Strength III and Resistance II for 8 seconds.");

    public final String display;
    public final int cost;
    public final int cooldownTicks;
    public final String description;

    Power(String display, int cost, int cooldownTicks, String description) {
        this.display = display;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
        this.description = description;
    }

    public double cooldownSeconds() {
        return cooldownTicks / 20.0;
    }

    public static Optional<Power> fromInput(String input) {
        String normalized = input.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        try {
            return Optional.of(valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
