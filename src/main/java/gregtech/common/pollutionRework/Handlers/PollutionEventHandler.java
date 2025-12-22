package gregtech.common.pollutionRework.Handlers;

import java.util.Map;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.net.GTPacketPollution;
import gregtech.common.pollutionRework.Api.PollutionApi;
import gregtech.common.pollutionRework.Api.PollutionRegistry;
import gregtech.common.pollutionRework.Api.PollutionType;
import gregtech.common.pollutionRework.Data.PollutionStorage;

public class PollutionEventHandler {

    private static final Map<String, PollutionType> POLLUTIONS = PollutionRegistry.getAllPollutions();

    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch event) {
        if (event.player == null || event.player.worldObj == null) return;

        World world = event.player.worldObj;
        ChunkCoordIntPair chunkCord = new ChunkCoordIntPair(event.chunk.chunkXPos, event.chunk.chunkZPos);

        for (PollutionType type : POLLUTIONS.values()) {
            PollutionStorage storage = PollutionApi.getStorage(type);
            int pollution = 0;

            if (storage.isCreated(world, chunkCord)) {
                pollution = storage.get(world, chunkCord)
                    .getPollutionAmount();
            }

            if (pollution > 0) {
                GTValues.NW.sendToPlayer(new GTPacketPollution(type, chunkCord, pollution), event.player);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) {
            for (PollutionType type : POLLUTIONS.values()) {
                PollutionApi.getStorage(type)
                    .loadAll(event.world);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null) return;
        for (PollutionType type : POLLUTIONS.values()) {
            type.getDimensionWisePollution()
                .remove(event.world.provider.dimensionId);
        }
    }
}
