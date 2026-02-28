package com.darkun7.runemobs.raids;

import com.darkun7.runemobs.RuneMobs;
import com.darkun7.runemobs.RuneManager;
import com.darkun7.runemobs.MobSpawnListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class RuneRaid {

    private final RuneMobs plugin;
    private final String raidId;
    private final Player target;
    private final Location raidLocation;
    private final Random random = new Random();

    private final List<LivingEntity> spawnedEntities = new ArrayList<>();
    private BossBar bossBar;

    // Config variables
    private String title;
    private BarColor barColor;
    private int waveInterval; // seconds
    private int durationMinutes;
    private ConfigurationSection wavesConfig;
    private int totalWaves;
    private boolean scaleWithPlayers;

    // Status tracking
    private int currentWaveIndex = 1;
    private BukkitRunnable cleanupTask;
    private LivingEntity mainBossEntity = null;
    private boolean active = false;

    public RuneRaid(RuneMobs plugin, String raidId, Player target) {
        this.plugin = plugin;
        this.raidId = raidId;
        this.target = target;
        this.raidLocation = target.getLocation();
        loadConfig();
    }

    public Player getTarget() {
        return target;
    }

    public void addSpawnedEntity(LivingEntity le) {
        if (!spawnedEntities.contains(le)) {
            spawnedEntities.add(le);
        }
    }

    public String getRaidId() {
        return raidId;
    }

    /**
     * Returns true if this raid is currently active (running).
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Check if this raid is a boss raid (not an army-only raid like eikthyr).
     * Boss raids have at least one wave entry with is_boss: true.
     */
    public boolean isBossRaid() {
        if (wavesConfig == null)
            return false;
        for (String waveKey : wavesConfig.getKeys(false)) {
            List<Map<?, ?>> mobList = wavesConfig.getMapList(waveKey);
            for (Map<?, ?> mobData : mobList) {
                if (mobData.containsKey("is_boss") && (boolean) mobData.get("is_boss")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateBossBarTitle() {
        if (bossBar != null) {
            int displayIdx = Math.min(currentWaveIndex, totalWaves);
            if ("eikthyr".equalsIgnoreCase(raidId)) {
                bossBar.setTitle(title + " - Wave " + displayIdx + "/" + totalWaves);
            } else {
                bossBar.setTitle(title + " - Level " + displayIdx);
            }
        }
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("raids." + raidId);
        if (section == null) {
            plugin.getLogger().log(Level.WARNING, "Raid config not found for: " + raidId);
            return;
        }

        this.title = section.getString("title", "Raid");
        this.title = org.bukkit.ChatColor.translateAlternateColorCodes('&', this.title);

        try {
            this.barColor = BarColor.valueOf(section.getString("bar_color", "RED").toUpperCase());
        } catch (IllegalArgumentException e) {
            this.barColor = BarColor.RED;
        }

        this.waveInterval = section.getInt("wave_interval", 45);
        this.durationMinutes = section.getInt("duration_minutes", 5);
        this.scaleWithPlayers = section.getBoolean("scale_with_players", false);
        this.wavesConfig = section.getConfigurationSection("waves");

        if (this.wavesConfig != null) {
            this.totalWaves = this.wavesConfig.getKeys(false).size();
        } else {
            this.totalWaves = 0;
        }
    }

    public void start() {
        if (totalWaves == 0) {
            plugin.getLogger().log(Level.WARNING, "Cannot start raid " + raidId + ": No waves defined.");
            return;
        }

        // Overlap prevention: only 1 boss raid at a time (eikthyr/army raids exempt)
        if (isBossRaid() && plugin.hasBossRaidActive()) {
            RuneRaid existing = plugin.getActiveBossRaid();
            String existingName = existing != null ? RuneMobs.formatRaidName(existing.getRaidId()) : "unknown";
            plugin.getServer().broadcastMessage(
                    "§7[§cRaid§7] §fA boss raid (§c" + existingName
                            + "§f) is already in progress! Wait for it to finish.");
            // Refund the scroll: caller should handle this if needed
            return;
        }

        active = true;

        // Register as active boss raid if applicable
        if (isBossRaid()) {
            plugin.setActiveBossRaid(this);
        }

        plugin.getServer().broadcastMessage("§cThe " + title + " §cis attacking!");

        // Initialize BossBar
        // Divide the bar style depending on wave count
        BarStyle style = BarStyle.SOLID;
        if (totalWaves == 6)
            style = BarStyle.SEGMENTED_6;
        else if (totalWaves == 10)
            style = BarStyle.SEGMENTED_10;
        else if (totalWaves == 12)
            style = BarStyle.SEGMENTED_12;
        else if (totalWaves == 20)
            style = BarStyle.SEGMENTED_20;

        bossBar = Bukkit.createBossBar(title, barColor, style);
        updateBossBarTitle();
        bossBar.setProgress(0.0);
        bossBar.setVisible(true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }

        // Schedule Wave Spawning
        new BukkitRunnable() {
            @Override
            public void run() {
                if (currentWaveIndex > totalWaves) {
                    cancel();
                    return;
                }

                spawnWave(currentWaveIndex);
                bossBar.setProgress((double) currentWaveIndex / totalWaves);
                updateBossBarTitle();
                currentWaveIndex++;
            }
        }.runTaskTimer(plugin, 0L, 20L * waveInterval);

        // Schedule Cleanup
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                int removed = 0;
                for (LivingEntity entity : spawnedEntities) {
                    if (!entity.isDead()) {
                        entity.remove();
                        removed++;
                    }
                }

                if (bossBar != null) {
                    bossBar.removeAll();
                    bossBar.setVisible(false);
                }
                RuneRaid.this.endRaid();
                plugin.getServer().broadcastMessage("§7[§cRaid§7] §fThe " + title + " §fhas dispersed.");
            }
        };
        cleanupTask.runTaskLater(plugin, 20L * 60 * durationMinutes);

        // Schedule Tracking
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossBar == null || !bossBar.isVisible()) {
                    cancel();
                    return;
                }

                boolean anyAlive = false;
                for (LivingEntity le : spawnedEntities) {
                    if (le.isValid() && !le.isDead()) {
                        anyAlive = true;

                        // Tether logic: If the mob goes too far from the initial raid location, pull it
                        // back
                        if (le.getLocation().getWorld().equals(raidLocation.getWorld())) {
                            if (le.getLocation().distance(raidLocation) > 40) {
                                le.teleport(raidLocation.clone().add(0, 1, 0));
                                if (le instanceof org.bukkit.entity.Player) {
                                    // Should not happen, just a safety check
                                } else {
                                    le.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, le.getLocation(), 20, 0.5,
                                            1, 0.5, 0.1);
                                    le.getWorld().playSound(le.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT,
                                            1.0f, 1.0f);
                                }
                            }
                        }

                        if (le instanceof org.bukkit.entity.Mob mob) {
                            mob.setTarget(target); // Force tracking the target
                        }
                        if (le instanceof org.bukkit.entity.Wolf wolf) {
                            wolf.setAngry(true);
                        }
                    }
                }

                // If this is a Boss Raid, victory relies purely on the boss's defeat
                if (mainBossEntity != null) {
                    if (mainBossEntity.isDead()) {
                        bossBar.removeAll();
                        bossBar.setVisible(false);
                        plugin.getServer().broadcastMessage("§7[§aVictory§7] §fThe " + title + " §fhas been defeated!");
                        if (cleanupTask != null)
                            cleanupTask.cancel();
                        RuneRaid.this.endRaid();

                        // --- Distribute Boss Rewards to Participants ---
                        RuneRaid.this.distributeBossRewards();

                        // Boss Chaining Drop System
                        String nextRaid = null;
                        if (raidId.equalsIgnoreCase("eikthyr")) {
                            nextRaid = "forgotten_king";
                        } else if (raidId.equalsIgnoreCase("forgotten_king")) {
                            nextRaid = "corrupted_guardian";
                        } else if (raidId.equalsIgnoreCase("corrupted_guardian")) {
                            nextRaid = "leviathan";
                        } else if (raidId.equalsIgnoreCase("leviathan")) {
                            nextRaid = "plague_doctor";
                        } else if (raidId.equalsIgnoreCase("plague_doctor")) {
                            nextRaid = "nether_warlord";
                        } else if (raidId.equalsIgnoreCase("nether_warlord")) {
                            nextRaid = "miner_titan";
                        } else if (raidId.equalsIgnoreCase("miner_titan")) {
                            nextRaid = "moonlight_stalker";
                        } else if (raidId.equalsIgnoreCase("moonlight_stalker")) {
                            nextRaid = "soul_collector";
                        }

                        if (nextRaid != null) {
                            org.bukkit.inventory.ItemStack bossScroll = new org.bukkit.inventory.ItemStack(
                                    org.bukkit.Material.PAPER);
                            org.bukkit.inventory.meta.ItemMeta meta = bossScroll.getItemMeta();
                            String formattedRaid = RuneMobs.formatRaidName(nextRaid);
                            meta.setDisplayName(org.bukkit.ChatColor.GOLD + "Mysterious Boss Summoning Scroll");
                            java.util.List<String> lore = new java.util.ArrayList<>();
                            lore.add(org.bukkit.ChatColor.GRAY + "A strange parchment dropped by the raid leader...");
                            lore.add(org.bukkit.ChatColor.GRAY + "Use this scroll to awaken the next raid!");
                            lore.add("");
                            lore.add(org.bukkit.ChatColor.DARK_RED + "Summons: " + org.bukkit.ChatColor.WHITE
                                    + formattedRaid);
                            meta.setLore(lore);

                            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "chained_raid_trigger");
                            pdc.set(key, org.bukkit.persistence.PersistentDataType.STRING, nextRaid);
                            bossScroll.setItemMeta(meta);

                            mainBossEntity.getWorld().dropItemNaturally(mainBossEntity.getLocation(), bossScroll);
                        }

                        // Execute all remaining minions when the boss dies
                        for (LivingEntity le : spawnedEntities) {
                            if (!le.isDead() && le != mainBossEntity) {
                                le.remove();
                            }
                        }
                        cancel();
                        return;
                    }
                } else {
                    // Standard Army condition: all waves spawned and all spawned entities are dead
                    if (currentWaveIndex > totalWaves && !anyAlive) {
                        bossBar.removeAll();
                        bossBar.setVisible(false);
                        plugin.getServer().broadcastMessage("§7[§aVictory§7] §fThe " + title + " §fhas been defeated!");
                        if (cleanupTask != null)
                            cleanupTask.cancel();
                        RuneRaid.this.endRaid();
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 5);
    }

    /**
     * Mark this raid as ended and clear the active boss raid tracker.
     */
    private void endRaid() {
        active = false;
        if (isBossRaid() && plugin.getActiveBossRaid() == this) {
            plugin.clearActiveBossRaid();
        }
    }

    /**
     * Forcefully ends the raid and cleans up.
     */
    public void forceEndRaid() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        for (LivingEntity le : spawnedEntities) {
            if (le.isValid() && !le.isDead()) {
                le.remove();
            }
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        endRaid();
    }

    /**
     * Distribute boss-specific rewards (rune + emerald blocks) to all
     * participants near the boss death location.
     */
    private void distributeBossRewards() {
        // Read config
        String runeTypeStr = plugin.getConfig().getString("boss_rewards.rune_mapping." + raidId);
        if (runeTypeStr == null) {
            // No reward mapping for this raid (e.g., eikthyr) - skip
            return;
        }

        int rewardRadius = plugin.getConfig().getInt("boss_rewards.reward_radius", 50);
        int emeraldBlocks = plugin.getConfig().getInt("boss_rewards.emerald_blocks", 3);

        // Parse the rune type
        RuneManager.RuneType runeType;
        try {
            runeType = RuneManager.RuneType.valueOf(runeTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid rune type in boss_rewards for " + raidId + ": " + runeTypeStr);
            return;
        }

        // Get boss death location
        Location bossLoc = mainBossEntity != null ? mainBossEntity.getLocation() : raidLocation;

        // Find all nearby players (participants)
        int rewardedCount = 0;
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR)
                continue;
            if (!player.getWorld().equals(bossLoc.getWorld()))
                continue;
            if (player.getLocation().distance(bossLoc) > rewardRadius)
                continue;

            // Give the rune
            org.bukkit.inventory.ItemStack rune = RuneManager.createRune(runeType);
            player.getInventory().addItem(rune);

            // Give emerald blocks
            if (emeraldBlocks > 0) {
                org.bukkit.inventory.ItemStack emeralds = new org.bukkit.inventory.ItemStack(
                        org.bukkit.Material.EMERALD_BLOCK, emeraldBlocks);
                player.getInventory().addItem(emeralds);
            }

            // Visual + sound reward feedback
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING,
                    player.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.5);
            player.sendMessage(org.bukkit.ChatColor.GOLD + "§l✦ BOSS REWARD ✦ " + org.bukkit.ChatColor.WHITE
                    + "You received " + runeType.getName() + org.bukkit.ChatColor.WHITE
                    + " + " + org.bukkit.ChatColor.GREEN + emeraldBlocks + " Emerald Block(s)!");

            rewardedCount++;
        }

        if (rewardedCount > 0) {
            plugin.getServer().broadcastMessage(org.bukkit.ChatColor.GOLD + "§l✦ " + org.bukkit.ChatColor.WHITE
                    + rewardedCount + " participant(s) received boss rewards!");
        }
    }

    private void spawnWave(int waveIndex) {
        String waveKey = String.valueOf(waveIndex);
        if (!wavesConfig.contains(waveKey))
            return;

        List<Map<?, ?>> mobList = wavesConfig.getMapList(waveKey);
        for (Map<?, ?> mobData : mobList) {
            String typeStr = (String) mobData.get("type");
            String rarity = (String) mobData.get("rarity");
            int amount = mobData.containsKey("amount") ? (int) mobData.get("amount") : 1;

            if (this.scaleWithPlayers) {
                int onlinePlayers = org.bukkit.Bukkit.getOnlinePlayers().size();
                if (onlinePlayers > 0) {
                    amount *= onlinePlayers;
                }
            }

            try {
                EntityType type = EntityType.valueOf(typeStr.toUpperCase());

                for (int i = 0; i < amount; i++) {
                    Location spawnLoc = getRandomSpawnLocation();
                    org.bukkit.entity.Entity entity = raidLocation.getWorld().spawnEntity(spawnLoc, type);

                    if (entity instanceof LivingEntity le) {
                        MobSpawnListener.applyRarity(le, rarity, plugin);
                        // Make raid mobs glow and unable to steal items
                        le.setGlowing(true);
                        le.setCanPickupItems(false);

                        // Make raid mobs aggressive towards target player immediately if possible
                        if (le instanceof org.bukkit.entity.Mob mob) {
                            mob.setTarget(target);
                        }
                        if (le instanceof org.bukkit.entity.Wolf wolf) {
                            wolf.setAngry(true);
                        }

                        if (le instanceof org.bukkit.entity.PiglinAbstract piglin) {
                            piglin.setImmuneToZombification(true); // Don't turn Piglins into Zombified Piglins in
                                                                   // Overworld
                        }

                        if (mobData.containsKey("name")) {
                            String customName = (String) mobData.get("name");
                            le.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', customName));
                            le.setCustomNameVisible(true);
                        }

                        if (mobData.containsKey("baby") && (boolean) mobData.get("baby")) {
                            if (le instanceof org.bukkit.entity.Ageable ageable) {
                                ageable.setBaby();
                            } else if (le instanceof org.bukkit.entity.Zombie zombie) {
                                zombie.setBaby();
                            }
                        }

                        if (mobData.containsKey("is_boss") && (boolean) mobData.get("is_boss")) {
                            mainBossEntity = le;
                        }

                        // Execute custom AI hooks if listed
                        if (mobData.containsKey("ai")) {
                            String aiType = (String) mobData.get("ai");
                            if ("forgotten_king".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.ForgottenKingAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 10L);
                            } else if ("corrupted_guardian".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.GuardianBossAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            } else if ("leviathan".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.LeviathanBossAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            } else if ("plague_doctor".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.PlagueDoctorAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            } else if ("nether_warlord".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.NetherWarlordAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            } else if ("miner_titan".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.MinerTitanAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            } else if ("moonlight_stalker".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.MoonlightStalkerAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            } else if ("soul_collector".equalsIgnoreCase(aiType)) {
                                new com.darkun7.runemobs.raids.ai.SoulCollectorAI(plugin, RuneRaid.this, le)
                                        .runTaskTimer(plugin, 0L, 1L);
                            }
                        }

                        applyEquipment(le, mobData);

                        spawnedEntities.add(le);

                        // Check for mount
                        if (mobData.containsKey("mount")) {
                            String mountTypeStr = (String) mobData.get("mount");
                            try {
                                EntityType mountType = EntityType.valueOf(mountTypeStr.toUpperCase());
                                org.bukkit.entity.Entity mountEnt = raidLocation.getWorld().spawnEntity(spawnLoc,
                                        mountType);
                                mountEnt.addPassenger(le);

                                if (mountEnt instanceof LivingEntity mle) {
                                    MobSpawnListener.applyRarity(mle, rarity, plugin);
                                    mle.setGlowing(true);
                                    mle.setCanPickupItems(false);
                                    if (mle instanceof org.bukkit.entity.Mob mmob) {
                                        mmob.setTarget(target);
                                    }
                                    if (mle instanceof org.bukkit.entity.Wolf mwolf) {
                                        mwolf.setAngry(true);
                                    }
                                    spawnedEntities.add(mle);
                                }
                            } catch (Exception mountEx) {
                                plugin.getLogger().log(Level.WARNING, "Failed to spawn mount for raid " + raidId,
                                        mountEx);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to spawn mob in raid " + raidId + " wave " + waveIndex,
                        e);
            }
        }
    }

    private Location getRandomSpawnLocation() {
        World world = raidLocation.getWorld();
        boolean isNether = world.getEnvironment() == org.bukkit.World.Environment.NETHER;

        for (int attempts = 0; attempts < 20; attempts++) {
            double radius = 10 + random.nextDouble() * 15; // 10–25 blocks away
            double angle = random.nextDouble() * 2 * Math.PI;
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;

            Location base = raidLocation.clone().add(xOffset, 0, zOffset);

            // Search around the center's Y level, favoring similar elevation
            int startY = Math.min(world.getMaxHeight() - 1, raidLocation.getBlockY() + 15);
            int endY = Math.max(world.getMinHeight() + 1, raidLocation.getBlockY() - 15);

            if (isNether) {
                // In nether, roof is bedrock, don't go above 120
                startY = Math.min(120, startY);
                endY = Math.max(5, endY);
            }

            for (int y = startY; y >= endY; y--) {
                Block block = world.getBlockAt(base.getBlockX(), y, base.getBlockZ());
                Block below = block.getRelative(0, -1, 0);
                Block above = block.getRelative(0, 1, 0);

                if (below.getType().isSolid() && below.getType() != Material.LAVA && below.getType() != Material.WATER
                        && !below.getType().name().contains("LEAVES")
                        && block.getType() == Material.AIR && above.getType() == Material.AIR) {
                    Location loc = new Location(world, base.getX(), y, base.getZ());
                    return loc.add(0.5, 0, 0.5); // ground level
                }
            }
        }

        // Failsafe
        return raidLocation.clone().add(0.5, 1, 0.5);
    }

    @SuppressWarnings("unchecked")
    private void applyEquipment(LivingEntity entity, Map<?, ?> mobData) {
        if (!mobData.containsKey("equipment"))
            return;

        Object eqObj = mobData.get("equipment");
        if (!(eqObj instanceof Map))
            return;

        Map<String, String> equipment = (Map<String, String>) eqObj;

        org.bukkit.inventory.EntityEquipment currEq = entity.getEquipment();
        if (currEq == null)
            return;

        if (equipment.containsKey("helmet")) {
            currEq.setHelmet(parseItem(equipment.get("helmet")));
            currEq.setHelmetDropChance(0.0f);
        }
        if (equipment.containsKey("chestplate")) {
            currEq.setChestplate(parseItem(equipment.get("chestplate")));
            currEq.setChestplateDropChance(0.0f);
        }
        if (equipment.containsKey("leggings")) {
            currEq.setLeggings(parseItem(equipment.get("leggings")));
            currEq.setLeggingsDropChance(0.0f);
        }
        if (equipment.containsKey("boots")) {
            currEq.setBoots(parseItem(equipment.get("boots")));
            currEq.setBootsDropChance(0.0f);
        }
        if (equipment.containsKey("main_hand")) {
            currEq.setItemInMainHand(parseItem(equipment.get("main_hand")));
            currEq.setItemInMainHandDropChance(0.0f);
        }
        if (equipment.containsKey("off_hand")) {
            currEq.setItemInOffHand(parseItem(equipment.get("off_hand")));
            currEq.setItemInOffHandDropChance(0.0f);
        }
    }

    private org.bukkit.inventory.ItemStack parseItem(String itemString) {
        if (itemString == null || itemString.isEmpty())
            return null;
        String[] parts = itemString.split(":");
        Material mat = Material.matchMaterial(parts[0].toUpperCase());
        if (mat == null)
            return null;

        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        if (parts.length > 1 && item.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            String colorStr = parts[1].toUpperCase();
            org.bukkit.Color color = org.bukkit.Color.WHITE;
            switch (colorStr) {
                case "BLACK" -> color = org.bukkit.Color.BLACK;
                case "BLUE" -> color = org.bukkit.Color.BLUE;
                case "BROWN" -> color = org.bukkit.Color.MAROON;
                case "CYAN" -> color = org.bukkit.Color.AQUA;
                case "GRAY" -> color = org.bukkit.Color.GRAY;
                case "GREEN" -> color = org.bukkit.Color.GREEN;
                case "LIGHT_BLUE" -> color = org.bukkit.Color.NAVY;
                case "MAGENTA" -> color = org.bukkit.Color.FUCHSIA;
                case "ORANGE" -> color = org.bukkit.Color.ORANGE;
                case "PINK" -> color = org.bukkit.Color.FUCHSIA;
                case "PURPLE" -> color = org.bukkit.Color.PURPLE;
                case "RED" -> color = org.bukkit.Color.RED;
                case "WHITE" -> color = org.bukkit.Color.WHITE;
                case "YELLOW" -> color = org.bukkit.Color.YELLOW;
                default -> {
                    try {
                        color = org.bukkit.Color.fromRGB(Integer.parseInt(colorStr.replace("#", ""), 16));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }
}
