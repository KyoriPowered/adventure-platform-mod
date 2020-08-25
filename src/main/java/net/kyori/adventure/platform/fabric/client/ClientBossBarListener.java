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

package net.kyori.adventure.platform.fabric.client;

import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.fabric.AbstractBossBarListener;
import net.kyori.adventure.platform.fabric.mixin.ClientboundBossEventPacketAccess;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ClientBossBarListener extends AbstractBossBarListener<LerpingBossEvent> {
  private final Map<UUID, LerpingBossEvent> hudBars;

  public ClientBossBarListener(final Map<UUID, LerpingBossEvent> hudBars) {
    this.hudBars = hudBars;
  }

  @Override
  protected LerpingBossEvent newBar(final @NonNull Component title, final BossEvent.@NonNull BossBarColor color, final BossEvent.@NonNull BossBarOverlay style) {
    final ClientboundBossEventPacket pkt = new ClientboundBossEventPacket();
    final ClientboundBossEventPacketAccess access = (ClientboundBossEventPacketAccess) pkt;
    access.setId(UUID.randomUUID());
    access.setName(title);
    access.setColor(color);
    access.setOverlay(style);
    return new LerpingBossEvent(pkt); // the only constructor uses a packet, so we use that
  }

  public void add(final BossBar bar) {
    final LerpingBossEvent mc = this.minecraftCreating(bar);
    this.hudBars.put(mc.getId(), mc);
  }

  public void remove(final BossBar bar) {
    final LerpingBossEvent mc = this.bars.remove(bar);
    if(mc != null) {
      bar.removeListener(this);
      this.hudBars.remove(mc.getId());
    }
  }

  public void clear() {
    for(final BossBar bar : this.bars.keySet()) {
      bar.removeListener(this);
    }
    this.bars.clear();
  }
}
