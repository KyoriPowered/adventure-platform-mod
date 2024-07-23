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

import java.util.function.Consumer;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ControlledAudience;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

public final class PlainAudience implements ControlledAudience {
  private final MinecraftAudiencesInternal controller;
  private final Pointered source;
  private final Consumer<String> plainOutput;

  public PlainAudience(final MinecraftAudiencesInternal controller, final Pointered source, final Consumer<String> plainOutput) {
    this.controller = controller;
    this.source = source;
    this.plainOutput = plainOutput;
  }

  @Override
  public @NotNull MinecraftAudiencesInternal controller() {
    return this.controller;
  }

  @Override
  public void sendMessage(final @NotNull Component message) {
    this.plainOutput.accept(PlainTextComponentSerializer.plainText().serialize(message));
  }

  @Override
  public void sendMessage(final @NotNull Component message, final ChatType.@NotNull Bound boundChatType) {
    this.plainOutput.accept(AdventureCommon.chatTypeToNative(boundChatType, this.controller).decorate(this.controller.toNative(message)).getString());
  }

  @Override
  @Deprecated
  public void sendMessage(final Identity source, final Component text, final MessageType type) {
    this.sendMessage(text);
  }

  @Override
  public void sendActionBar(final @NotNull Component message) {
    this.sendMessage(message);
  }

  @Override
  public @NotNull Pointers pointers() {
    return this.source.pointers();
  }
}
