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

rootProject.name = "adventure-platform-fabric"
