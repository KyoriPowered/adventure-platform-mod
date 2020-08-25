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

package net.kyori.adventure.platform.fabric.mixin.client;

import com.mojang.authlib.GameProfile;
import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricPlatform;
import net.kyori.adventure.platform.fabric.GameEnums;
import net.kyori.adventure.platform.fabric.client.AdventureBookAccess;
import net.kyori.adventure.platform.fabric.client.BossHealthOverlayBridge;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer implements Audience {

  // TODO: Do we want to enforce synchronization with the client thread?

  private LocalPlayerMixin(final ClientLevel level, final GameProfile profile) { // mixin will strip
    super(level, profile);
  }

  @Shadow @Final protected Minecraft minecraft;

  @Shadow public abstract void displayClientMessage(net.minecraft.network.chat.Component component, boolean bl);

  @Override
  public void sendMessage(final @NonNull Component message, final @NonNull MessageType type) {
    final ChatVisiblity visibility = this.minecraft.options.chatVisibility;
    if(type == MessageType.CHAT) {
      // Add to chat queue (following delay and such)
      if(visibility == ChatVisiblity.FULL) {
        this.minecraft.gui.getChat().enqueueMessage(FabricPlatform.adapt(message));
      }
    } else {
      // Add immediately as a system message
      if(visibility == ChatVisiblity.FULL || visibility == ChatVisiblity.SYSTEM) {
        this.minecraft.gui.getChat().addMessage(FabricPlatform.adapt(message));
      }
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.displayClientMessage(FabricPlatform.adapt(message), true);
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    final /* @Nullable */ net.minecraft.network.chat.Component titleText = title.title() == TextComponent.empty() ? null : FabricPlatform.adapt(title.title());
    final /* @Nullable */ net.minecraft.network.chat.Component subtitleText = title.subtitle() == TextComponent.empty() ? null : FabricPlatform.adapt(title.subtitle());
    final /* @Nullable */ Title.Times times = title.times();
    this.minecraft.gui.setTitles(titleText, subtitleText,
      this.adventure$ticks(times == null ? null : times.fadeIn()),
      this.adventure$ticks(times == null ? null : times.stay()),
      this.adventure$ticks(times == null ? null : times.fadeOut()));
  }

  private int adventure$ticks(final @Nullable Duration duration) {
    return duration == null || duration.getSeconds() == -1 ? -1 : (int) (duration.toMillis() / 50);
  }

  @Override
  public void clearTitle() {
    this.minecraft.gui.setTitles(null, null, -1, -1, -1);
  }

  @Override
  public void resetTitle() {
    this.minecraft.gui.resetTitleTimes();
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    BossHealthOverlayBridge.listener(this.minecraft.gui.getBossOverlay()).add(bar);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    BossHealthOverlayBridge.listener(this.minecraft.gui.getBossOverlay()).remove(bar);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    this.playSound(sound, this.getX(), this.getY(), this.getZ());
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    this.minecraft.getSoundManager().play(new SimpleSoundInstance(FabricPlatform.adapt(sound.name()), GameEnums.SOUND_SOURCE.toMinecraft(sound.source()),
      sound.volume(), sound.pitch(), false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false));
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    final /* @Nullable */ Key sound = stop.sound();
    final /* @Nullable */ ResourceLocation soundIdent = sound == null ? null : FabricPlatform.adapt(sound);
    final Sound./* @Nullable */ Source source = stop.source();
    final /* @Nullable */ SoundSource category = source == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(source);
    this.minecraft.getSoundManager().stop(soundIdent, category);
  }

  @Override
  public void openBook(final @NonNull Book book) {
    this.minecraft.setScreen(new BookViewScreen(new AdventureBookAccess(book)));
  }
}
