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
    // Abstract variables
    private final String name;
    private int cycleLen = 1_000;
    private int spreadThreshold = 100_000;
    private float naturalDecayRate = 0.9945f;
    private final List<Potion> potionList = new ArrayList<>();

    // DamageBLock variables
    private int pollutionDamageStart = 100_000;
    private int maxAttempts = 100;
    private int vegetationAttemptsDivisor = 25_000;
    private final List<BlockDamageManager> blockDamageManager = new ArrayList<>();
    private final List<Block> listBlockDestroy = new ArrayList<>();

    // Biome Variables
    private BiomeGenBase biome;
    private int biomeChangeThreshold;
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

    public PollutionBuilder addPotion(Potion... potions) {
        this.potionList.addAll(Arrays.asList(potions));
        return this;
    }

    public PollutionBuilder setSpreadThreshold(int spreadThreshold) {
        this.spreadThreshold = spreadThreshold;
        return this;
    }

    public PollutionBuilder setNaturalDecayRate(float naturalDecayRate) {
        this.naturalDecayRate = naturalDecayRate;
        return this;
    }

    public PollutionBuilder setCycleLen(int cycleLen) {
        this.cycleLen = cycleLen;
        return this;
    }

    public PollutionBuilder setPollutionDamageStart(int pollutionDamageStart) {
        this.pollutionDamageStart = pollutionDamageStart;
        return this;
    }

    public PollutionBuilder setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public PollutionBuilder setVegetationAttemptsDivisor(int vegetationAttemptsDivisor) {
        this.vegetationAttemptsDivisor = vegetationAttemptsDivisor;
        return this;
    }

    public PollutionBuilder setBlocksReplace(Block masterBlock, Block... replaceBlock) {
        this.blockDamageManager.add(BlockDamageManager.setBlocksReplace(masterBlock, replaceBlock));
        return this;
    }

    @SafeVarargs
    public final PollutionBuilder setBlocksReplace(Block masterBlock, Pair<Block, Integer>... replaceBlock) {
        this.blockDamageManager.add(BlockDamageManager.setBlocksReplace(masterBlock, replaceBlock));
        return this;
    }

    public PollutionBuilder setBlocksDestroy(Block... blocks) {
        this.listBlockDestroy.addAll(Arrays.asList(blocks));
        return this;
    }

    public PollutionBuilder setBiomeChanger(BiomeGenBase biome, int biomeChangeThreshold) {
        this.biome = biome;
        this.biomeChangeThreshold = biomeChangeThreshold;
        return this;
    }

    public PollutionType build() {
        return new PollutionType(
            name,
            spreadThreshold,
            cycleLen,
            naturalDecayRate,
            potionList,
            pollutionDamageStart,
            maxAttempts,
            vegetationAttemptsDivisor,
            blockDamageManager,
            listBlockDestroy,
            biome,
            biomeChangeThreshold);
    }
    // endregion
}
