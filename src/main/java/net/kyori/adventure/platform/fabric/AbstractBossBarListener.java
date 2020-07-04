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

package net.kyori.adventure.platform.fabric;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractBossBarListener<T extends net.minecraft.entity.boss.BossBar> implements BossBar.Listener {
  protected final Map<BossBar, T> bars = new IdentityHashMap<>();

  @Override
  public void bossBarNameChanged(@NonNull final BossBar bar, @NonNull final Component oldName, @NonNull final Component newName) {
    if(!oldName.equals(newName)) {
      this.minecraft(bar).setName(FabricPlatform.adapt(newName));
    }
  }

  @Override
  public void bossBarPercentChanged(@NonNull final BossBar bar, final float oldPercent, final float newPercent) {
    if(oldPercent != newPercent) {
      this.minecraft(bar).setPercent(newPercent);
    }
  }

  @Override
  public void bossBarColorChanged(@NonNull final BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
    if(oldColor != newColor) {
      this.minecraft(bar).setColor(GameEnums.BOSS_BAR_COLOR.toMinecraft(newColor));
    }
  }

  @Override
  public void bossBarOverlayChanged(@NonNull final BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
    if(oldOverlay != newOverlay) {
      this.minecraft(bar).setOverlay(GameEnums.BOSS_BAR_OVERLAY.toMinecraft(newOverlay));
    }
  }

  @Override
  public void bossBarFlagsChanged(@NonNull final BossBar bar, @NonNull final Set<BossBar.Flag> oldFlags, @NonNull final Set<BossBar.Flag> newFlags) {
    if(!oldFlags.equals(newFlags)) {
      updateFlags(this.minecraft(bar), newFlags);
    }
  }

  private static void updateFlags(final net.minecraft.entity.boss.@NonNull BossBar bar, final @NonNull Set<BossBar.Flag> flags) {
    bar.setThickenFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
    bar.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
    bar.setDragonMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
  }

  private T minecraft(final @NonNull BossBar bar) {
    final /* @Nullable */ T mc = this.bars.get(bar);
    if(mc == null) {
      throw new IllegalArgumentException("Unknown boss bar instance " + bar);
    }
    return mc;
  }

  protected abstract T newBar(final @NonNull Text title, final net.minecraft.entity.boss.BossBar.@NonNull Color color, final net.minecraft.entity.boss.BossBar.@NonNull Style style);

  protected T minecraftCreating(final @NonNull BossBar bar) {
    return this.bars.computeIfAbsent(bar, key -> {
      final T ret = this.newBar(FabricPlatform.adapt(key.name()),
        GameEnums.BOSS_BAR_COLOR.toMinecraft(key.color()),
        GameEnums.BOSS_BAR_OVERLAY.toMinecraft(key.overlay()));

      ret.setPercent(key.percent());
      updateFlags(ret, key.flags());
      key.addListener(this);
      return ret;
    });
  }

}
