import ca.stellardrift.build.apiInclude
import ca.stellardrift.build.implementationInclude
import ca.stellardrift.build.kyoriText
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("ca.stellardrift.opinionated") version "1.0.1"
    id("fabric-loom") version "0.2.7-SNAPSHOT"
}

val versionSelf = "1.0.1"
val versionMinecraft: String by project
val versionText: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project

group = "ca.stellardrift"
version = "$versionSelf+$versionText"

repositories {
    jcenter()
    mavenCentral()
}


dependencies {
    apiInclude(kyoriText("api", versionText))
    apiInclude(kyoriText("feature-pagination", versionText))
    apiInclude(kyoriText("serializer-plain", versionText))

    implementationInclude(kyoriText("serializer-gson", versionText)) {
        exclude("com.google.code.gson")
    }

    listOf("commands-v0", "api-base").forEach {
        implementationInclude("net.fabricmc.fabric-api:fabric-$it:$versionFabricApi")
    }

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")
}

minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}

license {
    header = rootProject.file("LICENSE")
}

tasks.processResources.configure {
    expand("project" to project)
}

bintray {
    pkg.version.name = project.version as String
    pkg.version.released = null
}

opinionated {
    github("PEXPlugins", "text-adapter-fabric")
    mit()
    publication.apply {
        val remapJar by tasks.getting(RemapJarTask::class)
        val remapSourcesJar by tasks.getting(RemapSourcesJarTask::class)
        suppressAllPomMetadataWarnings()

        artifact(tasks.jar.get()) {
            classifier = "dev"
        }
        artifact(remapJar)

        artifact(tasks.getByName("sourcesJar")) {
            builtBy(remapSourcesJar)
        }
        artifact(tasks.getByName("javadocJar"))
    }
}
