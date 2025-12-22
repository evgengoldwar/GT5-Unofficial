package gregtech.common.pollutionRework;

import net.minecraft.potion.Potion;

import gregtech.common.pollutionRework.Api.PollutionBuilder;
import gregtech.common.pollutionRework.Api.PollutionRegistry;
import gregtech.common.pollutionRework.Api.PollutionType;

public class PollutionTypes {

    public static void init() {
        PollutionRegistry.registerPollution(SMOG);
        PollutionRegistry.registerPollution(RADIATION);
    }

    public static final PollutionType SMOG = PollutionBuilder.builder("SMOG")
        .withSpreadThreshold(10_000)
        .withCycleLen(2_000)
        .withNaturalDecayRate(0.9945f)
        .addPotion(Potion.blindness)
        .build();

    public static final PollutionType RADIATION = PollutionBuilder.builder("RADIATION")
        .withSpreadThreshold(100_000)
        .withCycleLen(5_000)
        .withNaturalDecayRate(0.9945f)
        .addPotion(Potion.confusion)
        .build();
}
