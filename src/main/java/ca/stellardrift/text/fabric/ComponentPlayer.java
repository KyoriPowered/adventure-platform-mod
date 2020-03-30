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
package ca.stellardrift.text.fabric;

import net.kyori.text.Component;
import net.minecraft.client.network.packet.TitleS2CPacket;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * An interface to be implemented by players and other rich text component receivers
 */
public interface ComponentPlayer extends ComponentCommandOutput {
    /**
     * Send a chat message to this player.
     *
     * Messages of type {@link MessageType#GAME_INFO} should be
     * sent as action bar titles to preserve formatting.
     *
     * @param text The contents of the message
     * @param type The type of message to send.
     */
    void sendMessage(Component text, MessageType type);

    /**
     * Send a field of a title as a component.
     * Formatting will be preserved, but links will not be clickable.
     *
     * @param field The field to set -- one of {@link TitleS2CPacket.Action#TITLE},
     * {@link TitleS2CPacket.Action#SUBTITLE}, or {@link TitleS2CPacket.Action#ACTIONBAR}
     * @param text The text to set as the title
     */
    void sendTitle(TitleS2CPacket.Action field, Component text);

    static ComponentPlayer of(ServerPlayerEntity ply) {
        return (ComponentPlayer) ply;
    }
}
