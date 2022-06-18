/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2021 KyoriPowered
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.sound.Sound;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.Nullable;

public final class GameEnums {
  public static final MappedRegistry<BossEvent.BossBarColor, BossBar.Color> BOSS_BAR_COLOR
    = MappedRegistry.named(BossEvent.BossBarColor.class, BossEvent.BossBarColor::byName,
    BossBar.Color.class, BossBar.Color.NAMES);

  public static final MappedRegistry<BossEvent.BossBarOverlay, Overlay> BOSS_BAR_OVERLAY
    = MappedRegistry.named(BossEvent.BossBarOverlay.class, BossEvent.BossBarOverlay::byName,
    Overlay.class, Overlay.NAMES);

  public static final MappedRegistry<SoundSource, Sound.Source> SOUND_SOURCE
    = MappedRegistry.named(SoundSource.class, soundSourceProvider(),
    Sound.Source.class, Sound.Source.NAMES);

  private static Function<String, @Nullable SoundSource> soundSourceProvider() {
    final Map<String, SoundSource> sources = new HashMap<>();
    for (final SoundSource source : SoundSource.values()) {
      sources.put(source.getName(), source);
    }

    return sources::get;
  }

  private GameEnums() {
  }
}
