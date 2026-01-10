package gregtech.common.pollutionRework;

import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.world.biome.BiomeGenBase;

import gregtech.common.pollutionRework.Api.PollutionBuilder;
import gregtech.common.pollutionRework.Api.PollutionRegistry;
import gregtech.common.pollutionRework.Api.PollutionType;
import it.unimi.dsi.fastutil.Pair;

public class PollutionTypes {

    public static void init() {
        PollutionRegistry.registerPollution(SMOG);
        PollutionRegistry.registerPollution(RADIATION);

//         registerTestPollution();
    }

    public static final PollutionType SMOG = PollutionBuilder.builder("SMOG")
        .setSpreadThreshold(10_000)
        .setOperationCycle(200)
        .setNaturalDecayRate(0.9945f)
        .setPotion(Potion.blindness)
        .setMaxAttemptsBlockReplace(10)
        .setPollutionDamageThreshold(100)
        .setPollutionThresholdPerAttempt(1)
        .setBlocksDestroy(Blocks.sand, Blocks.grass)
        .setBiomeChanger(BiomeGenBase.iceMountains, 10_000_000)
        .setBlocksReplace(Blocks.grass,
            Blocks.coal_block,
            Blocks.brick_block,
            Blocks.diamond_block)
        .setBlocksReplace(Blocks.grass,
            Pair.of(Blocks.wool, 10),
            Pair.of(Blocks.bedrock, 20),
            Pair.of(Blocks.tnt, 30),
            Pair.of(Blocks.bookshelf, 40))
        .build();

    public static final PollutionType RADIATION = PollutionBuilder.builder("RADIATION")
        .setSpreadThreshold(100_000)
        .setOperationCycle(5_000)
        .setNaturalDecayRate(0.9945f)
        .setPotion(Potion.confusion)
        .setBlocksDestroy(Blocks.sand, Blocks.grass)
        .build();

    // TEST

    public static void registerTestPollution() {
        for (int i = 1; i <= 50; i++) {
            String name = "Pollution_" + i;

            PollutionType pollution = PollutionBuilder.builder(name)
                .setSpreadThreshold(10_000)
                .setOperationCycle(200)
                .build();
            PollutionRegistry.registerPollution(pollution);
        }
    }
}
