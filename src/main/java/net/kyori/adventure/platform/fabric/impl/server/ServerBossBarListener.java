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
package net.kyori.adventure.platform.fabric.impl.server;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarViewer;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.impl.AbstractBossBarListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public class ServerBossBarListener extends AbstractBossBarListener<ServerBossEvent, FabricServerAudiences> {
  public ServerBossBarListener(final FabricServerAudiences controller) {
    super(controller, ServerBossEvent.class);
  }

  public void subscribe(final ServerPlayer player, final BossBar bar) {
    this.minecraftCreating(requireNonNull(bar, "bar")).addPlayer(requireNonNull(player, "player"));
  }

  public void unsubscribe(final ServerPlayer player, final BossBar bar) {
    this.maybeRemoveMinecraft(bar, (adv, mc) -> {
      mc.removePlayer(player);
      return mc.getPlayers().isEmpty();
    });
  }

  /**
   * Replace a player entity without sending any packets.
   *
   * <p>This should be triggered when the entity representing a player changes
   * (such as during a respawn)</p>
   *
   * @param old old player
   * @param newPlayer new one
   */
  public void replacePlayer(final ServerPlayer old, final ServerPlayer newPlayer) {
    for (final BossBar bar : this.bars) {
      ((ServerBossEventBridge) this.minecraft(bar)).adventure$replaceSubscriber(old, newPlayer);
    }
  }

  /**
   * Refresh titles when a player's locale has changed.
   *
   * @param player player to refresh titles fro
   */
  public void refreshTitles(final ServerPlayer player) {
    for (final BossBar bar : this.bars) {
      final ServerBossEvent mc = this.minecraft(bar);
      if (mc.getPlayers().contains(player)) {
        player.connection.send(ClientboundBossEventPacket.createUpdateNamePacket(mc));
      }
    }
  }

  /**
   * Remove the player from all associated boss bars.
   *
   * @param player The player to remove
   */
  public void unsubscribeFromAll(final ServerPlayer player) {
    final Set<BossBar> bars = new HashSet<>(this.bars);
    for (final Iterator<BossBar> it = bars.iterator(); it.hasNext();) {
      this.maybeRemoveMinecraft(it.next(), (adv, mc) -> {
        if (mc.getPlayers().contains(player)) {
          mc.removePlayer(player);
          return mc.getPlayers().isEmpty();
        }
        return false;
      });
    }
  }

  @Override
  protected Iterable<? extends BossBarViewer> viewers(ServerBossEvent event) {
    return event.getPlayers().stream()
      .map(p -> (BossBarViewer) this.controller.audience(p))
      .toList();
  }

  @Override
  protected ServerBossEvent newBar(
    final @NotNull Component title,
    final net.minecraft.world.BossEvent.@NotNull BossBarColor color,
    final net.minecraft.world.BossEvent.@NotNull BossBarOverlay style,
    final float progress
  ) {
    final ServerBossEvent event = new ServerBossEvent(title, color, style);
    event.setProgress(progress);
    return event;
  }
}
