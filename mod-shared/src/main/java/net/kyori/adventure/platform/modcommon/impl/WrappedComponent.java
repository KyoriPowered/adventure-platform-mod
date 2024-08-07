/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrappedComponent implements Component {
  protected Component converted;
  protected @Nullable Object deepConvertedLocalized = null;
  private final net.kyori.adventure.text.Component wrapped;
  private final @Nullable Function<Pointered, ?> partition;
  private final @Nullable ComponentRenderer<Pointered> renderer;
  private final @Nullable NonWrappingComponentSerializer nonWrappingSerializer;
  private @Nullable Object lastData;
  private @Nullable WrappedComponent lastRendered;

  public WrappedComponent(
    final net.kyori.adventure.text.Component wrapped,
    final @Nullable Function<Pointered, ?> partition,
    final @Nullable ComponentRenderer<Pointered> renderer,
    final @Nullable NonWrappingComponentSerializer nonWrappingComponentSerializer
  ) {
    this.wrapped = wrapped;
    this.partition = partition;
    this.renderer = renderer;
    this.nonWrappingSerializer = nonWrappingComponentSerializer;
  }

  /**
   * Renderer to use to translate messages.
   *
   * @return the renderer, if any
   */
  public @Nullable ComponentRenderer<Pointered> renderer() {
    return this.renderer;
  }

  /**
   * Partition to use to generate cache keys.
   *
   * @return the partition, if any
   */
  public @Nullable Function<Pointered, ?> partition() {
    return this.partition;
  }

  public net.kyori.adventure.text.Component wrapped() {
    return this.wrapped;
  }

  public synchronized WrappedComponent rendered(final Pointered ptr) {
    final Object data = this.partition == null ? null : this.partition.apply(ptr);
    if (this.lastData != null && Objects.equals(data, this.lastData)) {
      return this.lastRendered;
    }
    this.lastData = data;
    return this.lastRendered = this.renderer == null ? this : AdventureCommon.HOOKS.createWrappedComponent(this.renderer.render(this.wrapped, ptr), null, null, this.nonWrappingSerializer);
  }

  public Component deepConverted() {
    Component converted = this.converted;
    if (converted == null || this.deepConvertedLocalized != null) {
      ComponentSerializer<net.kyori.adventure.text.Component, net.kyori.adventure.text.Component, Component> serializer = this.nonWrappingSerializer;
      if (serializer == null) {
        serializer = MinecraftAudiences.nonWrappingSerializer();
      }
      converted = this.converted = serializer.serialize(this.wrapped);
      this.deepConvertedLocalized = null;
    }
    return converted;
  }

  @ApiStatus.OverrideOnly
  protected Component deepConvertedLocalized() {
    return this.deepConverted();
  }

  public @Nullable Component deepConvertedIfPresent() {
    return this.converted;
  }

  @Override
  public Style getStyle() {
    return this.deepConverted().getStyle();
  }

  @Override
  public String getString() {
    return PlainTextComponentSerializer.plainText().serialize(this.rendered(AdventureCommon.pointered(Pointers::empty)).wrapped);
  }

  @Override
  public String getString(final int length) {
    return this.deepConverted().getString(length);
  }

  @Override
  public ComponentContents getContents() {
    if (this.wrapped instanceof TextComponent text) {
      return new PlainTextContents.LiteralContents(text.content());
    } else {
      return this.deepConverted().getContents();
    }
  }

  @Override
  public List<Component> getSiblings() {
    return this.deepConverted().getSiblings();
  }

  @Override
  public MutableComponent plainCopy() {
    return this.deepConverted().plainCopy();
  }

  @Override
  public MutableComponent copy() {
    return this.deepConverted().copy();
  }

  @Override
  public FormattedCharSequence getVisualOrderText() {
    return this.deepConvertedLocalized().getVisualOrderText();
  }

  @Override
  public <T> Optional<T> visit(final StyledContentConsumer<T> visitor, final Style style) {
    return this.deepConvertedLocalized().visit(visitor, style);
  }

  @Override
  public <T> Optional<T> visit(final ContentConsumer<T> visitor) {
    return this.deepConverted().visit(visitor);
  }

  // @Override TODO
  public net.kyori.adventure.text.@NotNull Component asComponent() {
    return this.wrapped;
  }
}
