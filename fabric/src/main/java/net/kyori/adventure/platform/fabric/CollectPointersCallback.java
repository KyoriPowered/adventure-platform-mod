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
package net.kyori.adventure.platform.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import org.jetbrains.annotations.NotNull;

/**
 * An event called when pointers for some {@link net.kyori.adventure.pointer.Pointered} implementation are required.
 *
 * @since 5.9.0
 */
public interface CollectPointersCallback {
  /**
   * The event holder for this callback.
   *
   * @since 5.9.0
   */
  Event<CollectPointersCallback> EVENT = EventFactory.createArrayBacked(
    CollectPointersCallback.class,
    (pointered, consumer) -> {},
    listeners -> (pointered, consumer) -> {
      for (final var listener : listeners) {
        listener.registerPointers(pointered, consumer);
      }
    }
  );

  /**
   * Register pointers for a specific pointered object.
   *
   * <p>This is currently called for every new player object instance,
   * which may be server or client sided. This does mean that callbacks will be called again after player respawn.</p>
   *
   * @param pointered the pointered object
   * @param consumer a pointers builder to register the pointers in
   * @since 5.9.0
   */
  void registerPointers(final @NotNull Pointered pointered, final Pointers.@NotNull Builder consumer);
}
