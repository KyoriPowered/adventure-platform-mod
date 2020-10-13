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

import java.util.Locale;
import java.util.function.UnaryOperator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.fabric.impl.WrappedComponent;
import net.kyori.adventure.platform.fabric.impl.accessor.ComponentSerializerAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.PolyNull;

import static java.util.Objects.requireNonNull;

/**
 * Common operations in both the client and server environments.
 *
 * <p>See {@link FabricServerAudiences} for logical server-specific operations,
 * and {@link FabricClientAudiences} for logical client-specific operations</p>
 * @since 4.0.0
 */
public interface FabricAudiences {
  /**
   * Return a {@link PlainComponentSerializer} instance that can resolve key bindings and translations using the game's data
   *
   * @return the plain serializer instance
   * @since 4.0.0
   */
  PlainComponentSerializer plainSerializer();

  /**
   * Given an existing native component, convert it into an Adventure component for working with.
   *
   * @param input source component
   * @param modifier operator to transform the component
   * @return new component
   * @since 4.0.0
   */
  static net.minecraft.network.chat.Component update(net.minecraft.network.chat.Component input, UnaryOperator<Component> modifier) {
    final Component modified;
    final /* @Nullable */ ComponentRenderer<Locale> renderer;
    if(input instanceof WrappedComponent) {
      modified = requireNonNull(modifier).apply(((WrappedComponent) input).wrapped());
      renderer = ((WrappedComponent) input).renderer();
    } else {
      final Component original = ComponentSerializerAccess.getGSON().fromJson(net.minecraft.network.chat.Component.Serializer.toJsonTree(input), Component.class);
      modified = modifier.apply(original);
      renderer = null;
    }
    return new WrappedComponent(modified, renderer);
  }

  /**
   * Convert a MC {@link ResourceLocation} instance to a text Key
   *
   * @param loc The Identifier to convert
   * @return The equivalent data as a Key
   * @since 4.0.0
   */
  static @PolyNull Key toAdventure(@PolyNull ResourceLocation loc) {
    if(loc == null) {
      return null;
    }
    return Key.key(loc.getNamespace(), loc.getPath());
  }

  /**
   * Convert a Kyori {@link Key} instance to a MC ResourceLocation
   *
   * @param key The Key to convert
   * @return The equivalent data as an Identifier
   * @since 4.0.0
   */
  static @PolyNull ResourceLocation toNative(@PolyNull Key key) {
    if(key == null) {
      return null;
    }
    return new ResourceLocation(key.namespace(), key.value());
  }

  /**
   * Return a TextSerializer instance that will do deep conversions between
   * Adventure {@link Component Components} and Minecraft {@link net.minecraft.network.chat.Component Components}.
   * <p>
   * This serializer will never wrap text, and can provide {@link net.minecraft.network.chat.MutableComponent}
   * instances suitable for passing around the game.
   *
   * @return a serializer instance
   * @since 4.0.0
   */
  static ComponentSerializer<Component, Component, net.minecraft.network.chat.Component> nonWrappingSerializer() {
    return NonWrappingComponentSerializer.INSTANCE;
  }

  /**
   * Active locale-based renderer for operations on provided audiences.
   *
   * @return Shared renderer
   * @since 4.0.0
   */
  ComponentRenderer<Locale> localeRenderer();

  /**
   * Get a native {@link net.minecraft.network.chat.Component} from an adventure {@link Component}.
   *
   * <p>The specific type of the returned component is undefined. For example, it may be a wrapper object.</p>
   *
   * @param adventure adventure input
   * @return native representation
   * @since 4.0.0
   */
  net.minecraft.network.chat.Component toNative(final Component adventure);

  /**
   * Get an adventure {@link Component} from a native {@link net.minecraft.network.chat.Component}
   *
   * @param vanilla the native component
   * @return adventure component
   * @since 4.0.0
   */
  Component toAdventure(final net.minecraft.network.chat.Component vanilla);
}
