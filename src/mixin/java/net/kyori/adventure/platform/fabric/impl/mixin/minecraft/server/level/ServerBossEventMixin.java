/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2020-2022 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.server.level;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.platform.fabric.impl.server.ServerBossEventBridge;
import net.kyori.adventure.platform.fabric.impl.server.ServerPlayerBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerBossEvent.class)
public abstract class ServerBossEventMixin extends BossEvent implements ServerBossEventBridge {
  private static final float MINIMUM_PERCENT_CHANGE = 5e-4f;

  // @formatter:off
  @Shadow @Final private Set<ServerPlayer> players;

  @Shadow public abstract boolean shadow$isVisible();
  // @formatter:on

  private float adventure$lastSentPercent;

  public ServerBossEventMixin(final UUID uuid, final Component name, final BossBarColor color, final BossBarOverlay style) {
    super(uuid, name, color, style);
    this.adventure$lastSentPercent = this.progress;
  }

  // If a player has respawned, we still want to be able to remove the player using old references to their entity
  @Redirect(method = "removePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"))
  private boolean adventure$removeByUuid(final Set<?> instance, final Object player) {
    if (player instanceof ServerPlayerBridge br) {
      br.adventure$bridge$untrackBossEvent((ServerBossEvent) (Object) this);
    }

    if (instance.remove(player)) {
      return true;
    }

    if (!(player instanceof ServerPlayer server)) {
      return false;
    }

    final UUID testId = server.getUUID();
    for (final Iterator<?> it = instance.iterator(); it.hasNext();) {
      if (((ServerPlayer) it.next()).getUUID().equals(testId)) {
        it.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public void adventure$addAll(final Collection<ServerPlayer> players) {
    final ClientboundBossEventPacket pkt = ClientboundBossEventPacket.createAddPacket(this);
    for (final ServerPlayer player : players) {
      if (this.players.add(player) && this.shadow$isVisible()) {
        ((ServerPlayerBridge) player).adventure$bridge$trackBossEvent((ServerBossEvent) (Object) this);
        player.connection.send(pkt);
      }
    }
  }

  @Override
  public void adventure$removeAll(final Collection<ServerPlayer> players) {
    final ClientboundBossEventPacket pkt = ClientboundBossEventPacket.createRemovePacket(this.getId());
    for (final ServerPlayer player : players) {
      ((ServerPlayerBridge) player).adventure$bridge$untrackBossEvent((ServerBossEvent) (Object) this);
      if (this.players.remove(player) && this.shadow$isVisible()) {
        player.connection.send(pkt);
      }
    }
  }

  @Override
  public void adventure$replaceSubscriber(final ServerPlayer oldSub, final ServerPlayer newSub) {
    if (this.players.remove(oldSub)) {
      this.players.add(newSub);
    }
  }

  // track on players

  @Inject(method = "addPlayer", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/server/level/ServerPlayer;connection:Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"))
  private void adventure$trackBossEventsOnPlayer(final ServerPlayer player, final CallbackInfo ci) {
    ((ServerPlayerBridge) player).adventure$bridge$trackBossEvent((ServerBossEvent) (Object) this);
  }

  @Inject(method = "removePlayer", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/server/level/ServerPlayer;connection:Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"))
  private void adventure$untrackBossEventsOnPlayer(final ServerPlayer player, final CallbackInfo ci) {
    ((ServerPlayerBridge) player).adventure$bridge$untrackBossEvent((ServerBossEvent) (Object) this);
  }

  @Inject(method = "setVisible", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/server/level/ServerPlayer;connection:Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"))
  private void adventure$adjustTrackingForVisibility(final boolean visible, final CallbackInfo ci) {
    for (final ServerPlayer player : this.players) {
      if (visible) {
        ((ServerPlayerBridge) player).adventure$bridge$trackBossEvent((ServerBossEvent) (Object) this);
      } else {
        ((ServerPlayerBridge) player).adventure$bridge$untrackBossEvent((ServerBossEvent) (Object) this);
      }
      // todo: make sure stale ServerPlayer instances are not tracked thru boss bars
    }
  }

  // Optimization -- don't send a packet for tiny changes

  @Inject(method = "setProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BossEvent;setProgress(F)V"), cancellable = true, require = 0)
  private void adventure$onlySetPercentIfBigEnough(final float newPercent, final CallbackInfo ci) {
    if (Math.abs(newPercent - this.adventure$lastSentPercent) < MINIMUM_PERCENT_CHANGE) {
      this.progress = newPercent;
      ci.cancel();
    } else {
      this.adventure$lastSentPercent = newPercent;
      // continue as normal
    }
  }
}
