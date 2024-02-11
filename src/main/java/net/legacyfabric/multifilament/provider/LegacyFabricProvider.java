package net.legacyfabric.multifilament.provider;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.legacyfabric.multifilament.util.VersionHelper;

public class LegacyFabricProvider implements IntermediaryProvider {
    private final int revision;
    final FabricLikeMetadata metadata = new FabricLikeMetadata("https://meta.legacyfabric.net/v2/versions/game");

    static final VersionPredicate v2Support = VersionHelper.parseMCPredicate(
            new String[]{"<", "18w43b"},
            new String[]{">=", "1.8.2-pre5"}
    );

    public LegacyFabricProvider(int revision) {
        this.revision = revision;
    }

    @Override
    public String getIntermediaryURL(String mcVersion) {
        return "https://github.com/Legacy-Fabric/Legacy-Intermediaries/raw/v2" + getIntermediaryFolder() + "/mappings/" + mcVersion + ".tiny";
    }

    private String getIntermediaryFolder() {
        switch (revision) {
            case 1:
                return "/legacy";
            case 0:
                return "/legacy_old";
            default:
                return "";
        }
    }

    @Override
    public boolean supportVersion(String mcVersion) {
        if (revision > 1) {
            SemanticVersion version = VersionHelper.parseMCVersion(mcVersion);
            return v2Support.test(version);
        }

        return metadata.contains(mcVersion);
    }

    @Override
    public boolean revisionMatch(int revision) {
        return revision == this.revision;
    }
}
