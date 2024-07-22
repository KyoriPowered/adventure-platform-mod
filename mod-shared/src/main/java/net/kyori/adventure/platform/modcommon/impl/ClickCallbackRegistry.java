/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
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
package net.kyori.adventure.platform.modcommon.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class ClickCallbackRegistry {
  private static final Logger LOGGER = LogUtils.getLogger();

  public static final ClickCallbackRegistry INSTANCE = new ClickCallbackRegistry();

  public static final int CLEAN_UP_RATE = 30 /* seconds */;

  private static final String UUID_ARG = "uuid";
  private static final String COMMAND_NAME = "adventure_callback";
  private static final Component FAILURE_MESSAGE = Component.text("No callback with that ID could be found! You may have used it too many times, or it may have expired.", NamedTextColor.RED);

  private final Cache<UUID, CallbackRegistration> registrations = CacheBuilder.newBuilder()
    .expireAfterWrite(24, TimeUnit.HOURS)
    .maximumSize(1024) // to avoid unbounded growth
    .removalListener((RemovalListener<UUID, CallbackRegistration>) notification -> LOGGER.debug("Removing callback {} from cache for reason {}", notification.getKey(), notification.getCause()))
    .build();

  private ClickCallbackRegistry() {
  }

  /**
   * Register a callback, returning the command to be attached to a click event.
   *
   * @param callback the callback to register
   * @param options options
   * @return a new callback handler command
   * @since 5.7.0
   */
  public String register(final ClickCallback<Audience> callback, final ClickCallback.Options options) {
    final UUID id = UUID.randomUUID();
    final CallbackRegistration reg = new CallbackRegistration(
      options,
      callback,
      Instant.now().plus(options.lifetime()),
      new AtomicInteger()
    );

    this.registrations.put(id, reg);

    return "/" + COMMAND_NAME + " " + id;
  }

  public void registerToDispatcher(final CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal(COMMAND_NAME)
      .requires(HiddenRequirement.alwaysAllowed()) // hide from client tab completion
      .then(Commands.argument(UUID_ARG, UuidArgument.uuid())
        .executes(ctx -> {
          final UUID callbackId = UuidArgument.getUuid(ctx, UUID_ARG);
          final @Nullable CallbackRegistration reg = this.registrations.getIfPresent(callbackId);

          if (reg == null) {
            // ctx.getSource().sendFailure(FAILURE_MESSAGE); TODO
            return 0; // unsuccessful
          }

          // check use count
          boolean expire = false;
          boolean allowUse = true;
          final int allowedUses = reg.options.uses();
          if (allowedUses != ClickCallback.UNLIMITED_USES) {
            final int useCount = reg.useCount().incrementAndGet();
            if (useCount >= allowedUses) {
              expire = true;
              allowUse = !(useCount > allowedUses);
              // allowUse: determine
            }
          }

          // check duration expiry
          final Instant now = Instant.now();
          if (now.isAfter(reg.expiryTime())) {
            expire = true;
            allowUse = false;
          }

          if (expire) {
            this.registrations.invalidate(callbackId);
          }
          if (allowUse) {
            // TODO reg.callback().accept(ctx.getSource()); // pass the CommandSourceStack to the callback action
          } else {
            // TODO ctx.getSource().sendFailure(FAILURE_MESSAGE);
          }
          return Command.SINGLE_SUCCESS;
        })));
  }

  /**
   * Perform a periodic cleanup of callbacks.
   */
  public void cleanUp() {
    final Instant now = Instant.now();

    final Set<UUID> uuidsToClean = new HashSet<>();
    for (final var entry : this.registrations.asMap().entrySet()) {
      final var reg = entry.getValue();
      final int allowedUses = reg.options().uses();
      if (allowedUses != ClickCallback.UNLIMITED_USES && reg.useCount().get() >= allowedUses) {
        uuidsToClean.add(entry.getKey());
      } else if (now.isAfter(reg.expiryTime())) {
        uuidsToClean.add(entry.getKey());
      }
    }

    for (final UUID id : uuidsToClean) {
      this.registrations.invalidate(id);
    }
  }

  private record CallbackRegistration(ClickCallback.Options options, ClickCallback<Audience> callback, Instant expiryTime, AtomicInteger useCount) {}
}
