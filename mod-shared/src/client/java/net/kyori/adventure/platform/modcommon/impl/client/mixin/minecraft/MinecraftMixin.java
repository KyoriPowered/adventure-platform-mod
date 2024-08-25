/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2023-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl.client.mixin.minecraft;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.LocaleHolderBridge;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin implements Pointered {
  @SuppressWarnings("MethodName")
  @Shadow
  public abstract GameProfile getGameProfile();

  @Shadow @Final public Options options;

  @Unique
  private final Supplier<Pointers> pointers = Suppliers.memoize(this::makePointers);

  @Unique
  private Pointers makePointers() {
    final Pointers.Builder builder = Pointers.builder()
      .withDynamic(Identity.LOCALE, () -> ((LocaleHolderBridge) this.options).adventure$locale())
      .withDynamic(Identity.NAME, () -> this.getGameProfile().getName())
      .withDynamic(Identity.UUID, () -> this.getGameProfile().getId());

    AdventureCommon.HOOKS.collectPointers(this, builder);

    return builder.build();
  }

  @Override
  public @NotNull Pointers pointers() {
    return this.pointers.get();
  }
}
