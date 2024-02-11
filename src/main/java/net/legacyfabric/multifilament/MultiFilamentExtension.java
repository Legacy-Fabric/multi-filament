package net.legacyfabric.multifilament;

import net.fabricmc.filament.FilamentExtension;
import net.fabricmc.filament.task.DownloadTask;
import net.legacyfabric.multifilament.provider.IntermediaryProvider;
import net.legacyfabric.multifilament.provider.ProviderHandler;
import net.legacyfabric.multifilament.task.DeduplicateMappingsTask;
import net.legacyfabric.multifilament.task.UnifyMappingsTask;
import net.legacyfabric.multifilament.task.VersionifyExcludeMappingsTask;
import net.legacyfabric.multifilament.task.VersionifyFilterMappingsTask;
import net.legacyfabric.multifilament.util.VersionHelper;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.io.File;

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

    private IntermediaryProvider intermediaryProvider;

    @Inject
    public MultiFilamentExtension() {
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
            downloadTask.setGroup(getBuildMappingGroup().get());
            downloadTask.getUrl().set(
                    getIntermediaryProvider()
                            .getIntermediaryURL(getMinecraftVersion().get())
            );
            downloadTask.getOutput().set(getMinecraftCacheDirectory().get().file(
                    getMinecraftVersion().get() + "-intermediary.tiny"
            ));
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
    }

    public Property<String> getMinecraftVersion() {
        return FilamentExtension.get(getProject()).getMinecraftVersion();
    }

    public IntermediaryProvider getIntermediaryProvider() {
        if (intermediaryProvider == null) {
            intermediaryProvider = ProviderHandler.get(getMinecraftVersion().get(), getIntermediaryRevision().get());
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

    public boolean isServerBundled() {
        return VersionHelper.isServerBundled(getMinecraftVersion().get());
    }
}
