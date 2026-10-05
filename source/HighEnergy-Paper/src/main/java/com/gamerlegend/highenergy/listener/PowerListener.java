package com.gamerlegend.highenergy.listener;

import com.gamerlegend.highenergy.HighEnergyConfig;
import com.gamerlegend.highenergy.HighEnergyPlugin;
import com.gamerlegend.highenergy.energy.EnergyManager;
import com.gamerlegend.highenergy.power.PowerSystem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Replaces the Fabric {@code UseItemCallback} / {@code UseEntityCallback} hooks:
 * sneak + click activates the selected power, sneak + click on a mob activates it
 * on that target (used by Energy Pull and Lightning Strike).
 */
public final class PowerListener implements Listener {

    private final HighEnergyPlugin plugin;

    public PowerListener(HighEnergyPlugin plugin) {
        this.plugin = plugin;
    }

    private EnergyManager energy() {
        return plugin.energy();
    }

    private PowerSystem powers() {
        return plugin.powers();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return; // avoid firing twice for the off-hand
        }
        Player player = event.getPlayer();
        HighEnergyConfig cfg = plugin.config();
        if (cfg.requireSneak() && !player.isSneaking()) {
            return;
        }
        Action action = event.getAction();
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if ((right && cfg.activateOnRightClick()) || (left && cfg.activateOnLeftClick())) {
            powers().activateSelected(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        HighEnergyConfig cfg = plugin.config();
        if (cfg.requireSneak() && !player.isSneaking()) {
            return;
        }
        if (!cfg.activateOnRightClick()) {
            return;
        }
        if (event.getRightClicked() instanceof LivingEntity target && !target.equals(player)) {
            powers().activateSelectedOnTarget(player, target);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        energy().ensure(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        powers().clear(event.getPlayer().getUniqueId());
        plugin.store().save();
    }
}
