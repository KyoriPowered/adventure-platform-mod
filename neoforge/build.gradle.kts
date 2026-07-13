plugins {
  id("net.neoforged.moddev")
  id("mod-publishing-conventions")
}

neoForge {
  version = libs.versions.neoforge.get()

  validateAccessTransformers = true

  runs {
    register("client") {
      client()
    }
    register("server") {
      server()
    }
  }

  mods {
    register("adventure-platform-neoforge") {
      sourceSet(sourceSets.main.get())
      // Mirror the nested mod-shared JAR used in production as transformed game content.
      sourceSet(project(":adventure-platform-mod-shared").sourceSets.main.get())
    }
  }
}

publishMods.modrinth {
  file = tasks.jar.flatMap { it.archiveFile }
  modLoaders = listOf("neoforge")
}

configurations.jarJar {
  extendsFrom(configurations.jarInJar.get())
}

dependencies {
  // Adventure discovers these bridges from its own non-transforming classloader.
  implementation(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))
  jarJar(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))

  // Expose the shared API without adding its untransformed implementation to dev runtimes.
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
