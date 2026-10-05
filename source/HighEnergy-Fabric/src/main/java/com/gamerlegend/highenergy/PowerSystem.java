package com.gamerlegend.highenergy;

import net.minecraft.ChatFormatting;
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

public final class PowerSystem {
    private static final Map<UUID, EnumMap<Power, Integer>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_ACTIVATION_TICK = new HashMap<>();

    private PowerSystem() {
    }

    public static Power selected(ServerPlayer player) {
        return HighEnergyData.get(player.getUUID()).selected();
    }

    public static void select(ServerPlayer player, Power power) {
        HighEnergyData.setPower(player.getUUID(), power);
        HighEnergyData.saveIfDirty();
        player.sendSystemMessage(Component.literal("Selected: " + power.display + " | Cost: " + power.cost)
                .withStyle(ChatFormatting.AQUA));
    }

    public static boolean ready(ServerPlayer player, Power power) {
        return remainingTicks(player, power) <= 0;
    }

    public static int remainingTicks(ServerPlayer player, Power power) {
        return COOLDOWNS.getOrDefault(player.getUUID(), new EnumMap<>(Power.class))
                .getOrDefault(power, 0);
    }

    public static void tick(ServerPlayer player) {
        EnumMap<Power, Integer> cooldowns = COOLDOWNS.get(player.getUUID());
        if (cooldowns == null) {
            return;
        }
        cooldowns.replaceAll((power, ticks) -> Math.max(0, ticks - 1));
        cooldowns.values().removeIf(ticks -> ticks <= 0);
        if (cooldowns.isEmpty()) {
            COOLDOWNS.remove(player.getUUID());
        }
    }

    public static void activateSelected(ServerPlayer player) {
        activate(player, selected(player), null);
    }

    public static void activateSelectedOnTarget(ServerPlayer player, LivingEntity target) {
        activate(player, selected(player), target);
    }

    public static void activate(ServerPlayer player, Power power, LivingEntity target) {
        long gameTime = player.level().getGameTime();
        if (LAST_ACTIVATION_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE) == gameTime) {
            return;
        }
        LAST_ACTIVATION_TICK.put(player.getUUID(), gameTime);

        if (power == Power.PULL && target == null) {
            player.sendSystemMessage(Component.literal(
                    "Energy Pull needs a living target. Sneak + left-click an entity.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        int remaining = remainingTicks(player, power);
        if (remaining > 0) {
            player.sendSystemMessage(Component.literal("Power cooldown: "
                    + String.format(java.util.Locale.ROOT, "%.1f", remaining / 20.0) + "s remaining.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (!EnergyManager.consume(player, power.cost)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        switch (power) {
            case DASH -> {
                Vec3 direction = player.getLookAngle().normalize();
                player.push(direction.x * 1.8, 0.45, direction.z * 1.8);
                burst(level, player.position(), ParticleTypes.ELECTRIC_SPARK, 40);
            }
            case STRIKE -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 80, 1));
                burst(level, player.position(), ParticleTypes.CRIT, 30);
            }
            case SHOCKWAVE -> {
                burst(level, player.position(), ParticleTypes.EXPLOSION, 50);
                for (LivingEntity entity : nearby(level, player, 5.0)) {
                    Vec3 direction = awayFromPlayer(player, entity);
                    entity.hurtServer(level, level.damageSources().playerAttack(player), 5.0F);
                    entity.push(direction.x * 1.8, 0.7, direction.z * 1.8);
                }
            }
            case SHIELD -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 2));
                burst(level, player.position(), ParticleTypes.END_ROD, 70);
            }
            case LIGHTNING -> {
                Vec3 position = target != null
                        ? target.position()
                        : player.position().add(player.getLookAngle().scale(8.0));
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                bolt.setPos(position.x, position.y, position.z);
                level.addFreshEntity(bolt);
                burst(level, position, ParticleTypes.ELECTRIC_SPARK, 80);
            }
            case PULL -> {
                Vec3 direction = player.position().subtract(target.position()).normalize();
                target.push(direction.x * 2.2, 0.4, direction.z * 2.2);
                burst(level, target.position(), ParticleTypes.ELECTRIC_SPARK, 35);
            }
            case BLAST -> {
                for (LivingEntity entity : nearby(level, player, 7.0)) {
                    Vec3 direction = awayFromPlayer(player, entity);
                    entity.hurtServer(level, level.damageSources().playerAttack(player), 7.0F);
                    entity.push(direction.x * 1.5, 0.5, direction.z * 1.5);
                }
                burst(level, player.position().add(player.getLookAngle().scale(5.0)), ParticleTypes.FLAME, 100);
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

        COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new EnumMap<>(Power.class))
                .put(power, power.cooldownTicks);
        player.sendSystemMessage(Component.literal(power.display + " activated! -" + power.cost + " Energy")
                .withStyle(ChatFormatting.AQUA));
    }

    private static Iterable<LivingEntity> nearby(ServerLevel level, ServerPlayer player, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive());
    }

    private static Vec3 awayFromPlayer(ServerPlayer player, LivingEntity entity) {
        Vec3 offset = entity.position().subtract(player.position());
        return offset.lengthSqr() < 0.0001 ? new Vec3(0, 0, 0) : offset.normalize();
    }

    private static void burst(ServerLevel level, Vec3 position, ParticleOptions particle, int count) {
        level.sendParticles(particle, position.x, position.y + 1.0, position.z,
                count, 0.7, 0.9, 0.7, 0.08);
    }
}
