
import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.sonatypeOss

plugins {
    id("ca.stellardrift.opinionated.fabric") version "3.1"
    id("ca.stellardrift.opinionated.publish") version "3.1"
}

val versionSelf = "2.0-SNAPSHOT"
val versionMinecraft: String by project
val versionAdventure: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project

group = "net.kyori"
version = "$versionSelf+${versionAdventure.replace("-SNAPSHOT", "")}"

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
    sonatypeOss()
}

dependencies {
    api(include("net.kyori:examination-api:1.0.0-SNAPSHOT")!!)
    api(include("net.kyori:examination-string:1.0.0-SNAPSHOT")!!)
    api(include(adventure("api", versionAdventure))!!)
    api(include(adventure("text-feature-pagination", versionAdventure))!!)
    api(include(adventure("text-serializer-plain", versionAdventure))!!)
    api(include(adventure("platform-common-api", versionAdventure))!!)

    implementation(include(adventure("text-serializer-gson", versionAdventure)) {
        exclude("com.google.code.gson")
    })
    modImplementation("ca.stellardrift:colonel:0.1")

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")

    // Testmod TODO figure out own scope
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")
}

tasks.withType(ProcessResources::class).configureEach {
    filesMatching("fabric.mod.json") {
        expand("project" to project)
    }
}

opinionated {
    github("KyoriPowered", "adventure-platform-fabric")
    mit()
    useJUnit5()
}
