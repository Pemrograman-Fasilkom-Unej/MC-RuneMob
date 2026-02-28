package com.darkun7.runemobs.raids.ai;

import com.darkun7.runemobs.RuneMobs;
import com.darkun7.runemobs.MobSpawnListener;
import com.darkun7.runemobs.raids.RuneRaid;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.attribute.Attribute;

public class ForgottenKingAI extends BukkitRunnable {
    private final RuneMobs plugin;
    private final RuneRaid raid;
    private final LivingEntity boss;

    private boolean phase2Triggered = false;
    private boolean phase3Triggered = false;

    public ForgottenKingAI(RuneMobs plugin, RuneRaid raid, LivingEntity boss) {
        this.plugin = plugin;
        this.raid = raid;
        this.boss = boss;

        // Phase 1: Slow attacks -> give SLOW and MINING_FATIGUE to visually and
        // physically slow down attacks
        boss.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
        boss.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, Integer.MAX_VALUE, 1, false, false));
    }

    @Override
    public void run() {
        if (!boss.isValid() || boss.isDead()) {
            cancel();
            return;
        }

        // Constant particle effect to identify as the Boss
        if (boss.getLocation().getWorld() != null) {
            boss.getLocation().getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, boss.getLocation().add(0, 1, 0), 2,
                    0.5, 0.5, 0.5, 0.02);
        }

        double maxHp = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
        double healthPct = boss.getHealth() / maxHp;

        // Phase 2 (50% HP): Summons Undead Knights
        // The king betrayed by his people summons knights to protect him
        if (!phase2Triggered && healthPct <= 0.5) {
            phase2Triggered = true;
            if (boss.getLocation().getWorld() != null) {
                boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
            }
            plugin.getServer()
                    .broadcastMessage("§7[§4The Forgotten King§7] §c\"Arise, my loyal knights! Protect the throne!\"");

            for (int i = 0; i < 4; i++) {
                org.bukkit.Location spawnLoc = boss.getLocation().clone().add(Math.random() * 4 - 2, 0,
                        Math.random() * 4 - 2);
                Skeleton knight = (Skeleton) boss.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
                knight.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Undead Knight"));
                knight.setCustomNameVisible(true);
                knight.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                knight.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                knight.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                knight.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
                knight.getEquipment().setHelmetDropChance(0.0f);
                knight.getEquipment().setChestplateDropChance(0.0f);
                knight.getEquipment().setItemInMainHandDropChance(0.0f);
                knight.getEquipment().setItemInOffHandDropChance(0.0f);
                MobSpawnListener.applyRarity(knight, "epic", plugin);
                knight.setTarget(raid.getTarget());
                raid.addSpawnedEntity(knight);
            }
        }

        // Phase 3 (20% HP): Enraged, faster attacks
        // King is furious and regains his speed to fight viciously
        if (!phase3Triggered && healthPct <= 0.2) {
            phase3Triggered = true;
            if (boss.getLocation().getWorld() != null) {
                boss.getLocation().getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f,
                        1.0f);
                boss.getLocation().getWorld().spawnParticle(Particle.FLAME, boss.getLocation().add(0, 1, 0), 30, 0.5,
                        1.0, 0.5, 0.1);
            }
            plugin.getServer()
                    .broadcastMessage("§7[§4The Forgotten King§7] §4\"I WILL NOT FALL TO THE LIKES OF YOU!\"");

            boss.removePotionEffect(PotionEffectType.SLOWNESS);
            boss.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            boss.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 2, false, false));
        }
    }
}
