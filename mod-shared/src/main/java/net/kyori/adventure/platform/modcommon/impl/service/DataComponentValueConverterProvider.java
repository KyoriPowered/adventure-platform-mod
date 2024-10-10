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
package net.kyori.adventure.platform.modcommon.impl.service;

import com.google.auto.service.AutoService;
import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.nbt.ModDataComponentValue;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.DataComponentValueConverterRegistry;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(DataComponentValueConverterRegistry.Provider.class)
public final class DataComponentValueConverterProvider implements DataComponentValueConverterRegistry.Provider {
  private static final Key ID = (Key) (Object) AdventureCommon.res("platform/mod");

  @Override
  public @NotNull Key id() {
    return ID;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public @NotNull Iterable<DataComponentValueConverterRegistry.Conversion<?, ?>> conversions() {
    return List.of(
      DataComponentValueConverterRegistry.Conversion.convert(
        ModDataComponentValue.Present.class,
        GsonDataComponentValue.class,
        (k, codec) -> GsonDataComponentValue.gsonDataComponentValue((JsonElement) codec.codec().encodeStart(JsonOps.INSTANCE, codec.value()).getOrThrow())
      ),
      DataComponentValueConverterRegistry.Conversion.convert(
        GsonDataComponentValue.class,
        ModDataComponentValue.class,
        (k, gson) -> {
          final DataComponentType<?> type = resolveComponentType(k);
          return new ModDataComponentValue.Present(type.codecOrThrow().parse(JsonOps.INSTANCE, gson.element()).getOrThrow(RuntimeException::new), type.codecOrThrow());
        }
      ),
      DataComponentValueConverterRegistry.Conversion.convert(
        DataComponentValue.TagSerializable.class,
        ModDataComponentValue.class,
        (k, tagSerializable) -> {
          final DataComponentType<?> type = resolveComponentType(k);
          final Tag decodedSnbt;
          try {
            decodedSnbt = tagSerializable.asBinaryTag().get(ModDataComponentValue.SNBT_CODEC);
          } catch (final CommandSyntaxException ex) {
            throw new IllegalArgumentException("Unable to parse SNBT value", ex);
          }

          return new ModDataComponentValue.Present(type.codecOrThrow().parse(NbtOps.INSTANCE, decodedSnbt).getOrThrow(RuntimeException::new), type.codecOrThrow());
        }
      ),
      DataComponentValueConverterRegistry.Conversion.convert(
        DataComponentValue.Removed.class,
        ModDataComponentValue.class,
        (k, $) -> ModDataComponentValue.Removed.INSTANCE
      )
    );
  }

  private static DataComponentType<?> resolveComponentType(final Key key) {
    final DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(MinecraftAudiences.asNative(key));
    if (type == null) {
      throw new IllegalArgumentException("Unknown data component type " + key.asString());
    }

    return type;
  }
}
