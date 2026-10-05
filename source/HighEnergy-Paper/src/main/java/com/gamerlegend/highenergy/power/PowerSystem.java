package com.gamerlegend.highenergy.power;

import com.gamerlegend.highenergy.HighEnergyConfig;
import com.gamerlegend.highenergy.Msg;
import com.gamerlegend.highenergy.energy.EnergyManager;
import com.gamerlegend.highenergy.storage.PlayerDataStore;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Power selection, cooldowns and the actual power effects.
 *
 * <p>All ranges, durations, amplifiers, damage values and knockback strengths are a 1:1
 * port of the Fabric mod.</p>
 */
public final class PowerSystem {

    private final EnergyManager energy;
    private final HighEnergyConfig config;
    private final PlayerDataStore store;

    /** Remaining cooldown ticks per player and power. */
    private final Map<UUID, Map<Power, Integer>> cooldowns = new HashMap<>();

    public PowerSystem(EnergyManager energy, HighEnergyConfig config, PlayerDataStore store) {
        this.energy = energy;
        this.config = config;
        this.store = store;
    }

    // ------------------------------------------------------------------ selection

    public Power selected(Player player) {
        return store.get(player.getUniqueId(), config.defaultMaxEnergy()).selected();
    }

    public void select(Player player, Power power) {
        store.get(player.getUniqueId(), config.defaultMaxEnergy()).selected(power);
        store.markDirty();
        Msg.send(player, "\u00a7b\u26a1 Selected: \u00a7f" + power.display()
                + " \u00a77| Cost: \u00a7b" + config.cost(power));
    }

    // ------------------------------------------------------------------ cooldowns

    public boolean ready(Player player, Power power) {
        return remaining(player, power) <= 0;
    }

    public int remaining(Player player, Power power) {
        Map<Power, Integer> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return 0;
        }
        Integer value = map.get(power);
        return value == null ? 0 : value;
    }

    private void startCooldown(Player player, Power power) {
        cooldowns.computeIfAbsent(player.getUniqueId(), key -> new EnumMap<>(Power.class))
                .put(power, config.cooldownTicks(power));
    }

    /** Ticks the cooldowns of one player down by one. */
    public void tick(Player player) {
        Map<Power, Integer> map = cooldowns.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            return;
        }
        map.replaceAll((power, left) -> Math.max(0, left - 1));
    }

    public void clear(UUID uuid) {
        cooldowns.remove(uuid);
    }

    // ------------------------------------------------------------------ activation

    public void activateSelected(Player player) {
        activate(player, selected(player), null);
    }

    public void activateSelectedOnTarget(Player player, LivingEntity target) {
        activate(player, selected(player), target);
    }

    public void activate(Player player, Power power, LivingEntity target) {
        if (!player.hasPermission("highenergy.use")) {
            Msg.send(player, "\u00a7cYou are not allowed to use High Energy powers.");
            return;
        }
        if (!ready(player, power)) {
            double seconds = remaining(player, power) / 20.0;
            Msg.send(player, "\u00a7cPower cooldown! \u00a77" + String.format("%.1f", seconds) + "s left");
            return;
        }
        int cost = config.cost(power);

        // PULL needs a target: check before spending any Energy.
        if (power == Power.PULL && target == null) {
            Msg.send(player, "\u00a7eAim at an entity and sneak + click it.");
            return;
        }
        if (!energy.consume(player, cost)) {
            return;
        }
        startCooldown(player, power);

        World world = player.getWorld();
        Location origin = player.getLocation();

        switch (power) {
            case DASH -> {
                Vector look = player.getLocation().getDirection().normalize();
                player.setVelocity(new Vector(look.getX() * 1.8, 0.45, look.getZ() * 1.8));
                burst(world, origin, Particle.ELECTRIC_SPARK, 40);
            }
            case STRIKE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 1));
                burst(world, origin, Particle.CRIT, 30);
            }
            case SHOCKWAVE -> {
                burst(world, origin, Particle.EXPLOSION, 50);
                for (LivingEntity victim : nearbyLiving(player, 5.0)) {
                    Vector push = victim.getLocation().toVector()
                            .subtract(origin.toVector()).normalize();
                    victim.damage(5.0, player);
                    victim.setVelocity(new Vector(push.getX() * 1.8, 0.7, push.getZ() * 1.8));
                }
            }
            case SHIELD -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
                burst(world, origin, Particle.END_ROD, 70);
            }
            case LIGHTNING -> {
                Location strike = target != null
                        ? target.getLocation()
                        : origin.clone().add(origin.getDirection().multiply(8));
                world.strikeLightning(strike);
                burst(world, strike, Particle.ELECTRIC_SPARK, 80);
            }
            case PULL -> {
                Vector pull = origin.toVector()
                        .subtract(target.getLocation().toVector()).normalize();
                target.setVelocity(new Vector(pull.getX() * 2.2, 0.4, pull.getZ() * 2.2));
            }
            case BLAST -> {
                for (LivingEntity victim : nearbyLiving(player, 7.0)) {
                    victim.damage(7.0, player);
                    Vector push = victim.getLocation().toVector()
                            .subtract(origin.toVector()).normalize();
                    victim.setVelocity(new Vector(push.getX() * 1.5, 0.5, push.getZ() * 1.5));
                }
                burst(world, origin.clone().add(origin.getDirection().multiply(5)), Particle.FLAME, 100);
            }
            case SPEED -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 3));
                burst(world, origin, Particle.SOUL_FIRE_FLAME, 50);
            }
            case REGEN -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2));
                burst(world, origin, Particle.HEART, 30);
            }
            case OVERDRIVE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1));
                burst(world, origin, Particle.ELECTRIC_SPARK, 160);
            }
            default -> {
                // unreachable - every power is handled above
            }
        }

        Msg.send(player, "\u00a7b\u26a1 " + power.display()
                + " \u00a7factivated! \u00a77-" + cost + " Energy");
    }

    // ------------------------------------------------------------------ helpers

    private java.util.List<LivingEntity> nearbyLiving(Player player, double radius) {
        java.util.List<LivingEntity> result = new java.util.ArrayList<>();
        for (Entity entity : player.getWorld().getNearbyEntities(
                player.getBoundingBox().expand(radius))) {
            if (entity instanceof LivingEntity living && !living.equals(player)) {
                result.add(living);
            }
        }
        return result;
    }

    private void burst(World world, Location at, Particle particle, int count) {
        world.spawnParticle(particle, at.getX(), at.getY() + 1, at.getZ(),
                count, 0.7, 0.9, 0.7, 0.08);
    }
}
