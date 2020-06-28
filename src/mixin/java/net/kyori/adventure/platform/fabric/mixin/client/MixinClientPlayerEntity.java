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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricPlatform;
import net.kyori.adventure.platform.fabric.GameEnums;
import net.kyori.adventure.platform.fabric.client.AdventureBookContents;
import net.kyori.adventure.platform.fabric.client.BossBarHudBridge;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.title.Title;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements Audience {

  // TODO: Do we want to enforce synchronization with the client thread?

  private MixinClientPlayerEntity(final ClientWorld world, final GameProfile profile) { // mixin will strip
    super(world, profile);
  }

  @Shadow public abstract void sendMessage(final Text message, final boolean actionBar);
  @Shadow @Final protected MinecraftClient client;

  @Override
  public void sendMessage(final @NonNull Component message) {
    this.sendMessage(FabricPlatform.adapt(message), false);
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.sendMessage(FabricPlatform.adapt(message), true);
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    final @Nullable Text titleText = title.title() == TextComponent.empty() ? null : FabricPlatform.adapt(title.title());
    final @Nullable Text subtitleText = title.subtitle() == TextComponent.empty() ? null : FabricPlatform.adapt(title.subtitle());
    this.client.inGameHud.setTitles(titleText, subtitleText,
      adventure$ticks(title.fadeInTime()),
      adventure$ticks(title.stayTime()),
      adventure$ticks(title.fadeOutTime()));
  }

  private int adventure$ticks(Duration duration) {
    return duration.getSeconds() == -1 ? -1 : (int) (duration.toMillis() / 50);
  }

  @Override
  public void clearTitle() {
    this.client.inGameHud.setTitles(null, null, -1, -1, -1);
  }

  @Override
  public void resetTitle() {
    this.client.inGameHud.setDefaultTitleFade();
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    BossBarHudBridge.listener(this.client.inGameHud.getBossBarHud()).add(bar);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    BossBarHudBridge.listener(this.client.inGameHud.getBossBarHud()).remove(bar);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    playSound(sound, this.getX(), getY(), getZ());
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    this.client.getSoundManager().play(new PositionedSoundInstance(FabricPlatform.adapt(sound.name()), GameEnums.SOUND_SOURCE.toMinecraft(sound.source()),
      sound.volume(), sound.pitch(), false, 0, SoundInstance.AttenuationType.LINEAR, x, y, z, false));
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    final @Nullable Key sound = stop.sound();
    final @Nullable Identifier soundIdent = sound == null ? null : FabricPlatform.adapt(sound);
    final Sound.@Nullable Source source = stop.source();
    final @Nullable SoundCategory category = source == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(source);
    this.client.getSoundManager().stopSounds(soundIdent, category);
  }

  @Override
  public void openBook(final @NonNull Book book) {
    this.client.openScreen(new BookScreen(new AdventureBookContents(book)));
  }
}
