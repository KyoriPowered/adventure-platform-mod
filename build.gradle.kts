
import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.isRelease
import ca.stellardrift.build.common.sonatypeOss

plugins {
  id("fabric-loom") version "0.5-SNAPSHOT"
  id("ca.stellardrift.opinionated.publish") version "3.1"
  id("ca.stellardrift.opinionated.fabric") version "3.1"
  checkstyle
}

val versionSelf = "2.0-SNAPSHOT"
val versionMinecraft: String by project
val versionAdventure: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project

group = "net.kyori"
version = "$versionSelf+${versionAdventure.replace("-SNAPSHOT", "")}"

repositories {
  mavenLocal()
  jcenter()
  mavenCentral()
  sonatypeOss()
}

dependencies {
  modApi(include("net.kyori:examination-api:1.0.0-SNAPSHOT")!!)
  modApi(include("net.kyori:examination-string:1.0.0-SNAPSHOT")!!)
  modApi(include(adventure("api", versionAdventure))!!)
  modApi(include(adventure("text-feature-pagination", versionAdventure))!!)
  modApi(include(adventure("text-serializer-plain", versionAdventure))!!)
  modApi(include(adventure("platform-api", versionAdventure)) {
    exclude("com.google.code.gson")
  })

  modImplementation(include(adventure("text-serializer-gson", versionAdventure)) {
      exclude("com.google.code.gson")
  })
  modImplementation("ca.stellardrift:colonel:0.1")

  minecraft("com.mojang:minecraft:$versionMinecraft")
  mappings(minecraft.officialMojangMappings())
  modImplementation("net.fabricmc:fabric-loader:$versionLoader")

  // Testmod TODO figure out own scope
  modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

  checkstyle("ca.stellardrift:stylecheck:0.1-SNAPSHOT")
}

checkstyle {
  val checkstyleDir = project.projectDir.resolve(".checkstyle")
  toolVersion = "8.34"
  configDirectory.set(checkstyleDir)
  configProperties = mapOf("basedir" to checkstyleDir)
}

tasks.withType(ProcessResources::class).configureEach {
  filesMatching("fabric.mod.json") {
    expand("project" to project)
  }
}

opinionated {
  github("KyoriPowered", "adventure-platform-fabric")
  mit()
  useJUnit5()
  publication?.apply {
    pom {
      url.set("https://github.com/KyoriPowered/adventure")

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
  if (isRelease()) {
    publishTo("ossrh", "https://oss.sonatype.org/service/local/staging/deploy/maven2/")
  } else {
    publishTo("ossrh", "https://oss.sonatype.org/content/repositories/snapshots/")
  }
}
