package com.darkun7.runemobs.raids.ai;

import com.darkun7.runemobs.RuneMobs;
import com.darkun7.runemobs.MobSpawnListener;
import com.darkun7.runemobs.raids.RuneRaid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GuardianBossAI extends BukkitRunnable {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();

    private final List<EnderCrystal> crystals = new ArrayList<>();
    private final List<LivingEntity> summons = new ArrayList<>();
    private final BattlefieldTerrain terrain;
    private int tickCounter = 0;

    public GuardianBossAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Transform terrain into corrupted forest
        this.terrain = new BattlefieldTerrain(plugin);
        terrain.applyCorruptedForest(boss.getLocation(), 15);

        spawnCrystals();
    }

    private void spawnCrystals() {
        World world = boss.getWorld();
        Location center = boss.getLocation();

        plugin.getServer()
                .broadcastMessage("§7[§2The Corrupted Forest Guardian§7] §a\"The forest will reclaim you...\"");

        // Spawn 3-4 Corruption Crystals nearby
        int numCrystals = 3 + random.nextInt(2);
        for (int i = 0; i < numCrystals; i++) {
            double angle = 2 * Math.PI * i / numCrystals;
            double radius = 8 + random.nextDouble() * 4; // 8-12 blocks away

            int x = (int) (center.getX() + radius * Math.cos(angle));
            int z = (int) (center.getZ() + radius * Math.sin(angle));
            int y = world.getHighestBlockYAt(x, z);

            Location crystalLoc = new Location(world, x + 0.5, y + 1, z + 0.5);
            EnderCrystal crystal = (EnderCrystal) world.spawnEntity(crystalLoc, EntityType.END_CRYSTAL);
            crystal.setShowingBottom(true);
            crystal.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aCorruption Crystal"));
            crystal.setCustomNameVisible(true);
            crystals.add(crystal);

            // Spawn some corrupted blocks under it as a visual tell
            world.spawnParticle(Particle.HAPPY_VILLAGER, crystalLoc, 50, 1.0, 1.0, 1.0, 0.1);
        }
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

        // 1. Crystal mechanics tracking
        crystals.removeIf(crystal -> !crystal.isValid() || crystal.isDead());
        int activeCrystals = crystals.size();

        if (activeCrystals > 0) {
            // Boss receives extreme damage resistance and regeneration based on living
            // crystals
            boss.addPotionEffect(
                    new PotionEffect(PotionEffectType.RESISTANCE, 40, activeCrystals - 1, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, activeCrystals - 1, false, false));

            // Visual link between boss and crystals
            for (EnderCrystal crystal : crystals) {
                if (tickCounter % 10 == 0) {
                    Location from = crystal.getLocation().add(0, 1, 0);
                    Location to = bossLoc.clone().add(0, 1.5, 0);
                    org.bukkit.util.Vector dir = to.toVector().subtract(from.toVector());
                    double dist = dir.length();
                    dir.normalize();
                    for (double i = 0; i < dist; i += 1) {
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, from.clone().add(dir.clone().multiply(i)), 1, 0,
                                0, 0, 0);
                    }
                }
            }
            boss.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&2Corrupted Guardian &8[" + activeCrystals + " Crystals Remaining]"));
        } else {
            // All crystals destroyed - weaken the boss
            if (tickCounter % 40 == 0) {
                world.spawnParticle(Particle.DAMAGE_INDICATOR, bossLoc.clone().add(0, 1.5, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }
            boss.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&2Weakened Guardian"));
        }

        // 2. Healing near trees (Logs)
        if (tickCounter % 20 == 0) { // Check every 1 second
            boolean nearTree = false;
            for (int x = -3; x <= 3; x++) {
                for (int y = -2; y <= 4; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Block b = bossLoc.clone().add(x, y, z).getBlock();
                        if (b.getType().name().contains("LOG")) {
                            nearTree = true;
                            break;
                        }
                    }
                    if (nearTree)
                        break;
                }
                if (nearTree)
                    break;
            }
            if (nearTree) {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 2, false, false));
                world.spawnParticle(Particle.HAPPY_VILLAGER, bossLoc.clone().add(0, 1, 0), 10, 0.5, 1.0, 0.5, 0);
            }
        }

        // 3. Roots trap players mechanic (Every ~8 seconds)
        if (tickCounter % 160 == 0) {
            boolean trapped = false;
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(12, 5, 12)) {
                if (entity instanceof Player player && !player.isDead()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, false));
                    world.playSound(player.getLocation(), Sound.BLOCK_VINE_PLACE, 1.0f, 0.5f);
                    // Spawn physical visual roots (particles acting as vines wrapping them)
                    world.spawnParticle(Particle.ITEM_SLIME, player.getLocation(), 40, 0.3, 0.5, 0.3, 0);
                    player.sendMessage(
                            org.bukkit.ChatColor.DARK_GREEN + "Roots erupt from the ground and trap your feet!");
                    trapped = true;
                }
            }
            if (trapped) {
                world.playSound(bossLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
            }
        }

        // 4. Summons Wolves (Every ~15 seconds)
        if (tickCounter % 300 == 0 && activeCrystals > 0) {
            summons.removeIf(s -> !s.isValid() || s.isDead());

            // Only summon if there aren't too many alive already
            if (summons.size() < 6) {
                world.playSound(bossLoc, Sound.ENTITY_WOLF_AMBIENT, 1.0f, 0.8f);
                for (int i = 0; i < 2; i++) {
                    Location spawnLoc = bossLoc.clone().add(random.nextDouble() * 6 - 3, 0,
                            random.nextDouble() * 6 - 3);
                    Wolf wolf = (Wolf) world.spawnEntity(spawnLoc, EntityType.WOLF);
                    wolf.setAngry(true);
                    wolf.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&2Corrupted Wolf"));
                    wolf.setCustomNameVisible(true);
                    wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                    MobSpawnListener.applyRarity(wolf, "epic", plugin);
                    if (boss instanceof org.bukkit.entity.Mob bm && bm.getTarget() != null) {
                        wolf.setTarget(bm.getTarget());
                    }
                    summons.add(wolf);
                    raid.addSpawnedEntity(wolf); // Bind to raid so they die with the boss
                }
            }
        }
    }

    // Safety cleanup in case boss arbitrarily despawns outside standard death
    private void cleanUp() {
        terrain.restore();
        for (EnderCrystal crystal : crystals) {
            if (crystal.isValid() && !crystal.isDead()) {
                crystal.remove();
            }
        }
        for (LivingEntity summon : summons) {
            if (summon.isValid() && !summon.isDead()) {
                summon.remove();
            }
        }
    }
}
