/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2020 KyoriPowered
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

package net.kyori.adventure.platform.fabric.impl;

import net.kyori.adventure.platform.fabric.impl.accessor.ComponentSerializerAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.minecraft.network.chat.MutableComponent;

class NonWrappingComponentSerializer implements ComponentSerializer<Component, Component, net.minecraft.network.chat.Component> {

  NonWrappingComponentSerializer() {
  }

  @Override
  public Component deserialize(final net.minecraft.network.chat.Component input) {
    if(input instanceof WrappedComponent) {
      return ((WrappedComponent) input).wrapped();
    }

    return ComponentSerializerAccess.getGSON().fromJson(net.minecraft.network.chat.Component.Serializer.toJsonTree(input), Component.class);
  }

  @Override
  public MutableComponent serialize(final Component component) {
    return net.minecraft.network.chat.Component.Serializer.fromJson(ComponentSerializerAccess.getGSON().toJsonTree(component));
  }
}
