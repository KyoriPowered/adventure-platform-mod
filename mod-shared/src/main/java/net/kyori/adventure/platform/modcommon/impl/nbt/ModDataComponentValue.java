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
package net.kyori.adventure.platform.modcommon.impl.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.DataComponentValue;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.jetbrains.annotations.NotNull;

public sealed interface ModDataComponentValue extends DataComponentValue {
  net.kyori.adventure.util.Codec<Tag, String, CommandSyntaxException, RuntimeException> SNBT_CODEC = net.kyori.adventure.util.Codec.codec(
    s -> new TagParser(new StringReader(s)).readValue(),
    Tag::toString
  );

  record Present<T>(@NotNull T value, @NotNull Codec<T> codec) implements ModDataComponentValue, DataComponentValue.TagSerializable {
    @Override
    public @NotNull BinaryTagHolder asBinaryTag() {
      return BinaryTagHolder.encode(
        this.codec.encodeStart(NbtOps.INSTANCE, this.value).getOrThrow(IllegalArgumentException::new),
        SNBT_CODEC
      );
    }
  }

  enum Removed implements ModDataComponentValue, DataComponentValue.Removed {
    INSTANCE;
  }
}
