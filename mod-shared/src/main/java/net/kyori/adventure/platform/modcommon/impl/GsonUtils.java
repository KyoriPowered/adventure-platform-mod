/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2025 KyoriPowered
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

import com.google.gson.stream.JsonReader;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;

public final class GsonUtils {
  private static final @NotNull Field JSON_READER_POS = requireField(JsonReader.class, "pos");
  private static final @NotNull Field JSON_READER_LINESTART = requireField(JsonReader.class, "lineStart");

  private GsonUtils() {
  }

  private static @NotNull Field requireField(final @NotNull Class<?> clazz, final @NotNull String name) {
    try {
      final Field declaredField = clazz.getDeclaredField(name);
      declaredField.setAccessible(true);
      return declaredField;
    } catch (final NoSuchFieldException ex) {
      throw new IllegalStateException("Couldn't get field '" + name + "' for " + clazz.getName(), ex);
    }
  }

  public static int posInLine(final @NotNull JsonReader reader) {
    try {
      return JSON_READER_POS.getInt(reader) - JSON_READER_LINESTART.getInt(reader);
    } catch (final IllegalAccessException ex) {
      throw new IllegalStateException("Couldn't read position of JsonReader", ex);
    }
  }
}
