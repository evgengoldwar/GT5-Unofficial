package gregtech.common.pollutionWork.api;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import net.minecraft.world.World;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.pollutionWork.PollutionRadioactivity;
import gregtech.common.pollutionWork.PollutionSmog;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public enum PollutionType {

    // spotless:off
    SMOG("Smog",
        PollutionSmog::new,
        PollutionSmog::onWorldTick,
        null),
    RADIOACTIVITY("RadioActivity",
        PollutionRadioactivity::new,
        PollutionRadioactivity::onWorldTick,
        null);

    private final String pollutionType;
    private final Int2ObjectOpenHashMap<AbstractPollution> dimensionWisePollution;
    private final BiFunction<World, PollutionType, AbstractPollution> factory;
    private final BiConsumer<TickEvent.WorldTickEvent, PollutionType> tickMethod;
    private final AbstractPollutionRenderer pollutionRenderer;
    private PollutionStorage storage;

    PollutionType(String pollutionType,
                  BiFunction<World, PollutionType, AbstractPollution> factory,
                  BiConsumer<TickEvent.WorldTickEvent, PollutionType> tickMethod,
                  AbstractPollutionRenderer pollutionRenderer) {
        this.pollutionType = pollutionType;
        this.dimensionWisePollution = new Int2ObjectOpenHashMap<>(16);
        this.factory = factory;
        this.tickMethod = tickMethod;
        this.pollutionRenderer = pollutionRenderer;
    }
    // spotless:on

    public String getPollutionType() {
        return pollutionType;
    }

    public AbstractPollutionRenderer getPollutionRenderer() {
        return pollutionRenderer;
    }

    public Int2ObjectOpenHashMap<AbstractPollution> getDimensionWisePollution() {
        return dimensionWisePollution;
    }

    public AbstractPollution createPollutionInstance(World world) {
        return factory.apply(world, this);
    }

    public void callOnWorldTick(TickEvent.WorldTickEvent event, PollutionType type) {
        tickMethod.accept(event, type);
    }

    public PollutionStorage getStorage() {
        if (storage == null) {
            storage = new PollutionStorage(this);
        }
        return storage;
    }
}
