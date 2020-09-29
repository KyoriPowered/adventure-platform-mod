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

package net.kyori.adventure.platform.fabric.impl.mixin;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.fabric.FabricServerAudienceProvider;
import net.kyori.adventure.platform.fabric.impl.LocaleHolderBridge;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.platform.fabric.impl.accessor.ServerboundClientInformationPacketAccess;
import net.kyori.adventure.platform.fabric.impl.server.FabricServerAudienceProviderImpl;
import net.kyori.adventure.platform.fabric.impl.server.RenderableAudience;
import net.kyori.adventure.platform.fabric.impl.server.ServerPlayerAudience;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements ForwardingAudience.Single, LocaleHolderBridge, RenderableAudience {
  @Shadow @Final
  public MinecraftServer server;

  @Shadow
  public ServerGamePacketListenerImpl connection;
  private @MonotonicNonNull Audience adventure$backing;
  private Locale adventure$locale;
  private final Map<FabricServerAudienceProviderImpl, Audience> adventure$renderers = new MapMaker().weakKeys().makeMap();

  public ServerPlayerMixin(final Level level, final BlockPos blockPos, final float f, final GameProfile gameProfile) {
    super(level, blockPos, f, gameProfile);
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  private void applyBacking(final CallbackInfo ci) {
    this.adventure$backing = FabricServerAudienceProvider.of(this.server).audience(this);
  }

  @Override
  public @NonNull Audience audience() {
    return this.adventure$backing;
  }

  @Override
  public Audience renderUsing(final FabricServerAudienceProviderImpl controller) {
    return this.adventure$renderers.computeIfAbsent(controller, ctrl -> new ServerPlayerAudience((ServerPlayer) (Object) this, ctrl));
  }

  @Override
  public Locale adventure$locale() {
    return this.adventure$locale;
  }

  // Locale tracking

  @Inject(method = "updateOptions", at = @At("HEAD"))
  private void handleLocaleUpdate(final ServerboundClientInformationPacket information, final CallbackInfo ci) {
    final String language = ((ServerboundClientInformationPacketAccess) information).getLanguage();
    final /* @Nullable */ Locale locale = LocaleHolderBridge.toLocale(language);
    if(!Objects.equals(this.adventure$locale, locale)) {
      this.adventure$locale = locale;
      PlayerLocales.CHANGED_EVENT.invoker().onLocaleChanged((ServerPlayer) (Object) this, locale);
    }
  }

  // Player tracking for boss bars

  @Inject(method = "restoreFrom", at = @At("RETURN"))
  private void copyBossBars(final ServerPlayer old, final boolean alive, final CallbackInfo ci) {
    FabricServerAudienceProviderImpl.forEachInstance(controller -> controller.bossBars().replacePlayer(old, (ServerPlayer) (Object) this));
  }

  @Inject(method = "disconnect", at = @At("RETURN"))
  private void removeBossBarsOnDisconnect(final CallbackInfo ci) {
    FabricServerAudienceProviderImpl.forEachInstance(controller -> controller.bossBars().unsubscribeFromAll((ServerPlayer) (Object) this));
  }
}
