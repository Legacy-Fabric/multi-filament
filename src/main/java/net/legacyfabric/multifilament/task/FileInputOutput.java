package net.legacyfabric.multifilament.task;

import net.fabricmc.filament.task.base.WithFileInput;
import net.fabricmc.filament.task.base.WithFileOutput;
import org.gradle.api.DefaultTask;

public abstract class FileInputOutput extends DefaultTask implements WithFileInput, WithFileOutput {
}
