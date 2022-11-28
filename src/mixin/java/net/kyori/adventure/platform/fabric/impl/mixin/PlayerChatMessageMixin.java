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
package net.kyori.adventure.platform.fabric.impl.mixin;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.examination.ExaminableProperty;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSigner;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerChatMessage.class)
public abstract class PlayerChatMessageMixin implements SignedMessage {

  // @formatter:off
  @Shadow public abstract MessageSignature shadow$headerSignature();
  @Shadow public abstract Optional<net.minecraft.network.chat.Component> shadow$unsignedContent();
  @Shadow public abstract SignedMessageBody shadow$signedBody();
  @Shadow public abstract MessageSigner shadow$signer();

  @Shadow @Final private SignedMessageHeader signedHeader;
  @Shadow @Final private MessageSignature headerSignature;
  @Shadow @Final private SignedMessageBody signedBody;
  @Shadow @Final private Optional<net.minecraft.network.chat.Component> unsignedContent;
  @Shadow @Final private FilterMask filterMask;
  // @formatter:on

  @Override
  public @NotNull Identity identity() {
    return this.shadow$signer().isSystem() ? Identity.nil() : Identity.identity(this.shadow$signer().profileId());
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
    return this.shadow$headerSignature();
  }

  @Override
  public @Nullable Component unsignedContent() {
    return ComponentLike.unbox(this.shadow$unsignedContent().orElse(this.shadow$signedBody().content().decorated()));
  }

  @Override
  public @NotNull String message() {
    return this.shadow$signedBody().content().plain();
  }

  @Override
  public boolean isSystem() {
    return this.shadow$signer().isSystem();
  }

  @Override
  public boolean canDelete() {
    return this.shadow$headerSignature() != null;
  }

  @Override
  public @NotNull Stream<? extends ExaminableProperty> examinableProperties() {
    return Stream.of(
      ExaminableProperty.of("signedHeader", this.signedHeader),
      ExaminableProperty.of("headerSignature", this.headerSignature),
      ExaminableProperty.of("signedBody", this.signedBody),
      ExaminableProperty.of("unsignedContent", this.unsignedContent),
      ExaminableProperty.of("filterMask", this.filterMask)
    );
  }
}
