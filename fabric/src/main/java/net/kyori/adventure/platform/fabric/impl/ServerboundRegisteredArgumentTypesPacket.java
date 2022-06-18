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
package net.kyori.adventure.platform.fabric.impl;

import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyori.adventure.Adventure;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A packet sent client to server, to let the server know which optional argument types are available on the server.
 *
 * <p>This packet is sent by players on join, before the command tree is sent to the client.</p>
 *
 * @param known Known argument type ids
 */
public record ServerboundRegisteredArgumentTypesPacket(Set<ResourceLocation> known) {
  public static final ResourceLocation ID = new ResourceLocation(Adventure.NAMESPACE, "registered_args");

  public static void register() {
    ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buffer, responder) -> {
      final ServerboundRegisteredArgumentTypesPacket pkt = ServerboundRegisteredArgumentTypesPacket.of(buffer);
      server.execute(() -> { // on main thread
        ServerArgumentTypes.knownArgumentTypes(player, pkt.known, responder);
      });
    });
  }

  public static ServerboundRegisteredArgumentTypesPacket of(final Set<ResourceLocation> idents) {
    return new ServerboundRegisteredArgumentTypesPacket(Set.copyOf(idents));
  }

  public static ServerboundRegisteredArgumentTypesPacket of(final @NonNull FriendlyByteBuf buf) {
    final int length = buf.readVarInt();
    final Set<ResourceLocation> items = new HashSet<>();
    for (int i = 0; i < length; ++i) {
      items.add(buf.readResourceLocation());
    }
    return of(items);
  }

  private void toPacket(final FriendlyByteBuf buffer) {
    buffer.writeVarInt(this.known.size());
    for (final ResourceLocation id : this.known) {
      buffer.writeResourceLocation(id);
    }
  }

  /**
   * Send the client's list of identifiers to the server.
   *
   * @param sender the sender to send the packet to
   */
  public void sendTo(final PacketSender sender) {
    if (ClientPlayNetworking.canSend(ID)) {
      final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(this.known.size() * 8));
      this.toPacket(buffer);
      sender.sendPacket(ID, buffer);
    }
  }
}
