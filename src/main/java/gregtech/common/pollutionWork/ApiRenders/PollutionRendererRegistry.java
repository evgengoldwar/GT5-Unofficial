package gregtech.common.pollutionWork.ApiRenders;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.GTMod;
import gregtech.common.pollutionWork.Api.PollutionType;
import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public class PollutionRendererRegistry {

    private static final PollutionRendererRegistry INSTANCE = new PollutionRendererRegistry();
    private final Map<PollutionType, AbstractPollutionRenderer> renderers = new ConcurrentHashMap<>();
    private final Map<Integer, AbstractPollutionRenderer> blockRenderers = new ConcurrentHashMap<>();
    private boolean initialized = false;

    private final Map<ChunkCoordIntPair, DominantRendererInfo> dominantRendererCache = new ConcurrentHashMap<>();
    private static final long CACHE_TIMEOUT = 10000;

    private static class DominantRendererInfo {
        final PollutionType dominantType;
        final float dominantFactor;
        final Map<PollutionType, Float> pollutionLevels;
        final long timestamp;

        DominantRendererInfo(PollutionType dominantType, float dominantFactor,
                             Map<PollutionType, Float> pollutionLevels) {
            this.dominantType = dominantType;
            this.dominantFactor = dominantFactor;
            this.pollutionLevels = pollutionLevels;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static PollutionRendererRegistry getInstance() {
        return INSTANCE;
    }

    public void registerRenderer(PollutionType pollutionType, AbstractPollutionRenderer renderer) {
        if (renderer == null) return;

        renderers.put(pollutionType, renderer);

        MinecraftForge.EVENT_BUS.register(renderer);
        FMLCommonHandler.instance().bus().register(renderer);

        updateBlockMappings();
    }

    private void updateBlockMappings() {
        synchronized (blockRenderers) {
            blockRenderers.clear();

            List<AbstractPollutionRenderer> sortedRenderers = new ArrayList<>(renderers.values());
            sortedRenderers.sort((r1, r2) -> Integer.compare(
                r2.getPriority().getPriorityLevel(),
                r1.getPriority().getPriorityLevel()
            ));

            for (AbstractPollutionRenderer renderer : sortedRenderers) {
                for (Integer blockId : renderer.getAffectedBlockIds()) {
                    if (!blockRenderers.containsKey(blockId)) {
                        blockRenderers.put(blockId, renderer);
                    }
                }
            }
        }
    }

    public AbstractPollutionRenderer getRendererForBlock(Block block) {
        if (block == null) return null;

        // Проверка на поток Sodium
        if (Thread.currentThread().getName().toLowerCase().contains("sodium") ||
            Thread.currentThread().getName().toLowerCase().contains("rubidium")) {
            return null;
        }

        int blockId = Block.getIdFromBlock(block);
        synchronized (blockRenderers) {
            return blockRenderers.get(blockId);
        }
    }

    public void processPollutionData(PollutionType type, ChunkCoordIntPair chunk, int pollution) {
        AbstractPollutionRenderer renderer = renderers.get(type);
        if (renderer == null) return;

        renderer.processPollutionData(chunk, pollution);

        // Очищаем кэш для этого чанка
        dominantRendererCache.remove(chunk);
    }

    private DominantRendererInfo calculateDominantRendererForChunk(ChunkCoordIntPair chunk) {
        // Проверка на поток Sodium
        if (Thread.currentThread().getName().toLowerCase().contains("sodium") ||
            Thread.currentThread().getName().toLowerCase().contains("rubidium")) {
            return null;
        }

        Map<PollutionType, Float> pollutionLevels = new HashMap<>();

        for (Map.Entry<PollutionType, AbstractPollutionRenderer> entry : renderers.entrySet()) {
            PollutionType type = entry.getKey();
            AbstractPollutionRenderer renderer = entry.getValue();

            try {
                int pollution = renderer.getPollutionAtChunkCenter(chunk);
                pollutionLevels.put(type, (float) pollution);
            } catch (Exception e) {
                GTMod.GT_FML_LOGGER.error("Error getting pollution for {} in chunk {}: {}",
                    type, chunk, e.getMessage());
                pollutionLevels.put(type, 0f);
            }
        }

        // Фильтруем активные загрязнения
        Map<PollutionType, Float> activePollutions = new HashMap<>();
        for (Map.Entry<PollutionType, Float> entry : pollutionLevels.entrySet()) {
            PollutionType type = entry.getKey();
            float pollution = entry.getValue();
            AbstractPollutionRenderer renderer = renderers.get(type);

            if (renderer != null && pollution >= renderer.getColorEffectThreshold()) {
                activePollutions.put(type, pollution);
            }
        }

        if (activePollutions.isEmpty()) {
            return null;
        }

        // Если только один активный тип
        if (activePollutions.size() == 1) {
            PollutionType onlyType = activePollutions.keySet().iterator().next();
            float pollution = activePollutions.get(onlyType);
            AbstractPollutionRenderer renderer = renderers.get(onlyType);

            float normalized = MathHelper.clamp_float(
                (pollution - renderer.getColorEffectThreshold()) /
                    (renderer.getParticleMaxPollution() - renderer.getColorEffectThreshold()),
                0, 1
            );

            return new DominantRendererInfo(onlyType, normalized, pollutionLevels);
        }

        // Выбор среди нескольких типов
        PollutionType dominantType = null;
        float highestScore = -1.0f;

        for (Map.Entry<PollutionType, Float> entry : activePollutions.entrySet()) {
            PollutionType type = entry.getKey();
            float pollution = entry.getValue();
            AbstractPollutionRenderer renderer = renderers.get(type);

            float normalizedPollution = MathHelper.clamp_float(
                (pollution - renderer.getColorEffectThreshold()) /
                    (renderer.getParticleMaxPollution() - renderer.getColorEffectThreshold()),
                0, 1
            );

            float priorityMultiplier = 1.0f + (renderer.getPriority().getPriorityLevel() * 0.5f);
            float score = normalizedPollution * priorityMultiplier;

            if (score > highestScore) {
                highestScore = score;
                dominantType = type;
            }
        }

        if (dominantType == null) {
            return null;
        }

        // Вычисляем фактор доминирования
        float dominantPollution = activePollutions.get(dominantType);
        AbstractPollutionRenderer dominantRenderer = renderers.get(dominantType);
        float dominantFactor = MathHelper.clamp_float(
            (dominantPollution - dominantRenderer.getColorEffectThreshold()) /
                (dominantRenderer.getParticleMaxPollution() - dominantRenderer.getColorEffectThreshold()),
            0, 1
        );

        return new DominantRendererInfo(dominantType, dominantFactor, pollutionLevels);
    }

    private DominantRendererInfo getDominantRendererInfo(ChunkCoordIntPair chunk) {
        // Проверка на поток Sodium
        if (Thread.currentThread().getName().toLowerCase().contains("sodium") ||
            Thread.currentThread().getName().toLowerCase().contains("rubidium")) {
            return null;
        }

        DominantRendererInfo info = dominantRendererCache.get(chunk);

        if (info != null && System.currentTimeMillis() - info.timestamp < CACHE_TIMEOUT) {
            return info;
        }

        info = calculateDominantRendererForChunk(chunk);
        if (info != null) {
            dominantRendererCache.put(chunk, info);
        } else {
            dominantRendererCache.remove(chunk);
        }

        return info;
    }

    public AbstractPollutionRenderer getDominantRendererForChunk(ChunkCoordIntPair chunk) {
        DominantRendererInfo info = getDominantRendererInfo(chunk);
        return info != null ? renderers.get(info.dominantType) : null;
    }

    public AbstractPollutionRenderer getDominantRendererForPosition(int worldX, int worldZ) {
        // Проверка на поток Sodium
        if (Thread.currentThread().getName().toLowerCase().contains("sodium") ||
            Thread.currentThread().getName().toLowerCase().contains("rubidium")) {
            return null;
        }

        int chunkX = worldX >> 4;
        int chunkZ = worldZ >> 4;

        float localX = (worldX & 15) / 16.0f;
        float localZ = (worldZ & 15) / 16.0f;

        ChunkCoordIntPair[] chunks = {
            new ChunkCoordIntPair(chunkX, chunkZ),
            new ChunkCoordIntPair(chunkX + 1, chunkZ),
            new ChunkCoordIntPair(chunkX, chunkZ + 1),
            new ChunkCoordIntPair(chunkX + 1, chunkZ + 1)
        };

        AbstractPollutionRenderer[] renderersAtCorners = new AbstractPollutionRenderer[4];
        DominantRendererInfo[] infosAtCorners = new DominantRendererInfo[4];

        for (int i = 0; i < 4; i++) {
            infosAtCorners[i] = getDominantRendererInfo(chunks[i]);
            renderersAtCorners[i] = infosAtCorners[i] != null ?
                renderers.get(infosAtCorners[i].dominantType) : null;
        }

        // Взвешенный выбор
        Map<AbstractPollutionRenderer, Float> rendererWeights = new HashMap<>();

        float[] cornerWeights = {
            (1 - localX) * (1 - localZ),
            localX * (1 - localZ),
            (1 - localX) * localZ,
            localX * localZ
        };

        for (int i = 0; i < 4; i++) {
            if (renderersAtCorners[i] != null && infosAtCorners[i] != null) {
                float dominanceWeight = infosAtCorners[i].dominantFactor;
                float weight = cornerWeights[i] * (0.5f + dominanceWeight * 0.5f);

                float currentWeight = rendererWeights.getOrDefault(renderersAtCorners[i], 0.0f);
                rendererWeights.put(renderersAtCorners[i], currentWeight + weight);
            }
        }

        AbstractPollutionRenderer dominantRenderer = null;
        float maxWeight = 0.0f;

        for (Map.Entry<AbstractPollutionRenderer, Float> entry : rendererWeights.entrySet()) {
            if (entry.getValue() > maxWeight) {
                maxWeight = entry.getValue();
                dominantRenderer = entry.getKey();
            }
        }

        return dominantRenderer;
    }

    public int getColorForBlock(Block block, int originalColor, int x, int z) {
        // Ключевое исправление: проверка на поток Sodium
        if (Thread.currentThread().getName().toLowerCase().contains("sodium") ||
            Thread.currentThread().getName().toLowerCase().contains("rubidium") ||
            Thread.currentThread().getName().toLowerCase().contains("chunk")) {
            return originalColor; // В потоке Sodium возвращаем исходный цвет
        }

        if (block == null || renderers.isEmpty()) return originalColor;

        AbstractPollutionRenderer blockRenderer = getRendererForBlock(block);
        AbstractPollutionRenderer dominantRenderer = getDominantRendererForPosition(x, z);

        if (blockRenderer == null && dominantRenderer == null) {
            return originalColor;
        }

        // Определяем рендер для использования
        AbstractPollutionRenderer rendererToUse = blockRenderer;

        if (dominantRenderer != null && dominantRenderer.affectsBlock(block)) {
            rendererToUse = dominantRenderer;
        } else if (blockRenderer == null) {
            return originalColor;
        }

        try {
            return rendererToUse.getInterpolatedColorForBlock(block, originalColor, x, z);
        } catch (Exception e) {
            GTMod.GT_FML_LOGGER.error("Error getting color for block {} at ({}, {}): {}",
                block.getUnlocalizedName(), x, z, e.getMessage());
            return originalColor;
        }
    }

    private float calculateRendererBlendFactor(int x, int z,
                                               AbstractPollutionRenderer renderer1,
                                               AbstractPollutionRenderer renderer2) {
        int pollution1 = renderer1.getPollutionAt(x, z);
        int pollution2 = renderer2.getPollutionAt(x, z);

        int threshold1 = renderer1.getColorEffectThreshold();
        int threshold2 = renderer2.getColorEffectThreshold();

        float effective1 = Math.max(0, pollution1 - threshold1);
        float effective2 = Math.max(0, pollution2 - threshold2);

        if (effective1 + effective2 < 0.01f) {
            return 0.0f;
        }

        return effective2 / (effective1 + effective2);
    }

    private int blendColors(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        float invFactor = 1.0f - factor;

        int r = (int)(r1 * invFactor + r2 * factor);
        int g = (int)(g1 * invFactor + g2 * factor);
        int b = (int)(b1 * invFactor + b2 * factor);

        return (r << 16) | (g << 8) | b;
    }

    public float[] getFogColor(float[] originalFogColor, int x, int z) {
        AbstractPollutionRenderer dominantRenderer = getDominantRendererForPosition(x, z);
        if (dominantRenderer == null) return originalFogColor;

        int pollution = dominantRenderer.getPollutionAt(x, z);
        if (pollution < dominantRenderer.getFogStartPollution()) {
            return originalFogColor;
        }

        // Смешиваем цвета тумана
        List<AbstractPollutionRenderer> activeRenderers = new ArrayList<>();
        List<Float> intensities = new ArrayList<>();

        for (AbstractPollutionRenderer renderer : renderers.values()) {
            int rendererPollution = renderer.getPollutionAt(x, z);
            if (rendererPollution >= renderer.getFogStartPollution()) {
                activeRenderers.add(renderer);
                float intensity = MathHelper.clamp_float(
                    (float)(rendererPollution - renderer.getFogStartPollution()) /
                        (renderer.getFogMaxPollution() - renderer.getFogStartPollution()),
                    0, 1
                );
                intensities.add(intensity);
            }
        }

        if (activeRenderers.isEmpty()) {
            return originalFogColor;
        }

        float[] finalColor = new float[3];
        float totalWeight = 0.0f;

        for (int i = 0; i < activeRenderers.size(); i++) {
            AbstractPollutionRenderer renderer = activeRenderers.get(i);
            float intensity = intensities.get(i);

            float weight = intensity * (1.0f + renderer.getPriority().getPriorityLevel() * 0.2f);

            float[] fogColor = renderer.getFogColor();
            for (int j = 0; j < 3; j++) {
                finalColor[j] += fogColor[j] * weight;
            }
            totalWeight += weight;
        }

        if (totalWeight > 0) {
            for (int i = 0; i < 3; i++) {
                finalColor[i] /= totalWeight;
            }
        }

        float maxIntensity = 0.0f;
        for (Float intensity : intensities) {
            if (intensity > maxIntensity) {
                maxIntensity = intensity;
            }
        }

        float cleanFactor = 1.0f - maxIntensity;
        float[] result = new float[3];

        for (int i = 0; i < 3; i++) {
            result[i] = originalFogColor[i] * cleanFactor + finalColor[i] * maxIntensity;
        }

        return result;
    }

    public void cleanup() {
        for (AbstractPollutionRenderer renderer : renderers.values()) {
            MinecraftForge.EVENT_BUS.unregister(renderer);
            FMLCommonHandler.instance().bus().unregister(renderer);
        }
        renderers.clear();
        blockRenderers.clear();
        dominantRendererCache.clear();
        initialized = false;
    }

    public void initialize() {
        if (initialized) return;

        for (PollutionType type : PollutionType.values()) {
            AbstractPollutionRenderer renderer = type.createRenderer();
            if (renderer != null) {
                registerRenderer(type, renderer);
            }
        }

        initialized = true;
    }
}
