package com.darkun7.runemobs.raids.ai;

import com.darkun7.runemobs.RuneMobs;
import com.darkun7.runemobs.MobSpawnListener;
import com.darkun7.runemobs.raids.RuneRaid;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LeviathanBossAI extends BukkitRunnable {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();

    private final List<LivingEntity> summons = new ArrayList<>();
    private int tickCounter = 0;

    public LeviathanBossAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        plugin.getServer().broadcastMessage("§7[§bThe Deep Sea Leviathan§7] §9\"The abyss demands tribute!\"");
    }

    @Override
    public void run() {
        if (!boss.isValid() || boss.isDead()) {
            cleanUp();
            cancel();
            return;
        }

        tickCounter++;
        World world = boss.getWorld();
        Location bossLoc = boss.getLocation();

        // Constant aura telling it's a deep sea leviathan
        world.spawnParticle(Particle.DRIPPING_WATER, bossLoc.clone().add(0, 1, 0), 10, 1.0, 1.0, 1.0, 0.1);

        // 1. Whirlpool Pull Mechanic (Every ~4 seconds)
        if (tickCounter % 80 == 0) {
            boolean pulled = false;
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(15, 8, 15)) {
                if (entity instanceof Player player && !player.isDead()
                        && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    // Create visual whirlpool vortex
                    drawWhirlpool(bossLoc, 5.0);

                    // Pull math: drag directly toward boss
                    org.bukkit.util.Vector dir = bossLoc.toVector().subtract(player.getLocation().toVector());
                    double dist = dir.length();
                    if (dist > 1.0) {
                        dir.normalize();
                        // Pull scale: stronger when further away but capped
                        double strength = Math.min(dist * 0.15, 1.5);
                        player.setVelocity(player.getVelocity().add(dir.multiply(strength)));

                        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0f, 0.5f);
                        pulled = true;
                    }
                }
            }
            if (pulled) {
                world.playSound(bossLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.8f);
            }
        }

        // 2. Suffocation (Oxygen ripping) if too close (Every ~2 seconds)
        if (tickCounter % 40 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Player player && !player.isDead()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, false));
                    world.spawnParticle(Particle.BUBBLE_POP, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
                    world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 1.0f);
                }
            }
        }

        // 3. Summon Drowned Minions (Every ~20 seconds)
        if (tickCounter % 400 == 0) {
            summons.removeIf(s -> !s.isValid() || s.isDead());

            // Only summon if there aren't too many alive already
            if (summons.size() < 4) {
                world.playSound(bossLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);
                for (int i = 0; i < 2; i++) {
                    Location spawnLoc = bossLoc.clone().add(random.nextDouble() * 8 - 4, 0,
                            random.nextDouble() * 8 - 4);
                    Zombie drowned = (Zombie) world.spawnEntity(spawnLoc, EntityType.DROWNED);
                    drowned.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&bLost Sailor"));
                    drowned.setCustomNameVisible(true);
                    drowned.addPotionEffect(
                            new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                    MobSpawnListener.applyRarity(drowned, "rare", plugin);

                    if (boss instanceof org.bukkit.entity.Mob bm && bm.getTarget() != null) {
                        drowned.setTarget(bm.getTarget());
                    }
                    summons.add(drowned);
                    raid.addSpawnedEntity(drowned);
                }
            }
        }
    }

    private void drawWhirlpool(Location center, double radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int degree = 0; degree < 360; degree += 30) {
            double radians = Math.toRadians(degree);
            double x = radius * Math.cos(radians);
            double z = radius * Math.sin(radians);
            Location particleLoc = center.clone().add(x, 0, z);
            world.spawnParticle(Particle.NAUTILUS, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void cleanUp() {
        for (LivingEntity summon : summons) {
            if (summon.isValid() && !summon.isDead()) {
                summon.remove();
            }
        }
    }
}
