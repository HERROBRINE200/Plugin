package com.gamerlegend.highenergy.energy;

import com.gamerlegend.highenergy.HighEnergyConfig;
import com.gamerlegend.highenergy.Msg;
import com.gamerlegend.highenergy.storage.PlayerData;
import com.gamerlegend.highenergy.storage.PlayerDataStore;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Energy pool, regeneration and the Energy bar.
 *
 * <p>Mechanics are identical to the Fabric mod: every player owns a pool of
 * {@code 200} Energy by default and regenerates {@code 3} Energy every {@code 10} ticks
 * (0.5&nbsp;s, i.e. 6 Energy per second).</p>
 */
public final class EnergyManager {

    private final PlayerDataStore store;
    private final HighEnergyConfig config;

    public EnergyManager(PlayerDataStore store, HighEnergyConfig config) {
        this.store = store;
        this.config = config;
    }

    private PlayerData data(Player player) {
        return store.get(player.getUniqueId(), config.defaultMaxEnergy());
    }

    /** Makes sure a (possibly brand new) player has a data entry. */
    public void ensure(Player player) {
        data(player);
    }

    public int get(Player player) {
        return data(player).energy();
    }

    public int getMax(Player player) {
        return data(player).maxEnergy();
    }

    public void set(Player player, int amount) {
        data(player).energy(amount);
        store.markDirty();
    }

    public void add(Player player, int amount) {
        PlayerData data = data(player);
        data.energy(data.energy() + amount);
        store.markDirty();
    }

    public void setMax(Player player, int amount) {
        data(player).maxEnergy(amount);
        store.markDirty();
    }

    public void refill(Player player) {
        PlayerData data = data(player);
        data.energy(data.maxEnergy());
        store.markDirty();
    }

    /** Combat / reward hook, kept for parity with the mod. */
    public void combatGain(Player player, int amount) {
        add(player, amount);
    }

    /**
     * Tries to spend {@code amount} Energy.
     *
     * @return {@code true} when the player could pay, {@code false} (with a message) otherwise.
     */
    public boolean consume(Player player, int amount) {
        PlayerData data = data(player);
        if (data.energy() < amount) {
            Msg.send(player, "\u00a7cNot enough Energy! \u00a77Need \u00a7f" + amount
                    + "\u00a77, you have \u00a7f" + data.energy());
            return false;
        }
        data.energy(data.energy() - amount);
        store.markDirty();
        return true;
    }

    /** Called once per server tick for every online player. */
    public void tick(Player player) {
        PlayerData data = data(player);
        if (config.regenAmount() <= 0) {
            return;
        }
        int timer = data.regenTimer() + 1;
        if (timer >= config.regenIntervalTicks()) {
            timer = 0;
            if (data.energy() < data.maxEnergy()) {
                data.energy(data.energy() + config.regenAmount());
                store.markDirty();
            }
        }
        data.regenTimer(timer);
    }

    /** The 20 segment Energy bar from the mod. */
    public String bar(Player player) {
        PlayerData data = data(player);
        int max = data.maxEnergy();
        int current = data.energy();
        int filled = Math.round(current / (float) max * 20);
        filled = Math.max(0, Math.min(20, filled));
        return "\u00a7bEnergy \u00a7f[\u00a73" + "\u25a0".repeat(filled)
                + "\u00a78" + "\u25a0".repeat(20 - filled)
                + "\u00a7f] \u00a7b" + current + "\u00a77/\u00a7b" + max;
    }

    public void forget(UUID uuid) {
        // data stays cached so it can be written to disk; nothing to do here.
    }
}
