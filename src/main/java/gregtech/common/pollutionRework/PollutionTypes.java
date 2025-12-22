package gregtech.common.pollutionRework;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;

import gregtech.common.pollutionRework.Api.PollutionBuilder;
import gregtech.common.pollutionRework.Api.PollutionRegistry;
import gregtech.common.pollutionRework.Api.PollutionType;

import java.util.Arrays;

public class PollutionTypes {

    public static void init() {
        PollutionRegistry.registerPollution(SMOG);
        PollutionRegistry.registerPollution(RADIATION);
    }

    public static final PollutionType SMOG = PollutionBuilder.builder("SMOG")
        .setSpreadThreshold(10_000)
        .setCycleLen(200)
        .setNaturalDecayRate(0.9945f)
        .addPotion(Potion.blindness)
        .setMaxAttempts(500)
        .setPollutionDamageStart(100)
        .setVegetationAttemptsDivisor(1)
        .setListBlocksDestroy(
            Arrays.asList(
                Blocks.grass,
                Blocks.sand,
                Blocks.gravel
            ))
        .setListPairBlocksReplace(
            Arrays.asList(
                Pair.of(Blocks.grass, Blocks.sandstone),
                Pair.of(Blocks.sandstone, Blocks.cobblestone)
            )
        )
        .build();

    public static final PollutionType RADIATION = PollutionBuilder.builder("RADIATION")
        .setSpreadThreshold(100_000)
        .setCycleLen(5_000)
        .setNaturalDecayRate(0.9945f)
        .addPotion(Potion.confusion)
        .setListBlocksDestroy(
            Arrays.asList(
                Blocks.grass,
                Blocks.sand,
                Blocks.gravel
            ))
        .build();
}
