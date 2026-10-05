package com.gamerlegend.highenergy;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HighEnergyMod implements ModInitializer {
    public static final String MOD_ID = "highenergy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int serverTicks;

    @Override
    public void onInitialize() {
        HighEnergyData.initialize(FabricLoader.getInstance().getConfigDir());
        EnergyCommands.register();
        registerActivationEvents();

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            serverTicks++;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                EnergyManager.tick(player);
                PowerSystem.tick(player);
                if (serverTicks % 20 == 0) {
                    player.displayClientMessage(EnergyManager.bar(player), true);
                }
            }
            if (serverTicks % 100 == 0) {
                HighEnergyData.saveIfDirty();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> HighEnergyData.saveNow());
        LOGGER.info("High Energy initialized for Minecraft 26.2.");
    }

    private static void registerActivationEvents() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                    || !player.isShiftKeyDown() || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            if (entity instanceof LivingEntity target) {
                PowerSystem.activateSelectedOnTarget(serverPlayer, target);
            } else {
                PowerSystem.activateSelected(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        });

        AttackBlockCallback.EVENT.register((player, level, hand, position, direction) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                    || !player.isShiftKeyDown() || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            PowerSystem.activateSelected(serverPlayer);
            return InteractionResult.SUCCESS;
        });

        // Fabric has no ordinary server callback for a left-click that hits only air.
        // Keep the original project's sneak-use behavior as a reliable air fallback.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                    || !player.isShiftKeyDown() || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            PowerSystem.activateSelected(serverPlayer);
            return InteractionResult.SUCCESS;
        });
    }
}
