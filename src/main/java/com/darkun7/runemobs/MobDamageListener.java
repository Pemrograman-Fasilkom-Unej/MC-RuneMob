package com.darkun7.runemobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class MobDamageListener implements Listener {

    private final RuneMobs plugin;
    private final Random random = new Random();

    public MobDamageListener(RuneMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity damager) {
            String rarity = getRarity(damager);
            if (rarity != null) {
                double dmgMult = plugin.getConfig().getDouble("rarities." + rarity + ".damage_multiplier", 1.0);
                event.setDamage(event.getDamage() * dmgMult);

                if (rarity.equals("epic") || rarity.equals("legendary")) {
                    if (damager instanceof org.bukkit.entity.Spider
                            || damager instanceof org.bukkit.entity.CaveSpider) {
                        if (event.getEntity() instanceof org.bukkit.entity.Player target) {
                            if (random.nextDouble() < 0.25) {
                                org.bukkit.block.Block block = target.getLocation().getBlock();
                                if (block.getType() == org.bukkit.Material.AIR) {
                                    block.setType(org.bukkit.Material.COBWEB);
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        if (block.getType() == org.bukkit.Material.COBWEB) {
                                            block.setType(org.bukkit.Material.AIR);
                                        }
                                    }, 60L); // Clear after 3 seconds
                                }
                            }
                        }
                    } else if (damager instanceof org.bukkit.entity.Skeleton
                            || damager instanceof org.bukkit.entity.Stray) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.SLOWNESS, 40, 1));
                            target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation(), 10,
                                    0.5, 0.5, 0.5, 0.05);
                        }
                    } else if (damager instanceof org.bukkit.entity.Vindicator) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target
                                && random.nextDouble() < 0.20) {
                            event.setDamage(event.getDamage() * 1.5);
                            target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 0.8, 0)));
                            target.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, target.getLocation(), 2);
                        }
                    } else if (damager instanceof org.bukkit.entity.Pillager) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target
                                && random.nextDouble() < 0.20) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.WEAKNESS, 60, 1));
                            target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER,
                                    1.0f, 0.5f);
                        }
                    } else if (damager instanceof org.bukkit.entity.Ravager) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            target.setVelocity(target.getLocation().toVector()
                                    .subtract(damager.getLocation().toVector()).normalize().multiply(1.5).setY(0.5));
                        }
                    } else if (damager instanceof org.bukkit.entity.Vex) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.WITHER, 60, 0));
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.NAUSEA, 100, 0));
                        }
                    } else if (damager instanceof org.bukkit.entity.WitherSkeleton) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.WITHER, 160, 1));
                            if (random.nextDouble() < 0.15) {
                                target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                        org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
                            }
                        }
                    } else if (damager instanceof org.bukkit.entity.Hoglin) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            target.setVelocity(target.getVelocity().add(new org.bukkit.util.Vector(0, 1.2, 0)));
                        }
                    } else if (damager instanceof org.bukkit.entity.Shulker) {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target
                                && random.nextDouble() < 0.25) {
                            target.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.LEVITATION, 60, 2));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMobDamageEvent(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob))
            return;

        String rarity = getRarity(mob);
        if (rarity != null && (rarity.equals("epic") || rarity.equals("legendary"))) {
            if (mob instanceof Creeper) {
                // If the damage is going to kill the creeper
                if (mob.getHealth() - event.getFinalDamage() <= 0) {
                    NamespacedKey revivedKey = new NamespacedKey(plugin, "mob_revived");
                    PersistentDataContainer pdc = mob.getPersistentDataContainer();

                    if (!pdc.has(revivedKey, PersistentDataType.BYTE)) {
                        event.setCancelled(true);

                        org.bukkit.attribute.AttributeInstance maxHpAttr = mob
                                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (maxHpAttr != null) {
                            mob.setHealth(maxHpAttr.getValue());
                        } else {
                            mob.setHealth(20.0);
                        }

                        pdc.set(revivedKey, PersistentDataType.BYTE, (byte) 1);
                        mob.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, mob.getLocation(), 50, 0.5,
                                0.5, 0.5, 0.1);
                        mob.getWorld().playSound(mob.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

                        // Optionally change name format to show revived state
                        if (mob.getCustomName() != null) {
                            mob.setCustomName(mob.getCustomName() + org.bukkit.ChatColor.DARK_RED + " (Revived)");
                        }
                    }
                }
            } else if (mob instanceof org.bukkit.entity.Zombie) {
                if (random.nextDouble() < 0.15 && event.getFinalDamage() < mob.getHealth()) {
                    org.bukkit.entity.Zombie minion = (org.bukkit.entity.Zombie) mob.getWorld()
                            .spawnEntity(mob.getLocation(), org.bukkit.entity.EntityType.ZOMBIE);
                    minion.setBaby();
                    minion.setCustomName(org.bukkit.ChatColor.DARK_GREEN + "Zombie Minion");
                    minion.setCustomNameVisible(true);

                    org.bukkit.attribute.AttributeInstance hA = minion
                            .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (hA != null)
                        hA.setBaseValue(10.0);
                    minion.setHealth(10.0);

                    mob.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, mob.getLocation(), 15, 0.5, 0.5, 0.5, 0.05);
                    mob.getWorld().playSound(mob.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f,
                            1.5f);
                }
            } else if (mob instanceof org.bukkit.entity.Enderman) {
                if (random.nextDouble() < 0.20 && event.getDamage() > 0) {
                    for (org.bukkit.entity.Entity e : mob.getNearbyEntities(6, 6, 6)) {
                        if (e instanceof org.bukkit.entity.Player p) {
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.BLINDNESS, 60, 0));
                            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_STARE, 1.0f, 1.0f);
                            p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, p.getLocation().add(0, 1, 0), 20,
                                    0.5, 0.5, 0.5, 1);
                        }
                    }
                }
            } else if (mob instanceof org.bukkit.entity.Evoker) {
                if (random.nextDouble() < 0.15 && event.getFinalDamage() < mob.getHealth()) {
                    org.bukkit.Location loc = mob.getLocation().add(random.nextInt(6) - 3, 0, random.nextInt(6) - 3);
                    int highestY = mob.getWorld().getHighestBlockYAt(loc);
                    if (highestY > 0)
                        loc.setY(highestY + 1);
                    mob.teleport(loc);
                    mob.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, mob.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
                    mob.getWorld().spawnEntity(mob.getLocation(), org.bukkit.entity.EntityType.VEX);
                    mob.getWorld().spawnEntity(mob.getLocation(), org.bukkit.entity.EntityType.VEX);
                }
            } else if (mob instanceof org.bukkit.entity.Piglin || mob instanceof org.bukkit.entity.PiglinBrute
                    || mob instanceof org.bukkit.entity.PigZombie) {
                if (random.nextDouble() < 0.20 && event.getFinalDamage() < mob.getHealth()) {
                    mob.addPotionEffect(
                            new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 100, 1));
                    mob.addPotionEffect(
                            new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.STRENGTH, 100, 0));
                    mob.getWorld().spawnParticle(org.bukkit.Particle.ANGRY_VILLAGER, mob.getLocation().add(0, 1, 0), 5,
                            0.5, 0.5, 0.5, 0);
                }
            } else if (mob instanceof org.bukkit.entity.Ghast) {
                if (random.nextDouble() < 0.20 && event.getFinalDamage() < mob.getHealth()) {
                    mob.getWorld().spawnEntity(mob.getLocation().add(0, -2, 0), org.bukkit.entity.EntityType.BLAZE);
                    mob.getWorld().playSound(mob.getLocation(), org.bukkit.Sound.ENTITY_GHAST_SCREAM, 1.0f, 1.0f);
                }
            } else if (mob instanceof org.bukkit.entity.MagmaCube) {
                if (random.nextDouble() < 0.25 && event.getFinalDamage() < mob.getHealth()) {
                    for (org.bukkit.entity.Entity e : mob.getNearbyEntities(4, 4, 4)) {
                        if (e instanceof org.bukkit.entity.Player) {
                            e.setFireTicks(100);
                        }
                    }
                    mob.getWorld().spawnParticle(org.bukkit.Particle.LAVA, mob.getLocation(), 15, 1, 1, 1, 0.1);
                }
            }
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        String rarity = getRarity(mob);
        if (rarity != null) {

            // Emerald drops
            int minEmerald = plugin.getConfig().getInt("rarities." + rarity + ".emerald_drop.min", 1);
            int maxEmerald = plugin.getConfig().getInt("rarities." + rarity + ".emerald_drop.max", 1);
            int dropAmount = random.nextInt((maxEmerald - minEmerald) + 1) + minEmerald;

            if (dropAmount > 0) {
                event.getDrops().add(new ItemStack(Material.EMERALD, dropAmount));
            }

            // Rune drops map specifically to legendary per config
            if (rarity.equals("legendary")) {
                double runeChance = plugin.getConfig().getDouble("rarities.legendary.rune_drop_chance", 1.0);
                if (random.nextDouble() <= runeChance) {
                    event.getDrops().add(RuneManager.createRandomRune());
                }
            }
        }
    }

    private String getRarity(LivingEntity mob) {
        NamespacedKey key = new NamespacedKey(plugin, "mob_rarity");
        PersistentDataContainer pdc = mob.getPersistentDataContainer();
        if (pdc.has(key, PersistentDataType.STRING)) {
            return pdc.get(key, PersistentDataType.STRING);
        }
        return null;
    }
}
