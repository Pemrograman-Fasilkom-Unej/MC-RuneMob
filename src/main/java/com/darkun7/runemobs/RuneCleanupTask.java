package com.darkun7.runemobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RuneCleanupTask implements Runnable, Listener {

    private final RuneMobs plugin;

    public RuneCleanupTask(RuneMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            cleanPlayer(player, now);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        cleanPlayer(event.getPlayer(), System.currentTimeMillis());
    }

    private void cleanPlayer(Player player, long now) {
        NamespacedKey expiryKey = new NamespacedKey(plugin, "rune_expiry");
        NamespacedKey hasRuneKey = new NamespacedKey(plugin, "has_rune");
        NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");

        PlayerInventory inv = player.getInventory();
        boolean cleansed = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !item.hasItemMeta())
                continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;

            if (meta.getPersistentDataContainer().has(expiryKey, PersistentDataType.LONG)) {
                long expiry = meta.getPersistentDataContainer().get(expiryKey, PersistentDataType.LONG);

                if (now >= expiry) {
                    // Rune has expired! Wipe it out.
                    meta.getPersistentDataContainer().remove(expiryKey);
                    meta.getPersistentDataContainer().remove(hasRuneKey);
                    meta.getPersistentDataContainer().remove(typeKey);

                    // Strip injected attributes
                    if (meta.hasAttributeModifiers()) {
                        // Max HP (Life)
                        if (meta.getAttributeModifiers(Attribute.MAX_HEALTH) != null) {
                            List<AttributeModifier> toRemove = new ArrayList<>();
                            for (AttributeModifier mod : meta.getAttributeModifiers(Attribute.MAX_HEALTH)) {
                                if (mod.getName().equals("rune_life"))
                                    toRemove.add(mod);
                            }
                            for (AttributeModifier mod : toRemove)
                                meta.removeAttributeModifier(Attribute.MAX_HEALTH, mod);
                        }

                        // Speed (Swiftness)
                        if (meta.getAttributeModifiers(Attribute.MOVEMENT_SPEED) != null) {
                            List<AttributeModifier> toRemove = new ArrayList<>();
                            for (AttributeModifier mod : meta
                                    .getAttributeModifiers(Attribute.MOVEMENT_SPEED)) {
                                if (mod.getName().equals("rune_speed"))
                                    toRemove.add(mod);
                            }
                            for (AttributeModifier mod : toRemove)
                                meta.removeAttributeModifier(Attribute.MOVEMENT_SPEED, mod);
                        }

                        // Knockback Res (Colossus)
                        if (meta.getAttributeModifiers(Attribute.KNOCKBACK_RESISTANCE) != null) {
                            List<AttributeModifier> toRemove = new ArrayList<>();
                            for (AttributeModifier mod : meta
                                    .getAttributeModifiers(Attribute.KNOCKBACK_RESISTANCE)) {
                                if (mod.getName().equals("rune_colossus"))
                                    toRemove.add(mod);
                            }
                            for (AttributeModifier mod : toRemove)
                                meta.removeAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, mod);
                        }
                    }

                    // Remove rune lore lines
                    if (meta.hasLore()) {
                        List<String> newLore = new ArrayList<>();
                        boolean skip = false;
                        for (String line : meta.getLore()) {
                            if (line.contains("---") && !skip) {
                                skip = true;
                                continue;
                            }
                            if (line.contains("---") && skip) {
                                skip = false;
                                continue;
                            }
                            if (!skip) {
                                newLore.add(line);
                            }
                        }
                        meta.setLore(newLore);
                    }

                    // Reapply
                    item.setItemMeta(meta);
                    cleansed = true;
                }
            }
        }

        if (cleansed) {
            player.sendMessage(
                    ChatColor.GRAY + "" + ChatColor.ITALIC + "A magical rune on your equipment has dissolved...");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.5f);
        }
    }
}
