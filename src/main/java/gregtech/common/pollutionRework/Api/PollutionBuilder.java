package gregtech.common.pollutionRework.Api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.potion.Potion;

public class PollutionBuilder {

    // region Variables
    private final String name;
    private int cycleLen = 1_000;
    private int spreadThreshold = 100_000;
    private float naturalDecayRate = 0.9945f;
    private final List<Potion> potionList = new ArrayList<>();
    // endregion

    // region Constructor
    private PollutionBuilder(String name) {
        this.name = name;
    }
    // endregion

    // region Builder Methods
    public static PollutionBuilder builder(String name) {
        return new PollutionBuilder(name);
    }

    public PollutionBuilder addPotion(Potion potion) {
        this.potionList.add(potion);
        return this;
    }

    public PollutionBuilder addPotion(List<Potion> potionList) {
        this.potionList.addAll(potionList);
        return this;
    }

    public PollutionBuilder withSpreadThreshold(int spreadThreshold) {
        this.spreadThreshold = spreadThreshold;
        return this;
    }

    public PollutionBuilder withNaturalDecayRate(float naturalDecayRate) {
        this.naturalDecayRate = naturalDecayRate;
        return this;
    }

    public PollutionBuilder withCycleLen(int cycleLen) {
        this.cycleLen = cycleLen;
        return this;
    }

    public PollutionType build() {
        return new PollutionType(name, spreadThreshold, cycleLen, naturalDecayRate, potionList);
    }
    // endregion
}
