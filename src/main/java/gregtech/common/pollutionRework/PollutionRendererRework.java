package gregtech.common.pollutionRework;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.GTMod;
import gregtech.common.pollution.EntityFXPollution;

@SideOnly(Side.CLIENT)
public class PollutionRendererRework {

    private static final boolean DEBUG = false;

    // Pollution thresholds and effects
    private static final int PARTICLES_MAX_COUNT = 100;
    private static final int PARTICLES_POLLUTION_START = 400000;
    private static final int PARTICLES_POLLUTION_END = 3500000;
    private static final int FOG_MAX_DISTANCE = 192 - 1;

    private static final int FOG_START_POLLUTION = 400000;
    private static final int FOG_MAX_POLLUTION = 7000000;
    private static final double FOG_EXPONENTIAL_THRESHOLD = 0.02D;

    // Color modifications for pollution
    private static final float[] FOG_COLOR = { 0.3f, 0.25f, 0.1f };
    private static final short[] GRASS_COLOR_MOD = { 230, 180, 40 };
    private static final short[] LEAVES_COLOR_MOD = { 160, 80, 15 };
    private static final short[] LIQUID_COLOR_MOD = { 160, 200, 10 };
    private static final short[] FOLIAGE_COLOR_MOD = { 160, 80, 15 };

    private final ClientPollutionMap pollutionMap = new ClientPollutionMap();
    private int currentPlayerPollution = 0;
    private double currentFogIntensity = 0;
    private double lastUpdateTime = 0;

