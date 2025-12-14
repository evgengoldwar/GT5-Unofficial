package gregtech.common.pollutionWork.ApiRenders;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.GTMod;
import gregtech.common.pollutionWork.Api.PollutionType;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SideOnly(Side.CLIENT)
public abstract class AbstractPollutionRenderer {

    protected final PollutionType pollutionType;
    protected final PollutionRenderPriority priority;
    protected final ClientPollutionMap pollutionMap = new ClientPollutionMap();

    protected final List<Class<? extends Block>> affectedBlocks = new ArrayList<>();
    protected final List<Block> affectedBlockInstances = new ArrayList<>();
    protected final Set<Integer> affectedBlockIds = new HashSet<>();

    protected boolean enableFog = true;
    protected boolean enableParticles = true;
    protected boolean enableColorEffects = true;

    private double currentFogIntensity = 0;
    private double lastUpdateTime = 0;
    private int currentPlayerPollution = 0;
    private final Map<ChunkCoordIntPair, Float> pollutionCache = new ConcurrentHashMap<>();
    private static final Map<Class<? extends Block>, Set<Integer>> classBlockCache = new HashMap<>();
    private static final Map<Block, Integer> blockIdCache = new HashMap<>();

    public AbstractPollutionRenderer(PollutionType pollutionType, PollutionRenderPriority priority) {
        this.pollutionType = pollutionType;
        this.priority = priority;
        initializeAffectedBlocks();
        buildAffectedBlockIds();
    }

    protected abstract void initializeAffectedBlocks();

    protected void addAffectedBlock(Class<? extends Block> blockClass) {
        affectedBlocks.add(blockClass);
    }

    protected void addAffectedBlock(Block block) {
        affectedBlockInstances.add(block);
    }

    protected void addAffectedBlockById(int blockId) {
        affectedBlockIds.add(blockId);
    }

