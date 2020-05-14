/*
 * Copyright © 2020 zml [at] stellardrift [.] ca
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.stellardrift.text.fabric.mixin;

import ca.stellardrift.text.fabric.ComponentPlayer;
import ca.stellardrift.text.fabric.TextAdapter;
import com.mojang.authlib.GameProfile;
import net.kyori.text.Component;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements ComponentPlayer {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;

    public MixinServerPlayerEntity(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    /**
     * Send a message to this receiver as a component
     *
     * @param text The text to send
     */
    @Override
    public void sendMessage(Component text) {
        sendMessage(text, MessageType.SYSTEM);
    }

    @Override
    public void sendMessage(Component text, MessageType type) {
        if (type == MessageType.GAME_INFO) {
            sendTitle(TitleS2CPacket.Action.ACTIONBAR, text);
        } else {
            networkHandler.sendPacket(TextAdapter.createChatPacket(text, type));
        }
    }

    @Override
    public void sendTitle(TitleS2CPacket.Action field, Component text) {
        networkHandler.sendPacket(TextAdapter.createTitlePacket(field, text));
    }
}
