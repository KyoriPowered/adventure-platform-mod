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

import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

/**
 * A fallback attempt to convert text types, so that one library can sort of work across minecraft versions.
 *
 * This is kind of ugly, but it will minimize breakage across versions (at least on this codebase)
 */
class Fallback {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final String MINECRAFT_PREFIX = "net.minecraft.";

    private static Class<?> findMinecraftClass(String... names) throws ClassNotFoundException {
        for (String name : names) {
            try {
                return Class.forName(MINECRAFT_PREFIX + name);
            } catch (ClassNotFoundException ignore) {
            }
        }
        throw new ClassNotFoundException("None of " + Arrays.toString(names) + " were found as classes");
    }

    private static @Nullable MethodHandle findStatic(MethodType type, String... names) {
        for (String methodName : names) {
            try {
                return LOOKUP.findStatic(Text.Serializer.class, methodName, type);
            } catch (NoSuchMethodException | IllegalAccessException ignore) {
            }
        }
        return null;
    }

    /**
     * method taking Json String and returning MC Text object
     *
     * {@code Text.Serializer.fromJson(String)Text} in 1.15
     * {@code Text.Serializer.fromJson(String)MutableText} in 1.16
     */
    private static final MethodHandle DESERIALIZE;

    static {
        Class<?> deserializeReturn;
        try {
            deserializeReturn = findMinecraftClass("class_5250", "text.MutableText", // >= 1.16
                    "class_2561", "text.Text"); // <= 1.15
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Could not find Text type");
        }

        DESERIALIZE = findStatic(MethodType.methodType(deserializeReturn, String.class), "method_10877", "fromJson");

        if (DESERIALIZE == null) {
            throw new ExceptionInInitializerError("Unable to determine Text$Serializer.fromJson method. Please check that text-adapter-fabric is up to date");
        }
    }

    private static final MethodHandle CHAT_MESSAGE_CONSTRUCTOR;

    static {
        MethodHandle chatConstructor;

        try {
            chatConstructor = LOOKUP.findConstructor(ChatMessageS2CPacket.class, MethodType.methodType(void.class, Text.class, MessageType.class, UUID.class));
        } catch(NoSuchMethodException | IllegalAccessException ex) {
            try {
                chatConstructor = LOOKUP.findConstructor(ChatMessageS2CPacket.class, MethodType.methodType(void.class, Text.class, MessageType.class));
                chatConstructor = MethodHandles.dropArguments(chatConstructor, 2, UUID.class);
            } catch(NoSuchMethodException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        CHAT_MESSAGE_CONSTRUCTOR = chatConstructor;
    }

    private Fallback() {
    }

    static ChatMessageS2CPacket newChatPacket(Text text, MessageType type, UUID senderId) {
        try {
            return (ChatMessageS2CPacket) CHAT_MESSAGE_CONSTRUCTOR.invoke(text, type, senderId);
        } catch(Throwable throwable) {
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }

            throw new RuntimeException(throwable);
        }
    }

    public static Object toText(Component component) {
        try {
            return DESERIALIZE.invoke(GsonComponentSerializer.INSTANCE.serialize(component));
        } catch (Throwable t) { // we can't handle :(
            throw new IllegalStateException(t);
        }
    }

    public static Component toComponent(Text text) {
        return GsonComponentSerializer.INSTANCE.deserialize(Text.Serializer.toJson(text));
    }
}
