pluginManagement {
  repositories {
    maven(url = "https://repo.stellardrift.ca/maven/internal/") {
      name = "stellardriftReleases"
      mavenContent { releasesOnly() }
    }
    maven(url = "https://repo.stellardrift.ca/maven/snapshots/") {
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
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
  id("fabric-loom") version "1.10.5"
  id("net.neoforged.moddev.repositories") version "2.0.88"
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
    maven(url= "https://maven.neoforged.net/") {
      name = "neoforge"
    }
  }
}

fun includeAndRename(path: String, name: String? = null) {
  include(path)
  findProject(":$path")?.name = "adventure-platform-${name ?: path.replace(":", "-")}"
}

// Common
includeAndRename("mod-shared")

include(":test-resources")
findProject(":test-resources")?.projectDir = file("mod-shared/test-resources")

// Fabric
includeAndRename("fabric")
includeAndRename("adventure-platform-fabric:mod-shared-repack", "mod-shared-fabric-repack")

// NeoForge

includeAndRename("neoforge")
includeAndRename("adventure-platform-neoforge:tester", "neoforge-tester")
includeAndRename("adventure-platform-neoforge:services", "neoforge-services")

