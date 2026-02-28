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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Miner Titan - Giant Husk with pickaxe. Breaks blocks, cave-ins, throws
 * boulders.
 * Spawns HP-stealing bats that heal the boss.
 */
public class MinerTitanAI extends BukkitRunnable {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();
    private final List<LivingEntity> summons = new ArrayList<>();
    private final BattlefieldTerrain terrain;
    private int tickCounter = 0;
    private boolean hasExploded = false;

    public MinerTitanAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Transform terrain into mine zone
        this.terrain = new BattlefieldTerrain(plugin);
        terrain.applyMineZone(boss.getLocation(), 12);

        // Miner titan appearance - big husk with pickaxe
        if (boss instanceof org.bukkit.entity.Mob mob) {
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
            mob.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            mob.getEquipment().setItemInMainHandDropChance(0f);
            mob.getEquipment().setHelmetDropChance(0f);
        }

        // Make it bigger via attribute
        if (boss.getAttribute(org.bukkit.attribute.Attribute.SCALE) != null) {
            boss.getAttribute(org.bukkit.attribute.Attribute.SCALE).setBaseValue(2.5);
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 1, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2, false, false));

        plugin.getServer().broadcastMessage("§7[§6The Miner Titan§7] §e\"The earth shakes beneath your feet!\"");
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

        // 0. Initial explosion to make area spacious (one-time)
        if (!hasExploded && tickCounter == 5) {
            hasExploded = true;
            world.createExplosion(bossLoc, 6.0F, false, true);
            world.playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
            world.spawnParticle(Particle.EXPLOSION_EMITTER, bossLoc, 5);
            plugin.getServer().broadcastMessage("§7[§6The Miner Titan§7] §e\"MAKE ROOM!\"");
        }

        // Dust aura
        world.spawnParticle(Particle.BLOCK, bossLoc.clone().add(0, 1, 0), 3,
                0.5, 0.5, 0.5, 0, Material.STONE.createBlockData());

        // 1. Break blocks around boss path (every ~2 seconds)
        if (tickCounter % 40 == 0) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block b = bossLoc.clone().add(x, y, z).getBlock();
                        if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK
                                && b.getType() != Material.WATER && b.getType() != Material.LAVA
                                && !b.getType().name().contains("CHEST") && !b.getType().name().contains("SPAWNER")) {
                            world.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5),
                                    5, b.getBlockData());
                            b.breakNaturally();
                        }
                    }
                }
            }
        }

        // 2. Cave-in above players (every ~8 seconds)
        if (tickCounter % 160 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(12, 8, 12)) {
                if (entity instanceof Player player && !player.isDead()) {
                    Location above = player.getLocation().clone().add(0, 3, 0);
                    // Drop falling blocks
                    for (int i = 0; i < 3; i++) {
                        Location dropLoc = above.clone().add(random.nextDouble() * 2 - 1, random.nextInt(2),
                                random.nextDouble() * 2 - 1);
                        world.spawnFallingBlock(dropLoc, Material.COBBLESTONE.createBlockData());
                    }
                    world.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 2.0f, 0.5f);
                    world.spawnParticle(Particle.BLOCK, above, 20, 1, 0.5, 1, 0, Material.STONE.createBlockData());
                }
            }
        }

        // 3. Throw boulders at players (every ~6 seconds)
        if (tickCounter % 120 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(15, 8, 15)) {
                if (entity instanceof Player player && !player.isDead()) {
                    Location shootFrom = bossLoc.clone().add(0, 2, 0);
                    Vector direction = player.getLocation().add(0, 1, 0).toVector()
                            .subtract(shootFrom.toVector()).normalize().multiply(1.5);

                    org.bukkit.entity.FallingBlock boulder = world.spawnFallingBlock(
                            shootFrom, Material.COBBLESTONE.createBlockData());
                    boulder.setVelocity(direction);
                    boulder.setDropItem(false);
                    boulder.setHurtEntities(true);

                    world.playSound(bossLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
                    break;
                }
            }
        }

        // 4. Teleport to player or flee (every ~10 seconds)
        if (tickCounter % 200 == 0) {
            Player closest = null;
            double closestDist = 25;
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(25, 10, 25)) {
                if (entity instanceof Player p && !p.isDead()) {
                    double dist = p.getLocation().distance(bossLoc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = p;
                    }
                }
            }

            if (closest != null) {
                Location targetLoc;
                if (closestDist > 10) {
                    // Teleport close to player
                    targetLoc = closest.getLocation().clone().add(
                            random.nextDouble() * 4 - 2, 0, random.nextDouble() * 4 - 2);
                } else {
                    // Flee to a nearby spot
                    targetLoc = bossLoc.clone().add(
                            random.nextDouble() * 8 - 4, 0, random.nextDouble() * 8 - 4);
                }
                // Ensure valid location
                targetLoc.setY(world.getHighestBlockYAt(targetLoc) + 1);

                world.spawnParticle(Particle.BLOCK, bossLoc, 30, 1, 2, 1, 0, Material.STONE.createBlockData());
                boss.teleport(targetLoc);
                world.spawnParticle(Particle.BLOCK, targetLoc, 30, 1, 2, 1, 0, Material.STONE.createBlockData());
                world.playSound(targetLoc, Sound.BLOCK_STONE_BREAK, 2.0f, 0.5f);
            }
        }

        // 5. Summon HP-stealing bats that heal boss (every ~15 seconds, up to wave 4
        // logic)
        if (tickCounter % 300 == 0) {
            summons.removeIf(s -> !s.isValid() || s.isDead());
            if (summons.size() < 6) {
                world.playSound(bossLoc, Sound.ENTITY_BAT_AMBIENT, 2.0f, 0.5f);

                for (int i = 0; i < 3; i++) {
                    Location spawnLoc = bossLoc.clone().add(
                            random.nextDouble() * 4 - 2, 2, random.nextDouble() * 4 - 2);
                    LivingEntity bat = (LivingEntity) world.spawnEntity(spawnLoc, EntityType.BAT);
                    bat.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6Titan Leech"));
                    bat.setCustomNameVisible(true);
                    bat.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                    summons.add(bat);
                    raid.addSpawnedEntity(bat);
                }
            }
        }

        // Bats heal the boss periodically (every ~3 seconds)
        if (tickCounter % 60 == 0) {
            int aliveBats = 0;
            for (LivingEntity bat : summons) {
                if (bat.isValid() && !bat.isDead()) {
                    aliveBats++;
                    // Visual link
                    Location batLoc = bat.getLocation();
                    int steps = 10;
                    for (int i = 0; i < steps; i++) {
                        double t = (double) i / steps;
                        double lx = bossLoc.getX() + (batLoc.getX() - bossLoc.getX()) * t;
                        double ly = bossLoc.getY() + 1 + (batLoc.getY() - bossLoc.getY()) * t;
                        double lz = bossLoc.getZ() + (batLoc.getZ() - bossLoc.getZ()) * t;
                        world.spawnParticle(Particle.DAMAGE_INDICATOR, new Location(world, lx, ly, lz), 1, 0, 0, 0, 0);
                    }
                }
            }
            // Heal boss based on alive bats
            if (aliveBats > 0) {
                double maxHp = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                double healAmount = aliveBats * 2.0;
                boss.setHealth(Math.min(maxHp, boss.getHealth() + healAmount));
            }
        }
    }

    private void cleanUp() {
        terrain.restore();
        for (LivingEntity summon : summons) {
            if (summon.isValid() && !summon.isDead()) {
                summon.remove();
            }
        }
    }
}
