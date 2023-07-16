/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2023 KyoriPowered
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

import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBarViewer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A specialization of the {@link BossBarViewer} interface for the Fabric ecosystem.
 *
 * <p>In the modded world, we apply a few extra constraints to limit the realm of possibilities that can be encountered:</p>
 * <ul>
 *   <li>Boss bars cannot be viewed across logical sides (i.e, if a boss bar instance exists on the server thread of a local game,
 *   it cannot also be used on the client thread)</li>
 *   <li>Any externally-created boss bars (from mobs, the {@code /bossbar command}, other mods) will only appear on the
 *   <em>primary</em> {@link FabricServerAudiences} instance for a server (i.e. the one with the default renderer).</li>
 *   <li>Boss bars can, in addition to the restriction on crossing logical sides, only be sent to viewers on one
 *   controller ({@link FabricAudiences} instance) at a time.</li>
 * </ul>
 */
public interface FabricBossBarViewer extends BossBarViewer {
  @Override
  default @UnmodifiableView @NotNull Iterable<? extends BossBar> activeBossBars() {
    return List.of(); // implemented by Mixin
  }
}
