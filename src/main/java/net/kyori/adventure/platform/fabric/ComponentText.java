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

import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComponentText implements Text {
  private @MonotonicNonNull Text converted;
  private final Component wrapped;

  ComponentText(final Component wrapped) {
    this.wrapped = wrapped;
  }

  public Component wrapped() {
    return this.wrapped;
  }

   Text deepConverted() {
    Text converted = this.converted;
    if(converted == null) {
      converted = this.converted = FabricPlatform.nonWrappingSerializer().serialize(this.wrapped);
    }
    return converted;
  }

  public @Nullable Text deepConvertedIfPresent() {
    return this.converted;
  }

  @Override
  public Style getStyle() {
    return this.deepConverted().getStyle();
  }

  @Override
  public String getString() {
    return FabricPlatform.plainSerializer().serialize(this.wrapped);
  }

  @Override
  public String asTruncatedString(final int length) {
    return this.deepConverted().asTruncatedString(length);
  }

  @Override
  public String asString() {
    if(this.wrapped instanceof TextComponent) {
      return ((TextComponent) this.wrapped).content();
    } else {
      return this.deepConverted().asString();
    }
  }

  @Override
  public List<Text> getSiblings() {
    return this.deepConverted().getSiblings();
  }

  @Override
  public MutableText copy() {
    return this.deepConverted().copy();
  }

  @Override
  public MutableText shallowCopy() {
    return this.deepConverted().shallowCopy();
  }

  @Override
  public OrderedText asOrderedText() {
    return this.deepConverted().asOrderedText();
  }

  @Override
  public <T> Optional<T> visit(final StyledVisitor<T> visitor, final Style style) {
    return this.deepConverted().visit(visitor, style);
  }

  @Override
  public <T> Optional<T> visit(final Visitor<T> visitor) {
    return this.deepConverted().visit(visitor);
  }
}
