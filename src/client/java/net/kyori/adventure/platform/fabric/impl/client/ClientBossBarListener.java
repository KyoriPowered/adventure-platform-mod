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
package net.kyori.adventure.platform.fabric.impl.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarViewer;
import net.kyori.adventure.platform.fabric.impl.AbstractBossBarListener;
import net.kyori.adventure.platform.fabric.impl.BossEventBridge;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.NotNull;

public class ClientBossBarListener extends AbstractBossBarListener<LerpingBossEvent, FabricClientAudiencesImpl> {
  private final Map<UUID, LerpingBossEvent> hudBars; // a live map of boss events displayed in the game gui

  public ClientBossBarListener(final FabricClientAudiencesImpl controller, final Map<UUID, LerpingBossEvent> hudBars) {
    super(controller, LerpingBossEvent.class);
    this.hudBars = hudBars;
  }

  @Override
  protected Iterable<? extends BossBarViewer> viewers(LerpingBossEvent event) {
    return ((BossEventBridge) event).adventure$bridge$controller() == this ? List.of((BossBarViewer) this.controller.audience()) : List.of(); // ClientAudience is a BossBarViewer
  }

  @Override
  protected LerpingBossEvent newBar(final @NotNull Component title,
                                    final BossEvent.@NotNull BossBarColor color,
                                    final BossEvent.@NotNull BossBarOverlay style,
                                    final float progress) {
    return new LerpingBossEvent(UUID.randomUUID(), title, progress, color, style, false, false, false);
  }

  public void add(final BossBar bar) {
    final LerpingBossEvent mc = this.minecraftCreating(bar);
    this.hudBars.put(mc.getId(), mc);
  }

  public void remove(final BossBar bar) {
    if (this.bars.remove(bar)) {
      bar.removeListener(this);
      this.hudBars.remove(this.minecraft(bar).getId());
    }
  }

  public void clear() {
    for (final BossBar entry : this.bars) {
      entry.removeListener(this);
      this.hudBars.remove(this.minecraft(entry).getId());
    }
    this.bars.clear();
  }
}
