/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2021 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.client;

import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.platform.fabric.impl.GameEnums;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientAudience implements Audience {
  private final Minecraft client;
  private final FabricClientAudiencesImpl controller;

  public ClientAudience(final Minecraft client, final FabricClientAudiencesImpl renderer) {
    this.client = client;
    this.controller = renderer;
  }

  @Override
  public void sendMessage(final Identity source, final @NonNull Component message, final @NonNull MessageType type) {
    if(this.client.isBlocked(source.uuid())) return;

    final ChatVisiblity visibility = this.client.options.chatVisibility;
    if(type == MessageType.CHAT) {
      // Add to chat queue (following delay and such)
      if(visibility == ChatVisiblity.FULL) {
        this.client.gui.getChat().enqueueMessage(this.controller.toNative(message));
      }
    } else {
      // Add immediately as a system message
      if(visibility == ChatVisiblity.FULL || visibility == ChatVisiblity.SYSTEM) {
        this.client.gui.getChat().addMessage(this.controller.toNative(message));
      }
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.client.gui.setOverlayMessage(this.controller.toNative(message), false);
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    final net.minecraft.network.chat.@Nullable Component titleText = title.title() == Component.empty() ? null : this.controller.toNative(title.title());
    final net.minecraft.network.chat.@Nullable Component subtitleText = title.subtitle() == Component.empty() ? null : this.controller.toNative(title.subtitle());
    final Title.@Nullable Times times = title.times();
    this.client.gui.setTitle(titleText);;
    this.client.gui.setSubtitle(subtitleText);
    this.client.gui.setTimes(
      this.adventure$ticks(times == null ? null : times.fadeIn()),
      this.adventure$ticks(times == null ? null : times.stay()),
      this.adventure$ticks(times == null ? null : times.fadeOut())
    );
  }

  private int adventure$ticks(final @Nullable Duration duration) {
    return duration == null || duration.getSeconds() == -1 ? -1 : (int) (duration.toMillis() / 50);
  }

  @Override
  public void clearTitle() {
    this.client.gui.setTitle(null);
    this.client.gui.setSubtitle(null);
  }

  @Override
  public void resetTitle() {
    this.client.gui.resetTitleTimes();
    this.clearTitle();
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    BossHealthOverlayBridge.listener(this.client.gui.getBossOverlay(), this.controller).add(bar);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    BossHealthOverlayBridge.listener(this.client.gui.getBossOverlay(), this.controller).remove(bar);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    final @Nullable LocalPlayer player = this.client.player;
    if(player != null) {
      this.playSound(sound, player.getX(), player.getY(), player.getZ());
    } else {
      // not in-game
      this.client.getSoundManager().play(new SimpleSoundInstance(FabricAudiences.toNative(sound.name()), GameEnums.SOUND_SOURCE.toMinecraft(sound.source()),
        sound.volume(), sound.pitch(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
    }
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    this.client.getSoundManager().play(new SimpleSoundInstance(FabricAudiences.toNative(sound.name()), GameEnums.SOUND_SOURCE.toMinecraft(sound.source()),
      sound.volume(), sound.pitch(), false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false));
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    final @Nullable Key sound = stop.sound();
    final @Nullable ResourceLocation soundIdent = sound == null ? null : FabricAudiences.toNative(sound);
    final Sound.@Nullable Source source = stop.source();
    final @Nullable SoundSource category = source == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(source);
    this.client.getSoundManager().stop(soundIdent, category);
  }

  @Override
  public void openBook(final @NonNull Book book) {
    this.client.setScreen(new BookViewScreen(new AdventureBookAccess(book, this.controller.localeRenderer())));
  }

  @Override
  public void sendPlayerListHeader(final @NonNull Component header) {
    this.client.gui.getTabList().setHeader(header == Component.empty() ? null : this.controller.toNative(header));
  }

  @Override
  public void sendPlayerListFooter(final @NonNull Component footer) {
    this.client.gui.getTabList().setHeader(footer == Component.empty() ? null : this.controller.toNative(footer));
  }

  @Override
  public void sendPlayerListHeaderAndFooter(final @NonNull Component header, final @NonNull Component footer) {
    this.sendPlayerListHeader(header);
    this.sendPlayerListFooter(footer);
  }
}
