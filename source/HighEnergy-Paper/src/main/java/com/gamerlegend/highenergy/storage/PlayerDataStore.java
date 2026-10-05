package com.gamerlegend.highenergy.storage;

import com.gamerlegend.highenergy.power.Power;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Flat-file persistence for Energy, maximum Energy and the selected power.
 *
 * <p>This fixes one of the biggest problems of the original mod: values were kept in a
 * plain {@link java.util.HashMap} and were therefore lost on every restart.</p>
 */
public final class PlayerDataStore {

    private final File file;
    private final Logger logger;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private volatile boolean dirty;

    public PlayerDataStore(File dataFolder, Logger logger) {
        this.logger = logger;
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("[HighEnergy] Could not create the plugin data folder.");
        }
        this.file = new File(dataFolder, "playerdata.yml");
    }

    /** Loads the whole file into memory. Called once on enable. */
    public void load(int defaultMaxEnergy) {
        cache.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                logger.warning("[HighEnergy] Skipping invalid UUID in playerdata.yml: " + key);
                continue;
            }
            int max = section.getInt("max-energy", defaultMaxEnergy);
            int energy = section.getInt("energy", max);
            Power power = Power.byName(section.getString("selected-power", Power.DASH.id()));
            cache.put(uuid, new PlayerData(energy, max, power));
        }
        logger.info("[HighEnergy] Loaded saved data for " + cache.size() + " player(s).");
    }

    /** Writes every cached entry back to disk. */
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerData> entry : cache.entrySet()) {
            PlayerData data = entry.getValue();
            String base = "players." + entry.getKey();
            yaml.set(base + ".energy", data.energy());
            yaml.set(base + ".max-energy", data.maxEnergy());
            yaml.set(base + ".selected-power", data.selected().id());
        }
        try {
            yaml.save(file);
            dirty = false;
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "[HighEnergy] Failed to save playerdata.yml", ex);
        }
    }

    /** Saves only when something changed since the last write. */
    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public PlayerData get(UUID uuid, int defaultMaxEnergy) {
        return cache.computeIfAbsent(uuid,
                id -> new PlayerData(defaultMaxEnergy, defaultMaxEnergy, Power.DASH));
    }

    public PlayerData peek(UUID uuid) {
        return cache.get(uuid);
    }

    public int size() {
        return cache.size();
    }
}
