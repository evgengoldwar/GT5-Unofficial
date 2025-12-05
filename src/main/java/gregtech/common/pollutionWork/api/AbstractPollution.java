package gregtech.common.pollutionWork.api;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import gregtech.common.pollutionWork.handlers.PollutionEventHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.gtnewhorizon.gtnhlib.capability.Capabilities;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.GTMod;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.ICleanroomReceiver;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.pollutionWork.handlers.PollutionEffectHandler;
import gregtech.common.pollutionWork.handlers.PollutionNetworkHandler;
import gregtech.common.pollutionWork.handlers.PollutionSpreadHandler;
import net.minecraftforge.common.MinecraftForge;

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
            getStorage(pollutionType).loadAll(world);
        }
    }
    // endregion

    // region Getters
    public Set<ChunkCoordIntPair> getPollutedChunks() {
        return pollutedChunks;
    }

    public static int getPollution(Chunk chunk, PollutionType type) {
        return getPollution(chunk.worldObj, chunk.xPosition, chunk.zPosition, type);
    }

    public static int getPollution(World world, int chunkX, int chunkZ, PollutionType type) {
        if (world.isRemote) {
            return GTMod.clientProxy().mPollutionRenderer.getKnownPollution(chunkX << 4, chunkZ << 4);
        }
        return getStorage(type).get(world, chunkX, chunkZ)
            .getPollutionAmount();
    }

    public static PollutionStorage getStorage(PollutionType type) {
        return type.getStorage();
    }
    // endregion

    // region Setters
    private void setChunkPollution(ChunkCoordIntPair cord, int pollution) {
        getStorage(pollutionType).mutatePollution(
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
        PollutionData data = getStorage(pollutionType).get(world, chunkPos);
        AtomicInteger pollution = new AtomicInteger((int) applyNaturalDecay(data.getPollutionAmount()));

        if (pollution.get() > getSpreadThreshold()) {
            SPREAD_HANDLER.handlePollutionSpread(world, chunkPos, pollution, getStorage(pollutionType), pollutedChunks);
            EFFECT_HANDLER.applyPollutionEffects(world, chunkPos, pollution.get());
        }

        setChunkPollution(chunkPos, pollution.get());
        PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution.get());
    }

    private float applyNaturalDecay(int pollution) {
        return getNaturalDecayRate() * pollution;
    }

    public static void addPollution(World world, int chunkX, int chunkZ, int pollution, PollutionType pollutionType) {
        if (pollution == 0 || world.isRemote) return;
        getStorage(pollutionType)
            .mutatePollution(world, chunkX, chunkZ, data -> data.changePollutionAmount(pollution), null);
    }

    public static void addPollution(IGregTechTileEntity te, int pollution, PollutionType pollutionType) {
        addPollution((TileEntity) te, pollution, pollutionType);
    }

    public static void addPollution(TileEntity te, int pollution, PollutionType pollutionType) {
        if (pollution == 0 || te.getWorldObj().isRemote) return;

        if (pollution > 0) {
            handleCleanroomPollution(te);
        }

        addPollution(te.getWorldObj(), te.xCoord >> 4, te.zCoord >> 4, pollution, pollutionType);
    }

    public static void addPollution(Chunk chunk, int pollution, PollutionType pollutionType) {
        addPollution(chunk.worldObj, chunk.xPosition, chunk.zPosition, pollution, pollutionType);
    }

    private static void handleCleanroomPollution(TileEntity te) {
        ICleanroomReceiver receiver = Capabilities.getCapability(te, ICleanroomReceiver.class);
        if (receiver != null) {
            ICleanroom cleanroom = receiver.getCleanroom();
            if (cleanroom != null && cleanroom.isValidCleanroom()) {
                cleanroom.pollute();
            }
        }
    }

    public static boolean hasPollution(Chunk chunk, PollutionType type) {
        return getStorage(type).isCreated(chunk.worldObj, chunk.getChunkCoordIntPair()) && getStorage(type).get(chunk)
            .getPollutionAmount() > 0;
    }
    // endregion
}
