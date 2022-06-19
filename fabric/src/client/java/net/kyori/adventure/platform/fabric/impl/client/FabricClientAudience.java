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
package net.kyori.adventure.platform.fabric.impl.client;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.platform.fabric.impl.accessor.LevelAccess;
import net.kyori.adventure.platform.fabric.impl.client.bridge.BossHealthOverlayBridge;
import net.kyori.adventure.platform.fabric.impl.client.mixin.AbstractSoundInstanceAccess;
import net.kyori.adventure.platform.modcommon.impl.GameEnums;
import net.kyori.adventure.platform.modcommon.impl.client.ClientAudience;
import net.kyori.adventure.sound.Sound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class FabricClientAudience extends ClientAudience {
  private final FabricClientAudiencesImpl controller;

  public FabricClientAudience(final Minecraft client, final FabricClientAudiencesImpl renderer) {
    super(client, renderer);
    this.controller = renderer;
  }

  @Override
  public void showBossBar(final @NotNull BossBar bar) {
    BossHealthOverlayBridge.listener(this.client.gui.getBossOverlay(), this.controller).add(bar);
  }

  @Override
  public void hideBossBar(final @NotNull BossBar bar) {
    BossHealthOverlayBridge.listener(this.client.gui.getBossOverlay(), this.controller).remove(bar);
  }

  @Override
  public void playSound(final @NotNull Sound sound, final Sound.@NotNull Emitter emitter) {
    final Entity targetEntity;
    if (emitter == Sound.Emitter.self()) {
      targetEntity = this.client.player;
    } else if (emitter instanceof final Entity entity) {
      targetEntity = entity;
    } else {
      throw new IllegalArgumentException("Provided emitter '" + emitter + "' was not Sound.Emitter.self() or an Entity");
    }

    // Initialize with a placeholder event
    final EntityBoundSoundInstance mcSound = new EntityBoundSoundInstance(
      SoundEvents.ITEM_PICKUP,
      GameEnums.SOUND_SOURCE.toMinecraft(sound.source()),
      sound.volume(),
      sound.pitch(),
      targetEntity,
      ((LevelAccess) targetEntity.level).accessor$threadSafeRandom().nextLong()
    );
    // Then apply the ResourceLocation of our real sound event
    ((AbstractSoundInstanceAccess) mcSound).setLocation(FabricAudiences.toNative(sound.name()));

    this.client.getSoundManager().play(mcSound);
  }

  @Override
  protected long nextSoundSeed() {
      return ((LevelAccess) this.client.level).accessor$threadSafeRandom().nextLong();
  }
}
