package gregtech.common.pollutionRework;

import gregtech.api.util.GTChunkAssociatedData;
import gregtech.api.util.GTUtility;

public final class PollutionData implements GTChunkAssociatedData.IData {

    private int pollutionAmount;

    public PollutionData() {
        this(0);
    }

    public PollutionData(int initialAmount) {
        this.pollutionAmount = calculateSafeAmount(initialAmount);
    }

    public int getAmount() {
        return pollutionAmount;
    }

    public void setAmount(int newAmount) {
        this.pollutionAmount = calculateSafeAmount(newAmount);
    }

    public void changeAmount(int delta) {
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
