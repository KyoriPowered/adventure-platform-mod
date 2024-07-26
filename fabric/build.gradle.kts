import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import net.fabricmc.loom.api.RemapConfigurationSettings
import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
  alias(libs.plugins.eclipseApt)
  alias(libs.plugins.loom)
  alias(libs.plugins.configurateTransformations)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
  alias(libs.plugins.indra.licenseHeader)
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.indra.crossdoc)
  alias(libs.plugins.ideaExt)
  id("com.diffplug.spotless")
}

spotless {
  //ratchetFrom("origin/mc/${libs.versions.minecraft.get().splitToSequence('.').take(2).joinToString(".")}")
  ratchetFrom("origin/mc/1.20")

  java {
    importOrderFile(rootProject.file(".spotless/kyori.importorder"))
    trimTrailingWhitespace()
    endWithNewline()
    indentWithSpaces(2)
    removeUnusedImports()
  }
}

dependencies {
  vineflowerDecompilerClasspath(libs.vineflower)
  annotationProcessor(libs.autoService)
  annotationProcessor(libs.contractValidator)
  compileOnlyApi(libs.autoService.annotations)
  sequenceOf(
    libs.adventure.key,
    libs.adventure.api,
    libs.adventure.textLoggerSlf4j,
    libs.adventure.textMinimessage,
    libs.adventure.textSerializerPlain,
    libs.adventure.textSerializerAnsi
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
  sequenceOf<(Any) -> Dependency?>(
    ::modImplementation, ::modApi, ::modCompileOnly
  ).forEach { it(platform(libs.fabric.api.bom)) }
  modApi(libs.fabric.api.base)
  modImplementation(libs.fabric.api.networking)
  modImplementation(libs.fabric.api.command)
  // Only used for prod test
  modCompileOnly(libs.fabric.api.lifecycle)

  // Transitive deps
  include(libs.examination.api)
  include(libs.examination.string)
  include(libs.adventure.textSerializerJson)
  include(libs.ansi)
  include(libs.option)
  modCompileOnly(libs.jetbrainsAnnotations)

  modImplementation(libs.fabric.loader)

  testImplementation(libs.fabric.loader.junit)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.engine)
  testRuntimeOnly(libs.junit.launcher)

  checkstyle(libs.stylecheck)

  localRuntime(project(":adventure-platform-mod-shared"))
  compileOnly(project(":adventure-platform-mod-shared"))
  testImplementation(project(":adventure-platform-mod-shared"))
  api(project(":adventure-platform-fabric:adventure-platform-mod-shared-fabric-repack", configuration = "namedElements"))
  include(project(":adventure-platform-fabric:adventure-platform-mod-shared-fabric-repack"))
}

configurations {
  runtimeClasspath {
    extendsFrom(vineflowerDecompilerClasspath.get())
    exclude("net.kyori", "adventure-platform-mod-shared-fabric-repack")
  }
  compileClasspath {
    exclude("net.kyori", "adventure-platform-mod-shared-fabric-repack")
  }
  testRuntimeClasspath {
    exclude("net.kyori", "adventure-platform-mod-shared-fabric-repack")
  }
  testCompileClasspath {
    exclude("net.kyori", "adventure-platform-mod-shared-fabric-repack")
  }
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

// create a secondary set, with a configuration name matching the source set name
// this configuration is available at compile- and runtime, and not published
fun createSecondarySet(name: String, action: Action<SourceSet> = Action { }): SourceSet {
  val set = sourceSets.create(name) {
    compileClasspath += sourceSets.named("client").get().compileClasspath
    runtimeClasspath += sourceSets.named("client").get().runtimeClasspath
    action(this)
  }

  val setConfig = configurations.create(name)

  configurations.named(set.compileClasspathConfigurationName) {
    extendsFrom(setConfig)
  }
  configurations.named(set.runtimeClasspathConfigurationName) {
    extendsFrom(setConfig)
  }

  loom.addRemapConfiguration("mod${name.capitalized()}") {
    sourceSet = set
    targetConfigurationName = name
    onCompileClasspath = true
    onRuntimeClasspath = true
    publishingMode = RemapConfigurationSettings.PublishingMode.NONE
  }

  dependencies {
    set.implementationConfigurationName(sourceSets.named("client").map { it.output })
  }

  return set
}

// The testmod is not split, not worth the effort
val testmod = createSecondarySet("testmod") {
  java.srcDirs("src/testmodMixin/java")
  resources.srcDirs("src/testmodMixin/resources")
}

val permissionsApiCompat = createSecondarySet("permissionsApiCompat")

configurations.named("clientAnnotationProcessor") {
  extendsFrom(configurations.annotationProcessor.get())
}

sourceSets {
  test {
    compileClasspath += main.get().compileClasspath
    runtimeClasspath += main.get().runtimeClasspath
    compileClasspath += getByName("client").compileClasspath
    runtimeClasspath += getByName("client").runtimeClasspath
  }
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
      vmArgs(
        // "-Dmixin.debug.countInjections=true",
        // "-Dmixin.debug.strict=true", // Breaks FAPI :(
      )
    }
  }

  mods {
    register("adventure-platform-fabric") {
      sourceSet(sourceSets.main.get())
      sourceSet(sourceSets.named("client").get())
      sourceSet(permissionsApiCompat)
      sourceSet("main", project(":adventure-platform-mod-shared"))
    }
    register("adventure-platform-fabric-testmod") {
      sourceSet(testmod)
    }
  }

  mixin {
    add(sourceSets.main.get(), "adventure-platform-fabric-refmap.json")
    add(sourceSets.named("client").get(), "adventure-platform-fabric-client-refmap.json")
    add(testmod, "adventure-platform-fabric-testmod-refmap.json")
  }

  decompilerOptions.named("vineflower") {
    options.put("win", "0")
  }
}


dependencies {
  "testmodRuntimeOnly"(permissionsApiCompat.output)
  "modPermissionsApiCompat"(libs.fabric.permissionsApi) {
    isTransitive = false
  }

  // Testmod-specific dependencies
  "modTestmod"(libs.fabric.api)
}

// Create a remapped testmod jar
val testmodDevJar = tasks.register("testmodJar", Jar::class) {
  from(testmod.output)
  archiveClassifier = "testmod-dev"
}

val remapTestmodJar = tasks.register("remapTestmodJar", RemapJarTask::class) {
  inputFile = testmodDevJar.flatMap { it.archiveFile }
  addNestedDependencies = false
  classpath.from(testmod.runtimeClasspath)
  archiveClassifier = "testmod"
}
tasks.build {
  dependsOn(remapTestmodJar)
}

tasks {
  javadoc {
    exclude("net/kyori/adventure/platform/fabric/impl/**")
    val client = sourceSets.getByName("client")
    source(client.allJava)
    classpath += client.output
    val advVersion = libs.versions.adventure.get()
    if (!advVersion.contains("SNAPSHOT")) {
      (options as? StandardJavadocDocletOptions)?.links(
        "https://jd.advntr.dev/api/${advVersion}",
        "https://jd.advntr.dev/key/${advVersion}",
      )
    }
    (options as? StandardJavadocDocletOptions)?.links(
      "https://jd.advntr.dev/platform/api/${libs.versions.adventurePlatform.get()}",
    )
  }

  jar {
    from(permissionsApiCompat.output)
  }

  sourcesJar {
    from(permissionsApiCompat.allSource)
  }
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

indraCrossdoc {
  baseUrl().set(providers.gradleProperty("javadocPublishRoot"))
  nameBasedDocumentationUrlProvider {
    projectNamePrefix = "adventure-platform-"
  }
}
