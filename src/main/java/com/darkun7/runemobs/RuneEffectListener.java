package com.darkun7.runemobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles all rune EFFECT triggers: combat procs, abilities, movement, etc.
 * Core rune application logic stays in RuneApplyListener.
 */
public class RuneEffectListener implements Listener {

    private final RuneMobs plugin;
    private final Map<UUID, Integer> jumpCounts = new java.util.HashMap<>();

    public RuneEffectListener(RuneMobs plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // COMBAT PROCS
    // =========================================================================

    @EventHandler
    public void onRuneCombatProc(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player))
            return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || !weapon.hasItemMeta())
            return;

        NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
        if (weapon.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
            String typeStr = weapon.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

            if ("VAMPIRISM".equals(typeStr)) {
                double healAmount = event.getFinalDamage() * 0.20;
                if (healAmount > 0) {
                    double currentHp = player.getHealth();
                    double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                    player.setHealth(Math.min(maxHp, currentHp + healAmount));

                    if (Math.random() < 0.2) {
                        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 3,
                                0.3, 0.3, 0.3, 0);
                    }
                }
            } else if ("ASCENDED_VAMPIRISM".equals(typeStr)) {
                // 35% lifesteal + Wither on enemy
                double healAmount = event.getFinalDamage() * 0.35;
                if (healAmount > 0) {
                    double currentHp = player.getHealth();
                    double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                    player.setHealth(Math.min(maxHp, currentHp + healAmount));
                    player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1, 0), 5,
                            0.3, 0.3, 0.3, 0);
                }
                if (event.getEntity() instanceof LivingEntity target) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                    target.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, target.getLocation().add(0, 1, 0), 8,
                            0.3, 0.3, 0.3, 0.02);
                }
            } else if ("VENOM".equals(typeStr)) {
                if (event.getEntity() instanceof LivingEntity target) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 1));
                    player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                            target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
                }
            } else if ("ASCENDED_VENOM".equals(typeStr)) {
                // Poison III + Wither I
                if (event.getEntity() instanceof LivingEntity target) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 2)); // Poison III
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0)); // Wither I
                    player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                            target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
                    target.getWorld().spawnParticle(org.bukkit.Particle.SMOKE,
                            target.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.02);
                }
            } else if ("STORMCALLER".equals(typeStr)) {
                if (Math.random() <= 0.50) {
                    org.bukkit.entity.Entity target = event.getEntity();
                    playCustomLightning(target.getLocation());
                    if (target instanceof org.bukkit.entity.Damageable d) {
                        d.damage(5.0); // No player attribute to avoid infinite loops
                    }
                }
            } else if ("ASCENDED_STORMCALLER".equals(typeStr)) {
                // 75% chance + chain to 3 nearby enemies
                if (Math.random() <= 0.75) {
                    org.bukkit.entity.Entity target = event.getEntity();
                    playCustomLightning(target.getLocation());
                    if (target instanceof org.bukkit.entity.Damageable d) {
                        d.damage(6.0); // No player attribute
                    }
                    // Chain to nearby enemies
                    int chained = 0;
                    for (org.bukkit.entity.Entity nearby : target.getNearbyEntities(4, 4, 4)) {
                        if (nearby instanceof LivingEntity le && nearby != player && chained < 3) {
                            playCustomLightningChain(target.getLocation(), le.getLocation());
                            le.damage(3.0); // No player attribute
                            chained++;
                        }
                    }
                }
            } else if ("BERSERK".equals(typeStr)) {
                double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHp = player.getHealth();
                double thresholdPercent = currentHp / maxHp;

                if (thresholdPercent <= 0.5) {
                    double boost = 1.0 + (0.5 - thresholdPercent) * 2.0;
                    event.setDamage(event.getDamage() * boost);
                    player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR,
                            event.getEntity().getLocation().add(0, 1, 0), 3);
                }
            } else if ("ASCENDED_BERSERK".equals(typeStr)) {
                // Higher scaling + lifesteal below 25% HP
                double maxHp = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                double currentHp = player.getHealth();
                double thresholdPercent = currentHp / maxHp;

                if (thresholdPercent <= 0.5) {
                    double boost = 1.0 + (0.5 - thresholdPercent) * 3.0; // Higher scaling
                    event.setDamage(event.getDamage() * boost);
                    player.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR,
                            event.getEntity().getLocation().add(0, 1, 0), 5);

                    // Lifesteal below 25% HP
                    if (thresholdPercent <= 0.25) {
                        double healAmount = event.getFinalDamage() * 0.15;
                        player.setHealth(Math.min(maxHp, currentHp + healAmount));
                        player.getWorld().spawnParticle(org.bukkit.Particle.HEART,
                                player.getLocation().add(0, 1.5, 0), 3, 0.3, 0.3, 0.3, 0);
                    }
                }
            } else if ("SCYTHE".equals(typeStr)) {
                event.setDamage(event.getDamage() * 1.5);
                if (event.getEntity() instanceof Player targetPlayer) {
                    targetPlayer.setFoodLevel(Math.max(0, targetPlayer.getFoodLevel() - 4));
                }

                org.bukkit.Location loc = event.getEntity().getLocation();
                for (double t = 0; t < 2 * Math.PI; t += Math.PI / 8) {
                    double x = 1.5 * Math.cos(t);
                    double z = 1.5 * Math.sin(t);
                    player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, loc.clone().add(x, 1, z), 1);
                }

                for (org.bukkit.entity.Entity e : player.getNearbyEntities(2, 2, 2)) {
                    if (e instanceof LivingEntity le && e != player && e != event.getEntity()) {
                        le.damage(event.getDamage() * 0.5); // Damage without attributing to player to avoid infinite
                                                            // loop
                    }
                }
            } else if ("SPEAR_THRUST".equals(typeStr)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
                if (event.getEntity() instanceof LivingEntity target) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 255));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128));

                    org.bukkit.util.Vector push = player.getLocation().getDirection().normalize().multiply(1.5)
                            .setY(0.2);
                    target.setVelocity(target.getVelocity().add(push));
                }
            }
        }
    }

    // =========================================================================
    // DEFENSIVE PROCS
    // =========================================================================

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof LivingEntity attacker) {

            // Check Helmet
            ItemStack helmet = player.getInventory().getHelmet();
            NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");

            if (helmet != null && helmet.hasItemMeta()
                    && helmet.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                String typeStr = helmet.getItemMeta().getPersistentDataContainer().get(typeKey,
                        PersistentDataType.STRING);
                if ("RETRIBUTION".equals(typeStr)) {
                    attacker.setFireTicks(80);
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.FLAME, attacker.getLocation().add(0, 1, 0),
                            10, 0.2, 0.2, 0.2, 0.05);
                } else if ("ASCENDED_RETRIBUTION".equals(typeStr)) {
                    // 8s fire + small explosion
                    attacker.setFireTicks(160); // 8 seconds
                    attacker.getWorld().spawnParticle(org.bukkit.Particle.FLAME, attacker.getLocation().add(0, 1, 0),
                            20, 0.3, 0.3, 0.3, 0.08);
                    attacker.getWorld().createExplosion(attacker.getLocation(), 1.5F, false, false);
                } else if ("SLOW_AURA".equals(typeStr)) {
                    for (org.bukkit.entity.Entity e : player.getNearbyEntities(4, 4, 4)) {
                        if (e instanceof LivingEntity le && e != player) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 60, 1));
                            le.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, le.getLocation().add(0, 2, 0),
                                    10, 0.5, 0.2, 0.5, 0.05);
                        }
                    }
                }
            }

            // Check Shield
            for (ItemStack item : new ItemStack[] { player.getInventory().getItemInMainHand(),
                    player.getInventory().getItemInOffHand() }) {
                if (item != null && item.getType() == Material.SHIELD && item.hasItemMeta()) {
                    String typeStr = item.getItemMeta().getPersistentDataContainer().get(typeKey,
                            PersistentDataType.STRING);
                    if ("RETRIBUTION".equals(typeStr)) {
                        attacker.setFireTicks(80);
                        attacker.getWorld().spawnParticle(org.bukkit.Particle.FLAME,
                                attacker.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);
                    } else if ("ASCENDED_RETRIBUTION".equals(typeStr)) {
                        // 8s fire + small explosion on shield too
                        attacker.setFireTicks(160);
                        attacker.getWorld().spawnParticle(org.bukkit.Particle.FLAME,
                                attacker.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.08);
                        attacker.getWorld().createExplosion(attacker.getLocation(), 1.5F, false, false);
                    } else if ("LIGHTNING_SHIELD".equals(typeStr)) {
                        if (player.isBlocking()) {
                            playCustomLightning(attacker.getLocation());
                            attacker.damage(5.0); // No player attribute
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // PROJECTILE EFFECTS
    // =========================================================================

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getShooter() instanceof Player player) {

            // Detonation (Bow/Crossbow)
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon != null && (weapon.getType() == Material.BOW || weapon.getType() == Material.CROSSBOW)) {
                if (weapon.hasItemMeta()) {
                    NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                    if ("DETONATION".equals(weapon.getItemMeta().getPersistentDataContainer().get(typeKey,
                            PersistentDataType.STRING))) {
                        event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), 2.0F, false,
                                false);
                    }
                }
            }

            // Stormcaller (Thrown Trident)
            if (event.getEntity() instanceof org.bukkit.entity.Trident trident) {
                ItemStack item = trident.getItem();
                if (item.hasItemMeta()) {
                    NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                    String runeTypeStr = item.getItemMeta().getPersistentDataContainer().get(typeKey,
                            PersistentDataType.STRING);
                    if ("STORMCALLER".equals(runeTypeStr)) {
                        if (Math.random() <= 0.50 && event.getHitEntity() != null) {
                            playCustomLightning(event.getHitEntity().getLocation());
                            if (event.getHitEntity() instanceof org.bukkit.entity.Damageable d) {
                                d.damage(5.0); // No player attribute
                            }
                        }
                    } else if ("ASCENDED_STORMCALLER".equals(runeTypeStr)) {
                        if (Math.random() <= 0.75 && event.getHitEntity() != null) {
                            playCustomLightning(event.getHitEntity().getLocation());
                            if (event.getHitEntity() instanceof org.bukkit.entity.Damageable d) {
                                d.damage(6.0);
                            }
                            int chained = 0;
                            for (org.bukkit.entity.Entity nearby : event.getHitEntity().getNearbyEntities(8, 8, 8)) {
                                if (nearby instanceof LivingEntity le && nearby != player && chained < 5) {
                                    playCustomLightningChain(event.getHitEntity().getLocation(), le.getLocation());
                                    le.damage(4.0);
                                    chained++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MINING / BLOCK BREAK EFFECTS
    // =========================================================================

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool != null && tool.hasItemMeta()) {
            NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
            if ("FURNACE"
                    .equals(tool.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING))) {
                Block block = event.getBlock();
                Material type = block.getType();
                if (type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE
                        || type == Material.RAW_IRON_BLOCK) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(),
                            new ItemStack(Material.IRON_INGOT, type == Material.RAW_IRON_BLOCK ? 9 : 1));
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5,
                            0.2, 0.2, 0.2, 0.05);
                } else if (type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE
                        || type == Material.RAW_GOLD_BLOCK) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(),
                            new ItemStack(Material.GOLD_INGOT, type == Material.RAW_GOLD_BLOCK ? 9 : 1));
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5,
                            0.2, 0.2, 0.2, 0.05);
                } else if (type == Material.COPPER_ORE || type == Material.DEEPSLATE_COPPER_ORE
                        || type == Material.RAW_COPPER_BLOCK) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.COPPER_INGOT,
                            type == Material.RAW_COPPER_BLOCK ? 9 : (int) (Math.random() * 4) + 2));
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 5,
                            0.2, 0.2, 0.2, 0.05);
                }
            } else if ("ASCENDED_FURNACE"
                    .equals(tool.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING))) {
                // Auto-smelt + DOUBLE output
                Block block = event.getBlock();
                Material type = block.getType();
                if (type == Material.IRON_ORE || type == Material.DEEPSLATE_IRON_ORE
                        || type == Material.RAW_IRON_BLOCK) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(),
                            new ItemStack(Material.IRON_INGOT, type == Material.RAW_IRON_BLOCK ? 18 : 2));
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5),
                            10,
                            0.3, 0.3, 0.3, 0.08);
                } else if (type == Material.GOLD_ORE || type == Material.DEEPSLATE_GOLD_ORE
                        || type == Material.RAW_GOLD_BLOCK) {
                    event.setDropItems(false);
                    block.getWorld().dropItemNaturally(block.getLocation(),
                            new ItemStack(Material.GOLD_INGOT, type == Material.RAW_GOLD_BLOCK ? 18 : 2));
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5),
                            10,
                            0.3, 0.3, 0.3, 0.08);
                } else if (type == Material.COPPER_ORE || type == Material.DEEPSLATE_COPPER_ORE
                        || type == Material.RAW_COPPER_BLOCK) {
                    event.setDropItems(false);
                    int baseAmount = type == Material.RAW_COPPER_BLOCK ? 9 : (int) (Math.random() * 4) + 2;
                    block.getWorld().dropItemNaturally(block.getLocation(),
                            new ItemStack(Material.COPPER_INGOT, baseAmount * 2));
                    block.getWorld().spawnParticle(org.bukkit.Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5),
                            10,
                            0.3, 0.3, 0.3, 0.08);
                }
            } else if ("EXCAVATOR"
                    .equals(tool.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING))) {
                Block center = event.getBlock();
                if (isExcavatable(center.getType())) {
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                Block b = center.getRelative(x, y, z);
                                if (isExcavatable(b.getType())) {
                                    b.breakNaturally(tool);
                                    player.getWorld().spawnParticle(org.bukkit.Particle.BLOCK,
                                            b.getLocation().add(0.5, 0.5, 0.5), 2, b.getBlockData());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isExcavatable(Material m) {
        String name = m.name();
        return name.contains("DIRT") || name.contains("SAND") || name.contains("GRAVEL") || name.contains("GRASS_BLOCK")
                || name.contains("CLAY") || name.contains("MUD") || name.contains("PODZOL")
                || name.contains("MYCELIUM");
    }

    // =========================================================================
    // ABILITY EFFECTS (Leap, Ascension, Grappler)
    // =========================================================================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool != null && tool.hasItemMeta()) {
                NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                String typeStr = tool.getItemMeta().getPersistentDataContainer().get(typeKey,
                        PersistentDataType.STRING);
                if ("LEAP".equals(typeStr)) {
                    if (!player.hasCooldown(tool.getType())) {
                        player.setCooldown(tool.getType(), 60);
                        org.bukkit.util.Vector direction = player.getLocation().getDirection().normalize();
                        direction.setY(0.6);
                        direction.multiply(2.5);
                        player.setVelocity(direction);
                        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION,
                                player.getLocation().add(0, 1, 0), 5);
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
                        long immunityExpiry = System.currentTimeMillis() + 5000;
                        player.getPersistentDataContainer().set(new NamespacedKey(plugin, "leap_immunity"),
                                PersistentDataType.LONG, immunityExpiry);
                    }
                }
            }
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (player.isGliding()) {
                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate != null && chestplate.getType() == Material.ELYTRA && chestplate.hasItemMeta()) {
                    NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                    if ("ASCENSION".equals(chestplate.getItemMeta().getPersistentDataContainer().get(typeKey,
                            PersistentDataType.STRING))) {

                        NamespacedKey cooldownKey = new NamespacedKey(plugin, "ascension_cooldown");
                        long current = System.currentTimeMillis();
                        long target = player.getPersistentDataContainer().getOrDefault(cooldownKey,
                                PersistentDataType.LONG, 0L);

                        if (current >= target) {
                            player.setVelocity(player.getLocation().getDirection().normalize().multiply(1.5)
                                    .add(new org.bukkit.util.Vector(0, 0.8, 0)));
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f,
                                    1.0f);
                            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.5,
                                    0.5, 0.5, 0.1);
                            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation(), 10, 0.2,
                                    0.2, 0.2, 0.05);

                            // 2.5 second cooldown (2500 ms)
                            player.getPersistentDataContainer().set(cooldownKey, PersistentDataType.LONG,
                                    current + 2500);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player && event.isGliding()) {
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && chestplate.getType() == Material.ELYTRA && chestplate.hasItemMeta()) {
                NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                if ("ASCENSION".equals(chestplate.getItemMeta().getPersistentDataContainer().get(typeKey,
                        PersistentDataType.STRING))) {
                    player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(0, 1.5, 0)));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                    player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5,
                            0.1);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && (event.getState() == PlayerFishEvent.State.IN_GROUND
                || event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY
                || event.getState() == PlayerFishEvent.State.REEL_IN)) {
            ItemStack rod = player.getInventory().getItemInMainHand();
            if (rod == null || rod.getType() != Material.FISHING_ROD) {
                rod = player.getInventory().getItemInOffHand();
            }

            if (rod != null && rod.getType() == Material.FISHING_ROD && rod.hasItemMeta()) {
                NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                if ("GRAPPLER".equals(
                        rod.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING))) {
                    org.bukkit.entity.FishHook hook = event.getHook();
                    org.bukkit.util.Vector direction = hook.getLocation().toVector()
                            .subtract(player.getLocation().toVector()).normalize();
                    double distance = hook.getLocation().distance(player.getLocation());

                    player.setVelocity(direction.multiply(Math.min(distance * 0.25, 3.0))
                            .add(new org.bukkit.util.Vector(0, 0.5, 0)));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f);
                    player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0),
                            3);
                }
            }
        }
    }

    // =========================================================================
    // SURVIVAL EFFECTS (Phoenix Core, Leap Immunity)
    // =========================================================================

    @EventHandler
    public void onLethalDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getFinalDamage() >= player.getHealth()) {
                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate != null && chestplate.hasItemMeta()) {
                    NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                    if ("PHOENIX_CORE".equals(chestplate.getItemMeta().getPersistentDataContainer().get(typeKey,
                            PersistentDataType.STRING))) {
                        event.setCancelled(true);
                        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 400, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 1));
                        player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation(), 100, 0.5, 1,
                                0.5, 0.1);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

                        ItemMeta meta = chestplate.getItemMeta();
                        meta.getPersistentDataContainer().remove(typeKey);
                        meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "has_rune"));

                        List<String> lore = meta.getLore();
                        if (lore != null) {
                            lore.add(ChatColor.DARK_RED + "Shattered Phoenix Core");
                            meta.setLore(lore);
                        }
                        chestplate.setItemMeta(meta);
                    } else if ("ASCENDED_PHOENIX_CORE"
                            .equals(chestplate.getItemMeta().getPersistentDataContainer().get(typeKey,
                                    PersistentDataType.STRING))) {
                        // Revive with Strength II + fire aura
                        event.setCancelled(true);
                        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1)); // Strength II
                        player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation(), 200, 1, 2,
                                1, 0.15);
                        player.getWorld().spawnParticle(org.bukkit.Particle.LAVA, player.getLocation(), 30, 1, 1,
                                1, 0);
                        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 0.8f);
                        // Fire aura: ignite nearby enemies
                        for (org.bukkit.entity.Entity e : player.getNearbyEntities(5, 3, 5)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.setFireTicks(100);
                            }
                        }

                        ItemMeta meta = chestplate.getItemMeta();
                        meta.getPersistentDataContainer().remove(typeKey);
                        meta.getPersistentDataContainer().remove(new NamespacedKey(plugin, "has_rune"));

                        List<String> lore = meta.getLore();
                        if (lore != null) {
                            lore.add(ChatColor.GOLD + "Ascended Phoenix Consumed");
                            meta.setLore(lore);
                        }
                        chestplate.setItemMeta(meta);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL) {
            NamespacedKey key = new NamespacedKey(plugin, "leap_immunity");
            if (player.getPersistentDataContainer().has(key, PersistentDataType.LONG)) {
                long expiry = player.getPersistentDataContainer().get(key, PersistentDataType.LONG);
                if (System.currentTimeMillis() < expiry) {
                    event.setCancelled(true);
                    player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.5, 0.1, 0.5,
                            0.1);
                }
                player.getPersistentDataContainer().remove(key);
            }
        }
    }

    // =========================================================================
    // MOVEMENT EFFECTS (Triple Jump)
    // =========================================================================

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR)
            return;

        if (((org.bukkit.entity.Entity) player).isOnGround()) {
            ItemStack boots = player.getInventory().getBoots();
            if (boots != null && boots.hasItemMeta()) {
                NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                if ("TRIPLE_JUMP".equals(
                        boots.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING))) {
                    player.setAllowFlight(true);
                    jumpCounts.put(player.getUniqueId(), 0);
                }
            }
        }
    }

    @EventHandler
    public void onJumpToggle(org.bukkit.event.player.PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR)
            return;

        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.hasItemMeta()) {
            NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
            if ("TRIPLE_JUMP"
                    .equals(boots.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING))) {
                event.setCancelled(true);

                int jumps = jumpCounts.getOrDefault(player.getUniqueId(), 0);
                if (jumps < 2) {
                    player.setVelocity(player.getLocation().getDirection().multiply(1.2).setY(1.0));
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 10, 0.2, 0.1, 0.2,
                            0.05);

                    jumps++;
                    jumpCounts.put(player.getUniqueId(), jumps);

                    if (jumps >= 2) {
                        player.setAllowFlight(false);
                    }
                } else {
                    player.setAllowFlight(false);
                }
            }
        }
    }

    private void playCustomLightning(org.bukkit.Location loc) {
        org.bukkit.World w = loc.getWorld();
        if (w == null)
            return;
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        for (double y = 0; y <= 15; y += 0.5) {
            org.bukkit.Location particleLoc = loc.clone().add(0, y, 0);
            w.spawnParticle(org.bukkit.Particle.FIREWORK, particleLoc, 2, 0.2, 0.2, 0.2, 0);
            w.spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, particleLoc, 2, 0.2, 0.2, 0.2, 0);
        }
    }

    private void playCustomLightningChain(org.bukkit.Location loc1, org.bukkit.Location loc2) {
        org.bukkit.World w = loc1.getWorld();
        if (w == null)
            return;

        org.bukkit.util.Vector vector = loc2.toVector().subtract(loc1.toVector());
        double distance = vector.length();
        if (distance > 0) {
            vector.normalize();
        }

        w.playSound(loc1, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 1.5f);

        for (double d = 0; d <= distance; d += 0.5) {
            org.bukkit.Location particleLoc = loc1.clone().add(vector.clone().multiply(d)).add(0, 1, 0);
            w.spawnParticle(org.bukkit.Particle.FIREWORK, particleLoc, 1, 0.1, 0.1, 0.1, 0);
            w.spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }
    }

}
