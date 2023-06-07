import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import net.fabricmc.loom.api.RemapConfigurationSettings
import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RunGameTask
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
  alias(libs.plugins.eclipseApt)
  alias(libs.plugins.loom)
  alias(libs.plugins.loomQuiltflower)
  alias(libs.plugins.configurateTransformations)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.licenseHeader)
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.indra.sonatype)
  alias(libs.plugins.indra.crossdoc)
  alias(libs.plugins.ideaExt)
  alias(libs.plugins.spotless)
}

repositories {
  mavenCentral()
  maven(url = "https://maven.parchmentmc.org/") {
    name = "parchment"
  }
  maven(url = "https://maven.quiltmc.org/repositories/release/") {
    name = "quiltReleases"
    mavenContent {
      includeGroup("org.quiltmc")
      releasesOnly()
    }
  }
  sonatype.ossSnapshots()
  sonatype.s01Snapshots()
}

quiltflower {
  quiltflowerVersion.set(libs.versions.quiltflower.get())
  preferences(
    "win" to 0
  )
  // addToRuntimeClasspath.set(true)
}

indra {
  javaVersions().target(17)
}

indraSonatype {
  useAlternateSonatypeOSSHost("s01")
}

indraSpotlessLicenser {
  licenseHeaderFile(file("LICENSE_HEADER"))
}

spotless {
  // ratchetFrom("origin/mc/${libs.versions.minecraft.get().splitToSequence('.').take(2).joinToString(".")}")
  ratchetFrom("origin/mc/1.19")

  java {
    importOrderFile(rootProject.file(".spotless/kyori.importorder"))
    trimTrailingWhitespace()
    endWithNewline()
    indentWithSpaces(2)
    removeUnusedImports()
  }
}

dependencies {
  annotationProcessor(libs.autoService)
  annotationProcessor(libs.contractValidator)
  compileOnlyApi(libs.autoService.annotations)
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
  modImplementation(fabricApi.module("fabric-networking-api-v1", libs.versions.fabricApi.get()))
  modImplementation(fabricApi.module("fabric-command-api-v2", libs.versions.fabricApi.get()))
  // Only used for prod test
  modCompileOnly(fabricApi.module("fabric-lifecycle-events-v1", libs.versions.fabricApi.get()))

  // Transitive deps
  include(libs.examination.api)
  include(libs.examination.string)
  include(libs.adventure.textSerializerJson)
  modCompileOnly(libs.jetbrainsAnnotations)

  minecraft(libs.minecraft)
  mappings(loom.layered {
    officialMojangMappings()
    parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
  })
  modImplementation(libs.fabric.loader)

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
    sourceSet.set(set)
    targetConfigurationName.set(name)
    onCompileClasspath.set(true)
    onRuntimeClasspath.set(true)
    publishingMode.set(RemapConfigurationSettings.PublishingMode.NONE)
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

loom {
  runtimeOnlyLog4j.set(true)
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
      vmArgs(
        "-Dmixin.debug.countInjections=true",
        // "-Dmixin.debug.strict=true", // Breaks FAPI :(
        "-Dadventure.mixins.audit=true"
      )
    }
  }

  mods {
    register("adventure-platform-fabric") {
      sourceSet(sourceSets.main.get())
      sourceSet(sourceSets.named("client").get())
      sourceSet(permissionsApiCompat)
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

}


dependencies {
  "testmodRuntimeOnly"(permissionsApiCompat.output)
  "modPermissionsApiCompat"(libs.fabric.permissionsApi)

  // Testmod-specific dependencies
  "modTestmod"(libs.fabric.api)
}

// Create a remapped testmod jar
val testmodDevJar = tasks.register("testmodJar", Jar::class) {
  from(testmod.output)
  archiveClassifier.set("testmod-dev")
}

val remapTestmodJar = tasks.register("remapTestmodJar", RemapJarTask::class) {
  inputFile.set(testmodDevJar.flatMap { it.archiveFile })
  addNestedDependencies.set(false)
  classpath.from(testmod.runtimeClasspath)
  archiveClassifier.set("testmod")
}
tasks.build {
  dependsOn(remapTestmodJar)
}

tasks {
  withType(RunGameTask::class).configureEach {
    javaLauncher.set(project.javaToolchains.launcherFor { languageVersion.set(indra.javaVersions().target().map { v -> JavaLanguageVersion.of(v) }) })
  }

  javadoc {
    exclude("net/kyori/adventure/platform/fabric/impl/**")
    val client = sourceSets.getByName("client")
    source(client.allJava)
    classpath += client.output
    (options as? StandardJavadocDocletOptions)?.links(
      "https://jd.advntr.dev/api/${libs.versions.adventure.get()}",
      "https://jd.advntr.dev/key/${libs.versions.adventure.get()}",
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
  idea.project.settings.taskTriggers {
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

// Ugly hack for easy genSourcening
afterEvaluate {
  tasks.matching { it.name == "genSources" }.configureEach {
    setDependsOn(setOf("genClientOnlySourcesWithQuiltflower", "genCommonSourcesWithQuiltflower"))
  }
}

indra {
  github("KyoriPowered", "adventure-platform-fabric") {
    ci(true)
  }
  mitLicense()
  checkstyle(libs.versions.checkstyle.get())

  configurePublications {
    pom {
      developers {
        developer {
          id.set("kashike")
          timezone.set("America/Vancouver")
        }

        developer {
          id.set("lucko")
          name.set("Luck")
          url.set("https://lucko.me")
          email.set("git@lucko.me")
        }

        developer {
          id.set("zml")
          name.set("zml")
          timezone.set("America/Vancouver")
        }

        developer {
          id.set("Electroid")
        }
      }
    }
  }
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
    projectNamePrefix.set("adventure-platform-")
  }
}
