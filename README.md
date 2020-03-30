# text-compat-fabric

[![Release](https://jitpack.io/v/ca.stellardrift/text-adapter-fabric.svg)](https://jitpack.io/#ca.stellardrift/text-adapter-fabric) | [Javadoc](https://jitpack.io/ca/stellardrift/text-adapter-fabric/master-SNAPSHOT/javadoc/)

This mod integrates the [Kyori text](https://github.com/KyoriPowered/text) library with Fabric and Minecraft's Text format. As this is a library mod, it's most likely only useful for other mod developers.

Currently, we support the following features:

- Converting between Component and Text instances using `TextAdapter.toComponent(Text)` and `TextAdapter.toMcText(Component)`
- Sending Components directly to ServerPlayerEntities, as chat messages and titles, by using methods on `ComponentPlayer.of(ServerPlayerEntity)`
- Sending Components to ServerCommandSources as either feedback or an error, using methods on `ComponentCommandSource.of(ServerCommandSource)`
- Sending Components to any `CommandOutput` using methods on `ComponentCommandOutput.of(CommandOutput)`
- Sending Components to a number of viewers using the appropriate conversions, with the `TextAdapter.sendMessage(...)` and `TextAdapter.sendTitle(...)` methods

## Usage

We're licensed under the MIT license and published on [Jitpack](https://jitpack.io). Here's how to add us. Including this library as a jar-in-jar is recommended

```kotlin
repositories {
    maven(url="https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    // [...]
    modImplementation("ca.stellardrift:text-adapter-fabric:3.0.3-SNAPSHOT")
    include("ca.stellardrift:text-adapter-fabric:3.0.3-SNAPSHOT")
    // [...]
}
```

To test that the mod is installed and functioning correctly, the `/kyoritext` command is available to any user with at least operator level 2

