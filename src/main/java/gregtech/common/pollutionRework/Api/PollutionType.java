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
    public final String name;
    public final int operationCycle;

    // Spread
    public final int spreadThreshold;
    public final float naturalDecayRate;

    // Effect
    public final List<Potion> potionList;
    public final int pollutionEffectThreshold;

    // DamageBLock variables
    public final int pollutionDamageThreshold;
    public final int maxAttemptsBlockReplace;
    public final int pollutionThresholdPerAttempt;
    public final List<BlockDamageManager> blockDamageManagerList;
    public final List<Block> blockDestroyList;

    // BiomeChange variables
    public final BiomeGenBase biome;
    public final int biomeChangeThreshold;

    public final PollutionStorage storage;
    public final Int2ObjectOpenHashMap<Pollution> dimensionWisePollution = new Int2ObjectOpenHashMap<>(16);
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

    // region Others methods
    public Pollution createPollutionInstance() {
        return new Pollution(this);
    }
    // endregion
}
