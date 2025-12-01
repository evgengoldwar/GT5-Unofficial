package gregtech.common.pollutionRework.handlers;

import java.util.Arrays;
import java.util.Set;

import gregtech.GTMod;
import gregtech.common.pollutionRework.Pollution;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import gregtech.api.util.GTUtility;
import gregtech.common.pollutionRework.PollutionData;
import gregtech.common.pollutionRework.PollutionStorage;

public class PollutionSpreadHandler {

    private static final ChunkCoordIntPair[] NEIGHBOR_OFFSETS = { new ChunkCoordIntPair(1, 0),
        new ChunkCoordIntPair(-1, 0), new ChunkCoordIntPair(0, 1), new ChunkCoordIntPair(0, -1) };

    private static final int SPREAD_RATIO_NUMERATOR = 6;
    private static final int SPREAD_RATIO_DENOMINATOR = 5;
    private static final int SPREAD_DIVISOR = 20;

    public void handlePollutionSpread(World world, ChunkCoordIntPair sourcePos, int sourcePollution,
        PollutionStorage storage, Set<ChunkCoordIntPair> pollutedChunks) {
        Arrays.stream(NEIGHBOR_OFFSETS)
            .map(
                offset -> new ChunkCoordIntPair(
                    sourcePos.chunkXPos + offset.chunkXPos,
                    sourcePos.chunkZPos + offset.chunkZPos))
            .forEach(neighborPos -> spreadPollutionToNeighbor(world, sourcePos, sourcePollution, neighborPos, storage, pollutedChunks));
    }

    private void spreadPollutionToNeighbor(World world, ChunkCoordIntPair sourcePos, int sourcePollution,
                                           ChunkCoordIntPair neighborPos, PollutionStorage storage, Set<ChunkCoordIntPair> pollutedChunks) {

        PollutionData neighborData = storage.get(world, neighborPos);
        int neighborPollution = neighborData.getAmount();

        if (shouldSpreadPollution(sourcePollution, neighborPollution)) {
            int difference = calculateSpreadDifference(sourcePollution, neighborPollution);
            if (difference > 0) {
                storage.mutatePollution(
                    world,
                    neighborPos.chunkXPos,
                    neighborPos.chunkZPos,
                    data -> data.changeAmount(difference),
                    pollutedChunks
                );
            }
        }
    }

    private boolean shouldSpreadPollution(int sourcePollution, int neighborPollution) {
        return neighborPollution * SPREAD_RATIO_NUMERATOR < sourcePollution * SPREAD_RATIO_DENOMINATOR;
    }

    private int calculateSpreadDifference(int sourcePollution, int neighborPollution) {
        return (sourcePollution - neighborPollution) / SPREAD_DIVISOR;
    }
}
