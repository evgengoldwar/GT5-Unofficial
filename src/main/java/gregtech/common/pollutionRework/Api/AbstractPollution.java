package gregtech.common.pollutionRework.Api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.potion.Potion;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.pollutionRework.Data.PollutionData;
import gregtech.common.pollutionRework.Handlers.*;

public abstract class AbstractPollution {

    // region AbstractMethods
    abstract protected int getCycleLen();

    abstract protected int getSpreadThreshold();

    abstract protected float getNaturalDecayRate();

    abstract protected List<Potion> getPotionList();
    // endregion

    // region Class Variables
    private List<ChunkCoordIntPair> pollutionList = new ArrayList<>();
    private final Set<ChunkCoordIntPair> pollutedChunks = new HashSet<>();
    private int operationsPerTick = 0;
    private boolean blank = true;
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
    public AbstractPollution(PollutionType pollutionType) {
        this.pollutionType = pollutionType;

        EFFECT_HANDLER = new PollutionEffectHandler(pollutionType.getPotionList());
        DAMAGE_HANDLER = new PollutionBlockDamager(
            pollutionType.getPollutionDamageStart(),
            pollutionType.getMaxAttempts(),
            pollutionType.getVegetationAttemptsDivisor(),
            pollutionType.getBlockDamageManager(),
            pollutionType.getListBlockDestroy());
    }
    // endregion

    // region Getters
    public Set<ChunkCoordIntPair> getPollutedChunks() {
        return pollutedChunks;
    }
    // endregion

    // region Setters
    private void setChunkPollution(World world, ChunkCoordIntPair cord, int pollution) {
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
            .computeIfAbsent(event.world.provider.dimensionId, i -> pollutionType.createPollutionInstance());

        pollutionInstance.tickPollutionInWorld(
            event.world,
            (int) (event.world.getTotalWorldTime() % pollutionInstance.getCycleLen()));
    }

    private void tickPollutionInWorld(World world, int tickId) {
        initializeCycleIfNeeded(tickId);

        int chunksToProcess = Math.min(operationsPerTick, pollutionList.size());
        for (int i = 0; i < chunksToProcess; i++) {
            ChunkCoordIntPair chunkPos = pollutionList.remove(pollutionList.size() - 1);
            processChunkPollution(world, chunkPos);
        }
    }

    private void initializeCycleIfNeeded(int tickId) {
        if (tickId == 0 || blank) {
            pollutionList = new ArrayList<>(pollutedChunks);
            operationsPerTick = pollutionList.isEmpty() ? 0 : Math.max(1, pollutionList.size() / getCycleLen());
            blank = false;
        }
    }

    private void processChunkPollution(World world, ChunkCoordIntPair chunkPos) {
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
            EFFECT_HANDLER.applyPotionEffects(world, chunkPos, pollution.get());
            DAMAGE_HANDLER.applyDamageEffects(world, chunkPos, pollution.get());
            PollutionBiomeChangeHandler.changeChunkBiome(world, chunkPos,pollutionType.getBiome());
        }

        setChunkPollution(world, chunkPos, pollution.get());
        PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution.get());
    }

    private float applyNaturalDecay(int pollution) {
        return getNaturalDecayRate() * pollution;
    }
    // endregion
}
