package com.darkun7.runemobs.vanilla;

import com.darkun7.runemobs.RuneMobs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EnderDragonManager implements Listener {

    private final RuneMobs plugin;
    private final Random random = new Random();

    private boolean isEnabled;
    private double healthMultiplier;
    private double damageMultiplier;
    private int crystalRespawnSeconds;
    private double fireballExplosionPower;
    private int aggroEndermanIntervalTicks;
    private int crystalBreakDebuffTicks;

    private EnderDragon currentDragon = null;
    private BukkitRunnable aggroTask = null;

    // To keep track of crystals that need respawning
    private final Set<Location> brokenCrystals = new HashSet<>();

    public EnderDragonManager(RuneMobs plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ender_dragon");
        if (section == null) {
            isEnabled = false;
            return;
        }

        isEnabled = section.getBoolean("enabled", true);
        healthMultiplier = section.getDouble("health_per_player_multiplier", 0.5);
        damageMultiplier = section.getDouble("damage_per_player_multiplier", 0.25);
        crystalRespawnSeconds = section.getInt("crystal_respawn_seconds", 60);
        fireballExplosionPower = section.getDouble("fireball_explosion_power", 3.0);
        aggroEndermanIntervalTicks = section.getInt("aggro_enderman_interval_ticks", 300);
        crystalBreakDebuffTicks = section.getInt("crystal_break_debuff_duration_ticks", 100);
    }

    @EventHandler
    public void onDragonSpawn(EntitySpawnEvent event) {
        if (!isEnabled)
            return;
        if (event.getEntity() instanceof EnderDragon dragon) {
            if ("world_the_end".equals(dragon.getWorld().getName())) {
                startDragonFight(dragon);
            }
        }
    }

    private void startDragonFight(EnderDragon dragon) {
        this.currentDragon = dragon;
        World endWorld = dragon.getWorld();

        // Calculate dynamic scaling
        int playerCount = endWorld.getPlayers().size();
        if (playerCount > 1) {
            int extraPlayers = playerCount - 1;

            // Scale Health
            if (dragon.getAttribute(Attribute.MAX_HEALTH) != null) {
                double baseHealth = 200.0; // Default base health
                double newHealth = baseHealth + (baseHealth * healthMultiplier * extraPlayers);
                dragon.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newHealth);
                dragon.setHealth(newHealth);
            }

            // Scale Damage
            if (dragon.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                double baseDamage = 10.0;
                double newDamage = baseDamage + (baseDamage * damageMultiplier * extraPlayers);
                dragon.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(newDamage);
            }

            plugin.getLogger().info("Scaled Ender Dragon for " + playerCount + " players.");
        }

        startAggroEndermenSpawns(endWorld);
    }

    private void startAggroEndermenSpawns(World endWorld) {
        if (aggroTask != null)
            aggroTask.cancel();

        aggroTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentDragon == null || currentDragon.isDead() || !currentDragon.isValid()) {
                    this.cancel();
                    return;
                }

                for (Player p : endWorld.getPlayers()) {
                    if (p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR)
                        continue;

                    // Spawn an enderman directly near the player
                    Location spawnLoc = p.getLocation().add((random.nextDouble() - 0.5) * 10, 0,
                            (random.nextDouble() - 0.5) * 10);
                    spawnLoc.setY(endWorld.getHighestBlockYAt(spawnLoc) + 1);

                    if (spawnLoc.getBlock().getType().isAir()) {
                        Enderman aggroEnderman = (Enderman) endWorld.spawnEntity(spawnLoc, EntityType.ENDERMAN);
                        aggroEnderman.setTarget(p); // Immediately aggro

                        // Give them a slightly glowing visual so players know they are targeted
                        aggroEnderman.setGlowing(true);

                        // Small particles to show it popped in specifically to kill them
                        endWorld.spawnParticle(Particle.PORTAL, spawnLoc, 50, 0.5, 1, 0.5, 0.1);
                        endWorld.playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    }
                }
            }
        };
        aggroTask.runTaskTimer(plugin, aggroEndermanIntervalTicks, aggroEndermanIntervalTicks);
    }

    @EventHandler
    public void onNaturalEndermanSpawn(CreatureSpawnEvent event) {
        if (!isEnabled)
            return;
        if (currentDragon == null || currentDragon.isDead() || !currentDragon.isValid())
            return;

        // Prevent natural enderman spawns while the dragon is alive to control the
        // chaos entirely via our custom task
        if (event.getEntity() instanceof Enderman && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            if ("world_the_end".equals(event.getEntity().getWorld().getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFireballHit(ProjectileHitEvent event) {
        if (!isEnabled)
            return;
        if (event.getEntity() instanceof DragonFireball) {
            // Explode it to add physical damage and knockback on top of the lingering
            // potion effect
            Location hitLoc = event.getEntity().getLocation();
            hitLoc.getWorld().createExplosion(hitLoc, (float) fireballExplosionPower, false, false);
        }
    }

    @EventHandler
    public void onCrystalBreak(EntityDamageByEntityEvent event) {
        if (!isEnabled)
            return;
        if (event.getEntity() instanceof EnderCrystal crystal) {
            World endWorld = crystal.getWorld();
            if (!"world_the_end".equals(endWorld.getName()))
                return;

            // Handle debuffs and spawns
            Player breaker = null;
            if (event.getDamager() instanceof Player p) {
                breaker = p;
            } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj) {
                if (proj.getShooter() instanceof Player p) {
                    breaker = p;
                }
            }

            Location crystalLoc = crystal.getLocation().clone();

            if (breaker != null) {
                // Apply severe debuffs for breaking a crystal
                if (random.nextBoolean()) {
                    breaker.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, crystalBreakDebuffTicks, 1));
                } else {
                    breaker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, crystalBreakDebuffTicks, 0));
                }

                // Spawn angry endermites at the crystal location to fall on players
                int miteCount = 2 + random.nextInt(3);
                for (int i = 0; i < miteCount; i++) {
                    endWorld.spawnEntity(crystalLoc, EntityType.ENDERMITE);
                }
            }

            // Schedule the crystal to respawn
            if (currentDragon != null && !currentDragon.isDead()) {
                scheduleCrystalRespawn(crystalLoc, endWorld);
            }
        }
    }

    private void scheduleCrystalRespawn(Location loc, World world) {
        // Prevent stacking respawns if multiple things happen at that exact spot
        if (brokenCrystals.contains(loc))
            return;
        brokenCrystals.add(loc);

        new BukkitRunnable() {
            @Override
            public void run() {
                brokenCrystals.remove(loc);
                if (currentDragon == null || currentDragon.isDead() || !currentDragon.isValid()) {
                    return; // Don't respawn if the dragon died while we waited
                }

                // Respawn the crystal
                world.spawnEntity(loc, EntityType.END_CRYSTAL);
                world.strikeLightningEffect(loc); // Visual + sound impact so players know it's back
                plugin.getLogger().info("An End Crystal has respawned!");
            }
        }.runTaskLater(plugin, crystalRespawnSeconds * 20L);
    }
}
