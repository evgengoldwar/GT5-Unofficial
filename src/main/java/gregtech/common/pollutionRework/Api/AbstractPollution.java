package gregtech.common.pollutionRework.Api;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.potion.Potion;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

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
    public static void onWorldTick(World world, PollutionType pollutionType, int tickId) {

        final AbstractPollution pollutionInstance = pollutionType.getDimensionWisePollution()
            .computeIfAbsent(world.provider.dimensionId, i -> pollutionType.createPollutionInstance());

        if (tickId == 0) {
            pollutionInstance.applyPollutionPrecess();
        }

        if (!pollutionInstance.pollutionList.isEmpty()) {
            pollutionInstance.tickPollutionInWorld(world, tickId);
        }
    }

    private void applyPollutionPrecess() {
        pollutionList.clear();
        pollutionList.addAll(pollutedChunks);
        operationsPerTick = pollutionList.isEmpty() ? 0 : Math.min(50, pollutionList.size());
    }

    private void tickPollutionInWorld(World world, int tickId) {
        long startNano = System.nanoTime();
        long startMc = System.currentTimeMillis();

        for (int i = 0; i < operationsPerTick && !pollutionList.isEmpty(); i++) {
            ChunkCoordIntPair chunkPos = pollutionList.remove(pollutionList.size() - 1);
            processChunkPollution(world, chunkPos);
        }

        System.out.println(
            "Pollution type: " + pollutionType.getName()
                + " Tick id: "
                + tickId
                + " Dim id: "
                + world.provider.dimensionId
                + " Time used: "
                + (System.currentTimeMillis() - startMc)
                + " mc "
                + (System.nanoTime() - startNano)
                + " nano");
    }

    private void processChunkPollution(World world, ChunkCoordIntPair chunkPos) {
        PollutionData data = PollutionApi.getStorage(pollutionType)
            .get(world, chunkPos);
        AtomicInteger pollution = new AtomicInteger((int) applyNaturalDecay(data.getPollutionAmount()));

        if (pollution.get() > getSpreadThreshold()) {
            synchronized (pollutedChunks) {
                SPREAD_HANDLER.handlePollutionSpread(
                    world,
                    chunkPos,
                    pollution,
                    PollutionApi.getStorage(pollutionType),
                    pollutedChunks);
                // EFFECT_HANDLER.applyPotionEffects(world, chunkPos, pollution.get());
                // DAMAGE_HANDLER.applyDamageEffects(world, chunkPos, pollution.get());
                PollutionBiomeChangeHandler.changeChunkBiome(world, chunkPos, pollutionType.getBiome());
                setChunkPollution(world, chunkPos, pollution.get());
                PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution.get(), pollutionType);
            }
        }
    }

    private float applyNaturalDecay(int pollution) {
        return getNaturalDecayRate() * pollution;
    }
    // endregion
}
