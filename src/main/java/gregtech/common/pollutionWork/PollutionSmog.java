package gregtech.common.pollutionWork;

import net.minecraft.world.World;

import gregtech.common.pollutionWork.api.AbstractPollution;
import gregtech.common.pollutionWork.api.PollutionType;

public class PollutionSmog extends AbstractPollution {

    public PollutionSmog(World world, PollutionType type) {
        super(world, type);
    }

    @Override
    protected int getCycleLen() {
        return 100;
    }

    @Override
    protected int getSpreadThreshold() {
        return 100_000;
    }

    @Override
    protected float getNaturalDecayRate() {
        return 0.9945f;
    }
}
