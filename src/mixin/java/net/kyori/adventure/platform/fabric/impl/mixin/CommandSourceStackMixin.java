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

package net.kyori.adventure.platform.fabric.impl.mixin;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.fabric.AdventureCommandSourceStack;
import net.kyori.adventure.platform.fabric.impl.AdventureCommandSourceStackInternal;
import net.kyori.adventure.platform.fabric.FabricServerAudienceProvider;
import net.kyori.adventure.platform.fabric.impl.server.FabricServerAudienceProviderImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * The methods in this class should match the implementations of their Text-using counterparts in {@link CommandSourceStack}
 *
 */
@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements AdventureCommandSourceStackInternal {
  @Shadow @Final private CommandSource source;
  @Shadow @Final private boolean silent;

  @Shadow protected abstract void broadcastToAdmins(net.minecraft.network.chat.Component text);

  @Shadow @Final
  private MinecraftServer server;
  private boolean assigned = false;
  private @MonotonicNonNull Audience out;
  private @MonotonicNonNull FabricServerAudienceProvider controller;

  @Override
  public void sendSuccess(final Component text, final boolean sendToOps) {
    if(this.source.acceptsSuccess() && !this.silent) {
      this.sendMessage(text);
    }

    if(sendToOps && this.source.shouldInformAdmins() && !this.silent) {
      this.broadcastToAdmins(this.controller.toNative(text));
    }
  }

  @Override
  public void sendFailure(final Component text) {
    if(this.source.acceptsFailure()) {
      this.sendMessage(text.color(NamedTextColor.RED));
    }
  }

  @Override
  public Audience audience() {
    if(this.out == null) {
      if(this.server == null) {
        throw new IllegalStateException("Cannot use adventure operations without an available server!");
      }
      this.controller = FabricServerAudienceProvider.of(this.server);
      this.out = this.controller.audience(this.source);
    }
    return this.out;
  }

  @Override
  public AdventureCommandSourceStack adventure$audience(final Audience wrapped, final FabricServerAudienceProviderImpl controller) {
    if(this.assigned) {
      throw new IllegalStateException("This command source has been attached to a specific renderer already!");
      // TODO: return a new instance
    }
    this.assigned = true;
    this.out = wrapped;
    this.controller = controller;
    return this;
  }

  @Override
  public CommandSource adventure$source() {
    return this.source;
  }
}
