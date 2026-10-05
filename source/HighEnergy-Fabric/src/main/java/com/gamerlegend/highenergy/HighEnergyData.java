package com.gamerlegend.highenergy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Small, dependency-free persistent store. It is deliberately keyed by UUID so
 * name changes do not reset a player's Energy, maximum, or selected power.
 */
public final class HighEnergyData {
    private static final Logger LOGGER = LoggerFactory.getLogger(HighEnergyMod.MOD_ID);
    private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();
    private static Path dataFile;
    private static boolean dirty;

    private HighEnergyData() {
    }

    public static void initialize(Path configDirectory) {
        dataFile = configDirectory.resolve("highenergy").resolve("players.properties");
        load();
    }

    private static void load() {
        PLAYERS.clear();
        if (dataFile == null || Files.notExists(dataFile)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
            for (String key : properties.stringPropertyNames()) {
                if (!key.endsWith(".energy")) {
                    continue;
                }
                String idText = key.substring(0, key.length() - ".energy".length());
                try {
                    UUID id = UUID.fromString(idText);
                    int max = positive(properties.getProperty(idText + ".max"), EnergyManager.MAX_DEFAULT);
                    int energy = bounded(properties.getProperty(key), max, max);
                    Power selected = Power.fromInput(properties.getProperty(idText + ".power", "DASH"))
                            .orElse(Power.DASH);
                    PLAYERS.put(id, new PlayerState(energy, max, selected));
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("Ignoring invalid High Energy player entry '{}'.", idText);
                }
            }
            LOGGER.info("Loaded High Energy data for {} player(s).", PLAYERS.size());
        } catch (IOException exception) {
            LOGGER.error("Could not read High Energy player data from {}.", dataFile, exception);
        }
        dirty = false;
    }

    private static int positive(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int bounded(String value, int maximum, int fallback) {
        try {
            return Math.max(0, Math.min(maximum, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    static PlayerState get(UUID id) {
        PlayerState existing = PLAYERS.get(id);
        if (existing != null) {
            return existing;
        }
        PlayerState created = new PlayerState(EnergyManager.MAX_DEFAULT, EnergyManager.MAX_DEFAULT, Power.DASH);
        PLAYERS.put(id, created);
        dirty = true;
        return created;
    }

    static void setEnergy(UUID id, int energy) {
        PlayerState state = get(id);
        int bounded = Math.max(0, Math.min(state.maximum, energy));
        if (state.energy != bounded) {
            state.energy = bounded;
            dirty = true;
        }
    }

    static void setMaximum(UUID id, int maximum) {
        PlayerState state = get(id);
        int bounded = Math.max(1, maximum);
        if (state.maximum != bounded) {
            state.maximum = bounded;
            state.energy = Math.min(state.energy, bounded);
            dirty = true;
        }
    }

    static void setPower(UUID id, Power power) {
        PlayerState state = get(id);
        if (state.selected != power) {
            state.selected = power;
            dirty = true;
        }
    }

    public static void saveIfDirty() {
        if (dirty) {
            saveNow();
        }
    }

    public static void saveNow() {
        if (dataFile == null) {
            return;
        }

        try {
            Files.createDirectories(dataFile.getParent());
            Path temporary = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
            List<Map.Entry<UUID, PlayerState>> entries = new ArrayList<>(PLAYERS.entrySet());
            entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));

            List<String> lines = new ArrayList<>();
            lines.add("# High Energy persistent player data - do not edit while the server is running");
            lines.add("version=1");
            for (Map.Entry<UUID, PlayerState> entry : entries) {
                String prefix = entry.getKey().toString();
                PlayerState state = entry.getValue();
                lines.add(prefix + ".energy=" + state.energy);
                lines.add(prefix + ".max=" + state.maximum);
                lines.add(prefix + ".power=" + state.selected.name());
            }
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, dataFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException exception) {
            LOGGER.error("Could not save High Energy player data to {}.", dataFile, exception);
        }
    }

    static final class PlayerState {
        private int energy;
        private int maximum;
        private Power selected;

        private PlayerState(int energy, int maximum, Power selected) {
            this.energy = energy;
            this.maximum = maximum;
            this.selected = selected;
        }

        int energy() {
            return energy;
        }

        int maximum() {
            return maximum;
        }

        Power selected() {
            return selected;
        }
    }
}
