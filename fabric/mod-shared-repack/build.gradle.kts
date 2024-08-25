plugins {
  alias(libs.plugins.loom)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
}

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.layered {
    officialMojangMappings()
    parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
  })
  compileOnly(libs.fabric.loader)
}

loom {
  mixin {
    useLegacyMixinAp = false
  }
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
