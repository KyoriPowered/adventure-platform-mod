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

import ca.stellardrift.text.fabric.mixin.AccessorStyle;
import com.google.common.collect.EnumBiMap;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonPrimitive;
import net.kyori.text.BlockNbtComponent;
import net.kyori.text.Component;
import net.kyori.text.ComponentBuilder;
import net.kyori.text.EntityNbtComponent;
import net.kyori.text.KeybindComponent;
import net.kyori.text.NbtComponent;
import net.kyori.text.NbtComponentBuilder;
import net.kyori.text.ScoreComponent;
import net.kyori.text.SelectorComponent;
import net.kyori.text.StorageNbtComponent;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.serializer.ComponentSerializer;
import net.kyori.text.serializer.gson.BlockNbtComponentPosSerializer;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.NbtText;
import net.minecraft.text.ScoreText;
import net.minecraft.text.SelectorText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.checkerframework.checker.nullness.qual.NonNull;


import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import static ca.stellardrift.text.fabric.TextAdapter.toKey;

public class MinecraftTextSerializer implements ComponentSerializer<Component, Component, Text> {
    public static final MinecraftTextSerializer INSTANCE = new MinecraftTextSerializer();
    private static final EnumBiMap<TextColor, Formatting> TEXT_COLORS = EnumBiMap.create(TextColor.class, Formatting.class);
    private static EnumBiMap<HoverEvent.Action, net.minecraft.text.HoverEvent.Action> HOVER_EVENTS;
    private static EnumBiMap<ClickEvent.Action, net.minecraft.text.ClickEvent.Action> CLICK_EVENTS;
    private static boolean fallbackMode;

    static {
        try {
            CLICK_EVENTS = EnumBiMap.create(ClickEvent.Action.class, net.minecraft.text.ClickEvent.Action.class);
            HOVER_EVENTS = EnumBiMap.create(HoverEvent.Action.class, net.minecraft.text.HoverEvent.Action.class);


            // Colors
            for (Formatting fmt : Formatting.values()) {
                if (fmt.isColor()) {
                    TextColor color = TextColor.NAMES.value(fmt.getName()).orElseThrow(() -> new ExceptionInInitializerError("Unknown MC Formatting color " + fmt));
                    TEXT_COLORS.put(color, fmt);
                }
            }
            checkCoverage(TEXT_COLORS, TextColor.class);

            // Click events
            for (ClickEvent.Action action : ClickEvent.Action.values()) {
                net.minecraft.text.ClickEvent.Action mcAction = net.minecraft.text.ClickEvent.Action.byName(action.name().toLowerCase(Locale.ROOT));
                if (mcAction == null) {
                    throw new ExceptionInInitializerError("Unknown MC ClickAction for " + action.name());
                }
                CLICK_EVENTS.put(action, mcAction);
            }
            checkCoverage(CLICK_EVENTS.inverse(), net.minecraft.text.ClickEvent.Action.class);

            // Hover events
            for (HoverEvent.Action action : HoverEvent.Action.values()) {
                net.minecraft.text.HoverEvent.Action mcAction = net.minecraft.text.HoverEvent.Action.byName(action.name().toLowerCase(Locale.ROOT));
                if (mcAction == null) {
                    throw new ExceptionInInitializerError("Unknown MC HoverAction for " + action.name());
                }
                HOVER_EVENTS.put(action, mcAction);
            }
            checkCoverage(HOVER_EVENTS.inverse(), net.minecraft.text.HoverEvent.Action.class);
            fallbackMode = false;
        } catch (Throwable e) {
            enterFallbackMode(e);
        }
    }

    private static void enterFallbackMode(Throwable t) {
        if (!fallbackMode) {
            TextAdapter.LOGGER.error("Unable to initialize text adapter, entering fallback mode. Is there an update available?", t);
            fallbackMode = true;
        }
    }