    protected void buildAffectedBlockIds() {
        affectedBlockIds.clear();
        long startTime = System.currentTimeMillis();

        synchronized (classBlockCache) {
            for (Class<? extends Block> blockClass : affectedBlocks) {
                Set<Integer> cachedIds = classBlockCache.get(blockClass);

                if (cachedIds == null) {
                    cachedIds = new HashSet<>();

                    for (Object obj : Block.blockRegistry.getKeys()) {
                        Block block = (Block) Block.blockRegistry.getObject(obj);
                        if (block != null && blockClass.isInstance(block)) {
                            int id = getBlockId(block);
                            if (id >= 0) {
                                cachedIds.add(id);
                            }
                        }
                    }

                    classBlockCache.put(blockClass, cachedIds);
                    GTMod.GT_FML_LOGGER.trace("Cached {} IDs for class {}", cachedIds.size(), blockClass.getName());
                }

                affectedBlockIds.addAll(cachedIds);
            }
        }

        for (Block block : affectedBlockInstances) {
            if (block != null) {
                int id = getBlockId(block);
                if (id >= 0) {
                    affectedBlockIds.add(id);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        GTMod.GT_FML_LOGGER.debug("Built {} affected block IDs for {} in {}ms",
            affectedBlockIds.size(), pollutionType.getPollutionType(), duration);
    }

    private int getBlockId(Block block) {
        Integer cachedId = blockIdCache.get(block);
        if (cachedId == null) {
            cachedId = Block.getIdFromBlock(block);
            blockIdCache.put(block, cachedId);
        }
        return cachedId;
    }

    public boolean affectsBlock(Block block) {
        if (block == null) return false;
        int blockId = Block.getIdFromBlock(block);
        return affectedBlockIds.contains(blockId);
    }

    protected abstract int getColorEffectThreshold();
    protected abstract int calculateModifiedColor(int originalColor, int pollution, int x, int z);
    public abstract float[] getFogColor();
    public abstract int getFogStartPollution();
    public abstract int getFogMaxPollution();
    public abstract int getParticleStartPollution();
    public abstract int getParticleMaxPollution();
    public abstract EntityFX createParticle(World world, double x, double y, double z);

    public void processPollutionData(ChunkCoordIntPair chunk, int pollution) {
        if (pollution > 0) {
            pollutionMap.updateChunkPollution(chunk.chunkXPos, chunk.chunkZPos, pollution);
            pollutionCache.put(chunk, (float) pollution);
        }
    }

    public int getPollutionAt(int x, int z) {
        return pollutionMap.getInterpolatedPollution(x, z);
    }

    public int getPollutionAt(double x, double z) {
        return getPollutionAt((int) x, (int) z);
    }

    private void updateRenderState(double currentTime) {
        if (currentTime < lastUpdateTime) {
            lastUpdateTime -= 1.0;
        }

        float timeStep = (float) ((currentTime - lastUpdateTime) / 50.0);
        lastUpdateTime = currentTime;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        int x = (int) mc.thePlayer.posX;
        int z = (int) mc.thePlayer.posZ;
        currentPlayerPollution = getPollutionAt(x, z);

        float targetFogIntensity = (currentPlayerPollution - getFogStartPollution())
            / (float) Math.max(getFogMaxPollution() - getFogStartPollution(), 1);
        targetFogIntensity = MathHelper.clamp_float(targetFogIntensity, 0.0f, 1.0f);

        double intensityChange = targetFogIntensity - currentFogIntensity;
        if (Math.abs(intensityChange) > 0.001) {
            intensityChange = MathHelper.clamp_double(intensityChange, -0.5, 0.2);
            currentFogIntensity += timeStep * intensityChange;
        } else {
            currentFogIntensity = targetFogIntensity;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFogColorRender(EntityViewRenderEvent.FogColors event) {
        if (!enableFog || !shouldRender()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode) return;

        // Получаем позицию игрока
        int x = (int) mc.thePlayer.posX;
        int z = (int) mc.thePlayer.posZ;

        // Получаем цвет тумана через реестр для правильного выбора рендера
        PollutionRendererRegistry registry = PollutionRendererRegistry.getInstance();
        float[] fogColor = registry.getFogColor(
            new float[] { event.red, event.green, event.blue },
            x, z
        );

        event.red = fogColor[0];
        event.green = fogColor[1];
        event.blue = fogColor[2];
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogRender(EntityViewRenderEvent.RenderFogEvent event) {
        if (!enableFog || !shouldRender() || currentFogIntensity < 0.02) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.capabilities.isCreativeMode) return;

        if (event.fogMode == 0) {
            double linearFactor = 1.0 - currentFogIntensity / 0.02;
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
            GL11.glFogf(GL11.GL_FOG_START, (float) ((191 - 20) * 0.75F * linearFactor + 20));
            GL11.glFogf(GL11.GL_FOG_END, (float) (191 * (0.75F + linearFactor * 0.25F)));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            pollutionMap.markDirty();
            pollutionCache.clear();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world.isRemote) {
            classBlockCache.clear();
            blockIdCache.clear();

            GTMod.GT_FML_LOGGER.info("Rebuilt affected block IDs for {}: {} blocks",
                pollutionType.getPollutionType(), affectedBlockIds.size());
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            updateRenderState(event.renderTickTime);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!enableParticles || !shouldRender()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.thePlayer.capabilities.isCreativeMode) return;

        spawnPollutionParticles(mc.thePlayer);
    }

    private void spawnPollutionParticles(EntityPlayer player) {
        World world = player.worldObj;
        int pollution = getPollutionAt(player.posX, player.posZ);
        float pollutionIntensity = ((float) pollution - getParticleStartPollution())
            / (float) (getParticleMaxPollution() - getParticleStartPollution());

        if (pollutionIntensity < 0) return;
        pollutionIntensity = Math.min(pollutionIntensity, 1.0f);
        pollutionIntensity *= pollutionIntensity;

        int particleCount = Math.round(pollutionIntensity * 100);
        for (int i = 0; i < particleCount; i++) {
            double x = player.posX + (Math.random() - 0.5) * 32;
            double y = player.posY + (Math.random() - 0.5) * 16;
            double z = player.posZ + (Math.random() - 0.5) * 32;

            EntityFX particle = createParticle(world, x, y, z);
            if (particle != null) {
                Minecraft.getMinecraft().effectRenderer.addEffect(particle);
            }
        }
    }

    private boolean shouldRender() {
        return currentFogIntensity > 0.001 || currentPlayerPollution > getColorEffectThreshold();
    }

    public PollutionRenderPriority getPriority() {
        return priority;
    }

    public Set<Integer> getAffectedBlockIds() {
        return Collections.unmodifiableSet(affectedBlockIds);
    }

    public int getInterpolatedColorForBlock(Block block, int originalColor, int x, int z) {
        if (!enableColorEffects || !affectsBlock(block)) return originalColor;

        int pollution = getPollutionAt(x, z);
        if (pollution < getColorEffectThreshold()) return originalColor;

        return calculateModifiedColor(originalColor, pollution, x, z);
    }

    public double getCurrentFogIntensity() {
        return currentFogIntensity;
    }

    public float[] getInterpolatedFogColor(float[] originalColor, int x, int z) {
        if (!enableFog) return originalColor;

        int pollution = getPollutionAt(x, z);
        if (pollution < getFogStartPollution()) return originalColor;

        float[] fogColor = getFogColor();
        float pollutionFactor = (float) Math.min(getCurrentFogIntensity(), 1.0);
        float cleanFactor = 1.0f - pollutionFactor;

        float[] result = new float[3];
        result[0] = cleanFactor * originalColor[0] + pollutionFactor * fogColor[0];
        result[1] = cleanFactor * originalColor[1] + pollutionFactor * fogColor[1];
        result[2] = cleanFactor * originalColor[2] + pollutionFactor * fogColor[2];

        return result;
    }

    // Новые методы для получения данных загрязнения в разных точках
    public int getPollutionAtChunkCenter(ChunkCoordIntPair chunk) {
        return getPollutionAt(chunk.getCenterXPos(), chunk.getCenterZPosition());
    }

    public int getPollutionAtChunkCenter(int chunkX, int chunkZ) {
        int worldX = (chunkX << 4) + 8;
        int worldZ = (chunkZ << 4) + 8;
        return getPollutionAt(worldX, worldZ);
    }

    public float getPollutionNormalized(int x, int z) {
        int pollution = getPollutionAt(x, z);
        float normalized = (float) (pollution - getColorEffectThreshold())
            / (float) Math.max(getParticleMaxPollution() - getColorEffectThreshold(), 1);
        return MathHelper.clamp_float(normalized, 0.0f, 1.0f);
    }
}
