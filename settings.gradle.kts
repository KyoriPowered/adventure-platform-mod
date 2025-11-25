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
  id("fabric-loom") version "1.13.6"
  id("net.neoforged.moddev.repositories") version "2.0.120"
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
    maven("https://repo.papermc.io/repository/maven-public/") {
      name = "papermc"
    }
    maven(url= "https://maven.neoforged.net/") {
      name = "neoforge"
    }
    maven { // TODO remove on neoforge release
      name = "Maven for PR #2815" // https://github.com/neoforged/NeoForge/pull/2815
      url = uri("https://prmaven.neoforged.net/NeoForge/pr2815")
      content {
        includeModule("net.neoforged", "neoforge")
        includeModule("net.neoforged", "testframework")
      }
    }
  }

  versionCatalogs {
    create("fabricApiLibs") {
      from("net.fabricmc.fabric-api:fabric-api-catalog:0.139.2+1.21.11")
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

