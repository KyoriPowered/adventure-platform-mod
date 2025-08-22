pluginManagement {
  repositories {
    maven("https://repo.papermc.io/repository/maven-snapshots/") {
      name = "papermcSnapshots"
      mavenContent { snapshotsOnly() }
    }
    maven(url = "https://repo.stellardrift.ca/maven/internal/") {
      name = "stellardriftReleases"
      mavenContent { releasesOnly() }
    }
    maven(url = "https://repo.stellardrift.ca/maven/snapshots/") {
      name = "stellardriftSnapshots"
      mavenContent { snapshotsOnly() }
    }
    mavenCentral {
      mavenContent { releasesOnly() }
    }
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net")
    }
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  id("fabric-loom") version "1.11.7"
  id("net.neoforged.moddev.repositories") version "2.0.107"
}

rootProject.name = "adventure-platform-mod-parent"

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  repositories {
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      name = "sonatypeSnapshots"
      mavenContent { snapshotsOnly() }
    }
    maven("https://repo.papermc.io/repository/maven-snapshots/") {
      name = "papermcSnapshots"
      mavenContent { snapshotsOnly() }
    }
    maven(url = "https://maven.parchmentmc.org/") {
      name = "parchment"
    }
    maven(url= "https://maven.neoforged.net/") {
      name = "neoforge"
    }
  }

  versionCatalogs {
    create("fabricApiLibs") {
      from("net.fabricmc.fabric-api:fabric-api-catalog:0.132.1+1.21.9")
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

