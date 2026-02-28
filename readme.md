# 🌟 RuneMobs Plugin

A unique Minecraft plugin that transforms regular monster combat into a thrilling RPG experience by introducing **Elite Mobs**, **Custom Drops**, and a brand new **Rune Engraving Mechanic**.

---

## ✨ Features

### ⚔️ Elite Mob Tiers
Not every mob is born equal. When a monster spawns, it has a chance to be promoted to an Elite tier with significantly buffed stats, custom colored nameplates, and special drops!

- **Rare Mobs** (Default 15% chance): Modest health and damage buffs. Drops 1-4 Emeralds.
- **Epic Mobs** (Default 5% chance): Significant health and damage buffs. Drops 4-8 Emeralds. Elite mobs of specific types gain special abilities!
- **Legendary Mobs** (Default 1% chance): Boss-level health and damage. Drops 8-16 Emeralds and is guaranteed to drop a special **Legendary Engraving Material (Rune)**! Legendary mobs trigger identical special abilities to Epic mobs.

### 💀 Elite Mob Abilities
When naturally spawned or summoned as Epic/Legendary status, unique enemies exhibit terrifying new behaviors:
- **Zombies:** Has a 15% chance whenever taking non-fatal damage to split off a vicious **Baby Zombie Minion** to attack you!
- **Skeletons/Strays:** Taking a hit or arrow from an elite skeleton instantly covers you in frost and applies **Slowness**. 
- **Spiders/Cave Spiders:** Whenever a spider attacks you, there's a 25% chance it will violently weave a physical block of **Cobweb** at your exact location, trapping you.
- **Endermen:** Getting too close and hitting an elite Enderman will unleash a shockwave of shadows in a 6-block radius, dynamically blasting all nearby players with **Blindness**.
- **Creepers:** When an Epic or Legendary Creeper receives fatal damage, its death is instantly canceled, it plays a Totem of Undying effect, heals to full HP, and its name turns `Dark Red` to indicate its enraged revived state!
- **Vindicators:** Their feral attacks have a 20% chance to deal 1.5x extreme damage and intensely knock you into the air.
- **Pillagers:** Arrows from an elite pillager have a 20% chance of applying **Weakness**.
- **Evokers:** Whenever an Evoker takes damage, there's a 15% chance they will dynamically teleport away to a safer block and immediately spawn two **Vexes**!
- **Ravagers:** Taking a massive hit from this beast will dynamically calculate momentum and launch you far away from it.
- **Vexes:** Getting slashed by these demons will immediately rot you with **Wither** and inflict debilitating **Nausea**.
- **Piglin/Brute/Zombified Piglin:** Whenever they take damage, there's a 20% chance they will become completely enraged, gaining **Speed II** and **Strength I**!
- **Wither Skeletons:** Being struck applies extreme Wither duration, and carries a 15% chance to completely blind you with **Blindness**.
- **Ghast:** When taking damage, there's a 20% chance the Ghast screams and rapidly spawns a **Blaze** beneath itself!
- **Magma Cubes:** When damaged, there's a 25% chance of instantly igniting all nearby players with fire and erupting in a shower of lava particles.
- **Hoglins:** Emulating the Iron Golem, hits from Elite Hoglins will forcefully knock players high up into the air.
- **Shulkers:** Standard hits from an elite Shulker have a 25% chance to inflict **Levitation Level II** for extreme launch distances.

### 💎 The Rune System
Legendary mobs will drop a Legendary Engraving Material (Rune). Runes are powerful artifacts used to permanently upgrade your equipment.
The engraving process is exceptionally simple and completely bypasses the anvil!

**How to Apply a Rune:**
1. Open your player inventory.
2. Left-click to pick up the Rune.
3. Hover over the piece of equipment you want to upgrade.
4. **Click the equipment** while holding the Rune.
5. If the item is eligible, the Rune will be consumed, and the special perk will be permanently bound to the item! 
*(Note: Each item can only be engraved once! You cannot stack multiple runes on the same item.)*

**Available Runes:**
- 🩸 **Rune of Vampirism**
  - **Applicable to:** Swords and Axes
  - **Effect:** Grants +20% Lifesteal. When you hit an enemy, you will heal for 20% of the raw damage you inflict!
- ☣️ **Rune of Venom**
  - **Applicable to:** Swords and Axes
  - **Effect:** Inflicts Poison II on struck enemies for 3 seconds. Watch them rot!
- � **Rune of the Berserker**
  - **Applicable to:** Swords and Axes
  - **Effect:** Increases damage significantly as your HP drops below 50%.
- �💚 **Rune of Life**
  - **Applicable to:** Chestplates and Leggings
  - **Effect:** Increases your Maximum Health by +8 (4 full hearts) while the armor piece is worn. 
- 🛡️ **Rune of the Colossus**
  - **Applicable to:** Chestplates and Leggings
  - **Effect:** Grants +50% Knockback Resistance. Stand your ground against the hordes!
- ☀️ **Rune of Solar Blessing**
  - **Applicable to:** Chestplates
  - **Effect:** Grants Max Health Boost during the day and Movement Speed Boost during the night.
- 🐦‍🔥 **Rune of the Phoenix**
  - **Applicable to:** Chestplates
  - **Effect:** Survives a fatal blow, restoring full health and granting powerful potion effects before shattering.
- ⚡ **Rune of Swiftness**
  - **Applicable to:** Boots
  - **Effect:** Increases your base movement speed by +10% and grants Jump Boost while the boots are worn.
- � **Rune of the Frog**
  - **Applicable to:** Boots
  - **Effect:** Allows double and triple jumping in mid-air.
- �🔥 **Rune of Retribution**
  - **Applicable to:** Helmets and Shields
  - **Effect:** Ignites enemies who dare to attack you in melee for 4 seconds!
