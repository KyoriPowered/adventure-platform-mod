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

package net.kyori.adventure.platform.fabric.mixin;

import net.kyori.adventure.platform.fabric.BulkServerBossBar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerBossBar.class)
public abstract class MixinServerBossBar extends BossBar implements BulkServerBossBar {
  private static final float MINIMUM_PERCENT_CHANGE = 5e-4f;

  private float adventure$lastSentPercent;
  @Shadow @Final private Set<ServerPlayerEntity> players;

  public MixinServerBossBar(final UUID uuid, final Text name, final Color color, final Style style) {
    super(uuid, name, color, style);
    adventure$lastSentPercent = percent;
  }

  @Shadow
  public abstract boolean isVisible();

  // If a player has respawned, we still want to be able to remove the player using old references to their entity
  @Redirect(method = "removePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"))
  private boolean removeByUuid(final Set<?> instance, final Object player) {
    if(instance.remove(player)) {
      return true;
    }
    if(!(player instanceof ServerPlayerEntity)) {
      return false;
    }

    final UUID testId = ((ServerPlayerEntity) player).getUuid();
    for(Iterator<?> it = instance.iterator(); it.hasNext(); ) {
      if(((ServerPlayerEntity) it.next()).getUuid().equals(testId)) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public void addAll(final Collection<ServerPlayerEntity> players) {
    final BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.ADD, this);
    for(final ServerPlayerEntity player : players) {
      if(this.players.add(player) && this.isVisible()) {
        player.networkHandler.sendPacket(pkt);
      }
    }
  }

  @Override
  public void removeAll(final Collection<ServerPlayerEntity> players) {
    final BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.REMOVE, this);
    for(final ServerPlayerEntity player : players) {
      if(this.players.remove(player) && this.isVisible()) {
        player.networkHandler.sendPacket(pkt);
      }
    }
  }

  @Override
  public void replaceSubscriber(final ServerPlayerEntity oldSub, final ServerPlayerEntity newSub) {
    if(this.players.remove(oldSub)) {
      this.players.add(newSub);
    }
  }

  // Optimization -- don't send a packet for tiny changes

  @Inject(method = "setPercent", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/BossBar;setPercent(F)V"), cancellable = true, require = 0)
  private void onlySetPercentIfBigEnough(float newPercent, CallbackInfo ci) {
    if(Math.abs(newPercent - adventure$lastSentPercent) < MINIMUM_PERCENT_CHANGE) {
      this.percent = newPercent;
      ci.cancel();
    } else {
      adventure$lastSentPercent = newPercent;
      // continue as normal
    }
  }
}
