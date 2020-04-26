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

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;

public final class ComponentText implements Text {
    private final Component wrapped;
    private volatile @MonotonicNonNull List<Text> siblings;

    ComponentText(final Component wrapped) {
        this.wrapped = wrapped;
    }

    public Component getWrapped() {
        return this.wrapped;
    }

    @Override
    public Style getStyle() {
        return TextAdapter.textNonWrapping().style(this.wrapped.style());
    }

    @Override
    public String getString() {
        return PlainComponentSerializer.INSTANCE.serialize(this.wrapped);
    }

    @Override
    public String asString() {
        if (this.wrapped instanceof TextComponent) {
            return ((TextComponent) this.wrapped).content();
        } else {
            return copy().asString();
        }
    }

    @Override
    public List<Text> getSiblings() {
        if (this.siblings == null) {
            final ImmutableList.Builder<Text> ret = ImmutableList.builder();
            for (Component child : this.wrapped.children()) {
                ret.add(new ComponentText(child));
            }
            return this.siblings = ret.build();
        }
        return this.siblings;
    }

    @Override
    public MutableText copy() {
        return TextAdapter.textNonWrapping().toText(this.wrapped);
    }

    @Override
    public MutableText shallowCopy() {
        return TextAdapter.textNonWrapping().serialize(this.wrapped);
    }
}
