package gregtech.common.pollutionWork.PollutionTypes.Smog;

import gregtech.common.pollution.EntityFXPollution;
import gregtech.common.pollutionWork.Api.PollutionType;
import gregtech.common.pollutionWork.ApiRenders.AbstractPollutionRenderer;
import gregtech.common.pollutionWork.ApiRenders.PollutionRenderPriority;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class PollutionSmogRenderer extends AbstractPollutionRenderer {

    public PollutionSmogRenderer(PollutionType pollutionType) {
        super(pollutionType, PollutionRenderPriority.MEDIUM);
    }

    @Override
    protected void initializeAffectedBlocks() {
        // Добавляем блоки по классам
        addAffectedBlock(BlockGrass.class);
        addAffectedBlock(BlockLeaves.class);
        addAffectedBlock(BlockLiquid.class);

        // Добавляем конкретные блоки
        addAffectedBlock(Blocks.tallgrass);
        addAffectedBlock(Blocks.double_plant);
        addAffectedBlock(Blocks.vine);
        addAffectedBlock(Blocks.water);
        addAffectedBlock(Blocks.flowing_water);
    }

    @Override
    protected int getColorEffectThreshold() {
        return 300000; // Порог для эффекта цвета
    }

    @Override
    protected int calculateModifiedColor(int originalColor, int pollution, int x, int z) {
        float pollutionFactor = Math.min((pollution - getColorEffectThreshold()) / 2000000f, 1.0f);
        float inverseFactor = 1.0f - pollutionFactor;

        int red = (originalColor >> 16) & 0xFF;
        int green = (originalColor >> 8) & 0xFF;
        int blue = originalColor & 0xFF;

        // Оранжевый цвет смога (255, 165, 0)
        red = (int) (red * inverseFactor + pollutionFactor * 255) & 0xFF;
        green = (int) (green * inverseFactor + pollutionFactor * 165) & 0xFF;
        blue = (int) (blue * inverseFactor + pollutionFactor * 0) & 0xFF;

        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public float[] getFogColor() {
        return new float[] { 0.8f, 0.5f, 0.1f }; // Оранжевый туман
    }

    @Override
    public int getFogStartPollution() {
        return 500000;
    }

    @Override
    public int getFogMaxPollution() {
        return 3000000;
    }

    @Override
    public int getParticleStartPollution() {
        return 400000;
    }

    @Override
    public int getParticleMaxPollution() {
        return 2000000;
    }

    @Override
    public EntityFX createParticle(World world, double x, double y, double z) {
        return new EntityFXPollution(world, x, y, z);
    }
}
