package com.darkun7.runemobs.raids.ai;

import com.darkun7.runemobs.RuneMobs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utility class for temporarily transforming terrain around a boss battlefield.
 * Stores original block states and restores them when the fight ends.
 */
public class BattlefieldTerrain {

    private final RuneMobs plugin;
    private final Map<Location, BlockData> originalBlocks = new HashMap<>();
    private final Random random = new Random();
    private boolean restored = false;

    public BattlefieldTerrain(RuneMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets a block temporarily, recording the original state for restoration.
     * Won't overwrite protected blocks (chests, spawners, bedrock, etc.)
     */
    public void setBlock(Block block, Material newMaterial) {
        if (isProtected(block.getType()))
            return;
        if (block.getType() == newMaterial)
            return;

        // Only record the original once (don't overwrite if already tracked)
        if (!originalBlocks.containsKey(block.getLocation())) {
            originalBlocks.put(block.getLocation().clone(), block.getBlockData().clone());
        }
        block.setType(newMaterial, false);
    }

    /**
     * Sets a block temporarily with specific BlockData.
     */
    public void setBlockData(Block block, BlockData data) {
        if (isProtected(block.getType()))
            return;

        if (!originalBlocks.containsKey(block.getLocation())) {
            originalBlocks.put(block.getLocation().clone(), block.getBlockData().clone());
        }
        block.setBlockData(data, false);
    }

    /**
     * Check if a block type should never be modified.
     */
    private boolean isProtected(Material mat) {
        String name = mat.name();
        return mat == Material.BEDROCK
                || mat == Material.BARRIER
                || mat == Material.COMMAND_BLOCK
                || mat == Material.CHAIN_COMMAND_BLOCK
                || mat == Material.REPEATING_COMMAND_BLOCK
                || name.contains("CHEST")
                || name.contains("SPAWNER")
                || name.contains("SHULKER")
                || name.contains("PORTAL")
                || name.contains("END_GATEWAY")
                || name.contains("SIGN")
                || name.contains("BED")
                || name.contains("DOOR")
                || name.contains("BANNER");
    }

    /**
     * Apply plague/toxic swamp terrain around a center location.
     */
    public void applyPlagueZone(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius)
                    continue; // circular area

                int surfaceY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                Block surface = world.getBlockAt(center.getBlockX() + x, surfaceY - 1, center.getBlockZ() + z);
                Block above = world.getBlockAt(center.getBlockX() + x, surfaceY, center.getBlockZ() + z);

                // Transform ground
                if (isNaturalGround(surface.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.4) {
                        setBlock(surface, Material.PODZOL);
                    } else if (roll < 0.7) {
                        setBlock(surface, Material.MYCELIUM);
                    } else if (roll < 0.9) {
                        setBlock(surface, Material.COARSE_DIRT);
                    } else {
                        setBlock(surface, Material.ROOTED_DIRT);
                    }
                }

                // Replace vegetation with dead/fungi things
                if (isVegetation(above.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.2) {
                        setBlock(above, Material.RED_MUSHROOM);
                    } else if (roll < 0.4) {
                        setBlock(above, Material.BROWN_MUSHROOM);
                    } else if (roll < 0.6) {
                        setBlock(above, Material.DEAD_BUSH);
                    } else if (roll < 0.8) {
                        setBlock(above, Material.FERN);
                    } else {
                        setBlock(above, Material.AIR);
                    }
                }

                // Randomly add atmospheric blocks on empty spaces
                if (above.getType() == Material.AIR && random.nextDouble() < 0.1) {
                    double roll = random.nextDouble();
                    if (roll < 0.3) {
                        setBlock(above, Material.BROWN_MUSHROOM);
                    } else if (roll < 0.5) {
                        setBlock(above, Material.RED_MUSHROOM);
                    } else {
                        setBlock(above, Material.SPORE_BLOSSOM);
                    }
                }

                // Cobwebs on tree trunks
                if (random.nextDouble() < 0.05) {
                    for (int y = surfaceY + 1; y < surfaceY + 4; y++) {
                        Block webSpot = world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z);
                        if (webSpot.getType() == Material.AIR) {
                            setBlock(webSpot, Material.COBWEB);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply nether corruption terrain around a center location.
     */
    public void applyNetherCorruption(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius)
                    continue;

                // In the Nether, we scan vertically around the boss location
                for (int y = -3; y <= 5; y++) {
                    Block block = world.getBlockAt(
                            center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);

                    // Transform solid ground to nether materials
                    if (isNaturalGround(block.getType()) || block.getType() == Material.STONE
                            || block.getType() == Material.DEEPSLATE || block.getType() == Material.NETHERRACK) {
                        double roll = random.nextDouble();
                        if (roll < 0.4) {
                            setBlock(block, Material.NETHERRACK);
                        } else if (roll < 0.7) {
                            setBlock(block, Material.CRIMSON_NYLIUM);
                        } else if (roll < 0.8) {
                            setBlock(block, Material.MAGMA_BLOCK);
                        } else if (roll < 0.9) {
                            setBlock(block, Material.BASALT);
                        } else {
                            setBlock(block, Material.BLACKSTONE);
                        }
                    }

                    // Add fire and vegetation on top
                    if (block.getType() == Material.NETHERRACK || block.getType() == Material.MAGMA_BLOCK
                            || block.getType() == Material.CRIMSON_NYLIUM) {
                        Block above = block.getRelative(0, 1, 0);
                        if (above.getType() == Material.AIR) {
                            double roll = random.nextDouble();
                            if (roll < 0.05) {
                                setBlock(above, Material.FIRE);
                            } else if (roll < 0.1 && block.getType() == Material.CRIMSON_NYLIUM) {
                                setBlock(above, Material.CRIMSON_FUNGUS);
                            } else if (roll < 0.2 && block.getType() == Material.CRIMSON_NYLIUM) {
                                setBlock(above, Material.CRIMSON_ROOTS);
                            }
                        }
                    }
                }

                // Replace vegetation on surface
                int surfaceY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                Block surfaceAbove = world.getBlockAt(center.getBlockX() + x, surfaceY, center.getBlockZ() + z);
                if (isVegetation(surfaceAbove.getType())) {
                    if (random.nextDouble() < 0.3) {
                        setBlock(surfaceAbove, Material.FIRE);
                    } else if (random.nextDouble() < 0.6) {
                        setBlock(surfaceAbove, Material.CRIMSON_ROOTS);
                    } else {
                        setBlock(surfaceAbove, Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Apply mine/excavation terrain around a center location (underground).
     */
    public void applyMineZone(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius)
                    continue;

                for (int y = -2; y <= 4; y++) {
                    Block block = world.getBlockAt(
                            center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);

                    // Transform smooth stone to rough variants
                    if (block.getType() == Material.STONE || block.getType() == Material.DEEPSLATE) {
                        double roll = random.nextDouble();
                        if (roll < 0.2) {
                            setBlock(block, Material.COBBLESTONE);
                        } else if (roll < 0.35) {
                            setBlock(block, Material.GRAVEL);
                        } else if (roll < 0.5) {
                            setBlock(block, Material.ANDESITE);
                        } else if (roll < 0.6) {
                            setBlock(block, Material.TUFF);
                        } else if (roll < 0.7) {
                            setBlock(block, Material.DEEPSLATE_BRICKS);
                        }
                    }

                    // Add mine props on ground level
                    if (block.getType() == Material.AIR && y == 0) {
                        Block below = block.getRelative(0, -1, 0);
                        if (below.getType().isSolid()) {
                            double roll = random.nextDouble();
                            if (roll < 0.02) {
                                setBlock(block, Material.LANTERN);
                            } else if (roll < 0.04) {
                                setBlock(block, Material.CHAIN);
                            } else if (roll < 0.06) {
                                setBlock(block, Material.BARREL);
                            } else if (roll < 0.08) {
                                setBlock(block, Material.RAIL);
                            } else if (roll < 0.1) {
                                setBlock(block, Material.COBWEB);
                            }
                        }
                    }
                }

                // Add cobwebs and chains at ceiling
                for (int y = 3; y <= 6; y++) {
                    Block block = world.getBlockAt(
                            center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (block.getType() == Material.AIR && random.nextDouble() < 0.06) {
                        Block above = block.getRelative(0, 1, 0);
                        if (above.getType().isSolid()) {
                            if (random.nextDouble() < 0.5) {
                                setBlock(block, Material.COBWEB);
                            } else {
                                setBlock(block, Material.CHAIN);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply shadow/dark domain terrain around a center location.
     */
    public void applyShadowDomain(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius)
                    continue;

                int surfaceY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                Block surface = world.getBlockAt(center.getBlockX() + x, surfaceY - 1, center.getBlockZ() + z);
                Block above = world.getBlockAt(center.getBlockX() + x, surfaceY, center.getBlockZ() + z);

                // Darken the ground
                if (isNaturalGround(surface.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.4) {
                        setBlock(surface, Material.GRAY_CONCRETE_POWDER);
                    } else if (roll < 0.7) {
                        setBlock(surface, Material.BLACK_CONCRETE_POWDER);
                    } else if (roll < 0.85) {
                        setBlock(surface, Material.BASALT);
                    } else {
                        setBlock(surface, Material.COAL_BLOCK);
                    }
                }

                // Replace vegetation with cobwebs and dead bushes
                if (isVegetation(above.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.3) {
                        setBlock(above, Material.COBWEB);
                    } else if (roll < 0.6) {
                        setBlock(above, Material.DEAD_BUSH);
                    } else if (roll < 0.7) {
                        setBlock(above, Material.WITHER_ROSE);
                    } else {
                        setBlock(above, Material.AIR);
                    }
                }

                // Dark lighting
                if (above.getType() == Material.AIR && random.nextDouble() < 0.02) {
                    Block below = above.getRelative(0, -1, 0);
                    if (below.getType().isSolid()) {
                        setBlock(above, Material.SOUL_LANTERN);
                    }
                }

                // Replace leaves with dark variants
                for (int y = 0; y <= 15; y++) {
                    Block leafBlock = world.getBlockAt(
                            center.getBlockX() + x, surfaceY + y, center.getBlockZ() + z);
                    if (leafBlock.getType().name().contains("LEAVES") && random.nextDouble() < 0.6) {
                        setBlock(leafBlock, Material.COBWEB);
                    }
                }
            }
        }
    }

    /**
     * Apply soul sanctum terrain around a center location.
     */
    public void applySoulSanctum(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius)
                    continue;

                int surfaceY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                Block surface = world.getBlockAt(center.getBlockX() + x, surfaceY - 1, center.getBlockZ() + z);
                Block above = world.getBlockAt(center.getBlockX() + x, surfaceY, center.getBlockZ() + z);

                // Transform ground to soul terrain without using annoying Soul Sand/Soil
                if (isNaturalGround(surface.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.3) {
                        setBlock(surface, Material.POLISHED_BLACKSTONE_BRICKS);
                    } else if (roll < 0.6) {
                        setBlock(surface, Material.CHISELED_POLISHED_BLACKSTONE);
                    } else if (roll < 0.8) {
                        setBlock(surface, Material.CRYING_OBSIDIAN);
                    } else {
                        setBlock(surface, Material.GILDED_BLACKSTONE);
                    }
                }

                // Stone → obsidian / blackstone
                if (surface.getType() == Material.STONE || surface.getType() == Material.DEEPSLATE) {
                    double roll = random.nextDouble();
                    if (roll < 0.3) {
                        setBlock(surface, Material.CRYING_OBSIDIAN);
                    } else if (roll < 0.6) {
                        setBlock(surface, Material.OBSIDIAN);
                    } else {
                        setBlock(surface, Material.POLISHED_BLACKSTONE);
                    }
                }

                // Replace vegetation
                if (isVegetation(above.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.4) {
                        setBlock(above, Material.WITHER_ROSE);
                    } else if (roll < 0.8) {
                        setBlock(above, Material.DEAD_BUSH);
                    } else {
                        setBlock(above, Material.AIR);
                    }
                }

                // Soul props
                if (above.getType() == Material.AIR && random.nextDouble() < 0.05) {
                    Block below = above.getRelative(0, -1, 0);
                    if (below.getType().isSolid()) {
                        double roll = random.nextDouble();
                        if (roll < 0.4) {
                            setBlock(above, Material.SOUL_LANTERN);
                        } else if (roll < 0.7) {
                            setBlock(above, Material.SOUL_CAMPFIRE); // Provides particles without ground blocks
                        } else {
                            setBlock(above, Material.CANDLE);
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply corrupted forest terrain around a center location.
     * Moss, vines, mushrooms, overgrown feel.
     */
    public void applyCorruptedForest(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius)
                    continue;

                int surfaceY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                Block surface = world.getBlockAt(center.getBlockX() + x, surfaceY - 1, center.getBlockZ() + z);
                Block above = world.getBlockAt(center.getBlockX() + x, surfaceY, center.getBlockZ() + z);

                // Transform ground to mossy/overgrown
                if (isNaturalGround(surface.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.5) {
                        setBlock(surface, Material.MOSS_BLOCK);
                    } else if (roll < 0.7) {
                        setBlock(surface, Material.PODZOL);
                    } else {
                        setBlock(surface, Material.ROOTED_DIRT);
                    }
                }

                // Stone → mossy equivalents
                if (surface.getType() == Material.STONE) {
                    setBlock(surface, Material.MOSSY_COBBLESTONE);
                } else if (surface.getType() == Material.COBBLESTONE) {
                    setBlock(surface, Material.MOSSY_COBBLESTONE);
                } else if (surface.getType() == Material.STONE_BRICKS) {
                    setBlock(surface, Material.MOSSY_STONE_BRICKS);
                }

                // Replace vegetation with corrupted vegetation
                if (isVegetation(above.getType())) {
                    double roll = random.nextDouble();
                    if (roll < 0.3) {
                        setBlock(above, Material.RED_MUSHROOM);
                    } else if (roll < 0.5) {
                        setBlock(above, Material.BROWN_MUSHROOM);
                    } else if (roll < 0.7) {
                        setBlock(above, Material.MOSS_CARPET);
                    } else {
                        setBlock(above, Material.DEAD_BUSH);
                    }
                }

                // Add moss carpet and small dripleaf on empty spaces
                if (above.getType() == Material.AIR && random.nextDouble() < 0.08) {
                    Block below = above.getRelative(0, -1, 0);
                    if (below.getType().isSolid()) {
                        if (random.nextDouble() < 0.6) {
                            setBlock(above, Material.MOSS_CARPET);
                        } else {
                            setBlock(above, Material.RED_MUSHROOM);
                        }
                    }
                }

                // Hanging vines from leaves/wood
                for (int y = 1; y <= 10; y++) {
                    Block treeBlock = world.getBlockAt(
                            center.getBlockX() + x, surfaceY + y, center.getBlockZ() + z);
                    if (treeBlock.getType().name().contains("LEAVES") && random.nextDouble() < 0.15) {
                        Block belowLeaf = treeBlock.getRelative(0, -1, 0);
                        if (belowLeaf.getType() == Material.AIR) {
                            setBlock(belowLeaf, Material.VINE);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a material is natural ground (grass, dirt, etc.)
     */
    private boolean isNaturalGround(Material mat) {
        return mat == Material.GRASS_BLOCK
                || mat == Material.DIRT
                || mat == Material.COARSE_DIRT
                || mat == Material.ROOTED_DIRT
                || mat == Material.DIRT_PATH
                || mat == Material.SAND
                || mat == Material.MUD
                || mat == Material.PODZOL
                || mat == Material.MOSS_BLOCK;
    }

    /**
     * Check if a material is vegetation (flowers, grass, etc.)
     */
    private boolean isVegetation(Material mat) {
        String name = mat.name();
        return mat == Material.SHORT_GRASS
                || mat == Material.TALL_GRASS
                || mat == Material.FERN
                || mat == Material.LARGE_FERN
                || mat == Material.POPPY
                || mat == Material.DANDELION
                || mat == Material.BLUE_ORCHID
                || mat == Material.ALLIUM
                || mat == Material.AZURE_BLUET
                || mat == Material.OXEYE_DAISY
                || mat == Material.CORNFLOWER
                || mat == Material.LILY_OF_THE_VALLEY
                || mat == Material.SUNFLOWER
                || mat == Material.LILAC
                || mat == Material.ROSE_BUSH
                || mat == Material.PEONY
                || mat == Material.SWEET_BERRY_BUSH
                || name.contains("TULIP");
    }

    /**
     * Restore all modified blocks to their original state.
     * Should be called when the boss dies or the raid ends.
     */
    public void restore() {
        if (restored)
            return;
        restored = true;

        // Schedule restoration with slight delay for dramatic effect
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                Location loc = entry.getKey();
                BlockData data = entry.getValue();
                if (loc.getWorld() != null && loc.getChunk().isLoaded()) {
                    loc.getBlock().setBlockData(data, false);
                }
            }
            originalBlocks.clear();
        }, 60L); // 3 second delay after boss death for dramatic effect
    }

    /**
     * Restore blocks immediately without delay.
     */
    public void restoreImmediate() {
        if (restored)
            return;
        restored = true;

        for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
            Location loc = entry.getKey();
            BlockData data = entry.getValue();
            if (loc.getWorld() != null && loc.getChunk().isLoaded()) {
                loc.getBlock().setBlockData(data, false);
            }
        }
        originalBlocks.clear();
    }

    /**
     * Get the number of blocks currently tracked for restoration.
     */
    public int getTrackedBlockCount() {
        return originalBlocks.size();
    }
}
