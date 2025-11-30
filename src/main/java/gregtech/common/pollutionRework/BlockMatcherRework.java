package gregtech.common.pollutionRework;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.event.world.WorldEvent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import gregtech.GTMod;

public class BlockMatcherRework {

    private final Map<Class<?>, ColorOverrideTypeRework> whiteList = new ConcurrentHashMap<>();
    private final Set<Class<?>> blackList = ConcurrentHashMap.newKeySet();
    private volatile Map<Integer, ColorOverrideTypeRework> blockIDs = ImmutableMap.of();

    /**
     * Matches block by ID with thread-safe read access
     */
    public ColorOverrideTypeRework matchesID(int blockId) {
        return blockIDs.get(blockId);
    }

    /**
     * Matches block instance with thread-safe read access
     */
    public ColorOverrideTypeRework matchesID(Block block) {
        if (block == null) return null;
        Integer blockId = Block.blockRegistry.getIDForObject(block);
        return blockIDs.get(blockId);
    }

    /**
     * Updates class lists from configuration with validation
     */
    public void updateClassList(String[] cfg) {
        if (cfg == null) return;

        Map<Class<?>, ColorOverrideTypeRework> newWhiteList = new ConcurrentHashMap<>();
        Set<Class<?>> newBlackList = ConcurrentHashMap.newKeySet();

        for (String line : cfg) {
            if (line == null || line.trim()
                .isEmpty()) continue;

            String trimmedLine = line.trim();
            GTMod.GT_FML_LOGGER.debug("Processing block configuration: {}", trimmedLine);

            String[] parts = trimmedLine.split(":");
            if (parts.length == 0) continue;

            String className = parts[0].trim();

            // Handle blacklist entries (starting with '-')
            if (className.startsWith("-")) {
                processBlacklistEntry(className.substring(1), newBlackList);
            } else {
                processWhitelistEntry(className, parts, newWhiteList);
            }
        }

        // Atomic update of collections
        this.whiteList.clear();
        this.whiteList.putAll(newWhiteList);
        this.blackList.clear();
        this.blackList.addAll(newBlackList);

        scheduleBlockIDsUpdate();
    }

    private void processBlacklistEntry(String className, Set<Class<?>> targetBlackList) {
        try {
            Class<?> clazz = Class.forName(className);
            targetBlackList.add(clazz);
            GTMod.GT_FML_LOGGER.debug("Added to blacklist: {}", className);
        } catch (ClassNotFoundException e) {
            GTMod.GT_FML_LOGGER.warn("Class not found for blacklist: {}", className);
        } catch (Exception e) {
            GTMod.GT_FML_LOGGER.error("Error processing blacklist entry {}: {}", className, e.getMessage());
        }
    }

    private void processWhitelistEntry(String className, String[] parts,
        Map<Class<?>, ColorOverrideTypeRework> targetWhiteList) {
        if (parts.length < 2) {
            GTMod.GT_FML_LOGGER.error("Missing type for whitelist entry: {}", className);
            return;
        }

        try {
            ColorOverrideTypeRework type = ColorOverrideTypeRework.fromName(parts[1].trim());
            Class<?> clazz = Class.forName(className);
            targetWhiteList.put(clazz, type);
            GTMod.GT_FML_LOGGER.debug("Added to whitelist: {} with type {}", className, type);
        } catch (IllegalArgumentException e) {
            GTMod.GT_FML_LOGGER.error("Invalid type [{}] for class {}", parts[1], className);
        } catch (ClassNotFoundException e) {
            GTMod.GT_FML_LOGGER.warn("Class not found for whitelist: {}", className);
        } catch (Exception e) {
            GTMod.GT_FML_LOGGER.error("Error processing whitelist entry {}: {}", className, e.getMessage());
        }
    }

    /**
     * Schedules an asynchronous update of block IDs
     */
    public void scheduleBlockIDsUpdate() {
        new Thread(this::updateBlockIDs, "BlockMatcher-Update").start();
    }

    private void updateBlockIDs() {
        FMLControlledNamespacedRegistry<Block> blockRegistry = GameData.getBlockRegistry();
        Map<Integer, ColorOverrideTypeRework> newBlockIDs = new ConcurrentHashMap<>();

        for (Block block : blockRegistry.typeSafeIterable()) {
            if (block == null) continue;

            ColorOverrideTypeRework type = matchesClass(block);
            if (type != null) {
                Integer blockId = Block.blockRegistry.getIDForObject(block);
                newBlockIDs.put(blockId, type);
            }
        }

        // Atomic update of the immutable map
        this.blockIDs = ImmutableMap.copyOf(newBlockIDs);
        GTMod.GT_FML_LOGGER.info("Updated block IDs cache, now tracking {} blocks", newBlockIDs.size());
    }

    private ColorOverrideTypeRework matchesClass(Block block) {
        Class<?> blockClass = block.getClass();

        // Check blacklist first for early exit
        for (Class<?> blackClass : blackList) {
            if (blackClass.isAssignableFrom(blockClass)) {
                return null;
            }
        }

        // Check whitelist
        for (Map.Entry<Class<?>, ColorOverrideTypeRework> entry : whiteList.entrySet()) {
            if (entry.getKey()
                .isAssignableFrom(blockClass)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Returns an immutable view of current block mappings
     */
    public Map<Integer, ColorOverrideTypeRework> getBlockMappings() {
        return blockIDs;
    }

    /**
     * Returns current whitelist snapshot
     */
    public Map<Class<?>, ColorOverrideTypeRework> getWhitelistSnapshot() {
        return ImmutableMap.copyOf(whiteList);
    }

    /**
     * Returns current blacklist snapshot
     */
    public Set<Class<?>> getBlacklistSnapshot() {
        return ImmutableSet.copyOf(blackList);
    }

    /**
     * Caches block IDs on world load for fast lookup
     */
    @SubscribeEvent
    public void handleWorldLoad(WorldEvent.Load event) {
        if (event.world instanceof WorldClient) {
            scheduleBlockIDsUpdate();
        }
    }
}
