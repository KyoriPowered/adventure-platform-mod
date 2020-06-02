/*
 * Copyright © 2020 zml [at] stellardrift [.] ca
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.stellardrift.text.fabric.mixin;

import ca.stellardrift.text.fabric.BulkServerBossBar;
import java.util.Collection;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ServerBossBar.class)
public abstract class MixinServerBossBar implements BulkServerBossBar {


  @Shadow @Final private Set<ServerPlayerEntity> players;

  @Shadow public abstract boolean isVisible();

  @Override
  public void addAll(final Collection<ServerPlayerEntity> players) {
    final BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.ADD, (BossBar) (Object) this);
    for (final ServerPlayerEntity player : players) {
      if (this.players.add(player) && this.isVisible()) {
        player.networkHandler.sendPacket(pkt);
      }
    }
  }

  @Override
  public void removeAll(final Collection<ServerPlayerEntity> players) {
    final BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.REMOVE, (BossBar) (Object) this);
    for (final ServerPlayerEntity player : players) {
      if (this.players.remove(player) && this.isVisible()) {
        player.networkHandler.sendPacket(pkt);
      }
    }
  }
}
