package gregtech.common.pollutionRework;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import gregtech.GTMod;

public enum ColorOverrideTypeRework {

    FLOWER("FLOWER") {

        @Override
        public int getColor(int originalColor, int x, int z) {
            return GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedFoliageColor(originalColor, x, z);
        }
    },
    GRASS("GRASS") {

        @Override
        public int getColor(int originalColor, int x, int z) {
            return GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedGrassColor(originalColor, x, z);
        }
    },
    LEAVES("LEAVES") {

        @Override
        public int getColor(int originalColor, int x, int z) {
            return GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedLeavesColor(originalColor, x, z);
        }
    },
    LIQUID("LIQUID") {

        @Override
        public int getColor(int originalColor, int x, int z) {
            return GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedLiquidColor(originalColor, x, z);
        }
    };

    private final String name;
    private static final Map<String, ColorOverrideTypeRework> NAME_MAP = Arrays.stream(values())
        .collect(Collectors.toMap(ColorOverrideTypeRework::getName, Function.identity()));

    ColorOverrideTypeRework(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ColorOverrideTypeRework fromName(String name) {
        ColorOverrideTypeRework type = NAME_MAP.get(name);
        if (type == null) {
            throw new IllegalArgumentException("Unknown color override type: " + name);
        }
        return type;
    }

    public abstract int getColor(int originalColor, int x, int z);
}
