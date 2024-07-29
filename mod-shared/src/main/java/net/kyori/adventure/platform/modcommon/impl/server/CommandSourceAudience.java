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
package net.kyori.adventure.platform.modcommon.impl.server;

import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ControlledAudience;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import org.jetbrains.annotations.NotNull;

/**
 * Audience implementation that can wrap a {@link CommandSource}.
 */
public final class CommandSourceAudience implements ControlledAudience {
  private final CommandSource output;
  private final MinecraftAudiencesInternal serializer;

  public CommandSourceAudience(final CommandSource output, final MinecraftAudiencesInternal serializer) {
    this.output = output;
    this.serializer = serializer;
  }

  @Override
  public @NotNull MinecraftAudiencesInternal controller() {
    return this.serializer;
  }

  @Override
  public void sendMessage(final @NotNull Component message) {
    this.output.sendSystemMessage(this.serializer.asNative(message));
  }

  @Override
  public void sendMessage(final @NotNull Component message, final ChatType.@NotNull Bound boundChatType) {
    this.output.sendSystemMessage(AdventureCommon.chatTypeToNative(boundChatType, this.serializer)
      .decorate(this.serializer.asNative(message)));
  }

  @Override
  public void sendMessage(final @NotNull SignedMessage signedMessage, final ChatType.@NotNull Bound boundChatType) {
    final Component message = signedMessage.unsignedContent() != null ? signedMessage.unsignedContent() : Component.text(signedMessage.message());
    this.output.sendSystemMessage(AdventureCommon.chatTypeToNative(boundChatType, this.serializer)
      .decorate(this.serializer.asNative(message)));
  }

  @Override
  @Deprecated
  public void sendMessage(final Identity source, final Component text, final MessageType type) {
    this.output.sendSystemMessage(this.serializer.asNative(text));
  }

  @Override
  public void sendActionBar(final @NotNull Component message) {
    this.sendMessage(message);
  }
}
