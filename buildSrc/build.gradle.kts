plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral {
    mavenContent { releasesOnly() }
  }
  gradlePluginPortal()
  maven("https://repo.papermc.io/repository/maven-snapshots/") {
    name = "papermcSnapshots"
    mavenContent { snapshotsOnly() }
  }
}

dependencies {
  // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(libs.build.indra.crossdoc)
  implementation(libs.build.indra)
  implementation(libs.build.indra.spotless)
  implementation(libs.build.configurate.transformations)
  implementation(libs.build.idea.ext)
}
