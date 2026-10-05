package com.gamerlegend.highenergy.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class EnergyService {
    private final PlayerDataStore data;
    private final int regenerationAmount;
    private final int regenerationInterval;
    private final boolean showActionBar;
    private final Map<UUID, Integer> regenerationTimers = new HashMap<>();
    private final Map<UUID, Integer> actionBarTimers = new HashMap<>();

    EnergyService(PlayerDataStore data, int regenerationAmount, int regenerationInterval,
                  boolean showActionBar) {
        this.data = data;
        this.regenerationAmount = Math.max(0, regenerationAmount);
        this.regenerationInterval = Math.max(1, regenerationInterval);
        this.showActionBar = showActionBar;
    }

    int get(Player player) {
        return data.get(player).energy;
    }

    int getMaximum(Player player) {
        return data.get(player).maximum;
    }

    boolean consume(Player player, int amount) {
        if (get(player) < amount) {
            player.sendMessage(Component.text("Not enough Energy! Need " + amount + ", you have "
                    + get(player) + ".", NamedTextColor.RED));
            return false;
        }
        set(player, get(player) - amount);
        return true;
    }

    void add(Player player, int amount) {
        long result = get(player) + (long) amount;
        data.setEnergy(player, (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result)));
    }

    void set(Player player, int amount) {
        data.setEnergy(player, amount);
    }

    void setMaximum(Player player, int maximum) {
        data.setMaximum(player, maximum);
    }

    void tick(Player player) {
        UUID id = player.getUniqueId();
        int timer = regenerationTimers.getOrDefault(id, 0) + 1;
        if (timer >= regenerationInterval) {
            add(player, regenerationAmount);
            timer = 0;
        }
        regenerationTimers.put(id, timer);

        int actionBarTimer = actionBarTimers.getOrDefault(id, 0) + 1;
        if (actionBarTimer >= 20) {
            if (showActionBar) {
                player.sendActionBar(bar(player));
            }
            actionBarTimer = 0;
        }
        actionBarTimers.put(id, actionBarTimer);
    }

    void forgetRuntimeState(UUID id) {
        regenerationTimers.remove(id);
        actionBarTimers.remove(id);
    }

    Component bar(Player player) {
        int maximum = getMaximum(player);
        int current = get(player);
        int filled = Math.round(current / (float) maximum * 20.0F);
        return Component.text("Energy [", NamedTextColor.AQUA)
                .append(Component.text("■".repeat(Math.max(0, filled)), NamedTextColor.DARK_AQUA))
                .append(Component.text("■".repeat(Math.max(0, 20 - filled)), NamedTextColor.DARK_GRAY))
                .append(Component.text("] " + current + "/" + maximum, NamedTextColor.AQUA));
    }
}
