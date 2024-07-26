import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import java.nio.file.Files
import net.neoforged.moddevgradle.internal.RunGameTask
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
  id("net.neoforged.moddev")
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.configurateTransformations)
}

neoForge {
  version = libs.versions.neoforge

  validateAccessTransformers = true

  runs {
    register("client") {
      client()
      mods.set(HashSet())
    }
    register("server") {
      server()
      mods.set(HashSet())
    }
  }

  mods {
    register("adventure-platform-neoforge-tester") {
      sourceSet(sourceSets.main.get())
    }
  }
}

tasks.withType(RunGameTask::class.java).configureEach {
  dependsOn(tasks.jar)
  doFirst {
    val jar = file("run/mods/main.jar")
    jar.parentFile.mkdirs()
    if (jar.exists()) jar.delete()
    Files.copy(tasks.jar.get().archiveFile.get().asFile.toPath(), jar.toPath())
  }
}

dependencies {
  implementation(project(":adventure-platform-neoforge"))
  jarJar(project(":adventure-platform-neoforge"))
  checkstyle(libs.stylecheck)
}

// Convert yaml files to josn
val generatedResourcesDir = project.layout.buildDirectory.dir("generated-resources").get().asFile
fun createProcessResourceTemplates(name: String, set: SourceSet): TaskProvider<out Task> {
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
  eclipse.synchronizationTasks(task)
  rootProject.idea.project.settings.taskTriggers {
    afterSync(task)
  }
  idea.module.generatedSourceDirs.add(destinationDir)

  return task
}

val generateTemplates = createProcessResourceTemplates("generateTemplates", sourceSets.main.get())

tasks.named("createMinecraftArtifacts").configure {
  dependsOn(generateTemplates)
}
