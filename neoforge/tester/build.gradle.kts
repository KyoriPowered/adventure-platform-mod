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
    }
    register("server") {
      server()
    }
  }

  mods {
    register("adventure-platform-neoforge-tester") {
      sourceSet(sourceSets.main.get())
      sourceSet(project(":test-resources").sourceSets.main.get())
    }
  }
}

tasks.jar {
  from(project(":test-resources").sourceSets.main.get().output)
}

dependencies {
  implementation(project(":adventure-platform-neoforge"))
  jarJar(project(":adventure-platform-neoforge"))
  checkstyle(libs.stylecheck)
}
