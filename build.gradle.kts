
import ca.stellardrift.build.common.adventure
import net.kyori.indra.sonatypeSnapshots

plugins {
  id("ca.stellardrift.opinionated.fabric") version "4.1"
  id("net.kyori.indra.publishing.sonatype") version "1.2.1"
}

val versionMinecraft: String by project
val versionAdventure: String by project
val versionAdventurePlatform: String by project
val versionAdventurePagination: String by project
val versionLoader: String by project
val versionFabricApi: String by project

group = "net.kyori"
version = "4.0.0-SNAPSHOT"

repositories {
  jcenter()
  mavenCentral()
  sonatypeSnapshots()
}

dependencies {
  modApi(include(adventure("api", versionAdventure))!!)
  modApi(include(adventure("text-serializer-plain", versionAdventure))!!)
  modApi(include(adventure("text-feature-pagination", versionAdventurePagination))!!)
  modApi(include(adventure("platform-api", versionAdventurePlatform)) {
    exclude("com.google.code.gson")
  })
  modImplementation(include(adventure("text-serializer-gson", versionAdventure)) {
    exclude("com.google.code.gson")
  })
  modApi(fabricApi.module("fabric-api-base", versionFabricApi))

  // Transitive deps
  include("net.kyori:examination-api:1.0.0")
  include("net.kyori:examination-string:1.0.0")

  modImplementation("ca.stellardrift:colonel:0.1")

  minecraft("com.mojang:minecraft:$versionMinecraft")
  mappings(minecraft.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$versionLoader")

  // Testmod TODO figure out own scope
  modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

  checkstyle("ca.stellardrift:stylecheck:0.1")
}

indra {
  github("KyoriPowered", "adventure-platform-fabric") {
    ci = true
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
