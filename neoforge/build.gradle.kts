import net.neoforged.moddevgradle.internal.RunGameTask

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

configurations.jarJar {
  extendsFrom(configurations.jarInJar.get())
}

dependencies {
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
  processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
      expand(props)
    }
  }
}
