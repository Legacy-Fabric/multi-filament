package net.legacyfabric.multifilament.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import net.legacyfabric.multifilament.MultiFilamentGradlePlugin;
import net.legacyfabric.multifilament.util.FabricMetaMCVersion;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public interface IntermediaryProvider {
    String getIntermediaryURL(String mcVersion);
    boolean supportVersion(String mcVersion);
    default boolean revisionMatch(int revision) {
        return true;
    }

    class FabricLikeMetadata {
        private final String url;
        private final List<String> versions = new ArrayList<>();

        public FabricLikeMetadata(String url) {
            this.url = url;
        }

        private void computeData() {
            try {
                TypeReference<List<FabricMetaMCVersion>> metaType = new TypeReference<>() {};
                List<FabricMetaMCVersion> versions = MultiFilamentGradlePlugin.OBJECT_MAPPER.readValue(new URL(url), metaType);

                if (versions != null) {
                    versions.forEach(v -> {
                        if (!this.versions.contains(v.version())) {
                            this.versions.add(v.version());
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public boolean contains(String version) {
            if (versions.isEmpty()) this.computeData();

            return versions.contains(version);
        }
    }
}
