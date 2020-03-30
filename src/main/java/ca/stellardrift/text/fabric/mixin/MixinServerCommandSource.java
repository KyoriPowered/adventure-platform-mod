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

import ca.stellardrift.text.fabric.ComponentCommandOutput;
import ca.stellardrift.text.fabric.ComponentCommandSource;
import ca.stellardrift.text.fabric.TextAdapter;
import net.kyori.text.Component;
import net.kyori.text.format.TextColor;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * The methods in this class should match the implementations of their Text-using counterparts in {@link ServerCommandSource}
 *
 */
@Mixin(ServerCommandSource.class)
public abstract class MixinServerCommandSource implements ComponentCommandSource {
    @Shadow @Final
    private CommandOutput output;
    @Shadow @Final
    private boolean silent;

    @Shadow
    abstract void sendToOps(Text text);

    private ComponentCommandOutput ownOut;


    @Override
    public void sendFeedback(Component text, boolean sendToOps) {
        if (getOutput().sendCommandFeedback() && !silent) {
            getOutput().sendMessage(text);
        }

        if (sendToOps && getOutput().shouldBroadcastConsoleToOps() && !silent) {
            this.sendToOps(TextAdapter.toMcText(text));
        }
    }

    @Override
    public void sendError(Component text) {
        if (getOutput().shouldTrackOutput()) {
            getOutput().sendMessage(text.color(TextColor.RED));
        }
    }

    @Override
    public ComponentCommandOutput getOutput() {
        if (ownOut == null) {
            ownOut = ComponentCommandOutput.of(output);
        }
        return ownOut;
    }
}
