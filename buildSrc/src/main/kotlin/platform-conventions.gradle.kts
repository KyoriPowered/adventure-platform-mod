plugins {
  `java-library`
}

val jarInJar = configurations.create("jarInJar")

dependencies {
  annotationProcessor(libs.autoService)
  annotationProcessor(libs.contractValidator)
  compileOnlyApi(libs.autoService.annotations)
  sequenceOf(
    libs.adventure.key,
    libs.adventure.api,
    libs.adventure.textLoggerSlf4j,
    libs.adventure.textMinimessage,
    libs.adventure.textSerializerPlain,
    libs.adventure.textSerializerAnsi
  ).forEach {
    api(it) {
      exclude("org.slf4j", "slf4j-api")
    }
    jarInJar(it)
  }

  sequenceOf(
    libs.adventure.platform.api,
    libs.adventure.textSerializerGson
  ).forEach {
    api(it) {
      exclude("com.google.code.gson")
    }
    jarInJar(it)
  }

  // Transitive deps
  jarInJar(libs.examination.api)
  jarInJar(libs.examination.string)
  jarInJar(libs.adventure.textSerializerJson)
  jarInJar(libs.ansi)
  jarInJar(libs.option)
  compileOnly(libs.jetbrainsAnnotations)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testRuntimeOnly(libs.junit.engine)
  testRuntimeOnly(libs.junit.launcher)
}
