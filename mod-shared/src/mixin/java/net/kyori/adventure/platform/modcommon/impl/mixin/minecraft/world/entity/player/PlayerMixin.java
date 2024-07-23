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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.world.entity.player;

import com.mojang.authlib.GameProfile;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.IdentifiedAtRuntime;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.PointerProviderBridge;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.ComponentLike;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IdentifiedAtRuntime, PointerProviderBridge {
  // @formatter:off
  @Shadow @Final private GameProfile gameProfile;

  @Shadow public abstract GameProfile shadow$getGameProfile();
  // @formatter:on

  private Pointers adventure$pointers;

  protected PlayerMixin(final EntityType<? extends LivingEntity> entityType, final Level level) {
    super(entityType, level);
  }

  @Override
  public @NotNull Identity identity() {
    return (Identity) this.gameProfile;
  }

  @Override
  public @NotNull Pointers adventure$pointers() {
    Pointers pointers = this.adventure$pointers;
    if (pointers == null) {
      synchronized (this) {
        if (this.adventure$pointers != null) {
          return this.adventure$pointers;
        }

        final Pointers.Builder builder = Pointers.builder()
          .withDynamic(Identity.NAME, () -> this.shadow$getGameProfile().getName())
          .withDynamic(Identity.UUID, this::getUUID)
          .withDynamic(Identity.DISPLAY_NAME, () -> ((ComponentLike) this.getDisplayName()).asComponent());

        // add any extra data
        if (this instanceof Pointered p) {
          AdventureCommon.HOOKS.collectPointers(p, builder);
        }

        this.adventure$pointers = pointers = builder.build();
      }
    }

    return pointers;
  }
}
