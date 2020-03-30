# text-compat-fabric

[![Release](https://jitpack.io/v/ca.stellardrift/text-adapter-fabric.svg)](https://jitpack.io/#ca.stellardrift/text-adapter-fabric) | [Javadoc](https://jitpack.io/ca/stellardrift/text-adapter-fabric/master-SNAPSHOT/javadoc/)

This mod integrates the Kyori text library with Fabric and Minecraft's Text format. As this is a library mod, it's most likely only useful for developers.

Currently, we support the following features:

- Converting between Component and Text instances using `TextAdapter.toComponent(Text)` and `TextAdapter.toMcText(Component)`
- Sending Components directly to ServerPlayerEntities, as chat messages and titles, by using methods on `ComponentPlayer.of(ServerPlayerEntity)`
- Sending Components to ServerCommandSources as either feedback or an error, using methods on `ComponentCommandSource.of(ServerCommandSource)`
- Sending Components to an unknown `CommandOutput` using methods on `ComponentCommandOutput.of(CommandOutput)`
- Sending Components to a number of viewers using the appropriate conversings, with `TextAdapter.sendMessage(...)` and `TextAdapter.sendTitle(...)`

## Usage

We're on Jitpack. Here's how to add us. Including this library as a jar-in-jar is recommended

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



### Integration

The fastest way to get started sending messages is using methods in the TextAdapter class to send responses.


