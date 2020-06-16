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

package net.kyori.adventure.platform.fabric.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.fabric.AbstractBossBarListener;
import net.kyori.adventure.platform.fabric.BulkServerBossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

public class ServerBossBarListener extends AbstractBossBarListener<ServerBossBar> {
  public static final ServerBossBarListener INSTANCE = new ServerBossBarListener();

  public void subscribe(final ServerPlayerEntity player, final BossBar bar) {
    minecraftCreating(requireNonNull(bar, "bar")).addPlayer(requireNonNull(player, "player"));
  }

  public void subscribeAll(final Collection<ServerPlayerEntity> players, final BossBar bar) {
    ((BulkServerBossBar) minecraftCreating(requireNonNull(bar, "bar"))).addAll(players);
  }

  public void unsubscribe(final ServerPlayerEntity player, final BossBar bar) {
    this.bars.computeIfPresent(bar, (key, old) -> {
      old.removePlayer(player);
      if(old.getPlayers().isEmpty()) {
        key.removeListener(this);
        return null;
      } else {
        return old;
      }
    });
  }

  public void unsubscribeAll(final Collection<ServerPlayerEntity> players, final BossBar bar) {
    this.bars.computeIfPresent(bar, (key, old) -> {
      ((BulkServerBossBar) old).removeAll(players);
      if(old.getPlayers().isEmpty()) {
        key.removeListener(this);
        return null;
      } else {
        return old;
      }
    });
  }

  /**
   * Replace a player entity without sending any packets.
   *
   * <p>This should be triggered when the entity representing a player changes
   * (such as during a respawn)
   *
   * @param old old player
   * @param newPlayer new one
   */
  public void replacePlayer(final ServerPlayerEntity old, ServerPlayerEntity newPlayer) {
    for(final ServerBossBar bar : this.bars.values()) {
      ((BulkServerBossBar) bar).replaceSubscriber(old, newPlayer);
    }
  }

  /**
   * Remove the player from all associated boss bars.
   *
   * @param player The player to remove
   */
  public void unsubscribeFromAll(final ServerPlayerEntity player) {
    for(Iterator<Map.Entry<BossBar, ServerBossBar>> it = this.bars.entrySet().iterator(); it.hasNext(); ) {
      final ServerBossBar bar = it.next().getValue();
      if(bar.getPlayers().contains(player)) {
        bar.removePlayer(player);
        if(bar.getPlayers().isEmpty()) {
          it.remove();
        }
      }
    }
  }

  @Override
  protected ServerBossBar newBar(final @NonNull Text title, final net.minecraft.entity.boss.BossBar.@NonNull Color color, final net.minecraft.entity.boss.BossBar.@NonNull Style style) {
    return new ServerBossBar(title, color, style);
  }
}
