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
package net.kyori.adventure.platform.modcommon.impl.client.mixin.minecraft.player;

import com.mojang.authlib.GameProfile;
import java.util.Locale;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.platform.modcommon.impl.ControlledAudience;
import net.kyori.adventure.platform.modcommon.impl.LocaleHolderBridge;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.sound.Sound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends Player implements ForwardingAudience.Single, ControlledAudience, LocaleHolderBridge {
  // @formatter:off
  @Shadow @Final protected Minecraft minecraft;
  // @formatter:on

  // TODO: Do we want to enforce synchronization with the client thread?

  private final Audience adventure$default = MinecraftClientAudiences.of().audience();

  private LocalPlayerMixin(final Level level, final BlockPos blockPos, final float f, final GameProfile gameProfile) {
    super(level, blockPos, f, gameProfile);
  }

  @Override
  public @NotNull Audience audience() {
    return this.adventure$default;
  }

  @Override
  public @NotNull Pointers pointers() {
    return this.audience().pointers();
  }

  @Override
  public @NotNull MinecraftAudiencesInternal controller() {
    return (MinecraftAudiencesInternal) MinecraftClientAudiences.of();
  }

  @Override
  public void playSound(final @NotNull Sound sound) {
    this.audience().playSound(sound, this.getX(), this.getY(), this.getZ());
  }

  @Override
  public @NotNull Locale adventure$locale() {
    return ((LocaleHolderBridge) this.minecraft.options).adventure$locale();
  }
}
