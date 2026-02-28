package com.darkun7.runemobs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RuneManager {

    private static final Random random = new Random();

    public enum RuneType {
        VAMPIRISM("Rune of Vampirism", ChatColor.RED, "+20% Lifesteal (Swords/Axes)", "runemobs_vampirism"),
        LIFE("Rune of Life", ChatColor.GREEN, "+8 Max HP (Chest/Leggings)", "runemobs_life"),
        SWIFTNESS("Rune of Swiftness", ChatColor.AQUA, "+10% Speed & Jump Boost (Boots)", "runemobs_speed"),
        VENOM("Rune of Venom", ChatColor.DARK_GREEN, "Inflicts Poison II (Swords/Axes)", "runemobs_venom"),
        COLOSSUS("Rune of the Colossus", ChatColor.GOLD, "+50% Knockback Resistance (Chest/Leggings)",
                "runemobs_colossus"),
        RETRIBUTION("Rune of Retribution", ChatColor.GOLD, "Ignites attackers for 4s (Helmets/Shields)",
                "runemobs_retribution"),
        STORMCALLER("Rune of the Stormcaller", ChatColor.YELLOW, "50% chance to smite with Lightning (Tridents)",
                "runemobs_stormcaller"),
        DETONATION("Rune of Detonation", ChatColor.RED, "Projectiles explode on impact (Bow/Crossbow)",
                "runemobs_detonation"),
        FURNACE("Rune of the Furnace", ChatColor.GOLD, "Auto-smelts mined ores (Pickaxes)", "runemobs_furnace"),
        ASCENSION("Rune of Ascension", ChatColor.WHITE, "Propels you upwards when taking flight (Elytra)",
                "runemobs_ascension"),
        EXCAVATOR("Rune of the Excavator", ChatColor.DARK_GRAY, "Mines a 3x3x1 area instantly (Shovels)",
                "runemobs_excavator"),
        GRAPPLER("Rune of the Grappler", ChatColor.BLUE,
                "Pulls you toward hooked targets while sneaking! (Fishing Rods)", "runemobs_grappler"),
        SLOW_AURA("Rune of Frost", ChatColor.AQUA, "Slows and fatigues a 4x4 area when hit (Helmets)",
                "runemobs_slow_aura"),
        LEAP("Rune of Leaping", ChatColor.DARK_PURPLE, "Massive leap & no fall damage on Left-Click block (Maces)",
                "runemobs_leap"),
        TRIPLE_JUMP("Rune of the Frog", ChatColor.GREEN, "Allows double and triple jumping (Boots)",
                "runemobs_triple_jump"),
        LIGHTNING_SHIELD("Rune of Thunder", ChatColor.YELLOW,
                "Strikes attackers with lightning while blocking (Shields)", "runemobs_lightning_shield"),
        SOLAR_BLESSING("Rune of Solar Blessing", ChatColor.GOLD, "Day: Max HP Boost | Night: Speed Boost (Chest)",
                "runemobs_solar_blessing"),
        SCYTHE("Rune of the Scythe", ChatColor.DARK_RED, "Area damage & reduces enemy hunger (Spears/Tridents/Hoes)",
                "runemobs_scythe"),
        SPEAR_THRUST("Rune of the Impaler", ChatColor.GRAY,
                "Knockback, Speed boost & immobilizes enemy for 2s (Spears/Tridents)", "runemobs_spear_thrust"),
        PHOENIX_CORE("Rune of the Phoenix", ChatColor.GOLD, "Revives once upon death (Chestplates)",
                "runemobs_phoenix_core"),
        BERSERK("Rune of the Berserker", ChatColor.DARK_RED, "Increases damage as your HP drops (Swords/Axes)",
                "runemobs_berserk"),

        // ====== ASCENDED BOSS RUNES ======
        ASCENDED_RETRIBUTION("Ascended Rune of Retribution", ChatColor.GOLD,
                "Ignites attackers for 8s + small explosion (Helmets/Shields)", "runemobs_ascended_retribution"),
        ASCENDED_SOLAR_BLESSING("Ascended Rune of Solar Blessing", ChatColor.GOLD,
                "Day: HP Boost + Regen | Night: Speed + Haste (Chest)", "runemobs_ascended_solar_blessing"),
        ASCENDED_STORMCALLER("Ascended Rune of the Stormcaller", ChatColor.GOLD,
                "75% lightning + chains to nearby enemies (Tridents)", "runemobs_ascended_stormcaller"),
        ASCENDED_VENOM("Ascended Rune of Venom", ChatColor.GOLD,
                "Inflicts Poison III + Wither I on hit (Swords/Axes)", "runemobs_ascended_venom"),
        ASCENDED_PHOENIX_CORE("Ascended Rune of the Phoenix", ChatColor.GOLD,
                "Revives with Strength II + fire aura on death (Chestplates)", "runemobs_ascended_phoenix"),
        ASCENDED_FURNACE("Ascended Rune of the Furnace", ChatColor.GOLD,
                "Auto-smelts ores + double ingot output (Pickaxes)", "runemobs_ascended_furnace"),
        ASCENDED_BERSERK("Ascended Rune of the Berserker", ChatColor.GOLD,
                "Higher damage scaling + lifesteal below 25% HP (Swords/Axes)", "runemobs_ascended_berserk"),
        ASCENDED_VAMPIRISM("Ascended Rune of Vampirism", ChatColor.GOLD,
                "+35% Lifesteal + inflicts Wither on enemies (Swords/Axes)", "runemobs_ascended_vampirism");

        private final String name;
        private final ChatColor color;
        private final String loreDesc;
        private final String keyName;

        RuneType(String name, ChatColor color, String loreDesc, String keyName) {
            this.name = name;
            this.color = color;
            this.loreDesc = loreDesc;
            this.keyName = keyName;
        }

        public String getName() {
            return ChatColor.WHITE + "<" + color + name + ChatColor.WHITE + ">";
        }

        public String getLoreDesc() {
            return ChatColor.GRAY + loreDesc;
        }

        public String getKeyName() {
            return keyName;
        }
    }

    public static ItemStack createRandomRune() {
        // Only regular runes can drop randomly, not ascended boss runes
        RuneType[] allRunes = RuneType.values();
        List<RuneType> normalRunes = new ArrayList<>();
        for (RuneType rt : allRunes) {
            if (!rt.name().startsWith("ASCENDED_")) {
                normalRunes.add(rt);
            }
        }
        RuneType chosen = normalRunes.get(random.nextInt(normalRunes.size()));
        return createRune(chosen);
    }

    public static ItemStack createRune(RuneType type) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.getName());
            List<String> lore = new ArrayList<>();

            boolean isAscended = type.name().startsWith("ASCENDED_");

            if (isAscended) {
                lore.add(ChatColor.GOLD + "✦ Boss Trophy Rune ✦");
                lore.add(ChatColor.DARK_GRAY + "Upgraded by the power of a slain boss");
            } else {
                lore.add(ChatColor.DARK_PURPLE + "Legendary Engraving Material");
            }

            lore.add("");
            lore.add(type.getLoreDesc());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Drag and drop this onto a piece of");
            lore.add(ChatColor.YELLOW + "equipment in your inventory to engrave.");
            meta.setLore(lore);

            NamespacedKey key = new NamespacedKey(RuneMobs.getInstance(), "rune_type");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());

            // Make ascended runes glow
            if (isAscended) {
                meta.setEnchantmentGlintOverride(true);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    public static RuneType getRuneTypeFromItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR)
            return null;
        if (!item.hasItemMeta())
            return null;

        NamespacedKey key = new NamespacedKey(RuneMobs.getInstance(), "rune_type");
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        if (pdc.has(key, PersistentDataType.STRING)) {
            String typeStr = pdc.get(key, PersistentDataType.STRING);
            try {
                return RuneType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public static void giveRandomRune(Player player) {
        ItemStack rune = createRandomRune();
        player.getInventory().addItem(rune);
    }
}
