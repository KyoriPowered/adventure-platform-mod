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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleS2CPacket.class)
public class MixinTitleS2CPacket implements ComponentHoldingPacket {
    private @Nullable Component component;
    @Shadow
    private Text text;


    @Override
    public Component getComponent() {
        if (component == null && text != null) {
            component = TextAdapter.toComponent(text);
        }
        return component;
    }

    @Override
    public void setComponent(Component component) {
        this.component = component;
        this.text = null;
    }

    @Inject(method = "getText", at = @At("HEAD"), require = 0)
    private void initTextIfComponent(CallbackInfoReturnable<Text> cir) {
        if (this.component != null) {
            this.text = TextAdapter.toMcText(this.component);
        }
    }

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeText(Lnet/minecraft/text/Text;)Lnet/minecraft/network/PacketByteBuf;"))
    public PacketByteBuf writeComponent(PacketByteBuf buf, Text param) {
        if (this.component != null) {
            return buf.writeString(GsonComponentSerializer.INSTANCE.serialize(this.component), MAX_TEXT_PACKET_LENGTH);
        } else {
            return buf.writeText(param);
        }
    }
}
