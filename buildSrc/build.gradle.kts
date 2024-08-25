plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
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
