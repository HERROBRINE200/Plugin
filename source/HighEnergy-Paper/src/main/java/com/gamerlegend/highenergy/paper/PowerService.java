package com.gamerlegend.highenergy.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PowerService {
    private final PlayerDataStore data;
    private final EnergyService energy;
    private final Map<UUID, EnumMap<Power, Integer>> cooldowns = new HashMap<>();
    private final Map<UUID, Long> lastActivationTick = new HashMap<>();
    private final Set<UUID> applyingPowerDamage = new HashSet<>();
    private long currentTick;

    PowerService(PlayerDataStore data, EnergyService energy) {
        this.data = data;
        this.energy = energy;
    }

    Power selected(Player player) {
        return data.get(player).selected;
    }

    void select(Player player, Power power) {
        data.setSelected(player, power);
        data.saveIfDirty();
        player.sendMessage(Component.text("Selected: " + power.display + " | Cost: " + power.cost,
                NamedTextColor.AQUA));
    }

    void tick() {
        currentTick++;
        cooldowns.values().removeIf(map -> {
            map.replaceAll((power, ticks) -> Math.max(0, ticks - 1));
            map.values().removeIf(ticks -> ticks <= 0);
            return map.isEmpty();
        });
    }

    boolean isApplyingPowerDamage(Player player) {
        return applyingPowerDamage.contains(player.getUniqueId());
    }

    void forgetRuntimeState(UUID id) {
        cooldowns.remove(id);
        lastActivationTick.remove(id);
        applyingPowerDamage.remove(id);
    }

    void activateSelected(Player player) {
        activate(player, selected(player), null);
    }

    void activateSelectedOnTarget(Player player, LivingEntity target) {
        activate(player, selected(player), target);
    }

    void activate(Player player, Power power, LivingEntity target) {
        UUID id = player.getUniqueId();
        if (lastActivationTick.getOrDefault(id, Long.MIN_VALUE) == currentTick) {
            return;
        }
        lastActivationTick.put(id, currentTick);

        if (power == Power.PULL && target == null) {
            player.sendMessage(Component.text(
                    "Energy Pull needs a living target. Sneak + left-click an entity.", NamedTextColor.YELLOW));
            return;
        }

        int remaining = remainingTicks(player, power);
        if (remaining > 0) {
            player.sendMessage(Component.text("Power cooldown: "
                    + String.format(Locale.ROOT, "%.1f", remaining / 20.0) + "s remaining.",
                    NamedTextColor.RED));
            return;
        }
        if (!energy.consume(player, power.cost)) {
            return;
        }

        applyingPowerDamage.add(id);
        try {
            execute(player, power, target);
        } finally {
            applyingPowerDamage.remove(id);
        }
        cooldowns.computeIfAbsent(id, ignored -> new EnumMap<>(Power.class))
                .put(power, power.cooldownTicks);
        player.sendMessage(Component.text(power.display + " activated! -" + power.cost + " Energy",
                NamedTextColor.AQUA));
    }

    private int remainingTicks(Player player, Power power) {
        EnumMap<Power, Integer> map = cooldowns.get(player.getUniqueId());
        return map == null ? 0 : map.getOrDefault(power, 0);
    }

    private void execute(Player player, Power power, LivingEntity target) {
        World world = player.getWorld();
        Location origin = player.getLocation();
        switch (power) {
            case DASH -> {
                Vector direction = origin.getDirection().normalize().multiply(1.8);
                direction.setY(0.45);
                player.setVelocity(direction);
                burst(world, origin, Particle.ELECTRIC_SPARK, 40);
            }
            case STRIKE -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 80, 1));
                burst(world, origin, Particle.CRIT, 30);
            }
            case SHOCKWAVE -> {
                burst(world, origin, Particle.EXPLOSION, 50);
                for (LivingEntity entity : nearby(player, 5.0)) {
                    Vector direction = awayFromPlayer(player, entity);
                    entity.damage(5.0, player);
                    entity.setVelocity(direction.multiply(1.8).setY(0.7));
                }
            }
            case SHIELD -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
                burst(world, origin, Particle.END_ROD, 70);
            }
            case LIGHTNING -> {
                Location position = target != null
                        ? target.getLocation()
                        : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(8.0));
                world.strikeLightning(position);
                burst(world, position, Particle.ELECTRIC_SPARK, 80);
            }
            case PULL -> {
                Vector direction = player.getLocation().toVector()
                        .subtract(target.getLocation().toVector());
                if (direction.lengthSquared() > 0.0001) {
                    direction.normalize();
                }
                target.setVelocity(direction.multiply(2.2).setY(0.4));
                burst(world, target.getLocation(), Particle.ELECTRIC_SPARK, 35);
            }
            case BLAST -> {
                for (LivingEntity entity : nearby(player, 7.0)) {
                    Vector direction = awayFromPlayer(player, entity);
                    entity.damage(7.0, player);
                    entity.setVelocity(direction.multiply(1.5).setY(0.5));
                }
                Location effect = player.getLocation().add(
                        player.getLocation().getDirection().normalize().multiply(5.0));
                burst(world, effect, Particle.FLAME, 100);
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
        }
    }

    private static Collection<LivingEntity> nearby(Player player, double radius) {
        return player.getWorld().getNearbyLivingEntities(player.getLocation(), radius,
                entity -> entity != player && entity.isValid() && !entity.isDead());
    }

    private static Vector awayFromPlayer(Player player, LivingEntity entity) {
        Vector direction = entity.getLocation().toVector().subtract(player.getLocation().toVector());
        return direction.lengthSquared() < 0.0001 ? new Vector() : direction.normalize();
    }

    private static void burst(World world, Location location, Particle particle, int count) {
        world.spawnParticle(particle, location.clone().add(0, 1, 0), count,
                0.7, 0.9, 0.7, 0.08);
    }
}
