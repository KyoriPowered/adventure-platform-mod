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

import ca.stellardrift.text.fabric.ComponentHoldingPacket;
import ca.stellardrift.text.fabric.TextAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.network.packet.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatMessageS2CPacket.class)
public class MixinChatMessageS2CPacket implements ComponentHoldingPacket {
    @Shadow
    private Text message;

    private Component component;

    @Override
    public Component getComponent() {
        Component comp = this.component;
        if (comp == null) {
            return this.component = TextAdapter.toComponent(this.message);
        } else {
            return comp;
        }
    }

    @Override
    public void setComponent(Component component) {
        this.component = component;
        this.message = null;
    }

    @Inject(method = "getMessage", at = @At("HEAD"), require = 0)
    public void returnConvertedComponent(CallbackInfoReturnable<Text> ci) {
        if (this.message == null && this.component != null) {
            this.message = TextAdapter.toMcText(this.component);
        }
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PacketByteBuf;writeText(Lnet/minecraft/text/Text;)Lnet/minecraft/util/PacketByteBuf;"))
    public PacketByteBuf writeText(PacketByteBuf buf, Text message) {
        if (this.component != null) {
            return buf.writeString(GsonComponentSerializer.INSTANCE.serialize(this.component));
        } else {
            return buf.writeText(message);
        }
    }
}
