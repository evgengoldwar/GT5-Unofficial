package gregtech.common.pollutionRework;

import java.util.Arrays;
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

    public ColorOverrideTypeRework matchesID(int blockId) {
        return blockIDs.get(blockId);
    }

    public ColorOverrideTypeRework matchesID(Block block) {
        if (block == null) return null;
        Integer blockId = Block.blockRegistry.getIDForObject(block);
        return blockIDs.get(blockId);
    }

    public void updateClassList(String[] cfg) {
        if (cfg == null) return;

        Map<Class<?>, ColorOverrideTypeRework> newWhiteList = new ConcurrentHashMap<>();
        Set<Class<?>> newBlackList = ConcurrentHashMap.newKeySet();

        Arrays.stream(cfg)
            .filter(
                line -> line != null && !line.trim()
                    .isEmpty())
            .map(String::trim)
            .forEach(line -> processConfigurationLine(line, newWhiteList, newBlackList));

        this.whiteList.clear();
        this.whiteList.putAll(newWhiteList);
        this.blackList.clear();
        this.blackList.addAll(newBlackList);

        scheduleBlockIDsUpdate();
    }

    private void processConfigurationLine(String line, Map<Class<?>, ColorOverrideTypeRework> whiteList,
        Set<Class<?>> blackList) {
        if (line.startsWith("-")) {
            processBlacklistEntry(line.substring(1), blackList);
        } else {
            processWhitelistEntry(line, whiteList);
        }
    }

    private void processBlacklistEntry(String className, Set<Class<?>> targetBlackList) {
        Class<?> clazz = loadClassSafely(className);
        if (clazz != null) {
            targetBlackList.add(clazz);
            GTMod.GT_FML_LOGGER.debug("Added to blacklist: {}", className);
        }
    }

    private void processWhitelistEntry(String line, Map<Class<?>, ColorOverrideTypeRework> targetWhiteList) {
        String[] parts = line.split(":");
        if (parts.length < 2) {
            GTMod.GT_FML_LOGGER.error("Missing type for whitelist entry: {}", parts[0]);
            return;
        }

        String className = parts[0].trim();
        String typeName = parts[1].trim();

        Class<?> clazz = loadClassSafely(className);
        if (clazz != null) {
            try {
                ColorOverrideTypeRework type = ColorOverrideTypeRework.fromName(typeName);
                targetWhiteList.put(clazz, type);
                GTMod.GT_FML_LOGGER.debug("Added to whitelist: {} with type {}", className, type);
            } catch (IllegalArgumentException e) {
                GTMod.GT_FML_LOGGER.error("Invalid type [{}] for class {}", typeName, className);
            }
        }
    }

    private Class<?> loadClassSafely(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            GTMod.GT_FML_LOGGER.warn("Class not found: {}", className);
        } catch (Exception e) {
            GTMod.GT_FML_LOGGER.error("Error processing class {}: {}", className, e.getMessage());
        }
        return null;
    }

    public void scheduleBlockIDsUpdate() {
        new Thread(this::updateBlockIDs, "BlockMatcher-Update").start();
    }

    private void updateBlockIDs() {
        FMLControlledNamespacedRegistry<Block> blockRegistry = GameData.getBlockRegistry();
        Map<Integer, ColorOverrideTypeRework> newBlockIDs = new ConcurrentHashMap<>();

        for (Block block : blockRegistry.typeSafeIterable()) {
            if (block == null) continue;

            Integer blockId = Block.blockRegistry.getIDForObject(block);
            ColorOverrideTypeRework type = matchesClass(block);

            if (type != null) {
                newBlockIDs.put(blockId, type);
            }
        }

        this.blockIDs = ImmutableMap.copyOf(newBlockIDs);
        GTMod.GT_FML_LOGGER.info("Updated block IDs cache, now tracking {} blocks", newBlockIDs.size());
    }

    private ColorOverrideTypeRework matchesClass(Block block) {
        Class<?> blockClass = block.getClass();

        boolean isBlacklisted = blackList.stream()
            .anyMatch(blackClass -> blackClass.isAssignableFrom(blockClass));

        if (isBlacklisted) {
            return null;
        }

        return whiteList.entrySet()
            .stream()
            .filter(
                entry -> entry.getKey()
                    .isAssignableFrom(blockClass))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    public Map<Integer, ColorOverrideTypeRework> getBlockMappings() {
        return blockIDs;
    }

    public Map<Class<?>, ColorOverrideTypeRework> getWhitelistSnapshot() {
        return ImmutableMap.copyOf(whiteList);
    }

    public Set<Class<?>> getBlacklistSnapshot() {
        return ImmutableSet.copyOf(blackList);
    }

    @SubscribeEvent
    public void handleWorldLoad(WorldEvent.Load event) {
        if (event.world instanceof WorldClient) {
            scheduleBlockIDsUpdate();
        }
    }
}
