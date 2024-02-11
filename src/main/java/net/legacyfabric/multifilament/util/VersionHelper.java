package net.legacyfabric.multifilament.util;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VersionHelper {
    public static SemanticVersion parseMCVersion(String version) {
        return MCVersionLookup.parse(version);
    }

    public static SemanticVersion parseVersion(String version) {
        try {
            return SemanticVersion.parse(version);
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    public static VersionPredicate parsePredicate(String predicate) {
        try {
            return VersionPredicate.parse(predicate);
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    public static VersionPredicate parseMCPredicate(String[] ...predicates) {
        String fullPredicate = Arrays.stream(predicates).map(predicate -> {
            String sign = predicate[0];
            String mcVersion = predicate[1];

            return sign + parseMCVersion(mcVersion).getFriendlyString();
        }).collect(Collectors.joining(" "));

        return parsePredicate(fullPredicate);
    }

    @SafeVarargs
    public static VersionPredicate parseMCPredicate(List<String> ...predicates) {
        String fullPredicate = Arrays.stream(predicates).map(predicate -> {
            String sign = predicate.get(0);
            String mcVersion = predicate.get(1);

            return sign + parseMCVersion(mcVersion).getFriendlyString();
        }).collect(Collectors.joining(" "));

        return parsePredicate(fullPredicate);
    }

    public static boolean isServerBundled(String mcVersion) {
        VersionPredicate predicate = parseMCPredicate(new String[]{
                ">", "21w38a"
        });

        SemanticVersion version = parseMCVersion(mcVersion);

        return predicate.test(version);
    }
}
