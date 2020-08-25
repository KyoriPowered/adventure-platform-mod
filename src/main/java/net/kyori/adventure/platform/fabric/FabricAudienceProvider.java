/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2020 KyoriPowered
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

import static java.util.Objects.requireNonNull;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.fabric.impl.FabricAudienceProviderImpl;
import net.kyori.adventure.platform.fabric.impl.WrappedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.Locale;
import java.util.function.UnaryOperator;

public interface FabricAudienceProvider extends AudienceProvider {

  static @NonNull FabricAudienceProvider of(@NonNull MinecraftServer server) {
    return new FabricAudienceProviderImpl(requireNonNull(server, "server"), (component, ctx) -> component);
  }

  static @NonNull FabricAudienceProvider of(@NonNull MinecraftServer server, @NonNull ComponentRenderer<Locale> renderer) {
    return new FabricAudienceProviderImpl(requireNonNull(server, "server"), requireNonNull(renderer, "renderer"));
  }

  /**
   * Return a {@link PlainComponentSerializer} instance that can resolve key bindings and translations using the game's data
   *
   * @return the plain serializer instance
   */
  static PlainComponentSerializer plainSerializer() {
    return FabricAudienceProviderImpl.PLAIN;
  }

  /**
   * Return a TextSerializer instance that will do deep conversions between
   * Adventure {@link Component Components} and Minecraft {@link net.minecraft.network.chat.Component Components}.
   * <p>
   * This serializer will never wrap text, and can provide {@link net.minecraft.network.chat.MutableComponent}
   * instances suitable for passing around the game.
   *
   * @return a serializer instance
   */
  static ComponentSerializer<Component, Component, net.minecraft.network.chat.Component> nonWrappingSerializer() {
    return FabricAudienceProviderImpl.TEXT_NON_WRAPPING;
  }

  static net.minecraft.network.chat.Component adapt(Component component) {
    return new WrappedComponent(component);
  }

  static Component adapt(net.minecraft.network.chat.Component text) {
    if(text instanceof WrappedComponent) {
      return ((WrappedComponent) text).wrapped();
    }
    return nonWrappingSerializer().deserialize(text);
  }

  static net.minecraft.network.chat.Component update(net.minecraft.network.chat.Component input, UnaryOperator<Component> modifier) {
    final Component modified;
    if(input instanceof WrappedComponent) {
      modified = requireNonNull(modifier).apply(((WrappedComponent) input).wrapped());
    } else {
      final Component original = nonWrappingSerializer().deserialize(input);
      modified = modifier.apply(original);
    }
    return new WrappedComponent(modified);
  }

  /**
   * Convert a MC {@link ResourceLocation} instance to a text Key
   *
   * @param loc The Identifier to convert
   * @return The equivalent data as a Key
   */
  static @PolyNull Key adapt(@PolyNull ResourceLocation loc) {
    if(loc == null) {
      return null;
    }
    return Key.of(loc.getNamespace(), loc.getPath());
  }

  /**
   * Convert a Kyori {@link Key} instance to a MC ResourceLocation
   *
   * @param key The Key to convert
   * @return The equivalent data as an Identifier
   */
  static @PolyNull ResourceLocation adapt(@PolyNull Key key) {
    if(key == null) {
      return null;
    }
    return new ResourceLocation(key.namespace(), key.value());
  }

  /**
   * Get an audience to send to a {@link CommandSourceStack}.
   *
   * This will delegate to the native implementation by the command source, or
   * otherwise use a wrapping implementation.
   *
   * @param source source to send to.
   * @return the audience
   */
  AdventureCommandSourceStack audience(@NonNull CommandSourceStack source);

  Audience audience(@NonNull CommandSource output);

  Audience audience(@NonNull Iterable<ServerPlayer> players);
}
