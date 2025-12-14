package gregtech.common.pollutionWork.ApiRenders;

public enum PollutionRenderPriority {

    LOW(0),
    MEDIUM(1),
    HIGH(2);

    private final int priorityLevel;

    PollutionRenderPriority(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }
}
