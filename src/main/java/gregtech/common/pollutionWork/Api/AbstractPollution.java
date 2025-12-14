package gregtech.common.pollutionWork.Api;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import gregtech.common.pollutionWork.Api.Handlers.PollutionEffectHandler;
import gregtech.common.pollutionWork.Api.Handlers.PollutionEventHandler;
import gregtech.common.pollutionWork.Api.Handlers.PollutionNetworkHandler;
import gregtech.common.pollutionWork.Api.Handlers.PollutionSpreadHandler;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.gameevent.TickEvent;

public abstract class AbstractPollution {

    // region AbstractMethods
    abstract protected int getCycleLen();

    abstract protected int getSpreadThreshold();

    abstract protected float getNaturalDecayRate();
    // endregion

    // region Class Variables
    private List<ChunkCoordIntPair> pollutionList = new ArrayList<>();
    private final Set<ChunkCoordIntPair> pollutedChunks = new HashSet<>();
    private int operationsPerTick = 0;
    private boolean blank = true;
    private final World world;
    protected final PollutionType pollutionType;
    private static final PollutionEffectHandler EFFECT_HANDLER = new PollutionEffectHandler();
    private static final PollutionSpreadHandler SPREAD_HANDLER = new PollutionSpreadHandler();
    private static final PollutionEventHandler EVENT_HANDLER = new PollutionEventHandler();
    // endregion

    // region Static
    static {
        MinecraftForge.EVENT_BUS.register(EVENT_HANDLER);
    }
    // endregion

    // region Constructors
    public AbstractPollution(World world, PollutionType pollutionType) {
        this.world = world;
        this.pollutionType = pollutionType;
        pollutionType.getDimensionWisePollution()
            .put(world.provider.dimensionId, this);

        if (!world.isRemote) {
            PollutionApi.getStorage(pollutionType)
                .loadAll(world);
        }
    }
    // endregion

    // region Getters
    public Set<ChunkCoordIntPair> getPollutedChunks() {
        return pollutedChunks;
    }
    // endregion

    // region Setters
    private void setChunkPollution(ChunkCoordIntPair cord, int pollution) {
        PollutionApi.getStorage(pollutionType)
            .mutatePollution(
                world,
                cord.chunkXPos,
                cord.chunkZPos,
                data -> data.setPollutionAmount(pollution),
                pollutedChunks);
    }
    // endregion

    // region Methods
    public static void onWorldTick(TickEvent.WorldTickEvent event, PollutionType pollutionType) {
        if (event.phase == TickEvent.Phase.START) return;

        final AbstractPollution pollutionInstance = pollutionType.getDimensionWisePollution()
            .computeIfAbsent(event.world.provider.dimensionId, i -> pollutionType.createPollutionInstance(event.world));

        pollutionInstance
            .tickPollutionInWorld((int) (event.world.getTotalWorldTime() % pollutionInstance.getCycleLen()));
    }

    private void tickPollutionInWorld(int tickId) {
        initializeCycleIfNeeded(tickId);

        int chunksToProcess = Math.min(operationsPerTick, pollutionList.size());
        for (int i = 0; i < chunksToProcess; i++) {
            ChunkCoordIntPair chunkPos = pollutionList.remove(pollutionList.size() - 1);
            processChunkPollution(chunkPos);
        }
    }

    private void initializeCycleIfNeeded(int tickId) {
        if (tickId == 0 || blank) {
            pollutionList = new ArrayList<>(pollutedChunks);
            operationsPerTick = pollutionList.isEmpty() ? 0 : Math.max(1, pollutionList.size() / getCycleLen());
            blank = false;
        }
    }

    private void processChunkPollution(ChunkCoordIntPair chunkPos) {
        PollutionData data = PollutionApi.getStorage(pollutionType)
            .get(world, chunkPos);
        AtomicInteger pollution = new AtomicInteger((int) applyNaturalDecay(data.getPollutionAmount()));

        if (pollution.get() > getSpreadThreshold()) {
            SPREAD_HANDLER.handlePollutionSpread(
                world,
                chunkPos,
                pollution,
                PollutionApi.getStorage(pollutionType),
                pollutedChunks);
            EFFECT_HANDLER.applyPollutionEffects(world, chunkPos, pollution.get());
        }

        setChunkPollution(chunkPos, pollution.get());
        PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution.get());
    }

    private float applyNaturalDecay(int pollution) {
        return getNaturalDecayRate() * pollution;
    }
    // endregion
}
