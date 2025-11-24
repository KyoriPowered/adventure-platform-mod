plugins {
  alias(libs.plugins.configurateTransformations)
  id("net.fabricmc.fabric-loom-companion")
}

val generateTemplates = createProcessResourceTemplates("generateTemplates", sourceSets.main.get())
