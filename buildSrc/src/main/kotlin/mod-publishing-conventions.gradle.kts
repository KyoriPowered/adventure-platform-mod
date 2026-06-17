import me.modmuss50.mpp.ReleaseType

plugins {
  id("publishing-conventions")
  id("me.modmuss50.mod-publish-plugin")
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
