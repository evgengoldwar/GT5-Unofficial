package gregtech.common.pollutionRework;

import java.util.Arrays;

import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;

import gregtech.common.pollutionRework.Api.PollutionBuilder;
import gregtech.common.pollutionRework.Api.PollutionRegistry;
import gregtech.common.pollutionRework.Api.PollutionType;
import gregtech.common.pollutionRework.Utils.BlockDamageManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.biome.BiomeGenBase;

public class PollutionTypes {

    public static void init() {
        PollutionRegistry.registerPollution(SMOG);
        PollutionRegistry.registerPollution(RADIATION);

//        registerTestPollution();
    }

    public static final PollutionType SMOG = PollutionBuilder.builder("SMOG")
        .setSpreadThreshold(10_000)
        .setCycleLen(200)
        .setNaturalDecayRate(0.9945f)
        .addPotion(Potion.blindness)
        .setMaxAttempts(500)
        .setPollutionDamageStart(100)
        .setVegetationAttemptsDivisor(1)
        .setBlocksDestroy(Blocks.sand, Blocks.grass)
        .setBiomeChanger(BiomeGenBase.iceMountains)
        .setBlocksDamage(
            BlockDamageManager.setBlocksReplace(
                Blocks.grass,
                Arrays.asList(
                    Pair.of(Blocks.emerald_block, 10),
                    Pair.of(Blocks.diamond_block, 20),
                    Pair.of(Blocks.gold_block, 30),
                    Pair.of(Blocks.quartz_block, 40))))
        .build();

    public static final PollutionType RADIATION = PollutionBuilder.builder("RADIATION")
        .setSpreadThreshold(100_000)
        .setCycleLen(5_000)
        .setNaturalDecayRate(0.9945f)
        .addPotion(Potion.confusion)
        .setBlocksDestroy(Blocks.sand, Blocks.grass)
        .build();

    // TEST

    public static void registerTestPollution() {
        for (int i = 1; i <= 100; i++) {
            String name = "Pollution_" + i;

            PollutionType pollution = PollutionBuilder.builder(name)
                .setCycleLen(200)
                .build();
            PollutionRegistry.registerPollution(pollution);
        }
    }
}
