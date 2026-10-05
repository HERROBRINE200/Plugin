package com.gamerlegend.highenergy;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Power selection, cooldowns and power effects.
 *
 * <p>The selected power is persisted through {@link EnergyStorage}; cooldowns are kept
 * in memory and reset when the server restarts.</p>
 */
public final class PowerSystem {

    private static final Map<UUID, Map<Power, Integer>> COOLDOWNS = new HashMap<>();

    private PowerSystem() {
    }

    public static void register() {
        // nothing to register - kept for parity with the original entry point
    }

    public static Power selected(ServerPlayer player) {
        return EnergyStorage.get(player.getUUID()).selected();
    }

    public static void select(ServerPlayer player, Power power) {
        EnergyStorage.get(player.getUUID()).selected(power);
        EnergyStorage.markDirty();
        player.sendSystemMessage(Component.literal(
                "\u00a7b\u26a1 Selected: \u00a7f" + power.display + " \u00a77| Cost: \u00a7b" + power.cost));
    }

    public static int remaining(ServerPlayer player, Power power) {
        Map<Power, Integer> map = COOLDOWNS.get(player.getUUID());
        if (map == null) {
            return 0;
        }
        Integer value = map.get(power);
        return value == null ? 0 : value;
    }

    public static boolean ready(ServerPlayer player, Power power) {
        return remaining(player, power) <= 0;
    }

    private static void startCooldown(ServerPlayer player, Power power) {
        COOLDOWNS.computeIfAbsent(player.getUUID(), key -> new EnumMap<>(Power.class))
                .put(power, power.cooldownTicks);
    }

    public static void tick(ServerPlayer player) {
        Map<Power, Integer> map = COOLDOWNS.get(player.getUUID());
        if (map != null && !map.isEmpty()) {
            map.replaceAll((power, left) -> Math.max(0, left - 1));
        }
    }

    public static void clear(UUID uuid) {
        COOLDOWNS.remove(uuid);
    }

    public static void activateSelected(ServerPlayer player) {
        activate(player, selected(player), null);
    }

    public static void activateSelectedOnTarget(ServerPlayer player, LivingEntity target) {
        activate(player, selected(player), target);
    }

    public static void activate(ServerPlayer player, Power power, LivingEntity target) {
        if (!ready(player, power)) {
            double seconds = remaining(player, power) / 20.0;
            player.sendSystemMessage(Component.literal(
                    "\u00a7cPower cooldown! \u00a77" + String.format("%.1f", seconds) + "s left"));
            return;
        }
        // Energy Pull needs a target: check before any Energy is spent.
        if (power == Power.PULL && target == null) {
            player.sendSystemMessage(Component.literal(
                    "\u00a7eAim at an entity and Sneak + Use it."));
            return;
        }
        if (!EnergyManager.consume(player, power.cost)) {
            return;
        }
        startCooldown(player, power);

        ServerLevel level = (ServerLevel) player.level();
        switch (power) {
            case DASH -> {
                Vec3 look = player.getLookAngle().normalize();
                player.push(look.x * 1.8, 0.45, look.z * 1.8);
                player.hurtMarked = true;
                burst(level, player.position(), ParticleTypes.ELECTRIC_SPARK, 40);
            }
            case STRIKE -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 1));
                burst(level, player.position(), ParticleTypes.CRIT, 30);
            }
            case SHOCKWAVE -> {
                burst(level, player.position(), ParticleTypes.EXPLOSION, 50);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(5), entity -> entity != player)) {
                    Vec3 push = victim.position().subtract(player.position()).normalize();
                    victim.hurt(level.damageSources().playerAttack(player), 5);
                    victim.push(push.x * 1.8, 0.7, push.z * 1.8);
                    victim.hurtMarked = true;
                }
            }
            case SHIELD -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 2));
                burst(level, player.position(), ParticleTypes.END_ROD, 70);
            }
            case LIGHTNING -> {
                Vec3 at = target != null
                        ? target.position()
                        : player.position().add(player.getLookAngle().scale(8));
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                bolt.setPos(at.x, at.y, at.z);
                level.addFreshEntity(bolt);
                burst(level, at, ParticleTypes.ELECTRIC_SPARK, 80);
            }
            case PULL -> {
                Vec3 pull = player.position().subtract(target.position()).normalize();
                target.push(pull.x * 2.2, 0.4, pull.z * 2.2);
                target.hurtMarked = true;
            }
            case BLAST -> {
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(7), entity -> entity != player)) {
                    victim.hurt(level.damageSources().playerAttack(player), 7);
                    Vec3 push = victim.position().subtract(player.position()).normalize();
                    victim.push(push.x * 1.5, 0.5, push.z * 1.5);
                    victim.hurtMarked = true;
                }
                burst(level, player.position().add(player.getLookAngle().scale(5)), ParticleTypes.FLAME, 100);
            }
            case SPEED -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 3));
                burst(level, player.position(), ParticleTypes.SOUL_FIRE_FLAME, 50);
            }
            case REGEN -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
                burst(level, player.position(), ParticleTypes.HEART, 30);
            }
            case OVERDRIVE -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 3));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 2));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 1));
                burst(level, player.position(), ParticleTypes.ELECTRIC_SPARK, 160);
            }
        }

        player.sendSystemMessage(Component.literal(
                "\u00a7b\u26a1 " + power.display + " \u00a7factivated! \u00a77-" + power.cost + " Energy"));
    }

    private static void burst(ServerLevel level, Vec3 at, ParticleOptions particle, int count) {
        level.sendParticles(particle, at.x, at.y + 1, at.z, count, 0.7, 0.9, 0.7, 0.08);
    }
}
