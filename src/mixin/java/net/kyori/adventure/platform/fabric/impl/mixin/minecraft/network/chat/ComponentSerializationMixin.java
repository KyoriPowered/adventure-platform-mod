/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2023 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.platform.fabric.impl.WrappedComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComponentSerialization.class)
public class ComponentSerializationMixin {
  // Inject our component logic into the codec
  // This is pretty inefficient for non-JsonOps codecs, but it is a minimal change which allows us to continue functioning.
  @Inject(method = "createCodec", at = @At("RETURN"), cancellable = true)
  private static void adventure$wrapCodec(final Codec<Component> recursive, final CallbackInfoReturnable<Codec<Component>> cir) {
    final Codec<Component> original = cir.getReturnValue();
    final Codec<Component> wrappingCodec = new Codec<>() {
      @Override
      @SuppressWarnings("unchecked")
      public <T> DataResult<T> encode(final Component input, final DynamicOps<T> ops, final T prefix) {
        if (input instanceof WrappedComponent w) {
          if (w.deepConvertedIfPresent() != null) { // already converted
            return original.encode(w.deepConvertedIfPresent(), ops, prefix);
          }

          final JsonElement json = GsonComponentSerializer.gson().serializeToTree(w.wrapped());
          if (ops instanceof JsonOps) {
            return DataResult.success((T) json);
          } else {
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
          }
        }

        return original.encode(input, ops, prefix);
      }

      @Override
      public <T> DataResult<Pair<Component, T>> decode(final DynamicOps<T> ops, final T input) {
        return original.decode(ops, input);
      }
    };

    cir.setReturnValue(wrappingCodec);
  }
}
