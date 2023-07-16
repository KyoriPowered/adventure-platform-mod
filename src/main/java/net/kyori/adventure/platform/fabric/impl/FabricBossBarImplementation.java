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
package net.kyori.adventure.platform.fabric.impl;

import java.util.List;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarImplementation;
import net.kyori.adventure.bossbar.BossBarViewer;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FabricBossBarImplementation implements BossBarImplementation {
  private @Nullable BossEvent nativeBar;
  private @Nullable Function<BossEvent, ? extends Iterable<? extends BossBarViewer>> viewerProvider;

  public static @NotNull FabricBossBarImplementation fabricImplementation(final @NotNull BossBar bar) {
    return BossBarImplementation.get(bar, FabricBossBarImplementation.class);
  }

  public @Nullable BossEvent nativeBar() {
    return this.nativeBar;
  }

  @SuppressWarnings("unchecked")
  public <T extends BossEvent> @Nullable T nativeBar(final @NotNull Class<T> expectedType) {
    final @Nullable BossEvent nativeBar = this.nativeBar;
    if (nativeBar != null && !expectedType.isInstance(nativeBar)) {
      throw new IllegalStateException("Expected backing native boss event to be a " + expectedType + ", but instead it was a " + nativeBar.getClass());
    }
    return (T) nativeBar;
  }

  @SuppressWarnings("unchecked")
  public <T extends BossEvent> @Nullable BossEvent nativeBar(final @Nullable T nativeBar, final Function<T, ? extends Iterable<? extends BossBarViewer>> viewerProvider) {
    final @Nullable BossEvent oldNative = this.nativeBar;
    this.nativeBar = nativeBar;
    this.viewerProvider = (Function<BossEvent, ? extends Iterable<? extends BossBarViewer>>) viewerProvider;
    return oldNative;
  }

  public void clearNativeBar() {
    this.nativeBar = null;
    this.viewerProvider = null;
  }


  @Override
  public @NotNull Iterable<? extends BossBarViewer> viewers() {
    final var nativeBar = this.nativeBar;
    final var viewerProvider = this.viewerProvider;
    return nativeBar == null || viewerProvider == null ? List.of() : viewerProvider.apply(this.nativeBar);
  }
}
