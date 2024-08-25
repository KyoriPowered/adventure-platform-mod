plugins {
  id("standard-conventions")
  id("net.kyori.indra.publishing")
  id("net.kyori.indra.crossdoc")
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
