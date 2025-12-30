package gregtech.common.pollutionRework.Api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.block.Block;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;

import gregtech.common.pollutionRework.Utils.BlockDamageManager;

public class PollutionBuilder {

    // region Variables
    // Base
    private final String name;
    private int operationCycle = 1_000;

    // Spread
    private int spreadThreshold = 500_000;
    private float naturalDecayRate = 0.9945f;

    // Effect
    private final List<Potion> potionList = new ArrayList<>();
    private int pollutionEffectThreshold = 500_000;

    // DamageBLock variables
    private int pollutionDamageThreshold = 500_000;
    private int maxAttemptsBlockReplace = 10;
    private int pollutionThresholdPerAttempt = 25_000;
    private final List<BlockDamageManager> blockDamageManagerList = new ArrayList<>();
    private final List<Block> blockDestroyList = new ArrayList<>();

    // Biome Variables
    private BiomeGenBase changeBiome;
    private int biomeChangeThreshold = 10_000_000;
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

    // Effect
    public PollutionBuilder setPotion(Potion... potions) {
        this.potionList.addAll(Arrays.asList(potions));
        return this;
    }

    public PollutionBuilder setPollutionEffectThreshold(int pollutionEffectThreshold) {
        this.pollutionEffectThreshold = pollutionEffectThreshold;
        return this;
    }

    // Spread
    public PollutionBuilder setSpreadThreshold(int spreadThreshold) {
        this.spreadThreshold = spreadThreshold;
        return this;
    }

    public PollutionBuilder setNaturalDecayRate(float naturalDecayRate) {
        this.naturalDecayRate = naturalDecayRate;
        return this;
    }

    // Base
    public PollutionBuilder setOperationCycle(int operationCycle) {
        this.operationCycle = operationCycle;
        return this;
    }

    // Damage
    public PollutionBuilder setPollutionDamageThreshold(int pollutionDamageThreshold) {
        this.pollutionDamageThreshold = pollutionDamageThreshold;
        return this;
    }

    public PollutionBuilder setMaxAttemptsBlockReplace(int maxAttemptsBlockReplace) {
        this.maxAttemptsBlockReplace = maxAttemptsBlockReplace;
        return this;
    }

    public PollutionBuilder setPollutionThresholdPerAttempt(int pollutionThresholdPerAttempt) {
        this.pollutionThresholdPerAttempt = pollutionThresholdPerAttempt;
        return this;
    }

    public PollutionBuilder setBlocksReplace(Block masterBlock, Block... replaceBlock) {
        this.blockDamageManagerList.add(BlockDamageManager.setBlocksReplace(masterBlock, replaceBlock));
        return this;
    }

    @SafeVarargs
    public final PollutionBuilder setBlocksReplace(Block masterBlock, Pair<Block, Integer>... replaceBlock) {
        this.blockDamageManagerList.add(BlockDamageManager.setBlocksReplace(masterBlock, replaceBlock));
        return this;
    }

    public PollutionBuilder setBlocksDestroy(Block... blocks) {
        this.blockDestroyList.addAll(Arrays.asList(blocks));
        return this;
    }

    // Biome
    public PollutionBuilder setBiomeChanger(BiomeGenBase biome, int biomeChangeThreshold) {
        this.changeBiome = biome;
        this.biomeChangeThreshold = biomeChangeThreshold;
        return this;
    }

    public PollutionType build() {
        return new PollutionType(
            name,
            spreadThreshold,
            operationCycle,
            naturalDecayRate,
            potionList,
            pollutionDamageThreshold,
            maxAttemptsBlockReplace,
            pollutionThresholdPerAttempt,
            blockDamageManagerList,
            blockDestroyList,
            changeBiome,
            biomeChangeThreshold,
            pollutionEffectThreshold);
    }
    // endregion
}
