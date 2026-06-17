# adventure-platform-mod

[![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE) [![Maven Central](https://img.shields.io/maven-central/v/net.kyori/adventure-platform-mod-shared?label=stable)](https://search.maven.org/search?q=g:net.kyori%20AND%20a:adventure*) ![Maven snapshots](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fnet%2Fkyori%2Fadventure-platform-mod-shared%2Fmaven-metadata.xml&strategy=highestVersion&label=dev) [![Modrinth Version](https://img.shields.io/modrinth/v/adventure-platform-mod?logo=modrinth&label=modrinth)](https://modrinth.com/mod/adventure-platform-mod)

Integration between the [adventure](https://github.com/PaperMC/adventure) library and *Minecraft: Java Edition* for the [Fabric](https://fabricmc.net) and [NeoForge](https://neoforged.net) modding systems.

See the [documentation] for usage and dependency information for this project and the main `adventure` library.

Players and Server Admins: If another mod is asking for `adventure-platform-fabric` or `adventure-platform-neoforge`, you can find the download at Modrinth: https://modrinth.com/mod/adventure-platform-mod.

### Versions

`adventure-platform-mod` tends to not work on more than one major version of Minecraft at a time. See the docs for the latest tested versions on each Minecraft release.

### Contributing

We appreciate contributions of any type.
For any new features or typo-fix/style changes, please open an issue or come talk to us in the [#adventure-contrib] channel on the [PaperMC Discord] first so we make sure you're going in the right direction for the project.

All the adventure projects are built with Gradle and use a common checkstyle configuration. `adventure-platform-mod` requires the same Java version that Minecraft itself does in the target version. Please make sure all tests pass, license headers are updated, and checkstyle passes to help us review your contribution.

An unfortunate quirk with Loom is that it resolves dependencies in the configuration phase, before any task execution can happen, so we have to run Gradle twice to generate templates (the first without the daemon, since loom caches some information), then apply our in-project interface injections. Sorry!

This looks like:

```sh
$ ./gradlew --no-daemon generateTemplates
$ ./gradlew build
```

To have browsable game source in your IDE, run `./gradlew genSources` before importing the mod

`adventure-platform-mod` is released under the terms of the [MIT License](LICENSE).

[documentation]: https://docs.papermc.io/adventure/
[#adventure-contrib]: https://discord.com/channels/289587909051416579/1342377788266512415
[PaperMC Discord]: https://discord.gg/PaperMC
