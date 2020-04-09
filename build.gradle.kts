import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    `java-library`
    `maven-publish`
    id("com.github.hierynomus.license") version "0.15.0"
    id("fabric-loom") version "0.2.7-SNAPSHOT"
}

group = "ca.stellardrift"
version = "3.0.3-SNAPSHOT"


repositories {
    jcenter()
    mavenCentral()
}

fun DependencyHandlerScope.apiInclude(dep: String, configure: ExternalModuleDependency.() -> Unit = {}) {
    include(dep, configure)
    modApi(dep, configure)
}

fun DependencyHandlerScope.implementationInclude(dep: String, configure: ExternalModuleDependency.() -> Unit = {}) {
    include(dep, configure)
    modImplementation(dep, configure)
}

val versionMinecraft = ext["version.minecraft"] as String
val versionText = ext["version.text"] as String
val versionMappings = ext["version.mappings"] as String
val versionLoader = ext["version.loader"] as String
val versionFabricApi = ext["version.fabricApi"] as String

dependencies {
    apiInclude("net.kyori:text-api:$versionText")
    apiInclude("net.kyori:text-feature-pagination:$versionText")
    apiInclude("net.kyori:text-serializer-plain:$versionText")
    implementationInclude("net.kyori:text-serializer-gson:$versionText") {
        exclude("com.google.code.gson")
    }


    listOf("commands-v0", "api-base").forEach {
        implementationInclude("net.fabricmc.fabric-api:fabric-$it:$versionFabricApi")
    }

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")


    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}

license {
    mapping("java", "SLASHSTAR_STYLE")
    include("**/*.java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Javadoc::class) {
    options {
        this.encoding = "UTF-8"
    }
}

tasks.processResources {
    expand("project" to project)
}

tasks.withType(Test::class) {
    // Use junit platform for unit tests
    useJUnitPlatform()
}

val remapJar by tasks.getting(RemapJarTask::class)
val remapSourcesJar by tasks.getting(RemapSourcesJarTask::class)

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(tasks.classes)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}


publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            suppressAllPomMetadataWarnings()

            artifact(tasks.jar.get()) {
                classifier = "dev"
            }
            artifact(remapJar)

            artifact(sourcesJar.get()) {
                builtBy(remapSourcesJar)
            }
            artifact(javadocJar.get())
        }
    }
    repositories {
        mavenLocal()
    }
}

