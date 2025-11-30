package gregtech.common.pollutionRework;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizon.gtnhlib.capability.Capabilities;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.GTMod;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.ICleanroomReceiver;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.pollution.PollutionConfig;
import gregtech.common.pollution.PollutionTooltip;
import gregtech.common.pollutionRework.handlers.PollutionEffectHandler;
import gregtech.common.pollutionRework.handlers.PollutionEventHandler;
import gregtech.common.pollutionRework.handlers.PollutionNetworkHandler;
import gregtech.common.pollutionRework.handlers.PollutionSpreadHandler;

@ParametersAreNonnullByDefault
public class Pollution {

    private static final PollutionStorage STORAGE = new PollutionStorage();
    private static final PollutionEffectHandler EFFECT_HANDLER = new PollutionEffectHandler();
    private static final PollutionSpreadHandler SPREAD_HANDLER = new PollutionSpreadHandler();
    private static PollutionEventHandler EVENT_HANDLER;

    private final World world;
    private final Set<ChunkCoordIntPair> pollutedChunks = new HashSet<>();
    private List<ChunkCoordIntPair> pollutionList = new ArrayList<>();
    private int operationsPerTick = 0;
    private boolean blank = true;

    private static final short CYCLE_LENGTH = 1200;
    private static final int POLLUTION_PACKET_MIN_VALUE = 1000;
    private static final float NATURAL_DECAY_RATE = 0.9945f;
    private static final int SPREAD_THRESHOLD = 400000;

    public static BlockMatcherRework standardBlocks;
    public static BlockMatcherRework liquidBlocks;
    public static BlockMatcherRework doublePlants;
    public static BlockMatcherRework crossedSquares;
    public static BlockMatcherRework blockVine;

    public static void onPostInitClient() {
        standardBlocks = createAndRegisterBlockMatcher(PollutionConfig.renderStandardBlock);
        liquidBlocks = createAndRegisterBlockMatcher(PollutionConfig.renderBlockLiquid);
        doublePlants = createAndRegisterBlockMatcher(PollutionConfig.renderBlockDoublePlant);
        crossedSquares = createAndRegisterBlockMatcher(PollutionConfig.renderCrossedSquares);
        blockVine = createAndRegisterBlockMatcher(PollutionConfig.renderblockVine);

        MinecraftForge.EVENT_BUS.register(new PollutionTooltip());
    }

    private static BlockMatcherRework createAndRegisterBlockMatcher(String[] configArray) {
        BlockMatcherRework matcher = new BlockMatcherRework();
        matcher.updateClassList(configArray);
        MinecraftForge.EVENT_BUS.register(matcher);
        return matcher;
    }

    public Pollution(World world) {
        this.world = world;
        GTMod.proxy.dimensionWisePollutionRework.put(world.provider.dimensionId, this);

        if (EVENT_HANDLER == null) {
            EVENT_HANDLER = new PollutionEventHandler();
            MinecraftForge.EVENT_BUS.register(EVENT_HANDLER);
        }

        if (!world.isRemote) {
            STORAGE.loadAll(world);
        }
    }

    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;

        final Pollution pollutionInstance = GTMod.proxy.dimensionWisePollutionRework
            .computeIfAbsent(event.world.provider.dimensionId, i -> new Pollution(event.world));

        pollutionInstance.tickPollutionInWorld((int) (event.world.getTotalWorldTime() % CYCLE_LENGTH));
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
            operationsPerTick = pollutionList.isEmpty() ? 0 : Math.max(1, pollutionList.size() / CYCLE_LENGTH);
            blank = false;
        }
    }

    private void processChunkPollution(ChunkCoordIntPair chunkPos) {
        PollutionData data = STORAGE.get(world, chunkPos);
        int pollution = applyNaturalDecay(data.getAmount());

        if (pollution > SPREAD_THRESHOLD) {
            SPREAD_HANDLER.handlePollutionSpread(world, chunkPos, pollution, STORAGE);
            EFFECT_HANDLER.applyPollutionEffects(world, chunkPos, pollution);
        }

        setChunkPollution(chunkPos, pollution);
        sendPollutionUpdateIfNeeded(chunkPos, pollution);
    }

    private int applyNaturalDecay(int pollution) {
        return (int) (NATURAL_DECAY_RATE * pollution);
    }

    private void setChunkPollution(ChunkCoordIntPair coord, int pollution) {
        STORAGE.mutatePollution(
            world,
            coord.chunkXPos,
            coord.chunkZPos,
            data -> data.setAmount(pollution),
            pollutedChunks);
    }

    private void sendPollutionUpdateIfNeeded(ChunkCoordIntPair chunkPos, int pollution) {
        if (pollution > POLLUTION_PACKET_MIN_VALUE) {
            PollutionNetworkHandler.sendPollutionUpdate(world, chunkPos, pollution);
        }
    }

    public static void addPollution(World world, int chunkX, int chunkZ, int pollution) {
        if (pollution == 0 || world.isRemote) return;
        STORAGE.mutatePollution(world, chunkX, chunkZ, data -> data.changeAmount(pollution), null);
    }

    public static void addPollution(IGregTechTileEntity te, int pollution) {
        addPollution((TileEntity) te, pollution);
    }

    public static void addPollution(TileEntity te, int pollution) {
        if (pollution == 0 || te.getWorldObj().isRemote) return;

        if (pollution > 0) {
            handleCleanroomPollution(te);
        }

        addPollution(te.getWorldObj(), te.xCoord >> 4, te.zCoord >> 4, pollution);
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

    public static void addPollution(Chunk chunk, int pollution) {
        addPollution(chunk.worldObj, chunk.xPosition, chunk.zPosition, pollution);
    }

    public static int getPollution(World world, int chunkX, int chunkZ) {
        if (world.isRemote) {
            return GTMod.clientProxy().mPollutionRenderer.getKnownPollution(chunkX << 4, chunkZ << 4);
        }
        return STORAGE.get(world, chunkX, chunkZ)
            .getAmount();
    }

    public static PollutionStorage getStorage() {
        return STORAGE;
    }

    public static int getPollution(Chunk chunk) {
        return getPollution(chunk.worldObj, chunk.xPosition, chunk.zPosition);
    }

    public static boolean hasPollution(Chunk chunk) {
        return STORAGE.isCreated(chunk.worldObj, chunk.getChunkCoordIntPair()) && STORAGE.get(chunk)
            .getAmount() > 0;
    }

    public Set<ChunkCoordIntPair> getPollutedChunks() {
        return pollutedChunks;
    }
}
