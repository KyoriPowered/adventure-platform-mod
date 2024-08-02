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

import net.kyori.adventure.platform.neoforge.impl.AdventureNeoforgeCommon;

/**
 * Helper for registering {@link net.kyori.adventure.platform.modcommon.ComponentArgumentType} and
 * {@link net.kyori.adventure.platform.modcommon.KeyArgumentType}.
 *
 * @since 6.0.0
 */
public final class AdventureArgumentTypes {
  private AdventureArgumentTypes() {
  }

  private static boolean registered = false;

  /**
   * Registers the Adventure {@link com.mojang.brigadier.arguments.ArgumentType}s.
   *
   * <p>This is not done by default as it requires the client to also register the types.</p>
   *
   * @since 6.0.0
   */
  public static synchronized void register() {
    if (registered) {
      return;
    }

    registered = true;

    AdventureNeoforgeCommon.registerArgumentTypes();
  }
}
