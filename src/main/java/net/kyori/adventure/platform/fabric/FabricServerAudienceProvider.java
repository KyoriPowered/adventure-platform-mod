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

package net.kyori.adventure.platform.fabric;

import static java.util.Objects.requireNonNull;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.fabric.impl.server.FabricServerAudienceProviderImpl;
import net.kyori.adventure.platform.fabric.impl.server.MinecraftServerBridge;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;

public interface FabricServerAudienceProvider extends AudienceProvider, FabricAudiences {

  static @NonNull FabricServerAudienceProvider of(@NonNull MinecraftServer server) {
    return ((MinecraftServerBridge) server).adventure$globalProvider();
  }

  static @NonNull FabricServerAudienceProvider of(@NonNull MinecraftServer server, @NonNull ComponentRenderer<Locale> renderer) {
    return new FabricServerAudienceProviderImpl(requireNonNull(server, "server"), requireNonNull(renderer, "renderer"));
  }

  /**
   * Get an audience to send to a {@link CommandSourceStack}.
   *
   * This will delegate to the native implementation by the command source, or
   * otherwise use a wrapping implementation.
   *
   * @param source source to send to.
   * @return the audience
   */
  AdventureCommandSourceStack audience(@NonNull CommandSourceStack source);

  Audience audience(@NonNull CommandSource output);

  Audience audience(@NonNull Iterable<ServerPlayer> players);
}
