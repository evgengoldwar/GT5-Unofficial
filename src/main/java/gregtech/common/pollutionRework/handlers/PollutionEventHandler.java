package gregtech.common.pollutionRework.handlers;

import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.GTMod;
import gregtech.api.enums.GTValues;
import gregtech.api.net.GTPacketPollution;
import gregtech.common.pollutionRework.Pollution;
import gregtech.common.pollutionRework.PollutionStorage;

public class PollutionEventHandler {

    PollutionStorage STORAGE = Pollution.getSTORAGE();

    @SubscribeEvent
    public void chunkWatch(ChunkWatchEvent.Watch event) {
        if (!GTMod.proxy.mPollution) return;
        World world = event.player.worldObj;
        if (STORAGE.isCreated(world, event.chunk)) {
            int pollution = STORAGE.get(world, event.chunk)
                .getAmount();
            if (PollutionNetworkHandler.shouldSendUpdate(pollution))
                GTValues.NW.sendToPlayer(new GTPacketPollution(event.chunk, pollution), event.player);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (!e.world.isRemote) {
            STORAGE.loadAll(e.world);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        GTMod.proxy.dimensionWisePollutionRework.remove(e.world.provider.dimensionId);
    }
}
