package gregtech.common.pollutionWork.Api;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.pollutionWork.ApiRenders.AbstractPollutionRenderer;
import gregtech.common.pollutionWork.PollutionTypes.Radioactivity.PollutionRadioactivityRenderer;
import gregtech.common.pollutionWork.PollutionTypes.Smog.PollutionSmogRenderer;
import net.minecraft.world.World;

import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.common.pollutionWork.PollutionTypes.Radioactivity.PollutionRadioactivity;
import gregtech.common.pollutionWork.PollutionTypes.Smog.PollutionSmog;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public enum PollutionType {

    // spotless:off
    SMOG("Smog",
        PollutionSmog::new,
        PollutionSmog::onWorldTick,
        PollutionSmogRenderer::new),
    RADIOACTIVITY("RadioActivity",
        PollutionRadioactivity::new,
        PollutionRadioactivity::onWorldTick,
        PollutionRadioactivityRenderer::new);

    private final String pollutionType;
    private final Int2ObjectOpenHashMap<AbstractPollution> dimensionWisePollution;
    private final BiFunction<World, PollutionType, AbstractPollution> factory;
    private final BiConsumer<TickEvent.WorldTickEvent, PollutionType> tickMethod;
    private final Function<PollutionType, AbstractPollutionRenderer> rendererFactory;
    private PollutionStorage storage;

    PollutionType(String pollutionType,
                  BiFunction<World, PollutionType, AbstractPollution> factory,
                  BiConsumer<TickEvent.WorldTickEvent, PollutionType> tickMethod,
                  Function<PollutionType, AbstractPollutionRenderer> rendererFactory) {
        this.pollutionType = pollutionType;
        this.dimensionWisePollution = new Int2ObjectOpenHashMap<>(16);
        this.factory = factory;
        this.tickMethod = tickMethod;
        this.rendererFactory = rendererFactory;
    }
    // spotless:on

    public String getPollutionType() {
        return pollutionType;
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

    @SideOnly(Side.CLIENT)
    public AbstractPollutionRenderer createRenderer() {
        if (rendererFactory == null) return null;
        return rendererFactory.apply(this);
    }
}
