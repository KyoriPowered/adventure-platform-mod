plugins {
  alias(libs.plugins.configurateTransformations)
}

val generateTemplates = createProcessResourceTemplates("generateTemplates", sourceSets.main.get())
