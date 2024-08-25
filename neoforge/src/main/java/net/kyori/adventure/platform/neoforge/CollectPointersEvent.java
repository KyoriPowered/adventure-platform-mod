/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2024 KyoriPowered
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
package net.kyori.adventure.platform.neoforge;

import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * An event called when pointers for some {@link net.kyori.adventure.pointer.Pointered} implementation are required.
 *
 * @since 6.0.0
 */
public final class CollectPointersEvent extends Event {
  private final Pointered pointered;
  private final Pointers.Builder builder;

  /**
   * Create a new {@link CollectPointersEvent}.
   *
   * @param pointered pointered instance
   * @param builder pointers builder
   * @since 6.0.0
   */
  @ApiStatus.Internal
  public CollectPointersEvent(final @NotNull Pointered pointered, final Pointers.@NotNull Builder builder) {
    this.pointered = pointered;
    this.builder = builder;
  }

  /**
   * Returns the {@link Pointered} object we are collecting pointers for.
   *
   * @return the pointered
   * @since 6.0.0
   */
  public Pointered pointered() {
    return this.pointered;
  }

  /**
   * Returns the builder for contributing pointers to {@link #pointered()}.
   *
   * @return the builder
   * @since 6.0.0
   */
  public Pointers.Builder pointers() {
    return this.builder;
  }
}
