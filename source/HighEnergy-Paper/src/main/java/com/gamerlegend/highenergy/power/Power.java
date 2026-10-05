package com.gamerlegend.highenergy.power;

import java.util.Locale;

/**
 * The ten High Energy powers.
 *
 * <p>Display names, Energy costs and cooldowns are identical to the original
 * Fabric mod. The values declared here are the <strong>defaults</strong>; they can be
 * overridden per server in {@code config.yml} (see {@code powers:} section).</p>
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

    private final String display;
    private final int defaultCost;
    private final int defaultCooldownTicks;

    Power(String display, int defaultCost, int defaultCooldownTicks) {
        this.display = display;
        this.defaultCost = defaultCost;
        this.defaultCooldownTicks = defaultCooldownTicks;
    }

    public String display() {
        return display;
    }

    public int defaultCost() {
        return defaultCost;
    }

    public int defaultCooldownTicks() {
        return defaultCooldownTicks;
    }

    /** Lower-case id used in commands, config keys and tab completion. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** @return the matching power, or {@code null} when the name is unknown. */
    public static Power byName(String raw) {
        if (raw == null) {
            return null;
        }
        String needle = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (Power power : values()) {
            if (power.name().equals(needle)) {
                return power;
            }
        }
        // also allow matching on the display name, e.g. "Energy Dash"
        for (Power power : values()) {
            if (power.display.toUpperCase(Locale.ROOT).replace(' ', '_').equals(needle)) {
                return power;
            }
        }
        return null;
    }
}
