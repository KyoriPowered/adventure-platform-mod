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

package net.kyori.adventure.platform.fabric.mixin;

import com.mojang.authlib.GameProfile;
import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricPlatform;
import net.kyori.adventure.platform.fabric.GameEnums;
import net.kyori.adventure.platform.fabric.server.ServerBossBarListener;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.minecraft.client.options.ChatVisibility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements Audience {
  @Shadow public ServerPlayNetworkHandler networkHandler;

  @Shadow
  private ChatVisibility clientChatVisibility;

  public MixinServerPlayerEntity(final World world, final BlockPos pos, final float yaw, final GameProfile gameProfile) {
    super(world, pos, yaw, gameProfile);
  }

  @Override
  public void sendMessage(final Component text, final net.kyori.adventure.audience.MessageType type) {
    final MessageType mcType;
    final boolean shouldSend;
    if(type == net.kyori.adventure.audience.MessageType.CHAT) {
      mcType = MessageType.CHAT;
      shouldSend = this.clientChatVisibility == ChatVisibility.FULL;
    } else {
      mcType = MessageType.SYSTEM;
      shouldSend = this.clientChatVisibility == ChatVisibility.FULL || this.clientChatVisibility == ChatVisibility.SYSTEM;
    }

    if(shouldSend) {
      this.networkHandler.sendPacket(new GameMessageS2CPacket(FabricPlatform.adapt(text), mcType, Util.NIL_UUID));
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, FabricPlatform.adapt(message)));
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    ServerBossBarListener.INSTANCE.subscribe((ServerPlayerEntity) (Object) this, bar);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    ServerBossBarListener.INSTANCE.unsubscribe((ServerPlayerEntity) (Object) this, bar);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    this.networkHandler.sendPacket(new PlaySoundIdS2CPacket(FabricPlatform.adapt(sound.name()),
      GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), this.getPos(), sound.volume(), sound.pitch()));
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    this.networkHandler.sendPacket(new PlaySoundIdS2CPacket(FabricPlatform.adapt(sound.name()),
      GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), new Vec3d(x, y, z), sound.volume(), sound.pitch()));
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    final /* @Nullable */ Key sound = stop.sound();
    final Sound./* @Nullable */ Source src = stop.source();
    final /* @Nullable */ SoundCategory cat = src == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(src);
    this.networkHandler.sendPacket(new StopSoundS2CPacket(sound == null ? null : FabricPlatform.adapt(sound), cat));
  }

  private static final String BOOK_TITLE = "title";
  private static final String BOOK_AUTHOR = "author";
  private static final String BOOK_PAGES = "pages";
  private static final String BOOK_RESOLVED = "resolved";

  @Override
  public void openBook(final @NonNull Book book) {
    final ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK, 1);
    final CompoundTag bookTag = bookStack.getOrCreateTag();
    bookTag.putString(BOOK_TITLE, this.adventure$serialize(book.title()));
    bookTag.putString(BOOK_AUTHOR, this.adventure$serialize(book.author()));
    final ListTag pages = new ListTag();
    for(final Component page : book.pages()) {
      pages.add(StringTag.of(this.adventure$serialize(page)));
    }
    bookTag.put(BOOK_PAGES, pages);
    bookTag.putBoolean(BOOK_RESOLVED, true); // todo: any parseable texts?

    final ItemStack previous = this.inventory.getMainHandStack();
    this.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, this.inventory.selectedSlot, bookStack));
    this.openEditBookScreen(bookStack, Hand.MAIN_HAND);
    this.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, this.inventory.selectedSlot, previous));
  }

  private String adventure$serialize(final @NonNull Component component) {
    return GsonComponentSerializer.gson().serialize(component);
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    if(title.subtitle() != TextComponent.empty()) {
      this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.SUBTITLE, FabricPlatform.adapt(title.subtitle())));
    }

    final int fadeIn = this.ticks(title.fadeInTime());
    final int fadeOut = this.ticks(title.fadeOutTime());
    final int dwell = this.ticks(title.stayTime());
    if(fadeIn != -1 || fadeOut != -1 || dwell != -1) {
      this.networkHandler.sendPacket(new TitleS2CPacket(fadeIn, dwell, fadeOut));
    }

    if(title.title() != TextComponent.empty()) {
      this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TITLE, FabricPlatform.adapt(title.title())));
    }
  }

  private int ticks(final @NonNull Duration duration) {
    return duration.getSeconds() == -1 ? -1 : (int) (duration.toMillis() / 50);
  }

  @Override
  public void clearTitle() {
    this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.CLEAR, null));
  }

  @Override
  public void resetTitle() {
    this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.RESET, null));
  }

  // Player tracking for boss bars

  @Inject(method = "copyFrom", at = @At("RETURN"))
  private void copyBossBars(final ServerPlayerEntity old, final boolean alive, final CallbackInfo ci) {
    ServerBossBarListener.INSTANCE.replacePlayer(old, (ServerPlayerEntity) (Object) this);
  }

  @Inject(method = "onDisconnect", at = @At("RETURN"))
  private void removeBossBarsOnDisconnect(final CallbackInfo ci) {
    ServerBossBarListener.INSTANCE.unsubscribeFromAll((ServerPlayerEntity) (Object) this);
  }

}
