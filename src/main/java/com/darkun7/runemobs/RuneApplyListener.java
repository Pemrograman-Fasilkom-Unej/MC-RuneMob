package com.darkun7.runemobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles core rune operations: applying runes to gear and preventing rune
 * crafting abuse.
 * All rune EFFECT triggers (combat, abilities, movement) are in
 * RuneEffectListener.
 */
public class RuneApplyListener implements Listener {

    private final RuneMobs plugin;

    public RuneApplyListener(RuneMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRuneApply(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || current == null || current.getType() == Material.AIR)
            return;

        RuneManager.RuneType runeType = RuneManager.getRuneTypeFromItem(cursor);
        if (runeType == null)
            return;

        boolean applicable = false;
        String applyError = "";

        ItemMeta meta = current.getItemMeta();
        if (meta == null)
            return;

        NamespacedKey hasRuneKey = new NamespacedKey(plugin, "has_rune");

        if (meta.getPersistentDataContainer().has(hasRuneKey, PersistentDataType.BYTE)) {
            player.sendMessage(ChatColor.RED + "This item is already engraved!");
            return;
        }

        switch (runeType) {
            case VAMPIRISM:
            case VENOM:
                if (current.getType().name().endsWith("_SWORD") || current.getType().name().endsWith("_AXE")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Swords or Axes.";
                }
                break;
            case LIFE:
            case COLOSSUS:
                if (current.getType().name().endsWith("_CHESTPLATE")
                        || current.getType().name().endsWith("_LEGGINGS")) {
                    applicable = true;
                    EquipmentSlot slot = current.getType().name().endsWith("_CHESTPLATE") ? EquipmentSlot.CHEST
                            : EquipmentSlot.LEGS;
                    if (runeType == RuneManager.RuneType.LIFE) {
                        AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), "rune_life", 8.0,
                                AttributeModifier.Operation.ADD_NUMBER, slot);
                        meta.addAttributeModifier(Attribute.MAX_HEALTH, mod);
                    } else {
                        AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), "rune_colossus", 0.5,
                                AttributeModifier.Operation.ADD_SCALAR, slot);
                        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, mod);
                    }
                } else {
                    applyError = "This rune can only be applied to Chestplates or Leggings.";
                }
                break;
            case SWIFTNESS:
                if (current.getType().name().endsWith("_BOOTS")) {
                    applicable = true;
                    AttributeModifier mod = new AttributeModifier(UUID.randomUUID(), "rune_speed", 0.1,
                            AttributeModifier.Operation.ADD_SCALAR, EquipmentSlot.FEET);
                    meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, mod);
                } else {
                    applyError = "Swiftness Rune can only be applied to Boots.";
                }
                break;
            case RETRIBUTION:
                if (current.getType().name().endsWith("_HELMET") || current.getType() == Material.SHIELD) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Helmets or Shields.";
                }
                break;
            case STORMCALLER:
                if (current.getType() == Material.TRIDENT) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Tridents.";
                }
                break;
            case DETONATION:
                if (current.getType() == Material.BOW || current.getType() == Material.CROSSBOW) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Bows or Crossbows.";
                }
                break;
            case FURNACE:
                if (current.getType().name().endsWith("_PICKAXE")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Pickaxes.";
                }
                break;

            case ASCENSION:
                if (current.getType() == Material.ELYTRA) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Elytras.";
                }
                break;
            case EXCAVATOR:
                if (current.getType().name().endsWith("_SHOVEL")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Shovels.";
                }
                break;
            case GRAPPLER:
                if (current.getType() == Material.FISHING_ROD) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Fishing Rods.";
                }
                break;
            case SLOW_AURA:
                if (current.getType().name().endsWith("_HELMET")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Helmets.";
                }
                break;
            case LEAP:
                if (current.getType().name().equals("MACE")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Maces.";
                }
                break;
            case TRIPLE_JUMP:
                if (current.getType().name().endsWith("_BOOTS")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Boots.";
                }
                break;
            case LIGHTNING_SHIELD:
                if (current.getType() == Material.SHIELD) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Shields.";
                }
                break;
            case SOLAR_BLESSING:
                if (current.getType().name().endsWith("_CHESTPLATE")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Chestplates.";
                }
                break;
            case SCYTHE:
                if (current.getType().name().endsWith("_HOE") || current.getType() == Material.TRIDENT
                        || current.getType().name().contains("SPEAR")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Spears, Hoes or Tridents.";
                }
                break;
            case SPEAR_THRUST:
                if (current.getType() == Material.TRIDENT || current.getType().name().contains("SPEAR")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Spears or Tridents.";
                }
                break;
            case PHOENIX_CORE:
                if (current.getType().name().endsWith("_CHESTPLATE")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Chestplates.";
                }
                break;
            case BERSERK:
                if (current.getType().name().endsWith("_SWORD") || current.getType().name().endsWith("_AXE")) {
                    applicable = true;
                } else {
                    applyError = "This rune can only be applied to Swords or Axes.";
                }
                break;

            // ====== ASCENDED BOSS RUNES ======
            // Same equipment restrictions as base runes, enhanced effects at proc time
            case ASCENDED_VAMPIRISM:
            case ASCENDED_VENOM:
            case ASCENDED_BERSERK:
                if (current.getType().name().endsWith("_SWORD") || current.getType().name().endsWith("_AXE")) {
                    applicable = true;
                } else {
                    applyError = "This ascended rune can only be applied to Swords or Axes.";
                }
                break;
            case ASCENDED_RETRIBUTION:
                if (current.getType().name().endsWith("_HELMET") || current.getType() == Material.SHIELD) {
                    applicable = true;
                } else {
                    applyError = "This ascended rune can only be applied to Helmets or Shields.";
                }
                break;
            case ASCENDED_STORMCALLER:
                if (current.getType() == Material.TRIDENT) {
                    applicable = true;
                } else {
                    applyError = "This ascended rune can only be applied to Tridents.";
                }
                break;
            case ASCENDED_SOLAR_BLESSING:
            case ASCENDED_PHOENIX_CORE:
                if (current.getType().name().endsWith("_CHESTPLATE")) {
                    applicable = true;
                } else {
                    applyError = "This ascended rune can only be applied to Chestplates.";
                }
                break;
            case ASCENDED_FURNACE:
                if (current.getType().name().endsWith("_PICKAXE")) {
                    applicable = true;
                } else {
                    applyError = "This ascended rune can only be applied to Pickaxes.";
                }
                break;
        }

        if (!applicable) {
            player.sendMessage(ChatColor.RED + applyError);
            return;
        }

        event.setCancelled(true);
        cursor.setAmount(cursor.getAmount() - 1);
        event.getView().setCursor(cursor.getAmount() == 0 ? null : cursor);

        // Compute expiry
        int durationMins = plugin.getConfig().getInt("rune_duration_minutes", 60);
        long expiryTime = System.currentTimeMillis() + ((long) durationMins * 60 * 1000);

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "-------------------");
        lore.add(runeType.getName());
        lore.add(runeType.getLoreDesc());
        lore.add("");
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm");
        lore.add(ChatColor.GRAY + "Expires: " + ChatColor.WHITE + sdf.format(new Date(expiryTime)));
        lore.add(ChatColor.DARK_GRAY + "-------------------");
        meta.setLore(lore);

        meta.getPersistentDataContainer().set(hasRuneKey, PersistentDataType.BYTE, (byte) 1);
        NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, runeType.name());

        NamespacedKey expiryKey = new NamespacedKey(plugin, "rune_expiry");
        meta.getPersistentDataContainer().set(expiryKey, PersistentDataType.LONG, expiryTime);

        current.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Rune successfully engraved!");
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getMatrix() != null) {
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (item != null && item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
                    NamespacedKey typeKey = new NamespacedKey(plugin, "rune_type");
                    if (item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                        event.getInventory().setResult(new ItemStack(Material.AIR));
                        return;
                    }
                }
            }
        }
    }
}
