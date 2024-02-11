package net.legacyfabric.multifilament.provider;

import java.util.ArrayList;
import java.util.List;

public class ProviderHandler {
    private static final List<IntermediaryProvider> availableProviders = new ArrayList<>();

    static {
        availableProviders.add(new FabricProvider());
        availableProviders.add(new LegacyFabricProvider(1));
        availableProviders.add(new LegacyFabricProvider(2));
    }

    public static IntermediaryProvider get(String mcVersion, int revision) {
        for (IntermediaryProvider provider : availableProviders) {
            if (provider.supportVersion(mcVersion) && provider.revisionMatch(revision)) {
                return provider;
            }
        }

        throw new RuntimeException(mcVersion + " with revision " + revision + " isn't supported by any Intermediary Provider!");
    }
}
