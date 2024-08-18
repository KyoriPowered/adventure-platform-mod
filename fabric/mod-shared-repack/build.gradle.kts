plugins {
  alias(libs.plugins.loom)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.publishing)
}

dependencies {
  compileOnly(libs.fabric.loader)
}

loom {
  mixin {
    useLegacyMixinAp = false
  }
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
    enabled = false
  }
}

afterEvaluate {
  val javaComponent = components["java"] as AdhocComponentWithVariants
  javaComponent.withVariantsFromConfiguration(configurations["javadocElements"]) {
    skip()
  }
}
