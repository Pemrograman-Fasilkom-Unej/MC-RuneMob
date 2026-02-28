package com.darkun7.runemobs;

import org.bukkit.ChatColor;
import com.darkun7.runemobs.vanilla.EnderDragonManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RuneMobs extends JavaPlugin {

    private static RuneMobs instance;

    // Track active boss raid to prevent overlapping boss fights
    // Eikthyr (army raid) is exempt and can run alongside a boss raid
    private com.darkun7.runemobs.raids.RuneRaid activeBossRaid = null;

    @Override
    public void onEnable() {
        instance = this;

        // Auto-update config.yml if outdated
        String expectedVersion = "240807-prod";
        String currentVersion = getConfig().getString("config-version", "0");

        if (!expectedVersion.equals(currentVersion)) {
            getLogger().warning("Outdated config.yml detected! Backing up and regenerating...");

            java.io.File configFile = new java.io.File(getDataFolder(), "config.yml");
            java.io.File backupFile = new java.io.File(getDataFolder(), "config-old.yml");

            if (configFile.exists()) {
                configFile.renameTo(backupFile);
                getLogger().info("Old config backed up as config-old.yml.");
            }

            saveResource("config.yml", true);
        } else {
            saveDefaultConfig();
        }

        reloadConfig();

        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new MobDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new RuneApplyListener(this), this);
        getServer().getPluginManager().registerEvents(new RuneEffectListener(this), this);
        getServer().getPluginManager().registerEvents(new com.darkun7.runemobs.raids.RaidScrollListener(this), this);
        getServer().getPluginManager().registerEvents(new EnderDragonManager(this), this);

        // Schedule periodic rune cleanup (every 10 seconds / 200 ticks)
        RuneCleanupTask cleanupTask = new RuneCleanupTask(this);
        getServer().getPluginManager().registerEvents(cleanupTask, this);
        getServer().getScheduler().runTaskTimer(this, cleanupTask, 200L, 200L);

        // Schedule Auto Raids
        if (getConfig().getBoolean("auto_raid.enabled", false)) {
            long intervalTicks = getConfig().getInt("auto_raid.interval_minutes", 60) * 60 * 20L;
            getServer().getScheduler().runTaskTimer(this, () -> {
                if (getServer().getOnlinePlayers().isEmpty())
                    return;

                String raidId = getConfig().getString("auto_raid.raid_id", "eikthyr");
                if (getConfig().getConfigurationSection("raids." + raidId) == null) {
                    getLogger().warning("Auto-raid configured for unknown raid: " + formatRaidName(raidId));
                    return;
                }

                // Pick a random online player as the target
                java.util.List<org.bukkit.entity.Player> players = new java.util.ArrayList<>(
                        getServer().getOnlinePlayers());
                org.bukkit.entity.Player target = players.get(new java.util.Random().nextInt(players.size()));

                // Skip auto-raid if a boss raid is already active (eikthyr is exempt)
                if (!raidId.equalsIgnoreCase("eikthyr") && hasBossRaidActive()) {
                    getLogger()
                            .info("Auto-raid '" + formatRaidName(raidId) + "' skipped: a boss raid is already active.");
                    return;
                }

                com.darkun7.runemobs.raids.RuneRaid raid = new com.darkun7.runemobs.raids.RuneRaid(this, raidId,
                        target);
                raid.start();
                getLogger().info("Auto-raid '" + formatRaidName(raidId) + "' started targeting " + target.getName());
            }, intervalTicks, intervalTicks);
        }

        // Schedule Passive Rune Task (every 40 ticks / 2 seconds)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                org.bukkit.NamespacedKey typeKey = new org.bukkit.NamespacedKey(this, "rune_type");
                for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
                    if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                            .has(typeKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        String typeStr = item.getItemMeta().getPersistentDataContainer().get(typeKey,
                                org.bukkit.persistence.PersistentDataType.STRING);
                        playRunePassiveParticle(player, typeStr);

                        // Solar Blessing
                        if ("SOLAR_BLESSING".equals(typeStr)) {
                            long time = player.getWorld().getTime();
                            if (time < 12000) { // Day
                                if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.HEALTH_BOOST)
                                        || player.getPotionEffect(org.bukkit.potion.PotionEffectType.HEALTH_BOOST)
                                                .getDuration() < 60) {
                                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                            org.bukkit.potion.PotionEffectType.HEALTH_BOOST, 200, 1, false, false,
                                            true));
                                }
                            } else { // Night
                                if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED) || player
                                        .getPotionEffect(org.bukkit.potion.PotionEffectType.SPEED).getDuration() < 60) {
                                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                            org.bukkit.potion.PotionEffectType.SPEED, 200, 0, false, false, true));
                                }
                            }
                        }
                        // Swiftness Jump Boost
                        else if ("SWIFTNESS".equals(typeStr)) {
                            if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST)
                                    || player.getPotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST)
                                            .getDuration() < 60) {
                                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                        org.bukkit.potion.PotionEffectType.JUMP_BOOST, 200, 1, false, false, true));
                            }
                        }
                    }
                }

                for (org.bukkit.inventory.ItemStack item : new org.bukkit.inventory.ItemStack[] {
                        player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand() }) {
                    if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                            .has(typeKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                        String typeStr = item.getItemMeta().getPersistentDataContainer().get(typeKey,
                                org.bukkit.persistence.PersistentDataType.STRING);
                        playRunePassiveParticle(player, typeStr);
                    }
                }
            }
        }, 40L, 40L);

        getLogger().info("RuneMobs plugin enabled!");
    }

    private void playRunePassiveParticle(org.bukkit.entity.Player player, String typeStr) {
        org.bukkit.Location loc = player.getLocation().add(0, 1.2, 0);
        org.bukkit.World w = player.getWorld();

        // Strip ASCENDED_ prefix so upgraded runes share the same particle effects
        String particleStr = typeStr.startsWith("ASCENDED_") ? typeStr.substring(9) : typeStr;

        switch (particleStr) {
            case "VAMPIRISM":
                w.spawnParticle(org.bukkit.Particle.HEART, loc, 1, 0.4, 0.4, 0.4, 0);
                break;
            case "LIFE":
                w.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc, 2, 0.4, 0.4, 0.4, 0);
                break;
            case "SWIFTNESS":
                w.spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 2, 0.3, 0.1, 0.3, 0.05);
                break;
            case "VENOM":
                w.spawnParticle(org.bukkit.Particle.COMPOSTER, loc, 3, 0.4, 0.4, 0.4, 0);
                break;
            case "COLOSSUS":
                w.spawnParticle(org.bukkit.Particle.BLOCK, loc, 5, 0.4, 0.4, 0.4, 0,
                        org.bukkit.Material.OBSIDIAN.createBlockData());
                break;
            case "RETRIBUTION":
                org.bukkit.Location haloLoc = player.getLocation().add(0, 2.1, 0);
                for (int i = 0; i < 360; i += 45) {
                    double angle = Math.toRadians(i);
                    double x = 0.4 * Math.cos(angle);
                    double z = 0.4 * Math.sin(angle);
                    w.spawnParticle(org.bukkit.Particle.FLAME, haloLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }
                break;
            case "STORMCALLER":
                w.spawnParticle(org.bukkit.Particle.NAUTILUS, loc, 2, 0.4, 0.4, 0.4, 0.02);
                break;
            case "DETONATION":
                w.spawnParticle(org.bukkit.Particle.SMOKE, loc, 2, 0.4, 0.4, 0.4, 0.02);
                break;
            case "FURNACE":
                w.spawnParticle(org.bukkit.Particle.LAVA, loc, 1, 0.4, 0.4, 0.4, 0);
                break;
            case "ASCENSION":
                w.spawnParticle(org.bukkit.Particle.END_ROD, loc, 2, 0.4, 0.4, 0.4, 0.02);
                break;
            case "EXCAVATOR":
                w.spawnParticle(org.bukkit.Particle.CRIT, loc, 2, 0.4, 0.4, 0.4, 0.02);
                break;
            case "GRAPPLER":
                w.spawnParticle(org.bukkit.Particle.SPLASH, loc, 3, 0.4, 0.4, 0.4, 0.02);
                break;
            case "SLOW_AURA":
                org.bukkit.Location frostLoc = player.getLocation().add(0, 0.1, 0);
                for (int i = 0; i < 360; i += 45) {
                    double angle = Math.toRadians(i);
                    double x = 1.0 * Math.cos(angle);
                    double z = 1.0 * Math.sin(angle);
                    w.spawnParticle(org.bukkit.Particle.SNOWFLAKE, frostLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }
                break;
            case "LEAP":
                w.spawnParticle(org.bukkit.Particle.EXPLOSION, loc, 1, 0.4, 0.4, 0.4, 0);
                break;
            case "TRIPLE_JUMP":
                w.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, loc, 2, 0.4, 0.4, 0.4, 0.1);
                break;
            case "LIGHTNING_SHIELD":
                w.spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, loc, 2, 0.4, 0.4, 0.4, 0.05);
                break;
            case "SOLAR_BLESSING":
                org.bukkit.Location bodyLoc = player.getLocation().add(0, 1.0, 0);
                for (int i = 0; i < 360; i += 45) {
                    double angle = Math.toRadians(i);
                    double x = 0.6 * Math.cos(angle);
                    double z = 0.6 * Math.sin(angle);
                    w.spawnParticle(org.bukkit.Particle.WAX_ON, bodyLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }
                break;
            case "SCYTHE":
                w.spawnParticle(org.bukkit.Particle.SOUL, loc, 2, 0.4, 0.4, 0.4, 0.02);
                break;
            case "SPEAR_THRUST":
                w.spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, loc, 2, 0.4, 0.4, 0.4, 0.05);
                break;
            case "PHOENIX_CORE":
                org.bukkit.Location phoenixLoc = player.getLocation().add(0, 1.0, 0);
                for (int i = 0; i < 360; i += 45) {
                    double angle = Math.toRadians(i);
                    double x = 0.5 * Math.cos(angle);
                    double z = 0.5 * Math.sin(angle);
                    w.spawnParticle(org.bukkit.Particle.CAMPFIRE_COSY_SMOKE, phoenixLoc.clone().add(x, -0.5, z), 1, 0,
                            0, 0, 0.02);
                    w.spawnParticle(org.bukkit.Particle.FLAME, phoenixLoc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0.02);
                }
                break;
            case "BERSERK":
                w.spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, loc, 1, 0.4, 0.4, 0.4, 0);
                break;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("RuneMobs plugin disabled!");
    }

    public static RuneMobs getInstance() {
        return instance;
    }

    /**
     * Check if a boss raid is currently active.
     */
    public boolean hasBossRaidActive() {
        return activeBossRaid != null && activeBossRaid.isActive();
    }

    /**
     * Get the active boss raid, if any.
     */
    public com.darkun7.runemobs.raids.RuneRaid getActiveBossRaid() {
        return activeBossRaid;
    }

    /**
     * Register a boss raid as the active one.
     */
    public void setActiveBossRaid(com.darkun7.runemobs.raids.RuneRaid raid) {
        this.activeBossRaid = raid;
    }

    /**
     * Clear the active boss raid (called when it ends).
     */
    public void clearActiveBossRaid() {
        this.activeBossRaid = null;
    }

    /**
     * Formats a raid ID into a professional display name (e.g.,
     * "corrupted_guardian" to "Corrupted Guardian")
     */
    public static String formatRaidName(String raidId) {
        if (raidId == null || raidId.isEmpty())
            return raidId;
        String[] words = raidId.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 0) {
                sb.append(Character.toUpperCase(words[i].charAt(0)))
                        .append(words[i].substring(1).toLowerCase());
                if (i < words.length - 1)
                    sb.append(" ");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("runemobs")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "RuneMobs configuration reloaded!");
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
                // Command to give runes (admin only)
                if (!sender.isOp()) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }

                org.bukkit.entity.Player target = null;
                if (sender instanceof org.bukkit.entity.Player player) {
                    target = player;
                }

                if (args.length >= 2) {
                    target = getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                        return true;
                    }
                }

                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "You must specify a player if running from console.");
                    return true;
                }

                if (args.length >= 3) {
                    try {
                        RuneManager.RuneType type = RuneManager.RuneType.valueOf(args[2].toUpperCase());
                        target.getInventory().addItem(RuneManager.createRune(type));
                        sender.sendMessage(ChatColor.GREEN + "Given " + type.name() + " to " + target.getName() + "!");
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid rune type: " + args[2]);
                    }
                } else {
                    target.getInventory().addItem(RuneManager.createRandomRune());
                    sender.sendMessage(ChatColor.GREEN + "Given a random rune to " + target.getName() + "!");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("summon")) {
                if (sender instanceof org.bukkit.entity.Player player) {
                    if (!player.isOp()) {
                        player.sendMessage(ChatColor.RED + "You do not have permission.");
                        return true;
                    }
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /runemobs summon <mobType> <rarity>");
                        return true;
                    }
                    try {
                        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(args[1].toUpperCase());
                        String rarity = args[2].toLowerCase();

                        org.bukkit.Location loc = player.getLocation()
                                .add(player.getLocation().getDirection().multiply(2));
                        org.bukkit.entity.Entity entity = player.getWorld().spawnEntity(loc, type);

                        if (entity instanceof org.bukkit.entity.LivingEntity le) {
                            MobSpawnListener.applyRarity(le, rarity, this);
                            player.sendMessage(ChatColor.GREEN + "Summoned " + rarity + " " + type.name() + "!");
                        } else {
                            entity.remove();
                            player.sendMessage(ChatColor.RED + "Entity must be a Living Entity!");
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid mob type: " + args[1]);
                    }
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("raid")) {
                if (sender instanceof org.bukkit.entity.Player player) {
                    if (!player.isOp()) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "You do not have permission.");
                        return true;
                    }
                    if (args.length < 2) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "Usage: /runemobs raid <raidName>");
                        return true;
                    }

                    String raidId = args[1].toLowerCase();
                    if (getInstance().getConfig().getConfigurationSection("raids." + raidId) == null) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "Raid '" + formatRaidName(raidId)
                                + "' not found in config.");
                        return true;
                    }

                    com.darkun7.runemobs.raids.RuneRaid raid = new com.darkun7.runemobs.raids.RuneRaid(this, raidId,
                            player);
                    raid.start();
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "Raid '" + formatRaidName(raidId) + "' started!");
                } else {
                    sender.sendMessage(org.bukkit.ChatColor.RED + "Only players can start a raid this way.");
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("stopraid")) {
                if (sender instanceof org.bukkit.entity.Player player) {
                    if (!player.isOp()) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "You do not have permission.");
                        return true;
                    }
                }
                if (hasBossRaidActive()) {
                    getActiveBossRaid().forceEndRaid();
                    sender.sendMessage(
                            org.bukkit.ChatColor.GREEN + "The active boss raid has been forcefully stopped!");
                } else {
                    sender.sendMessage(org.bukkit.ChatColor.RED + "There is no boss raid currently active.");
                }
                return true;
            }
            sender.sendMessage(org.bukkit.ChatColor.AQUA + "Usage: /runemobs <reload|give|summon|raid|stopraid>");
            return true;
        }
        return false;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> completions = new java.util.ArrayList<>();
        if (command.getName().equalsIgnoreCase("runemobs")) {
            if (args.length == 1) {
                java.util.List<String> subcommands = java.util.Arrays.asList("reload", "give", "summon", "raid",
                        "stopraid");
                for (String sub : subcommands) {
                    if (sub.startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("give")) {
                    for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("raid")) {
                    if (getConfig().getConfigurationSection("raids") != null) {
                        for (String raidId : getConfig().getConfigurationSection("raids").getKeys(false)) {
                            if (raidId.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(raidId);
                            }
                        }
                    }
                } else if (args[0].equalsIgnoreCase("summon")) {
                    for (org.bukkit.entity.EntityType type : org.bukkit.entity.EntityType.values()) {
                        if (type.isSpawnable() && type.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(type.name().toLowerCase());
                        }
                    }
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("give")) {
                    for (RuneManager.RuneType type : RuneManager.RuneType.values()) {
                        if (type.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(type.name().toLowerCase());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("summon")) {
                    java.util.List<String> rarities = java.util.Arrays.asList("common", "uncommon", "rare", "epic",
                            "legendary", "mythic");
                    for (String r : rarities) {
                        if (r.startsWith(args[2].toLowerCase())) {
                            completions.add(r);
                        }
                    }
                }
            }
        }
        return completions;
    }
}
