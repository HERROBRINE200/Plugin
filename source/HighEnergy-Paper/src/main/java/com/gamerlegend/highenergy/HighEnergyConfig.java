package com.gamerlegend.highenergy;

import com.gamerlegend.highenergy.power.Power;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Typed view over {@code config.yml}.
 *
 * <p>Every value defaults to the behaviour of the original Fabric mod, so an empty
 * configuration file reproduces the mod exactly.</p>
 */
public final class HighEnergyConfig {

    private final int defaultMaxEnergy;
    private final int regenAmount;
    private final int regenIntervalTicks;
    private final boolean actionBarEnabled;
    private final boolean activateOnRightClick;
    private final boolean activateOnLeftClick;
    private final boolean requireSneak;
    private final int autoSaveIntervalTicks;

    private final Map<Power, Integer> costs = new EnumMap<>(Power.class);
    private final Map<Power, Integer> cooldowns = new EnumMap<>(Power.class);

    public HighEnergyConfig(FileConfiguration cfg) {
        this.defaultMaxEnergy = Math.max(1, cfg.getInt("energy.default-max", 200));
        this.regenAmount = Math.max(0, cfg.getInt("energy.regen-amount", 3));
        this.regenIntervalTicks = Math.max(1, cfg.getInt("energy.regen-interval-ticks", 10));
        this.actionBarEnabled = cfg.getBoolean("energy.action-bar", true);
        this.activateOnRightClick = cfg.getBoolean("activation.sneak-right-click", true);
        this.activateOnLeftClick = cfg.getBoolean("activation.sneak-left-click", true);
        this.requireSneak = cfg.getBoolean("activation.require-sneak", true);
        this.autoSaveIntervalTicks = Math.max(200, cfg.getInt("storage.auto-save-interval-ticks", 6000));

        ConfigurationSection powers = cfg.getConfigurationSection("powers");
        for (Power power : Power.values()) {
            int cost = power.defaultCost();
            int cooldown = power.defaultCooldownTicks();
            if (powers != null) {
                ConfigurationSection section = powers.getConfigurationSection(power.id());
                if (section != null) {
                    cost = Math.max(0, section.getInt("cost", cost));
                    cooldown = Math.max(0, section.getInt("cooldown-ticks", cooldown));
                }
            }
            costs.put(power, cost);
            cooldowns.put(power, cooldown);
        }
    }

    public int defaultMaxEnergy() {
        return defaultMaxEnergy;
    }

    public int regenAmount() {
        return regenAmount;
    }

    public int regenIntervalTicks() {
        return regenIntervalTicks;
    }

    public boolean actionBarEnabled() {
        return actionBarEnabled;
    }

    public boolean activateOnRightClick() {
        return activateOnRightClick;
    }

    public boolean activateOnLeftClick() {
        return activateOnLeftClick;
    }

    public boolean requireSneak() {
        return requireSneak;
    }

    public int autoSaveIntervalTicks() {
        return autoSaveIntervalTicks;
    }

    public int cost(Power power) {
        Integer value = costs.get(power);
        return value == null ? power.defaultCost() : value;
    }

    public int cooldownTicks(Power power) {
        Integer value = cooldowns.get(power);
        return value == null ? power.defaultCooldownTicks() : value;
    }
}
