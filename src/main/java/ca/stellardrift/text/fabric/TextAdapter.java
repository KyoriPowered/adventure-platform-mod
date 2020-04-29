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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.EnumSet;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.TextComponent.*;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Adapter methods for converting text objects between Minecraft and Adventure types
 *
 * @see Audiences for ways to send messages to different groups of people, including players
 * @see ComponentCommandOutput#message(Component) for sending to a single user
 * @see ComponentCommandSource for sending to a single command source
 */
public class TextAdapter implements ModInitializer {
    private static final PlainComponentSerializer PLAIN;
    private static final MinecraftTextSerializer TEXT_NON_WRAPPING = new MinecraftTextSerializer();
    private static final MinecraftWrappingTextSerializer TEXT = new MinecraftWrappingTextSerializer();

    static {
        Function<KeybindComponent, String> keybindNamer;

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            keybindNamer = keybind -> KeyBinding.getLocalizedName(keybind.keybind()).get().asString();
        } else {
            keybindNamer = KeybindComponent::keybind;
        }
        PLAIN = new PlainComponentSerializer(keybindNamer, trans -> TEXT_NON_WRAPPING.toText(trans).asString());
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
    public static MinecraftTextSerializer textNonWrapping() {
        return TEXT_NON_WRAPPING;
    }

    /**
     * Create a new component serializer that will convert between Adventure and Minecraft text objects by wrapping and
     * unwrapping the Adventure versions of the object hierarchy.
     *
     * @return a serializer instance
     */
    public static ComponentSerializer<Component, Component, Text> text() {
        return TEXT;
    }


