pluginManagement {
  repositories {
    maven(url = "https://repo.stellardrift.ca/repository/internal/") {
      name = "stellardriftReleases"
      mavenContent { releasesOnly() }
    }
    maven(url = "https://repo.stellardrift.ca/repository/snapshots/") {
      name = "stellardriftSnapshots"
      mavenContent { snapshotsOnly() }
    }
    mavenCentral()
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net")
    }
    maven(url = "https://repo.jpenilla.xyz/snapshots/") {
      name = "jmpSnapshots"
      mavenContent { snapshotsOnly() }
    }
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
  id("xyz.jpenilla.quiet-architectury-loom") version "1.7.292"
}

rootProject.name = "adventure-platform-mod-parent"

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
      name = "ossSnapshots"
      mavenContent { snapshotsOnly() }
    }
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
      name = "s01ossSnapshots"
      mavenContent { snapshotsOnly() }
    }
    maven(url = "https://maven.parchmentmc.org/") {
      name = "parchment"
    }
  }
}

setOf("fabric", "neoforge", "mod-shared").forEach {
  include(it)
  findProject(":$it")?.name = "adventure-platform-$it"
}
