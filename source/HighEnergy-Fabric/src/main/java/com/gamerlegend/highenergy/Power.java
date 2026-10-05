package com.gamerlegend.highenergy;

import java.util.Locale;

/**
 * The ten High Energy powers with their Energy cost and cooldown (in ticks).
 */
public enum Power {

    DASH("Energy Dash", 15, 30),
    STRIKE("Energy Strike", 20, 40),
    SHOCKWAVE("Shockwave", 30, 60),
    SHIELD("Energy Shield", 25, 100),
    LIGHTNING("Lightning Strike", 40, 100),
    PULL("Energy Pull", 25, 60),
    BLAST("Energy Blast", 35, 50),
    SPEED("Speed Surge", 20, 80),
    REGEN("Regeneration Burst", 30, 100),
    OVERDRIVE("Energy Overdrive", 80, 300);

    public final String display;
    public final int cost;
    public final int cooldownTicks;

    Power(String display, int cost, int cooldownTicks) {
        this.display = display;
        this.cost = cost;
        this.cooldownTicks = cooldownTicks;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the power or {@code null} if the name is unknown. */
    public static Power byName(String raw) {
        if (raw == null) {
            return null;
        }
        String needle = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (Power power : values()) {
            if (power.name().equals(needle)
                    || power.display.toUpperCase(Locale.ROOT).replace(' ', '_').equals(needle)) {
                return power;
            }
        }
        return null;
    }
}
