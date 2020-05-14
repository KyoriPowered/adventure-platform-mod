import ca.stellardrift.build.apiInclude
import ca.stellardrift.build.implementationInclude
import ca.stellardrift.build.isRelease
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("ca.stellardrift.opinionated") version "2.0"
    id("ca.stellardrift.opinionated.publish") version "2.0"
    id("fabric-loom") version "0.4.3"
}

val versionSelf = "1.0.1"
val versionMinecraft: String by project
val versionAdventure: String by project
val versionMappings: String by project
val versionLoader: String by project

group = "ca.stellardrift"
version = "$versionSelf+$versionAdventure"

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
    apiInclude("net.kyori:examination-api:1.0.0-SNAPSHOT")
    apiInclude("net.kyori:examination-string:1.0.0-SNAPSHOT")
    apiInclude(adventure("api", versionAdventure))
    apiInclude(adventure("text-feature-pagination", versionAdventure))
    apiInclude(adventure("text-serializer-plain", versionAdventure))

    implementationInclude(adventure("text-serializer-gson", versionAdventure)) {
        exclude("com.google.code.gson")
    }

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
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

val gitInfo by tasks.registering {
    doLast {
        println("Is release: ${isRelease()}")
        val tags = grgit.repository.jgit.tagList().call()
        val tagName = tags?.lastOrNull()?.name
        println("Tag name: $tagName")
        val grgitTag = grgit.resolve.toTag(tagName)
        println("JGit tag: ${tags.lastOrNull()}")
        println("Grgit tag: $grgitTag")
        println("Commit: " + grgit.head())
        println("Tag is commit: ${grgitTag.commit == grgit.head()}")
    }

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
