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
package net.kyori.adventure.platform.fabric.impl.mixin;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.impl.accessor.InvalidKeyExceptionAccess;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Key.class, remap = false)
public interface KeyMixin {
  /**
   * Creates a key.
   *
   * @param namespace the namespace
   * @param value the value
   * @return the key
   * @throws InvalidKeyException if the namespace or value contains an invalid character
   * @reason implemented by mixin to the Vanilla class
   */
  @Overwrite
  @SuppressWarnings("overwrite")
  static @NotNull Key key(final String namespace, final String value) {
    try {
      return new ResourceLocation(namespace, value);
    } catch (final ResourceLocationException ex) {
      throw InvalidKeyExceptionAccess.newInvalidKeyException(namespace, value, ex.getMessage());
    }
  }
}
