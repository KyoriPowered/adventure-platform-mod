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
import net.kyori.adventure.platform.fabric.mixin.AccessorBossBarS2CPacket;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ClientBossBarListener extends AbstractBossBarListener<ClientBossBar> {
  private final Map<UUID, ClientBossBar> hudBars;

  public ClientBossBarListener(final Map<UUID, ClientBossBar> hudBars) {
    this.hudBars = hudBars;
  }

  @Override
  protected ClientBossBar newBar(final @NonNull Text title, final net.minecraft.entity.boss.BossBar.@NonNull Color color, final net.minecraft.entity.boss.BossBar.@NonNull Style style) {
    final BossBarS2CPacket pkt = new BossBarS2CPacket();
    final AccessorBossBarS2CPacket access = (AccessorBossBarS2CPacket) pkt;
    access.setUuid(UUID.randomUUID());
    access.setName(title);
    access.setColor(color);
    access.setOverlay(style);
    return new ClientBossBar(pkt); // the only constructor uses a packet, so we use that
  }

  public void add(final BossBar bar) {
    final ClientBossBar mc = this.minecraftCreating(bar);
    this.hudBars.put(mc.getUuid(), mc);
  }

  public void remove(final BossBar bar) {
    final ClientBossBar mc = this.bars.remove(bar);
    if(mc != null) {
      bar.removeListener(this);
      this.hudBars.remove(mc.getUuid());
    }
  }

  public void clear() {
    for(BossBar bar : this.bars.keySet()) {
      bar.removeListener(this);
    }
    this.bars.clear();
  }
}
