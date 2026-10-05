package com.gamerlegend.highenergy.paper;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class HighEnergyPlugin extends JavaPlugin {
    private PlayerDataStore data;
    private EnergyService energy;
    private PowerService powers;
    private BukkitTask tickTask;
    private long ticks;
    private int autosaveInterval;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        int defaultMaximum = Math.max(1, getConfig().getInt("energy.default-maximum", 200));
        int regenerationAmount = Math.max(0, getConfig().getInt("energy.regeneration-amount", 3));
        int regenerationInterval = Math.max(1,
                getConfig().getInt("energy.regeneration-interval-ticks", 20));
        boolean showActionBar = getConfig().getBoolean("energy.show-action-bar", true);
        boolean requireSneaking = getConfig().getBoolean("activation.require-sneaking", true);
        boolean rightClickFallback = getConfig().getBoolean("activation.allow-right-click-fallback", true);
        autosaveInterval = Math.max(20, getConfig().getInt("data.autosave-interval-ticks", 100));

        data = new PlayerDataStore(this, defaultMaximum);
        data.load();
        energy = new EnergyService(data, regenerationAmount, regenerationInterval, showActionBar);
        powers = new PowerService(data, energy);

        EnergyCommand commandHandler = new EnergyCommand(data, energy, powers);
        PluginCommand command = Objects.requireNonNull(getCommand("energy"),
                "The energy command is missing from plugin.yml");
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(
                new HighEnergyListener(data, energy, powers, requireSneaking, rightClickFallback), this);
        Bukkit.getOnlinePlayers().forEach(data::get);

        tickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            ticks++;
            powers.tick();
            Bukkit.getOnlinePlayers().forEach(energy::tick);
            if (ticks % autosaveInterval == 0) {
                data.saveIfDirty();
            }
        }, 1L, 1L);

        getLogger().info("High Energy enabled for Paper 26.2.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (data != null) {
            data.saveNow();
        }
    }
}