    /**
     * Validates that all members of an enum are present in the given map
     * Throws {@link IllegalStateException} if there is a missing value
     *
     * @param toCheck The map to check
     * @param enumClass The enum class to verify coverage
     * @param <T> The type of enum
     */
    private static <T extends Enum<T>> void checkCoverage(Map<T, ?> toCheck,  Class<T> enumClass) throws IllegalStateException {
        for (T value : enumClass.getEnumConstants()) {
            if (!toCheck.containsKey(value)) {
                throw new IllegalStateException("Unmapped " + enumClass.getSimpleName() + " element '" + value + '!');
            }
        }
    }

    private MinecraftTextSerializer() {

    }

    @NonNull
    @Override
    public Component deserialize(@NonNull Text input) {
        if (!fallbackMode) {
            try {
                ComponentBuilder<?, ?> builder = toBuilder(input);
                applyStyle(builder, input.getStyle());

                for (Text child : input.getSiblings()) {
                    builder.append(deserialize(child));
                }
                return builder.build();
            } catch (Throwable t) {
                enterFallbackMode(t);
            }
        }
        return GsonComponentSerializer.INSTANCE.deserialize(Text.Serializer.toJson(input));
    }

    @NonNull
    @Override
    public Text serialize(@NonNull Component component) {
        if (!fallbackMode) {
            try {
                Text text = toText(component);
                applyStyle(text, component.style());

                for (Component child : component.children()) {
                    text.append(serialize(child));
                }

                return text;
            } catch (Throwable t) {
                enterFallbackMode(t);
            }
        }
        return Text.Serializer.fromJson(GsonComponentSerializer.INSTANCE.serialize(component));
    }

    /**
     * Apply the Minecraft style class to a {@link Component}
     *
     * @param builder The component builder
     * @param format The style to corvert
     */
    private void applyStyle(ComponentBuilder<?, ?> builder, Style format) {
        builder.style(b -> {
            AccessorStyle access = (AccessorStyle) format;
            if (format.getColor() != null) {
                b.color(TEXT_COLORS.inverse().get(format.getColor()));
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

            if (format.getClickEvent() != null) {
                net.minecraft.text.ClickEvent ev = format.getClickEvent();
                b.clickEvent(ClickEvent.of(CLICK_EVENTS.inverse().get(ev.getAction()), ev.getValue()));
            }

            if (format.getHoverEvent() != null) {
                net.minecraft.text.HoverEvent ev = format.getHoverEvent();
                b.hoverEvent(HoverEvent.of(HOVER_EVENTS.inverse().get(ev.getAction()), deserialize(ev.getValue())));
            }

            if (format.getInsertion() != null) {
                b.insertion(format.getInsertion());
            }
        });
    }

    private void applyStyle(Text text, net.kyori.text.format.Style format) {
        text.styled(s -> {
            if (format.color() != null) {
                s.setColor(TEXT_COLORS.get(format.color()));
            }

            s.setBold(toBoolean(format.decoration(TextDecoration.BOLD)));
            s.setItalic(toBoolean(format.decoration(TextDecoration.ITALIC)));
            s.setStrikethrough(toBoolean(format.decoration(TextDecoration.STRIKETHROUGH)));
            s.setUnderline(toBoolean(format.decoration(TextDecoration.UNDERLINED)));
            s.setObfuscated(toBoolean(format.decoration(TextDecoration.OBFUSCATED)));

            ClickEvent click = format.clickEvent();
            if (click != null) {
                s.setClickEvent(new net.minecraft.text.ClickEvent(CLICK_EVENTS.get(click.action()), click.value()));
            }

            HoverEvent hover = format.hoverEvent();
            if (hover != null) {
                s.setHoverEvent(new net.minecraft.text.HoverEvent(HOVER_EVENTS.get(hover.action()), serialize(hover.value())));
            }

            s.setInsertion(format.insertion());
        });
    }

    private Boolean toBoolean(TextDecoration.State state) {
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
                String pos = ((NbtText.BlockNbtText) nbt).getPos();
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
            return ScoreComponent.builder(score.getName(), score.getObjective())
                    .value(score.asString());
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

    private Text toText(Component component) {
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
            ScoreText text = new ScoreText(score.name(), score.value());
            text.setScore(score.value());
            return text;
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
