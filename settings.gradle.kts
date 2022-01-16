pluginManagement {
  repositories {
    mavenCentral()
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net")
    }
    maven {
      name = "Cotton"
      url = uri("https://server.bbkr.space/artifactory/libs-release/")
    }
    gradlePluginPortal()
  }
}

rootProject.name = "adventure-platform-fabric"
