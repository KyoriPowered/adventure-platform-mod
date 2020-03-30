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
import net.minecraft.server.command.CommandOutput;
import net.minecraft.text.Text;

/**
 * Represents a {@link CommandOutput} that can receive {@link Component} messages
 */
public interface ComponentCommandOutput extends CommandOutput {
    /**
     * Send a message to this receiver as a component
     *
     * @param text The text to send
     */
    void sendMessage(Component text);

    /**
     * Convert a standard {@link CommandOutput} into a ComponentCommandOutput.
     *
     * @param out The original output
     * @return The original output, if it supports components directly, or a wrapper that converts
     */
    static ComponentCommandOutput of(CommandOutput out) {
        return out == null ? null : new Wrapping(out);
    }

    /**
     * Wrapper for otherwise incompatible CommandOutput instances
     */
    final class Wrapping implements ComponentCommandOutput {
        private final CommandOutput original;

        Wrapping(CommandOutput original) {
            this.original = original;
        }

        public CommandOutput getOriginal() {
            return this.original;
        }

        /**
         * Send a message to this receiver as a component
         *
         * @param text The text to send
         */
        @Override
        public void sendMessage(Component text) {
           sendMessage(TextAdapter.toMcText(text));
        }

        @Override
        public void sendMessage(Text text) {
            getOriginal().sendMessage(text);
        }

        @Override
        public boolean sendCommandFeedback() {
            return getOriginal().sendCommandFeedback();
        }

        @Override
        public boolean shouldTrackOutput() {
            return getOriginal().shouldTrackOutput();
        }

        @Override
        public boolean shouldBroadcastConsoleToOps() {
            return getOriginal().shouldBroadcastConsoleToOps();
        }
    }

}
