package gregtech.common.pollutionRework.Api;

import java.util.*;

public class PollutionRegistry {

    private static final Map<String, PollutionType> REGISTERED_POLLUTIONS = new HashMap<>();

    public static void registerPollution(PollutionType pollutionType) {
        REGISTERED_POLLUTIONS.put(pollutionType.getName(), pollutionType);
    }

    public static PollutionType getPollutionType(String name) {
        return REGISTERED_POLLUTIONS.get(name);
    }

    public static List<PollutionType> getAllPollutions() {
        return new ArrayList<>(REGISTERED_POLLUTIONS.values());
    }
}