    public void processPollutionData(ChunkCoordIntPair chunk, int pollution) {
        pollutionMap.updateChunkPollution(chunk.chunkXPos, chunk.chunkZPos, pollution);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            pollutionMap.markDirty();
        }
    }

    private int applyPollutionColorEffect(int originalColor, int pollution, int lowThreshold, float highThreshold,
        short[] pollutionColor) {
        if (pollution < lowThreshold) return originalColor;

        int red = (originalColor >> 16) & 0xFF;
        int green = (originalColor >> 8) & 0xFF;
        int blue = originalColor & 0xFF;

        float pollutionFactor = Math.min((pollution - lowThreshold) / highThreshold, 1.0f);
        float inverseFactor = 1.0f - pollutionFactor;

        red = (int) (red * inverseFactor + pollutionFactor * pollutionColor[0]) & 0xFF;
        green = (int) (green * inverseFactor + pollutionFactor * pollutionColor[1]) & 0xFF;
        blue = (int) (blue * inverseFactor + pollutionFactor * pollutionColor[2]) & 0xFF;

        return (red << 16) | (green << 8) | blue;
    }

    public int getPollutionAdjustedGrassColor(int originalColor, int x, int z) {
        return applyPollutionColorEffect(originalColor, getPollutionAt(x, z) / 1000, 350, 600, GRASS_COLOR_MOD);
    }

    public int getPollutionAdjustedLeavesColor(int originalColor, int x, int z) {
        return applyPollutionColorEffect(originalColor, getPollutionAt(x, z) / 1000, 300, 500, LEAVES_COLOR_MOD);
    }

    public int getPollutionAdjustedLiquidColor(int originalColor, int x, int z) {
        return applyPollutionColorEffect(originalColor, getPollutionAt(x, z) / 1000, 300, 500, LIQUID_COLOR_MOD);
    }

    public int getPollutionAdjustedFoliageColor(int originalColor, int x, int z) {
        return applyPollutionColorEffect(originalColor, getPollutionAt(x, z) / 1000, 300, 500, FOLIAGE_COLOR_MOD);
    }

    public int getPollutionAt(int x, int z) {
        return pollutionMap.getInterpolatedPollution(x, z);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFogColorRender(EntityViewRenderEvent.FogColors event) {
        if (!DEBUG && Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode) return;
        if (event.block.getMaterial() == Material.water || event.block.getMaterial() == Material.lava) return;

        float pollutionFactor = (float) Math.min(currentFogIntensity, 1.0);
        float cleanFactor = 1.0f - pollutionFactor;

        event.red = cleanFactor * event.red + pollutionFactor * FOG_COLOR[0];
        event.green = cleanFactor * event.green + pollutionFactor * FOG_COLOR[1];
        event.blue = cleanFactor * event.blue + pollutionFactor * FOG_COLOR[2];
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogRender(EntityViewRenderEvent.RenderFogEvent event) {
        if ((!DEBUG && Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode)
            || currentFogIntensity < FOG_EXPONENTIAL_THRESHOLD) return;

        if (event.fogMode == 0) {
            double linearFactor = 1.0 - currentFogIntensity / FOG_EXPONENTIAL_THRESHOLD;
            GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
            GL11.glFogf(GL11.GL_FOG_START, (float) ((FOG_MAX_DISTANCE - 20) * 0.75F * linearFactor + 20));
            GL11.glFogf(GL11.GL_FOG_END, (float) (FOG_MAX_DISTANCE * (0.75F + linearFactor * 0.25F)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFogDensityRender(EntityViewRenderEvent.FogDensity event) {
        if (!DEBUG && Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode) return;

        if (event.entity.isPotionActive(Potion.blindness) || currentFogIntensity < FOG_EXPONENTIAL_THRESHOLD
            || event.block.getMaterial() == Material.water
            || event.block.getMaterial() == Material.lava) return;

        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP2);
        event.density = (float) Math.pow(currentFogIntensity - FOG_EXPONENTIAL_THRESHOLD, 0.75) / 5 + 0.01F;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;

        if (event.phase == TickEvent.Phase.START) {
            updateFogIntensity(event.renderTickTime);
        } else if (DEBUG) {
            renderDebugInfo();
        }
    }

    private void updateFogIntensity(double currentTime) {
        if (currentTime < lastUpdateTime) {
            lastUpdateTime -= 1.0;
        }

        float timeStep = (float) ((currentTime - lastUpdateTime) / 50.0);
        lastUpdateTime = currentTime;

        float targetFogIntensity = (currentPlayerPollution - FOG_START_POLLUTION) / (float) FOG_MAX_POLLUTION;
        targetFogIntensity = MathHelper.clamp_float(targetFogIntensity, 0.0f, 1.0f);

        double intensityChange = targetFogIntensity - currentFogIntensity;
        if (intensityChange != 0) {
            intensityChange = MathHelper.clamp_double(intensityChange, -0.5, 0.2);

            if (Math.abs(intensityChange) > 0.001) {
                currentFogIntensity += timeStep * intensityChange;
            } else {
                currentFogIntensity = targetFogIntensity;
            }
        }
    }

    private void renderDebugInfo() {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);

        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;

        mc.fontRenderer.drawStringWithShadow("Intensity: " + (currentFogIntensity * 10000), 0, 0, 0xFFFFFFFF);
        mc.fontRenderer.drawStringWithShadow(
            "Pollution: " + getPollutionAt((int) player.lastTickPosX, (int) player.lastTickPosZ),
            0,
            20,
            0xFFFFFFFF);
        mc.fontRenderer.drawStringWithShadow(
            "Density: "
                + ((float) (Math.pow(currentFogIntensity - FOG_EXPONENTIAL_THRESHOLD, 0.75) / 5 + 0.01F) * 10000),
            0,
            40,
            0xFFFFFFFF);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (!GTMod.proxy.mRenderDirtParticles) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null || (player.capabilities.isCreativeMode && !DEBUG)) return;

        spawnPollutionParticles(player);
    }

    private void spawnPollutionParticles(EntityClientPlayerMP player) {
        World world = player.worldObj;
        currentPlayerPollution = getPollutionAt((int) player.lastTickPosX, (int) player.lastTickPosZ);

        float pollutionIntensity = ((float) currentPlayerPollution - PARTICLES_POLLUTION_START)
            / PARTICLES_POLLUTION_END;
        if (pollutionIntensity < 0) return;

        pollutionIntensity = Math.min(pollutionIntensity, 1.0f);
        pollutionIntensity *= pollutionIntensity; // Quadratic falloff

        int particleCount = Math.round(pollutionIntensity * PARTICLES_MAX_COUNT);
        int playerX = MathHelper.floor_double(player.posX);
        int playerY = MathHelper.floor_double(player.posY);
        int playerZ = MathHelper.floor_double(player.posZ);

        for (int i = 0; i < particleCount; i++) {
            int particleX = playerX + world.rand.nextInt(16) - world.rand.nextInt(16);
            int particleY = playerY + world.rand.nextInt(16) - world.rand.nextInt(16);
            int particleZ = playerZ + world.rand.nextInt(16) - world.rand.nextInt(16);

            Block block = world.getBlock(particleX, particleY, particleZ);
            if (block.getMaterial() == Material.air) {
                EntityFX pollutionParticle = new EntityFXPollution(
                    world,
                    particleX + world.rand.nextFloat(),
                    particleY + world.rand.nextFloat(),
                    particleZ + world.rand.nextFloat());
                Minecraft.getMinecraft().effectRenderer.addEffect(pollutionParticle);
            }
        }
    }
}
