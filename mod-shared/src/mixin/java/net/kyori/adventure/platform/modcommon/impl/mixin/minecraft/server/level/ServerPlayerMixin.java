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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.server.level;

import com.google.common.collect.MapMaker;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.impl.ControlledAudience;
import net.kyori.adventure.platform.modcommon.impl.LocaleHolderBridge;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.world.entity.player.PlayerMixin;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.platform.modcommon.impl.server.RenderableAudience;
import net.kyori.adventure.platform.modcommon.impl.server.ServerPlayerAudience;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin implements ForwardingAudience.Single, LocaleHolderBridge, RenderableAudience, ControlledAudience {
  // @formatter:off
  @Shadow @Final public MinecraftServer server;
  @Shadow public ServerGamePacketListenerImpl connection;
  @Shadow private String language;
  // @formatter:on

  private Audience adventure$backing;
  private Locale adventure$locale;
  private final Map<MinecraftServerAudiencesImpl, Audience> adventure$renderers = new MapMaker().weakKeys().makeMap();

  protected ServerPlayerMixin(final EntityType<? extends LivingEntity> entityType, final Level level) {
    super(entityType, level);
  }

  @Inject(
    method = "<init>",
    at = @At(
      target = "Lnet/minecraft/server/level/ServerPlayer;updateOptions(Lnet/minecraft/server/level/ClientInformation;)V",
      value = "INVOKE",
      shift = At.Shift.AFTER
    )
  )
  private void adventure$init(final CallbackInfo ci) {
    // This is just setting it to the default en_us, to avoid event calls if it doesn't change (this is the reason for our target not being TAIL)
    this.adventure$locale = LocaleHolderBridge.toLocale(this.language);

    this.adventure$backing = MinecraftServerAudiences.of(this.server).audience((ServerPlayer) (Object) this);
  }

  @Override
  public @NotNull Audience audience() {
    return this.adventure$backing;
  }

  @Override
  public Audience renderUsing(final MinecraftServerAudiencesImpl controller) {
    return this.adventure$renderers.computeIfAbsent(controller, ctrl -> new ServerPlayerAudience((ServerPlayer) (Object) this, ctrl));
  }

  @Override
  public @NotNull Locale adventure$locale() {
    return this.adventure$locale;
  }

  @Override
  public void adventure$locale(final Locale locale) {
    this.adventure$locale = locale;
  }

  @Override
  public @NotNull MinecraftAudiencesInternal controller() {
    return (MinecraftAudiencesInternal) MinecraftServerAudiences.of(this.server);
  }

  // Player tracking for boss bars and rendering

  @Inject(method = "restoreFrom", at = @At("RETURN"))
  private void copyData(final ServerPlayer old, final boolean alive, final CallbackInfo ci) {
    MinecraftServerAudiencesImpl.forEachInstance(controller -> controller.bossBars().replacePlayer(old, (ServerPlayer) (Object) this));
  }

  @Inject(method = "disconnect", at = @At("RETURN"))
  private void adventure$removeBossBarsOnDisconnect(final CallbackInfo ci) {
    MinecraftServerAudiencesImpl.forEachInstance(controller -> controller.bossBars().unsubscribeFromAll((ServerPlayer) (Object) this));
  }
}
