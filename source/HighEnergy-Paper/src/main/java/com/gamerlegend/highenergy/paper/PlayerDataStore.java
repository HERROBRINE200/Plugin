package com.gamerlegend.highenergy.paper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class PlayerDataStore {
    private final JavaPlugin plugin;
    private final File file;
    private final int defaultMaximum;
    private final Map<UUID, PlayerState> players = new HashMap<>();
    private boolean dirty;

    PlayerDataStore(JavaPlugin plugin, int defaultMaximum) {
        this.plugin = plugin;
        this.defaultMaximum = Math.max(1, defaultMaximum);
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    void load() {
        players.clear();
        if (!file.isFile()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String idText : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(idText);
                String path = "players." + idText + ".";
                int maximum = Math.max(1, yaml.getInt(path + "maximum", defaultMaximum));
                int energy = Math.max(0, Math.min(maximum, yaml.getInt(path + "energy", maximum)));
                Power selected = Power.fromInput(yaml.getString(path + "selected-power", "DASH"))
                        .orElse(Power.DASH);
                String name = yaml.getString(path + "last-known-name", "unknown");
                players.put(id, new PlayerState(energy, maximum, selected, name));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Ignoring invalid player UUID in players.yml: " + idText);
            }
        }
        dirty = false;
        plugin.getLogger().info("Loaded High Energy data for " + players.size() + " player(s).");
    }

    PlayerState get(Player player) {
        PlayerState state = get(player.getUniqueId());
        if (!player.getName().equals(state.lastKnownName)) {
            state.lastKnownName = player.getName();
            dirty = true;
        }
        return state;
    }

    PlayerState get(UUID id) {
        PlayerState state = players.get(id);
        if (state != null) {
            return state;
        }
        PlayerState created = new PlayerState(defaultMaximum, defaultMaximum, Power.DASH, "unknown");
        players.put(id, created);
        dirty = true;
        return created;
    }

    void setEnergy(Player player, int energy) {
        PlayerState state = get(player);
        int bounded = Math.max(0, Math.min(state.maximum, energy));
        if (state.energy != bounded) {
            state.energy = bounded;
            dirty = true;
        }
    }

    void setMaximum(Player player, int maximum) {
        PlayerState state = get(player);
        int bounded = Math.max(1, maximum);
        if (state.maximum != bounded) {
            state.maximum = bounded;
            state.energy = Math.min(state.energy, bounded);
            dirty = true;
        }
    }

    void setSelected(Player player, Power power) {
        PlayerState state = get(player);
        if (state.selected != power) {
            state.selected = power;
            dirty = true;
        }
    }

    void saveIfDirty() {
        if (dirty) {
            saveNow();
        }
    }

    void saveNow() {
        if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().severe("Could not create plugin data directory: " + plugin.getDataFolder());
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("data-version", 1);
        players.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    String path = "players." + entry.getKey() + ".";
                    PlayerState state = entry.getValue();
                    yaml.set(path + "last-known-name", state.lastKnownName);
                    yaml.set(path + "energy", state.energy);
                    yaml.set(path + "maximum", state.maximum);
                    yaml.set(path + "selected-power", state.selected.name());
                });

        Path target = file.toPath();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save players.yml: " + exception.getMessage());
        }
    }
}
