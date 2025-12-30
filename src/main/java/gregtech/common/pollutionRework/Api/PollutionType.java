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
    // Base variables
    private final String name;
    private final int operationCycle;

    // Spread
    private final int spreadThreshold;
    private final float naturalDecayRate;

    // Effect
    private final List<Potion> potionList;
    private final int pollutionEffectThreshold;

    // DamageBLock variables
    private final int pollutionDamageThreshold;
    private final int maxAttemptsBlockReplace;
    private final int pollutionThresholdPerAttempt;
    private final List<BlockDamageManager> blockDamageManagerList;
    private final List<Block> blockDestroyList;

    // BiomeChange variables
    private final BiomeGenBase biome;
    private final int biomeChangeThreshold;

    private final PollutionStorage storage;
    private final Int2ObjectOpenHashMap<Pollution> dimensionWisePollution = new Int2ObjectOpenHashMap<>(16);
    // endregion

    // region Constructors
    // spotless:off
    public PollutionType(String name,
                         int spreadThreshold,
                         int operationCycle,
                         float naturalDecayRate,
                         List<Potion> potionList,
                         int pollutionDamageThreshold,
                         int maxAttemptsBlockReplace,
                         int pollutionThresholdPerAttempt,
                         List<BlockDamageManager> blockDamageManagerList,
                         List<Block> blockDestroyList,
                         BiomeGenBase biome,
                         int biomeChangeThreshold,
                         int pollutionEffectThreshold) {

        this.name = name;
        this.spreadThreshold = spreadThreshold;
        this.operationCycle = operationCycle;
        this.naturalDecayRate = naturalDecayRate;
        this.potionList = potionList;
        this.pollutionDamageThreshold = pollutionDamageThreshold;
        this.maxAttemptsBlockReplace = maxAttemptsBlockReplace;
        this.pollutionThresholdPerAttempt = pollutionThresholdPerAttempt;
        this.blockDamageManagerList = blockDamageManagerList;
        this.blockDestroyList = blockDestroyList;
        this.biome = biome;
        this.biomeChangeThreshold = biomeChangeThreshold;
        this.pollutionEffectThreshold = pollutionEffectThreshold;

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

    public int getOperationCycle() {
        return operationCycle;
    }

    public float getNaturalDecayRate() {
        return naturalDecayRate;
    }

    public Int2ObjectOpenHashMap<Pollution> getDimensionWisePollution() {
        return dimensionWisePollution;
    }

    public PollutionStorage getStorage() {
        return storage;
    }

    public List<Potion> getPotionList() {
        return Collections.unmodifiableList(potionList);
    }

    public int getPollutionDamageThreshold() {
        return pollutionDamageThreshold;
    }

    public int getMaxAttemptsBlockReplace() {
        return maxAttemptsBlockReplace;
    }

    public int getPollutionThresholdPerAttempt() {
        return pollutionThresholdPerAttempt;
    }

    public List<BlockDamageManager> getBlockDamageManagerList() {
        return blockDamageManagerList;
    }

    public List<Block> getBlockDestroyList() {
        return blockDestroyList;
    }

    public BiomeGenBase getBiome() {
        return biome;
    }

    public int getBiomeChangeThreshold() {
        return biomeChangeThreshold;
    }

    public int getPollutionEffectThreshold() {
        return pollutionEffectThreshold;
    }

    // endregion

    // region Others methods
    public Pollution createPollutionInstance() {
        return new Pollution(this);
    }
    // endregion
}
