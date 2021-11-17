import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import net.kyori.indra.repository.sonatypeSnapshots

plugins {
  val indraVersion = "2.0.6"
  id("fabric-loom") version "0.10-SNAPSHOT"
  // id("ca.stellardrift.opinionated.fabric") version "5.0.0"
  id("ca.stellardrift.configurate-transformations") version "5.0.0"
  id("net.kyori.indra") version indraVersion
  id("net.kyori.indra.license-header") version indraVersion
  id("net.kyori.indra.checkstyle") version indraVersion
  id("net.kyori.indra.publishing.sonatype") version indraVersion
}

val versionMinecraft: String by project
val versionAdventure: String by project
val versionAdventurePlatform: String by project
val versionLoader: String by project
val versionFabricApi: String by project

group = "net.kyori"
version = "5.0.0-SNAPSHOT"

repositories {
  mavenCentral()
  sonatypeSnapshots()
  maven(url = "https://maven.parchmentmc.org/") {
    name = "parchment"
  }
}

indra {
  javaVersions().target(17)
}

license {
  header(file("LICENSE_HEADER"))
}

dependencies {
  annotationProcessor("ca.stellardrift:contract-validator:1.0.1")
  modApi(include("net.kyori:adventure-key:$versionAdventure")!!)
  modApi(include("net.kyori:adventure-api:$versionAdventure")!!)
  modApi(include("net.kyori:adventure-text-serializer-plain:$versionAdventure")!!)
  modApi(include("net.kyori:adventure-platform-api:$versionAdventurePlatform") {
    exclude("com.google.code.gson")
  })
  modImplementation(include("net.kyori:adventure-text-serializer-gson:$versionAdventure") {
    exclude("com.google.code.gson")
  })
  modApi(fabricApi.module("fabric-api-base", versionFabricApi))

  // Transitive deps
  include("net.kyori:examination-api:1.3.0")
  include("net.kyori:examination-string:1.3.0")
  modCompileOnly("org.jetbrains:annotations:22.0.0")

  modImplementation("ca.stellardrift:colonel:0.2.1")

  minecraft("com.mojang:minecraft:$versionMinecraft")
  mappings(loom.layered {
    officialMojangMappings()
    // parchment("org.parchmentmc.data:parchment-1.17.1:2021.10.24@zip") // not published for snapshots
  })
  modImplementation("net.fabricmc:fabric-loader:$versionLoader")

  // Testmod TODO figure out own scope
  modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")
  // modRuntime("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

  checkstyle("ca.stellardrift:stylecheck:0.1")
}

/*tasks.sourcesJar {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE // duplicate package-info.java coming in from somewhere?
}*/

// tasks.withType(net.fabricmc.loom.task.RunGameTask::class) {
//   setClasspath(files(loom.unmappedModCollection, sourceSets.main.map { it.runtimeClasspath }))
// }

sourceSets {
  main {
    java.srcDirs(
      "src/accessor/java",
      "src/mixin/java"
    )
    resources.srcDirs(
      "src/accessor/resources/",
      "src/mixin/resources/"
    )
  }
  register("testmod") {
    compileClasspath += main.get().compileClasspath
    runtimeClasspath += main.get().runtimeClasspath
  }
}

dependencies {
  "testmodImplementation"(sourceSets.main.map { it.output })
}

// todo: restore testmod

// Convert yaml files to josn
tasks.withType(ProcessResources::class.java).configureEach {
    inputs.property("version", project.version)

    // Convert data files yaml -> json
    filesMatching(
        sequenceOf(
            "fabric.mod",
            "data/**/*",
            "assets/**/*"
        ).flatMap { base -> sequenceOf("$base.yml", "$base.yaml") }
            .toList()
    ) {
        convertFormat(ConfigFormats.YAML, ConfigFormats.JSON)
        if (name.startsWith("fabric.mod")) {
            expand("project" to project)
        }
        name = name.substringBeforeLast('.') + ".json"
    }
    // Convert pack meta, without changing extension
    filesMatching("pack.mcmeta") { convertFormat(ConfigFormats.YAML, ConfigFormats.JSON) }
}

indra {
  github("KyoriPowered", "adventure-platform-fabric") {
    ci(true)
  }
  mitLicense()

  configurePublications {
    pom {
      developers {
        developer {
          id.set("kashike")
          timezone.set("America/Vancouver")
        }

        developer {
          id.set("lucko")
          name.set("Luck")
          url.set("https://lucko.me")
          email.set("git@lucko.me")
        }

        developer {
          id.set("zml")
          name.set("zml")
          timezone.set("America/Vancouver")
        }

        developer {
          id.set("Electroid")
        }
      }
    }
  }
}
