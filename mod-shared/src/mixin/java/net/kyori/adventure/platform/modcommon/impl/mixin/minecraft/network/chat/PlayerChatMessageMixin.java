/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2022-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.network.chat;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.platform.modcommon.impl.PlayerChatMessageBridge;
import net.kyori.adventure.text.Component;
import net.kyori.examination.ExaminableProperty;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Implements(@Interface(iface = SignedMessage.class, prefix = "signedMessage$"))
@Mixin(PlayerChatMessage.class)
public abstract class PlayerChatMessageMixin implements SignedMessage, PlayerChatMessageBridge {

  // @formatter:off
  @Shadow public abstract MessageSignature shadow$signature();
  @Shadow public abstract net.minecraft.network.chat.@Nullable Component shadow$unsignedContent();
  @Shadow public abstract SignedMessageBody shadow$signedBody();
  @Shadow public abstract SignedMessageLink shadow$link();
  @Shadow public abstract boolean shadow$isSystem();

  @Shadow @Final private SignedMessageLink link;
  @Shadow @Final private MessageSignature signature;
  @Shadow @Final private SignedMessageBody signedBody;
  @Shadow @Final private net.minecraft.network.chat.@Nullable Component unsignedContent;
  @Shadow @Final private FilterMask filterMask;
  // @formatter:on

  private MinecraftAudiencesInternal adventure$controller;

  @Override
  public @NotNull Identity identity() {
    return this.shadow$isSystem() ? Identity.nil() : Identity.identity(this.shadow$link().sender());
  }

  @Override
  public @NotNull Instant timestamp() {
    return this.shadow$signedBody().timeStamp();
  }

  @Override
  public long salt() {
    return this.shadow$signedBody().salt();
  }

  @Override
  public @Nullable Signature signature() {
    return (Signature) (Object) this.shadow$signature();
  }

  @Override
  public void adventure$controller(final MinecraftAudiencesInternal controller) {
    this.adventure$controller = controller;
  }

  @Override
  public @Nullable Component unsignedContent() {
    final MinecraftAudiencesInternal controller = Objects.requireNonNull(this.adventure$controller, "Missing controller");
    final net.minecraft.network.chat.Component unsignedContent = this.shadow$unsignedContent();
    if (unsignedContent == null) {
      return null;
    }
    return controller.asAdventure(unsignedContent);
  }

  @Inject(
    method = {
      "withUnsignedContent",
      "removeUnsignedContent",
      "filter(Lnet/minecraft/network/chat/FilterMask;)Lnet/minecraft/network/chat/PlayerChatMessage;",
      "removeSignature"
    },
    at = @At(value = "RETURN")
  )
  private void injectCopyCreation(final CallbackInfoReturnable<PlayerChatMessage> cir) {
    ((PlayerChatMessageBridge) (Object) cir.getReturnValue()).adventure$controller(this.adventure$controller);
  }

  @Override
  public @NotNull String message() {
    return this.shadow$signedBody().content();
  }

  @Intrinsic
  public boolean signedMessage$isSystem() {
    return this.shadow$isSystem();
  }

  @Override
  public boolean canDelete() {
    return this.shadow$signature() != null;
  }

  @Override
  public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
    return Stream.of(
      ExaminableProperty.of("link", this.link),
      ExaminableProperty.of("signature", this.signature),
      ExaminableProperty.of("signedBody", this.signedBody),
      ExaminableProperty.of("unsignedContent", this.unsignedContent),
      ExaminableProperty.of("filterMask", this.filterMask)
    );
  }
}
