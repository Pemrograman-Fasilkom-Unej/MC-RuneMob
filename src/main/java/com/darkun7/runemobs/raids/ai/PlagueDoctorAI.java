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
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Plague Doctor - Throws poison potions, infects players (DoT), raises
 * villagers as zombies.
 * Boss spawned since wave 1. Raid ends when boss is killed.
 */
public class PlagueDoctorAI extends BukkitRunnable {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();
    private final List<LivingEntity> summons = new ArrayList<>();
    private final BattlefieldTerrain terrain;
    private int tickCounter = 0;

    public PlagueDoctorAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Transform terrain into plague zone
        this.terrain = new BattlefieldTerrain(plugin);
        terrain.applyPlagueZone(boss.getLocation(), 15);

        // Give boss plague doctor appearance - dark leather armor
        if (boss instanceof org.bukkit.entity.Mob mob) {
            ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
            if (helmet.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta helmetMeta) {
                helmetMeta.setColor(org.bukkit.Color.fromRGB(0x1B3A1B));
                helmet.setItemMeta(helmetMeta);
            }
            ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
            if (chestplate.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta chestMeta) {
                chestMeta.setColor(org.bukkit.Color.fromRGB(0x1B3A1B));
                chestplate.setItemMeta(chestMeta);
            }
            ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
            if (leggings.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta legMeta) {
                legMeta.setColor(org.bukkit.Color.fromRGB(0x1B3A1B));
                leggings.setItemMeta(legMeta);
            }
            ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
            if (boots.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta bootMeta) {
                bootMeta.setColor(org.bukkit.Color.fromRGB(0x1B3A1B));
                boots.setItemMeta(bootMeta);
            }

            mob.getEquipment().setHelmet(helmet);
            mob.getEquipment().setChestplate(chestplate);
            mob.getEquipment().setLeggings(leggings);
            mob.getEquipment().setBoots(boots);
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.SPLASH_POTION));
            mob.getEquipment().setHelmetDropChance(0f);
            mob.getEquipment().setChestplateDropChance(0f);
            mob.getEquipment().setLeggingsDropChance(0f);
            mob.getEquipment().setBootsDropChance(0f);
            mob.getEquipment().setItemInMainHandDropChance(0f);
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        plugin.getServer()
                .broadcastMessage("§7[§2The Plague Doctor§7] §a\"Breathe deep... my cure is most effective.\"");
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

        // Plague aura particles
        world.spawnParticle(Particle.HAPPY_VILLAGER, bossLoc.clone().add(0, 1.5, 0), 5, 0.8, 0.8, 0.8, 0);
        world.spawnParticle(Particle.ITEM_SLIME, bossLoc.clone().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0);

        // 1. Throw actual splash poison potions at players (every ~4 seconds)
        if (tickCounter % 80 == 0) {
            Player target = getNearestPlayer(15);
            if (target != null) {
                Location shootFrom = bossLoc.clone().add(0, 1.5, 0);
                Vector direction = target.getLocation().add(0, 1, 0).toVector()
                        .subtract(shootFrom.toVector()).normalize().multiply(1.2);
                // Add arc
                direction.setY(direction.getY() + 0.4);

                ThrownPotion potion = world.spawn(shootFrom, ThrownPotion.class);
                ItemStack potionItem = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) potionItem.getItemMeta();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 120, 1, false, true), true);
                meta.addCustomEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 2, false, true), true);
                potionItem.setItemMeta(meta);
                potion.setItem(potionItem);
                potion.setVelocity(direction);
                potion.setShooter(boss);

                world.playSound(bossLoc, Sound.ENTITY_WITCH_THROW, 1.0f, 0.8f);
            }
        }

        // 2. Infection spread - damage over time aura on close players (every ~5
        // seconds)
        if (tickCounter % 100 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(8, 5, 8)) {
                if (entity instanceof Player player && !player.isDead()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, false, true));
                    world.spawnParticle(Particle.DUST_PLUME, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
                    player.sendMessage(
                            org.bukkit.ChatColor.DARK_GREEN + "§oYou feel the plague spreading through your veins...");
                }
            }
            world.playSound(bossLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.5f);
        }

        // 3. Raise villagers as zombies (every ~15 seconds)
        if (tickCounter % 300 == 0) {
            summons.removeIf(s -> !s.isValid() || s.isDead());
            if (summons.size() < 5) {
                world.playSound(bossLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.5f);
                plugin.getServer()
                        .broadcastMessage("§7[§2The Plague Doctor§7] §a\"Rise, my subjects... serve your doctor!\"");

                for (int i = 0; i < 2; i++) {
                    Location spawnLoc = bossLoc.clone().add(random.nextDouble() * 6 - 3, 0,
                            random.nextDouble() * 6 - 3);
                    Zombie zombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE_VILLAGER);
                    zombie.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&2Plagued Villager"));
                    zombie.setCustomNameVisible(true);
                    zombie.addPotionEffect(
                            new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                    zombie.addPotionEffect(
                            new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                    MobSpawnListener.applyRarity(zombie, "epic", plugin);

                    if (boss instanceof org.bukkit.entity.Mob bm && bm.getTarget() != null) {
                        zombie.setTarget(bm.getTarget());
                    }
                    summons.add(zombie);
                    raid.addSpawnedEntity(zombie);
                }

                world.spawnParticle(Particle.ITEM_SLIME, bossLoc, 30, 2, 1, 2, 0.1);
            }
        }
    }

    private Player getNearestPlayer(double range) {
        Player nearest = null;
        double nearestDist = range;
        for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(range, range / 2, range)) {
            if (entity instanceof Player p && !p.isDead()
                    && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                double dist = p.getLocation().distance(boss.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = p;
                }
            }
        }
        return nearest;
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
