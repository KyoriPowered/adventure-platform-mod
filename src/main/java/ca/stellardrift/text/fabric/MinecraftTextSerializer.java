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

import ca.stellardrift.text.fabric.mixin.AccessorHoverEventShowItem;
import ca.stellardrift.text.fabric.mixin.AccessorStyle;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.BlockNbtComponentPosSerializer;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import net.minecraft.text.*;
import net.minecraft.text.HoverEvent.class_5247;
import net.minecraft.util.registry.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ListIterator;

import static ca.stellardrift.text.fabric.TextAdapter.toKey;

public class MinecraftTextSerializer implements ComponentSerializer<Component, Component, Text> {
    public static final MinecraftTextSerializer INSTANCE = new MinecraftTextSerializer();

    private MinecraftTextSerializer() {

    }

    @NonNull
    @Override
    public Component deserialize(@NonNull Text input) {
        ComponentBuilder<?, ?> builder = toBuilder(input);
        applyStyle(builder, input.getStyle());

        for (Text child : input.getSiblings()) {
            builder.append(deserialize(child));
        }
        return builder.build();
    }

    @NonNull
    @Override
    public Text serialize(@NonNull Component component) {
        class_5250 text = toText(component);
        applyStyle(text, component.style());

        for (Component child : component.children()) {
            text.append(serialize(child));
        }

        return text;
    }

    Style style(net.kyori.adventure.text.format.Style style) {
        @Nullable ClickEvent click = style.clickEvent();
        @Nullable HoverEvent<?> hover = style.hoverEvent();
        @Nullable Key font = style.font();
        return AccessorStyle.createNew(
                style.color() == null ? null : class_5251.method_27717(style.color().value()),
                toBoolean(style.decoration(TextDecoration.BOLD)),
                toBoolean(style.decoration(TextDecoration.ITALIC)),
                toBoolean(style.decoration(TextDecoration.UNDERLINED)),
                toBoolean(style.decoration(TextDecoration.STRIKETHROUGH)),
                toBoolean(style.decoration(TextDecoration.OBFUSCATED)),
                click == null ? null : new net.minecraft.text.ClickEvent(GameEnums.CLICK_EVENT.toMinecraft(click.action()), click.value()),
                null,
                style.insertion(),
                font == null ? null : TextAdapter.toIdentifier(font)
        );

    }

    net.kyori.adventure.text.format.Style style(Style mcStyle) {
        return net.kyori.adventure.text.format.Style.make(b -> {
            AccessorStyle access = (AccessorStyle) mcStyle;
            if (mcStyle.getColor() != null) {
                b.color(TextColor.of(mcStyle.getColor().method_27716()));
            }
            if (access.getBold() != null) {
                b.decoration(TextDecoration.BOLD, access.getBold());
            }

            if (access.getItalic() != null) {
                b.decoration(TextDecoration.ITALIC, access.getItalic());
            }

            if (access.getObfuscated() != null) {
                b.decoration(TextDecoration.OBFUSCATED, access.getObfuscated());
            }

            if (access.getStrikethrough() != null) {
                b.decoration(TextDecoration.STRIKETHROUGH, access.getStrikethrough());
            }

            if (access.getUnderline() != null) {
                b.decoration(TextDecoration.UNDERLINED, access.getUnderline());
            }

            if (mcStyle.getClickEvent() != null) {
                net.minecraft.text.ClickEvent ev = mcStyle.getClickEvent();
                b.clickEvent(ClickEvent.of(GameEnums.CLICK_EVENT.toAdventure(ev.getAction()), ev.getValue()));
            }

            if (mcStyle.getHoverEvent() != null) {
                net.minecraft.text.HoverEvent ev = mcStyle.getHoverEvent();
                b.hoverEvent(convertHoverEvent(ev));
            }

            if (mcStyle.getInsertion() != null) {
                b.insertion(mcStyle.getInsertion());
            }

            if (access.getFont() != null) {
                b.font(TextAdapter.toKey(access.getFont()));
            }
        });
    }

    /**
     * Apply the Minecraft style class to a {@link Component}
     *
     * @param builder The component builder
     * @param format The style to corvert
     */
    private void applyStyle(ComponentBuilder<?, ?> builder, Style format) {
        builder.style(style(format));
    }

    private void applyStyle(class_5250 text, net.kyori.adventure.text.format.Style format) {
        text.setStyle(style(format));
    }

    private HoverEvent<?> convertHoverEvent(net.minecraft.text.HoverEvent mcEvent) {
        class_5247<?> action = mcEvent.getAction();
        if (action == class_5247.SHOW_TEXT) {
            return HoverEvent.showText(deserialize(mcEvent.getValue(class_5247.SHOW_TEXT)));
        } else if (action == class_5247.SHOW_ENTITY) {
            return HoverEvent.showEntity(convertShowEntity(mcEvent.getValue(class_5247.SHOW_ENTITY)));
        } else if (action == class_5247.SHOW_ITEM) {
            return HoverEvent.showItem(convertShowItem(mcEvent.getValue(class_5247.SHOW_ITEM)));
        } else {
            throw unknownType(action);
        }
    }

