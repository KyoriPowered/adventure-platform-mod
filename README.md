# adventure-platform-fabric

![GitHub Workflow Status (branch)](https://img.shields.io/github/workflow/status/KyoriPowered/adventure-platform-fabric/build/master) [![MIT License](https://img.shields.io/badge/license-MIT-blue)](LICENSE) [![Maven Central](https://img.shields.io/maven-central/v/net.kyori/adventure-platform-fabric?label=stable)](https://search.maven.org/search?q=g:net.kyori%20AND%20a:adventure*) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/net.kyori/adventure-platform-fabric?label=dev&server=https%3A%2F%2Fs01.oss.sonatype.org)

Integration between the [adventure](https://github.com/KyoriPowered/adventure) library and *Minecraft: Java Edition* using the [Fabric](https://fabricmc.net) modding system.

See the [documentation](https://docs.adventure.kyori.net/platform/fabric.html) for usage and dependency information for this project and the main `adventure` library.

### Versions

`adventure-platform-fabric` tends to not work on more than one major version of Minecraft at a time. We test the following version combinations:

Minecraft Version | `adventure-platform-fabric` version
----------------- | ------------------------------------
1.16.x            | `4.0.0`
1.17.x            | `4.1.0`
1.18.1            | `5.1.0`
1.18.2            | `5.3.1`
1.19              | `5.4.0`
1.19.1 / 1.19.2   | `5.5.0`
1.19.3            | `5.6.0-SNAPSHOT`

### Contributing

We appreciate contributions of any type. For any new features or typo-fix/style changes, please open an issue or come talk to us in our [Discord] first, so we make sure you're going in the right direction for the project.

All the adventure projects are built with Gradle and use a common checkstyle configuration. `adventure-platform-fabric` requires the same Java version that Minecraft itself does in the target version. Please make sure all tests pass, license headers are updated, and checkstyle passes to help us review your contribution.

An unfortunate quirk with Loom is that it resolves dependencies in the configuration phase, before any task execution can happen, so we have to run Gradle twice to generate templates (the first without the daemon, since loom caches some information), then apply our in-project interface injections. Sorry!

This looks like:

```sh
$ ./gradlew --no-daemon generateTemplates
$ ./gradlew build
```

To have browseable game source in your IDE, run `./gradlew genSources` before importing the mod

`adventure-platform-fabric` is released under the terms of the [MIT License](LICENSE).

[Discord]: https://discord.gg/MMfhJ8F
