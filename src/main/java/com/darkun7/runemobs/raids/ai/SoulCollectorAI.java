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
import org.bukkit.entity.Allay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Soul Collector - Gains power each time a player dies nearby, steals XP,
 * summons ghost Vex periodically. Boss is invulnerable while ghosts are alive
 * (particle link like corruption crystals). Raid ends when boss is killed.
 */
public class SoulCollectorAI extends BukkitRunnable implements Listener {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;
    private final Random random = new Random();
    private final List<LivingEntity> ghosts = new ArrayList<>();
    private final Set<UUID> countedDeaths = new HashSet<>();
    private final BattlefieldTerrain terrain;
    private int tickCounter = 0;
    private int soulsCollected = 0;
    private int ghostCooldownEndTick = 0;

    public SoulCollectorAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Transform terrain into soul sanctum
        this.terrain = new BattlefieldTerrain(plugin);
        terrain.applySoulSanctum(boss.getLocation(), 15);

        // Evoker appearance
        if (boss instanceof org.bukkit.entity.Mob mob) {
            mob.getEquipment().setItemInMainHand(new ItemStack(Material.TOTEM_OF_UNDYING));
            mob.getEquipment().setItemInMainHandDropChance(0f);
        }

        boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));

        // Register this as a listener for player death events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().broadcastMessage("§7[§5The Soul Collector§7] §d\"Your souls will fuel my ascension!\"");
    }

    /**
     * Listen for player deaths near the boss to gain power.
     * Uses event-based tracking instead of polling isDead() to avoid
     * double-counting.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!boss.isValid() || boss.isDead())
            return;

        Player player = event.getEntity();
        Location deathLoc = player.getLocation();

        // Check if death occurred near the boss (within 25 blocks)
        if (!deathLoc.getWorld().equals(boss.getWorld()))
            return;
        if (deathLoc.distance(boss.getLocation()) > 25)
            return;

        // Prevent counting the same death twice (edge case protection)
        UUID deathId = player.getUniqueId();
        if (countedDeaths.contains(deathId))
            return;
        countedDeaths.add(deathId);

        // Clear the death tracking after a short delay so player can die again later
        new BukkitRunnable() {
            @Override
            public void run() {
                countedDeaths.remove(deathId);
            }
        }.runTaskLater(plugin, 100L); // 5 seconds cooldown

        soulsCollected++;
        World world = boss.getWorld();
        Location bossLoc = boss.getLocation();

        // Buff the boss
        double maxHp = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(maxHp + 20);
        boss.setHealth(Math.min(boss.getHealth() + 20, maxHp + 20));

        // Buff the boss damage with each soul
        boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE,
                Math.min(soulsCollected - 1, 4), false, false));

        world.spawnParticle(Particle.PORTAL, deathLoc, 30, 0.5, 1, 0.5, 0.1);
        world.playSound(bossLoc, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.5f);
        plugin.getServer().broadcastMessage(
                "§7[§5The Soul Collector§7] §d\"Another soul... I grow stronger!\" §7(Souls: "
                        + soulsCollected + ")");
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

        // Count active ghosts
        ghosts.removeIf(g -> !g.isValid() || g.isDead());
        int activeGhosts = ghosts.size();

        // Boss invulnerability while ghosts exist - cancel damage via event
        if (activeGhosts > 0) {
            // Extreme resistance makes boss essentially invulnerable
            boss.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 254, false, false));

            // Visual link between boss and ghosts (like corruption crystals in corrupted
            // forest)
            for (LivingEntity ghost : ghosts) {
                if (ghost.isValid() && !ghost.isDead()) {
                    Location ghostLoc = ghost.getLocation();
                    int steps = 15;
                    for (int i = 0; i < steps; i++) {
                        double t = (double) i / steps;
                        double x = bossLoc.getX() + (ghostLoc.getX() - bossLoc.getX()) * t;
                        double y = bossLoc.getY() + 1.5 + (ghostLoc.getY() - bossLoc.getY() - 1.5) * t;
                        double z = bossLoc.getZ() + (ghostLoc.getZ() - bossLoc.getZ()) * t;
                        world.spawnParticle(Particle.PORTAL, new Location(world, x, y, z), 1, 0, 0, 0, 0);
                    }
                }
            }

            // Show shield status in name
            boss.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&5The Soul Collector &8[&d" + activeGhosts + " Ghosts Shield&8]"));
        } else {
            // Ghosts gone - boss is vulnerable
            boss.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&5The Soul Collector &8[&cVulnerable&8]"));
        }

        // Soul collector aura
        world.spawnParticle(Particle.PORTAL, bossLoc.clone().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0.02);
        if (soulsCollected > 0) {
            // More souls = more intense aura
            world.spawnParticle(Particle.WITCH, bossLoc.clone().add(0, 1, 0),
                    Math.min(soulsCollected * 2, 10), 0.8, 0.8, 0.8, 0.02);
        }

        // 1. Steal XP from nearby players (every ~4 seconds)
        if (tickCounter % 80 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(10, 5, 10)) {
                if (entity instanceof Player player && !player.isDead()) {
                    int xpToSteal = Math.min(player.getTotalExperience(), 50 + soulsCollected * 10);
                    if (xpToSteal > 0) {
                        player.giveExp(-xpToSteal);
                        world.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0),
                                20, 0.5, 0.5, 0.5, 1.0);
                        world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.3f);
                        player.sendMessage(
                                org.bukkit.ChatColor.DARK_PURPLE + "§oThe Soul Collector drains your mind...");
                    }
                }
            }
        }

        // 2. Summon ghost Vex periodically after cooldown expires
        if (activeGhosts == 0 && tickCounter < ghostCooldownEndTick) {
            // Keep cooldown running, boss is vulnerable
        } else if (activeGhosts == 0 && tickCounter >= ghostCooldownEndTick && tickCounter > 100) {
            // If they are all gone and cooldown is met, immediately respawn 3 ghosts
            world.playSound(bossLoc, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 1.0f, 0.5f);

            int toSpawn = 3;
            for (int i = 0; i < toSpawn; i++) {
                Location spawnLoc = bossLoc.clone().add(
                        random.nextDouble() * 4 - 2, 2, random.nextDouble() * 4 - 2);
                Allay ghost = (Allay) world.spawnEntity(spawnLoc, EntityType.ALLAY);
                ghost.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&dTormented Ghost"));
                ghost.setCustomNameVisible(true);
                ghost.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
                MobSpawnListener.applyRarity(ghost, "rare", plugin);

                // Allays don't naturally attack, so we might need to listen for damage, but
                // here we just register them as the shield
                ghosts.add(ghost);
                raid.addSpawnedEntity(ghost);
            }

            world.spawnParticle(Particle.PORTAL, bossLoc, 30, 2, 2, 2, 0.1);
            plugin.getServer().broadcastMessage(
                    "§7[§5The Soul Collector§7] §5\"My ghosts protect me... destroy them if you dare!\"");

            // Set the cooldown so they won't respawn for 30s AFTER they all die next time
            ghostCooldownEndTick = Integer.MAX_VALUE;
        } else if (activeGhosts > 0) {
            // While they are alive, constantly push back the cooldown to 30s (600 ticks) in
            // the future
            ghostCooldownEndTick = tickCounter + 600;
        }
        // 3. Periodic attack - soul burst on close players (every ~6 seconds)
        if (tickCounter % 120 == 0) {
            for (org.bukkit.entity.Entity entity : boss.getNearbyEntities(6, 4, 6)) {
                if (entity instanceof Player player && !player.isDead()) {
                    // Wither amplifier scales with souls collected (capped at 3)
                    int witherLevel = Math.min(soulsCollected, 3);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, witherLevel, false, true));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, false, true));
                    world.spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.05);
                    world.playSound(player.getLocation(), Sound.ENTITY_VEX_HURT, 1.0f, 0.5f);
                }
            }
        }
    }

    private void cleanUp() {
        // Unregister the listener
        HandlerList.unregisterAll(this);

        terrain.restore();
        for (LivingEntity ghost : ghosts) {
            if (ghost.isValid() && !ghost.isDead()) {
                ghost.remove();
            }
        }
    }
}
