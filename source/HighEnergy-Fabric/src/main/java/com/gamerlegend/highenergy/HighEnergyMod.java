package com.gamerlegend.highenergy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High Energy - Fabric entry point.
 *
 * <p>Registers the commands, the server tick loop (Energy regeneration + cooldowns),
 * the activation hooks (Sneak + Use) and the save/load hooks that keep player data
 * across restarts.</p>
 */
public class HighEnergyMod implements ModInitializer {

    public static final String MOD_ID = "highenergy";
    public static final Logger LOGGER = LoggerFactory.getLogger("High Energy");

    /** Auto-save interval in ticks (5 minutes). */
    private static final int AUTO_SAVE_TICKS = 6000;

    private int saveCounter;

    @Override
    public void onInitialize() {
        EnergyManager.init();
        EnergyCommands.register();
        PowerSystem.register();

        // ---- persistence -------------------------------------------------
        ServerLifecycleEvents.SERVER_STARTED.register(EnergyStorage::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EnergyStorage.save();
            LOGGER.info("[High Energy] Saved data for {} player(s).", EnergyStorage.size());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                EnergyManager.ensure(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PowerSystem.clear(handler.getPlayer().getUUID());
            EnergyStorage.save();
        });

        // ---- server tick: regeneration + cooldowns ------------------------
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                EnergyManager.tick(player);
                PowerSystem.tick(player);
            }
            if (++saveCounter >= AUTO_SAVE_TICKS) {
                saveCounter = 0;
                EnergyStorage.saveIfDirty();
            }
        });

        // ---- activation: Sneak + Use --------------------------------------
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (player.isShiftKeyDown()) {
                PowerSystem.activateSelected(serverPlayer);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (player.isShiftKeyDown() && entity instanceof LivingEntity target && target != player) {
                PowerSystem.activateSelectedOnTarget(serverPlayer, target);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        LOGGER.info("[High Energy] Initialised with {} powers.", Power.values().length);
    }
}
