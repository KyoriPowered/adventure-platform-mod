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

package net.kyori.adventure.platform.fabric.impl;

import java.util.List;
import java.util.Optional;

import net.kyori.adventure.platform.fabric.FabricAudienceProvider;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class WrappedComponent implements Component {
  private @MonotonicNonNull Component converted;
  private final net.kyori.adventure.text.Component wrapped;

  public WrappedComponent(final net.kyori.adventure.text.Component wrapped) {
    this.wrapped = wrapped;
  }

  public net.kyori.adventure.text.Component wrapped() {
    return this.wrapped;
  }

  Component deepConverted() {
    Component converted = this.converted;
    if(converted == null) {
      converted = this.converted = FabricAudienceProvider.nonWrappingSerializer().serialize(this.wrapped);
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
    return FabricAudienceProvider.plainSerializer().serialize(this.wrapped);
  }

  @Override
  public String getString(final int length) {
    return this.deepConverted().getString(length);
  }

  @Override
  public String getContents() {
    if(this.wrapped instanceof TextComponent) {
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
  public FormattedCharSequence getVisualOrderText() {
    return this.deepConverted().getVisualOrderText();
  }

  @Override
  public <T> Optional<T> visit(final StyledContentConsumer<T> visitor, final Style style) {
    return this.deepConverted().visit(visitor, style);
  }

  @Override
  public <T> Optional<T> visit(final ContentConsumer<T> visitor) {
    return this.deepConverted().visit(visitor);
  }
}
