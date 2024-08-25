/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
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
package net.kyori.adventure.platform.modcommon.impl.service;

import com.google.auto.service.AutoService;
import java.util.function.Consumer;
import net.kyori.adventure.platform.modcommon.impl.nbt.NBTLegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.minecraft.SharedConstants;
import org.jetbrains.annotations.NotNull;

@AutoService(GsonComponentSerializer.Provider.class)
public final class GsonComponentSerializerProviderImpl implements GsonComponentSerializer.Provider {
  @Override
  public @NotNull GsonComponentSerializer gson() {
    return GsonComponentSerializer.builder()
      .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE)
      .build();
  }

  @Override
  public @NotNull GsonComponentSerializer gsonLegacy() {
    return GsonComponentSerializer.builder()
      .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE)
      .editOptions(b -> b
        .value(JSONOptions.EMIT_RGB, false)
        .value(JSONOptions.EMIT_HOVER_EVENT_TYPE, JSONOptions.HoverEventValueMode.BOTH)
      )
      .build();
  }

  @Override
  public @NotNull Consumer<GsonComponentSerializer.Builder> builder() {
    return builder -> builder
      .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE)
      .options(JSONOptions.byDataVersion().at(SharedConstants.getCurrentVersion().getDataVersion().getVersion()));
  }
}
