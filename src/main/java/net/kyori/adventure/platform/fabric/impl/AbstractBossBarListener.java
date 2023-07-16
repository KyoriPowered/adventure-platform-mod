/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2020-2023 KyoriPowered
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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarViewer;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.platform.fabric.impl.FabricBossBarImplementation.fabricImplementation;

/**
 * Boss bar tracking for syncing between Adventure and Vanilla boss bars.
 *
 * @param <T> the type of game boss bar we handle
 * @param <A> the sided audience provider
 */
public abstract class AbstractBossBarListener<T extends BossEvent, A extends FabricAudiences> implements BossBar.Listener {
  protected final A controller;
  protected final Set<BossBar> bars = ConcurrentHashMap.newKeySet();
  protected final Class<T> barType;

  protected AbstractBossBarListener(final A controller, final Class<T> barType) {
    this.controller = controller;
    this.barType = barType;
  }

  @Override
  public void bossBarNameChanged(final @NotNull BossBar bar, final @NotNull Component oldName, final @NotNull Component newName) {
    if (!oldName.equals(newName)) {
      this.minecraft(bar).setName(this.controller.toNative(newName));
    }
  }

  @Override
  public void bossBarProgressChanged(final @NotNull BossBar bar, final float oldPercent, final float newPercent) {
    if (oldPercent != newPercent) {
      this.minecraft(bar).setProgress(newPercent);
    }
  }

  @Override
  public void bossBarColorChanged(final @NotNull BossBar bar, final BossBar.@NotNull Color oldColor, final BossBar.@NotNull Color newColor) {
    if (oldColor != newColor) {
      this.minecraft(bar).setColor(GameEnums.BOSS_BAR_COLOR.toMinecraft(newColor));
    }
  }

  @Override
  public void bossBarOverlayChanged(final @NotNull BossBar bar, final BossBar.@NotNull Overlay oldOverlay, final BossBar.@NotNull Overlay newOverlay) {
    if (oldOverlay != newOverlay) {
      this.minecraft(bar).setOverlay(GameEnums.BOSS_BAR_OVERLAY.toMinecraft(newOverlay));
    }
  }

  @Override
  public void bossBarFlagsChanged(final @NotNull BossBar bar, final @NotNull Set<BossBar.Flag> flagsRemoved, final @NotNull Set<BossBar.Flag> flagsAdded) {
    updateFlags(this.minecraft(bar), bar.flags());
  }

  private static void updateFlags(final @NotNull BossEvent bar, final @NotNull Set<BossBar.Flag> flags) {
    bar.setCreateWorldFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
    bar.setDarkenScreen(flags.contains(BossBar.Flag.DARKEN_SCREEN));
    bar.setPlayBossMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
  }

  protected T minecraft(final @NotNull BossBar bar) {
    final @Nullable T mc = fabricImplementation(bar).nativeBar(this.barType);
    if (mc == null || ((BossEventBridge) mc).adventure$bridge$controller() != this) {
      throw new IllegalArgumentException("Unknown boss bar instance " + bar);
    }
    return mc;
  }

  protected abstract Iterable<? extends BossBarViewer> viewers(final T event);

  protected abstract T newBar(final net.minecraft.network.chat.@NotNull Component title,
                              final BossEvent.@NotNull BossBarColor color,
                              final BossEvent.@NotNull BossBarOverlay style,
                              final float progress);

  public Iterable<? extends BossBar> activeBars() {
    return this.bars;
  }

  protected T minecraftCreating(final @NotNull BossBar bar) {
    final var fabricImpl = fabricImplementation(bar);

    if (fabricImpl.nativeBar() == null) {
      final T ret = this.newBar(this.controller.toNative(bar.name()),
        GameEnums.BOSS_BAR_COLOR.toMinecraft(bar.color()),
        GameEnums.BOSS_BAR_OVERLAY.toMinecraft(bar.overlay()),
        bar.progress());
      ((BossEventBridge) ret).adventure$bridge$controller(this);

      updateFlags(ret, bar.flags());
      final BossEvent oldBar = fabricImpl.nativeBar(ret, this::viewers);
      bar.addListener(this);

      if (oldBar != null) {
        final AbstractBossBarListener<?, ?> oldController = ((BossEventBridge) oldBar).adventure$bridge$controller(null);
        if (oldController != null) {
          bar.removeListener(oldController);
        }
      }
      return ret;
    } else {
      return fabricImpl.nativeBar(this.barType);
    }
  }

  @SuppressWarnings("unchecked")
  protected void maybeRemoveMinecraft(final @NotNull BossBar bar, final @NotNull BiPredicate<BossBar, T> shouldRemove) {
    final var impl = fabricImplementation(bar);
    final BossEvent nativeBar = impl.nativeBar();
    if (this.barType.isInstance(nativeBar)) {
      if (shouldRemove.test(bar, (T) nativeBar)) {
        impl.clearNativeBar();
        ((BossEventBridge) nativeBar).adventure$bridge$controller(null);
      }
    }
  }
}
