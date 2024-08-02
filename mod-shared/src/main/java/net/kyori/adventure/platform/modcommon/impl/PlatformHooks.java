/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
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
package net.kyori.adventure.platform.modcommon.impl;

import java.util.Locale;
import java.util.function.Function;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service interface for platform-specific things.
 */
public interface PlatformHooks extends SidedProxy {
  SidedProxy sidedProxy();

  @Override
  default void contributeFlattenerElements(final ComponentFlattener.@NotNull Builder flattenerBuilder) {
    this.sidedProxy().contributeFlattenerElements(flattenerBuilder);
  }

  @Override
  default @NotNull WrappedComponent createWrappedComponent(
    final net.kyori.adventure.text.@NotNull Component wrapped,
    final @Nullable Function<Pointered, ?> partition,
    final @Nullable ComponentRenderer<Pointered> renderer,
    final @Nullable NonWrappingComponentSerializer nonWrappingSerializer
  ) {
    return this.sidedProxy().createWrappedComponent(wrapped, partition, renderer, nonWrappingSerializer);
  }

  void updateTabList(final ServerPlayer player, final @Nullable Component header, final @Nullable Component footer);

  default void collectPointers(Pointered pointered, Pointers.Builder builder) {
    if (pointered instanceof LocaleHolderBridge holder) {
      builder.withDynamic(Identity.LOCALE, holder::adventure$locale);
    }
  }

  default void onLocaleChange(ServerPlayer player, Locale newLocale) {
    ((LocaleHolderBridge) player).adventure$locale(newLocale);
    MinecraftServerAudiencesImpl.forEachInstance(instance -> {
      instance.bossBars().refreshTitles(player);
    });
  }
}
