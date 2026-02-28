package com.darkun7.runemobs.raids;

import com.darkun7.runemobs.RuneMobs;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class RaidScrollListener implements Listener {
    private final RuneMobs plugin;

    public RaidScrollListener(RuneMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onScrollUse(PlayerInteractEvent event) {
        if (event.getAction().isRightClick()) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.PAPER && item.hasItemMeta()) {
                NamespacedKey key = new NamespacedKey(plugin, "chained_raid_trigger");
                if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    String nextRaidId = item.getItemMeta().getPersistentDataContainer().get(key,
                            PersistentDataType.STRING);

                    Player player = event.getPlayer();

                    // Environment Check for Leviathan - requires Ocean biome
                    if ("leviathan".equalsIgnoreCase(nextRaidId)) {
                        org.bukkit.block.Biome biome = player.getLocation().getBlock().getBiome();
                        if (!biome.toString().contains("OCEAN")) {
                            player.sendMessage(org.bukkit.ChatColor.RED
                                    + "The scroll remains dormant... It requires the deep waters of an Ocean to awaken the Leviathan!");
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f,
                                    1.0f);
                            return;
                        }
                    }

                    // Environment Check for Nether Warlord - requires Nether
                    if ("nether_warlord".equalsIgnoreCase(nextRaidId)) {
                        if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NETHER) {
                            player.sendMessage(org.bukkit.ChatColor.RED
                                    + "The scroll smolders but refuses to ignite... You must be in the Nether to challenge the Warlord!");
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f,
                                    1.0f);
                            return;
                        }
                    }

                    // Environment Check for Miner Titan - requires being underground (below y=56 in
                    // a cave)
                    if ("miner_titan".equalsIgnoreCase(nextRaidId)) {
                        if (player.getLocation().getY() > 56
                                || player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
                            player.sendMessage(org.bukkit.ChatColor.RED
                                    + "The scroll vibrates but remains sealed... You must go deep underground in the Overworld to awaken the Titan!");
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f,
                                    1.0f);
                            return;
                        }
                    }

                    // Environment Check for Moonlight Stalker - requires nighttime in Overworld
                    if ("moonlight_stalker".equalsIgnoreCase(nextRaidId)) {
                        long time = player.getWorld().getTime();
                        boolean isNight = time >= 13000 && time <= 23000;
                        if (!isNight || player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
                            player.sendMessage(org.bukkit.ChatColor.RED
                                    + "The scroll whispers but stays silent... The Stalker only hunts under the moonlight in the Overworld!");
                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f,
                                    1.0f);
                            return;
                        }
                    }

                    // Boss raid overlap check - don't consume scroll if blocked
                    // (eikthyr/army raids are exempt from this check)
                    if (!"eikthyr".equalsIgnoreCase(nextRaidId) && plugin.hasBossRaidActive()) {
                        RuneRaid existingRaid = plugin.getActiveBossRaid();
                        String existingName = existingRaid != null ? RuneMobs.formatRaidName(existingRaid.getRaidId())
                                : "unknown";
                        player.sendMessage(org.bukkit.ChatColor.RED
                                + "A boss raid (" + existingName + ") is already in progress! Wait for it to end.");
                        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f,
                                1.0f);
                        return;
                    }

                    // Consume scroll
                    item.setAmount(item.getAmount() - 1);

                    player.sendMessage(org.bukkit.ChatColor.GOLD + "The scroll crumbles into dust... "
                            + org.bukkit.ChatColor.RED + "A new threat approaches!");
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

                    // Trigger the chained raid
                    RuneRaid raid = new RuneRaid(plugin, nextRaidId, player);
                    raid.start();
                }
            }
        }
    }
}
