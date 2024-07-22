/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
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
package net.kyori.adventure.platform.fabric;

import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * An interface applied to {@link net.minecraft.commands.CommandSourceStack} to allow sending {@link Component Components}.
 *
 * @since 4.0.0
 */
public interface AdventureCommandSourceStack extends ForwardingAudience.Single, Identified {
  @Override
  default @NotNull Identity identity() {
    throw new UnsupportedOperationException("Must be overridden");
  }

  @Override
  default @NotNull Audience audience() {
    throw new UnsupportedOperationException("Must be overridden");
  }

  /**
   * Send a result message to the command source.
   *
   * @param text The text to send
   * @param sendToOps If this message should be sent to all ops listening
   * @since 4.0.0
   */
  default void sendSuccess(final @NotNull Component text, final boolean sendToOps) {
    // Implemented by Mixin
  }

  /**
   * Send a result message to the command source.
   *
   * @param text A supplier providing the message to send
   * @param sendToOps If this message should be sent to all ops listening
   * @since 5.10.0
   */
  default void sendLazySuccess(final @NotNull Supplier<Component> text, final boolean sendToOps) {
    // Implemented by Mixin
  }

  /**
   * Send an error message to the command source.
   *
   * @param text The error
   * @since 4.0.0
   */
  default void sendFailure(final @NotNull Component text) {
    // Implemented by Mixin
  }
}
