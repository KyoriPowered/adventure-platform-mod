import net.fabricmc.loom.api.RemapConfigurationSettings
import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RunGameTask
import org.gradle.configurationcache.extensions.capitalized

plugins {
  alias(libs.plugins.loom)
  alias(libs.plugins.configurateTransformations)
  id("standard-conventions")
  // id("publishing-conventions") // disable until 1.21.2
}

dependencies {
  vineflowerDecompilerClasspath(libs.vineflower)
  sequenceOf<(Any) -> Dependency?>(
    ::modImplementation, ::modApi, ::modCompileOnly
  ).forEach { it(platform(libs.fabric.api.bom)) }
  modApi(libs.fabric.api.base)
  modImplementation(libs.fabric.api.networking)
  modImplementation(libs.fabric.api.command)
  // Only used for prod test
  modCompileOnly(libs.fabric.api.lifecycle)

  minecraft(libs.minecraft)
  mappings(loom.layered {
    officialMojangMappings()
    parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
  })
  modImplementation(libs.fabric.loader)

  testImplementation(libs.fabric.loader.junit)

  localRuntime(project(":adventure-platform-mod-shared"))
  compileOnly(project(":adventure-platform-mod-shared"))
  testImplementation(project(":adventure-platform-mod-shared"))
  api(project(":adventure-platform-fabric:adventure-platform-mod-shared-fabric-repack", configuration = "namedElements"))
  include(project(":adventure-platform-fabric:adventure-platform-mod-shared-fabric-repack"))
}

configurations {
  include {
    extendsFrom(configurations.jarInJar.get())
  }
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
      sourceSet("main", project(":test-resources"))
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

  runtimeOnlyLog4j.set(true)
}

// Loom -- needs to run late to avoid preset config not applying properly
afterEvaluate {
  tasks.withType(RunGameTask::class).configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(indra.javaVersions().target().map { v -> JavaLanguageVersion.of(v) })})
  }
}

dependencies {
  "testmodRuntimeOnly"(permissionsApiCompat.output)
  "testmodRuntimeOnly"(project.project(":test-resources").sourceSets.main.get().output)
  "modPermissionsApiCompat"(libs.fabric.permissionsApi) {
    isTransitive = false
  }

  // Testmod-specific dependencies
  "modTestmod"(libs.fabric.api)
}

// Create a remapped testmod jar
val testmodDevJar = tasks.register("testmodJar", Jar::class) {
  from(testmod.output)
  from(project(":test-resources").sourceSets.main.get().output)
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
  jar {
    from(permissionsApiCompat.output)
  }

  sourcesJar {
    from(permissionsApiCompat.allSource)
  }
}

val generateTemplates = createProcessResourceTemplates("generateTemplates", sourceSets.main.get())
val generateTestmodTemplates = createProcessResourceTemplates("generateTestmodTemplates", testmod)

tasks.withType(GenerateSourcesTask::class).configureEach {
  dependsOn(generateTemplates)
}

// Workaround for both loom and indra doing publication logic in an afterEvaluate :(
indra.includeJavaSoftwareComponentInPublications(false)
/*publishing {
  publications.named("maven", MavenPublication::class) {
    from(components["java"])
  }
}*/
