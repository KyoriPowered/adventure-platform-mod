import net.neoforged.moddevgradle.internal.RunGameTask
import java.net.URI
import java.nio.file.FileSystems
import java.util.jar.Manifest
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream

plugins {
  alias(libs.plugins.configurateTransformations)
  id("net.neoforged.moddev")
  id("publishing-conventions")
}

neoForge {
  version = libs.versions.neoforge

  parchment {
    parchmentArtifact = "org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip"
  }

  validateAccessTransformers = true

  runs {
    register("client") {
      client()
      mods.set(emptySet()) // Work around classpath issues by using the production jar for dev runs
    }
    register("server") {
      server()
      mods.set(emptySet()) // Work around classpath issues by using the production jar for dev runs
    }
  }

  mods {
    register("adventure-platform-neoforge") {
      sourceSet(sourceSets.main.get())
    }
  }
}

// Work around classpath issues by using the production jar for dev runs
tasks.withType<RunGameTask>().configureEach {
  dependsOn(tasks.jar)
  doFirst {
    val jar = file("run/mods/main.jar")
    jar.parentFile.mkdirs()
    tasks.jar.get().archiveFile.get().asFile.copyTo(jar, true)
  }
}

tasks {
  // Force adventure libs to be GAMELIBRARY - fixes issues with service loaders and mixins to API
  val injectMeta = register("injectGameLibraryMeta") {
    dependsOn(jarJar)
    doFirst {
      val dir = jarJar.get().outputDirectory.get().asFile.toPath()
      for (nestedJar in dir.resolve("META-INF/jarjar").listDirectoryEntries("*.jar")) {
        FileSystems.newFileSystem(URI.create("jar:" + nestedJar.toUri()), emptyMap<String, Any>()).use { fs ->
          val manifestPath = fs.rootDirectories.single().resolve("META-INF/MANIFEST.MF")
          val manifest = manifestPath.inputStream().use { Manifest(it) }
          if (manifest.mainAttributes.getValue("FMLModType") == null) {
            manifest.mainAttributes.putValue("FMLModType", "GAMELIBRARY")
          }
          manifestPath.outputStream().use { manifest.write(it) }
        }
      }
    }
  }
  jar {
    dependsOn(injectMeta)
  }
}

configurations.jarJar {
  extendsFrom(configurations.jarInJar.get())
}

dependencies {
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
  processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
      expand(props)
    }
  }
}
