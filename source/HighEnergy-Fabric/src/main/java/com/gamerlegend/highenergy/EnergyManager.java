package com.gamerlegend.highenergy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Energy pool, regeneration and the Energy bar.
 *
 * <p>Defaults match the original mod: 200 maximum Energy and +3 Energy every 10 ticks
 * (0.5&nbsp;s, i.e. 6 Energy per second).</p>
 */
public final class EnergyManager {

    public static final int MAX_DEFAULT = 200;
    public static final int REGEN_AMOUNT = 3;
    public static final int REGEN_INTERVAL_TICKS = 10;

    private EnergyManager() {
    }

    public static void init() {
        // nothing to do - storage is loaded when the server starts
    }

    private static PlayerEnergyData data(ServerPlayer player) {
        return EnergyStorage.get(player.getUUID());
    }

    public static void ensure(ServerPlayer player) {
        data(player);
    }

    public static int get(ServerPlayer player) {
        return data(player).energy;
    }

    public static int getMax(ServerPlayer player) {
        return data(player).maxEnergy;
    }

    public static void set(ServerPlayer player, int amount) {
        PlayerEnergyData d = data(player);
        d.energy = Math.max(0, Math.min(d.maxEnergy, amount));
        EnergyStorage.markDirty();
    }

    public static void add(ServerPlayer player, int amount) {
        PlayerEnergyData d = data(player);
        d.energy = Math.max(0, Math.min(d.maxEnergy, d.energy + amount));
        EnergyStorage.markDirty();
    }

    public static void setMax(ServerPlayer player, int amount) {
        PlayerEnergyData d = data(player);
        d.maxEnergy = Math.max(1, amount);
        d.energy = Math.min(d.energy, d.maxEnergy);
        EnergyStorage.markDirty();
    }

    public static void refill(ServerPlayer player) {
        PlayerEnergyData d = data(player);
        d.energy = d.maxEnergy;
        EnergyStorage.markDirty();
    }

    public static boolean consume(ServerPlayer player, int amount) {
        PlayerEnergyData d = data(player);
        if (d.energy < amount) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7cNot enough Energy! \u00a77Need \u00a7f" + amount
                            + "\u00a77, you have \u00a7f" + d.energy));
            return false;
        }
        d.energy -= amount;
        EnergyStorage.markDirty();
        return true;
    }

    public static void combatGain(ServerPlayer player, int amount) {
        add(player, amount);
    }

    /** Called every server tick for every online player. */
    public static void tick(ServerPlayer player) {
        PlayerEnergyData d = data(player);
        int timer = d.regenTimer + 1;
        if (timer >= REGEN_INTERVAL_TICKS) {
            timer = 0;
            if (d.energy < d.maxEnergy) {
                d.energy = Math.min(d.maxEnergy, d.energy + REGEN_AMOUNT);
                EnergyStorage.markDirty();
            }
        }
        d.regenTimer = timer;
    }

    public static String bar(ServerPlayer player) {
        PlayerEnergyData d = data(player);
        int filled = Math.round(d.energy / (float) d.maxEnergy * 20);
        filled = Math.max(0, Math.min(20, filled));
        return "\u00a7bEnergy \u00a7f[\u00a73" + "\u25a0".repeat(filled)
                + "\u00a78" + "\u25a0".repeat(20 - filled)
                + "\u00a7f] \u00a7b" + d.energy + "\u00a77/\u00a7b" + d.maxEnergy;
    }
}
