package gregtech.common.pollutionRework.Api;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;

import gregtech.common.pollutionRework.Data.PollutionStorage;
import gregtech.common.pollutionRework.Utils.BlockDamageManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PollutionType {

    // region Variables
    // Abstract variables
    private final String name;
    private final int spreadThreshold;
    private final int cycleLen;
    private final float naturalDecayRate;
    private final List<Potion> potionList;

    // DamageBLock variables
    private final int pollutionDamageStart;
    private final int maxAttempts;
    private final int vegetationAttemptsDivisor;
    private final BlockDamageManager blockDamageManager;
    private final List<Block> listBlockDestroy;

    // BiomeChange variables
    private final BiomeGenBase biome;

    private final PollutionStorage storage;
    private final Int2ObjectOpenHashMap<AbstractPollution> dimensionWisePollution = new Int2ObjectOpenHashMap<>(16);
    // endregion

    // region Constructors
    // spotless:off
    public PollutionType(String name,
                         int spreadThreshold,
                         int cycleLen,
                         float naturalDecayRate,
                         List<Potion> potionList,
                         int pollutionDamageStart,
                         int maxAttempts,
                         int vegetationAttemptsDivisor,
                         BlockDamageManager blockDamageManager,
                         List<Block> listBlockDestroy,
                         BiomeGenBase biome) {

        this.name = name;
        this.spreadThreshold = spreadThreshold;
        this.cycleLen = cycleLen;
        this.naturalDecayRate = naturalDecayRate;
        this.potionList = potionList;
        this.pollutionDamageStart = pollutionDamageStart;
        this.maxAttempts = maxAttempts;
        this.vegetationAttemptsDivisor = vegetationAttemptsDivisor;
        this.blockDamageManager = blockDamageManager;
        this.listBlockDestroy = listBlockDestroy;
        this.biome = biome;

        this.storage = new PollutionStorage(this);
    }
    // spotless:on
    // endregion

    // region Getters
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

    public int getPollutionDamageStart() {
        return pollutionDamageStart;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getVegetationAttemptsDivisor() {
        return vegetationAttemptsDivisor;
    }

    public BlockDamageManager getBlockDamageManager() {
        return blockDamageManager;
    }

    public List<Block> getListBlockDestroy() {
        return listBlockDestroy;
    }

    public BiomeGenBase getBiome() {
        return biome;
    }

    // endregion

    // region Others methods
    public AbstractPollution createPollutionInstance() {
        return new Pollution(this);
    }
    // endregion
}
