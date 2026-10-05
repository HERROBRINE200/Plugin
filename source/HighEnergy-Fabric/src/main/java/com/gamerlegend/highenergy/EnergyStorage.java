package com.gamerlegend.highenergy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON persistence for Energy, maximum Energy and the selected power.
 *
 * <p>The original mod stored everything in plain {@link HashMap}s, so all progress was
 * lost on restart. The data is now written to {@code <world>/highenergy-players.json}.</p>
 */
public final class EnergyStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, PlayerEnergyData>>() {
    }.getType();
    private static final String FILE_NAME = "highenergy-players.json";

    private static final Map<UUID, PlayerEnergyData> DATA = new ConcurrentHashMap<>();
    private static Path file;
    private static boolean dirty;

    private EnergyStorage() {
    }

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        DATA.clear();
        if (file == null || !Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, PlayerEnergyData> raw = GSON.fromJson(reader, TYPE);
            if (raw != null) {
                for (Map.Entry<String, PlayerEnergyData> entry : raw.entrySet()) {
                    try {
                        DATA.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                        // skip malformed uuid
                    }
                }
            }
            HighEnergyMod.LOGGER.info("[High Energy] Loaded data for {} player(s).", DATA.size());
        } catch (IOException | RuntimeException ex) {
            HighEnergyMod.LOGGER.error("[High Energy] Could not read {}", FILE_NAME, ex);
        }
    }

    public static void save() {
        if (file == null) {
            return;
        }
        Map<String, PlayerEnergyData> raw = new HashMap<>();
        for (Map.Entry<UUID, PlayerEnergyData> entry : DATA.entrySet()) {
            raw.put(entry.getKey().toString(), entry.getValue());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(raw, TYPE, writer);
            }
            dirty = false;
        } catch (IOException ex) {
            HighEnergyMod.LOGGER.error("[High Energy] Could not write {}", FILE_NAME, ex);
        }
    }

    public static void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public static void markDirty() {
        dirty = true;
    }

    public static PlayerEnergyData get(UUID uuid) {
        return DATA.computeIfAbsent(uuid, id -> new PlayerEnergyData());
    }

    public static int size() {
        return DATA.size();
    }
}
