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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;
import java.util.Optional;

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
        return TextAdapter.nonWrapping().style(this.wrapped.style(), false);
    }

    @Override
    public String getString() {
        return TextAdapter.plain().serialize(this.wrapped);
    }

    @Override
    public String asString() {
        if (this.wrapped instanceof TextComponent) {
            return ((TextComponent) this.wrapped).content();
        } else {
            return TextAdapter.nonWrapping().toText(this.wrapped).asString();
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
        return TextAdapter.nonWrapping().toText(this.wrapped);
    }

    @Override
    public MutableText shallowCopy() {
        return TextAdapter.nonWrapping().serialize(this.wrapped);
    }

    @Override
    public <T> Optional<T> visit(StyledVisitor<T> visitor, Style style) {
        return visit(this.wrapped, visitor, style);
    }

    @Override
    public <T> Optional<T> visit(Visitor<T> visitor) {
        return visit(this.wrapped, visitor);
    }

    // reimplement visitor methods but without creating a wrapper ComponentText instance

    private static <T> Optional<T> visit(Component comp, StyledVisitor<T> visitor, Style style) {
        final Style filledStyle = TextAdapter.nonWrapping().styleWithParent(comp.style(), style, false);
        Optional<T> ret = visitSelf(comp, visitor, filledStyle);
        if (ret.isPresent()) {
            return ret;
        }

        for (Component child : comp.children()) {
            ret = visit(child, visitor, filledStyle);
            if (ret.isPresent()) {
                return ret;
            }
        }

        return Optional.empty();
    }

    private static <T> Optional<T> visit(Component comp, Visitor<T> visitor) {
        Optional<T> ret = visitSelf(comp, visitor);
        if (ret.isPresent()) {
            return ret;
        }

        for (Component child : comp.children()) {
            Optional<T> response = visit(child, visitor);
            if (response.isPresent()) {
                return response;
            }
        }

        return Optional.empty();
    }

    /**
     * Visit the provided component. For TextComponents we can provide
     * an accurate string, but for anything more complicated we delegate to the game.
     *
     * @param comp    The component to represent
     * @param visitor callback to handle the string content
     * @param style   applicable style for the text
     * @return an optional that is present to terminate the visit
     */
    private static <T> Optional<T> visitSelf(Component comp, StyledVisitor<T> visitor, Style style) {
        if (comp instanceof TextComponent) {
            return visitor.accept(style, ((TextComponent) comp).content());
        } else {
            return TextAdapter.nonWrapping().toText(comp).visitSelf(visitor, style);
        }
    }

    /**
     * Visit the provided component in an unstyled manner
     *
     * @param comp The component to visit
     * @param visitor The visitor to use
     * @param <T> the type of the returned optional
     * @return an optional that if present will terminate the visit
     * @see #visitSelf(Component, StyledVisitor, Style) for details on how specific component types are handled
     */
    private static <T> Optional<T> visitSelf(Component comp, Visitor<T> visitor) {
        if (comp instanceof TextComponent) {
            return visitor.accept(((TextComponent) comp).content());
        } else {
            return TextAdapter.nonWrapping().toText(comp).visitSelf(visitor);
        }
    }
}
