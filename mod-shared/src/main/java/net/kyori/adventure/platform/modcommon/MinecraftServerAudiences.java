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
package net.kyori.adventure.platform.modcommon;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerBridge;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@link Audience} instances for a specific server instance.
 *
 * @since 6.0.0
 */
public interface MinecraftServerAudiences extends AudienceProvider, MinecraftAudiences {
  /**
   * Get the shared audience provider for the server.
   *
   * <p>This provider will render messages using the global translation registry.</p>
   *
   * @param server server instance to work with
   * @return common audience provider
   * @since 6.0.0
   */
  static @NotNull MinecraftServerAudiences of(final @NotNull MinecraftServer server) {
    return ((MinecraftServerBridge) server).adventure$globalProvider();
  }

  /**
   * Create an audience provider for this server with customized settings.
   *
   * @param server the server
   * @return audience provider builder
   * @since 6.0.0
   */
  static MinecraftServerAudiences.@NotNull Builder builder(final @NotNull MinecraftServer server) {
    return new MinecraftServerAudiencesImpl.Builder(requireNonNull(server, "server"));
  }

  /**
   * Get an audience to send to a {@link CommandSourceStack}.
   *
   * <p>This will delegate to the native implementation by the command source, or
   * otherwise use a wrapping implementation.</p>
   *
   * @param source source to send to.
   * @return the audience
   * @since 6.0.0
   */
  @NotNull AdventureCommandSourceStack audience(@NotNull CommandSourceStack source);

  /**
   * Get an audience that will send to the provided {@link ServerPlayer}.
   *
   * <p>Depending on the specific source, the returned audience may only support
   * a subset of abilities.</p>
   *
   * @param source source to send to
   * @return an audience for the source
   * @since 6.1.0
   */
  @NotNull Audience audience(@NotNull ServerPlayer source);

  /**
   * Get an audience that will send to the provided {@link CommandSource}.
   *
   * <p>Depending on the specific source, the returned audience may only support
   * a subset of abilities.</p>
   *
   * @param source source to send to
   * @return an audience for the source
   * @since 6.0.0
   */
  @NotNull Audience audience(@NotNull CommandSource source);

  /**
   * Create an audience that will send to every listed player.
   *
   * @param players Players to send to.
   * @return a new audience
   * @since 6.0.0
   */
  @NotNull Audience audience(@NotNull Iterable<ServerPlayer> players);

  /**
   * Builder for {@link MinecraftServerAudiences} with custom attributes.
   *
   * @since 6.0.0
   */
  interface Builder extends AudienceProvider.Builder<MinecraftServerAudiences, MinecraftServerAudiences.Builder> {
  }
}
