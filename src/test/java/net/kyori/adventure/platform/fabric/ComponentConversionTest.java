/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2023 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.impl.AdventureCommon;
import net.kyori.adventure.platform.fabric.impl.WrappedComponent;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.kyori.adventure.text.format.Style.style;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Named.named;

class ComponentConversionTest extends BootstrappedTest {
  private static final Gson PRETTY_GSON = new GsonBuilder()
    .setPrettyPrinting()
    .create();

  static Stream<Object> testedComponents() {
    return Stream.of(
      Component.text("Hi"),
      Component.translatable("gameMode.creative", style(NamedTextColor.RED).font(Key.key("uniform")))
    )
      .map(comp -> named(MiniMessage.miniMessage().serialize(comp), comp));
  }

  @ParameterizedTest
  @MethodSource("testedComponents")
  void testComponentEqualSerializationWrapped(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(input);
    final JsonElement serializedNative = net.minecraft.network.chat.Component.Serializer.toJsonTree(this.toNativeWrapped(input));

    assertJsonTreesEqual(serializedNative, serialized);
  }

  @ParameterizedTest
  @MethodSource("testedComponents")
  void testNonWrappingSerializerComponentsEqual(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(input);
    final JsonElement serializedNative = net.minecraft.network.chat.Component.Serializer.toJsonTree(net.minecraft.network.chat.Component.Serializer.fromJson(serialized));

    assertJsonTreesEqual(serializedNative, serialized);
  }

  @ParameterizedTest
  @MethodSource("testedComponents")
  void testComponentEqualSerializationWrappedAfterDeepConversion(final Component input) {
    final JsonElement serialized = GsonComponentSerializer.gson().serializeToTree(input);
    final net.minecraft.network.chat.Component mc = this.toNativeWrapped(input);
    mc.getStyle(); // trigger deep conversion
    final JsonElement serializedNative = net.minecraft.network.chat.Component.Serializer.toJsonTree(this.toNativeWrapped(input));

    assertJsonTreesEqual(serializedNative, serialized);
  }

  private static void assertJsonTreesEqual(final JsonElement expected, final JsonElement actual) {
    assertEquals(PRETTY_GSON.toJson(expected), PRETTY_GSON.toJson(actual));
  }

  private WrappedComponent toNativeWrapped(final Component component) {
    final Function<Pointered, Locale> partition = AdventureCommon.localePartition();
    return new WrappedComponent(component, partition, GlobalTranslator.renderer().mapContext(partition));
  }
}
