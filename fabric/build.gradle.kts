import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RunGameTask
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
  alias(libs.plugins.eclipseApt)
  alias(libs.plugins.loom)
  alias(libs.plugins.loomQuiltflower)
  alias(libs.plugins.configurateTransformations)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
  alias(libs.plugins.indra.licenseHeader)
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.ideaExt)
}

quiltflower {
  quiltflowerVersion.set(libs.versions.quiltflower.get())
  preferences(
    "win" to 0
  )
  // addToRuntimeClasspath.set(true)
}

dependencies {
  annotationProcessor(libs.autoService)
  annotationProcessor(libs.contractValidator)
  compileOnlyApi(libs.autoService.annotations)
  implementation(project(":adventure-platform-mod-shared")) {
    exclude(group = "net.minecraft")
  }
  
  sequenceOf(
    libs.adventure.key,
    libs.adventure.api,
    libs.adventure.textLoggerSlf4j,
    libs.adventure.textMinimessage,
    libs.adventure.textSerializerPlain
  ).forEach {
    modApi(it) {
      exclude("org.slf4j", "slf4j-api")
    }
    include(it)
  }

  sequenceOf(
    libs.adventure.platform.api,
    libs.adventure.textSerializerGson
  ).forEach {
   modApi(it) {
     exclude("com.google.code.gson")
   }
   include(it)
  }
  modApi(fabricApi.module("fabric-api-base", libs.versions.fabricApi.get()))

  // Transitive deps
  include(libs.examination.api)
  include(libs.examination.string)
  modCompileOnly(libs.jetbrainsAnnotations)

  modImplementation(libs.fabric.loader)

  // Testmod TODO figure out own scope
  if (gradle.startParameter.taskNames.contains("publish")) {
    modCompileOnly(libs.fabric.api)
  } else {
    modImplementation(libs.fabric.api)
  }

  checkstyle(libs.stylecheck)
}

sourceSets {
  main {
    java.srcDirs(
      "src/accessor/java",
      "src/mixin/java"
    )
    resources.srcDirs(
      "src/accessor/resources/",
      "src/mixin/resources/"
    )
  }
}

loom.splitEnvironmentSourceSets()

// The testmod is not split, not worth the effort
val testmod = sourceSets.register("testmod") {
  compileClasspath += sourceSets.named("client").get().compileClasspath
  runtimeClasspath += sourceSets.named("client").get().runtimeClasspath
  java.srcDirs("src/testmodMixin/java")
  resources.srcDirs("src/testmodMixin/resources")
}

configurations.named("clientAnnotationProcessor") {
  extendsFrom(configurations.annotationProcessor.get())
}

dependencies {
  "testmodImplementation"(sourceSets.named("client").map { it.output })
}

loom {
  runs {
    register("testmodClient") {
      source("testmod")
      client()
    }
    register("testmodServer") {
      source("testmod")
      server()
    }
    configureEach {
      isIdeConfigGenerated = true
    }
  }

  mods {
    register("adventure-platform-fabric") {
      sourceSet(sourceSets.main.get())
      sourceSet(sourceSets.named("client").get())
    }
    register("adventure-platform-fabric-testmod") {
      sourceSet(testmod.get())
    }
  }
  mixin {
    add(sourceSets.main.get(), "adventure-platform-fabric-refmap.json")
    add(sourceSets.named("client").get(), "adventure-platform-fabric-client-refmap.json")
    add(testmod.get(), "adventure-platform-fabric-testmod-refmap.json")
  }
}

// Create a remapped testmod jar
val testmodDevJar = tasks.register("testmodJar", Jar::class) {
  from(testmod.map { it.output })
  archiveClassifier.set("testmod-dev")
}

val remapTestmodJar = tasks.register("remapTestmodJar", RemapJarTask::class) {
  inputFile.set(testmodDevJar.flatMap { it.archiveFile })
  addNestedDependencies.set(false)
  classpath.from(testmod.map { it.runtimeClasspath })
  archiveClassifier.set("testmod")
}

tasks.build {
  dependsOn(remapTestmodJar)
}

tasks.jar {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE // include all service elements
}

// Convert yaml files to josn
val generatedResourcesDir = project.layout.buildDirectory.dir("generated-resources").get().asFile
fun createProcessResourceTemplates(name: String, setProvider: NamedDomainObjectProvider<SourceSet>): TaskProvider<out Task> {
  val destinationDir = generatedResourcesDir.resolve(name)
  val set = setProvider.get()
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

val generateTemplates = createProcessResourceTemplates("generateTemplates", sourceSets.main)
val generateTestmodTemplates = createProcessResourceTemplates("generateTestmodTemplates", testmod)

tasks.withType(GenerateSourcesTask::class).configureEach {
  dependsOn(generateTemplates)
}

// Workaround for both loom and indra doing publication logic in an afterEvaluate :(
indra.includeJavaSoftwareComponentInPublications(false)
publishing {
  publications.named("maven", MavenPublication::class) {
    from(components["java"])
  }
}
