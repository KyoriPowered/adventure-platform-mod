import java.nio.file.Files
import net.neoforged.moddevgradle.internal.RunGameTask

plugins {
  id("net.neoforged.moddev")
  alias(libs.plugins.indra.checkstyle)
}

neoForge {
  version = libs.versions.neoforge

  validateAccessTransformers = true

  runs {
    register("client") {
      client()
      mods.set(HashSet())
    }
    register("server") {
      server()
      mods.set(HashSet())
    }
  }

  mods {
    register("adventure-platform-neoforge-tester") {
      sourceSet(sourceSets.main.get())
    }
  }
}

tasks.jar {
  from(project(":test-resources").sourceSets.main.get().output)
}

tasks.withType(RunGameTask::class.java).configureEach {
  dependsOn(tasks.jar)
  doFirst {
    val jar = file("run/mods/main.jar")
    jar.parentFile.mkdirs()
    if (jar.exists()) jar.delete()
    Files.copy(tasks.jar.get().archiveFile.get().asFile.toPath(), jar.toPath())
  }
}

dependencies {
  implementation(project(":adventure-platform-neoforge"))
  jarJar(project(":adventure-platform-neoforge"))
  checkstyle(libs.stylecheck)
}
