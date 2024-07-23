plugins {
  alias(libs.plugins.loom)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
}

dependencies {
  compileOnly(libs.fabric.loader)
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
  javadocJar {
    from(zipTree(common.tasks.javadocJar.flatMap { it.archiveFile }))
  }
}
