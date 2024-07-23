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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.world.item;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.impl.nbt.FabricDataComponentValue;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements HoverEventSource<HoverEvent.ShowItem> {
  // @formatter:off
  @Shadow public abstract int shadow$getCount();
  @Shadow public abstract Item shadow$getItem();
  @Shadow public abstract DataComponentPatch shadow$getComponentsPatch();
  // @formatter:on

  @Override
  public @NotNull HoverEvent<HoverEvent.ShowItem> asHoverEvent(final @NotNull UnaryOperator<HoverEvent.ShowItem> op) {
    final Key itemType = (Key) (Object) BuiltInRegistries.ITEM.getKey(this.shadow$getItem());
    final Map<Key, DataComponentValue> components;
    final DataComponentPatch patch = this.shadow$getComponentsPatch();
    if (patch.isEmpty()) {
      components = Collections.emptyMap();
    } else {
      components = new HashMap<>();
      for (final Map.Entry<DataComponentType<?>, Optional<?>> entry : patch.entrySet()) {
        if (entry.getKey().isTransient()) continue;

        final Key componentKey = (Key) (Object) BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
        if (entry.getValue().isEmpty()) {
          components.put(componentKey, FabricDataComponentValue.Removed.INSTANCE);
        } else {
          @SuppressWarnings({"rawtypes", "unchecked"}) final FabricDataComponentValue.Present<?> holder = new FabricDataComponentValue.Present(entry.getValue().orElse(null), entry.getKey().codecOrThrow());
          components.put(componentKey, holder);
        }
      }
    }

    final HoverEvent.ShowItem item = HoverEvent.ShowItem.showItem(itemType, this.shadow$getCount(), components);
    return HoverEvent.showItem(op.apply(item));
  }

}
