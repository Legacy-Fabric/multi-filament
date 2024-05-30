package net.legacyfabric.multifilament;

import net.fabricmc.filament.FilamentExtension;
import net.fabricmc.filament.task.DownloadTask;
import net.fabricmc.filament.task.MapJarTask;
import net.fabricmc.filament.task.enigma.MapSpecializedMethodsTask;
import net.fabricmc.filament.task.mappingio.CompleteMappingsTask;
import net.fabricmc.filament.task.mappingio.ConvertMappingsTask;
import net.fabricmc.filament.task.mappingio.MergeMappingsTask;
import net.fabricmc.filament.task.minecraft.ExtractBundledServerTask;
import net.fabricmc.filament.task.minecraft.MergeMinecraftTask;
import net.fabricmc.mappingio.format.MappingFormat;
import net.legacyfabric.multifilament.provider.IntermediaryProvider;
import net.legacyfabric.multifilament.provider.ProviderHandler;
import net.legacyfabric.multifilament.task.*;
import net.legacyfabric.multifilament.util.VersionHelper;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Jar;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public abstract class MultiFilamentExtension {
    public static MultiFilamentExtension get(Project project) {
        return project.getExtensions().getByType(MultiFilamentExtension.class);
    }

    @Inject
    protected abstract Project getProject();

    public abstract Property<Integer> getIntermediaryRevision();
    public abstract DirectoryProperty getActiveMappingsDir();
    public abstract DirectoryProperty getMultiMappingsDir();
    public abstract DirectoryProperty getTempMappingsDir();
    public abstract Property<String> getYarnGroup();
    public abstract Property<String> getBuildMappingGroup();
    public abstract Property<String> getMapJarGroupGroup();
    public abstract Property<String> getVersion();

    private IntermediaryProvider intermediaryProvider;
    private final Property<String> minecraftVersion;

    @Inject
    public MultiFilamentExtension() {
        minecraftVersion = FilamentExtension.get(getProject()).getMinecraftVersion();
        getIntermediaryRevision().finalizeValueOnRead();
        getActiveMappingsDir().finalizeValueOnRead();
        getMultiMappingsDir().finalizeValueOnRead();
        getTempDirectory().finalizeValueOnRead();
        getYarnGroup().finalizeValueOnRead();
        getBuildMappingGroup().finalizeValueOnRead();
        getMapJarGroupGroup().finalizeValueOnRead();
    }

    protected void createTasks() {
        DownloadTask downloadIntermediary = getProject().getTasks().create("downloadIntermediary", DownloadTask.class, downloadTask -> {
//            downloadTask.setGroup(getBuildMappingGroup().get());
            downloadTask.setGroup("mapping build");
            downloadTask.getUrl().set(
                    minecraftVersionGetter().map(v -> getIntermediaryProvider().getIntermediaryURL(v))
            );
            downloadTask.getOutput().set(getMinecraftCacheDirectory().flatMap(
                    dir -> minecraftVersionGetter().map(v -> dir.file(v + "-intermediary.tiny")))
            );
        });

        VersionifyFilterMappingsTask versionifyMappingsFilter = getProject().getTasks().create("versionifyMappingsFilter", VersionifyFilterMappingsTask.class, task -> {
            task.dependsOn(downloadIntermediary);
            task.getIntermediaryFile().set(downloadIntermediary.getOutputFile());
            task.getInputDir().set(getMultiMappingsDir());
            task.getOutputDir().set(getActiveMappingsDir());
            task.getOutputs().upToDateWhen(c -> false);
        });

        VersionifyExcludeMappingsTask versionifyMappingsExclude = getProject().getTasks().create("versionifyMappingsExclude", VersionifyExcludeMappingsTask.class, task -> {
            task.dependsOn(downloadIntermediary, versionifyMappingsFilter);
            task.getIntermediaryFile().set(downloadIntermediary.getOutputFile());
            task.getInputDir().set(getMultiMappingsDir());
            task.getOutputDir().set(getTempMappingsDir());
            task.getOutputs().upToDateWhen(c -> false);
        });

        DeduplicateMappingsTask deduplicateMappings = getProject().getTasks().create("deduplicateMappings", DeduplicateMappingsTask.class, task -> {
            task.getInputDir().set(getMultiMappingsDir());
            task.getOutputDir().set(getTempMappingsDir());
        });

        UnifyMappingsTask unifyMappings = getProject().getTasks().create("unifyMappings", UnifyMappingsTask.class, task -> {
            task.dependsOn(versionifyMappingsExclude);
            task.getUnifiedDir().set(getTempMappingsDir());
            task.getVersionedDir().set(getActiveMappingsDir());
            task.getOutputDir().set(getMultiMappingsDir());
            task.getOutputs().upToDateWhen(c -> false);
        });

        UnifyMappingsTask unifyMappingsFix = getProject().getTasks().create("unifyMappingsFix", UnifyMappingsTask.class, task -> {
            task.getUnifiedDir().set(getTempMappingsDir());
            task.getVersionedDir().set(getActiveMappingsDir());
            task.getOutputDir().set(getMultiMappingsDir());
            task.getOutputs().upToDateWhen(c -> false);
        });

        MergeMinecraftTask mergeOriginalJars = getProject().getTasks().create("mergeOriginalMinecraftJars", MergeMinecraftTask.class, task -> {
            DownloadTask clientJar = (DownloadTask) getProject().getTasks().getByName("downloadMinecraftClientJar");
            DownloadTask serverJar = (DownloadTask) getProject().getTasks().getByName("downloadMinecraftServerJar");

            task.dependsOn(clientJar, serverJar);
            task.getClientJar().set(clientJar.getOutput());
            task.getServerJar().set(serverJar.getOutput());

            task.getOutput().set(FilamentExtension.get(getProject()).getMinecraftFile("merged.jar"));
        });

        MapJarTask mapIntermediaryJar = getProject().getTasks().create("mapIntermediaryJar", MapJarTask.class, task -> {
            MergeMinecraftTask mergeJars = (MergeMinecraftTask) getProject().getTasks().getByName("mergeMinecraftJars");

            task.dependsOn(isServerBundled() ? mergeJars : mergeOriginalJars);
//            task.setGroup(getMapJarGroupGroup().get());
            task.setGroup("jar mappings");
            task.getOutput().set(minecraftVersionGetter().flatMap(v -> getCacheFile(v + "-intermediary.jar")));
            task.getInput().set(isServerBundled() ? mergeJars.getOutput() : mergeOriginalJars.getOutput());
            task.getMappings().set(downloadIntermediary.getOutput());
            task.getClasspath().from(getMinecraftLibsConfiguration());
            task.getFrom().set("official");
            task.getTo().set("intermediary");
        });

        MapJarTask mapServerIntermediaryJar = getProject().getTasks().create("mapServerIntermediaryJar", MapJarTask.class, task -> {
            ExtractBundledServerTask extractBundledServer = (ExtractBundledServerTask) getProject().getTasks().getByName("extractBundledServer");
            DownloadTask serverJar = (DownloadTask) getProject().getTasks().getByName("downloadMinecraftServerJar");

            task.dependsOn(isServerBundled() ? extractBundledServer : serverJar);
//            task.setGroup(getMapJarGroupGroup().get());
            task.setGroup("jar mappings");
            task.getOutput().set(minecraftVersionGetter().flatMap(v -> getCacheFile(v + "-server-intermediary.jar")));
            task.getInput().set(isServerBundled() ? extractBundledServer.getOutput() : serverJar.getOutput());
            task.getMappings().set(downloadIntermediary.getOutput());
            task.getClasspath().from(getMinecraftLibsConfiguration());
            task.getFrom().set("official");
            task.getTo().set("intermediary");
        });

        MapSpecializedMethodsTask mapSpecializedMethods = getProject().getTasks().create("mapSpecializedMethods", MapSpecializedMethodsTask.class, task -> {
            task.dependsOn(versionifyMappingsExclude);
            task.getIntermediaryJarFile().set(mapIntermediaryJar.getOutput());
            task.getMappings().set(getActiveMappingsDir());
            task.getOutput().set(getTempDirectory().file("yarn-specialized-mappings-v2.tiny"));

            task.getInputMappingsFormat().set("enigma");
            task.getOutputMappingsFormat().set("tinyv2:intermediary:named");
        });

        CompleteMappingsTask completeMappings = getProject().getTasks().create("completeMappings", CompleteMappingsTask.class, task -> {
            task.getInput().set(mapSpecializedMethods.getOutput());
            task.getOutput().set(getTempDirectory().file("yarn-mappings-v2.tiny"));
            task.getOutputFormat().set(MappingFormat.TINY_2_FILE);
        });

        ConvertMappingsTask convertToV1 = getProject().getTasks().create("convertToV1", ConvertMappingsTask.class, task -> {
            task.getInput().set(mapSpecializedMethods.getOutput());
            task.getOutput().set(getTempDirectory().file("yarn-mappings.tiny"));
            task.getOutputFormat().set(MappingFormat.TINY_FILE);
        });

        MergeMappingsTask mergeTiny = getProject().getTasks().create("mergeTiny", MergeMappingsTask.class, task -> {
//            task.setGroup(getBuildMappingGroup().get());
            task.setGroup("mapping build");
            task.getOutput().set(getTempDirectory().file("mappings.tiny"));
            task.getMappingInputs().from(downloadIntermediary.getOutput());
            task.getMappingInputs().from(convertToV1.getOutput());
            task.getOutputFormat().set(MappingFormat.TINY_FILE);
        });

        Action<Manifest> manifestAction = manifest -> {
            manifest.getAttributes().put("Minecraft-Version-Id", minecraftVersionGetter().get());
//            manifest.getAttributes().put("Intermediary-Version", getIntermediaryRevision().get());
        };

        Jar tinyJar = getProject().getTasks().create("tinyJar", Jar.class, task -> {
            task.dependsOn(mergeTiny);
//            task.setGroup(getBuildMappingGroup().get());
            task.setGroup("mapping build");
            task.getArchiveFileName().set(getVersion().map(v -> "yarn-" + v + ".jar"));
            task.getDestinationDirectory().set(getLibsDirectory());
            task.getArchiveClassifier().set("");

            task.from(mergeTiny.getOutput(), from -> {
                from.rename(s -> "mappings/mappings.tiny");
            });

            task.manifest(manifestAction);
        });

        FileInputOutput compressTiny = getProject().getTasks().create("compressTiny", FileInputOutput.class, task -> {
            task.dependsOn(tinyJar, mergeTiny);
//            task.setGroup(getBuildMappingGroup().get());
            task.setGroup("mapping build");

            task.getInput().set(mergeTiny.getOutput());
            task.getOutput().set(getVersion().flatMap(v -> getLibsDirectory().file("yarn-tiny-" + v + ".gz")));

            task.doLast(currentTaskGradle -> {
                try {
                    FileInputOutput currentTask = (FileInputOutput) currentTaskGradle;
                    var buffer = new byte[1024];
                    var fileOutputStream = new FileOutputStream(currentTask.getOutputFile());
                    var outputStream = new GZIPOutputStream(fileOutputStream);
                    var fileInputStream = new FileInputStream(currentTask.getInputFile());

                    int length;
                    while ((length = fileInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }

                    fileInputStream.close();
                    outputStream.finish();
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private Property<String> minecraftVersionGetter() {
        return minecraftVersion;
    }

    public IntermediaryProvider getIntermediaryProvider() {
        if (intermediaryProvider == null) {
            intermediaryProvider = ProviderHandler.get(minecraftVersionGetter().get(), getIntermediaryRevision().get());
        }

        return intermediaryProvider;
    }

    public DirectoryProperty getTempDirectory() {
//        return getProject().getObjects().directoryProperty().fileValue(new File(getProject().getRootDir(), ".gradle/multi-filament"));
        return getProject().getObjects().directoryProperty().fileValue(new File(getProject().getRootDir(), "build/temp/yarn"));
    }

    public Provider<Directory> getMinecraftCacheDirectory() {
//        return getCacheDirectory().dir("minecraft");
        return getTempDirectory().dir("minecraft");
    }

    public DirectoryProperty getLibsDirectory() {
//        return getCacheDirectory().dir("libs");
        return getProject().getObjects().directoryProperty().fileValue(new File(getProject().getRootDir(), "build/libs"));
    }

    public DirectoryProperty getCacheDirectory() {
        return getProject().getObjects().directoryProperty().fileValue(new File(getProject().getRootDir(), ".gradle/multi-filament"));
    }

    public Provider<Directory> getIntermediaryRevisionDirectory() {
        return getCacheDirectory().dir(getIntermediaryRevision().map(r -> "v" + r));
    }

    public Provider<RegularFile> getCacheFile(String filename) {
        return getIntermediaryRevisionDirectory().map(directory -> directory.file(filename));
    }

    private Configuration getMinecraftLibsConfiguration() {
        return getProject().getConfigurations().getByName("minecraftLibraries");
    }

    public boolean isServerBundled() {
        return VersionHelper.isServerBundled(minecraftVersionGetter().get());
    }
}
