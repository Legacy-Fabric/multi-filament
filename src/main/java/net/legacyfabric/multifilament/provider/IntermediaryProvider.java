package net.legacyfabric.multifilament.provider;

import org.gradle.internal.impldep.com.google.gson.Gson;
import org.gradle.internal.impldep.com.google.gson.JsonArray;
import org.gradle.internal.impldep.com.google.gson.JsonElement;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public interface IntermediaryProvider {
    String getIntermediaryURL(String mcVersion);
    boolean supportVersion(String mcVersion);

    class FabricLikeMetadata {
        private final String url;
        private final List<String> versions = new ArrayList<>();
        private static final Gson gson = new Gson();

        public FabricLikeMetadata(String url) {
            this.url = url;
        }

        private void computeData() {
            try {
                URL url1 = new URL(url);
                try (InputStream stream = url1.openStream()) {
                    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        JsonArray array = gson.fromJson(reader, JsonArray.class);

                        for (JsonElement element : array) {
                            if (element instanceof JsonObject object) {
                                if (object.has("version")) {
                                    String version = object.get("version").getAsString();

                                    if (!versions.contains(version)) versions.add(version);
                                }
                            }
                        }
                    }
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