    private HoverEvent.ShowEntity convertShowEntity(net.minecraft.text.HoverEvent.class_5248 mcEntity) {
        final Key type = TextAdapter.toKey(Registry.ENTITY_TYPE.getId(mcEntity.field_24351));
        final @Nullable Text text = mcEntity.field_24353;
        return new HoverEvent.ShowEntity(type, mcEntity.field_24352, text == null ? null : deserialize(text));
    }

    private HoverEvent.ShowItem convertShowItem(net.minecraft.text.HoverEvent.class_5249 mcItem) {
        AccessorHoverEventShowItem mc = (AccessorHoverEventShowItem) mcItem;
        return new HoverEvent.ShowItem(TextAdapter.toKey(Registry.ITEM.getId(mc.getItem())), mc.getCount());
    }

    private @Nullable Boolean toBoolean(TextDecoration.State state) {
        switch (state) {
            case TRUE: return true;
            case FALSE: return false;
            case NOT_SET: return null;
            default: throw new Error();
        }
    }

    private ComponentBuilder<?, ?> toBuilder(Text text) {
        if (text instanceof NbtText) {
            NbtText nbt = (NbtText) text;
            NbtComponentBuilder<?, ?> builder;
            if (text instanceof NbtText.BlockNbtText) {
                @Nullable String pos = ((NbtText.BlockNbtText) nbt).getPos();
                if (pos != null) {
                    builder = BlockNbtComponent.builder().pos(BlockNbtComponentPosSerializer.INSTANCE.deserialize(new JsonPrimitive(pos), null, null)); // TODO: Make this less bad
                } else {
                    builder = BlockNbtComponent.builder();
                }
            } else if (text instanceof NbtText.EntityNbtText) {
                builder = EntityNbtComponent.builder().selector(((NbtText.EntityNbtText) nbt).getSelector());
            } else if (text instanceof NbtText.StorageNbtText) {
                builder = StorageNbtComponent.builder().storage(toKey(((NbtText.StorageNbtText) text).method_23728()));
            } else {
                throw unknownType(text);
            }
            builder.nbtPath(nbt.getPath());
            builder.interpret(nbt.shouldInterpret());
            return builder;
        } else if (text instanceof KeybindText) {
            KeybindText keybind = (KeybindText) text;
            return KeybindComponent.builder(keybind.getKey());
        } else if (text instanceof LiteralText) {
            LiteralText literal = (LiteralText) text;
            return TextComponent.builder(literal.asString());
        } else if (text instanceof ScoreText) {
            ScoreText score = (ScoreText) text;
            return ScoreComponent.builder(score.getName(), score.getObjective());
        } else if (text instanceof SelectorText) {
            SelectorText selector = (SelectorText) text;
            return SelectorComponent.builder(selector.getPattern());
        } else if (text instanceof TranslatableText) {
            TranslatableText translatable = (TranslatableText) text;
            TranslatableComponent.Builder build = TranslatableComponent.builder(translatable.getKey());
            ImmutableList.Builder<Component> args = ImmutableList.builder();
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Text) {
                    args.add(deserialize(((Text) arg)));
                } else {
                    args.add(TextComponent.of(arg.toString())); // sure?
                }
            }
            build.args(args.build());
            return build;
        } else {
            throw unknownType(text);
        }
    }

    private class_5250 toText(Component component) {
        if (component instanceof NbtComponent<?, ?>) {
            NbtComponent<?, ?> nbt = (NbtComponent<?, ?>) component;
            if (component instanceof BlockNbtComponent) {
                return new NbtText.BlockNbtText(nbt.nbtPath(), nbt.interpret(), ((BlockNbtComponent) component).pos().toString());
            } else if (component instanceof EntityNbtComponent) {
                return new NbtText.EntityNbtText(nbt.nbtPath(), nbt.interpret(), ((EntityNbtComponent) nbt).selector());
            } else if (component instanceof StorageNbtComponent) {
                return new NbtText.StorageNbtText(nbt.nbtPath(), nbt.interpret(), TextAdapter.toIdentifier(((StorageNbtComponent) nbt).storage()));
            } else {
                throw unknownType(component);
            }
        } else if (component instanceof KeybindComponent) {
            KeybindComponent keybind = (KeybindComponent) component;
            return new KeybindText(keybind.keybind());
        } else if (component instanceof TextComponent) {
            TextComponent text = (TextComponent) component;
            return new LiteralText(text.content());
        } else if (component instanceof ScoreComponent) {
            ScoreComponent score = (ScoreComponent) component;
            return new ScoreText(score.name(), score.objective());
        } else if (component instanceof SelectorComponent) {
            SelectorComponent selector = (SelectorComponent) component;
            return new SelectorText(selector.pattern());
        } else if (component instanceof TranslatableComponent) {
            TranslatableComponent translatable = (TranslatableComponent) component;
            Object[] args = new Object[translatable.args().size()];
            ListIterator<Component> it = translatable.args().listIterator();
            while (it.hasNext()) {
                args[it.nextIndex()] = serialize(it.next());
            }
            return new TranslatableText(translatable.key(), args);
        } else {
            throw unknownType(component);
        }
    }

    private RuntimeException unknownType(Object obj) {
        throw new IllegalArgumentException("Unknown text type " + obj.getClass());
    }
}
