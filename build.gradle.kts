import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.transformations.convertFormat
import net.fabricmc.loom.task.RunGameTask

plugins {
  alias(libs.plugins.loom)
  alias(libs.plugins.loomQuiltflower)
  alias(libs.plugins.configurateTransformations)
  alias(libs.plugins.indra)
  alias(libs.plugins.indra.licenseHeader)
  alias(libs.plugins.indra.checkstyle)
  alias(libs.plugins.indra.sonatype)
}

repositories {
  mavenCentral()
  maven(url = "https://maven.parchmentmc.org/") {
    name = "parchment"
  }
}

quiltflower {
  quiltflowerVersion.set(libs.versions.quiltflower.get())
  preferences(
    "win" to 0
  )
  addToRuntimeClasspath.set(true)
}

indra {
  javaVersions().target(17)
}

indraSonatype {
  useAlternateSonatypeOSSHost("s01")
}

license {
  header(file("LICENSE_HEADER"))
}

dependencies {
  annotationProcessor(libs.contractValidator)
  sequenceOf(
    libs.adventure.key,
    libs.adventure.api,
    libs.adventure.textSerializerPlain,
    libs.adventure.textMinimessage
  ).forEach {
    modApi(it)
    include(it)
  }

  sequenceOf(
    libs.adventure.platform.api,
    libs.adventure.textSerializerGson
  ).forEach {
   modApi(it) {
     exclude("com.google.code.gson")
   }
   include(it)
  }
  modApi(fabricApi.module("fabric-api-base", libs.versions.fabricApi.get()))

  // Transitive deps
  include(libs.examination.api)
  include(libs.examination.string)
  modCompileOnly(libs.jetbrainsAnnotations)

  modImplementation(libs.colonel)

  minecraft(libs.minecraft)
  mappings(loom.layered {
    officialMojangMappings()
    parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
  })
  modImplementation(libs.fabric.loader)

  // Testmod TODO figure out own scope
  if (gradle.startParameter.taskNames.contains("publish")) {
    modCompileOnly(libs.fabric.api)
  } else {
    modImplementation(libs.fabric.api)
  }

  checkstyle(libs.stylecheck)
}

// tasks.withType(net.fabricmc.loom.task.RunGameTask::class) {
//   setClasspath(files(loom.unmappedModCollection, sourceSets.main.map { it.runtimeClasspath }))
// }

sourceSets {
  main {
    java.srcDirs(
      "src/accessor/java",
      "src/mixin/java"
    )
    resources.srcDirs(
      "src/accessor/resources/",
      "src/mixin/resources/"
    )
  }
  register("testmod") {
    compileClasspath += main.get().compileClasspath
    runtimeClasspath += main.get().runtimeClasspath
    java.srcDirs("src/testmodMixin/java")
    resources.srcDirs("src/testmodMixin/resources")
  }
}

dependencies {
  "testmodImplementation"(sourceSets.main.map { it.output })
}

loom {
  runs {
    register("testmodClient") {
      source("testmod")
      client()
    }
    register("testmodServer") {
      source("testmod")
      server()
    }
  }
}

tasks.withType(RunGameTask::class) {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(indra.javaVersions().target().map { v -> JavaLanguageVersion.of(v) })})
}

// Convert yaml files to josn
tasks.withType(ProcessResources::class.java).configureEach {
    inputs.property("version", project.version)

    // Convert data files yaml -> json
    filesMatching(
        sequenceOf(
            "fabric.mod",
            "data/**/*",
            "assets/**/*"
        ).flatMap { base -> sequenceOf("$base.yml", "$base.yaml") }
            .toList()
    ) {
        convertFormat(ConfigFormats.YAML, ConfigFormats.JSON)
        if (name.startsWith("fabric.mod")) {
            expand("project" to project)
        }
        name = name.substringBeforeLast('.') + ".json"
    }
    // Convert pack meta, without changing extension
    filesMatching("pack.mcmeta") { convertFormat(ConfigFormats.YAML, ConfigFormats.JSON) }
}

indra {
  github("KyoriPowered", "adventure-platform-fabric") {
    ci(true)
  }
  mitLicense()
  checkstyle(libs.versions.checkstyle.get())

  configurePublications {
    pom {
      developers {
        developer {
          id.set("kashike")
          timezone.set("America/Vancouver")
        }

        developer {
          id.set("lucko")
          name.set("Luck")
          url.set("https://lucko.me")
          email.set("git@lucko.me")
        }

        developer {
          id.set("zml")
          name.set("zml")
          timezone.set("America/Vancouver")
        }

        developer {
          id.set("Electroid")
        }
      }
    }
  }
}

// Workaround for both loom and indra doing publication logic in an afterEvaluate :(
indra.includeJavaSoftwareComponentInPublications(false)
publishing {
  publications.named("maven", MavenPublication::class) {
    from(components["java"])
  }
}
