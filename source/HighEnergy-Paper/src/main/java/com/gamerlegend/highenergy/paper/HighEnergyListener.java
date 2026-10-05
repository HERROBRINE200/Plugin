package com.gamerlegend.highenergy.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

final class HighEnergyListener implements Listener {
    private final PlayerDataStore data;
    private final EnergyService energy;
    private final PowerService powers;
    private final boolean requireSneaking;
    private final boolean allowRightClickFallback;

    HighEnergyListener(PlayerDataStore data, EnergyService energy, PowerService powers,
                       boolean requireSneaking, boolean allowRightClickFallback) {
        this.data = data;
        this.energy = energy;
        this.powers = powers;
        this.requireSneaking = requireSneaking;
        this.allowRightClickFallback = allowRightClickFallback;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND || !canActivate(player)) {
            return;
        }
        Action action = event.getAction();
        boolean leftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean rightClick = allowRightClickFallback
                && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        if (!leftClick && !rightClick) {
            return;
        }
        event.setCancelled(true);
        powers.activateSelected(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttackEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || powers.isApplyingPowerDamage(player)) {
            return;
        }
        if (!canActivate(player) || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        event.setCancelled(true);
        powers.activateSelectedOnTarget(player, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUseEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!allowRightClickFallback || event.getHand() != EquipmentSlot.HAND
                || !canActivate(player) || !(event.getRightClicked() instanceof LivingEntity target)) {
            return;
        }
        event.setCancelled(true);
        powers.activateSelectedOnTarget(player, target);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        data.get(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUIDCleanup.cleanup(event.getPlayer(), data, energy, powers);
    }

    private boolean canActivate(Player player) {
        return player.hasPermission("highenergy.use") && (!requireSneaking || player.isSneaking());
    }

    /** Keeps quit handling in one place and makes all transient maps release UUIDs. */
    private static final class UUIDCleanup {
        private static void cleanup(Player player, PlayerDataStore data,
                                    EnergyService energy, PowerService powers) {
            data.get(player);
            data.saveIfDirty();
            energy.forgetRuntimeState(player.getUniqueId());
            powers.forgetRuntimeState(player.getUniqueId());
        }
    }
}
