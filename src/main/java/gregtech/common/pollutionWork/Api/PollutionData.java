package gregtech.common.pollutionWork.Api;

import gregtech.api.util.GTChunkAssociatedData;
import gregtech.api.util.GTUtility;

public class PollutionData implements GTChunkAssociatedData.IData {

    private int pollutionAmount;

    public PollutionData() {
        this(0);
    }

    public PollutionData(int initialAmount) {
        this.pollutionAmount = calculateSafeAmount(initialAmount);
    }

    public int getPollutionAmount() {
        return pollutionAmount;
    }

    public void setPollutionAmount(int pollutionAmount) {
        this.pollutionAmount = pollutionAmount;
    }

    public void changePollutionAmount(int delta) {
        this.pollutionAmount = calculateSafeAmount(GTUtility.safeInt(pollutionAmount + (long) delta));
    }

    @Override
    public boolean isSameAsDefault() {
        return pollutionAmount == 0;
    }

    private static int calculateSafeAmount(int amount) {
        return Math.max(amount, 0);
    }
}