    /**
     * Convert a MC {@link Identifier} instance to a text Key
     *
     * @param ident The Identifier to convert
     * @return The equivalent data as a Key
     */
    public static @PolyNull Key toKey(@PolyNull Identifier ident) {
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
    public static @PolyNull Identifier toIdentifier(@PolyNull Key key) {
        if (key == null) {
            return null;
        }
        return new Identifier(key.namespace(), key.value());
    }

    /**
     * Send a title to multiple players
     *
     * @param targets The players to send a title to
     * @param text    The text to send
     * @param type    The field of the title to use
     */
    public static void sendTitle(Iterable<? extends ServerPlayerEntity> targets, Component text, TitleS2CPacket.Action type) {
        TitleS2CPacket pkt = createTitlePacket(type, text);
        for (ServerPlayerEntity target : requireNonNull(targets, "targets")) {
            target.networkHandler.sendPacket(pkt);
        }
    }

    public static GameMessageS2CPacket createChatPacket(Component text, MessageType type) {
        return new GameMessageS2CPacket(text().serialize(text), type);
    }

    private static final EnumSet<TitleS2CPacket.Action> ALLOWED_ACTIONS
            = EnumSet.of(TitleS2CPacket.Action.TITLE, TitleS2CPacket.Action.SUBTITLE, TitleS2CPacket.Action.ACTIONBAR);

    public static TitleS2CPacket createTitlePacket(TitleS2CPacket.Action action, Component text) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Action provided was not one of supported actions " + ALLOWED_ACTIONS);
        }
        return new TitleS2CPacket(action, text().serialize(text));
    }

    /// Mod adapter implementation

    private @MonotonicNonNull ModContainer container;

    @Override
    public void onInitialize() {
        // not much to do?
        this.container = FabricLoader.getInstance().getModContainer("text-adapter-fabric")
                .orElseThrow(() -> new IllegalStateException("Mod ID for text-adapter-fabric has been changed without updating the initializer!"));
        CommandRegistry.INSTANCE.register(false, disp -> disp.register(createInfoCommand()));
    }

    private static final String BLOCK = "█"; // full block
    private static final int RGB_SQUARE_SIZE = 10;
    private static final int RGB_WIDTH = RGB_SQUARE_SIZE;
    private static final int RGB_HEIGHT = RGB_SQUARE_SIZE;

    private static Component createColorScale(TextColor start, TextColor end, int steps) {
        final short r0 = start.red(), g0 = start.green(), b0 = start.blue();
        final int dr = (end.red() - r0) / steps, dg = (end.green() - g0) / steps, db = (end.blue() - b0) / steps;
        int r = r0, g = g0, b = b0;
        final TextComponent.Builder build = TextComponent.builder();
        for (int i = 0; i < steps; ++i) {
            build.append(TextComponent.of(BLOCK, TextColor.of(r, g, b)));
            r += dr;
            g += dg;
            b += db;
        }
        return build.build();
    }

    private static Component addFlags(Component original, int width) {
        @Nullable TextColor norm = original.color();
        if (norm == null) {
            norm = NamedTextColor.WHITE;
        }
        final TextColor light = TextColor.of((byte) (norm.red() * 1.1), (byte) (norm.green() * 1.1), (byte) (norm.blue() * 1.1));
        final TextColor dark = TextColor.of((byte) (norm.red() * 0.9), (byte) (norm.green() * 0.9), (byte) (norm.blue() * 0.9));
        final Component lightT = TextComponent.of(BLOCK, light);
        final Component darkT = TextComponent.of(BLOCK, dark);
        final TextComponent.Builder ret = TextComponent.builder();
        for (int i = 0; i < width; ++i) {
            ret.append(i % 2 == 0 ? darkT : lightT);
        }
        ret.append(space()).append(original).append(space());

        for (int i = 0; i < width; ++i) {
            ret.append(i % 2 == 0 ? lightT : darkT);
        }
        return ret.build();
    }

    public LiteralArgumentBuilder<ServerCommandSource> createInfoCommand() {
        return literal("kyoritext")
                .requires(it -> it.hasPermissionLevel(2))
                .then(literal("rgb").executes(ctx -> {
                    final ComponentCommandSource out = ComponentCommandSource.of(ctx.getSource());
                    out.sendFeedback(createColorScale(NamedTextColor.BLACK, TextColor.of(0xFF, 0x00, 0x00), 10), false);
                    out.sendFeedback(createColorScale(NamedTextColor.BLACK, TextColor.of(0x00, 0xFF, 0x00), 10), false);
                    out.sendFeedback(createColorScale(NamedTextColor.BLACK, TextColor.of(0x00, 0x00, 0xFF), 10), false);
                    out.sendFeedback(createColorScale(TextColor.of(0xEF22EE), TextColor.of(0x22AA33), 20), false);
                    out.sendFeedback(addFlags(of("Hello world! 🌎", NamedTextColor.GOLD), 10), false);
                    out.sendFeedback(builder("Hello ").append(of("unicode", Style.builder().font(Key.of("uniform")).build())).build(), true);
                    /*final short r0 = 0, g0 = 0, b0 = 0;
                    final int dr = (end.red() - r0) / steps, dg = (end.green() - g0) / steps, db = (end.blue() - b0) / steps;
                    for (int y = 0; y < RGB_HEIGHT; ++y) {
                        for (int x = 0; x < RGB_WIDTH; ++x) {

                        }
                    }*/
                    return 1;
                }))
                .executes(ctx -> {
                    String apiVersion = container.getMetadata().getCustomValue("text-version").getAsString();
                    String adapterVersion = container.getMetadata().getVersion().getFriendlyString();
                    ServerCommandSource src = ctx.getSource();
                    ComponentCommandSource component = ComponentCommandSource.of(src);
                    component.sendFeedback(make("KyoriPowered Text ", b -> {
                        b.color(NamedTextColor.GRAY);
                        b.append(highlight("v" + apiVersion));
                        b.append(newline());
                        b.append(make("text-adapter-fabric ", v -> v.append(highlight("v" + adapterVersion))));
                    }), false);

                    return 1;
                });
    }

    private TextComponent highlight(String input) {
        return TextComponent.of(input, NamedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC);
    }
}
