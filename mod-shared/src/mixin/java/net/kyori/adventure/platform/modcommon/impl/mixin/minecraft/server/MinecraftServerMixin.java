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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.server;

import com.google.common.collect.MapMaker;
import java.util.Map;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.impl.ControlledAudience;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerBridge;
import net.kyori.adventure.platform.modcommon.impl.server.PlainAudience;
import net.kyori.adventure.platform.modcommon.impl.server.RenderableAudience;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.util.TriState;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Implement ComponentCommandOutput for output to the server console.
 */
@Mixin(value = MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerBridge, RenderableAudience, ForwardingAudience.Single, ControlledAudience {
  // @formatter:off
  @Shadow @Final private static Logger LOGGER;
  // @formatter:on

  @Shadow
  public abstract LayeredRegistryAccess<RegistryLayer> registries();

  private final MinecraftServerAudiencesImpl adventure$globalProvider = new MinecraftServerAudiencesImpl.Builder((MinecraftServer) (Object) this).build();
  private final Map<MinecraftAudiencesInternal, Audience> adventure$renderers = new MapMaker().weakKeys().makeMap();
  private final Audience adventure$backing = this.renderUsing(this.adventure$globalProvider);
  private volatile Pointers adventure$pointers;

  @Override
  public MinecraftServerAudiences adventure$globalProvider() {
    return this.adventure$globalProvider;
  }

  @Override
  public @NotNull Audience audience() {
    return this.adventure$backing;
  }

  @Override
  public Audience renderUsing(final MinecraftServerAudiencesImpl controller) {
    return this.adventure$renderers.computeIfAbsent(controller, ctrl -> new PlainAudience(ctrl, this, LOGGER::info));
  }

  @Override
  public @NotNull Pointers pointers() {
    if (this.adventure$pointers == null) {
      synchronized (this) {
        if (this.adventure$pointers == null) {
          return this.adventure$pointers = Pointers.builder()
            .withStatic(Identity.NAME, "Server")
            .withStatic(PermissionChecker.POINTER, perm -> TriState.TRUE)
            .build();
        }
      }
    }
    return this.adventure$pointers;
  }

  @Override
  public @NotNull MinecraftAudiencesInternal controller() {
    return this.adventure$globalProvider;
  }

  @Override
  public void refresh() {
    // nothing to refresh
  }
}