- ❄️ **Rune of Frost**
  - **Applicable to:** Helmets
  - **Effect:** Slows and fatigues attackers in a 4x4 area when you are hit.
- 🌩️ **Rune of Thunder**
  - **Applicable to:** Shields
  - **Effect:** Strikes attackers with lightning while you are actively blocking.
- 🌩️ **Rune of the Stormcaller**
  - **Applicable to:** Tridents
  - **Effect:** 50% chance to call down a lightning strike on the target when striking in melee or hitting with a throw!
- 🔱 **Rune of the Impaler**
  - **Applicable to:** Tridents
  - **Effect:** Deals extreme knockback, immobilizes the target for 2s, and grants you a speed boost.
- 💥 **Rune of Detonation**
  - **Applicable to:** Bows and Crossbows
  - **Effect:** Arrows shot from this weapon will dynamically explode upon hitting a target!
- ⛏️ **Rune of the Furnace**
  - **Applicable to:** Pickaxes
  - **Effect:** Automatically smelts Raw Iron, Gold, and Copper into ingots when mined, showering you with flames!
- 🪽 **Rune of Ascension**
  - **Applicable to:** Elytras
  - **Effect:** Instantly propels the wearer upwards with a burst of force whenever they take flight!
- 🕳️ **Rune of the Excavator**
  - **Applicable to:** Shovels
  - **Effect:** Vaporizes massive amounts of earth! Breaking dirt, sand, or gravel mines an entire 3x3x3 area at once!
- 🎣 **Rune of the Grappler**
  - **Applicable to:** Fishing Rods
  - **Effect:** Hold Sneak while reeling in your hook to massively propel yourself towards whatever you hooked to traverse terrain or enemies rapidly!
- ☄️ **Rune of Leaping**
  - **Applicable to:** Maces
  - **Effect:** Left-click a block to launch yourself forward and upward with a massive leap. Grants 5s fall damage immunity.
- ☠️ **Rune of the Scythe**
  - **Applicable to:** Hoes and Tridents
  - **Effect:** Deals area-of-effect damage, increases raw damage, and drains hunger/saturation from players.

### ⏳ Rune Decay (Time Limit)
Runes are powerful, but their magic is unstable. By default, an engraved rune will **expire after 60 minutes**. 
You can view exactly when a rune will expire in the lore of the item. Once the time limit passes, the magic dissipates dynamically, and the item returns to normal! This duration can be fully customized in the configuration.

---

## ⚔️ Raids System
RuneMobs features a built-in, fully configurable RAID system to spawn consecutive waves of fierce, custom Elite and Legendary enemies directly on a targeted player!

- By default, the `Army of Eikthyr` is fully configured, summoning 5 deadly waves containing Elite Wolves, Epic Zombies, Skeletons, and concluding in a brutal clash with a Legendary Ravager and Evoker!
- Boss Bars correctly track the percentage of waves completed during a Raid.
- Completely customizable via `config.yml`. Create your own dynamic battle sequences by specifying mob types, rarity modifiers, wave timings, and boss bars.

---

## 💻 Commands
- `/runemobs give` - Grants the executor a random Rune (OP Only).
- `/runemobs summon <mobType> <epic|legendary>` - Spawns a fully modified Elite/Legendary entity of that type directly in front of the executor. (E.g. `/runemobs summon zombie epic`) (OP Only).
- `/runemobs raid <raidName>` - Triggers a massive raid event on the executor's location, pulling config wave values from `raids.<raidName>`. (E.g. `/runemobs raid eikthyr`) (OP Only).
- `/runemobs stopraid` - Forcefully stops the currently active boss raid event and wipes the spawned boss + minions (OP Only).
- `/runemobs reload` - Reloads the `config.yml` file, allowing real-time edits to mob stats without server restart.
- **Master Permission:** `runemobs.admin`

---

## 🔧 Configuration

All variables for spawn chances, power levels, and gemstone yields are strictly customizable via `config.yml`.

### `config.yml`

```yaml
rarities:
  rare:
    chance: 0.15 # 15% chance for a spawned mob to be Rare
    name_format: "&9[Rare] &f{mob}"
    health_multiplier: 1.5
    damage_multiplier: 1.25
    emerald_drop:
      min: 1
      max: 4
  epic:
    chance: 0.05 # 5% chance
    name_format: "&5[Epic] &f{mob}"
    health_multiplier: 2.5
    damage_multiplier: 1.5
    emerald_drop:
      min: 4
      max: 8
  legendary:
    chance: 0.01 # 1% chance
    name_format: "&6[Legendary] &f{mob}"
    health_multiplier: 5.0
    damage_multiplier: 2.0
    emerald_drop:
      min: 8
      max: 16
    rune_drop_chance: 1.0 # 100% chance for legendary to drop a Rune
```

---

## 🏗 How to Build

If you are a developer looking to compile the plugin from the source, the project uses **Gradle** as its build tool and targets **Paper 1.21.7** (Java 21).

### Steps to Compile:
1. Ensure you have **Java 21** installed on your system.
2. Open a terminal or Command Prompt in the root directory of the plugin folder.
3. Run the Gradle build command:
   - On Windows: `.\gradlew.bat clean build`
   - On Linux/Mac: `./gradlew clean build`
4. Wait for the compilation process to finish. If successful, you'll see a `BUILD SUCCESSFUL` message.
5. The freshly compiled `.jar` file will be located in the `build/libs/` folder.
6. Copy `RuneMobs-1.0.0.jar` into your Minecraft server's `plugins` folder, and start the server!

*(Note: Run `.\gradlew.bat clean build --no-daemon` if you need to trace full compiler outputs or errors without JVM caching.)*
