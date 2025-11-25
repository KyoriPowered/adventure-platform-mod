import me.modmuss50.mpp.ReleaseType

plugins {
  id("standard-conventions")
  id("net.kyori.indra.publishing")
  id("net.kyori.indra.crossdoc")
  id("me.modmuss50.mod-publish-plugin")
}

indraCrossdoc {
  baseUrl().set(providers.gradleProperty("javadocPublishRoot"))
  nameBasedDocumentationUrlProvider {
    projectNamePrefix = "adventure-platform-"
  }
}

tasks {
  javadoc {
    val options = this.options as StandardJavadocDocletOptions
    exclude("net/kyori/adventure/platform/**/impl/**")
    val client = sourceSets.findByName("client")
    client?.let {
      source(client.allJava)
      classpath += client.output
    }
    val advVersion = libs.versions.adventure.get()
    if (!advVersion.contains("SNAPSHOT")) {
      options.links(
        "https://jd.advntr.dev/api/${advVersion}",
        "https://jd.advntr.dev/key/${advVersion}",
      )
    }
    options.links(
      "https://jd.advntr.dev/platform/api/${libs.versions.adventurePlatform.get()}",
    )
  }
}

publishMods.modrinth {
  projectId = "O5VsIpQY"
  type = ReleaseType.STABLE
  changelog = providers.environmentVariable("RELEASE_NOTES")
  accessToken = providers.environmentVariable("MODRINTH_TOKEN")
  minecraftVersions = providers.gradleProperty("modrinthMinecraftVersions").map {
    it.split(',').map(String::trim)
  }
}
