pluginManagement {
  repositories {
      mavenLocal {
          mavenContent { snapshotsOnly() }
      }
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
  id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
  id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.4" apply false
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven(url = "https://maven.parchmentmc.org/") {
      name = "parchment"
    }
    maven(url = "https://maven.minecraftforge.net/") {
      name = "forge"
      mavenContent {
        releasesOnly()
      }
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


