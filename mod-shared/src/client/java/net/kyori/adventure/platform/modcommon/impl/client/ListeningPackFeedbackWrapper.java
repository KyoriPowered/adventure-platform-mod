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
package net.kyori.adventure.platform.modcommon.impl.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.resource.ResourcePackCallback;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.client.resources.server.PackLoadFeedback;
import org.jetbrains.annotations.NotNull;

public final class ListeningPackFeedbackWrapper implements PackLoadFeedback {
  private static final ComponentLogger LOGGER = ComponentLogger.logger();
  private static final Map<UUID, PackCallback> CALLBACKS = new ConcurrentHashMap<>();

  private final PackLoadFeedback delegate;

  record PackCallback(@NotNull ResourcePackCallback cb, @NotNull ClientAudience listener) {}

  public static void registerCallback(final UUID pack, final ResourcePackCallback cb, final ClientAudience audience) {
    if (CALLBACKS.put(pack, new PackCallback(cb, audience)) != null) {
      LOGGER.warn("Duplicate client resource pack callbacks registered for pack {}", pack);
    }
  }

  public ListeningPackFeedbackWrapper(final PackLoadFeedback delegate) {
    this.delegate = delegate;
  }

  @Override
  public void reportUpdate(final UUID packId, final Update status) {
    final PackCallback cb = CALLBACKS.get(packId);
    if (cb != null) {
      final ResourcePackStatus advStatus = switch (status) {
        case ACCEPTED -> ResourcePackStatus.ACCEPTED;
        case DOWNLOADED -> ResourcePackStatus.DOWNLOADED;
      };
      cb.cb().packEventReceived(packId, advStatus, cb.listener());
      return; // we have a clientside pack, handle it ourselves rather than sending to server
    }
    this.delegate.reportUpdate(packId, status);
  }

  @Override
  public void reportFinalResult(final UUID packId, final FinalResult status) {
    final PackCallback cb = CALLBACKS.remove(packId);
    if (cb != null) {
      final ResourcePackStatus advStatus = switch (status) {
        case DECLINED -> ResourcePackStatus.DECLINED;
        case APPLIED -> ResourcePackStatus.SUCCESSFULLY_LOADED;
        case DISCARDED -> ResourcePackStatus.DISCARDED;
        case DOWNLOAD_FAILED -> ResourcePackStatus.FAILED_DOWNLOAD;
        case ACTIVATION_FAILED -> ResourcePackStatus.FAILED_RELOAD;
      };

      cb.cb().packEventReceived(packId, advStatus, cb.listener());
      return; // we have a clientside pack, handle it ourselves rather than sending to server
    }
    this.delegate.reportFinalResult(packId, status);
  }
}
