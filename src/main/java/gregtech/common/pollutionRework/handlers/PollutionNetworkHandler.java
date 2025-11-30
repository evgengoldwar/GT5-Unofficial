package gregtech.common.pollutionRework.handlers;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.NetworkRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.net.GTPacketPollution;

public class PollutionNetworkHandler {

    private static final int POLLUTION_PACKET_MIN_VALUE = 1000;

    public static void sendPollutionUpdate(World world, ChunkCoordIntPair chunkPos, int pollution) {
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(
            world.provider.dimensionId,
            chunkPos.chunkXPos << 4,
            64,
            chunkPos.chunkZPos << 4,
            256);
        GTValues.NW.sendToAllAround(new GTPacketPollution(chunkPos, pollution), point);
    }

    public static boolean shouldSendUpdate(int pollution) {
        return pollution > POLLUTION_PACKET_MIN_VALUE;
    }
}
