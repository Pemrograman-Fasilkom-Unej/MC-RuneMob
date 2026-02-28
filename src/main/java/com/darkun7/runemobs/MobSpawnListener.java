package com.darkun7.runemobs;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.Random;

public class MobSpawnListener implements Listener {

    private final RuneMobs plugin;
    private final Random random = new Random();

    public MobSpawnListener(RuneMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster))
            return;

        LivingEntity mob = event.getEntity();
        if (mob.getCustomName() != null)
            return; // Ignore already named mobs

        double roll = random.nextDouble();
        String rarity = null;

        double legChance = plugin.getConfig().getDouble("rarities.legendary.chance", 0.01);
        double epicChance = plugin.getConfig().getDouble("rarities.epic.chance", 0.05);
        double rareChance = plugin.getConfig().getDouble("rarities.rare.chance", 0.1);

        if (roll < legChance) {
            rarity = "legendary";
        } else if (roll < legChance + epicChance) {
            rarity = "epic";
        } else if (roll < legChance + epicChance + rareChance) {
            rarity = "rare";
        }

        if (rarity != null) {
            applyRarity(mob, rarity, plugin);
        }
    }

    public static void applyRarity(LivingEntity mob, String rarity, org.bukkit.plugin.Plugin plugin) {
        String nameFormat = plugin.getConfig().getString("rarities." + rarity + ".name_format",
                "&c[" + rarity + "] {mob}");
        String mobName = mob.getType().name().replace("_", " ");

        // Capitalize mob name
        mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).toLowerCase();

        String fullName = ChatColor.translateAlternateColorCodes('&', nameFormat.replace("{mob}", mobName));

        mob.setCustomName(fullName);
        mob.setCustomNameVisible(true);

        double hpMult = plugin.getConfig().getDouble("rarities." + rarity + ".health_multiplier", 1.0);

        if (mob.getAttribute(Attribute.MAX_HEALTH) != null) {
            double baseHp = mob.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseHp * hpMult);
            mob.setHealth(mob.getAttribute(Attribute.MAX_HEALTH).getValue());
        }

        // Save rarity into PersistentDataContainer
        NamespacedKey key = new NamespacedKey(plugin, "mob_rarity");
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, rarity);
    }
}
