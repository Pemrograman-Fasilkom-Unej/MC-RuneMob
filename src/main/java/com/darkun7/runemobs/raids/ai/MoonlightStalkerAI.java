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
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/**
 * The Moonlight Stalker - Invisible in darkness, teleports, hunts one player at
 * a time. Spawns clones; killing a clone gives the killer blindness.
 * Only spawns at night in the Overworld. Raid ends when boss is killed.
 */
public class MoonlightStalkerAI extends BukkitRunnable implements Listener {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();
    private final List<LivingEntity> clones = new ArrayList<>();
    private final Set<UUID> cloneUUIDs = new HashSet<>();
    private final BattlefieldTerrain terrain;
    private int tickCounter = 0;
    private Player huntTarget = null;

    public MoonlightStalkerAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Transform terrain into shadow domain
        this.terrain = new BattlefieldTerrain(plugin);
        terrain.applyShadowDomain(boss.getLocation(), 15);

        // Stalker appearance
        if (boss instanceof org.bukkit.entity.Mob mob) {
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
            mob.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
            mob.getEquipment().setItemInMainHandDropChance(0f);
            mob.getEquipment().setHelmetDropChance(0f);
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));

        // Register listener for clone death tracking
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().broadcastMessage("§7[§8The Moonlight Stalker§7] §7\"You can't see me... but I see you.\"");
    }

    /**
     * Listen for clone deaths and apply blindness to the killer player.
     */
    @EventHandler
    public void onCloneDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!cloneUUIDs.contains(dead.getUniqueId()))
            return;

        // Remove from tracking
        cloneUUIDs.remove(dead.getUniqueId());
        clones.remove(dead);

        // Apply blindness to the actual killer
        Player killer = dead.getKiller();
        if (killer != null) {
            World world = killer.getWorld();
            killer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0, false, true));
            killer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true));
            killer.sendMessage(org.bukkit.ChatColor.DARK_GRAY + "§oThe shadow dissolves into your eyes...");
            world.spawnParticle(Particle.SMOKE, killer.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
            world.playSound(killer.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 1.0f, 0.3f);
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

        // 1. Invisibility in darkness - check light level
        int lightLevel = bossLoc.getBlock().getLightLevel();
        if (lightLevel <= 7) {
            if (!boss.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                boss.addPotionEffect(
                        new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                // Remove glowing when invisible to make it actually hidden
                boss.removePotionEffect(PotionEffectType.GLOWING);
                world.spawnParticle(Particle.SMOKE, bossLoc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.02);
            }
        } else {
            if (boss.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                boss.removePotionEffect(PotionEffectType.INVISIBILITY);
                boss.addPotionEffect(
                        new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                world.spawnParticle(Particle.END_ROD, bossLoc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
            }
        }

        // Eerie particles
        world.spawnParticle(Particle.SMOKE, bossLoc.clone().add(0, 1.5, 0), 2, 0.3, 0.3, 0.3, 0.01);

        // 2. Hunt target selection (every ~5 seconds, pick one player to stalk)
        if (tickCounter % 100 == 0 || huntTarget == null || huntTarget.isDead() || !huntTarget.isOnline()) {
            List<Player> nearby = new ArrayList<>();
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(30, 15, 30)) {
                if (entity instanceof Player p && !p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    nearby.add(p);
                }
            }
            if (!nearby.isEmpty()) {
                huntTarget = nearby.get(random.nextInt(nearby.size()));
                huntTarget.sendMessage(
                        org.bukkit.ChatColor.DARK_GRAY + "§o[You feel eyes watching you from the shadows...]");
                world.playSound(huntTarget.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);

                // Force boss aggro
                if (boss instanceof org.bukkit.entity.Mob mob) {
                    mob.setTarget(huntTarget);
                }
            }
        }

        // 3. Teleport to hunt target (every ~4 seconds)
        if (tickCounter % 80 == 0 && huntTarget != null && !huntTarget.isDead()) {
            Location behind = huntTarget.getLocation().clone();
            org.bukkit.util.Vector dir = huntTarget.getLocation().getDirection().normalize().multiply(-3);
            behind.add(dir);

            world.spawnParticle(Particle.SMOKE, bossLoc, 15, 0.5, 1, 0.5, 0.05);
            boss.teleport(behind);
            world.spawnParticle(Particle.SMOKE, behind, 15, 0.5, 1, 0.5, 0.05);
            world.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.3f);
        }

        // 4. Spawn clones (every ~20 seconds)
        if (tickCounter % 400 == 0) {
            clones.removeIf(c -> !c.isValid() || c.isDead());
            if (clones.size() < 3) {
                world.playSound(bossLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.8f);
                plugin.getServer().broadcastMessage("§7[§8The Moonlight Stalker§7] §7\"Can you tell which is real?\"");

                for (int i = 0; i < 2; i++) {
                    Location spawnLoc = bossLoc.clone().add(
                            random.nextDouble() * 8 - 4, 0, random.nextDouble() * 8 - 4);
                    LivingEntity clone = (LivingEntity) world.spawnEntity(spawnLoc, EntityType.VINDICATOR);
                    clone.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Shadow Clone"));
                    clone.setCustomNameVisible(true);
                    clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
                    clone.addPotionEffect(
                            new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

                    if (clone instanceof org.bukkit.entity.Mob mob) {
                        mob.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_AXE));
                        mob.getEquipment().setHelmet(new ItemStack(Material.CARVED_PUMPKIN));
                        mob.getEquipment().setItemInMainHandDropChance(0f);
                        mob.getEquipment().setHelmetDropChance(0f);
                    }

                    MobSpawnListener.applyRarity(clone, "rare", plugin);
                    clones.add(clone);
                    cloneUUIDs.add(clone.getUniqueId());
                    raid.addSpawnedEntity(clone);
                }

                world.spawnParticle(Particle.SMOKE, bossLoc, 30, 2, 1, 2, 0.1);
            }
        }
    }

    private void cleanUp() {
        // Unregister the listener
        HandlerList.unregisterAll(this);

        terrain.restore();
        for (LivingEntity clone : clones) {
            if (clone.isValid() && !clone.isDead()) {
                clone.remove();
            }
        }
    }
}
