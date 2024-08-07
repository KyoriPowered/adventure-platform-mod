# adventure-platform-mod

![GitHub Workflow Status (branch)](https://img.shields.io/github/actions/workflow/status/KyoriPowered/adventure-platform-mod/build.yml?branch=mc/1.21) [![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE) [![Maven Central](https://img.shields.io/maven-central/v/net.kyori/adventure-platform-mod-shared?label=stable)](https://search.maven.org/search?q=g:net.kyori%20AND%20a:adventure*) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/net.kyori/adventure-platform-mod-shared?label=dev&server=https%3A%2F%2Fs01.oss.sonatype.org)

Integration between the [adventure](https://github.com/KyoriPowered/adventure) library and *Minecraft: Java Edition* for the [Fabric](https://fabricmc.net) and [NeoForge](https://neoforged.net) modding systems.

See the [documentation](https://docs.adventure.kyori.net/platform/modded.html) for usage and dependency information for this project and the main `adventure` library.

### Versions

`adventure-platform-mod` tends to not work on more than one major version of Minecraft at a time. See the docs for the latest tested versions on each Minecraft release.

### Contributing

We appreciate contributions of any type. For any new features or typo-fix/style changes, please open an issue or come talk to us in our [Discord] first, so we make sure you're going in the right direction for the project.

All the adventure projects are built with Gradle and use a common checkstyle configuration. `adventure-platform-mod` requires the same Java version that Minecraft itself does in the target version. Please make sure all tests pass, license headers are updated, and checkstyle passes to help us review your contribution.

An unfortunate quirk with Loom is that it resolves dependencies in the configuration phase, before any task execution can happen, so we have to run Gradle twice to generate templates (the first without the daemon, since loom caches some information), then apply our in-project interface injections. Sorry!

This looks like:

```sh
$ ./gradlew --no-daemon generateTemplates
$ ./gradlew build
```

To have browsable game source in your IDE, run `./gradlew genSources` before importing the mod

`adventure-platform-mod` is released under the terms of the [MIT License](LICENSE).

[Discord]: https://discord.gg/MMfhJ8F
