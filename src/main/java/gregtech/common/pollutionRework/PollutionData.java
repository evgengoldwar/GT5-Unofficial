package gregtech.common.pollutionRework;

import gregtech.api.util.GTChunkAssociatedData;
import gregtech.api.util.GTUtility;

public class PollutionData implements GTChunkAssociatedData.IData {

    private int amount;

    public PollutionData() {
        this(0);
    }

    public PollutionData(int amount) {
        this.amount = Math.max(0, amount);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(amount, 0);
    }

    public void changeAmount(int delta) {
        this.amount = Math.max(GTUtility.safeInt(amount + (long) delta, 0), 0);
    }

    @Override
    public boolean isSameAsDefault() {
        return amount == 0;
    }
}
