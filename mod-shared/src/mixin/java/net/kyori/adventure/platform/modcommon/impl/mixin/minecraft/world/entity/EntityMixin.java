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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.world.entity;

import java.util.function.UnaryOperator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.EntityHoverEventSource;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin implements Sound.Emitter, EntityHoverEventSource, Nameable, EntityAccess {
  // @formatter:off
  @Shadow private Level level;

  @Shadow public abstract EntityType<?> shadow$getType();
  // @formatter:on

  @Override
  public @NotNull HoverEvent<ShowEntity> asHoverEvent(final @NotNull UnaryOperator<ShowEntity> op) {
    final Key entityType = (Key) (Object) this.level.registryAccess()
      .lookupOrThrow(Registries.ENTITY_TYPE)
      .getKey(this.shadow$getType());

    final ShowEntity data = HoverEvent.ShowEntity.showEntity(
      entityType,
      this.getUUID(),
      new NonWrappingComponentSerializer(this.level::registryAccess).deserialize(this.getName())
    );
    return HoverEvent.showEntity(op.apply(data));
  }
}
