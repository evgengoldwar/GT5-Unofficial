package gregtech.common.pollutionRework;

import gregtech.GTMod;

public enum ColorOverrideTypeRework {

    FLOWER,
    GRASS,
    LEAVES,
    LIQUID;

    public static ColorOverrideTypeRework fromName(String name) {
        return switch (name) {
            case "FLOWER" -> FLOWER;
            case "GRASS" -> GRASS;
            case "LEAVES" -> LEAVES;
            case "LIQUID" -> LIQUID;
            default -> throw new RuntimeException();
        };
    }

    public int getColor(int oColor, int x, int z) {
        return switch (this) {
            case FLOWER -> GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedFoliageColor(oColor, x, z);
            case GRASS -> GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedGrassColor(oColor, x, z);
            case LEAVES -> GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedLeavesColor(oColor, x, z);
            case LIQUID -> GTMod.clientProxy().mPollutionRendererRework.getPollutionAdjustedLiquidColor(oColor, x, z);
        };
    }
}
