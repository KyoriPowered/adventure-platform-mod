/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2023-2024 KyoriPowered
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

import java.util.Objects;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * A requirement for commands that prevents them from being synced to the client.
 *
 * <p>Not yet implemented for client commands.</p>
 *
 * @param base the base predicate
 * @param <V> value type
 */
public record HiddenRequirement<V>(Predicate<V> base) implements Predicate<V> {
  public static <T> HiddenRequirement<T> alwaysAllowed() {
    return new HiddenRequirement<>(t -> true);
  }

  @Override
  public boolean test(final V v) {
    return this.base.test(v);
  }

  @Override
  public @NotNull Predicate<V> and(final @NotNull Predicate<? super V> other) {
    return new HiddenRequirement<>(this.base.and(unwrap(Objects.requireNonNull(other, "other"))));
  }

  @Override
  public @NotNull Predicate<V> negate() {
    return new HiddenRequirement<>(this.base.negate());
  }

  @Override
  public @NotNull Predicate<V> or(final @NotNull Predicate<? super V> other) {
    return new HiddenRequirement<>(this.base.or(unwrap(Objects.requireNonNull(other, "other"))));
  }

  private static <T> @NotNull Predicate<T> unwrap(final @NotNull Predicate<T> pred) {
    return pred instanceof HiddenRequirement<T>(Predicate<T> base) ? base : pred;
  }
}
