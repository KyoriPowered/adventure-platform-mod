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

import com.mojang.authlib.GameProfile;
import java.time.Duration;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.FabricAudienceProvider;
import net.kyori.adventure.platform.fabric.impl.GameEnums;
import net.kyori.adventure.platform.fabric.impl.server.ServerBossBarListener;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements Audience {

  @Shadow private ChatVisiblity chatVisibility;

  @Shadow public ServerGamePacketListenerImpl connection;

  public ServerPlayerMixin(final Level level, final BlockPos pos, final float yaw, final GameProfile gameProfile) {
    super(level, pos, yaw, gameProfile);
  }

  @Override
  public void sendMessage(final Component text, final net.kyori.adventure.audience.MessageType type) {
    final ChatType mcType;
    final boolean shouldSend;
    if(type == net.kyori.adventure.audience.MessageType.CHAT) {
      mcType = ChatType.CHAT;
      shouldSend = this.chatVisibility == ChatVisiblity.FULL;
    } else {
      mcType = ChatType.SYSTEM;
      shouldSend = this.chatVisibility == ChatVisiblity.FULL || this.chatVisibility == ChatVisiblity.SYSTEM;
    }

    if(shouldSend) {
      this.connection.send(new ClientboundChatPacket(FabricAudienceProvider.adapt(text), mcType, Util.NIL_UUID));
    }
  }

  @Override
  public void sendActionBar(final @NonNull Component message) {
    this.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.ACTIONBAR, FabricAudienceProvider.adapt(message)));
  }

  @Override
  public void showBossBar(final @NonNull BossBar bar) {
    ServerBossBarListener.INSTANCE.subscribe((ServerPlayer) (Object) this, bar);
  }

  @Override
  public void hideBossBar(final @NonNull BossBar bar) {
    ServerBossBarListener.INSTANCE.unsubscribe((ServerPlayer) (Object) this, bar);
  }

  @Override
  public void playSound(final @NonNull Sound sound) {
    this.connection.send(new ClientboundCustomSoundPacket(FabricAudienceProvider.adapt(sound.name()),
      GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), this.position(), sound.volume(), sound.pitch()));
  }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
    this.connection.send(new ClientboundCustomSoundPacket(FabricAudienceProvider.adapt(sound.name()),
      GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), new Vec3(x, y, z), sound.volume(), sound.pitch()));
  }

  @Override
  public void stopSound(final @NonNull SoundStop stop) {
    final /* @Nullable */ Key sound = stop.sound();
    final Sound./* @Nullable */ Source src = stop.source();
    final /* @Nullable */ SoundSource cat = src == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(src);
    this.connection.send(new ClientboundStopSoundPacket(sound == null ? null : FabricAudienceProvider.adapt(sound), cat));
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
      pages.add(StringTag.valueOf(this.adventure$serialize(page)));
    }
    bookTag.put(BOOK_PAGES, pages);
    bookTag.putBoolean(BOOK_RESOLVED, true); // todo: any parseable texts?

    final ItemStack previous = this.inventory.getSelected();
    this.connection.send(new ClientboundContainerSetSlotPacket(-2, this.inventory.selected, bookStack));
    this.openItemGui(bookStack, InteractionHand.MAIN_HAND);
    this.connection.send(new ClientboundContainerSetSlotPacket(-2, this.inventory.selected, previous));
  }

  private String adventure$serialize(final @NonNull Component component) {
    return GsonComponentSerializer.gson().serialize(component);
  }

  @Override
  public void showTitle(final @NonNull Title title) {
    if(title.subtitle() != TextComponent.empty()) {
      this.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.SUBTITLE, FabricAudienceProvider.adapt(title.subtitle())));
    }

    final /* @Nullable */ Title.Times times = title.times();
    if(times != null) {
      final int fadeIn = this.ticks(times.fadeIn());
      final int fadeOut = this.ticks(times.fadeOut());
      final int dwell = this.ticks(times.stay());
      if(fadeIn != -1 || fadeOut != -1 || dwell != -1) {
        this.connection.send(new ClientboundSetTitlesPacket(fadeIn, dwell, fadeOut));
      }
    }

    if(title.title() != TextComponent.empty()) {
      this.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.TITLE, FabricAudienceProvider.adapt(title.title())));
    }
  }

  private int ticks(final @NonNull Duration duration) {
    return duration.getSeconds() == -1 ? -1 : (int) (duration.toMillis() / 50);
  }

  @Override
  public void clearTitle() {
    this.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.CLEAR, null));
  }

  @Override
  public void resetTitle() {
    this.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.RESET, null));
  }

  // Player tracking for boss bars

  @Inject(method = "restoreFrom", at = @At("RETURN"))
  private void copyBossBars(final ServerPlayer old, final boolean alive, final CallbackInfo ci) {
    ServerBossBarListener.INSTANCE.replacePlayer(old, (ServerPlayer) (Object) this);
  }

  @Inject(method = "disconnect", at = @At("RETURN"))
  private void removeBossBarsOnDisconnect(final CallbackInfo ci) {
    ServerBossBarListener.INSTANCE.unsubscribeFromAll((ServerPlayer) (Object) this);
  }

}
