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
    private final PollutionSpreadHandler SPREAD_HANDLER = new PollutionSpreadHandler();
    private static final PollutionEventHandler EVENT_HANDLER = new PollutionEventHandler();
    private final PollutionEffectHandler EFFECT_HANDLER;
    private final PollutionBlockDamager DAMAGE_HANDLER;
    // endregion

    // region Static
    static {
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER);
    }
    // endregion

    // region Constructors
    public Pollution(PollutionType pollutionType) {
        this.pollutionType = pollutionType;

        EFFECT_HANDLER = new PollutionEffectHandler(pollutionType.getPotionList());
        DAMAGE_HANDLER = new PollutionBlockDamager(
            pollutionType.getMaxAttemptsBlockReplace(),
            pollutionType.getPollutionThresholdPerAttempt(),
            pollutionType.getBlockDamageManagerList(),
            pollutionType.getBlockDestroyList());
    }
    // endregion

    // region Getters
    public Set<ChunkCoordIntPair> getPollutedChunks() {
        return pollutedChunks;
    }
    // endregion

    // region Setters
    private void setChunkPollution(World world, ChunkCoordIntPair cord, int pollution, PollutionStorage storage) {
        storage.mutatePollution(
                world,
                cord.chunkXPos,
                cord.chunkZPos,
                data -> data.setPollutionAmount(pollution),
                pollutedChunks);
    }
    // endregion

    // region Methods
    public static void onWorldTick(World world, PollutionType pollutionType, int tickId) {

        final Pollution pollutionInstance = pollutionType.getDimensionWisePollution()
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
        PollutionStorage storage = PollutionApi.getStorage(pollutionType);
        PollutionData data = storage.get(world, chunkPos);
        AtomicInteger pollution = new AtomicInteger((int) (pollutionType.getNaturalDecayRate() * data.getPollutionAmount()));

        if (pollution.get() > pollutionType.getSpreadThreshold()) {
            SPREAD_HANDLER.handlePollutionSpread(
                world,
                chunkPos,
                pollution,
                storage,
                pollutedChunks);
        }

        if (pollution.get() > pollutionType.getPollutionDamageThreshold()) {
            DAMAGE_HANDLER.applyDamageEffects(world, chunkPos, pollution.get());
        }

        if (pollution.get() > pollutionType.getPollutionEffectThreshold()) {
            EFFECT_HANDLER.applyPotionEffects(world, chunkPos, pollution.get());
        }

        if (pollution.get() > pollutionType.getBiomeChangeThreshold()) {
            PollutionBiomeChangeHandler.changeChunkBiome(world, chunkPos, pollutionType.getBiome());
        }

        setChunkPollution(world, chunkPos, pollution.get(), storage);
        PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution.get(), pollutionType);
    }
    // endregion
}
