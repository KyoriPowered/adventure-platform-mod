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
package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.network.protocol.game;

import net.kyori.adventure.platform.fabric.impl.ServerArgumentTypes;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientboundCommandsPacket.class)
public abstract class ClientboundCommandsPacketMixin {
  @Redirect(
    method = "read(Lnet/minecraft/network/FriendlyByteBuf;B)Lnet/minecraft/network/protocol/game/ClientboundCommandsPacket$NodeStub;",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;byId(I)Ljava/lang/Object;")
  )
  private static Object redirectUnmap(final Registry<?> instance, final int id) {
    if (ServerArgumentTypes.hasId(id)) {
      return ServerArgumentTypes.byId(id).argumentTypeInfo();
    }
    return instance.byId(id);
  }

  @Mixin(targets = "net.minecraft.network.protocol.game.ClientboundCommandsPacket$ArgumentNodeStub")
  public static abstract class ArgumentNodeStubMixin {
    @Redirect(
      method = "serializeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo;Lnet/minecraft/commands/synchronization/ArgumentTypeInfo$Template;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getId(Ljava/lang/Object;)I")
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int redirectGetId(final Registry registry, final @Nullable Object argumentTypeInfo) {
      if (ServerArgumentTypes.isServerType((ArgumentTypeInfo<?, ?>) argumentTypeInfo)) {
        return ServerArgumentTypes.id((ArgumentTypeInfo<?, ?>) argumentTypeInfo);
      }
      return registry.getId(argumentTypeInfo);
    }
  }
}
