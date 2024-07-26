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
package net.kyori.adventure.platform.modcommon.impl;

import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.ComponentArgumentType;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ComponentArgumentTypeSerializer implements ArgumentTypeInfo<ComponentArgumentType, ComponentArgumentTypeSerializer.Template> {

  public static ComponentArgumentTypeSerializer INSTANCE = new ComponentArgumentTypeSerializer();

  private ComponentArgumentTypeSerializer() {
  }

  @Override
  public void serializeToNetwork(final Template type, final FriendlyByteBuf buffer) {
    buffer.writeResourceLocation(MinecraftAudiences.toNative(type.format.id()));
  }

  @Override
  public Template deserializeFromNetwork(final FriendlyByteBuf buffer) {
    final ResourceLocation id = buffer.readResourceLocation();
    final ComponentArgumentType.Format format = ComponentArgumentType.Format.INDEX.value((Key) (Object) id);
    if (format == null) {
      throw new IllegalArgumentException("Unknown Adventure component format: " + id);
    }
    return new Template(format);
  }

  @Override
  public void serializeToJson(final Template type, final JsonObject json) {
    json.addProperty("serializer", type.format.id().asString());
  }

  @Override
  public Template unpack(final ComponentArgumentType argumentType) {
    return new Template(argumentType.format());
  }

  record Template(ComponentArgumentType.Format format) implements ArgumentTypeInfo.Template<ComponentArgumentType> {
    @Override
    public ComponentArgumentType instantiate(final CommandBuildContext commandBuildContext) {
      return ComponentArgumentType.component(this.format);
    }

    @Override
    public ArgumentTypeInfo<ComponentArgumentType, ?> type() {
      return ComponentArgumentTypeSerializer.INSTANCE;
    }
  }
}
