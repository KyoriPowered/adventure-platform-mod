/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2021 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

public final class WrappedComponent implements Component {
  private Component converted;
  private @Nullable Locale deepConvertedLocalized = null;
  private final net.kyori.adventure.text.Component wrapped;
  private final @Nullable ComponentRenderer<Locale> renderer;
  private @Nullable Locale lastLocale;
  private @Nullable WrappedComponent lastRendered;

  public WrappedComponent(final net.kyori.adventure.text.Component wrapped, final @Nullable ComponentRenderer<Locale> renderer) {
    this.wrapped = wrapped;
    this.renderer = renderer;
  }

  /**
   * Renderer to use to translate messages.
   *
   * @return the renderer, if any
   */
  public @Nullable ComponentRenderer<Locale> renderer() {
    return this.renderer;
  }

  public net.kyori.adventure.text.Component wrapped() {
    return this.wrapped;
  }

  public synchronized WrappedComponent rendered(final Locale locale) {
    if (Objects.equals(locale, this.lastLocale)) {
      return this.lastRendered;
    }
    this.lastLocale = locale;
    return this.lastRendered = this.renderer == null ? this : new WrappedComponent(this.renderer.render(this.wrapped, locale), null);
  }

  Component deepConverted() {
    Component converted = this.converted;
    if (converted == null || this.deepConvertedLocalized != null) {
      converted = this.converted = FabricAudiences.nonWrappingSerializer().serialize(this.wrapped);
      this.deepConvertedLocalized = null;
    }
    return converted;
  }

  @Environment(EnvType.CLIENT)
  Component deepConvertedLocalized() {
    Component converted = this.converted;
    final Locale target = ((LocaleHolderBridge) Minecraft.getInstance().options).adventure$locale();
    if (converted == null || this.deepConvertedLocalized != target) {
      converted = this.converted = this.rendered(target).deepConverted();
      this.deepConvertedLocalized = target;
    }
    return converted;
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
    return AdventureCommon.PLAIN.serialize(this.rendered(Locale.getDefault()).wrapped);
  }

  @Override
  public String getString(final int length) {
    return this.deepConverted().getString(length);
  }

  @Override
  public String getContents() {
    if (this.wrapped instanceof TextComponent) {
      return ((TextComponent) this.wrapped).content();
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
  @Environment(EnvType.CLIENT)
  public FormattedCharSequence getVisualOrderText() {
    return this.deepConvertedLocalized().getVisualOrderText();
  }

  @Override
  @Environment(EnvType.CLIENT)
  public <T> Optional<T> visit(final StyledContentConsumer<T> visitor, final Style style) {
    return this.deepConvertedLocalized().visit(visitor, style);
  }

  @Override
  public <T> Optional<T> visit(final ContentConsumer<T> visitor) {
    return this.deepConverted().visit(visitor);
  }
}
