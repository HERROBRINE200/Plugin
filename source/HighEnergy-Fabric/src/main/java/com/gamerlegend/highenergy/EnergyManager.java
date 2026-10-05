package com.gamerlegend.highenergy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EnergyManager {
    public static final int MAX_DEFAULT = 200;
    public static final int REGEN_AMOUNT = 3;
    public static final int REGEN_INTERVAL_TICKS = 20;

    private static final Map<UUID, Integer> REGEN_TIMERS = new HashMap<>();

    private EnergyManager() {
    }

    public static int get(ServerPlayer player) {
        return HighEnergyData.get(player.getUUID()).energy();
    }

    public static int getMax(ServerPlayer player) {
        return HighEnergyData.get(player.getUUID()).maximum();
    }

    public static void ensure(ServerPlayer player) {
        HighEnergyData.get(player.getUUID());
    }

    public static boolean consume(ServerPlayer player, int amount) {
        ensure(player);
        if (get(player) < amount) {
            player.sendSystemMessage(Component.literal("Not enough Energy! Need " + amount
                    + ", you have " + get(player) + ".").withStyle(ChatFormatting.RED));
            return false;
        }
        HighEnergyData.setEnergy(player.getUUID(), get(player) - amount);
        return true;
    }

    public static void add(ServerPlayer player, int amount) {
        ensure(player);
        long result = get(player) + (long) amount;
        HighEnergyData.setEnergy(player.getUUID(),
                (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result)));
    }

    public static void set(ServerPlayer player, int amount) {
        ensure(player);
        HighEnergyData.setEnergy(player.getUUID(), amount);
    }

    public static void setMax(ServerPlayer player, int amount) {
        HighEnergyData.setMaximum(player.getUUID(), amount);
    }

    public static void tick(ServerPlayer player) {
        ensure(player);
        int timer = REGEN_TIMERS.getOrDefault(player.getUUID(), 0) + 1;
        if (timer >= REGEN_INTERVAL_TICKS) {
            add(player, REGEN_AMOUNT);
            timer = 0;
        }
        REGEN_TIMERS.put(player.getUUID(), timer);
    }

    public static Component bar(ServerPlayer player) {
        int maximum = getMax(player);
        int current = get(player);
        int filled = Math.round(current / (float) maximum * 20.0F);
        MutableComponent result = Component.literal("Energy [").withStyle(ChatFormatting.AQUA);
        result.append(Component.literal("■".repeat(Math.max(0, filled))).withStyle(ChatFormatting.DARK_AQUA));
        result.append(Component.literal("■".repeat(Math.max(0, 20 - filled))).withStyle(ChatFormatting.DARK_GRAY));
        result.append(Component.literal("] " + current + "/" + maximum).withStyle(ChatFormatting.AQUA));
        return result;
    }
}
