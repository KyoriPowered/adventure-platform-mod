/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.commands;

import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommandSourceStackInternal;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static java.util.Objects.requireNonNull;

/**
 * The methods in this class should match the implementations of their Text-using counterparts in {@link CommandSourceStack}.
 */
@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin implements AdventureCommandSourceStackInternal {
  // @formatter:off
  @Shadow @Final private Entity entity;
  @Shadow @Final private CommandSource source;
  @Shadow @Final private boolean silent;
  @Shadow @Final private MinecraftServer server;

  @Shadow protected abstract void shadow$broadcastToAdmins(net.minecraft.network.chat.Component text);
  @Shadow public abstract void shadow$sendSuccess(Supplier<net.minecraft.network.chat.Component> text, boolean sendToOps);
  // @formatter:on

  private boolean adventure$assigned = false;
  private Audience adventure$out;
  private MinecraftServerAudiences adventure$controller;

  @Override
  public void sendSuccess(final @NotNull Component text, final boolean sendToOps) {
    this.audience(); // Populate controller if needed
    this.shadow$sendSuccess(() -> this.adventure$controller.asNative(text), sendToOps);
  }

  @Override
  public void sendLazySuccess(final @NotNull Supplier<Component> text, final boolean sendToOps) {
    requireNonNull(text, "text");
    this.audience(); // Populate controller if needed
    this.shadow$sendSuccess(() -> this.adventure$controller.asNative(text.get()), sendToOps);
  }

  @Override
  public void sendFailure(final @NotNull Component text) {
    if (this.source.acceptsFailure() && !this.silent) {
      this.sendMessage(text.color(NamedTextColor.RED));
    }
  }

  @Override
  public @NotNull Audience audience() {
    if (this.adventure$out == null) {
      if (this.server == null) {
        throw new IllegalStateException("Cannot use adventure operations without an available server!");
      }
      this.adventure$controller = MinecraftServerAudiences.of(this.server);
      this.adventure$out = this.entity instanceof final ServerPlayer ply && ply.commandSource() == this.source ? this.adventure$controller.audience(ply) : this.adventure$controller.audience(this.source);
    }
    return this.adventure$out;
  }

  @Override
  public @NotNull Identity identity() {
    if (this.source instanceof Identified) {
      return ((Identified) this.source).identity();
    } else {
      return Identity.nil();
    }
  }

  @Override
  public AdventureCommandSourceStack adventure$audience(final Audience wrapped, final MinecraftServerAudiences controller) {
    if (this.adventure$assigned && !Objects.equals(controller, this.adventure$controller)) {
      throw new IllegalStateException("This command source has been attached to a specific renderer already!");
      // TODO: return a new instance
    }
    this.adventure$assigned = true;
    this.adventure$out = wrapped;
    this.adventure$controller = controller;
    return this;
  }

  @Override
  public CommandSource adventure$source() {
    return this.source;
  }
}
