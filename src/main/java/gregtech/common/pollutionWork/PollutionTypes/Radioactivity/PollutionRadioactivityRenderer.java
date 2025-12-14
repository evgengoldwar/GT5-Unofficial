package gregtech.common.pollutionWork.PollutionTypes.Radioactivity;

import gregtech.common.pollution.EntityFXPollution;
import gregtech.common.pollutionWork.ApiRenders.AbstractPollutionRenderer;
import gregtech.common.pollutionWork.ApiRenders.PollutionRenderPriority;
import gregtech.common.pollutionWork.Api.PollutionType;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class PollutionRadioactivityRenderer extends AbstractPollutionRenderer {
    public PollutionRadioactivityRenderer(PollutionType pollutionType) {
        super(pollutionType, PollutionRenderPriority.HIGH);
    }

    @Override
    protected void initializeAffectedBlocks() {
        // Радиоактивность влияет на большее количество блоков
        addAffectedBlock(BlockGrass.class);
        addAffectedBlock(BlockLeaves.class);
        addAffectedBlock(Blocks.flowing_water);
        addAffectedBlock(Blocks.water);
//        addAffectedBlock(Block.class); // Все блоки

    }

    @Override
    protected int getColorEffectThreshold() {
        return 200000; // Более низкий порог для радиоактивности
    }

    @Override
    protected int calculateModifiedColor(int originalColor, int pollution, int x, int z) {
        float pollutionFactor = Math.min((pollution - getColorEffectThreshold()) / 1500000f, 1.0f);
        float inverseFactor = 1.0f - pollutionFactor;

        int red = (originalColor >> 16) & 0xFF;
        int green = (originalColor >> 8) & 0xFF;
        int blue = originalColor & 0xFF;

        // Черно-зеленый цвет радиации (30, 255, 0)
        red = (int) (red * inverseFactor + pollutionFactor * 30) & 0xFF;
        green = (int) (green * inverseFactor + pollutionFactor * 255) & 0xFF;
        blue = (int) (blue * inverseFactor + pollutionFactor * 0) & 0xFF;

        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public float[] getFogColor() {
        return new float[] { 0.1f, 0.8f, 0.1f }; // Зеленый туман
    }

    @Override
    public int getFogStartPollution() {
        return 300000;
    }

    @Override
    public int getFogMaxPollution() {
        return 2000000;
    }

    @Override
    public int getParticleStartPollution() {
        return 250000;
    }

    @Override
    public int getParticleMaxPollution() {
        return 1500000;
    }

    @Override
    public EntityFX createParticle(World world, double x, double y, double z) {
        return new EntityFXPollution(world, x, y, z);
    }
}
