plugins {
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
    }
    register("server") {
      server()
    }
  }

  mods {
    register("adventure-platform-neoforge") {
      sourceSet(sourceSets.main.get())
    }
  }
}

configurations.jarJar {
  extendsFrom(configurations.jarInJar.get())
}

dependencies {
  additionalRuntimeClasspath(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))
  implementation(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))
  jarJar(project(":adventure-platform-neoforge:adventure-platform-neoforge-services"))

  api(project(":adventure-platform-mod-shared"))
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
