package com.gamerlegend.highenergy;

import com.gamerlegend.highenergy.command.EnergyCommand;
import com.gamerlegend.highenergy.energy.EnergyManager;
import com.gamerlegend.highenergy.guide.GuideBook;
import com.gamerlegend.highenergy.listener.PowerListener;
import com.gamerlegend.highenergy.power.PowerSystem;
import com.gamerlegend.highenergy.storage.PlayerDataStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * High Energy - Paper plugin entry point.
 *
 * <p>Paper equivalent of the {@code HighEnergyMod} Fabric initialiser: it wires the
 * Energy manager, the power system, the commands, the listeners and a server tick task
 * that drives regeneration, cooldowns and the Energy action bar.</p>
 */
public final class HighEnergyPlugin extends JavaPlugin {

    private HighEnergyConfig config;
    private PlayerDataStore store;
    private EnergyManager energy;
    private PowerSystem powers;
    private GuideBook guide;
    private BukkitTask tickTask;
    private BukkitTask saveTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new HighEnergyConfig(getConfig());
        this.store = new PlayerDataStore(getDataFolder(), getLogger());
        this.store.load(config.defaultMaxEnergy());
        this.energy = new EnergyManager(store, config);
        this.powers = new PowerSystem(energy, config, store);
        this.guide = new GuideBook(config);

        PluginCommand command = getCommand("energy");
        if (command != null) {
            EnergyCommand executor = new EnergyCommand(this, energy, powers, guide);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().severe("Command /energy is missing from plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(new PowerListener(this), this);

        for (Player player : getServer().getOnlinePlayers()) {
            energy.ensure(player);
        }

        startTasks();
        getLogger().info("High Energy enabled - " + com.gamerlegend.highenergy.power.Power.values().length
                + " powers ready.");
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (store != null) {
            store.save();
            getLogger().info("Saved Energy data for " + store.size() + " player(s).");
        }
    }

    private void startTasks() {
        // one server tick -> regeneration + cooldowns + action bar (mirrors END_SERVER_TICK)
        tickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                energy.tick(player);
                powers.tick(player);
                if (config.actionBarEnabled() && player.isSneaking()) {
                    player.sendActionBar(Msg.of(energy.bar(player)));
                }
            }
        }, 1L, 1L);

        saveTask = getServer().getScheduler().runTaskTimer(this,
                () -> store.saveIfDirty(),
                config.autoSaveIntervalTicks(), config.autoSaveIntervalTicks());
    }

    private void stopTasks() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    /** Used by {@code /energy reload}. */
    public void reloadEverything() {
        stopTasks();
        store.save();
        reloadConfig();
        this.config = new HighEnergyConfig(getConfig());
        this.energy = new EnergyManager(store, config);
        this.powers = new PowerSystem(energy, config, store);
        this.guide = new GuideBook(config);

        PluginCommand command = getCommand("energy");
        if (command != null) {
            EnergyCommand executor = new EnergyCommand(this, energy, powers, guide);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        startTasks();
    }

    public HighEnergyConfig config() {
        return config;
    }

    public PlayerDataStore store() {
        return store;
    }

    public EnergyManager energy() {
        return energy;
    }

    public PowerSystem powers() {
        return powers;
    }

    public GuideBook guide() {
        return guide;
    }
}
