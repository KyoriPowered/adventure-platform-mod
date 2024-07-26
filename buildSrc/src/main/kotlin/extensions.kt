import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.expand
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

val Project.libs: LibrariesForLibs
  get() = extensions.getByType()

// Convert yaml files to josn
fun Project.createProcessResourceTemplates(name: String, set: SourceSet): TaskProvider<out Task> {
  val generatedResourcesDir = layout.buildDirectory.dir("generated-resources").get().asFile
  val destinationDir = generatedResourcesDir.resolve(name)
  val task = tasks.register(name, Sync::class.java) {
    this.destinationDir = destinationDir
    from("src/${set.name}/resource-templates")

    inputs.property("version", project.version)

    // Convert data files yaml -> json
    filesMatching(
      sequenceOf(
        "fabric.mod",
        "data/**/*",
        "assets/**/*"
      ).flatMap { base -> sequenceOf("$base.yml", "$base.yaml") }
        .toList()
    ) {
      convertFormat(ConfigFormats.YAML, ConfigFormats.JSON)
      if (this.name.startsWith("fabric.mod")) {
        expand("project" to project)
      }
      this.name = this.name.substringBeforeLast('.') + ".json"
    }
    // Convert pack meta, without changing extension
    filesMatching("pack.mcmeta") { convertFormat(ConfigFormats.YAML, ConfigFormats.JSON) }
  }

  tasks.named(set.processResourcesTaskName) {
    dependsOn(name)
  }
  set.resources.srcDir(task.map { it.outputs })

  // Have templates ready for IDEs
  extensions.getByType(EclipseModel::class).synchronizationTasks(task)
  rootProject.extensions.getByType(IdeaModel::class).project.settings.taskTriggers {
    afterSync(task)
  }
  extensions.getByType(IdeaModel::class).module.generatedSourceDirs.add(destinationDir)

  return task
}
