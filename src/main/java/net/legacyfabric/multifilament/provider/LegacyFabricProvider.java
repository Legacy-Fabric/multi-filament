package net.legacyfabric.multifilament.provider;

public class LegacyFabricProvider implements IntermediaryProvider {
    private final int revision;
    final FabricLikeMetadata metadata = new FabricLikeMetadata("https://meta.legacyfabric.net/v2/versions/game");

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
                return "legacy/";
            case 0:
                return "legacy_old/";
            default:
                return "";
        }
    }

    @Override
    public boolean supportVersion(String mcVersion) {
        return metadata.contains(mcVersion);
    }
}
