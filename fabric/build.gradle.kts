import net.fabricmc.loom.task.GenerateSourcesTask
import net.fabricmc.loom.task.RunGameTask

plugins {
  id("net.fabricmc.fabric-loom")
  alias(libs.plugins.configurateTransformations)
  id("standard-conventions")
  id("mod-publishing-conventions")
}

publishMods.modrinth {
  file = tasks.jar.flatMap { it.archiveFile }
  modLoaders = listOf("fabric")
  requires("fabric-api")
}

dependencies {
  vineflowerDecompilerClasspath(libs.vineflower)
  sequenceOf<(Any) -> Dependency?>(
    ::implementation, ::api, ::compileOnly
  ).forEach { it(platform(fabricApiLibs.bom)) }
  api(fabricApiLibs.base)
  implementation(fabricApiLibs.networking.api.v1)
  implementation(fabricApiLibs.command.api.v2)
  // Only used for prod test
  compileOnly(fabricApiLibs.lifecycle.events.v1)

  minecraft(libs.minecraft)
  implementation(libs.fabric.loader)

  testImplementation(libs.fabric.loader.junit)

  api(project(":adventure-platform-mod-shared"))
  include(project(":adventure-platform-mod-shared"))
}

configurations {
  include {
    extendsFrom(configurations.jarInJar.get())
  }
  runtimeClasspath {
    extendsFrom(vineflowerDecompilerClasspath.get())
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

val enablePermissionsApiCompat = true
val permissionsApiCompat = if (enablePermissionsApiCompat) createSecondarySet("permissionsApiCompat") else createSecondarySet("dummyPermissionsApiCompat")

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
      sourceSet = "testmod"
      client()
    }
    register("testmodServer") {
      sourceSet = "testmod"
      server()
    }

    configureEach {
      generateRunConfig = true
      jvmArguments.addAll(
        // "-Dmixin.debug.countInjections=true",
        // "-Dmixin.debug.strict=true", // Breaks FAPI :(
      )
    }
  }

  mods {
    register("adventure-platform-fabric") {
      sourceSet(sourceSets.main.get())
      sourceSet(sourceSets.named("client").get())
      if (enablePermissionsApiCompat) sourceSet(permissionsApiCompat)
      sourceSet("main", ":adventure-platform-mod-shared")
    }
    register("adventure-platform-fabric-testmod") {
      sourceSet(testmod)
      sourceSet("main", ":test-resources")
    }
  }

  decompilerOptions.named("vineflower") {
    options.put("win", "0")
  }

  runtimeOnlyLog4j.set(true)
}

tasks.withType(RunGameTask::class).configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(indra.javaVersions().target().map { v -> JavaLanguageVersion.of(v) })})
}

dependencies {
  if (enablePermissionsApiCompat) "testmodRuntimeOnly"(permissionsApiCompat.output)
  "testmodRuntimeOnly"(project.project(":test-resources").sourceSets.main.get().output)
  if (enablePermissionsApiCompat) "permissionsApiCompat"(libs.fabric.permissionsApi) {
    isTransitive = false
  }

  // Testmod-specific dependencies
  "testmod"(fabricApiLibs.fabric.api)
}

// Create a testmod jar
val testmodJar = tasks.register("testmodJar", Jar::class) {
  from(testmod.output)
  from(project(":test-resources").sourceSets.main.get().output)
  archiveClassifier = "testmod"
}

tasks.build {
  dependsOn(testmodJar)
}

tasks {
  jar {
    if (enablePermissionsApiCompat) from(permissionsApiCompat.output)
  }

  sourcesJar {
    if (enablePermissionsApiCompat) from(permissionsApiCompat.allSource)
  }
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
