/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
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
package net.kyori.adventure.platform.fabric.impl.service;

import com.google.auto.service.AutoService;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.platform.fabric.impl.nbt.CodecableDataComponentValue;
import net.kyori.adventure.text.event.DataComponentValueConverterRegistry;
import net.kyori.adventure.text.serializer.gson.GsonDataComponentValue;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

@AutoService(DataComponentValueConverterRegistry.Provider.class)
public class DataComponentValueConverterProvider implements DataComponentValueConverterRegistry.Provider {
  private static final Key ID = Key.key("adventure", "platform/fabric");

  @Override
  public @NotNull Key id() {
    return ID;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public @NotNull Iterable<DataComponentValueConverterRegistry.Conversion<?, ?>> conversions() {
    return List.of(
      DataComponentValueConverterRegistry.Conversion.convert(
        CodecableDataComponentValue.class,
        GsonDataComponentValue.class,
        (k, codec) -> GsonDataComponentValue.gsonDatacomponentValue((JsonElement) codec.codec().encodeStart(JsonOps.INSTANCE, codec.value()).getOrThrow())
      ),
      DataComponentValueConverterRegistry.Conversion.convert(
        GsonDataComponentValue.class,
        CodecableDataComponentValue.class,
        (k, gson) -> {
          final DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(FabricAudiences.toNative(k));
          if (type == null) {
            throw new IllegalArgumentException("Unknown data component type " + k);
          }
          return new CodecableDataComponentValue(type.codecOrThrow().parse(JsonOps.INSTANCE, gson.element()).getOrThrow(RuntimeException::new), type.codecOrThrow());
        }
      )
    );
  }
}
