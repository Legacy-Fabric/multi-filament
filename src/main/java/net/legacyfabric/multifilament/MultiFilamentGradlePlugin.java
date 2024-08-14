package net.legacyfabric.multifilament;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.fabricmc.filament.FilamentGradlePlugin;

import net.fabricmc.filament.task.CombineUnpickDefinitionsTask;
import net.legacyfabric.multifilament.task.FixedRemapUnpickDefinitionsTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

public class MultiFilamentGradlePlugin implements Plugin<Project> {
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(FilamentGradlePlugin.class);

		final MultiFilamentExtension extension = project.getExtensions().create("multiFilament", MultiFilamentExtension.class);
		extension.createConfigurations();
		extension.createTasks();

		TaskContainer tasks = project.getTasks();
		CombineUnpickDefinitionsTask combineUnpickDefinitions = (CombineUnpickDefinitionsTask) tasks.getByName("combineUnpickDefinitions");
		tasks.register("fixedRemapUnpickDefinitionsIntermediary", FixedRemapUnpickDefinitionsTask.class, (task) -> {
			task.setGroup("multi-filament");
			task.dependsOn(combineUnpickDefinitions);
			task.getInput().set(combineUnpickDefinitions.getOutput());
			task.getSourceNamespace().set("named");
			task.getTargetNamespace().set("intermediary");
		});

		project.afterEvaluate(p -> {
			extension.createTasksLate();
		});
	}
}
