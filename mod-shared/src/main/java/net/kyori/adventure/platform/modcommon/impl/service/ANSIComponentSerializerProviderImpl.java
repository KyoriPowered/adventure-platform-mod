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
package net.kyori.adventure.platform.modcommon.impl.service;

import com.google.auto.service.AutoService;
import java.util.function.Consumer;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import org.jetbrains.annotations.NotNull;

@AutoService(ANSIComponentSerializer.Provider.class)
public final class ANSIComponentSerializerProviderImpl implements ANSIComponentSerializer.Provider {
  @Override
  public @NotNull ANSIComponentSerializer ansi() {
    return ANSIComponentSerializer.builder().flattener(AdventureCommon.FLATTENER).build();
  }

  @Override
  public @NotNull Consumer<ANSIComponentSerializer.Builder> builder() {
    return builder -> builder.flattener(AdventureCommon.FLATTENER);
  }
}
