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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.network.packet.ChatMessageS2CPacket;
import net.minecraft.client.network.packet.TitleS2CPacket;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.EnumSet;

import static java.util.Objects.requireNonNull;
import static net.kyori.text.TextComponent.*;

public class TextAdapter implements ModInitializer {

    /**
     * Convert Minecraft-internal {@link Text} objects to Kyori-{@link Component Components}
     *
     * If a null value is provided, a null value will be returned
     *
     * @param text The text to convert
     * @return The adapted component
     */
    public static Component toComponent(Text text) {
        if (text == null) {
            return null;
        }
        return GsonComponentSerializer.INSTANCE.deserialize(Text.Serializer.toJson(text));
    }

    /**
     * Convert a Kyori-{@link Component} to Minecraft-internal {@link Text} objects
     *
     * If a null value is provided, a null value will be returned
     *
     * @param component The component to convert
     * @return The adapted text
     */
    public static Text toMcText(Component component) {
        if (component == null) {
            return null;
        }
        return Text.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(component));
    }

    /**
     * Send a message to any number of message receivers. For message receivers that are players,
     * these messages will sent as the {@link MessageType#SYSTEM} message type
     *
     * @param targets The receivers of the message
     * @param text The message to send
     */
    public static void sendMessage(Iterable<? extends CommandOutput> targets, Component text) {
        ChatMessageS2CPacket pkt = null;
        Text mcText = null;

        for (CommandOutput target : targets) {
            if (target instanceof ServerPlayerEntity) {
                if (pkt == null) {
                    pkt = createChatPacket(text, MessageType.SYSTEM);
                }
                ((ServerPlayerEntity) target).networkHandler.sendPacket(pkt);
            } else if (target instanceof ComponentCommandOutput) {
                ((ComponentCommandOutput) target).sendMessage(text);
            } else {
                if (mcText == null) {
                    mcText = toMcText(text);
                }

                target.sendMessage(mcText);
            }
        }
    }

    /**
     * Send a message to a collection of players
     *
     * @param targets The targets to send the message to
     * @param text The text to send
     * @param type The field to send to
     */
    public static void sendMessage(Iterable<? extends ServerPlayerEntity> targets, Component text, MessageType type) {
        if (type == MessageType.GAME_INFO) { // Use title for better appearance
            sendTitle(targets, text, TitleS2CPacket.Action.ACTIONBAR);
            return;
        }

        ChatMessageS2CPacket pkt = createChatPacket(text, type);
        for (ServerPlayerEntity target : targets) {
            target.networkHandler.sendPacket(pkt);
        }
    }

    /**
     * Send a title to multiple players
     *
     * @param targets The players to send a title to
     * @param text The text to send
     * @param type The field of the title to use
     */
    public static void sendTitle(Iterable<? extends ServerPlayerEntity> targets, Component text, TitleS2CPacket.Action type) {
        TitleS2CPacket pkt = createTitlePacket(type, text);
        for (ServerPlayerEntity target : requireNonNull(targets, "targets")) {
            target.networkHandler.sendPacket(pkt);
        }
    }


    public static ChatMessageS2CPacket createChatPacket(Component text, MessageType type) {
        ChatMessageS2CPacket pkt = new ChatMessageS2CPacket(null, type);
        ((ComponentHoldingPacket) pkt).setComponent(text);
        return pkt;
    }

    private static final EnumSet<TitleS2CPacket.Action> ALLOWED_ACTIONS
            = EnumSet.of(TitleS2CPacket.Action.TITLE, TitleS2CPacket.Action.SUBTITLE, TitleS2CPacket.Action.ACTIONBAR);
    public static TitleS2CPacket createTitlePacket(TitleS2CPacket.Action action, Component text) {
        if (!ALLOWED_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("Action provided was not one of supported actions " + ALLOWED_ACTIONS);
        }
        TitleS2CPacket pkt = new TitleS2CPacket(action, null);
        ((ComponentHoldingPacket) pkt).setComponent(text);
        return pkt;
    }

    /// Mod adapter implementation

    private ModContainer container;

    @Override
    public void onInitialize() {
        // not much to do?
        this.container = FabricLoader.getInstance().getModContainer("text-adapter-fabric")
                .orElseThrow(() -> new IllegalStateException("Mod ID for text-adapter-fabric has been changed without updating the initializer!"));
        CommandRegistry.INSTANCE.register(false, disp -> disp.register(createInfoCommand()));
    }

    public LiteralArgumentBuilder<ServerCommandSource> createInfoCommand() {
        return CommandManager.literal("kyoritext")
                .requires(it -> it.hasPermissionLevel(2))
                .executes(ctx -> {
                    String apiVersion = container.getMetadata().getCustomValue("text-version").getAsString();
                    String adapterVersion = container.getMetadata().getVersion().getFriendlyString();
                    ServerCommandSource src = ctx.getSource();
                    ComponentCommandSource component = ComponentCommandSource.of(src);
                    component.sendFeedback(make("KyoriPowered Text ", b -> {
                        b.color(TextColor.GRAY);
                        b.append(highlight("v" + apiVersion));
                        b.append(newline());
                        b.append(make("text-compat-fabric ", v -> v.append(highlight("v" + adapterVersion))));
                    }), false);

                    return 1;
                });
    }

    private TextComponent highlight(String input) {
        return TextComponent.of(input, TextColor.LIGHT_PURPLE, TextDecoration.ITALIC);
    }
}
