/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2023-2024 KyoriPowered
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

import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.impl.ControlledAudience;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Resource pack callbacks for the modded environment.
 *
 * @since 6.0.0
 */
public final class ModResourcePackCallbacks {
  private static final ComponentLogger LOGGER = ComponentLogger.logger();
  private static final Component DEFAULT_KICK_MESSAGE = Component.translatable("multiplayer.requiredTexturePrompt.disconnect"); // vanilla translation

  private ModResourcePackCallbacks() {
  }

  /**
   * Kick a resource pack receiver if they do not accept a resource pack.
   *
   * @return the kicking callback
   * @since 5.11.0
   */
  public static @NotNull ResourcePackCallback kickIfNotApplied() {
    return kickIfNotApplied(DEFAULT_KICK_MESSAGE);
  }

  /**
   * Kick a resource pack receiver if they do not accept a resource pack.
   *
   * @param kickMessage the message to kick the player with
   * @return the kicking callback
   * @since 5.11.0
   */
  public static @NotNull ResourcePackCallback kickIfNotApplied(final @NotNull Component kickMessage) {
    requireNonNull(kickMessage, "kickMessage");

    return (uuid, status, audience) -> {
      if (!status.intermediate() && status != ResourcePackStatus.SUCCESSFULLY_LOADED) { // we've reached a terminal, non-successful state
        // now we attempt to extract a connection that can be kicked -- assuming that it's players that will be sent a resource pack
        if (!(audience instanceof ControlledAudience controlled)) {
          LOGGER.debug("Audience {} was not a ControlledAudience, we cannot kick them", audience);
          return;
        }
        final ServerCommonPacketListenerImpl kicker;
        if (audience instanceof ServerPlayer player) { // when you send a resource pack via the Audience implemented on ServerPlayer
          kicker = player.connection;
        } else {
          final @Nullable UUID id = audience.get(Identity.UUID).orElse(null);
          if (id == null) return; // not a player?
          if (!(controlled.controller() instanceof MinecraftServerAudiencesImpl server)) return;

          final ServerPlayer ply = server.server().getPlayerList().getPlayer(id);
          if (ply == null) return;

          kicker = ply.connection;
        }

        LOGGER.debug("Audience {} did not successfully apply a resource pack with ID {}, kicking with message: {}", kicker.getOwner(), uuid, kickMessage);
        kicker.disconnect(controlled.controller().toNative(kickMessage));
      }
    };
  }
}
