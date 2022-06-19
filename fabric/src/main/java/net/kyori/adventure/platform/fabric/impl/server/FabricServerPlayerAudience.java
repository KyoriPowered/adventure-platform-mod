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
package net.kyori.adventure.platform.fabric.impl.server;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.modcommon.impl.server.ServerPlayerAudience;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public final class FabricServerPlayerAudience extends ServerPlayerAudience {
  private final FabricServerAudiencesImpl controller;

  public FabricServerPlayerAudience(final ServerPlayer player, final FabricServerAudiencesImpl controller) {
    super(player, controller);
    this.controller = controller;
  }

  @Override
  public void showBossBar(final @NotNull BossBar bar) {
    FabricServerAudiencesImpl.forEachInstance(controller -> {
      if (controller != this.controller) {
        controller.bossBars.unsubscribe(this.player, bar);
      }
    });
    this.controller.bossBars.subscribe(this.player, bar);
  }

  @Override
  public void hideBossBar(final @NotNull BossBar bar) {
    FabricServerAudiencesImpl.forEachInstance(controller -> controller.bossBars.unsubscribe(this.player, bar));
  }

}
