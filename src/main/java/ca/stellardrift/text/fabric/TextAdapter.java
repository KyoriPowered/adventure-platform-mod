/*
 * Copyright © 2020 zml [at] stellardrift [.] ca
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.stellardrift.text.fabric;

import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import static java.util.Objects.requireNonNull;

/**
 * Adapter methods for converting text objects between Minecraft and Adventure types
 *
 * @see Audiences for ways to send messages to different groups of people, including players
 * @see CommandOutputAudience#sendMessage(Component) for sending to a single user
 * @see AdventureCommandSource for sending to a single command source
 */
public class TextAdapter implements ModInitializer {
    private static @Nullable MinecraftServer server;
    private static final PlainComponentSerializer PLAIN;
    private static final MinecraftTextSerializer TEXT_NON_WRAPPING = new MinecraftTextSerializer();

    /**
     * Get the active server instance. This instance may change
     * throughout the lifetime of a game if we are running with an integrated server.
     *
     * @return the server instance
     */
    static @Nullable MinecraftServer server() {
        @Nullable MinecraftServer instance = server;
        return server;
    }

    static {
        Function<KeybindComponent, String> keybindNamer;

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            keybindNamer = keybind -> KeyBinding.getLocalizedName(keybind.keybind()).get().asString();
        } else {
            keybindNamer = KeybindComponent::keybind;
        }
        PLAIN = new PlainComponentSerializer(keybindNamer, trans -> adapt(trans).asString());
    }

    /**
     * Return a {@link PlainComponentSerializer} instance that can resolve key bindings and translations using the game's data
     *
     * @return the plain serializer instance
     */
    public static PlainComponentSerializer plain() {
        return PLAIN;
    }

    /**
     * Return a TextSerializer instance that will do deep conversions between Adventure {@link Component Components} and Minecraft {@link Text} objects.
     * <p>
     * This serializer will never wrap text, and can provide {@link net.minecraft.text.MutableText} instances suitable for passing around the game.
     *
     * @return a serializer instance
     */
    public static MinecraftTextSerializer nonWrapping() {
        return TEXT_NON_WRAPPING;
    }


    public static Text adapt(final Component component) {
        return new ComponentText(component);
    }

    public static Component adapt(final Text text) {
        if (text instanceof ComponentText) {
            return ((ComponentText) text).getWrapped();
        }
        return nonWrapping().deserialize(text);
    }

    public static Text update(final Text input, final UnaryOperator<Component> modifier) {
        Component modified;
        if (input instanceof ComponentText) {
            modified = requireNonNull(modifier).apply(((ComponentText) input).getWrapped());
        } else {
            final Component original = nonWrapping().deserialize(input);
            modified = modifier.apply(original);
        }
        return new ComponentText(modified);
    }


    /**
     * Convert a MC {@link Identifier} instance to a text Key
     *
     * @param ident The Identifier to convert
     * @return The equivalent data as a Key
     */
    public static @PolyNull Key adapt(@PolyNull final Identifier ident) {
        if (ident == null) {
            return null;
        }
        return Key.of(ident.getNamespace(), ident.getPath());
    }


    /**
     * Convert a Kyori {@link Key} instance to a MC Identifier
     *
     * @param key The Key to convert
     * @return The equivalent data as an Identifier
     */
    public static @PolyNull Identifier adapt(@PolyNull final Key key) {
        if (key == null) {
            return null;
        }
        return new Identifier(key.namespace(), key.value());
    }

    /// Mod adapter implementation

    private @MonotonicNonNull ModContainer container;

    @Override
    public void onInitialize() {
        // not much to do?
        // TODO: make this client-optional when commands use it
        ArgumentTypes.register("adventure:component", ComponentArgumentType.class, new ComponentArgumentType.Serializer());

        final Object game = FabricLoader.getInstance().getGameInstance();
        if (game instanceof MinecraftServer) {
            server = (MinecraftServer) game;
        }

        this.container = FabricLoader.getInstance().getModContainer("text-adapter-fabric")
                .orElseThrow(() -> new IllegalStateException("Mod ID for text-adapter-fabric has been changed without updating the initializer!"));
    }
}
