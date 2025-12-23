package gregtech.common.pollutionRework.Api;

import java.util.List;

import net.minecraft.potion.Potion;

public class Pollution extends AbstractPollution {

    private final PollutionType pollutionType;

    public Pollution(PollutionType pollutionType) {
        super(pollutionType);
        this.pollutionType = pollutionType;
    }

    @Override
    protected int getCycleLen() {
        return pollutionType.getCycleLen();
    }

    @Override
    protected int getSpreadThreshold() {
        return pollutionType.getSpreadThreshold();
    }

    @Override
    protected float getNaturalDecayRate() {
        return pollutionType.getNaturalDecayRate();
    }

    @Override
    protected List<Potion> getPotionList() {
        return pollutionType.getPotionList();
    }
}
