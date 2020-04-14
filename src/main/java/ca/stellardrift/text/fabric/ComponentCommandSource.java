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
import net.minecraft.server.command.ServerCommandSource;

/**
 * An interface applied to {@link ServerCommandSource} to allow sending {@link Component Components}
 */
public interface ComponentCommandSource {
    /**
     * Send a result message to the command source
     *
     * @param text The text to send
     * @param sendToOps If this message should be sent to all ops listening
     */
    void sendFeedback(Component text, boolean sendToOps);

    /**
     * Send an error message to the command source
     * @param text The error
     */
    void sendError(Component text);

    ComponentCommandOutput getOutput();

    static ComponentCommandSource of(ServerCommandSource src) {
        if (src == null) {
            return null;
        }

        if (!(src instanceof ComponentCommandSource)) {
            throw new IllegalArgumentException("The ComponentCommandSource mixin failed!");
        }

        return (ComponentCommandSource) src;
    }
}
