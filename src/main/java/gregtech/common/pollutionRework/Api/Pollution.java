package gregtech.common.pollutionRework.Api;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import gregtech.common.pollutionRework.Data.PollutionStorage;
import gregtech.common.pollutionRework.Utils.PollutionUtils;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import gregtech.common.pollutionRework.Data.PollutionData;
import gregtech.common.pollutionRework.Handlers.*;

public class Pollution {
    // region Class Variables
    private final List<ChunkCoordIntPair> pollutionList = new ArrayList<>();
    private final Set<ChunkCoordIntPair> pollutedChunks = new HashSet<>();
    private int operationsPerTick = 0;
    protected final PollutionType pollutionType;
    private final PollutionSpreadHandler SPREAD_HANDLER;
    private final PollutionEffectHandler EFFECT_HANDLER;
    private final PollutionBlockDamager DAMAGE_HANDLER;
    private final PollutionStorage POLLUTION_STORAGE;
    // endregion

    // region Constructors
    public Pollution(PollutionType pollutionType) {
        this.pollutionType = pollutionType;

        SPREAD_HANDLER = new PollutionSpreadHandler();
        EFFECT_HANDLER = new PollutionEffectHandler(pollutionType.potionList);
        DAMAGE_HANDLER = new PollutionBlockDamager(
            pollutionType.maxAttemptsBlockReplace,
            pollutionType.pollutionThresholdPerAttempt,
            pollutionType.blockDamageManagerList,
            pollutionType.blockDestroyList);
        POLLUTION_STORAGE = PollutionApi.getStorage(pollutionType);
    }
    // endregion

    // region Getters
    public Set<ChunkCoordIntPair> getPollutedChunks() {
        return pollutedChunks;
    }
    // endregion

    // region Setters
    private void setChunkPollution(World world, ChunkCoordIntPair cord, int pollution) {
        POLLUTION_STORAGE.mutatePollution(
                world,
                cord.chunkXPos,
                cord.chunkZPos,
                data -> data.setPollutionAmount(pollution),
                pollutedChunks);
    }
    // endregion

    // region Methods
    public static void onWorldTick(World world, PollutionType pollutionType, int tickId) {
        final Pollution pollutionInstance = pollutionType.dimensionWisePollution
            .computeIfAbsent(world.provider.dimensionId, i -> pollutionType.createPollutionInstance());

        if (tickId == 0) {
            pollutionInstance.applyPollutionPrecess();
        }

        if (!pollutionInstance.pollutionList.isEmpty()) {
            pollutionInstance.tickPollutionInWorld(world);
        }
    }

    private void applyPollutionPrecess() {
        pollutionList.clear();
        pollutionList.addAll(pollutedChunks);
        operationsPerTick = pollutionList.isEmpty() ? 0 : Math.min(50, pollutionList.size());
    }

    private void tickPollutionInWorld(World world) {
        for (int i = 0; i < operationsPerTick && !pollutionList.isEmpty(); i++) {
            ChunkCoordIntPair chunkPos = pollutionList.remove(pollutionList.size() - 1);

            if (PollutionUtils.checkIsChunkLoaded(chunkPos, world)) {
                processChunkPollution(world, chunkPos);
            }
        }
    }

    private void processChunkPollution(World world, ChunkCoordIntPair chunkPos) {
        PollutionData pollutionData = POLLUTION_STORAGE.get(world, chunkPos);
        AtomicInteger pollution = new AtomicInteger((int) (pollutionType.naturalDecayRate * pollutionData.getPollutionAmount()));

        if (pollution.get() > pollutionType.spreadThreshold) {
            SPREAD_HANDLER.handlePollutionSpread(
                world,
                chunkPos,
                pollution,
                POLLUTION_STORAGE,
                pollutedChunks);
        }

        if (pollution.get() > pollutionType.pollutionDamageThreshold) {
            DAMAGE_HANDLER.applyDamageEffects(world, chunkPos, pollution.get());
        }

        if (pollution.get() > pollutionType.pollutionEffectThreshold) {
            EFFECT_HANDLER.applyPotionEffects(world, chunkPos, pollution.get());
        }

        if (pollution.get() > pollutionType.biomeChangeThreshold) {
            PollutionBiomeChangeHandler.changeChunkBiome(world, chunkPos, pollutionType.biome);
        }

        setChunkPollution(world, chunkPos, pollution.get());
        PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution.get(), pollutionType);
    }
    // endregion
}
