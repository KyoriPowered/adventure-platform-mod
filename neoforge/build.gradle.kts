import net.neoforged.moddevgradle.internal.RunGameTask

plugins {
  alias(libs.plugins.eclipseApt)
  alias(libs.plugins.configurateTransformations)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
  alias(libs.plugins.indra.licenseHeader)
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.indra.crossdoc)
  // TODO alias(libs.plugins.ideaExt)
  id("com.diffplug.spotless")
  id("net.neoforged.moddev")
}

neoForge {
  // We currently only support NeoForge versions later than 21.0.x
  // See https://projects.neoforged.net/neoforged/neoforge for the latest updates
  version = "21.0.114-beta"

  parchment {
    parchmentArtifact = "org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip"
  }

  // Validate AT files and raise errors when they have invalid targets
  // This option is false by default, but turning it on is recommended
  validateAccessTransformers = true

  runs {
    register("client") {
      client()
      mods.set(emptySet())
    }
    register("server") {
      server()
      mods.set(emptySet())
    }
  }

  mods {
    register("adventure-platform-neoforge") {
      sourceSet(sourceSets.main.get())
    }
  }
}

tasks.withType<RunGameTask>().configureEach {
  dependsOn(tasks.jar)
  doFirst {
    val jar = file("run/mods/main.jar")
    jar.parentFile.mkdirs()
    tasks.jar.get().archiveFile.get().asFile.copyTo(jar, true)
  }
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
    api(it) {
      exclude("org.slf4j", "slf4j-api")
    }
    jarJar(it)
  }

  sequenceOf(
    libs.adventure.platform.api,
    libs.adventure.textSerializerGson
  ).forEach {
    api(it) {
      exclude("com.google.code.gson")
    }
    jarJar(it)
  }

  // Transitive deps
  jarJar(libs.examination.api)
  jarJar(libs.examination.string)
  jarJar(libs.adventure.textSerializerJson)
  jarJar(libs.ansi)
  jarJar(libs.option)
  compileOnly(libs.jetbrainsAnnotations)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.engine)
  testRuntimeOnly(libs.junit.launcher)

  checkstyle(libs.stylecheck)

  compileOnly(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))
  jarJar(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))

  compileOnlyApi(project(":adventure-platform-mod-shared"))
  jarJar(project(":adventure-platform-mod-shared"))
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

tasks {
  javadoc {
    // exclude("net/kyori/adventure/platform/fabric/impl/**")
    // val client = sourceSets.getByName("client")
    // source(client.allJava)
    // classpath += client.output
    // val advVersion = libs.versions.adventure.get()
    // if (!advVersion.contains("SNAPSHOT")) {
    //   (options as? StandardJavadocDocletOptions)?.links(
    //     "https://jd.advntr.dev/api/${advVersion}",
    //     "https://jd.advntr.dev/key/${advVersion}",
    //   )
    // }
    // (options as? StandardJavadocDocletOptions)?.links(
    //   "https://jd.advntr.dev/platform/api/${libs.versions.adventurePlatform.get()}",
    // )
  }
  processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
      expand(props)
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
    projectNamePrefix = "adventure-platform-"
  }
}
