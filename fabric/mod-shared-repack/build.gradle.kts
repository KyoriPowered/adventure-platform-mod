plugins {
  alias(libs.plugins.loom)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
  id("standard-conventions")
}

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.layered {
    officialMojangMappings()
    parchment("io.papermc.parchment.data:parchment:${libs.versions.parchment.get()}")
  })
  compileOnly(libs.fabric.loader)
}

loom {
  runtimeOnlyLog4j.set(true)
}

tasks {
  val common = project(":adventure-platform-mod-shared")
  jar {
    from(zipTree(common.tasks.jar.flatMap { it.archiveFile })) {
      exclude("META-INF/MANIFEST.MF")
    }
    manifest {
      attributes("Fabric-Loom-Remap" to true)
    }
  }
  sourcesJar {
    from(zipTree(common.tasks.sourcesJar.flatMap { it.archiveFile }))
  }
  javadoc {
    enabled = false
  }
}
