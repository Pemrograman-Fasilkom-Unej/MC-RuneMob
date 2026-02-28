package com.darkun7.runemobs.raids.ai;

import com.darkun7.runemobs.RuneMobs;
import com.darkun7.runemobs.MobSpawnListener;
import com.darkun7.runemobs.raids.RuneRaid;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
 * The Nether Warlord - Fire waves, wither skeleton army, teleports behind
 * players, fire projectiles.
 * Phase change: turns into demon form (black skeleton) at low HP.
 */
public class NetherWarlordAI extends BukkitRunnable {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();
    private final List<LivingEntity> summons = new ArrayList<>();
    private final BattlefieldTerrain terrain;
    private int tickCounter = 0;
    private boolean demonPhase = false;

    public NetherWarlordAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Transform terrain into nether corruption zone
        this.terrain = new BattlefieldTerrain(plugin);
        terrain.applyNetherCorruption(boss.getLocation(), 12);

        // White skeleton warlord appearance
        if (boss instanceof org.bukkit.entity.Mob mob) {
            mob.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            mob.getEquipment().setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
            mob.getEquipment().setHelmetDropChance(0f);
            mob.getEquipment().setChestplateDropChance(0f);
            mob.getEquipment().setItemInMainHandDropChance(0f);
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        plugin.getServer().broadcastMessage("§7[§cThe Nether Warlord§7] §4\"The Nether bows to no mortal!\"");
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

        // Phase check - Demon form at 30% HP
        double healthPercent = boss.getHealth()
                / boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        if (!demonPhase && healthPercent <= 0.30) {
            demonPhase = true;
            boss.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&4&lDemon Warlord"));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));

            // Transform visuals
            if (boss instanceof org.bukkit.entity.Mob mob) {
                mob.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                mob.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                mob.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                mob.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
                mob.getEquipment().setLeggingsDropChance(0f);
                mob.getEquipment().setBootsDropChance(0f);
            }

            world.spawnParticle(Particle.EXPLOSION_EMITTER, bossLoc, 3);
            world.playSound(bossLoc, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            plugin.getServer().broadcastMessage("§7[§4The Nether Warlord§7] §c\"You've awakened my TRUE power!\"");
        }

        // Fire aura particles
        Particle auraParticle = demonPhase ? Particle.SOUL_FIRE_FLAME : Particle.FLAME;
        world.spawnParticle(auraParticle, bossLoc.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.02);

        // 1. Fire wave (every ~4 seconds, faster in demon phase)
        int fireInterval = demonPhase ? 40 : 80;
        if (tickCounter % fireInterval == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(10, 5, 10)) {
                if (entity instanceof Player player && !player.isDead()) {
                    player.setFireTicks(demonPhase ? 120 : 60);
                    world.spawnParticle(Particle.FLAME, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    world.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
                }
            }
        }

        // 2. Teleport behind closest player (every ~6 seconds)
        if (tickCounter % 120 == 0) {
            Player closest = null;
            double closestDist = 20;
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(20, 10, 20)) {
                if (entity instanceof Player p && !p.isDead()) {
                    double dist = p.getLocation().distance(bossLoc);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = p;
                    }
                }
            }
            if (closest != null) {
                Location behind = closest.getLocation().clone();
                Vector dir = closest.getLocation().getDirection().setY(0).normalize();
                behind.subtract(dir.multiply(2));

                // Keep the Y safe
                behind.setY(closest.getLocation().getY());

                world.spawnParticle(Particle.SMOKE, bossLoc, 20, 0.5, 1, 0.5, 0.1);
                boss.teleport(behind);
                world.spawnParticle(demonPhase ? Particle.SOUL_FIRE_FLAME : Particle.SMOKE, behind, 20, 0.5, 1, 0.5,
                        0.1);
                world.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
        }

        // 3. Launch fire projectile (every ~5 seconds)
        if (tickCounter % 100 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(20, 10, 20)) {
                if (entity instanceof Player player && !player.isDead()) {
                    Location shootFrom = bossLoc.clone().add(0, 1.5, 0);
                    Vector direction = player.getLocation().add(0, 1, 0).toVector()
                            .subtract(shootFrom.toVector()).normalize();

                    // Spawn the fireball slightly in front of the boss so it doesn't instantly hit
                    // itself
                    shootFrom.add(direction.clone().multiply(1.5));

                    org.bukkit.entity.SmallFireball fireball = world.spawn(shootFrom,
                            org.bukkit.entity.SmallFireball.class);
                    fireball.setDirection(direction);
                    fireball.setShooter(boss);
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0); // No explosion

                    world.playSound(bossLoc, Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                    break; // Only target one player per volley
                }
            }
        }

        // 4. Summon wither skeleton army (every ~18 seconds)
        if (tickCounter % 360 == 0) {
            summons.removeIf(s -> !s.isValid() || s.isDead());
            if (summons.size() < 4) {
                world.playSound(bossLoc, Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.0f, 0.5f);
                int count = demonPhase ? 3 : 2;
                for (int i = 0; i < count; i++) {
                    Location spawnLoc = bossLoc.clone().add(random.nextDouble() * 6 - 3, 0,
                            random.nextDouble() * 6 - 3);
                    LivingEntity skeleton = (LivingEntity) world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
                    skeleton.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&4Nether Guard"));
                    skeleton.setCustomNameVisible(true);
                    skeleton.addPotionEffect(
                            new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                    MobSpawnListener.applyRarity(skeleton, "epic", plugin);

                    if (boss instanceof org.bukkit.entity.Mob bm && bm.getTarget() != null) {
                        if (skeleton instanceof org.bukkit.entity.Mob sm) {
                            sm.setTarget(bm.getTarget());
                        }
                    }
                    summons.add(skeleton);
                    raid.addSpawnedEntity(skeleton);
                }
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, bossLoc, 30, 2, 1, 2, 0.1);
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
