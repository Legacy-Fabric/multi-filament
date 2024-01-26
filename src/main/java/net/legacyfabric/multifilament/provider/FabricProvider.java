package net.legacyfabric.multifilament.provider;

public class FabricProvider implements IntermediaryProvider {
    final FabricLikeMetadata metadata = new FabricLikeMetadata("https://meta.fabricmc.net/v2/versions/game");

    @Override
    public String getIntermediaryURL(String mcVersion) {
        return "https://github.com/FabricMC/intermediary/raw/master/mappings/" + mcVersion + ".tiny";
    }

    @Override
    public boolean supportVersion(String mcVersion) {
        return metadata.contains(mcVersion);
    }
}
