
import ca.stellardrift.build.common.adventure
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("fabric-loom") version "0.4-SNAPSHOT"
    id("ca.stellardrift.opinionated.fabric") version "3.0"
    id("ca.stellardrift.opinionated.publish") version "3.0"
}

val versionSelf = "2.0-SNAPSHOT"
val versionMinecraft: String by project
val versionAdventure: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project

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

    // Testmod TODO figure out own scope
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")
}

license {
    header = rootProject.file("LICENSE")
}

tasks.withType(ProcessResources::class).configureEach {
    filesMatching("fabric.mod.json") {
        expand("project" to project)
    }
}

tasks.withType(Javadoc::class).configureEach {
    val options = this.options
    if (options is StandardJavadocDocletOptions) {
        options.tags = listOf("reason:m:Reason for overwrite:") // Add Mixin @reason JD tag definition
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
