package tectech.mechanics.pipe;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Created by Tec on 26.02.2017.
 */
public interface IConnectsToDataPipe {

    boolean canConnectData(ForgeDirection side);

    IConnectsToDataPipe getNext(IConnectsToDataPipe source);

    boolean isDataInputFacing(ForgeDirection side);

    byte getColorization();
}
