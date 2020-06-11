# adventures-platform-fabric

 [ ![Download](https://api.bintray.com/packages/zml/stellardrift-repo/text-adapter-fabric/images/download.svg) ](https://bintray.com/zml/stellardrift-repo/text-adapter-fabric/_latestVersion) | [Javadoc](https://jitpack.io/ca/stellardrift/text-adapter-fabric/master-SNAPSHOT/javadoc/)

This mod integrates the [Kyori text](https://github.com/KyoriPowered/text) library with Fabric and Minecraft's Text format. As this is a library mod, it's most likely only useful for other mod developers.

Currently, we support the following features:

- Converting between Component and Text instances using `TextAdapter.toComponent(Text)` and `TextAdapter.toMcText(Component)`
- Sending Components directly to ServerPlayerEntities, as chat messages and titles, by using methods on `ComponentPlayer.of(ServerPlayerEntity)`
- Sending Components to ServerCommandSources as either feedback or an error, using methods on `ComponentCommandSource.of(ServerCommandSource)`
- Sending Components to any `CommandOutput` using methods on `ComponentCommandOutput.of(CommandOutput)`
- Sending Components to a number of viewers using the appropriate conversions, with the `TextAdapter.sendMessage(...)` and `TextAdapter.sendTitle(...)` methods

## Usage

We're licensed under the MIT license and published on JCenter. Here's how to add us. Including this library as a jar-in-jar is recommended

```kotlin
repositories {
    jcenter()
}

dependencies {
    // [...]
    modImplementation("ca.stellardrift:text-adapter-fabric:1.0.1+3.0.4")
    include("ca.stellardrift:text-adapter-fabric:1.0.1+3.0.4")
    // [...]
}
```

To test that the mod is installed and functioning correctly, the `/kyoritext` command is available to any user with at least operator level 2

