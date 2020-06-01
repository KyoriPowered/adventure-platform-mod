
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("ca.stellardrift.opinionated.fabric") version "3.0"
    id("ca.stellardrift.opinionated.publish") version "3.0"
}

val versionSelf = "2.0-SNAPSHOT"
val versionMinecraft: String by project
val versionAdventure: String by project
val versionMappings: String by project
val versionLoader: String by project

group = "ca.stellardrift"
version = "$versionSelf+${versionAdventure.replace("-SNAPSHOT", "")}"

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

fun DependencyHandler.adventure(comp: String, version: Any): String {
    return "net.kyori:adventure-$comp:$version"
}

dependencies {
    api(include("net.kyori:examination-api:1.0.0-SNAPSHOT")!!)
    api(include("net.kyori:examination-string:1.0.0-SNAPSHOT")!!)
    api(include(adventure("api", versionAdventure))!!)
    api(include(adventure("text-feature-pagination", versionAdventure))!!)
    api(include(adventure("text-serializer-plain", versionAdventure))!!)

    implementation(include(adventure("text-serializer-gson", versionAdventure)) {
        exclude("com.google.code.gson")
    })

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")
}

license {
    header = rootProject.file("LICENSE")
}

tasks.processResources.configure {
    expand("project" to project)
}

opinionated {
    github("PEXPlugins", "text-adapter-fabric")
    mit()
    publication?.apply {
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
