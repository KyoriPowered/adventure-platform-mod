import net.fabricmc.loom.api.RemapConfigurationSettings
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.configurationcache.extensions.capitalized

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

  // Transitive deps
  include(libs.examination.api)
  include(libs.examination.string)
  include(libs.adventure.textSerializerJson)
  include(libs.ansi)
  include(libs.option)
  modCompileOnly(libs.jetbrainsAnnotations)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.engine)
  testRuntimeOnly(libs.junit.launcher)

  checkstyle(libs.stylecheck)

  implementation(project(":adventure-platform-mod-shared", "namedElements"))

  neoForge("net.neoforged:neoforge:21.0.114-beta")
}

configurations {
  runtimeClasspath { extendsFrom(vineflowerDecompilerClasspath.get()) }
}


sourceSets {
  main {
    java.srcDirs(
      "src/accessor/java",
      "src/mixin/java",
      "src/client/java"
    )
    resources.srcDirs(
      "src/accessor/resources/",
      "src/mixin/resources/",
      "src/client/resources/"
    )
  }
}

// loom.splitEnvironmentSourceSets() // not supported with (neo)forge

// create a secondary set, with a configuration name matching the source set name
// this configuration is available at compile- and runtime, and not published
fun createSecondarySet(name: String, action: Action<SourceSet> = Action { }): SourceSet {
  val set = sourceSets.create(name) {
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

  return set
}

// The testmod is not split, not worth the effort
val testmod = createSecondarySet("testmod") {
  java.srcDirs("src/testmodMixin/java")
  resources.srcDirs("src/testmodMixin/resources")
}

val permissionsApiCompat = createSecondarySet("permissionsApiCompat")

sourceSets {
  test {
    compileClasspath += main.get().compileClasspath
    runtimeClasspath += main.get().runtimeClasspath
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
    register("adventure-platform-neoforge") {
      sourceSet(sourceSets.main.get())
      sourceSet(permissionsApiCompat)
    }
    register("adventure-platform-neoforge-testmod") {
      sourceSet(testmod)
    }
  }

  mixin {
    useLegacyMixinAp = true
    add(sourceSets.main.get(), "adventure-platform-neoforge-refmap.json")
    add(testmod, "adventure-platform-neoforge-testmod-refmap.json")
  }

  decompilerOptions.named("vineflower") {
    options.put("win", "0")
  }
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
