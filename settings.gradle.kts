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
    gradlePluginPortal()
  }
}

plugins {
  id("fabric-loom") version "0.12.+"
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  repositories {
    mavenCentral()
    maven(url = "https://maven.parchmentmc.org/") {
      name = "parchment"
    }
    maven(url = "https://maven.quiltmc.org/repositories/release/") {
      name = "quiltReleases"
      mavenContent {
        includeGroup("org.quiltmc")
        releasesOnly()
      }
    }
  }
}

rootProject.name = "adventure-platform-mod-parent"

setOf("fabric", "forge", "mod-shared").forEach {
  include(it)
  findProject(":$it")?.name = "adventure-platform-$it"
}


