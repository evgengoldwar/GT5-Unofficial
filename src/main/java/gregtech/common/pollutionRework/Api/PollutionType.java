package gregtech.common.pollutionRework.Api;

import java.util.Collections;
import java.util.List;

import net.minecraft.potion.Potion;

import gregtech.common.pollutionRework.Data.PollutionStorage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PollutionType {

    private final String name;
    private final int spreadThreshold;
    private final int cycleLen;
    private final float naturalDecayRate;
    private final List<Potion> potionList;

    private final PollutionStorage storage;
    private final Int2ObjectOpenHashMap<AbstractPollution> dimensionWisePollution = new Int2ObjectOpenHashMap<>(16);

    public PollutionType(String name, int spreadThreshold, int cycleLen, float naturalDecayRate,
        List<Potion> potionList) {
        this.name = name;
        this.spreadThreshold = spreadThreshold;
        this.cycleLen = cycleLen;
        this.naturalDecayRate = naturalDecayRate;
        this.potionList = potionList;

        this.storage = new PollutionStorage(this);

    }

    public String getName() {
        return name;
    }

    public int getSpreadThreshold() {
        return spreadThreshold;
    }

    public int getCycleLen() {
        return cycleLen;
    }

    public float getNaturalDecayRate() {
        return naturalDecayRate;
    }

    public Int2ObjectOpenHashMap<AbstractPollution> getDimensionWisePollution() {
        return dimensionWisePollution;
    }

    public PollutionStorage getStorage() {
        return storage;
    }

    public List<Potion> getPotionList() {
        return Collections.unmodifiableList(potionList);
    }

    public AbstractPollution createPollutionInstance() {
        return new Pollution(this);
    }
}
